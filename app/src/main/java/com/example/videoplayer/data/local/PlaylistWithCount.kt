package com.example.videoplayer.data.local

import androidx.room.Embedded

/**
 * 歌单列表行：附带曲目数量（Room 查询投影）。
 */
data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val itemCount: Int,
)
