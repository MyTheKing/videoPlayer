package com.example.videoplayer.playlists

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videoplayer.data.LocalVideo
import com.example.videoplayer.data.PlaylistRepository
import com.example.videoplayer.data.local.PlaylistItemEntity
import com.example.videoplayer.data.local.PlaylistWithCount
import com.example.videoplayer.data.probeAndPersistPlaylistZeroDurations
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 歌单列表与条目操作。
 */
class PlaylistViewModel(private val repository: PlaylistRepository) : ViewModel() {

    val playlists: StateFlow<List<PlaylistWithCount>> = repository
        .observePlaylistsWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 在 Composable 中请配合 `remember(playlistId)` 使用，以保证收集的是同一 Flow 实例。 */
    fun playlistItemsFlow(playlistId: Long): Flow<List<PlaylistItemEntity>> =
        repository.observePlaylistItems(playlistId)

    fun observePlaylistIdsContainingVideo(contentUri: String): Flow<Set<Long>> =
        repository.observePlaylistIdsContainingContentUri(contentUri)

    fun createPlaylist(name: String) {
        viewModelScope.launch { repository.createPlaylist(name) }
    }

    fun renamePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch { repository.renamePlaylist(playlistId, name) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { repository.deletePlaylist(playlistId) }
    }

    fun addVideoToPlaylist(playlistId: Long, video: LocalVideo) {
        viewModelScope.launch { repository.addVideoToPlaylist(playlistId, video) }
    }

    fun toggleVideoInPlaylist(playlistId: Long, video: LocalVideo) {
        viewModelScope.launch { repository.toggleVideoInPlaylist(playlistId, video) }
    }

    fun addContentUriToPlaylist(playlistId: Long, uri: Uri, title: String) {
        viewModelScope.launch { repository.addContentUriToPlaylist(playlistId, uri, title) }
    }

    fun removePlaylistItem(itemId: Long) {
        viewModelScope.launch { repository.removeItem(itemId) }
    }

    /** 补全歌单内时长为 0 的条目并写回数据库（进入详情或新增视频后触发）。 */
    fun probeMissingDurationsInPlaylist(context: Context, playlistId: Long) {
        viewModelScope.launch {
            val items = repository.snapshotPlaylistItems(playlistId)
            probeAndPersistPlaylistZeroDurations(context.applicationContext, items) { itemId, ms ->
                repository.updatePlaylistItemDuration(itemId, ms)
            }
        }
    }
}
