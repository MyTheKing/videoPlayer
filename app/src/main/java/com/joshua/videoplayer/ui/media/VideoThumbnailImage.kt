package com.joshua.videoplayer.ui.media

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * 展示本地视频的缩略图（相册同款策略见 [loadVideoThumbnail]）。
 * 带简单内存缓存，减少列表快速滑动时重复解码。
 */
@Composable
fun VideoThumbnailImage(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    /** 解码目标像素，越大越清晰、越耗内存 */
    requestWidth: Int = 720,
    requestHeight: Int = 480,
    /** 加载失败或加载中时的占位 */
    showPlayPlaceholder: Boolean = true,
) {
    val context = LocalContext.current
    val key = remember(videoUri) { videoUri.toString() }

    var frame by remember(key) {
        mutableStateOf(ThumbnailMemoryCache.get(key)?.asImageBitmap())
    }

    LaunchedEffect(key, requestWidth, requestHeight) {
        ThumbnailMemoryCache.get(key)?.let {
            frame = it.asImageBitmap()
            return@LaunchedEffect
        }
        val bmp = loadVideoThumbnail(context, videoUri, requestWidth, requestHeight)
        if (bmp != null) {
            ThumbnailMemoryCache.put(key, bmp)
            frame = bmp.asImageBitmap()
        }
    }

    Box(modifier = modifier) {
        if (frame != null) {
            Image(
                bitmap = frame!!,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (showPlayPlaceholder) {
                    Icon(
                        imageVector = Icons.Outlined.PlayCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

/** 进程内 LRU：满员时回收最久未用的 [Bitmap]。 */
private object ThumbnailMemoryCache {
    private const val MAX_ENTRIES = 80

    private val map = object : java.util.LinkedHashMap<String, Bitmap>(MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
            if (size <= MAX_ENTRIES) return false
            eldest?.value?.recycle()
            return true
        }
    }

    @Synchronized
    fun get(key: String): Bitmap? = map[key]

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        map.remove(key)?.recycle()
        map[key] = bitmap
    }
}
