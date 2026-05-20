package com.joshua.videoplayer.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自建歌单（对应文档「学习列表、运动歌单」等）。
 *
 * @property name 展示名称
 * @property createdAtMillis 创建时间，用于默认排序
 * @property kind [KIND_NORMAL] 或系统内置 [KIND_LIKED]（「喜欢」歌单，不可删、不可自定义封面）或 [KIND_IGNORED]（「忽略」歌单）
 * @property coverImageUri 用户自选封面图（content Uri 字符串）；喜欢歌单忽略此字段
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "kind", defaultValue = "0") val kind: Int = KIND_NORMAL,
    @ColumnInfo(name = "coverImageUri") val coverImageUri: String? = null,
) {
    companion object {
        const val KIND_NORMAL = 0
        const val KIND_LIKED = 1
        const val KIND_IGNORED = 2
    }
}
