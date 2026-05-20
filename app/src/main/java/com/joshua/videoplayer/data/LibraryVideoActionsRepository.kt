package com.joshua.videoplayer.data

import com.joshua.videoplayer.data.local.FavoriteVideoUriEntity
import com.joshua.videoplayer.data.local.IgnoredVideoUriEntity
import com.joshua.videoplayer.data.local.LibraryVideoActionsDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LibraryVideoActionsRepository(private val dao: LibraryVideoActionsDao) {

    private val fileSyncScope = CoroutineScope(Dispatchers.IO)

    @Volatile
    private var suppressFileWrite = false

    fun observeIgnoredUris(): Flow<Set<String>> =
        dao.observeIgnoredUris().map { it.toSet() }

    fun observeFavoriteUris(): Flow<Set<String>> =
        dao.observeFavoriteUris().map { it.toSet() }

    suspend fun ignoreUri(uri: String) {
        dao.addIgnored(IgnoredVideoUriEntity(contentUri = uri))
        syncIgnoredToFile()
    }

    suspend fun favoriteUri(uri: String) {
        dao.addFavorite(FavoriteVideoUriEntity(contentUri = uri))
        syncFavoritesToFile()
    }

    suspend fun unfavoriteUri(uri: String) {
        dao.removeFavorite(uri)
        syncFavoritesToFile()
    }

    private fun syncFavoritesToFile() {
        if (suppressFileWrite) return
        fileSyncScope.launch {
            try {
                val uris = dao.snapshotFavoriteUris()
                android.util.Log.d("LibraryVideoActionsRepo", "Syncing ${uris.size} favorites to file")
                FileStorageManager.debounceWriteFavorites(FavoritesJson(uris))
            } catch (e: Exception) {
                android.util.Log.e("LibraryVideoActionsRepo", "Failed to sync favorites to file", e)
            }
        }
    }

    private fun syncIgnoredToFile() {
        if (suppressFileWrite) return
        fileSyncScope.launch {
            try {
                val uris = dao.snapshotIgnoredUris()
                android.util.Log.d("LibraryVideoActionsRepo", "Syncing ${uris.size} ignored to file")
                FileStorageManager.debounceWriteIgnored(IgnoredJson(uris))
            } catch (e: Exception) {
                android.util.Log.e("LibraryVideoActionsRepo", "Failed to sync ignored to file", e)
            }
        }
    }

    /**
     * 双缓存同步（默认文件夹）：缓存有数据 → 覆盖文件；缓存无数据 → 从文件恢复。
     */
    suspend fun bidirectionalSync() {
        val existingFavs = dao.snapshotFavoriteUris()
        val existingIgnored = dao.snapshotIgnoredUris()
        if (existingFavs.isNotEmpty() || existingIgnored.isNotEmpty()) {
            android.util.Log.d("LibraryVideoActionsRepo", "bidirectionalSync: cache→file (${existingFavs.size} favs, ${existingIgnored.size} ignored)")
            syncFavoritesToFile()
            syncIgnoredToFile()
        } else {
            android.util.Log.d("LibraryVideoActionsRepo", "bidirectionalSync: file→cache")
            syncFromFileIfNeeded()
        }
    }

    /**
     * 切换存储路径同步：文件有内容 → 读文件覆盖缓存；文件无内容 → 缓存写入文件。
     */
    suspend fun syncOnPathSwitch() {
        suppressFileWrite = true
        try {
            val dir = FileStorageManager.getStorageDir()
            val favFile = java.io.File(dir, "favorites.json")
            val ignFile = java.io.File(dir, "ignored.json")
            val hasFileData = try {
                (favFile.exists() && favFile.readText().let { it.isNotBlank() && it != "{\"uris\":[]}" }) ||
                (ignFile.exists() && ignFile.readText().let { it.isNotBlank() && it != "{\"uris\":[]}" })
            } catch (_: Exception) { false }

            if (hasFileData) {
                android.util.Log.d("LibraryVideoActionsRepo", "syncOnPathSwitch: file→cache (new folder has data)")
                syncFromFileIfNeeded(force = true)
            } else {
                android.util.Log.d("LibraryVideoActionsRepo", "syncOnPathSwitch: cache→file (new folder empty)")
            }
            // 同步写回确认文件内容正确
            val favs = dao.snapshotFavoriteUris()
            val ignored = dao.snapshotIgnoredUris()
            FileStorageManager.writeFavorites(FavoritesJson(favs))
            FileStorageManager.writeIgnored(IgnoredJson(ignored))
        } finally {
            suppressFileWrite = false
        }
    }

    /**
     * 从文件存储恢复收藏和忽略数据到 Room。
     * @param force true 时清空 Room 后从文件读取覆盖（切换路径用）
     */
    suspend fun syncFromFileIfNeeded(force: Boolean = false) {
        try {
            val existingFavs = dao.snapshotFavoriteUris()
            val existingIgnored = dao.snapshotIgnoredUris()
            android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: Room has ${existingFavs.size} favs, ${existingIgnored.size} ignored, force=$force")

            if (force) {
                // 强制模式：清空后从文件完整恢复
                android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: force mode, clearing all")
                dao.deleteAllFavorites()
                dao.deleteAllIgnored()
            }

            val shouldReadFavs = force || existingFavs.isEmpty()
            val shouldReadIgnored = force || existingIgnored.isEmpty()

            if (shouldReadFavs) {
                val fileFavs = FileStorageManager.readFavorites()
                android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: file has ${fileFavs.uris.size} favs")
                if (fileFavs.uris.isNotEmpty()) {
                    android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: restoring ${fileFavs.uris.size} favorites")
                    for (uri in fileFavs.uris) {
                        dao.addFavorite(FavoriteVideoUriEntity(contentUri = uri))
                    }
                }
            }

            if (shouldReadIgnored) {
                val fileIgnored = FileStorageManager.readIgnored()
                android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: file has ${fileIgnored.uris.size} ignored")
                if (fileIgnored.uris.isNotEmpty()) {
                    android.util.Log.d("LibraryVideoActionsRepo", "syncFromFile: restoring ${fileIgnored.uris.size} ignored")
                    for (uri in fileIgnored.uris) {
                        dao.addIgnored(IgnoredVideoUriEntity(contentUri = uri))
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LibraryVideoActionsRepo", "syncFromFile failed", e)
        }
    }
}
