package com.example.videoplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Query(
        """
        SELECT playlists.*,
        (SELECT COUNT(*) FROM playlist_items WHERE playlist_items.playlistId = playlists.id) AS itemCount
        FROM playlists
        ORDER BY playlists.createdAtMillis DESC
        """,
    )
    fun observePlaylistsWithCount(): Flow<List<PlaylistWithCount>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC, id ASC")
    fun observeItems(playlistId: Long): Flow<List<PlaylistItemEntity>>

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY sortOrder ASC, id ASC")
    suspend fun snapshotItems(playlistId: Long): List<PlaylistItemEntity>

    @Query("UPDATE playlist_items SET durationMs = :durationMs WHERE id = :itemId")
    suspend fun updateItemDuration(itemId: Long, durationMs: Long)

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertItem(item: PlaylistItemEntity): Long

    @Query("DELETE FROM playlist_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Long)

    @Query(
        """
        SELECT DISTINCT playlistId FROM playlist_items
        WHERE contentUri = :contentUri
        """,
    )
    fun observePlaylistIdsContainingContentUri(contentUri: String): Flow<List<Long>>

    @Query(
        """
        SELECT COUNT(*) FROM playlist_items
        WHERE playlistId = :playlistId AND contentUri = :contentUri
        """,
    )
    suspend fun countItemsInPlaylistWithUri(playlistId: Long, contentUri: String): Int

    @Query(
        """
        DELETE FROM playlist_items
        WHERE playlistId = :playlistId AND contentUri = :contentUri
        """,
    )
    suspend fun deleteItemsInPlaylistWithContentUri(playlistId: Long, contentUri: String)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun maxSortOrder(playlistId: Long): Int?

    @Query("UPDATE playlists SET name = :newName WHERE id = :playlistId")
    suspend fun updatePlaylistName(playlistId: Long, newName: String)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)
}
