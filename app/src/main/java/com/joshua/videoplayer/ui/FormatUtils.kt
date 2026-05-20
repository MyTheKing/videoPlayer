package com.joshua.videoplayer.ui

import androidx.media3.common.C
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 将毫秒时长格式化为 m:ss，供列表与迷你条展示。 */
fun formatDurationMs(ms: Long): String {
    if (ms <= 0L || ms == C.TIME_UNSET) return "-"
    val totalSec = ms / 1000L
    val m = totalSec / 60L
    val s = totalSec % 60L
    return String.format(Locale.getDefault(), "%d:%02d", m, s)
}

/** 将「秒级时间戳」转为简短日期（用于 DATE_ADDED）。 */
fun formatDateAddedSeconds(sec: Long): String {
    if (sec <= 0L) return "—"
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date(sec * 1000L))
}

fun formatSizeBytes(bytes: Long): String {
    if (bytes <= 0L) return "—"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 1024) {
        String.format(Locale.getDefault(), "%.1f MB", mb)
    } else {
        String.format(Locale.getDefault(), "%.2f GB", mb / 1024.0)
    }
}
