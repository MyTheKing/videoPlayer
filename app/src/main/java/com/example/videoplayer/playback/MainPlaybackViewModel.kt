package com.example.videoplayer.playback

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Activity 级播放状态：当前队列请求与已连接的 [MediaController]（供全屏播放器与迷你条共用）。
 */
class MainPlaybackViewModel : ViewModel() {

    data class PlaybackRequest(
        val uriStrings: List<String>,
        val titles: List<String>,
        /** 与 [uriStrings] 对齐；>0 时写入 [androidx.media3.common.MediaMetadata]，供迷你条在 Exo 时长未知时回退。 */
        val durationMsList: List<Long>,
        val startIndex: Int,
        val shuffleEnabled: Boolean = false,
        val repeatMode: Int = Player.REPEAT_MODE_ALL,
        val queueOrigin: PlaybackQueueOrigin = PlaybackQueueOrigin.LocalLibrary,
    )

    private val _playbackRequest = MutableStateFlow<PlaybackRequest?>(null)
    val playbackRequest: StateFlow<PlaybackRequest?> = _playbackRequest.asStateFlow()

    private val _playbackQueueOrigin = MutableStateFlow<PlaybackQueueOrigin?>(null)
    val playbackQueueOrigin: StateFlow<PlaybackQueueOrigin?> = _playbackQueueOrigin.asStateFlow()

    private val _mediaController = MutableStateFlow<MediaController?>(null)
    val mediaController: StateFlow<MediaController?> = _mediaController.asStateFlow()

    private val _fullPlayerVisible = MutableStateFlow(false)
    val fullPlayerVisible: StateFlow<Boolean> = _fullPlayerVisible.asStateFlow()

    /** 每次发起新播放递增，用于 LaunchedEffect 在队列内容相同时仍能强制重连。 */
    private val _sessionGeneration = MutableStateFlow(0L)
    val sessionGeneration: StateFlow<Long> = _sessionGeneration.asStateFlow()

    fun playQueue(
        uriStrings: List<String>,
        titles: List<String>,
        durationMsList: List<Long> = emptyList(),
        startIndex: Int = 0,
        shuffleEnabled: Boolean = false,
        repeatMode: Int = Player.REPEAT_MODE_ALL,
        queueOrigin: PlaybackQueueOrigin = PlaybackQueueOrigin.LocalLibrary,
    ) {
        if (uriStrings.isEmpty()) return
        val idx = startIndex.coerceIn(0, uriStrings.size - 1)
        val durs = durationMsList.takeIf { it.size == uriStrings.size }
            ?: List(uriStrings.size) { 0L }
        _playbackRequest.value = PlaybackRequest(
            uriStrings = uriStrings,
            titles = titles,
            durationMsList = durs,
            startIndex = idx,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            queueOrigin = queueOrigin,
        )
        _playbackQueueOrigin.value = queueOrigin
        _sessionGeneration.value = _sessionGeneration.value + 1
    }

    fun openFullPlayer() {
        _fullPlayerVisible.value = true
    }

    fun closeFullPlayer() {
        // 收起全屏前解除输出面，避免 PlayerView 尚未 onRelease 时 Surface 已失效导致 MediaCodec 报错
        _mediaController.value?.clearVideoSurface()
        _fullPlayerVisible.value = false
    }

    /** 播放失败或用户退出时释放控制器并清空队列，避免迷你条残留。 */
    fun stopAndClearSession() {
        _playbackRequest.value = null
        _playbackQueueOrigin.value = null
        _fullPlayerVisible.value = false
        _mediaController.value?.release()
        _mediaController.value = null
    }

    fun setMediaController(controller: MediaController?) {
        _mediaController.value?.release()
        _mediaController.value = controller
    }

    override fun onCleared() {
        _mediaController.value?.release()
        _mediaController.value = null
        super.onCleared()
    }
}
