package com.example.videoplayer.data

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.inspector.MetadataRetriever
import com.example.videoplayer.data.local.PlaylistItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * MediaStore 中 [android.provider.MediaStore.Video.Media.DURATION] 常为空或 0，
 * 此处用 [MetadataRetriever]（Media3，与 ExoPlayer 同源解析）、[MediaMetadataRetriever]、[MediaExtractor]
 * 在后台补全时长（单次刷新有数量上限，避免扫库卡死）。
 */
suspend fun probeVideoDurationMs(context: Context, video: LocalVideo): Long = withContext(Dispatchers.IO) {
    val resolvedSize = maxOf(video.sizeBytes, openableDeclaredLengthBytes(context, video.contentUri))
    probeVideoDurationCore(context, video.contentUri, resolvedSize, video.absolutePath)
}

/** MediaStore 的 SIZE 常为 0 时，[AssetFileDescriptor.getLength] 仍可能给出可读长度（利于 FD 定长解析）。 */
private fun openableDeclaredLengthBytes(context: Context, uri: Uri): Long {
    return runCatching {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
            val len = afd.length
            if (len > 0L && len != AssetFileDescriptor.UNKNOWN_LENGTH) len else 0L
        } ?: 0L
    }.getOrDefault(0L)
}

private fun extractDurationMs(r: MediaMetadataRetriever): Long =
    r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L

/** 从各轨道取最大 [MediaFormat.KEY_DURATION]（微秒），兼容仅有音频轨带时长等情况。 */
private fun maxDurationMsFromExtractor(extractor: MediaExtractor): Long {
    var maxUs = 0L
    for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        if (format.containsKey(MediaFormat.KEY_DURATION)) {
            maxUs = maxOf(maxUs, format.getLong(MediaFormat.KEY_DURATION))
        }
    }
    return if (maxUs > 0L) maxUs / 1000L else 0L
}

@Suppress("TooGenericExceptionCaught")
private fun probeWithExtractorUri(context: Context, uri: Uri): Long {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, uri, null)
        return maxDurationMsFromExtractor(extractor)
    } catch (_: Exception) {
        return 0L
    } finally {
        runCatching { extractor.release() }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun probeWithExtractorFd(context: Context, uri: Uri, sizeHintBytes: Long): Long {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val len = when {
                pfd.statSize > 0 -> pfd.statSize
                sizeHintBytes > 0 -> sizeHintBytes
                else -> 0L
            }
            val extractor = MediaExtractor()
            try {
                if (len > 0L) {
                    extractor.setDataSource(pfd.fileDescriptor, 0, len)
                } else {
                    extractor.setDataSource(pfd.fileDescriptor)
                }
                maxDurationMsFromExtractor(extractor)
            } catch (_: Exception) {
                0L
            } finally {
                runCatching { extractor.release() }
            }
        } ?: 0L
    }.getOrDefault(0L)
}

/**
 * 部分机型（如部分小米）上必须用带 length 的 [MediaMetadataRetriever.setDataSource]，
 * 长度取自 [ParcelFileDescriptor.statSize] 或 MediaStore [LocalVideo.sizeBytes]。
 */
@Suppress("TooGenericExceptionCaught")
private fun probeWithRetrieverFd(context: Context, uri: Uri, sizeHintBytes: Long): Long {
    return runCatching {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val len = when {
                pfd.statSize > 0 -> pfd.statSize
                sizeHintBytes > 0 -> sizeHintBytes
                else -> 0L
            }
            val r = MediaMetadataRetriever()
            try {
                if (len > 0L) {
                    r.setDataSource(pfd.fileDescriptor, 0, len)
                } else {
                    r.setDataSource(pfd.fileDescriptor)
                }
                extractDurationMs(r)
            } finally {
                runCatching { r.release() }
            }
        } ?: 0L
    }.getOrDefault(0L)
}

@Suppress("TooGenericExceptionCaught")
private fun probeWithRetrieverPath(path: String): Long {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(path)
        return extractDurationMs(r)
    } catch (_: Exception) {
        return 0L
    } finally {
        runCatching { r.release() }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun probeWithExtractorPath(path: String): Long {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(path)
        return maxDurationMsFromExtractor(extractor)
    } catch (_: Exception) {
        return 0L
    } finally {
        runCatching { extractor.release() }
    }
}

/**
 * 官方推荐用 Media3 [MetadataRetriever] 拉时长（微秒），与播放器支持格式一致；
 * 见 [检索媒体元数据](https://developer.android.com/media/media3/inspector/retrieve-metadata)。
 */
@Suppress("TooGenericExceptionCaught")
private fun probeWithMedia3Inspector(context: Context, item: MediaItem): Long {
    val appCtx = context.applicationContext
    return runCatching {
        MetadataRetriever.Builder(appCtx, item).build().use { retriever ->
            val future = retriever.retrieveDurationUs()
            val us = try {
                future.get(15, TimeUnit.SECONDS)
            } catch (_: TimeoutException) {
                future.cancel(true)
                return@use 0L
            } catch (_: ExecutionException) {
                return@use 0L
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                future.cancel(true)
                return@use 0L
            }
            when {
                us == C.TIME_UNSET || us <= 0L -> 0L
                else -> us / 1000L
            }
        }
    }.getOrDefault(0L)
}

@Suppress("TooGenericExceptionCaught")
private fun probeWithRetrieverUri(context: Context, uri: Uri): Long {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(context, uri)
        return extractDurationMs(r)
    } catch (_: Exception) {
        return 0L
    } finally {
        runCatching { r.release() }
    }
}

/**
 * 与真实播放相同的 [ExoPlayer] 准备流程，部分文件仅在此阶段才能拿到 [Player.getDuration]。
 */
