package com.example.videoplayer.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.videoplayer.R
import com.example.videoplayer.ui.media.loadVideoThumbnail
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 托管 MediaSession + ExoPlayer 的媒体前台服务；
 * ExoPlayer 仅由本服务持有；支持单文件 [Intent.getData] 或队列 [ACTION_PLAY_QUEUE]。
 *
 * 通知样式由 [DefaultMediaNotificationProvider] 生成，贴近系统媒体控件（参考 stitch 媒体通知稿）。
 */
class MediaPlaybackService : MediaSessionService() {

    /** 真实 Exo 实例，仅用于 [release]（外层 [MetadataDurationSeekableForwardingPlayer] 不负责释放）。 */
    private var exoPlayer: ExoPlayer? = null
    /** 交给 [MediaSession] 与 [onStartCommand] 的 [Player]（含时长/seek 能力修补）。 */
    private var sessionPlayer: Player? = null
    private var mediaSession: MediaSession? = null

    private val artworkJobParent = SupervisorJob()
    private val artworkScope = CoroutineScope(artworkJobParent + Dispatchers.Main.immediate)
    private var artworkLoadJob: Job? = null

    private val artworkListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val p = sessionPlayer ?: return
            scheduleArtworkForCurrentItem(p)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 必须调用 super：Media3 在此完成会话与客户端绑定等内部状态；省略会导致 MediaController 连不上，底部迷你条永远不出现。
        val sticky = super.onStartCommand(intent, flags, startId)
        val player = sessionPlayer ?: return sticky
        when (intent?.action) {
            ACTION_PLAY_QUEUE -> {
                val uriStrings = intent.getStringArrayListExtra(EXTRA_URI_STRINGS) ?: return START_STICKY
                if (uriStrings.isEmpty()) return START_STICKY
                val titles = intent.getStringArrayListExtra(EXTRA_TITLES) ?: arrayListOf()
                val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0).coerceIn(0, uriStrings.size - 1)
                startForeground(NOTIF_ID, buildMinimalNotification())
                val durs = intent.getLongArrayExtra(EXTRA_DURATIONS_MS)
                val items = uriStrings.mapIndexed { index, uriStr ->
                    val title = titles.getOrNull(index)?.takeIf { it.isNotBlank() }
                        ?: uriStr.toUri().lastPathSegment?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.notification_playing)
                    val dur = durs?.getOrNull(index)?.takeIf { it > 0L } ?: 0L
                    val meta = MediaMetadata.Builder().setTitle(title).apply {
                        if (dur > 0L) setDurationMs(dur)
                    }.build()
                    MediaItem.Builder()
                        .setUri(uriStr.toUri())
                        .setMediaMetadata(meta)
                        .build()
                }
                player.setMediaItems(items, startIndex, C.TIME_UNSET)
                val shuffle = intent.getBooleanExtra(EXTRA_SHUFFLE_ENABLED, false)
                val repeat = intent.getIntExtra(EXTRA_REPEAT_MODE, Player.REPEAT_MODE_ALL)
                player.shuffleModeEnabled = shuffle
                player.repeatMode = repeat
                player.prepare()
                player.playWhenReady = true
                scheduleArtworkForCurrentItem(player)
            }
            else -> {
                intent?.data?.let { uri ->
                    startForeground(NOTIF_ID, buildMinimalNotification())
                    val title = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: getString(R.string.notification_playing)
                    val item = MediaItem.Builder()
                        .setUri(uri)
                        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                        .build()
                    player.setMediaItem(item)
                    player.prepare()
                    player.playWhenReady = true
                    scheduleArtworkForCurrentItem(player)
                }
            }
        }
        return sticky
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_media),
                NotificationManager.IMPORTANCE_LOW,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildMinimalNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
        val inner = ExoPlayer.Builder(this)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
            .also {
                // 日常主场景：列表顺序循环；可在 UI 切换单曲/随机
                it.repeatMode = Player.REPEAT_MODE_ALL
            }
        exoPlayer = inner
        sessionPlayer = MetadataDurationSeekableForwardingPlayer(this, inner)
        sessionPlayer!!.addListener(artworkListener)
        mediaSession = MediaSession.Builder(this, sessionPlayer!!).build()
    }

    /**
     * 将当前条目的视频帧写入 [MediaMetadata.artworkData]，供系统媒体通知 / 控制中心显示封面。
     * 已有封面时不重复拉取，避免 [Player.replaceMediaItem] 再次触发 transition 形成循环。
     */
    private fun scheduleArtworkForCurrentItem(player: Player) {
        artworkLoadJob?.cancel()
        val item = player.currentMediaItem ?: return
        if (item.mediaMetadata.artworkData != null) return
        val uri = item.localConfiguration?.uri ?: return
        val index = player.currentMediaItemIndex
        if (index == C.INDEX_UNSET) return

        artworkLoadJob = artworkScope.launch {
            val bmp = loadVideoThumbnail(this@MediaPlaybackService, uri, 512, 512) ?: return@launch
            val jpeg = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { os ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 82, os)
                    os.toByteArray()
                }
            }
            bmp.recycle()

            if (player.currentMediaItemIndex != index) return@launch
            val now = player.currentMediaItem ?: return@launch
            if (now.localConfiguration?.uri != uri) return@launch
            if (now.mediaMetadata.artworkData != null) return@launch

            val at = player.getMediaItemAt(index)
            if (at.localConfiguration?.uri != uri) return@launch

            val newMeta = at.mediaMetadata.buildUpon()
                .setArtworkData(jpeg, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
            val newItem = at.buildUpon().setMediaMetadata(newMeta).build()
            player.replaceMediaItem(index, newItem)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        artworkLoadJob?.cancel()
        artworkJobParent.cancel()
        sessionPlayer?.removeListener(artworkListener)
        mediaSession?.release()
        mediaSession = null
        sessionPlayer = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "media_playback"

        const val ACTION_PLAY_QUEUE = "com.example.videoplayer.action.PLAY_QUEUE"
        const val EXTRA_URI_STRINGS = "extra_uri_strings"
        const val EXTRA_TITLES = "extra_titles"
        const val EXTRA_DURATIONS_MS = "extra_durations_ms"
        const val EXTRA_START_INDEX = "extra_start_index"
        const val EXTRA_SHUFFLE_ENABLED = "extra_shuffle_enabled"
        const val EXTRA_REPEAT_MODE = "extra_repeat_mode"

        /**
         * 以前台服务方式提交完整队列并开始播放（由 UI 在发起 [MediaController] 连接前调用）。
         */
        fun startQueue(
            context: Context,
            uriStrings: List<String>,
            titles: List<String>,
            startIndex: Int,
            durationMs: List<Long> = emptyList(),
            shuffleEnabled: Boolean = false,
            repeatMode: Int = Player.REPEAT_MODE_ALL,
        ) {
            val durs = when {
                durationMs.size == uriStrings.size -> durationMs.map { it.coerceAtLeast(0L) }.toLongArray()
                else -> LongArray(uriStrings.size) { 0L }
            }
            val i = Intent(context, MediaPlaybackService::class.java).apply {
                action = ACTION_PLAY_QUEUE
                putStringArrayListExtra(EXTRA_URI_STRINGS, ArrayList(uriStrings))
                putStringArrayListExtra(EXTRA_TITLES, ArrayList(titles))
                putExtra(EXTRA_DURATIONS_MS, durs)
                putExtra(EXTRA_START_INDEX, startIndex)
                putExtra(EXTRA_SHUFFLE_ENABLED, shuffleEnabled)
                putExtra(EXTRA_REPEAT_MODE, repeatMode)
            }
            ContextCompat.startForegroundService(context, i)
        }
    }
}
