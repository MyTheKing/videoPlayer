package com.joshua.videoplayer.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.videoplayer.data.DurationFilterManager
import com.joshua.videoplayer.data.LibraryVideoActionsRepository
import com.joshua.videoplayer.data.LibraryWarmCache
import com.joshua.videoplayer.data.LocalVideo
import com.joshua.videoplayer.data.PlaylistRepository
import com.joshua.videoplayer.data.scanLocalLibraryWithDurationProbes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 本地库：扫描、时长补全、搜索；并过滤「已忽略」、维护「我喜欢」Uri 集合。
 */
class LibraryViewModel(
    private val application: Application,
    private val actionsRepository: LibraryVideoActionsRepository,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    /** 与 [com.joshua.videoplayer.data.withProbedDurations] 配合，按批轮换探测时长为 0 的条目。 */
    private var durationProbeRotate = 0

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _ignoredUris = MutableStateFlow<Set<String>>(emptySet())

    val favoriteUriSet: StateFlow<Set<String>> = actionsRepository
        .observeFavoriteUris()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _durationFilterEnabled = MutableStateFlow(DurationFilterManager.filterEnabled)
    private val _minDurationMs = MutableStateFlow(DurationFilterManager.minDurationMs)
    private val _maxDurationMs = MutableStateFlow(DurationFilterManager.maxDurationMs)

    fun updateDurationFilter() {
        _durationFilterEnabled.value = DurationFilterManager.filterEnabled
        _minDurationMs.value = DurationFilterManager.minDurationMs
        _maxDurationMs.value = DurationFilterManager.maxDurationMs
    }

    val filteredVideos: StateFlow<List<LocalVideo>> = combine(
        _videos,
        _searchQuery,
        _ignoredUris,
        _durationFilterEnabled,
        _minDurationMs,
        _maxDurationMs,
    ) { args ->
        val list = args[0] as List<LocalVideo>
        val q = args[1] as String
        val ignored = args[2] as Set<String>
        val filterEnabled = args[3] as Boolean
        val minMs = args[4] as Long
        val maxMs = args[5] as Long
        list
            .filter { it.contentUri.toString() !in ignored }
            .filter {
                q.isBlank() || it.displayName.contains(q.trim(), ignoreCase = true)
            }
            .filter { video ->
                if (!filterEnabled) return@filter true
                if (minMs > 0 && video.durationMs < minMs) return@filter false
                if (maxMs > 0 && video.durationMs > maxMs) return@filter false
                true
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        LibraryWarmCache.peekLatest()?.let { _videos.value = it }
        refresh()
    }

    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val (list, nextRotate) = application.scanLocalLibraryWithDurationProbes(durationProbeRotate)
                durationProbeRotate = nextRotate
                _videos.value = list
                _ignoredUris.value = playlistRepository.getIgnoredContentUris()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun setSearchQuery(text: String) {
        _searchQuery.value = text
    }

    fun ignoreVideo(video: LocalVideo) {
        viewModelScope.launch {
            playlistRepository.addVideoToIgnoredPlaylist(video)
            // 更新忽略列表
            _ignoredUris.value = playlistRepository.getIgnoredContentUris()
        }
    }

    fun removeFromIgnored(video: LocalVideo) {
        viewModelScope.launch {
            playlistRepository.removeVideoFromIgnoredPlaylist(video)
            // 更新忽略列表
            _ignoredUris.value = playlistRepository.getIgnoredContentUris()
        }
    }

    fun addFavorite(video: LocalVideo) {
        viewModelScope.launch {
            playlistRepository.addVideoToLikedPlaylist(video)
        }
    }

    fun removeFavorite(video: LocalVideo) {
        viewModelScope.launch {
            playlistRepository.removeVideoFromLikedPlaylist(video)
        }
    }

    /** 双缓存同步（授权存储权限后调用：缓存有数据覆盖文件，缓存无数据从文件读取） */
    fun syncFromFile() {
        viewModelScope.launch {
            actionsRepository.bidirectionalSync()
            playlistRepository.bidirectionalSync()
            _ignoredUris.value = playlistRepository.getIgnoredContentUris()
            com.joshua.videoplayer.ui.theme.ThemeColorManager.bidirectionalSync()
            DurationFilterManager.bidirectionalSync()
            updateDurationFilter()
        }
    }

    /** 播放失败时移除无效资源（缓存+文件同步） */
    fun removeInvalidUri(contentUri: String) {
        viewModelScope.launch {
            playlistRepository.removeUriEverywhere(contentUri)
            _ignoredUris.value = playlistRepository.getIgnoredContentUris()
        }
    }

    /** 切换存储路径同步（新文件夹有数据则读文件覆盖缓存，无数据则缓存写入文件） */
    fun syncOnPathSwitch() {
        viewModelScope.launch {
            actionsRepository.syncOnPathSwitch()
            playlistRepository.syncOnPathSwitch()
            _ignoredUris.value = playlistRepository.getIgnoredContentUris()
            com.joshua.videoplayer.ui.theme.ThemeColorManager.syncOnPathSwitch(application)
            DurationFilterManager.syncOnPathSwitch(application)
            updateDurationFilter()
        }
    }
}
