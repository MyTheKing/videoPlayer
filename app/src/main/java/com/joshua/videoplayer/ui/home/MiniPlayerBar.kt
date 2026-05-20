package com.joshua.videoplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import com.joshua.videoplayer.player.service.PlaybackSeekDiagnostics
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.formatDurationMs
import com.joshua.videoplayer.ui.media.VideoThumbnailImage
import com.joshua.videoplayer.ui.media.scrubSessionIdentity
import com.joshua.videoplayer.ui.media.uiDurationMs
import com.joshua.videoplayer.ui.media.uiPlaybackPositionMs
import com.joshua.videoplayer.ui.theme.GlassNavBorder
import com.joshua.videoplayer.ui.theme.MiniPlayerBackground
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * 底部「当前播放」卡片：标题/副标题、可拖动进度、切歌；下一首右侧为 **单曲循环 ⟷ 列表顺序循环**（关闭随机）。
 * 仅缩略图点击进入全屏播放页。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayerBar(
    controller: MediaController,
    onExpand: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var title by remember(controller) { mutableStateOf("") }
    var subtitle by remember(controller) { mutableStateOf("") }
    var artUri by remember(controller) { mutableStateOf(controller.currentMediaItem?.localConfiguration?.uri) }
    var isPlaying by remember(controller) { mutableStateOf(controller.playWhenReady) }
    var repeatMode by remember(controller) { mutableIntStateOf(controller.repeatMode) }

    var positionMs by remember(controller) { mutableLongStateOf(controller.uiPlaybackPositionMs()) }
    var durationMs by remember(controller) { mutableLongStateOf(controller.uiDurationMs()) }

    var scrubbing by remember(controller) { mutableStateOf(false) }
    var scrubFraction by remember(controller) { mutableFloatStateOf(0f) }

    val scope = rememberCoroutineScope()
    val settleJobHolder = remember(controller) { mutableStateOf<Job?>(null) }
    val scrubbingRef = rememberUpdatedState(scrubbing)

    DisposableEffect(controller) {
        fun syncMetadata() {
            val item: MediaItem? = controller.currentMediaItem
            val meta = item?.mediaMetadata
            artUri = item?.localConfiguration?.uri
            title = meta?.title?.toString().orEmpty()
                .ifBlank { item?.mediaId.orEmpty() }
                .ifBlank { context.getString(R.string.notification_playing) }
            subtitle = meta?.artist?.toString().orEmpty()
                .ifBlank { meta?.albumTitle?.toString().orEmpty() }
            isPlaying = controller.playWhenReady
            repeatMode = controller.repeatMode
            durationMs = controller.uiDurationMs()
            if (!scrubbingRef.value) {
                positionMs = controller.uiPlaybackPositionMs()
            }
        }
        syncMetadata()
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                settleJobHolder.value?.cancel()
                settleJobHolder.value = null
                scrubbing = false
                syncMetadata()
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onPlaybackStateChanged(state: Int) {
                durationMs = controller.uiDurationMs()
                if (!scrubbingRef.value) {
                    positionMs = controller.uiPlaybackPositionMs()
                }
                // 播放器状态变化时同步播放/暂停状态
                isPlaying = controller.playWhenReady && controller.isPlaying
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                durationMs = controller.uiDurationMs()
                if (!scrubbingRef.value) {
                    positionMs = controller.uiPlaybackPositionMs()
                }
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

    val miniPlayerShape = RoundedCornerShape(18.dp)
    val miniShadowColor = MaterialTheme.colorScheme.scrim
    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .shadow(
                elevation = 10.dp,
                shape = miniPlayerShape,
                ambientColor = miniShadowColor.copy(alpha = 0.2f),
                spotColor = miniShadowColor.copy(alpha = 0.26f),
            )
            .clip(miniPlayerShape)
            .background(MiniPlayerBackground)
            .border(1.dp, GlassNavBorder, miniPlayerShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onExpand),
            ) {
                val uri = artUri
                if (uri != null) {
                    VideoThumbnailImage(
                        videoUri = uri,
                        contentScale = ContentScale.Crop,
                        requestWidth = 256,
                        requestHeight = 256,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = stringResource(R.string.cd_previous),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = {
                    if (controller.playWhenReady) controller.pause() else controller.play()
                },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_play_pause),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = stringResource(R.string.cd_next),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            val singleRepeat = repeatMode == Player.REPEAT_MODE_ONE
            IconButton(
                onClick = {
                    controller.setShuffleModeEnabled(false)
                    if (singleRepeat) {
                        controller.setRepeatMode(Player.REPEAT_MODE_ALL)
                    } else {
                        controller.setRepeatMode(Player.REPEAT_MODE_ONE)
                    }
                },
            ) {
                Icon(
                    imageVector = if (singleRepeat) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    contentDescription = stringResource(R.string.cd_repeat_toggle),
                    tint = if (singleRepeat) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
        val miniSliderInteractionSource = remember { MutableInteractionSource() }
        val miniSliderColors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 4.dp),
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
                            "SCRUB_END screen=mini targetMs=$target uiDurationMs=$durationMs " +
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
                interactionSource = miniSliderInteractionSource,
                colors = miniSliderColors,
                thumb = {
                    Box(modifier = Modifier.offset(y = 2.dp)) {
                        SliderDefaults.Thumb(
                            interactionSource = miniSliderInteractionSource,
                            colors = miniSliderColors,
                            enabled = durationValid,
                            thumbSize = DpSize(10.dp, 10.dp),
                        )
                    }
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        modifier = Modifier.height(3.dp),
                        colors = miniSliderColors,
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
}
