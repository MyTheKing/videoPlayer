package com.example.videoplayer.player.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.videoplayer.R

/**
 * 托管 MediaSession + ExoPlayer 的媒体前台服务；
 * ExoPlayer 仅由本服务持有，不暴露给 UI；播放控制经 MediaController 连接本 Session。
 *
 * @see MediaSession
 * @see MediaSessionService
 */
class MediaPlaybackService : MediaSessionService() {

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.data?.let { uri ->
            exoPlayer?.let { player ->
                startForeground(NOTIF_ID, buildMinimalNotification())
                val title = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: getString(R.string.notification_playing)
                val item = MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
                    .build()
                player.setMediaItem(item)
                player.prepare()
                player.playWhenReady = true
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_media), NotificationManager.IMPORTANCE_LOW)
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
        val player = ExoPlayer.Builder(this).build()
        exoPlayer = player
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        exoPlayer?.release()
        exoPlayer = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1
        private const val CHANNEL_ID = "media_playback"
    }
}
