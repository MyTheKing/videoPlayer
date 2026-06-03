package com.joshua.videoplayer.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.joshua.videoplayer.data.PlaybackCacheManager
import com.joshua.videoplayer.data.PlaylistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Activity 级播放状态：当前队列请求与已连接的 [MediaController]（供全屏播放器与迷你条共用）。
 */
class MainPlaybackViewModel(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

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

    /** 恢复播放后是否暂停 */
    internal var _pauseAfterRestore = false

    /** 用户正在主动播放，不需要自动恢复缓存 */
    internal var userInitiatedPlay = false

    /** 忽略列表同步任务 */
    private var ignoredSyncJob: Job? = null

    /** 歌单同步任务 */
    private var playlistSyncJob: Job? = null

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
        userInitiatedPlay = true
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

        // 保存播放队列到缓存
        PlaybackCacheManager.savePlaybackState(
            uriStrings = uriStrings,
            titles = titles,
            durationMsList = durs,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            queueOrigin = queueOrigin,
        )

        // 启动同步监听
        startQueueSync(queueOrigin)
    }

    /**
     * 恢复缓存的播放队列（默认暂停状态）
     */
    fun restoreCachedPlayback() {
        val cached = PlaybackCacheManager.getCachedPlaybackState() ?: return
        playQueue(
            uriStrings = cached.uriStrings,
            titles = cached.titles,
            durationMsList = cached.durationMsList,
            startIndex = cached.currentIndex,
            shuffleEnabled = cached.shuffleEnabled,
            repeatMode = cached.repeatMode,
            queueOrigin = cached.queueOrigin,
        )
        // 恢复后暂停
        _pauseAfterRestore = true
    }

    /**
     * 启动播放队列与数据源的同步监听
     */
    private fun startQueueSync(queueOrigin: PlaybackQueueOrigin) {
        // 取消之前的同步任务
        ignoredSyncJob?.cancel()
        playlistSyncJob?.cancel()

        // 1. 监听忽略列表变化
        ignoredSyncJob = viewModelScope.launch {
            playlistRepository.observeIgnoredContentUris().collect { ignoredUris ->
                syncQueueWithIgnoredUris(ignoredUris)
            }
        }

        // 2. 如果来源是歌单，监听歌单条目变化
        if (queueOrigin is PlaybackQueueOrigin.UserPlaylist) {
            playlistSyncJob = viewModelScope.launch {
                playlistRepository.observePlaylistItems(queueOrigin.playlistId).collect { items ->
                    syncQueueWithPlaylistItems(items.map { it.contentUri }.toSet())
                }
            }
        }
    }

    /**
     * 同步播放队列与忽略列表
     */
    private suspend fun syncQueueWithIgnoredUris(ignoredUris: Set<String>) {
        val controller = _mediaController.value ?: return
        val currentQueue = _playbackRequest.value ?: return
        val currentIndex = controller.currentMediaItemIndex

        // 找出需要移除的 URI（排除当前播放的）
        val urisToRemove = currentQueue.uriStrings.filterIndexed { index, uri ->
            uri in ignoredUris && index != currentIndex
        }

        if (urisToRemove.isEmpty()) {
            // 即使没有需要移除的，也要检查当前播放的是否被忽略
            val currentUri = currentQueue.uriStrings.getOrNull(currentIndex)
            if (currentUri != null && currentUri in ignoredUris) {
                PlaybackSkipManager.addSkipUri(currentUri)
            }
            return
        }

        // 标记当前播放的视频（如果被忽略）
        val currentUri = currentQueue.uriStrings.getOrNull(currentIndex)
        if (currentUri != null && currentUri in ignoredUris) {
            PlaybackSkipManager.addSkipUri(currentUri)
        }

        // 从队列中移除（从后往前移除，避免索引偏移）
        val indicesToRemove = urisToRemove.mapNotNull { uri ->
            currentQueue.uriStrings.indexOf(uri).takeIf { it >= 0 }
        }.sortedDescending()

        for (index in indicesToRemove) {
            controller.removeMediaItem(index)
        }

        // 更新 ViewModel 中的队列状态
        updateQueueStateAfterRemoval(indicesToRemove, currentIndex)
    }

    /**
     * 同步播放队列与歌单条目
     */
    private suspend fun syncQueueWithPlaylistItems(playlistUris: Set<String>) {
        val controller = _mediaController.value ?: return
        val currentQueue = _playbackRequest.value ?: return
        val currentIndex = controller.currentMediaItemIndex

        // 找出需要移除的 URI（不在歌单中的，排除当前播放的）
        val urisToRemove = currentQueue.uriStrings.filterIndexed { index, uri ->
            uri !in playlistUris && index != currentIndex
        }

        if (urisToRemove.isEmpty()) {
            // 即使没有需要移除的，也要检查当前播放的是否已从歌单移除
            val currentUri = currentQueue.uriStrings.getOrNull(currentIndex)
            if (currentUri != null && currentUri !in playlistUris) {
                PlaybackSkipManager.addSkipUri(currentUri)
            }
            return
        }

        // 标记当前播放的视频（如果已从歌单移除）
        val currentUri = currentQueue.uriStrings.getOrNull(currentIndex)
        if (currentUri != null && currentUri !in playlistUris) {
            PlaybackSkipManager.addSkipUri(currentUri)
        }

        // 从队列中移除
        val indicesToRemove = urisToRemove.mapNotNull { uri ->
            currentQueue.uriStrings.indexOf(uri).takeIf { it >= 0 }
        }.sortedDescending()

        for (index in indicesToRemove) {
            controller.removeMediaItem(index)
        }

        updateQueueStateAfterRemoval(indicesToRemove, currentIndex)
    }

    /**
     * 移除后更新队列状态
     */
    private fun updateQueueStateAfterRemoval(removedIndices: List<Int>, currentIndex: Int) {
        val request = _playbackRequest.value ?: return
        val newUriStrings = request.uriStrings.filterIndexed { index, _ -> index !in removedIndices }
        val newTitles = request.titles.filterIndexed { index, _ -> index !in removedIndices }
        val newDurations = request.durationMsList.filterIndexed { index, _ -> index !in removedIndices }

        // 计算新的当前索引
        val removedBeforeCurrent = removedIndices.count { it < currentIndex }
        val newCurrentIndex = (currentIndex - removedBeforeCurrent)
            .coerceIn(0, (newUriStrings.size - 1).coerceAtLeast(0))

        _playbackRequest.value = request.copy(
            uriStrings = newUriStrings,
            titles = newTitles,
            durationMsList = newDurations,
            startIndex = newCurrentIndex,
        )

        // 更新缓存
        PlaybackCacheManager.savePlaybackState(
            uriStrings = newUriStrings,
            titles = newTitles,
            durationMsList = newDurations,
            shuffleEnabled = request.shuffleEnabled,
            repeatMode = request.repeatMode,
            queueOrigin = request.queueOrigin,
        )

        // 如果队列为空，停止播放
        if (newUriStrings.isEmpty()) {
            stopAndClearSession()
        }
    }

    /**
     * 检查指定 URI 是否应该被跳过
     * 由 MediaPlaybackService 在 onMediaItemTransition 时调用
     */
    fun shouldSkipUri(uri: String): Boolean {
        return PlaybackSkipManager.shouldSkip(uri)
    }

    /**
     * 移除已跳过的 URI
     */
    fun removeSkippedUri(uri: String) {
        PlaybackSkipManager.removeSkipped(uri)
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

        // 清空跳过列表
        PlaybackSkipManager.clear()

        // 取消同步任务
        ignoredSyncJob?.cancel()
        playlistSyncJob?.cancel()
    }

    fun setMediaController(controller: MediaController?) {
        _mediaController.value?.release()
        _mediaController.value = controller
    }

    override fun onCleared() {
        _mediaController.value?.release()
        _mediaController.value = null
        ignoredSyncJob?.cancel()
        playlistSyncJob?.cancel()
        super.onCleared()
    }
}
