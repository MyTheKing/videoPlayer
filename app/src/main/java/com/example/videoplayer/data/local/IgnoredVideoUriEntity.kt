package com.example.videoplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 在本地库中「忽略」的视频（按 content Uri 去重）。 */
@Entity(tableName = "ignored_video_uris")
data class IgnoredVideoUriEntity(
    @PrimaryKey val contentUri: String,
    val addedAtMillis: Long = System.currentTimeMillis(),
)
