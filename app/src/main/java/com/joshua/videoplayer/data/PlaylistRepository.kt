package com.joshua.videoplayer.data

import android.content.Context
import android.net.Uri
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.local.PlaylistDao
import com.joshua.videoplayer.data.local.PlaylistEntity
import com.joshua.videoplayer.data.local.PlaylistItemEntity
import com.joshua.videoplayer.data.local.PlaylistWithCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 歌单与条目的仓库层，封装 Room DAO；与「喜欢」收藏表双向同步（系统喜欢歌单）。
 */
class PlaylistRepository(
    private val dao: PlaylistDao,
    private val libraryActions: LibraryVideoActionsRepository,
) {

    private val fileSyncScope = CoroutineScope(Dispatchers.IO)

    /** 同步进行中时抑制写回，防止 force 同步期间旧数据覆盖新文件 */
    @Volatile
    private var suppressFileWrite = false

    /** 将当前歌单状态导出到文件存储（防抖） */
    private fun syncPlaylistsToFile() {
        if (suppressFileWrite) {
            android.util.Log.d("PlaylistRepository", "syncPlaylistsToFile: suppressed during sync")
            return
        }
        fileSyncScope.launch {
            try {
                val playlists = dao.snapshotPlaylistsWithCount()
                val playlistJsons = playlists.map { pw ->
                    val items = dao.snapshotItems(pw.playlist.id)
                    PlaylistJson(
                        name = pw.playlist.name,
                        kind = pw.playlist.kind,
                        coverImageUri = pw.playlist.coverImageUri,
                        items = items.map { item ->
                            PlaylistItemJson(
                                contentUri = item.contentUri,
                                displayTitle = item.displayTitle,
                                durationMs = item.durationMs,
                                sortOrder = item.sortOrder,
                            )
                        },
                    )
                }
                android.util.Log.d("PlaylistRepository", "Syncing ${playlistJsons.size} playlists to file")
                FileStorageManager.debounceWritePlaylists(PlaylistsJson(playlistJsons))
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepository", "Failed to sync playlists to file", e)
            }
        }
    }

    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>> = dao.observePlaylistsWithCount()

    fun observePlaylistItems(playlistId: Long) = dao.observeItems(playlistId)

    suspend fun snapshotPlaylistItems(playlistId: Long): List<PlaylistItemEntity> =
        dao.snapshotItems(playlistId)

    suspend fun updatePlaylistItemDuration(itemId: Long, durationMs: Long) =
        dao.updateItemDuration(itemId, durationMs)

    fun observePlaylistIdsContainingContentUri(contentUri: String): Flow<Set<Long>> =
        dao.observePlaylistIdsContainingContentUri(contentUri).map { it.toSet() }

    suspend fun ensureLikedPlaylistId(): Long {
        dao.getPlaylistIdByKind(PlaylistEntity.KIND_LIKED)?.let { return it }
        return dao.insertPlaylist(
            PlaylistEntity(
                name = LIKED_PLAYLIST_NAME,
                kind = PlaylistEntity.KIND_LIKED,
            ),
        )
    }

    suspend fun ensureIgnoredPlaylistId(): Long {
        dao.getPlaylistIdByKind(PlaylistEntity.KIND_IGNORED)?.let { return it }
        return dao.insertPlaylist(
            PlaylistEntity(
                name = IGNORED_PLAYLIST_NAME,
                kind = PlaylistEntity.KIND_IGNORED,
            ),
        )
    }

    /** 菜单「添加到我喜欢的」：写入收藏表并加入系统喜欢歌单（去重）。 */
    suspend fun addVideoToLikedPlaylist(video: LocalVideo) {
        val likedId = ensureLikedPlaylistId()
        val uri = video.contentUri.toString()
        libraryActions.favoriteUri(uri)
        if (dao.countItemsInPlaylistWithUri(likedId, uri) == 0) {
            val max = dao.maxSortOrder(likedId) ?: -1
            dao.insertItem(
                PlaylistItemEntity(
                    playlistId = likedId,
                    contentUri = uri,
                    displayTitle = video.displayName,
                    durationMs = video.durationMs,
                    sortOrder = max + 1,
                ),
            )
        }
        syncPlaylistsToFile()
    }

    /** 菜单「取消喜欢」：从收藏表与喜欢歌单一并移除。 */
    suspend fun removeVideoFromLikedPlaylist(video: LocalVideo) {
        val likedId = dao.getPlaylistIdByKind(PlaylistEntity.KIND_LIKED) ?: return
        val uri = video.contentUri.toString()
        libraryActions.unfavoriteUri(uri)
        dao.deleteItemsInPlaylistWithContentUri(likedId, uri)
        syncPlaylistsToFile()
    }

    /** 菜单「忽略」：加入系统忽略歌单（去重）。 */
    suspend fun addVideoToIgnoredPlaylist(video: LocalVideo) {
        val ignoredId = ensureIgnoredPlaylistId()
        val uri = video.contentUri.toString()
        if (dao.countItemsInPlaylistWithUri(ignoredId, uri) == 0) {
            val max = dao.maxSortOrder(ignoredId) ?: -1
            dao.insertItem(
                PlaylistItemEntity(
                    playlistId = ignoredId,
                    contentUri = uri,
                    displayTitle = video.displayName,
                    durationMs = video.durationMs,
                    sortOrder = max + 1,
                ),
            )
        }
        syncPlaylistsToFile()
    }

    /** 从忽略歌单中移除（恢复显示）。 */
    suspend fun removeVideoFromIgnoredPlaylist(video: LocalVideo) {
        val ignoredId = dao.getPlaylistIdByKind(PlaylistEntity.KIND_IGNORED) ?: return
        val uri = video.contentUri.toString()
        dao.deleteItemsInPlaylistWithContentUri(ignoredId, uri)
        syncPlaylistsToFile()
    }

    /** 获取忽略歌单中的所有 contentUri。 */
    suspend fun getIgnoredContentUris(): Set<String> {
        val ignoredId = dao.getPlaylistIdByKind(PlaylistEntity.KIND_IGNORED) ?: return emptySet()
        return dao.snapshotItems(ignoredId).map { it.contentUri }.toSet()
    }

    suspend fun createPlaylist(name: String): Long {
        val id = dao.insertPlaylist(
            PlaylistEntity(
                name = name.trim().ifBlank { "未命名歌单" },
                kind = PlaylistEntity.KIND_NORMAL,
            ),
        )
        syncPlaylistsToFile()
        return id
    }

    suspend fun addVideoToPlaylist(playlistId: Long, video: LocalVideo) {
        val pl = dao.getPlaylistById(playlistId) ?: return
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
        if (pl.kind == PlaylistEntity.KIND_LIKED) {
            libraryActions.favoriteUri(video.contentUri.toString())
        }
        syncPlaylistsToFile()
    }

    /** 已在该歌单中则按 contentUri 全部移除，否则追加一条。 */
    suspend fun toggleVideoInPlaylist(playlistId: Long, video: LocalVideo) {
        val pl = dao.getPlaylistById(playlistId) ?: return
        val uri = video.contentUri.toString()
        if (dao.countItemsInPlaylistWithUri(playlistId, uri) > 0) {
            dao.deleteItemsInPlaylistWithContentUri(playlistId, uri)
            if (pl.kind == PlaylistEntity.KIND_LIKED) {
                libraryActions.unfavoriteUri(uri)
            }
        } else {
            addVideoToPlaylist(playlistId, video)
            return
        }
        syncPlaylistsToFile()
    }

    /** 从系统文件选择器得到的 [Uri] 追加到歌单（无时长时填 0）。 */
    suspend fun addContentUriToPlaylist(playlistId: Long, uri: Uri, displayTitle: String, durationMs: Long = 0L) {
        val pl = dao.getPlaylistById(playlistId) ?: return
        val max = dao.maxSortOrder(playlistId) ?: -1
        val uriStr = uri.toString()
        dao.insertItem(
            PlaylistItemEntity(
                playlistId = playlistId,
                contentUri = uriStr,
                displayTitle = displayTitle.ifBlank { uri.lastPathSegment ?: "video" },
                durationMs = durationMs,
                sortOrder = max + 1,
            ),
        )
        if (pl.kind == PlaylistEntity.KIND_LIKED) {
            libraryActions.favoriteUri(uriStr)
        }
        syncPlaylistsToFile()
    }

    suspend fun removeItem(itemId: Long) {
        val item = dao.getItemById(itemId) ?: return
        val pl = dao.getPlaylistById(item.playlistId) ?: return
        dao.deleteItem(itemId)
        if (pl.kind == PlaylistEntity.KIND_LIKED) {
            libraryActions.unfavoriteUri(item.contentUri)
        }
        syncPlaylistsToFile()
    }

    suspend fun removePlaylistItems(itemIds: List<Long>) {
        if (itemIds.isEmpty()) return
        val items = itemIds.mapNotNull { dao.getItemById(it) }
        val playlistIds = items.map { it.playlistId }.distinct()
        val likedPlaylistIds = playlistIds.filter { pid ->
            dao.getPlaylistById(pid)?.kind == PlaylistEntity.KIND_LIKED
        }
        // 对属于「喜欢」歌单的条目取消收藏
        for (item in items) {
            if (item.playlistId in likedPlaylistIds) {
                libraryActions.unfavoriteUri(item.contentUri)
            }
        }
        dao.deleteItemsByIds(itemIds)
        syncPlaylistsToFile()
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylistIfDeletable(playlistId)
        syncPlaylistsToFile()
    }

    suspend fun deletePlaylists(ids: List<Long>) {
        if (ids.isEmpty()) return
        dao.deletePlaylistsByIds(ids)
        syncPlaylistsToFile()
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        dao.updatePlaylistName(playlistId, name.trim().ifBlank { "未命名歌单" })
        syncPlaylistsToFile()
    }

    suspend fun setPlaylistCover(playlistId: Long, coverUri: String?) {
        val pl = dao.getPlaylistById(playlistId) ?: return
        if (pl.kind != PlaylistEntity.KIND_NORMAL) return
        dao.updatePlaylistCover(playlistId, coverUri)
        syncPlaylistsToFile()
    }

    /**
     * 双缓存同步（默认文件夹）：缓存有数据 → 覆盖文件；缓存无数据 → 从文件恢复。
     */
    suspend fun bidirectionalSync() {
        val existing = dao.snapshotPlaylistsWithCount()
        val hasData = existing.any { it.playlist.kind == PlaylistEntity.KIND_NORMAL }
        if (hasData) {
            android.util.Log.d("PlaylistRepository", "bidirectionalSync: cache→file (${existing.size} playlists)")
            syncPlaylistsToFile()
        } else {
            android.util.Log.d("PlaylistRepository", "bidirectionalSync: file→cache")
            syncFromFileIfNeeded()
        }
    }

    /**
     * 切换存储路径同步：文件有内容 → 读文件覆盖缓存；文件无内容 → 缓存写入文件。
     */
    suspend fun syncOnPathSwitch() {
        // 先抑制写回，防止 Room 数据变化触发 syncPlaylistsToFile 覆盖新文件
        suppressFileWrite = true
        try {
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "playlists.json")
            val hasFileData = try {
                if (file.exists()) {
                    val raw = file.readText()
                    raw.isNotBlank() && raw != "{\"playlists\":[]}"
                } else false
            } catch (_: Exception) { false }

            if (hasFileData) {
                // 新文件夹有数据 → 读文件覆盖缓存
                android.util.Log.d("PlaylistRepository", "syncOnPathSwitch: file→cache (new folder has data)")
                syncFromFileIfNeeded(force = true)
                // 同步写回确认文件内容正确
                syncPlaylistsToFileInternal()
            } else {
                // 新文件夹无数据 → 缓存写入文件
                android.util.Log.d("PlaylistRepository", "syncOnPathSwitch: cache→file (new folder empty)")
                syncPlaylistsToFileInternal()
            }
        } finally {
            suppressFileWrite = false
        }
    }

    /** 内部直接写入（suspend），不受 suppress 限制 */
    private suspend fun syncPlaylistsToFileInternal() {
        try {
            val playlists = dao.snapshotPlaylistsWithCount()
            val playlistJsons = playlists.map { pw ->
                val items = dao.snapshotItems(pw.playlist.id)
                PlaylistJson(
                    name = pw.playlist.name,
                    kind = pw.playlist.kind,
                    coverImageUri = pw.playlist.coverImageUri,
                    items = items.map { item ->
                        PlaylistItemJson(
                            contentUri = item.contentUri,
                            displayTitle = item.displayTitle,
                            durationMs = item.durationMs,
                            sortOrder = item.sortOrder,
                        )
                    },
                )
            }
            FileStorageManager.writePlaylists(PlaylistsJson(playlistJsons))
            android.util.Log.d("PlaylistRepository", "syncPlaylistsToFileInternal: wrote ${playlistJsons.size} playlists")
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "syncPlaylistsToFileInternal failed", e)
        }
    }

    /**
     * 从文件存储恢复歌单数据到 Room。
     * @param force true 时忽略 Room 现有数据，强制从文件读取覆盖（切换路径用）
     */
    suspend fun syncFromFileIfNeeded(force: Boolean = false) {
        try {
            val existing = dao.snapshotPlaylistsWithCount()
            val hasNormalPlaylists = existing.any { it.playlist.kind == PlaylistEntity.KIND_NORMAL }
            android.util.Log.d("PlaylistRepository", "syncFromFile: Room has ${existing.size} playlists, hasNormal=$hasNormalPlaylists, force=$force")
            if (hasNormalPlaylists && !force) {
                android.util.Log.d("PlaylistRepository", "syncFromFile: skip, Room already has data")
                return
            }
            if (force) {
                // 强制模式：清空 Room 中所有歌单（含喜欢/忽略），级联删除条目，再从文件完整恢复
                android.util.Log.d("PlaylistRepository", "syncFromFile: force mode, clearing all playlists")
                dao.deleteAllPlaylists()
            }

            // 直接读文件，绕过 readJsonFile 的默认值回退逻辑
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "playlists.json")
            android.util.Log.d("PlaylistRepository", "syncFromFile: reading ${file.absolutePath}, exists=${file.exists()}")
            if (!file.exists()) {
                android.util.Log.w("PlaylistRepository", "syncFromFile: file not found")
                return
            }
            val rawJson = file.readText()
            android.util.Log.d("PlaylistRepository", "syncFromFile: raw JSON (${rawJson.length} bytes): ${rawJson.take(300)}")
            if (rawJson.isBlank() || rawJson == "{\"playlists\":[]}") {
                android.util.Log.w("PlaylistRepository", "syncFromFile: file is empty or default")
                return
            }
            val fileData = try {
                com.google.gson.Gson().fromJson(rawJson, com.joshua.videoplayer.data.PlaylistsJson::class.java)
            } catch (e: Exception) {
                android.util.Log.e("PlaylistRepository", "syncFromFile: JSON parse error", e)
                null
            }
            if (fileData == null || fileData.playlists.isEmpty()) {
                android.util.Log.w("PlaylistRepository", "syncFromFile: parsed data is null or empty")
                return
            }

            android.util.Log.d("PlaylistRepository", "syncFromFile: restoring ${fileData.playlists.size} playlists from file")

            for (pj in fileData.playlists) {
                when (pj.kind) {
                    PlaylistEntity.KIND_LIKED -> {
                        // 喜欢歌单已由 Room callback 自动创建，只需恢复条目
                        val likedId = ensureLikedPlaylistId()
                        for (item in pj.items) {
                            if (dao.countItemsInPlaylistWithUri(likedId, item.contentUri) == 0) {
                                dao.insertItem(
                                    PlaylistItemEntity(
                                        playlistId = likedId,
                                        contentUri = item.contentUri,
                                        displayTitle = item.displayTitle,
                                        durationMs = item.durationMs,
                                        sortOrder = item.sortOrder,
                                    ),
                                )
                            }
                        }
                    }
                    PlaylistEntity.KIND_IGNORED -> {
                        val ignoredId = ensureIgnoredPlaylistId()
                        for (item in pj.items) {
                            if (dao.countItemsInPlaylistWithUri(ignoredId, item.contentUri) == 0) {
                                dao.insertItem(
                                    PlaylistItemEntity(
                                        playlistId = ignoredId,
                                        contentUri = item.contentUri,
                                        displayTitle = item.displayTitle,
                                        durationMs = item.durationMs,
                                        sortOrder = item.sortOrder,
                                    ),
                                )
                            }
                        }
                    }
                    else -> {
                        // 普通歌单：创建并恢复所有条目
                        val newId = dao.insertPlaylist(
                            PlaylistEntity(
                                name = pj.name.ifBlank { "未命名歌单" },
                                kind = PlaylistEntity.KIND_NORMAL,
                                coverImageUri = pj.coverImageUri,
                            ),
                        )
                        for (item in pj.items) {
                            dao.insertItem(
                                PlaylistItemEntity(
                                    playlistId = newId,
                                    contentUri = item.contentUri,
                                    displayTitle = item.displayTitle,
                                    durationMs = item.durationMs,
                                    sortOrder = item.sortOrder,
                                ),
                            )
                        }
                    }
                }
            }
            android.util.Log.d("PlaylistRepository", "File restore complete")
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to restore playlists from file", e)
        }
    }

    /** 播放失败时，从所有歌单、收藏、忽略中移除该 URI（缓存+文件同步） */
    suspend fun removeUriEverywhere(contentUri: String) {
        try {
            // 从所有歌单中移除
            val playlists = dao.snapshotPlaylistsWithCount()
            for (pw in playlists) {
                if (dao.countItemsInPlaylistWithUri(pw.playlist.id, contentUri) > 0) {
                    dao.deleteItemsInPlaylistWithContentUri(pw.playlist.id, contentUri)
                    android.util.Log.d("PlaylistRepository", "Removed URI from playlist ${pw.playlist.name}")
                }
            }
            // 从收藏中移除
            libraryActions.unfavoriteUri(contentUri)
            // 从忽略中移除
            dao.getPlaylistIdByKind(PlaylistEntity.KIND_IGNORED)?.let { ignoredId ->
                dao.deleteItemsInPlaylistWithContentUri(ignoredId, contentUri)
            }
            // 同步到文件
            syncPlaylistsToFile()
            android.util.Log.d("PlaylistRepository", "Removed invalid URI from everywhere: $contentUri")
        } catch (e: Exception) {
            android.util.Log.e("PlaylistRepository", "Failed to remove URI everywhere", e)
        }
    }

    companion object {
        private const val LIKED_PLAYLIST_NAME = "喜欢"
        private const val IGNORED_PLAYLIST_NAME = "忽略"
        private const val UNNAMED_PLAYLIST_NAME = "未命名歌单"

        /** 获取系统播放列表的本地化显示名称 */
        fun getLocalizedPlaylistName(context: Context, playlistName: String): String {
            return when (playlistName) {
                LIKED_PLAYLIST_NAME -> context.getString(R.string.system_playlist_liked)
                IGNORED_PLAYLIST_NAME -> context.getString(R.string.system_playlist_ignored)
                UNNAMED_PLAYLIST_NAME -> context.getString(R.string.default_unnamed_playlist)
                else -> playlistName
            }
        }

        /** 获取本地化的「喜欢」歌单名称 */
        fun getLikedDisplayName(context: Context): String = context.getString(R.string.system_playlist_liked)

        /** 获取本地化的「忽略」歌单名称 */
        fun getIgnoredDisplayName(context: Context): String = context.getString(R.string.system_playlist_ignored)

        /** 获取本地化的未命名歌单默认名称 */
        fun getUnnamedDisplayName(context: Context): String = context.getString(R.string.default_unnamed_playlist)
    }
}
