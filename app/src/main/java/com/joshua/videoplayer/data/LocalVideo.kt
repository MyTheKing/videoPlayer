package com.joshua.videoplayer.data

import android.net.Uri

/**
 * MediaStore 扫描得到的本地视频摘要（用于列表与歌单添加）。
 *
 * [id] 仅为**当前卷内**的 [MediaStore.Video.Media._ID]，跨存储卷会重复。
 * 同一物理文件在不同 MediaStore 集合中可能对应**不同** [contentUri]，列表展示与 Room 等仍以 Uri 为准，
 * 扫描合并时需按路径/[relativePath] 去重（见 [com.joshua.videoplayer.data.queryLocalVideos]）。
 */
data class LocalVideo(
    val id: Long,
    val contentUri: Uri,
    val displayName: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    /** MediaStore 的 `_DATA`；部分 ROM 上比 content [Uri] 更易被框架解析。 */
    val absolutePath: String? = null,
    /** API 29+ [MediaStore.MediaColumns.RELATIVE_PATH]，与 [displayName] 可组成稳定逻辑路径用于跨卷去重。 */
    val relativePath: String? = null,
)
