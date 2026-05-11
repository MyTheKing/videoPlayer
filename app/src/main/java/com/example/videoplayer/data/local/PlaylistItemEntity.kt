package com.example.videoplayer.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 歌单内的一条视频引用（仅存 content Uri 与展示信息，便于日后对接远端元数据）。
 *
 * @property sortOrder 列表内顺序，数值越小越靠前
 */
@Entity(
    tableName = "playlist_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("playlistId")],
)
data class PlaylistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val contentUri: String,
    val displayTitle: String,
    val durationMs: Long,
    val sortOrder: Int,
)
