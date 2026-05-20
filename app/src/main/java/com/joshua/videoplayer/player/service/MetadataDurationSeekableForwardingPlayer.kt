package com.joshua.videoplayer.player.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import java.io.File
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.Volatile
import kotlin.math.max

/**
 * 部分 fMP4 在 Exo 中 [Player.getDuration] 为 [C.TIME_UNSET] 且/或 [Player.isCurrentMediaItemSeekable] 为 false，
 * [ForwardingPlayer.getAvailableCommands] 又直接委托给内核，导致 [COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM] 不可用，
 * [androidx.media3.session.MediaController.seekTo] 无效或回到开头。
 *
 * [Player.getDuration]/[Player.getContentDuration]：仅当内核返回无效（≤0 或 [C.TIME_UNSET]）时用元数据补齐，便于系统控件/会话客户端；迷你条与全屏条 [com.joshua.videoplayer.ui.media.uiDurationMs] 在内核时长有效时以内核为准，避免歌单元数据覆盖。
 *
 * 仅在「列表里已有可靠 [MediaMetadata.durationMs]」且**内核** [Player.isCurrentMediaItemSeekable] 为 false 时，对 [seekTo] 走
 * [ExoPlayer.stop] + [ExoPlayer.setMediaItem]/[ExoPlayer.setMediaItems]（**始终从 0 毫秒打开**）+ [ExoPlayer.prepare]，
 * 再在 [Player.STATE_READY] 时对 **底层 [exo] 直接** [ExoPlayer.seekTo]（勿走本类 [seekTo] 覆写，避免递归）。
 * 部分 fMP4 会忽略 [ExoPlayer.setMediaItem] 的 startPositionMs，故在 READY 后再 seek。
 * **不用** [MediaItem.ClippingConfiguration]：对「不可 seek」的渐进式源会触发
 * [androidx.media3.exoplayer.source.ClippingMediaSource.IllegalClippingException]（not seekable to start），且错误在 prepare 后异步抛出。
 * 内核已标为可 seek 时始终走委托 [seekTo]，不因时长未就绪或元数据/内核时长差走硬重载。
 */
