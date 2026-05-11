package com.example.videoplayer.data

import android.net.Uri
import com.example.videoplayer.data.local.PlaylistDao
import com.example.videoplayer.data.local.PlaylistEntity
import com.example.videoplayer.data.local.PlaylistItemEntity
import com.example.videoplayer.data.local.PlaylistWithCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 歌单与条目的仓库层，封装 Room DAO。
 */
class PlaylistRepository(private val dao: PlaylistDao) {

    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()

    fun observePlaylistItems(playlistId: Long) = dao.observeItems(playlistId)

    suspend fun snapshotPlaylistItems(playlistId: Long): List<PlaylistItemEntity> =
        dao.snapshotItems(playlistId)

    suspend fun updatePlaylistItemDuration(itemId: Long, durationMs: Long) =
        dao.updateItemDuration(itemId, durationMs)

    fun observePlaylistIdsContainingContentUri(contentUri: String): Flow<Set<Long>> =
        dao.observePlaylistIdsContainingContentUri(contentUri).map { it.toSet() }

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name.trim().ifBlank { "未命名歌单" }))
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: LocalVideo) {
        val max = dao.maxSortOrder(playlistId) ?: -1
        val next = max + 1
        dao.insertItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                contentUri = video.contentUri.toString(),
                displayTitle = video.displayName,
                durationMs = video.durationMs,
                sortOrder = next,
            ),
        )
    }

    /** 已在该歌单中则按 contentUri 全部移除，否则追加一条。 */
    suspend fun toggleVideoInPlaylist(playlistId: Long, video: LocalVideo) {
        val uri = video.contentUri.toString()
        if (dao.countItemsInPlaylistWithUri(playlistId, uri) > 0) {
            dao.deleteItemsInPlaylistWithContentUri(playlistId, uri)
        } else {
            addVideoToPlaylist(playlistId, video)
        }
    }

    /** 从系统文件选择器得到的 [Uri] 追加到歌单（无时长时填 0）。 */
    suspend fun addContentUriToPlaylist(playlistId: Long, uri: Uri, displayTitle: String, durationMs: Long = 0L) {
        val max = dao.maxSortOrder(playlistId) ?: -1
        dao.insertItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                contentUri = uri.toString(),
                displayTitle = displayTitle.ifBlank { uri.lastPathSegment ?: "video" },
                durationMs = durationMs,
                sortOrder = max + 1,
            ),
        )
    }

    suspend fun removeItem(itemId: Long) = dao.deleteItem(itemId)

    suspend fun deletePlaylist(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun renamePlaylist(playlistId: Long, name: String) =
        dao.updatePlaylistName(playlistId, name.trim().ifBlank { "未命名歌单" })
}
