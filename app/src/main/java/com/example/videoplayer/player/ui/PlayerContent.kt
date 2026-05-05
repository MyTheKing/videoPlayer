package com.example.videoplayer.player.ui

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.videoplayer.R
import com.example.videoplayer.player.service.MediaPlaybackService
import com.example.videoplayer.ui.theme.GlassBarTint
import com.example.videoplayer.ui.theme.PlayerGradientBottom
import com.example.videoplayer.ui.theme.PlayerGradientTop

/**
 * 使用 MediaController 显示播放画面与声音；不持 ExoPlayer，仅消费 Controller（实现 Player）。
 *
 * @param playUri 待播媒体 URI，非 null 时启动服务并连接 Controller
 * @param onPlaybackError 播放失败（如路径失效）时回调，由调用方提供「重新选择」等重试入口
 * @param modifier 用于外层布局
 */
@Composable
fun PlayerContent(
    playUri: Uri?,
    onPlaybackError: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var controller by remember { mutableStateOf<MediaController?>(null) }

    LaunchedEffect(playUri) {
        if (playUri == null) return@LaunchedEffect
        context.startForegroundService(Intent(context, MediaPlaybackService::class.java).setData(playUri))
        val token = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                try {
                    controller = future.get()
                } catch (_: Exception) {}
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    DisposableEffect(controller) {
        onDispose {
            controller?.release()
        }
    }

    controller?.let { c ->
        var repeatMode by remember(c) { mutableIntStateOf(c.repeatMode) }
        var shuffleEnabled by remember(c) { mutableStateOf(c.shuffleModeEnabled) }
        var playbackError by remember { mutableStateOf(false) }
        DisposableEffect(c) {
            val listener = object : Player.Listener {
                override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
                override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleEnabled = enabled }
                override fun onPlayerError(error: PlaybackException) {
                    playbackError = true
                }
            }
            c.addListener(listener)
            onDispose { c.removeListener(listener) }
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(PlayerGradientTop, PlayerGradientBottom)))
        ) {
            Column(Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply { player = c }
                    },
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = GlassBarTint
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val listActive = !shuffleEnabled && repeatMode == Player.REPEAT_MODE_ALL
                        FilterChip(
                            selected = listActive,
                            onClick = {
                                c.setShuffleModeEnabled(false)
                                c.setRepeatMode(Player.REPEAT_MODE_ALL)
                            },
                            label = { Text(stringResource(R.string.repeat_mode_list)) }
                        )
                        val oneActive = !shuffleEnabled && repeatMode == Player.REPEAT_MODE_ONE
                        FilterChip(
                            selected = oneActive,
                            onClick = {
                                c.setShuffleModeEnabled(false)
                                c.setRepeatMode(Player.REPEAT_MODE_ONE)
                            },
                            label = { Text(stringResource(R.string.repeat_mode_one)) }
                        )
                        FilterChip(
                            selected = shuffleEnabled,
                            onClick = {
                                c.setShuffleModeEnabled(true)
                                c.setRepeatMode(Player.REPEAT_MODE_ALL)
                            },
                            label = { Text(stringResource(R.string.repeat_mode_shuffle)) }
                        )
                    }
                }
            }
            if (playbackError && onPlaybackError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                stringResource(R.string.error_playback_failed),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Button(
                                onClick = onPlaybackError,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.retry_pick_video))
                            }
                        }
                    }
                }
            }
        }
    }
}