internal class MetadataDurationSeekableForwardingPlayer(
    appContext: Context,
    private val exo: ExoPlayer,
) : ForwardingPlayer(exo) {

    private val applicationContext: Context = appContext.applicationContext

    private val appHandler = Handler(exo.applicationLooper)

    private val proactiveRemuxRunnable = Runnable { tryProactiveRemuxToSeekableFile() }
    private val proactiveRemuxInFlight = AtomicBoolean(false)

    init {
        exo.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState != Player.STATE_READY) return
                appHandler.removeCallbacks(proactiveRemuxRunnable)
                appHandler.postDelayed(proactiveRemuxRunnable, 600L)
            }
        })
    }

    private inline fun runOnApplicationLooper(crossinline block: () -> Unit) {
        if (Looper.myLooper() == exo.applicationLooper) {
            block()
        } else {
            appHandler.post { block() }
        }
    }

    /** [stop] 后再 [setMediaItem] 更干净；克隆避免同一 [MediaItem] 实例复用导致起始位置被忽略。 */
    private fun cloneItemForHardSeek(item: MediaItem): MediaItem {
        val uri = item.localConfiguration?.uri ?: return item
        val lc = item.localConfiguration
        val b = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(item.mediaMetadata)
        if (lc != null && lc.mimeType != null) {
            b.setMimeType(lc.mimeType)
        }
        return b.build()
    }

    private fun metadataDurationMsAt(mediaItemIndex: Int): Long {
        if (mediaItemIndex < 0 || mediaItemIndex >= exo.mediaItemCount) return C.TIME_UNSET
        val m = exo.getMediaItemAt(mediaItemIndex).mediaMetadata.durationMs ?: C.TIME_UNSET
        return if (m > 0L && m != C.TIME_UNSET) m else C.TIME_UNSET
    }

    private fun metadataDurationMs(): Long = metadataDurationMsAt(exo.currentMediaItemIndex)

    override fun getDuration(): Long {
        val d = super.getDuration()
        if (d > 0L && d != C.TIME_UNSET) return d
        val md = metadataDurationMs()
        return if (md > 0L && md != C.TIME_UNSET) md else d
    }

    override fun getContentDuration(): Long {
        val d = super.getContentDuration()
        if (d > 0L && d != C.TIME_UNSET) return d
        val md = metadataDurationMs()
        return if (md > 0L && md != C.TIME_UNSET) md else d
    }

    /**
     * 列表已写入可靠时长时：内核不可 seek、或内核无有效时长 → 为 UI 补齐 seek 相关命令（实际跳转由 [hardReseekTo] 兜底）。
     */
    private fun innerTimelineMuchShorterThanMetadata(innerDur: Long, md: Long): Boolean {
        if (innerDur <= 0L || innerDur == C.TIME_UNSET || md <= 0L) return false
        val gap = max(5000L, md / 10L)
        return innerDur < md - gap
    }

    private fun shouldAugmentSeekFromMetadata(): Boolean {
        val md = metadataDurationMs()
        if (md <= 0L || md == C.TIME_UNSET) return false
        if (!super.isCurrentMediaItemSeekable()) return true
        val innerDur = super.getDuration()
        if (innerDur <= 0L || innerDur == C.TIME_UNSET) return true
        return innerTimelineMuchShorterThanMetadata(innerDur, md)
    }

    private fun canRebuildQueueForHardReseek(): Boolean =
        exo.mediaItemCount <= 1 || !exo.shuffleModeEnabled

    /**
     * 仅当**内核** [Player.isCurrentMediaItemSeekable] 为 false 且**非本地 Uri** 时硬重载。
     *
     * 日志实证：部分 `content://` 分片 MP4 上 Exo 报 `innerSeekable=false`、时长 [C.TIME_UNSET]，
     * 硬重载（stop→prepare→READY 后 [exo.seekTo]）仍停留在 0，拖进度即从头播。
     * 同一文件直接 [ForwardingPlayer.seekTo] 往往仍可跳转，故 **file/content/android.resource** 永不走硬重载。
     *
     * 远程流（如 http）在不可 seek 时仍可尝试硬重载兜底。
     */
    private fun needsHardReseekForSeek(mediaItemIndex: Int): Boolean {
        val md = metadataDurationMsAt(mediaItemIndex)
        if (md <= 0L || md == C.TIME_UNSET) return false
        if (!canRebuildQueueForHardReseek()) return false
        if (isLocalPlaybackUri(mediaItemIndex)) return false
        return !super.isCurrentMediaItemSeekable()
    }

    private fun isLocalPlaybackUri(mediaItemIndex: Int): Boolean {
        if (mediaItemIndex < 0 || mediaItemIndex >= exo.mediaItemCount) return false
        val scheme = exo.getMediaItemAt(mediaItemIndex).localConfiguration?.uri?.scheme
            ?: return false
        return scheme.equals("file", ignoreCase = true) ||
            scheme.equals("content", ignoreCase = true) ||
            scheme.equals("android.resource", ignoreCase = true)
    }

    // 不可 seek 的 content 片源：先拷到缓存再以 file Uri 重载（见 ContentUriSeekCache）。
    private fun shouldRelocalizeWithStartPosition(mediaItemIndex: Int, positionMs: Long): Boolean {
        if (positionMs <= 0L) return false
        if (!isLocalPlaybackUri(mediaItemIndex)) return false
        if (super.isCurrentMediaItemSeekable()) return false
        if (!canRebuildQueueForHardReseek()) return false
        return true
    }

    private fun clampSeekMsToMetadataIfKnown(mediaItemIndex: Int, positionMs: Long): Long {
        val md = metadataDurationMsAt(mediaItemIndex)
        return if (md > 0L && md != C.TIME_UNSET) {
            positionMs.coerceIn(0L, md)
        } else {
            positionMs.coerceAtLeast(0L)
        }
    }

    private fun relocalizeLocalSeekWithStartPosition(mediaItemIndex: Int, positionMs: Long) {
        val count = exo.mediaItemCount
        if (count <= 0 || mediaItemIndex !in 0 until count) {
            super.seekTo(mediaItemIndex, positionMs)
            return
        }
        val pos = clampSeekMsToMetadataIfKnown(mediaItemIndex, positionMs)
        val itemsSnapshot = List(count) { cloneItemForHardSeek(exo.getMediaItemAt(it)) }
        val wasPlaying = exo.playWhenReady
        val targetIndex = mediaItemIndex
        val srcUri = exo.getMediaItemAt(mediaItemIndex).localConfiguration?.uri

        if (srcUri != null && srcUri.scheme.equals("content", ignoreCase = true)) {
            PlaybackSeekDiagnostics.log(
                "localRelocalize content:// -> cache copy then file reload pos=$pos idx=$targetIndex",
            )
            ContentUriSeekCache.ensureLocalFile(applicationContext, srcUri) { result ->
                runOnApplicationLooper {
                    result.fold(
                        onSuccess = { file ->
                            PlaybackSeekDiagnostics.log(
                                "localRelocalize cache ok bytes=${file.length()} path=${file.absolutePath}",
                            )
                            @Suppress("DEPRECATION")
                            val fileUri = Uri.fromFile(file)
                            val snapshot = List(count) { i ->
                                if (i == targetIndex) {
                                    mediaItemWithUri(itemsSnapshot[targetIndex], fileUri)
                                } else {
                                    itemsSnapshot[i]
                                }
                            }
                            scheduleBeginRelocalizeAfterRemux(
                                baseSnapshot = snapshot,
                                targetIndex = targetIndex,
                                localMediaFile = file,
                                pos = pos,
                                wasPlaying = wasPlaying,
                                mediaItemIndex = mediaItemIndex,
                                originalSeekMs = positionMs,
                            )
                        },
                        onFailure = { e ->
                            PlaybackSeekDiagnostics.log("localRelocalize cache failed: ${e.message}")
                            beginLocalRelocalizePlayback(
                                itemsSnapshot = itemsSnapshot,
                                targetIndex = targetIndex,
                                pos = pos,
                                wasPlaying = wasPlaying,
                                mediaItemIndex = mediaItemIndex,
                                originalSeekMs = positionMs,
                            )
                        },
                    )
                }
            }
            return
        }

        val filePath = srcUri?.takeIf { it.scheme.equals("file", ignoreCase = true) }?.path
        if (!filePath.isNullOrBlank()) {
            scheduleBeginRelocalizeAfterRemux(
                baseSnapshot = itemsSnapshot,
                targetIndex = targetIndex,
                localMediaFile = File(filePath),
                pos = pos,
                wasPlaying = wasPlaying,
                mediaItemIndex = mediaItemIndex,
                originalSeekMs = positionMs,
            )
        } else {
            beginLocalRelocalizePlayback(
                itemsSnapshot = itemsSnapshot,
                targetIndex = targetIndex,
                pos = pos,
                wasPlaying = wasPlaying,
                mediaItemIndex = mediaItemIndex,
                originalSeekMs = positionMs,
            )
        }
    }

    /** fMP4 等：先 FFmpeg remux 出 `*_exo.mp4` 再交给 Exo 起播/seek。 */
    private fun scheduleBeginRelocalizeAfterRemux(
        baseSnapshot: List<MediaItem>,
        targetIndex: Int,
        localMediaFile: File,
        pos: Long,
        wasPlaying: Boolean,
        mediaItemIndex: Int,
        originalSeekMs: Long,
    ) {
        if (localMediaFile.name.endsWith("_exo.mp4", ignoreCase = true)) {
            beginLocalRelocalizeWithFileItem(
                baseSnapshot = baseSnapshot,
                targetIndex = targetIndex,
                playFile = localMediaFile,
                pos = pos,
                wasPlaying = wasPlaying,
                mediaItemIndex = mediaItemIndex,
                originalSeekMs = originalSeekMs,
            )
            return
        }
        SeekableMp4Remuxer.ensureRemuxed(applicationContext, localMediaFile) { remuxResult ->
            runOnApplicationLooper {
                val playFile = remuxResult.getOrElse {
                    PlaybackSeekDiagnostics.log("remux failed, keep pre-remux file: ${it.message}")
                    localMediaFile
                }
                beginLocalRelocalizeWithFileItem(
                    baseSnapshot = baseSnapshot,
                    targetIndex = targetIndex,
                    playFile = playFile,
                    pos = pos,
                    wasPlaying = wasPlaying,
                    mediaItemIndex = mediaItemIndex,
                    originalSeekMs = originalSeekMs,
                )
            }
        }
    }

    private fun beginLocalRelocalizeWithFileItem(
        baseSnapshot: List<MediaItem>,
        targetIndex: Int,
        playFile: File,
        pos: Long,
        wasPlaying: Boolean,
        mediaItemIndex: Int,
        originalSeekMs: Long,
    ) {
        @Suppress("DEPRECATION")
        val playUri = Uri.fromFile(playFile)
        val snapshot = List(baseSnapshot.size) { i ->
            if (i == targetIndex) mediaItemWithUri(baseSnapshot[targetIndex], playUri) else baseSnapshot[i]
        }
        beginLocalRelocalizePlayback(
            itemsSnapshot = snapshot,
            targetIndex = targetIndex,
            pos = pos,
            wasPlaying = wasPlaying,
            mediaItemIndex = mediaItemIndex,
            originalSeekMs = originalSeekMs,
        )
    }

    private fun mediaItemWithUri(item: MediaItem, uri: Uri): MediaItem {
        val lc = item.localConfiguration
        val b = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(item.mediaMetadata)
        if (lc != null && lc.mimeType != null) {
            b.setMimeType(lc.mimeType)
        }
        return b.build()
    }

    /**
     * 首次 [STATE_READY] 后延迟触发：对仍不可 seek 的本地片做缓存拷贝 + remux，再 [ExoPlayer.replaceMediaItem]，
     * 用户第一次拖动时往往已可走普通 [seekTo]，避免明显「回 0 再跳」。
     */
    private fun tryProactiveRemuxToSeekableFile() {
        if (exo.playbackState != Player.STATE_READY) return
        if (proactiveRemuxInFlight.get()) return
        val idx = exo.currentMediaItemIndex
        if (idx < 0 || idx >= exo.mediaItemCount) return
        if (super.isCurrentMediaItemSeekable()) return
        if (!isLocalPlaybackUri(idx)) return
        val uri = exo.getMediaItemAt(idx).localConfiguration?.uri ?: return
        if (uri.scheme.equals("file", ignoreCase = true) &&
            uri.path?.endsWith("_exo.mp4", ignoreCase = true) == true
        ) {
            return
        }
        if (!proactiveRemuxInFlight.compareAndSet(false, true)) return

        val itemForSwap = exo.getMediaItemAt(idx)
        val startUriString = uri.toString()

        fun swapInPlaySeekableFile(playFile: File) {
            runOnApplicationLooper {
                try {
                    if (exo.playbackState == Player.STATE_IDLE) {
                        proactiveRemuxInFlight.set(false)
                        return@runOnApplicationLooper
                    }
                    val curIdx = exo.currentMediaItemIndex
                    if (curIdx != idx) {
                        proactiveRemuxInFlight.set(false)
                        return@runOnApplicationLooper
                    }
                    val stillSameUri =
                        exo.getMediaItemAt(curIdx).localConfiguration?.uri?.toString() == startUriString
                    if (!stillSameUri) {
                        PlaybackSeekDiagnostics.log(
                            "proactiveRemux skip: slot uri changed (user switched media)",
                        )
                        proactiveRemuxInFlight.set(false)
                        return@runOnApplicationLooper
                    }
                    if (super.isCurrentMediaItemSeekable()) {
                        proactiveRemuxInFlight.set(false)
                        return@runOnApplicationLooper
                    }
                    val pos = exo.currentPosition
                    val play = exo.playWhenReady
                    @Suppress("DEPRECATION")
                    val newItem = mediaItemWithUri(cloneItemForHardSeek(itemForSwap), Uri.fromFile(playFile))
                    exo.replaceMediaItem(curIdx, newItem)
                    exo.prepare()
                    exo.playWhenReady = play
                    appHandler.post {
                        if (exo.playbackState != Player.STATE_IDLE &&
                            exo.currentMediaItemIndex == curIdx &&
                            exo.getMediaItemAt(curIdx).localConfiguration?.uri?.toString() ==
                            Uri.fromFile(playFile).toString()
                        ) {
                            exo.seekTo(curIdx, pos.coerceAtLeast(0L))
                        }
                        proactiveRemuxInFlight.set(false)
                        PlaybackSeekDiagnostics.log(
                            "proactiveRemux done idx=$curIdx pos=$pos file=${playFile.name}",
                        )
                    }
                } catch (e: Exception) {
                    proactiveRemuxInFlight.set(false)
                    PlaybackSeekDiagnostics.log("proactiveRemux swap error: ${e.message}")
                }
            }
        }

        when {
            uri.scheme.equals("content", ignoreCase = true) -> {
                ContentUriSeekCache.ensureLocalFile(applicationContext, uri) { copyRes ->
                    copyRes.fold(
                        onSuccess = { file ->
                            SeekableMp4Remuxer.ensureRemuxed(applicationContext, file) { mux ->
                                mux.fold(
                                    onSuccess = { play -> swapInPlaySeekableFile(play) },
                                    onFailure = {
                                        proactiveRemuxInFlight.set(false)
                                        PlaybackSeekDiagnostics.log("proactiveRemux remux fail: ${it.message}")
                                    },
                                )
                            }
                        },
                        onFailure = {
                            proactiveRemuxInFlight.set(false)
                            PlaybackSeekDiagnostics.log("proactiveRemux copy fail: ${it.message}")
                        },
                    )
                }
            }
            uri.scheme.equals("file", ignoreCase = true) && !uri.path.isNullOrBlank() -> {
                val f = File(uri.path!!)
                SeekableMp4Remuxer.ensureRemuxed(applicationContext, f) { mux ->
                    mux.fold(
                        onSuccess = { play -> swapInPlaySeekableFile(play) },
                        onFailure = {
                            proactiveRemuxInFlight.set(false)
                            PlaybackSeekDiagnostics.log("proactiveRemux remux fail: ${it.message}")
                        },
                    )
                }
            }
            else -> proactiveRemuxInFlight.set(false)
        }
    }

    private fun beginLocalRelocalizePlayback(
        itemsSnapshot: List<MediaItem>,
        targetIndex: Int,
        pos: Long,
        wasPlaying: Boolean,
        mediaItemIndex: Int,
        originalSeekMs: Long,
    ) {
        val expectedPlaybackUriString =
            itemsSnapshot.getOrNull(targetIndex)?.localConfiguration?.uri?.toString()
        PlaybackSeekDiagnostics.log(
            "localRelocalize beginPlayback items=${itemsSnapshot.size} startMs=$pos idx=$targetIndex wasPlaying=$wasPlaying",
        )
        runCatching {
            exo.stop()
            val boost = LocalRelocalizeSeekBoost(
                exo = exo,
                handler = appHandler,
                mediaItemIndex = targetIndex,
                seekPositionMs = pos,
                expectedPlaybackUriString = expectedPlaybackUriString,
            )
            exo.addListener(boost)
            if (itemsSnapshot.size == 1) {
                exo.setMediaItem(itemsSnapshot[0], pos)
            } else {
                exo.setMediaItems(itemsSnapshot, targetIndex, pos)
            }
            exo.prepare()
            exo.playWhenReady = wasPlaying
        }.onFailure {
            PlaybackSeekDiagnostics.log("localRelocalize failed: ${it.message}")
            super.seekTo(mediaItemIndex, originalSeekMs)
        }
    }

    /**
     * 在 READY 后对 **exo** 直接 seek（勿走 [MetadataDurationSeekableForwardingPlayer.seekTo]），并延迟校验是否仍偏离目标。
     */
    private class LocalRelocalizeSeekBoost(
        private val exo: ExoPlayer,
        private val handler: Handler,
        private val mediaItemIndex: Int,
        private val seekPositionMs: Long,
        /** 重载后当前槽位应对应的播放 URI；换歌后同 index 不同片时不应再 seek。 */
        private val expectedPlaybackUriString: String?,
    ) : Player.Listener {

        @Volatile
        private var readyHandled = false

        private fun slotStillMatches(): Boolean {
            if (mediaItemIndex < 0 || mediaItemIndex >= exo.mediaItemCount) return false
            if (exo.currentMediaItemIndex != mediaItemIndex) return false
            val expected = expectedPlaybackUriString
            if (expected == null) return true
            val actual = exo.getMediaItemAt(mediaItemIndex).localConfiguration?.uri?.toString()
                ?: return false
            return actual == expected
        }

        private val verifyRunnable = Runnable {
            if (!slotStillMatches()) {
                PlaybackSeekDiagnostics.log("localRelocalize verify skip: media slot changed")
                return@Runnable
            }
            val count = exo.mediaItemCount
            if (count <= 0) return@Runnable
            val idx = mediaItemIndex.coerceIn(0, count - 1)
            val cur = exo.currentPosition
            val want = seekPositionMs
            PlaybackSeekDiagnostics.log(
                "localRelocalize +800ms verify cur=$cur want=$want state=${exo.playbackState}",
            )
            if (kotlin.math.abs(cur - want) > 2500L && want > 3000L) {
                PlaybackSeekDiagnostics.log("localRelocalize verify -> exo.seekTo retry")
                exo.seekTo(idx, want)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            handler.removeCallbacks(verifyRunnable)
            if (readyHandled) return
            // 同 index 换源（如 A→A_exo）必须保留；仅当用户已离开目标槽位时取消。
            if (exo.currentMediaItemIndex != mediaItemIndex) {
                readyHandled = true
                exo.removeListener(this)
                PlaybackSeekDiagnostics.log("localRelocalize boost cancelled: left target index")
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY || readyHandled) return
            if (!slotStillMatches()) {
                readyHandled = true
                exo.removeListener(this)
                PlaybackSeekDiagnostics.log("localRelocalize READY skip: media slot changed")
                return
            }
            readyHandled = true
            exo.removeListener(this)
            val count = exo.mediaItemCount
            if (count <= 0) return
            val idx = mediaItemIndex.coerceIn(0, count - 1)
            PlaybackSeekDiagnostics.log(
                "localRelocalize READY -> exo.seekTo($idx,$seekPositionMs) pos=${exo.currentPosition}",
            )
            exo.seekTo(idx, seekPositionMs)
            handler.postDelayed(verifyRunnable, 800L)
        }

        override fun onPlayerError(error: PlaybackException) {
            handler.removeCallbacks(verifyRunnable)
            if (!readyHandled) {
                readyHandled = true
                exo.removeListener(this)
            }
        }
    }

    private fun hardReseekTo(mediaItemIndex: Int, positionMs: Long) {
        val count = exo.mediaItemCount
        if (count <= 0 || mediaItemIndex !in 0 until count) {
            super.seekTo(mediaItemIndex, positionMs)
            return
        }
        val md = metadataDurationMsAt(mediaItemIndex)
        if (md <= 0L || md == C.TIME_UNSET) {
            super.seekTo(mediaItemIndex, positionMs)
            return
        }
        if (!canRebuildQueueForHardReseek()) {
            super.seekTo(mediaItemIndex, positionMs)
            return
        }
        val pos = positionMs.coerceIn(0L, md)
        PlaybackSeekDiagnostics.log(
            "hardReseekTo start index=$mediaItemIndex requestMs=$positionMs clampedMs=$pos metaMs=$md " +
                "playbackState=${exo.playbackState} posBefore=${exo.currentPosition}",
        )
        val itemsSnapshot = List(count) { cloneItemForHardSeek(exo.getMediaItemAt(it)) }
        val wasPlaying = exo.playWhenReady
        val targetIndex = mediaItemIndex
        if (pos <= 0L) {
            runCatching {
                exo.stop()
                if (itemsSnapshot.size == 1) {
                    exo.setMediaItem(itemsSnapshot[0], 0L)
                } else {
                    exo.setMediaItems(itemsSnapshot, targetIndex, 0L)
                }
                exo.prepare()
                exo.playWhenReady = wasPlaying
            }.onFailure {
                super.seekTo(mediaItemIndex, positionMs)
            }
            return
        }

        val expectedUriAtTarget =
            itemsSnapshot.getOrNull(targetIndex)?.localConfiguration?.uri?.toString()
        val seekWhenReady = HardSeekSeekWhenReady(
            exo = exo,
            handler = appHandler,
            mediaItemIndex = targetIndex,
            seekPositionMs = pos,
            expectedMediaUriString = expectedUriAtTarget,
        )
        exo.addListener(seekWhenReady)
        runCatching {
            exo.stop()
            if (itemsSnapshot.size == 1) {
                exo.setMediaItem(itemsSnapshot[0], 0L)
            } else {
                exo.setMediaItems(itemsSnapshot, targetIndex, 0L)
            }
            exo.prepare()
            exo.playWhenReady = wasPlaying
        }.onFailure {
            seekWhenReady.cancel()
            super.seekTo(mediaItemIndex, positionMs)
        }
    }

    /**
     * 在 READY 后对 **exo 本体** seek，避免再次进入 [MetadataDurationSeekableForwardingPlayer.seekTo]。
     */
    private class HardSeekSeekWhenReady(
        private val exo: ExoPlayer,
        private val handler: Handler,
        private val mediaItemIndex: Int,
        private val seekPositionMs: Long,
        private val expectedMediaUriString: String?,
    ) : Player.Listener {

        @Volatile
        private var finished = false

        private fun slotStillMatches(): Boolean {
            if (mediaItemIndex < 0 || mediaItemIndex >= exo.mediaItemCount) return false
            if (exo.currentMediaItemIndex != mediaItemIndex) return false
            val expected = expectedMediaUriString ?: return true
            val actual = exo.getMediaItemAt(mediaItemIndex).localConfiguration?.uri?.toString()
                ?: return false
            return actual == expected
        }

        private val timeoutRunnable: Runnable = object : Runnable {
            override fun run() {
                if (finished) return
                if (!slotStillMatches()) {
                    PlaybackSeekDiagnostics.log("hardReseek timeout skip: media slot changed")
                    cancel()
                    return
                }
                finished = true
                handler.removeCallbacks(this)
                exo.removeListener(this@HardSeekSeekWhenReady)
                val count = exo.mediaItemCount
                if (count > 0) {
                    val idx = mediaItemIndex.coerceIn(0, count - 1)
                    PlaybackSeekDiagnostics.log(
                        "hardReseek READY timeout -> exo.seekTo($idx, $seekPositionMs) " +
                            "state=${exo.playbackState} pos=${exo.currentPosition}",
                    )
                    exo.seekTo(idx, seekPositionMs)
                }
            }
        }

        init {
            handler.postDelayed(timeoutRunnable, 12_000L)
        }

        fun cancel() {
            if (finished) return
            finished = true
            handler.removeCallbacks(timeoutRunnable)
            exo.removeListener(this)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (finished) return
            if (exo.currentMediaItemIndex != mediaItemIndex) {
                PlaybackSeekDiagnostics.log("hardReseek cancel: left target index")
                cancel()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState != Player.STATE_READY || finished) return
            if (!slotStillMatches()) {
                PlaybackSeekDiagnostics.log("hardReseek READY skip: media slot changed")
                cancel()
                return
            }
            finished = true
            handler.removeCallbacks(timeoutRunnable)
            exo.removeListener(this)
            val count = exo.mediaItemCount
            if (count <= 0) return
            val idx = mediaItemIndex.coerceIn(0, count - 1)
            PlaybackSeekDiagnostics.log(
                "hardReseek STATE_READY -> exo.seekTo($idx, $seekPositionMs) " +
                    "posBefore=${exo.currentPosition}",
            )
            exo.seekTo(idx, seekPositionMs)
        }

        override fun onPlayerError(error: PlaybackException) {
            PlaybackSeekDiagnostics.log(
                "hardReseek onPlayerError ${Log.getStackTraceString(error)}",
            )
            cancel()
        }
    }

    override fun seekTo(positionMs: Long) {
        runOnApplicationLooper {
            val idx = currentMediaItemIndex
            logSeekDecision(idx, positionMs, singleArgOverload = true)
            when {
                needsHardReseekForSeek(idx) -> hardReseekTo(idx, positionMs)
                shouldRelocalizeWithStartPosition(idx, positionMs) ->
                    relocalizeLocalSeekWithStartPosition(idx, positionMs)
                else -> {
                    super.seekTo(positionMs)
                    logPostDelegateSeek("seekTo(ms)", positionMs)
                }
            }
        }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        runOnApplicationLooper {
            if (mediaItemIndex != currentMediaItemIndex) {
                PlaybackSeekDiagnostics.log(
                    "seekTo(index=$mediaItemIndex, ms=$positionMs) -> delegate (index!=current)",
                )
                super.seekTo(mediaItemIndex, positionMs)
                return@runOnApplicationLooper
            }
            logSeekDecision(mediaItemIndex, positionMs, singleArgOverload = false)
            when {
                needsHardReseekForSeek(mediaItemIndex) -> hardReseekTo(mediaItemIndex, positionMs)
                shouldRelocalizeWithStartPosition(mediaItemIndex, positionMs) ->
                    relocalizeLocalSeekWithStartPosition(mediaItemIndex, positionMs)
                else -> {
                    super.seekTo(mediaItemIndex, positionMs)
                    logPostDelegateSeek("seekTo(index,ms)", positionMs)
                }
            }
        }
    }

    private fun logSeekDecision(mediaItemIndex: Int, positionMs: Long, singleArgOverload: Boolean) {
        val innerSeekable = super.isCurrentMediaItemSeekable()
        val innerDur = super.getDuration()
        val meta = metadataDurationMsAt(mediaItemIndex)
        val hard = needsHardReseekForSeek(mediaItemIndex)
        val relocalize = shouldRelocalizeWithStartPosition(mediaItemIndex, positionMs)
        val uri = exo.getMediaItemAt(mediaItemIndex).localConfiguration?.uri?.toString().orEmpty()
        val uriShort = if (uri.length > 96) uri.take(96) + "…" else uri
        PlaybackSeekDiagnostics.log(
            "seekTo${if (singleArgOverload) "(ms)" else "(index,ms)"} pos=$positionMs idx=$mediaItemIndex " +
                "innerSeekable=$innerSeekable innerDur=$innerDur metaMs=$meta needsHardReseek=$hard " +
                "localRelocalize=$relocalize localUri=${isLocalPlaybackUri(mediaItemIndex)} " +
                "augmentSeek=${shouldAugmentSeekFromMetadata()} state=${exo.playbackState} " +
                "posNow=${exo.currentPosition} uri=$uriShort",
        )
    }

    private fun logPostDelegateSeek(label: String, positionMs: Long) {
        appHandler.post {
            PlaybackSeekDiagnostics.log(
                "$label delegated innerDur=${super.getDuration()} posAfter=${exo.currentPosition} " +
                    "state=${exo.playbackState} (requestedMs=$positionMs)",
            )
        }
    }

    override fun isCurrentMediaItemSeekable(): Boolean {
        if (super.isCurrentMediaItemSeekable()) return true
        return shouldAugmentSeekFromMetadata()
    }

    override fun getAvailableCommands(): Player.Commands {
        val base = super.getAvailableCommands()
        if (!shouldAugmentSeekFromMetadata()) return base
        val b = base.buildUpon()
        if (!base.contains(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
            b.add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
        }
        if (!base.contains(Player.COMMAND_SEEK_BACK)) {
            b.add(Player.COMMAND_SEEK_BACK)
        }
        if (!base.contains(Player.COMMAND_SEEK_FORWARD)) {
            b.add(Player.COMMAND_SEEK_FORWARD)
        }
        return b.build()
    }

    override fun isCommandAvailable(command: Int): Boolean {
        if (super.isCommandAvailable(command)) return true
        if (!shouldAugmentSeekFromMetadata()) return false
        return when (command) {
            Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
            Player.COMMAND_SEEK_BACK,
            Player.COMMAND_SEEK_FORWARD,
            -> true
            else -> false
        }
    }
}
