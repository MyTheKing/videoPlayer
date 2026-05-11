package com.example.videoplayer.player.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import com.example.videoplayer.R
import com.example.videoplayer.ui.media.MediaControllerScrubber
import com.example.videoplayer.ui.theme.GlassBarTint
import com.example.videoplayer.ui.theme.PlayerGradientBottom
import com.example.videoplayer.ui.theme.PlayerGradientTop

/**
 * 全屏播放页：使用已连接的 [MediaController] 绑定 [PlayerView]；底部为列表循环/单曲循环，不负责建立 Session。
 *
 * @param controller 由 [com.example.videoplayer.playback.PlaybackConnector] 注入
 * @param onClose 关闭全屏（返回主导航）
 * @param onPlaybackError 媒体错误时回调（如文件被删）
 */
@Composable
fun PlayerContent(
    controller: MediaController?,
    onClose: () -> Unit,
    onPlaybackError: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (controller == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.player_connecting), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    var shuffleEnabled by remember(controller) { mutableStateOf(controller.shuffleModeEnabled) }
    var repeatMode by remember(controller) { mutableIntStateOf(controller.repeatMode) }
    var isPlaying by remember(controller) { mutableStateOf(controller.playWhenReady) }
    var playbackError by remember { mutableStateOf(false) }

    /** 先解除 [PlayerView] 与 [controller] 再离开全屏，避免 Surface 已销毁仍向 MediaCodec 送帧。 */
    val playerViewHolder = remember { mutableStateOf<PlayerView?>(null) }
    fun detachPlayerViewFromController() {
        playerViewHolder.value?.player = null
    }

    BackHandler {
        detachPlayerViewFromController()
        onClose()
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                shuffleEnabled = enabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = true
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PlayerGradientTop, PlayerGradientBottom))),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        detachPlayerViewFromController()
                        onClose()
                    },
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
                Text(
                    text = stringResource(R.string.now_playing),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        // MediaController 不是 ExoPlayer：内置条在窗口时长未知时不会用元数据，拖动常无效。
                        useController = false
                        player = controller
                        playerViewHolder.value = this
                    }
                },
                modifier = Modifier.weight(1f),
                update = { pv ->
                    pv.useController = false
                    pv.player = controller
                    playerViewHolder.value = pv
                },
                onRelease = { pv ->
                    pv.player = null
                    if (playerViewHolder.value === pv) {
                        playerViewHolder.value = null
                    }
                },
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = GlassBarTint,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MediaControllerScrubber(
                        controller = controller,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { controller.seekToPrevious() },
                            enabled = controller.currentTimeline.windowCount > 0,
                        ) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.cd_previous))
                        }
                        IconButton(
                            onClick = {
                                if (controller.playWhenReady) controller.pause() else controller.play()
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play_pause),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        IconButton(
                            onClick = { controller.seekToNext() },
                            enabled = controller.currentTimeline.windowCount > 0,
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.cd_next))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val listSelected =
                            repeatMode != Player.REPEAT_MODE_ONE && !shuffleEnabled
                        val oneSelected =
                            repeatMode == Player.REPEAT_MODE_ONE && !shuffleEnabled
                        FilterChip(
                            selected = listSelected,
                            onClick = {
                                controller.setShuffleModeEnabled(false)
                                controller.setRepeatMode(Player.REPEAT_MODE_ALL)
                            },
                            label = { Text(stringResource(R.string.player_repeat_list_chip)) },
                            leadingIcon = if (listSelected) {
                                {
                                    Icon(
                                        Icons.Filled.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                        FilterChip(
                            selected = oneSelected,
                            onClick = {
                                controller.setShuffleModeEnabled(false)
                                controller.setRepeatMode(Player.REPEAT_MODE_ONE)
                            },
                            label = { Text(stringResource(R.string.player_repeat_one_chip)) },
                            leadingIcon = if (oneSelected) {
                                {
                                    Icon(
                                        Icons.Filled.RepeatOne,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
        if (playbackError && onPlaybackError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            stringResource(R.string.error_playback_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Button(
                            onClick = onPlaybackError,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.retry_pick_video))
                        }
                    }
                }
            }
        }
    }
}
