package com.example.videoplayer.library

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.LibraryVideoActionsRepository
import com.example.videoplayer.data.LocalVideo
import com.example.videoplayer.data.queryLocalVideos
import com.example.videoplayer.data.withProbedDurations
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
) : ViewModel() {

    /** 与 [com.example.videoplayer.data.withProbedDurations] 配合，按批轮换探测时长为 0 的条目。 */
    private var durationProbeRotate = 0

    private val _videos = MutableStateFlow<List<LocalVideo>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val ignoredUris: StateFlow<Set<String>> = actionsRepository
        .observeIgnoredUris()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val favoriteUriSet: StateFlow<Set<String>> = actionsRepository
        .observeFavoriteUris()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val filteredVideos: StateFlow<List<LocalVideo>> = combine(
        _videos,
        _searchQuery,
        ignoredUris,
    ) { list, q, ignored ->
        list
            .filter { it.contentUri.toString() !in ignored }
            .filter {
                q.isBlank() || it.displayName.contains(q.trim(), ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val ctx = application.applicationContext
            var list = ctx.queryLocalVideos()
            var rotate = durationProbeRotate
            // 多轮探测：单次下拉可扫过更多「时长为 0」的条目，减少反复刷新。
            repeat(5) {
                if (list.none { it.durationMs <= 0L }) return@repeat
                val (nextList, nextRotate) = list.withProbedDurations(ctx, rotate)
                list = nextList
                rotate = nextRotate
            }
            durationProbeRotate = rotate
            _videos.value = list
        }
    }

    fun setSearchQuery(text: String) {
        _searchQuery.value = text
    }

    fun ignoreVideo(video: LocalVideo) {
        viewModelScope.launch {
            actionsRepository.ignoreUri(video.contentUri.toString())
        }
    }

    fun addFavorite(video: LocalVideo) {
        viewModelScope.launch {
            actionsRepository.favoriteUri(video.contentUri.toString())
        }
    }

    fun removeFavorite(video: LocalVideo) {
        viewModelScope.launch {
            actionsRepository.unfavoriteUri(video.contentUri.toString())
        }
    }
}
