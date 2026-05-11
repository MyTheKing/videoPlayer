package com.example.videoplayer.ui.media

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.example.videoplayer.player.service.PlaybackSeekDiagnostics
import com.example.videoplayer.ui.formatDurationMs
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * 迷你条 / 全屏共用：只要 [MediaController.getDuration] 有效（含会话内 ForwardingPlayer 已做的补齐），
 * **一律采用播放器时长**，不再用 [MediaItem.mediaMetadata.durationMs] 覆盖。
 * 否则歌单/探测写入的误差会压过 Exo，把本来正常的总时长显示乱、拖动目标也算错。
 * 仅当播放器时长仍无效时才退回元数据。
 */
/** 当前项若带裁剪起点，[MediaController.getCurrentPosition] 从窗口 0 计，需加上裁剪起点才是文件内绝对时间。 */
fun MediaItem?.clippingStartMs(): Long {
    if (this == null) return 0L
    val us = clippingConfiguration.startPositionUs
    if (us <= 0L || us == C.TIME_UNSET) return 0L
    return us / 1000L
}

fun MediaController.uiPlaybackPositionMs(): Long {
    val start = currentMediaItem.clippingStartMs()
    return if (start > 0L) start + currentPosition else currentPosition
}

fun MediaController.uiDurationMs(): Long {
    val playerDur = duration
    if (playerDur > 0L && playerDur != C.TIME_UNSET) return playerDur
    val metaDur = currentMediaItem?.mediaMetadata?.durationMs ?: C.TIME_UNSET
    return if (metaDur > 0L && metaDur != C.TIME_UNSET) metaDur else playerDur
}

/** 区分「本次拖动 settle」是否仍针对当前媒体，换片后不得再用旧 target 对齐 UI。 */
fun MediaController.scrubSessionIdentity(): Pair<String?, String?> =
    currentMediaItem?.mediaId to currentMediaItem?.localConfiguration?.uri?.toString()

/**
 * 与 [com.example.videoplayer.ui.home.MiniPlayerBar] 进度条逻辑一致，供全屏页使用。
 * （[androidx.media3.ui.PlayerView] 内置条在 Timeline 窗口时长为 [C.TIME_UNSET] 时不会回退到元数据，且 [MediaController] 非 Exo 时 scrub 行为异常。）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaControllerScrubber(
    controller: MediaController,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember(controller) { mutableLongStateOf(controller.uiPlaybackPositionMs()) }
    var durationMs by remember(controller) { mutableLongStateOf(controller.uiDurationMs()) }
    var scrubbing by remember(controller) { mutableStateOf(false) }
    var scrubFraction by remember(controller) { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    val settleJobHolder = remember(controller) { mutableStateOf<Job?>(null) }
    val scrubbingRef = rememberUpdatedState(scrubbing)

    DisposableEffect(controller) {
        fun sync() {
            durationMs = controller.uiDurationMs()
            if (!scrubbingRef.value) {
                positionMs = controller.uiPlaybackPositionMs()
            }
        }
        sync()
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                settleJobHolder.value?.cancel()
                settleJobHolder.value = null
                scrubbing = false
                sync()
            }

            override fun onPlaybackStateChanged(state: Int) {
                sync()
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                sync()
            }
        }
        controller.addListener(listener)
        onDispose {
            settleJobHolder.value?.cancel()
            controller.removeListener(listener)
        }
    }

    val scrubbingState = rememberUpdatedState(scrubbing)
    LaunchedEffect(controller) {
        while (coroutineContext.isActive) {
            if (!scrubbingState.value) {
                positionMs = controller.uiPlaybackPositionMs()
                durationMs = controller.uiDurationMs()
            }
            delay(500)
        }
    }

    val durationValid = durationMs > 0L && durationMs != C.TIME_UNSET
    val progress = if (durationValid) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val displayProgress = if (scrubbing) scrubFraction else progress
    val displayedPositionMs = when {
        !durationValid -> positionMs
        scrubbing -> (scrubFraction * durationMs.toDouble()).toLong().coerceIn(0L, durationMs)
        else -> positionMs
    }

    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatDurationMs(displayedPositionMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = displayProgress,
            onValueChange = { newValue ->
                if (!durationValid) return@Slider
                settleJobHolder.value?.cancel()
                settleJobHolder.value = null
                if (!scrubbing) {
                    scrubFraction = progress
                }
                scrubbing = true
                scrubFraction = newValue.coerceIn(0f, 1f)
            },
            onValueChangeFinished = {
                if (durationValid && scrubbing) {
                    val target = (scrubFraction * durationMs.toDouble()).toLong().coerceIn(0L, durationMs)
                    val posBefore = controller.currentPosition
                    val ctlDur = controller.duration
                    val meta = controller.currentMediaItem?.mediaMetadata?.durationMs ?: C.TIME_UNSET
                    PlaybackSeekDiagnostics.log(
                        "SCRUB_END screen=fullscreen targetMs=$target uiDurationMs=$durationMs " +
                            "controller.duration=$ctlDur posBefore=$posBefore metaMs=$meta",
                    )
                    controller.seekTo(target)
                    val identityAtSeek = controller.scrubSessionIdentity()
                    settleJobHolder.value?.cancel()
                    settleJobHolder.value = scope.launch {
                        try {
                            for (i in 0 until 150) {
                                if (!coroutineContext.isActive) break
                                if (controller.scrubSessionIdentity() != identityAtSeek) break
                                delay(85)
                                val cur = controller.uiPlaybackPositionMs()
                                if (abs(cur - target) < 800L) break
                            }
                        } finally {
                            scrubbing = false
                            positionMs = controller.uiPlaybackPositionMs()
                            durationMs = controller.uiDurationMs()
                            settleJobHolder.value = null
                        }
                    }
                } else {
                    scrubbing = false
                }
            },
            enabled = durationValid,
            modifier = Modifier
                .weight(1f)
                .height(20.dp),
            valueRange = 0f..1f,
            interactionSource = interactionSource,
            colors = colors,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    enabled = durationValid,
                    thumbSize = DpSize(12.dp, 12.dp),
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(3.dp),
                    colors = colors,
                    enabled = durationValid,
                    thumbTrackGapSize = 2.dp,
                )
            },
        )
        Text(
            text = formatDurationMs(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
