package com.joshua.videoplayer.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从系统 [MediaStore] 读取设备上的视频条目（需已授予读视频权限）。
 *
 * Android 10+ 上不同存储卷（内置、SD 卡等）有各自的 [MediaStore] Uri，
 * 仅查 [MediaStore.VOLUME_EXTERNAL] 会漏掉部分视频，故合并多卷查询。
 * 同一文件在不同集合中可能对应不同 content Uri（不同 _ID），按 `_DATA` 或
 * `RELATIVE_PATH`+`DISPLAY_NAME` 去重后再展示。
 */
suspend fun Context.queryLocalVideos(): List<LocalVideo> = withContext(Dispatchers.IO) {
    val ctx = this@queryLocalVideos
    val collections = LinkedHashSet<Uri>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        collections.add(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL))
        for (volume in MediaStore.getExternalVolumeNames(ctx)) {
            collections.add(MediaStore.Video.Media.getContentUri(volume))
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        collections.add(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL))
    } else {
        @Suppress("DEPRECATION")
        collections.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
    }

    val merged = LinkedHashMap<String, LocalVideo>()
    for (collection in collections) {
        ctx.queryVideosSingleCollection(collection).forEach { video ->
            val key = video.scanDedupKey()
            val existing = merged[key]
            merged[key] = when {
                existing == null -> video
                else -> mergeScanDuplicate(existing, video)
            }
        }
    }
    merged.values.sortedByDescending { it.dateAddedSec }
}

/** 跨卷扫描时同一文件可能有多条 MediaStore 行，优先用绝对路径再相对路径+文件名去重。 */
private fun LocalVideo.scanDedupKey(): String {
    absolutePath?.trim()?.takeIf { it.isNotEmpty() }?.let { return "path:$it" }
    relativePath?.trim()?.takeIf { it.isNotEmpty() }?.let { rp ->
        val name = displayName.trim()
        if (name.isNotEmpty()) {
            val folder = rp.trimEnd('/')
            return "rel:$folder/$name"
        }
    }
    return "uri:${contentUri}"
}

private fun mergeScanDuplicate(a: LocalVideo, b: LocalVideo): LocalVideo {
    fun score(v: LocalVideo): Int {
        var s = 0
        if (!v.absolutePath.isNullOrBlank()) s += 4
        if (!v.relativePath.isNullOrBlank()) s += 2
        if (v.durationMs > 0L) s += 1
        if (v.sizeBytes > 0L) s += 1
        return s
    }
    return if (score(b) > score(a)) b else a
}

@Suppress("DEPRECATION")
private fun Context.queryVideosSingleCollection(collection: Uri): List<LocalVideo> {
    val projection = buildList {
        add(MediaStore.Video.Media._ID)
        add(MediaStore.Video.Media.DISPLAY_NAME)
        add(MediaStore.Video.Media.DURATION)
        add(MediaStore.Video.Media.SIZE)
        add(MediaStore.Video.Media.DATE_ADDED)
        add(MediaStore.Video.Media.DATA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(MediaStore.Video.Media.RELATIVE_PATH)
        }
    }.toTypedArray()
    val sort = "${MediaStore.Video.Media.DATE_ADDED} DESC"
    val list = mutableListOf<LocalVideo>()
    contentResolver.query(collection, projection, null, null, sort)?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
        val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
        val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
        val pathCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)
        val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
        } else {
            -1
        }
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val name = cursor.getString(nameCol) ?: continue
            val duration = if (cursor.isNull(durCol)) 0L else cursor.getLong(durCol)
            val size = if (cursor.isNull(sizeCol)) 0L else cursor.getLong(sizeCol)
            val dateAdded = if (cursor.isNull(dateCol)) 0L else cursor.getLong(dateCol)
            val path = if (pathCol < 0 || cursor.isNull(pathCol)) {
                null
            } else {
                cursor.getString(pathCol)?.takeIf { it.isNotBlank() }
            }
            val relativePath = if (relPathCol < 0 || cursor.isNull(relPathCol)) {
                null
            } else {
                cursor.getString(relPathCol)?.takeIf { it.isNotBlank() }
            }
            val uri = ContentUris.withAppendedId(collection, id)
            list.add(
                LocalVideo(
                    id = id,
                    contentUri = uri,
                    displayName = name,
                    durationMs = duration,
                    sizeBytes = size,
                    dateAddedSec = dateAdded,
                    absolutePath = path,
                    relativePath = relativePath,
                ),
            )
        }
    }
    return list
}

/**
 * 从 [ContentResolver] 读取展示文件名（用于文档选择器返回的 content [Uri]）。
 */
fun Context.readDisplayNameForUri(uri: Uri): String {
    return try {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { return@use it }
                }
            }
            null
        } ?: uri.lastPathSegment
    } catch (_: Exception) {
        uri.lastPathSegment
    } ?: "video"
}
