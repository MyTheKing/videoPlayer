package com.joshua.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit

object SleepTimerManager {
    private const val PREFS_NAME = "videoPlayer_sleep_timer"
    private const val KEY_ENABLED = "timer_enabled"
    private const val KEY_DURATION_MS = "timer_duration_ms"

    var timerEnabled by mutableStateOf(false)
        private set

    var timerDurationMs by mutableLongStateOf(0L)
        private set

    var remainingMs by mutableLongStateOf(0L)
        private set

    var isRunning by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        timerEnabled = prefs?.getBoolean(KEY_ENABLED, false) ?: false
        timerDurationMs = prefs?.getLong(KEY_DURATION_MS, 0L) ?: 0L
        if (timerEnabled && timerDurationMs > 0) {
            remainingMs = timerDurationMs
            isRunning = true
        }
    }

    fun setEnabled(enabled: Boolean) {
        timerEnabled = enabled
        prefs?.edit { putBoolean(KEY_ENABLED, enabled) }
        if (!enabled) {
            stop()
        }
    }

    fun setDuration(durationMs: Long) {
        timerDurationMs = durationMs
        prefs?.edit { putLong(KEY_DURATION_MS, durationMs) }
    }

    fun start() {
        if (timerDurationMs <= 0L) {
            timerDurationMs = DEFAULT_DURATION_MS
            prefs?.edit { putLong(KEY_DURATION_MS, DEFAULT_DURATION_MS) }
        }
        remainingMs = timerDurationMs
        isRunning = true
    }

    fun stop() {
        isRunning = false
        remainingMs = 0L
    }

    /** 每秒调用一次，返回 true 表示倒计时结束 */
    fun tick(): Boolean {
        if (!isRunning) return false
        remainingMs = (remainingMs - 1000L).coerceAtLeast(0L)
        if (remainingMs <= 0L) {
            clearAfterTimeout()
            return true
        }
        return false
    }

    /** 倒计时结束后自动清除，恢复到关闭状态 */
    fun clearAfterTimeout() {
        isRunning = false
        remainingMs = 0L
        timerEnabled = false
        timerDurationMs = DEFAULT_DURATION_MS
        prefs?.edit {
            putBoolean(KEY_ENABLED, false)
            putLong(KEY_DURATION_MS, DEFAULT_DURATION_MS)
        }
    }

    private const val DEFAULT_DURATION_MS = 30 * 60 * 1000L // 30 分钟

    fun formatRemaining(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}
