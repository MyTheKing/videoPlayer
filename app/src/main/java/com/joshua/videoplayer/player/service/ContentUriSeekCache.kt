package com.joshua.videoplayer.player.service

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * 部分 `content://` 分片 MP4 在 Exo 上 `isSeekable=false` 且 seek 无效；复制到应用私有缓存后以 `file://`
 * 打开通常可随机访问。单线程复制，结果回主线程。
 */
internal object ContentUriSeekCache {

    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ContentUriSeekCache").apply { isDaemon = true }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    /** 超过则拒绝缓存，避免占满磁盘（字节）。 */
    private const val MAX_BYTES = 900L * 1024 * 1024

    fun ensureLocalFile(
        context: Context,
        contentUri: Uri,
        callback: (Result<File>) -> Unit,
    ) {
        val app = context.applicationContext
        io.execute {
            try {
                val size = queryOpenableSize(app, contentUri)
                if (size > MAX_BYTES) {
                    throw IllegalStateException("skip cache: size=${size}b > max")
                }
                val dir = File(app.cacheDir, "seek_content_cache").apply { mkdirs() }
                val name = "${abs(contentUri.toString().hashCode())}.mp4"
                val out = File(dir, name)
                if (out.exists() && out.length() > 0L) {
                    val hit = when {
                        size > 0L -> out.length() == size
                        else -> true
                    }
                    if (hit) {
                        mainHandler.post { callback(Result.success(out)) }
                        return@execute
                    }
                    out.delete()
                }
                app.contentResolver.openInputStream(contentUri)?.use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("openInputStream null")
                mainHandler.post { callback(Result.success(out)) }
            } catch (e: Exception) {
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    private fun queryOpenableSize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (!c.moveToFirst()) return@use -1L
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (idx < 0 || c.isNull(idx)) return@use -1L
            c.getLong(idx)
        } ?: -1L
    }
}
