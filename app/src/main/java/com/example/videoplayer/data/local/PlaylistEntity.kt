package com.example.videoplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自建歌单（对应文档「学习列表、运动歌单」等）。
 *
 * @property name 展示名称
 * @property createdAtMillis 创建时间，用于默认排序
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)
