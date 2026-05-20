package com.joshua.videoplayer.data.local

import androidx.room.Embedded

/**
 * 歌单列表行：附带曲目数量（Room 查询投影）。
 *
 * @property firstItemContentUri 列表内首条视频 Uri，用于默认封面缩略图
 */
data class PlaylistWithCount(
    @Embedded val playlist: PlaylistEntity,
    val itemCount: Int,
    val firstItemContentUri: String?,
)