@Suppress("TooGenericExceptionCaught")
private fun probeWithExoPlayer(context: Context, uri: Uri): Long {
    val player = ExoPlayer.Builder(context.applicationContext).build()
    return try {
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        val deadline = System.currentTimeMillis() + 12_000L
        while (System.currentTimeMillis() < deadline) {
            when (player.playbackState) {
                Player.STATE_READY -> {
                    val d = player.duration
                    if (d > 0L && d != C.TIME_UNSET) return d
                }
                Player.STATE_IDLE -> return 0L
                else -> { }
            }
            Thread.sleep(40)
        }
        val d = player.duration
        if (d > 0L && d != C.TIME_UNSET) d else 0L
    } catch (_: Exception) {
        0L
    } finally {
        runCatching { player.release() }
    }
}

/**
 * 按兼容性叠加策略：
 * 可读绝对路径（框架）→ 同路径 Media3 → content [Uri] 的 Media3 → 框架 URI/FD → [ExoPlayer]。
 */
@Suppress("TooGenericExceptionCaught")
private fun probeVideoDurationCore(
    context: Context,
    uri: Uri,
    sizeHintBytes: Long,
    absolutePath: String?,
): Long {
    val sizeResolved = maxOf(sizeHintBytes, openableDeclaredLengthBytes(context, uri))
    absolutePath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { f -> f.isFile && f.canRead() }
        ?.let { f ->
            val p = f.absolutePath
            probeWithRetrieverPath(p).takeIf { it > 0L }?.let { return it }
            probeWithExtractorPath(p).takeIf { it > 0L }?.let { return it }
            probeWithMedia3Inspector(context, MediaItem.fromUri(f.toURI().toString()))
                .takeIf { it > 0L }?.let { return it }
            Fmp4DurationProbe.durationMsFromFilePath(p).takeIf { it > 0L }?.let { return it }
        }

    probeWithMedia3Inspector(context, MediaItem.fromUri(uri)).takeIf { it > 0L }?.let { return it }

    probeWithRetrieverUri(context, uri).takeIf { it > 0L }?.let { return it }
    probeWithRetrieverFd(context, uri, sizeResolved).takeIf { it > 0L }?.let { return it }
    probeWithExtractorUri(context, uri).takeIf { it > 0L }?.let { return it }
    probeWithExtractorFd(context, uri, sizeResolved).takeIf { it > 0L }?.let { return it }
    Fmp4DurationProbe.durationMsFromUri(context, uri).takeIf { it > 0L }?.let { return it }
    probeWithExoPlayer(context, uri).takeIf { it > 0L }?.let { return it }
    return 0L
}

/**
 * 为歌单中 [PlaylistItemEntity.durationMs] 为 0 的条目探测时长并 [onPersist] 写回（并行受 [probeConcurrency] 限制）。
 */
suspend fun probeAndPersistPlaylistZeroDurations(
    context: Context,
    items: List<PlaylistItemEntity>,
    onPersist: suspend (itemId: Long, durationMs: Long) -> Unit,
) = coroutineScope {
    val targets = items.filter { it.durationMs <= 0L }
    targets.map { item ->
        async(Dispatchers.IO) {
            probeConcurrency.withPermit {
                val uri = Uri.parse(item.contentUri)
                val d = probeVideoDurationCore(
                    context,
                    uri,
                    sizeHintBytes = openableDeclaredLengthBytes(context, uri),
                    absolutePath = null,
                )
                if (d > 0L) onPersist(item.id, d)
            }
        }
    }.awaitAll()
}

private const val MAX_PROBE_PER_REFRESH = 180

/** 并行探测上限，避免部分 ROM 上同时打开过多提取器导致全失败。 */
private val probeConcurrency = Semaphore(8)

private fun <T> List<T>.takeCircularFrom(start: Int, count: Int): List<T> {
    if (isEmpty() || count <= 0) return emptyList()
    val n = size
    val s = ((start % n) + n) % n
    val firstLen = n - s
    return if (firstLen >= count) {
        drop(s).take(count)
    } else {
        drop(s) + take(count - firstLen)
    }
}

/**
 * 为列表中时长为 0 的条目探测真实时长（并行限制在 [MAX_PROBE_PER_REFRESH] 条内）。
 *
 * [rotateStart] 为累计偏移（由调用方持久递增），按 [LocalVideo.contentUri] 排序后做环形取 batch。
 * 必须用 **contentUri 字符串** 关联探测结果：[_ID] 在不同存储卷上会重复，用 id 会导致结果被错误覆盖。
 *
 * 返回更新后的列表与下一轮的 [rotateStart]（当前值 + 本批尝试条数）。
 */
suspend fun List<LocalVideo>.withProbedDurations(
    context: Context,
    rotateStart: Int = 0,
): Pair<List<LocalVideo>, Int> {
    val needProbe = filter { it.durationMs <= 0L }.sortedBy { it.contentUri.toString() }
    if (needProbe.isEmpty()) return this to 0
    val start = ((rotateStart % needProbe.size) + needProbe.size) % needProbe.size
    val batch = needProbe.takeCircularFrom(start, MAX_PROBE_PER_REFRESH)
    val uriToDuration = coroutineScope {
        batch.map { v ->
            async(Dispatchers.IO) {
                probeConcurrency.withPermit {
                    v.contentUri.toString() to probeVideoDurationMs(context, v)
                }
            }
        }.awaitAll().toMap()
    }
    val updated = map { v ->
        val d = uriToDuration[v.contentUri.toString()]
        if (d != null && d > 0L) v.copy(durationMs = d) else v
    }
    return updated to (rotateStart + batch.size)
}
