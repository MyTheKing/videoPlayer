package com.joshua.videoplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryVideoActionsDao {

    @Query("SELECT contentUri FROM ignored_video_uris")
    fun observeIgnoredUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addIgnored(entity: IgnoredVideoUriEntity)

    @Query("DELETE FROM ignored_video_uris WHERE contentUri = :uri")
    suspend fun removeIgnored(uri: String)

    @Query("SELECT contentUri FROM favorite_video_uris")
    fun observeFavoriteUris(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteVideoUriEntity)

    @Query("DELETE FROM favorite_video_uris WHERE contentUri = :uri")
    suspend fun removeFavorite(uri: String)

    @Query("SELECT contentUri FROM favorite_video_uris")
    suspend fun snapshotFavoriteUris(): List<String>

    @Query("SELECT contentUri FROM ignored_video_uris")
    suspend fun snapshotIgnoredUris(): List<String>

    @Query("DELETE FROM favorite_video_uris")
    suspend fun deleteAllFavorites()

    @Query("DELETE FROM ignored_video_uris")
    suspend fun deleteAllIgnored()
}
