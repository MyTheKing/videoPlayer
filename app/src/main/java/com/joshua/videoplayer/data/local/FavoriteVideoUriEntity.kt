package com.joshua.videoplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 「我喜欢」收藏（仅存 Uri，与歌单独立）。 */
@Entity(tableName = "favorite_video_uris")
data class FavoriteVideoUriEntity(
    @PrimaryKey val contentUri: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
)
