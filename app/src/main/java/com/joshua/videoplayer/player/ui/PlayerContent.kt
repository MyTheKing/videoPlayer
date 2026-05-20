package com.joshua.videoplayer.player.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.ui.PlayerView
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.formatDurationMs
import com.joshua.videoplayer.ui.media.uiDurationMs
import com.joshua.videoplayer.ui.media.uiPlaybackPositionMs
import com.joshua.videoplayer.ui.media.scrubSessionIdentity
import com.joshua.videoplayer.ui.theme.GlassBarTint
import com.joshua.videoplayer.ui.theme.PlayerAccent
import com.joshua.videoplayer.ui.theme.PlayerAccentDim
import com.joshua.videoplayer.ui.theme.PlayerBackground
import com.joshua.videoplayer.ui.theme.PlayerOnSurface
import com.joshua.videoplayer.ui.theme.PlayerOnSurfaceDim
import com.joshua.videoplayer.ui.theme.PlayerSurfaceAlpha
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * 全屏播放页（深色沉浸风格）。
 *
 * @param controller 已连接的 [MediaController]
 * @param videoTitle 当前视频标题（文件名）
 * @param isLandscape 是否处于横屏模式
 * @param onClose 关闭全屏播放页
 * @param onPlaybackError 媒体错误回调
 * @param onToggleOrientation 切换横竖屏方向
 */
