package com.joshua.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.joshua.videoplayer.data.DurationFilterJson

/**
 * 时长过滤管理器
 * 负责时长过滤设置的持久化和动态切换
 */
object DurationFilterManager {
    private const val PREFS_NAME = "videoPlayer_duration_filter"
    private const val KEY_FILTER_ENABLED = "filter_enabled"
    private const val KEY_MIN_DURATION_MS = "min_duration_ms"
    private const val KEY_MAX_DURATION_MS = "max_duration_ms"

    // 当前过滤设置状态
    var filterEnabled by mutableStateOf(false)
        private set

    var minDurationMs by mutableStateOf(0L)
        private set

    var maxDurationMs by mutableStateOf(0L)
        private set

    private var prefs: SharedPreferences? = null

    private var initialized = false

    /**
     * 初始化，优先从文件存储加载，其次从 SharedPreferences 加载
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (initialized) return
        initialized = true
        loadFromFileOrPrefs()
    }

    /**
     * 首次授权存储权限时调用：SP 有数据 → 覆盖文件；SP 无数据 → 从文件读取。
     */
    fun bidirectionalSync() {
        val spHasData = prefs?.contains(KEY_FILTER_ENABLED) == true
        Log.d("DurationFilterManager", "bidirectionalSync: SP hasData=$spHasData")
        if (spHasData) {
            loadSettings()
            FileStorageManager.writeDurationFilter(
                DurationFilterJson(filterEnabled, minDurationMs, maxDurationMs)
            )
        } else {
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "duration_filter.json")
            if (!file.exists()) return
            val fileData = try {
                com.google.gson.Gson().fromJson(file.readText(), DurationFilterJson::class.java)
            } catch (_: Exception) { null }
            if (fileData != null && (fileData.filterEnabled || fileData.minDurationMs > 0 || fileData.maxDurationMs > 0)) {
                filterEnabled = fileData.filterEnabled
                minDurationMs = fileData.minDurationMs
                maxDurationMs = fileData.maxDurationMs
                prefs?.edit {
                    putBoolean(KEY_FILTER_ENABLED, fileData.filterEnabled)
                    putLong(KEY_MIN_DURATION_MS, fileData.minDurationMs)
                    putLong(KEY_MAX_DURATION_MS, fileData.maxDurationMs)
                }
            }
        }
    }

    /** 切换存储路径时调用：新文件夹有数据则读文件覆盖 SP，无数据则 SP 写入文件 */
    fun syncOnPathSwitch(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dir = FileStorageManager.getStorageDir()
        val file = java.io.File(dir, "duration_filter.json")
        val hasFileData = try {
            if (file.exists()) {
                val raw = file.readText()
                raw.isNotBlank() && raw != "{}"
            } else false
        } catch (_: Exception) { false }

        if (hasFileData) {
            Log.d("DurationFilterManager", "syncOnPathSwitch: file→SP")
            val fileData = try {
                com.google.gson.Gson().fromJson(file.readText(), DurationFilterJson::class.java)
            } catch (_: Exception) { null }
            if (fileData != null) {
                filterEnabled = fileData.filterEnabled
                minDurationMs = fileData.minDurationMs
                maxDurationMs = fileData.maxDurationMs
                prefs?.edit {
                    putBoolean(KEY_FILTER_ENABLED, fileData.filterEnabled)
                    putLong(KEY_MIN_DURATION_MS, fileData.minDurationMs)
                    putLong(KEY_MAX_DURATION_MS, fileData.maxDurationMs)
                }
            }
        } else {
            Log.d("DurationFilterManager", "syncOnPathSwitch: SP→file")
            loadSettings()
            FileStorageManager.writeDurationFilter(
                DurationFilterJson(filterEnabled, minDurationMs, maxDurationMs)
            )
        }
    }

    /**
     * 双缓存加载：先读 SP，SP 无数据时从文件补回。
     */
    private fun loadFromFileOrPrefs() {
        // 1. 先读 SP（缓存）
        loadSettings()
        val spHasData = prefs?.contains(KEY_FILTER_ENABLED) == true
        Log.d("DurationFilterManager", "load: SP hasData=$spHasData, enabled=$filterEnabled, min=$minDurationMs, max=$maxDurationMs")

        if (spHasData) {
            // SP 有数据，同步到文件
            FileStorageManager.writeDurationFilter(
                DurationFilterJson(filterEnabled, minDurationMs, maxDurationMs)
            )
            return
        }

        // 2. SP 无数据（升级/重装），从文件补回
        try {
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "duration_filter.json")
            Log.d("DurationFilterManager", "load: SP empty, try file ${file.absolutePath}, exists=${file.exists()}")
            if (!file.exists()) return
            val rawJson = file.readText()
            val fileData = try {
                com.google.gson.Gson().fromJson(rawJson, DurationFilterJson::class.java)
            } catch (e: Exception) {
                Log.e("DurationFilterManager", "load: file parse error", e)
                null
            }
            if (fileData != null && (fileData.filterEnabled || fileData.minDurationMs > 0 || fileData.maxDurationMs > 0)) {
                filterEnabled = fileData.filterEnabled
                minDurationMs = fileData.minDurationMs
                maxDurationMs = fileData.maxDurationMs
                Log.d("DurationFilterManager", "load: restored from file, enabled=$filterEnabled, min=$minDurationMs, max=$maxDurationMs")
                // 同步回 SP
                prefs?.edit {
                    putBoolean(KEY_FILTER_ENABLED, fileData.filterEnabled)
                    putLong(KEY_MIN_DURATION_MS, fileData.minDurationMs)
                    putLong(KEY_MAX_DURATION_MS, fileData.maxDurationMs)
                }
            }
        } catch (e: Exception) {
            Log.e("DurationFilterManager", "load: file fallback failed", e)
        }
    }

    /**
     * 加载保存的设置
     */
    private fun loadSettings() {
        filterEnabled = prefs?.getBoolean(KEY_FILTER_ENABLED, false) ?: false
        minDurationMs = prefs?.getLong(KEY_MIN_DURATION_MS, 0L) ?: 0L
        maxDurationMs = prefs?.getLong(KEY_MAX_DURATION_MS, 0L) ?: 0L
    }

    /**
     * 设置过滤开关
     */
    fun updateFilterEnabled(enabled: Boolean) {
        filterEnabled = enabled
        saveSettings()
    }

    /**
     * 设置最小时长
     * @param value 时长数值
     * @param unit 时长单位枚举
     */
    fun setMinDuration(value: String, unit: DurationUnit) {
        minDurationMs = parseDurationToMs(value, unit)
        saveSettings()
    }

    /**
     * 设置最大时长
     * @param value 时长数值
     * @param unit 时长单位枚举
     */
    fun setMaxDuration(value: String, unit: DurationUnit) {
        maxDurationMs = parseDurationToMs(value, unit)
        saveSettings()
    }

    /**
     * 将时长字符串转换为毫秒
     */
    private fun parseDurationToMs(value: String, unit: DurationUnit): Long {
        val numValue = value.toLongOrNull() ?: return 0L
        return numValue * unit.multiplier
    }

    /**
     * 将毫秒转换为 (数值, 单位枚举) 对
     */
    fun msToDisplayValue(ms: Long, defaultUnit: DurationUnit = DurationUnit.SECONDS): Pair<String, DurationUnit> {
        return DurationUnit.msToDisplayValue(ms, defaultUnit)
    }

    /**
     * 保存设置到 SharedPreferences 和文件存储
     */
    private fun saveSettings() {
        prefs?.edit {
            putBoolean(KEY_FILTER_ENABLED, filterEnabled)
            putLong(KEY_MIN_DURATION_MS, minDurationMs)
            putLong(KEY_MAX_DURATION_MS, maxDurationMs)
        }
        Log.d("DurationFilterManager", "saveSettings: enabled=$filterEnabled, min=$minDurationMs, max=$maxDurationMs")
        FileStorageManager.debounceWriteDurationFilter(
            DurationFilterJson(filterEnabled, minDurationMs, maxDurationMs)
        )
    }

    /**
     * 检查视频是否符合时长过滤条件
     */
    fun matchesFilter(durationMs: Long): Boolean {
        if (!filterEnabled) return true
        if (minDurationMs > 0 && durationMs < minDurationMs) return false
        if (maxDurationMs > 0 && durationMs > maxDurationMs) return false
        return true
    }
}
