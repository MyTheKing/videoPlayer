package com.example.videoplayer.player.service

import android.util.Log
import com.example.videoplayer.BuildConfig

/**
 * Debug 包下输出 seek 诊断；过滤 Logcat 标签：**KotlinDemoSeek**
 *
 * 抓取示例：`adb logcat -s KotlinDemoSeek:D`
 */
internal object PlaybackSeekDiagnostics {
    const val TAG = "KotlinDemoSeek"

    fun log(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }
}