@Composable
fun PlayerContent(
    controller: MediaController?,
    videoTitle: String,
    isLandscape: Boolean,
    onClose: () -> Unit,
    onPlaybackError: (() -> Unit)? = null,
    onToggleOrientation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (controller == null) {
        Box(modifier = modifier.fillMaxSize().background(PlayerBackground), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.player_connecting),
                color = PlayerOnSurfaceDim,
            )
        }
        return
    }

    // ── 状态 ──
    var shuffleEnabled by remember(controller) { mutableStateOf(controller.shuffleModeEnabled) }
    var repeatMode by remember(controller) { mutableIntStateOf(controller.repeatMode) }
    var isPlaying by remember(controller) { mutableStateOf(controller.playWhenReady) }
    var playbackError by remember { mutableStateOf(false) }
    var isLyricsMode by remember { mutableStateOf(false) }
    var currentTitle by remember(controller) { mutableStateOf(videoTitle) }

    val playerViewHolder = remember { mutableStateOf<PlayerView?>(null) }
    fun detachPlayerViewFromController() {
        playerViewHolder.value?.player = null
    }

    BackHandler {
        if (isLandscape) {
            onToggleOrientation()
        } else {
            detachPlayerViewFromController()
            onClose()
        }
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { shuffleEnabled = enabled }
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlayerError(error: PlaybackException) { playbackError = true }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val title = controller.mediaMetadata.title?.toString()
                    ?: controller.currentMediaItem?.localConfiguration?.uri?.lastPathSegment
                    ?: ""
                if (title.isNotEmpty()) currentTitle = title
            }
        }
        controller.addListener(listener)
        onDispose { controller.removeListener(listener) }
    }

    // 沉浸式：横屏时隐藏系统栏 + 状态栏透明
    val view = LocalView.current
    val window = (view.context as? Activity)?.window
    DisposableEffect(isLandscape) {
        if (window != null) {
            val insetsController = WindowInsetsControllerCompat(window, view)
            if (isLandscape) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                insetsController.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (window != null) {
                WindowInsetsControllerCompat(window, view).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 横屏控件自动隐藏
    var controlsVisible by remember { mutableStateOf(true) }
    var hideJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun resetHideTimer() {
        hideJob?.cancel()
        if (isLandscape) {
            if (controlsVisible) {
                // 已显示 → 点击直接隐藏
                controlsVisible = false
            } else {
                // 已隐藏 → 点击显示，4秒后自动隐藏
                controlsVisible = true
                hideJob = scope.launch {
                    delay(4000)
                    controlsVisible = false
                }
            }
        }
    }

    LaunchedEffect(isLandscape) {
        if (isLandscape) {
            controlsVisible = true
            hideJob?.cancel()
            hideJob = scope.launch {
                delay(4000)
                controlsVisible = false
            }
        } else {
            controlsVisible = true
            hideJob?.cancel()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PlayerBackground)
            .then(
                if (isLandscape && !controlsVisible) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { resetHideTimer() }
                else if (!isLandscape) Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* 拦截触摸，防止穿透到底层 tabbar */ }
                else Modifier
            )
    ) {
        if (isLandscape) {
            // ── 横屏布局 ──
            LandscapeLayout(
                controller = controller,
                videoTitle = currentTitle,
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                controlsVisible = controlsVisible,
                onBack = {
                    detachPlayerViewFromController()
                    onClose()
                },
                onTogglePlayPause = {
                    if (controller.playWhenReady) controller.pause() else controller.play()
                },
                onPrevious = { controller.seekToPrevious() },
                onNext = { controller.seekToNext() },
                onCycleRepeat = { cycleRepeatMode(controller) },
                onToggleControls = { resetHideTimer() },
                playerViewHolder = playerViewHolder,
            )
        } else {
            // ── 竖屏布局 ──
            PortraitLayout(
                controller = controller,
                videoTitle = currentTitle,
                isPlaying = isPlaying,
                repeatMode = repeatMode,
                isLyricsMode = isLyricsMode,
                onBack = {
                    detachPlayerViewFromController()
                    onClose()
                },
                onTogglePlayPause = {
                    if (controller.playWhenReady) controller.pause() else controller.play()
                },
                onPrevious = { controller.seekToPrevious() },
                onNext = { controller.seekToNext() },
                onCycleRepeat = { cycleRepeatMode(controller) },
                onToggleLyricsMode = { isLyricsMode = !isLyricsMode },
                onToggleOrientation = onToggleOrientation,
                playerViewHolder = playerViewHolder,
            )
        }

        // 错误弹窗
        if (playbackError && onPlaybackError != null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(PlayerSurfaceAlpha)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.error_playback_failed),
                        color = PlayerOnSurface,
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PlayerAccent)
                            .clickable { onPlaybackError() }
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.retry_pick_video),
                            color = PlayerOnSurface,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 横屏布局
// ─────────────────────────────────────────────

@Composable
private fun LandscapeLayout(
    controller: MediaController,
    videoTitle: String,
    isPlaying: Boolean,
    repeatMode: Int,
    controlsVisible: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleControls: () -> Unit,
    playerViewHolder: androidx.compose.runtime.MutableState<PlayerView?>,
) {
    Box(Modifier.fillMaxSize()) {
        // 视频铺满
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    player = controller
                    playerViewHolder.value = this
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { pv ->
                pv.useController = false
                pv.player = controller
                playerViewHolder.value = pv
            },
            onRelease = { pv ->
                pv.player = null
                if (playerViewHolder.value === pv) playerViewHolder.value = null
            },
        )

        // 视频区域点击：控件显示时点击隐藏，控件隐藏时点击显示（由外层 Box 的 clickable 处理）
        if (controlsVisible) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { onToggleControls() },
            )
        }

        // 顶部：返回 + 标题（左上角叠加）
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent))
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PlayerOnSurface)
                    }
                    Text(
                        text = videoTitle,
                        color = PlayerOnSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.FullscreenExit, contentDescription = null, tint = PlayerOnSurface)
                    }
                }
            }
        }

        // 底部控件（居中布局）
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(500)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 进度条
                LandscapeScrubber(controller = controller)

                Spacer(Modifier.height(10.dp))

                // 播放控制（居中）+ 循环按钮（最右）
                Box(modifier = Modifier.fillMaxWidth()) {
                    // 播放控制居中
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = null, tint = PlayerOnSurface, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.size(16.dp))
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(PlayerAccent, CircleShape)
                                .clickable(onClick = onTogglePlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = PlayerOnSurface,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                        IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.SkipNext, contentDescription = null, tint = PlayerOnSurface, modifier = Modifier.size(32.dp))
                        }
                    }
                    // 循环按钮最右
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        RepeatToggleButton(repeatMode = repeatMode, onCycle = onCycleRepeat)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 竖屏布局
// ─────────────────────────────────────────────

