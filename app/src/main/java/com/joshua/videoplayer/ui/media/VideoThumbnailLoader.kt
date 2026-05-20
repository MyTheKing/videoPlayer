package com.joshua.videoplayer.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 与系统相册类似：优先走 [android.content.ContentResolver.loadThumbnail]（Android 10+），
 * 失败或低版本再用 [MediaMetadataRetriever] 抽帧，避免 Coil 对 content:// 视频只显示占位。
 */
suspend fun loadVideoThumbnail(
    context: Context,
    uri: Uri,
    width: Int = 720,
    height: Int = 480,
): Bitmap? = withContext(Dispatchers.IO) {
    val maxSide = maxOf(width, height).coerceIn(240, 1280)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            context.contentResolver.loadThumbnail(uri, Size(width, height), null)
        }.getOrNull()?.let { return@withContext scaleDownIfNeeded(it, maxSide) }
    }
    loadFrameWithRetriever(context, uri, maxSide)
}

private fun loadFrameWithRetriever(context: Context, uri: Uri, maxSide: Int): Bitmap? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        // 取约 1s 处帧，减少全黑片头
        val bmp = r.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: r.getFrameAtTime(0L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        bmp?.let { scaleDownIfNeeded(it, maxSide) }
    } catch (_: Exception) {
        null
    } finally {
        try {
            r.release()
        } catch (_: Exception) {
        }
    }
}

private fun scaleDownIfNeeded(src: Bitmap, maxSide: Int): Bitmap {
    val w = src.width
    val h = src.height
    if (w <= maxSide && h <= maxSide) return src
    val scale = maxSide.toFloat() / maxOf(w, h)
    val nw = (w * scale).toInt().coerceAtLeast(1)
    val nh = (h * scale).toInt().coerceAtLeast(1)
    val out = Bitmap.createScaledBitmap(src, nw, nh, true)
    if (out != src) {
        src.recycle()
    }
    return out
}