@Composable
private fun PortraitLayout(
    controller: MediaController,
    videoTitle: String,
    isPlaying: Boolean,
    repeatMode: Int,
    isLyricsMode: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLyricsMode: () -> Unit,
    onToggleOrientation: () -> Unit,
    playerViewHolder: androidx.compose.runtime.MutableState<PlayerView?>,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // ── 顶部栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = PlayerOnSurface)
            }
            Text(
                text = videoTitle,
                color = PlayerOnSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        // ── 视频区域 ──
        val videoAlpha by animateFloatAsState(
            targetValue = if (isLyricsMode) 0f else 1f,
            animationSpec = tween(400),
            label = "videoAlpha",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(12.dp)),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = false
                        player = controller
                        playerViewHolder.value = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                update = { pv ->
                    pv.useController = false
                    pv.player = controller
                    pv.alpha = videoAlpha
                    playerViewHolder.value = pv
                },
                onRelease = { pv ->
                    pv.player = null
                    if (playerViewHolder.value === pv) playerViewHolder.value = null
                },
            )
        }

        // ── 歌词区域 ──
        val lyricsAreaWeight by animateFloatAsState(
            targetValue = if (isLyricsMode) 1f else 0.35f,
            animationSpec = tween(400),
            label = "lyricsWeight",
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(lyricsAreaWeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = if (isLyricsMode) Alignment.TopCenter else Alignment.Center,
        ) {
            LyricsPlaceholder()
        }

        // ── 控件栏 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(PlayerSurfaceAlpha)
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 60.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 进度条
            PortraitScrubber(controller = controller)

            // 播放控制：居中播放控件 + 左右两侧
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                // 左侧：循环 + 词
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RepeatToggleButton(repeatMode = repeatMode, onCycle = onCycleRepeat)
                    // LyricsToggleButton(isLyricsMode = isLyricsMode, onToggle = onToggleLyricsMode)
                }
                // 中间：播放控件（屏幕居中）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = controller.currentTimeline.windowCount > 0,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = null,
                            tint = PlayerOnSurface,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(PlayerAccent, CircleShape)
                            .clickable(onClick = onTogglePlayPause),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = PlayerOnSurface,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = controller.currentTimeline.windowCount > 0,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = null,
                            tint = PlayerOnSurface,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                // 右侧：横屏按钮
                IconButton(
                    onClick = onToggleOrientation,
                    modifier = Modifier.align(Alignment.CenterEnd),
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = PlayerOnSurface)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// 歌词占位
// ─────────────────────────────────────────────

@Composable
private fun LyricsPlaceholder() {
    // 预留 LazyColumn 结构，后续接入 LyricsState 数据
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        item {
            Text(
                text = stringResource(R.string.lyrics_placeholder),
                color = PlayerOnSurfaceDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 32.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────
// 循环按钮（三态切换）
// ─────────────────────────────────────────────

@Composable
private fun RepeatToggleButton(
    repeatMode: Int,
    onCycle: () -> Unit,
) {
    val isOne = repeatMode == Player.REPEAT_MODE_ONE

    IconButton(onClick = onCycle) {
        Icon(
            imageVector = if (isOne) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
            contentDescription = null,
            tint = PlayerAccent,
            modifier = Modifier.size(22.dp),
        )
    }
}

// ─────────────────────────────────────────────
// [词] 按钮
// ─────────────────────────────────────────────

@Composable
private fun LyricsToggleButton(
    isLyricsMode: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.player_lyrics_label),
            color = if (isLyricsMode) PlayerAccent else PlayerAccentDim,
            fontWeight = if (isLyricsMode) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ─────────────────────────────────────────────
// 共享进度条状态（使用 uiDurationMs / uiPlaybackPositionMs 处理裁剪偏移和元数据回退）
// ─────────────────────────────────────────────

@Composable
private fun rememberScrubberState(controller: MediaController): ScrubberState {
    val scope = rememberCoroutineScope()
    val state = remember(controller) { ScrubberState(controller, scope) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                state.settleJob?.cancel()
                state.settleJob = null
                state.scrubbing = false
                state.sync()
            }
            override fun onPlaybackStateChanged(state2: Int) {
                state.sync()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                state.sync()
            }
        }
        controller.addListener(listener)
        state.sync()
        onDispose {
            state.settleJob?.cancel()
            controller.removeListener(listener)
        }
    }

    LaunchedEffect(controller) {
        while (isActive) {
            if (!state.scrubbing) {
                state.positionMs = controller.uiPlaybackPositionMs()
                state.durationMs = controller.uiDurationMs()
            }
            delay(500)
        }
    }

    return state
}

private class ScrubberState(
    private val controller: MediaController,
    private val scope: kotlinx.coroutines.CoroutineScope,
) {
    var positionMs by mutableLongStateOf(controller.uiPlaybackPositionMs())
    var durationMs by mutableLongStateOf(controller.uiDurationMs())
    var scrubbing by mutableStateOf(false)
    var scrubFraction by mutableFloatStateOf(0f)
    var settleJob by mutableStateOf<Job?>(null)

    val durationValid: Boolean get() = durationMs > 0L && durationMs != C.TIME_UNSET

    val progress: Float
        get() = if (durationValid) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val displayProgress: Float get() = if (scrubbing) scrubFraction else progress

    val displayPositionMs: Long
        get() = when {
            !durationValid -> positionMs
            scrubbing -> (scrubFraction * durationMs.toDouble()).toLong().coerceIn(0L, durationMs)
            else -> positionMs
        }

    fun sync() {
        durationMs = controller.uiDurationMs()
        if (!scrubbing) {
            positionMs = controller.uiPlaybackPositionMs()
        }
    }

    fun onScrub(value: Float) {
        if (!durationValid) return
        settleJob?.cancel()
        settleJob = null
        if (!scrubbing) {
            scrubFraction = progress
        }
        scrubbing = true
        scrubFraction = value.coerceIn(0f, 1f)
    }

    fun onScrubFinished() {
        if (durationValid && scrubbing) {
            val target = (scrubFraction * durationMs.toDouble()).toLong().coerceIn(0L, durationMs)
            controller.seekTo(target)
            val identityAtSeek = controller.scrubSessionIdentity()
            settleJob?.cancel()
            settleJob = scope.launch {
                try {
                    for (i in 0 until 150) {
                        @OptIn(kotlinx.coroutines.InternalCoroutinesApi::class)
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
                    settleJob = null
                }
            }
        } else {
            scrubbing = false
        }
    }
}

// ─────────────────────────────────────────────
// 竖屏进度条
// ─────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PortraitScrubber(controller: MediaController) {
    val scrubberState = rememberScrubberState(controller)

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatDurationMs(scrubberState.displayPositionMs),
            color = PlayerOnSurfaceDim,
            modifier = Modifier.weight(0.1f),
        )
        androidx.compose.material3.Slider(
            value = scrubberState.displayProgress,
            onValueChange = { v -> scrubberState.onScrub(v) },
            onValueChangeFinished = { scrubberState.onScrubFinished() },
            enabled = scrubberState.durationValid,
            modifier = Modifier.weight(0.8f).height(20.dp),
            interactionSource = interactionSource,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = PlayerAccent,
                activeTrackColor = PlayerAccent,
                inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
            ),
            thumb = {
                Box(modifier = Modifier.offset(y = 2.dp)) {
                    androidx.compose.material3.SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = PlayerAccent,
                            activeTrackColor = PlayerAccent,
                            inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
                        ),
                        enabled = scrubberState.durationValid,
                        thumbSize = DpSize(10.dp, 10.dp),
                    )
                }
            },
            track = { sliderState ->
                androidx.compose.material3.SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(3.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = PlayerAccent,
                        activeTrackColor = PlayerAccent,
                        inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
                    ),
                    enabled = scrubberState.durationValid,
                    thumbTrackGapSize = 2.dp,
                )
            },
        )
        Text(
            text = formatDurationMs(scrubberState.durationMs),
            color = PlayerOnSurfaceDim,
            modifier = Modifier.weight(0.1f),
            textAlign = TextAlign.End,
        )
    }
}

// ─────────────────────────────────────────────
// 横屏进度条（简洁版）
// ─────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LandscapeScrubber(controller: MediaController) {
    val scrubberState = rememberScrubberState(controller)

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatDurationMs(scrubberState.displayPositionMs),
            color = PlayerOnSurfaceDim,
        )
        androidx.compose.material3.Slider(
            value = scrubberState.displayProgress,
            onValueChange = { v -> scrubberState.onScrub(v) },
            onValueChangeFinished = { scrubberState.onScrubFinished() },
            enabled = scrubberState.durationValid,
            modifier = Modifier.weight(1f).height(16.dp),
            interactionSource = interactionSource,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = PlayerAccent,
                activeTrackColor = PlayerAccent,
                inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
            ),
            thumb = {
                Box(modifier = Modifier.offset(y = 2.dp)) {
                    androidx.compose.material3.SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = PlayerAccent,
                            activeTrackColor = PlayerAccent,
                            inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
                        ),
                        enabled = scrubberState.durationValid,
                        thumbSize = DpSize(10.dp, 10.dp),
                    )
                }
            },
            track = { sliderState ->
                androidx.compose.material3.SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        thumbColor = PlayerAccent,
                        activeTrackColor = PlayerAccent,
                        inactiveTrackColor = PlayerOnSurfaceDim.copy(alpha = 0.3f),
                    ),
                    enabled = scrubberState.durationValid,
                    thumbTrackGapSize = 2.dp,
                )
            },
        )
        Text(
            text = formatDurationMs(scrubberState.durationMs),
            color = PlayerOnSurfaceDim,
            textAlign = TextAlign.End,
        )
    }
}

// ─────────────────────────────────────────────
// 循环模式切换工具函数
// ─────────────────────────────────────────────

private fun cycleRepeatMode(controller: MediaController) {
    controller.shuffleModeEnabled = false
    when (controller.repeatMode) {
        Player.REPEAT_MODE_ONE -> controller.setRepeatMode(Player.REPEAT_MODE_ALL)
        else -> controller.setRepeatMode(Player.REPEAT_MODE_ONE)
    }
}
