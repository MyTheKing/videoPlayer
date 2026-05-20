package com.joshua.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

object FileStorageManager {
    private const val TAG = "FileStorageManager"
    private const val PREFS_NAME = "videoPlayer_file_storage"
    private const val KEY_CUSTOM_PATH = "custom_path"
    private const val DEBOUNCE_MS = 500L

    private const val FILE_THEME = "theme.json"
    private const val FILE_DURATION_FILTER = "duration_filter.json"
    private const val FILE_FAVORITES = "favorites.json"
    private const val FILE_IGNORED = "ignored.json"
    private const val FILE_PLAYLISTS = "playlists.json"
    private const val DEFAULT_DIR_NAME = "VideoPlayerData"

    private val gson = Gson()
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    private val debounceJobs = mutableMapOf<String, Job>()
    private val pendingWrites = mutableMapOf<String, () -> Unit>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        checkStoragePermission()
        ensureFiles()
    }

    /** 检查是否有外部存储写入权限 */
    private fun checkStoragePermission() {
        val ctx = appContext ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val hasManage = android.os.Environment.isExternalStorageManager()
            Log.d(TAG, "Android 11+ MANAGE_EXTERNAL_STORAGE granted=$hasManage")
            if (!hasManage) {
                Log.w(TAG, "No MANAGE_EXTERNAL_STORAGE permission! File writes will fail on Android 11+")
            }
        } else {
            val hasWrite = androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "Android <11 WRITE_EXTERNAL_STORAGE granted=$hasWrite")
        }
    }

    /** 存储目录是否可写 */
    fun isStorageWritable(): Boolean {
        val dir = getStorageDir()
        if (!dir.exists()) {
            val created = dir.mkdirs()
            if (!created) return false
        }
        val testFile = File(dir, ".write_test")
        return try {
            testFile.writeText("test")
            testFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Storage not writable: ${dir.absolutePath}", e)
            false
        }
    }

    /** 是否需要请求 MANAGE_EXTERNAL_STORAGE 权限（Android 11+） */
    fun needsStoragePermission(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return false
        return !android.os.Environment.isExternalStorageManager()
    }

    /** 跳转到系统设置页面让用户授予「所有文件访问权限」 */
    fun requestManageStoragePermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    android.net.Uri.parse("package:${context.packageName}")
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (e: Exception) {
                // fallback: 打开通用的全部文件访问权限页面
                val intent = android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                )
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    // ── 路径管理 ──

    /** 当前存储目录 */
    fun getStorageDir(): File {
        val custom = prefs?.getString(KEY_CUSTOM_PATH, null)
        if (!custom.isNullOrBlank()) {
            val dir = File(custom)
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
        return getDefaultDir()
    }

    /** 默认路径：/sdcard/Documents/VideoPlayerData/ */
    private fun getDefaultDir(): File {
        val docs = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOCUMENTS
        )
        return File(docs, DEFAULT_DIR_NAME).also { if (!it.exists()) it.mkdirs() }
    }

    /** 当前路径字符串 */
    fun getStorageDirPath(): String {
        return getStorageDir().absolutePath
    }

    /** 是否使用自定义路径 */
    fun isUsingCustomPath(): Boolean {
        return !prefs?.getString(KEY_CUSTOM_PATH, null).isNullOrBlank()
    }

    /** 更改存储路径，迁移文件 */
    fun setStorageDir(newPath: String): Boolean {
        val newDir = File(newPath)
        if (!newDir.exists() && !newDir.mkdirs()) {
            Log.e(TAG, "Failed to create dir: $newPath")
            return false
        }
        val oldDir = getStorageDir()
        prefs?.edit { putString(KEY_CUSTOM_PATH, newPath) }
        migrateFiles(oldDir, newDir)
        return true
    }

    /** 恢复为默认路径 */
    fun resetToDefault(): Boolean {
        val oldDir = getStorageDir()
        prefs?.edit { remove(KEY_CUSTOM_PATH) }
        val newDir = getDefaultDir()
        migrateFiles(oldDir, newDir)
        return true
    }

    /** 迁移文件（冲突保留目标目录已有文件） */
    private fun migrateFiles(from: File, to: File) {
        if (!from.exists() || from.absolutePath == to.absolutePath) return
        to.mkdirs()
        val files = listOf(FILE_THEME, FILE_DURATION_FILTER, FILE_FAVORITES, FILE_IGNORED, FILE_PLAYLISTS)
        for (name in files) {
            val src = File(from, name)
            val dst = File(to, name)
            if (!src.exists()) continue
            if (dst.exists()) continue
            try {
                src.copyTo(dst, overwrite = false)
                src.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate $name", e)
            }
        }
    }

    // ── 确保文件存在 ──

    private fun ensureFiles() {
        val dir = getStorageDir()
        Log.d(TAG, "Storage dir: ${dir.absolutePath}, exists=${dir.exists()}")
        ensureFile(dir, FILE_THEME) { ThemeJson.default() }
        ensureFile(dir, FILE_DURATION_FILTER) { DurationFilterJson.default() }
        ensureFile(dir, FILE_FAVORITES) { FavoritesJson.default() }
        ensureFile(dir, FILE_IGNORED) { IgnoredJson.default() }
        ensureFile(dir, FILE_PLAYLISTS) { PlaylistsJson.default() }
    }

    private fun <T> ensureFile(dir: File, fileName: String, defaultFactory: () -> T) {
        dir.mkdirs()
        val file = File(dir, fileName)
        if (!file.exists()) {
            try {
                file.writeText(gson.toJson(defaultFactory()))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create $fileName", e)
            }
        }
    }

    // ── JSON 读写 ──

    fun <T> readJsonFile(fileName: String, clazz: Class<T>, defaultFactory: () -> T): T {
        val dir = getStorageDir()
        val file = File(dir, fileName)
        Log.d(TAG, "readJsonFile: $fileName → ${file.absolutePath}, exists=${file.exists()}, dir=${dir.absolutePath}")
        if (!file.exists()) {
            Log.w(TAG, "readJsonFile: $fileName not found, creating default")
            val default = defaultFactory()
            writeJsonFile(fileName, default)
            return default
        }
        return try {
            val json = file.readText()
            Log.d(TAG, "readJsonFile: $fileName raw (${json.length} bytes): ${json.take(200)}")
            val result = gson.fromJson(json, clazz)
            if (result != null) {
                Log.d(TAG, "readJsonFile: $fileName parsed OK")
                result
            } else {
                Log.w(TAG, "readJsonFile: $fileName parsed to null, overwriting with default")
                val default = defaultFactory()
                writeJsonFile(fileName, default)
                default
            }
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "readJsonFile: $fileName JSON parse error, overwriting with default", e)
            val default = defaultFactory()
            writeJsonFile(fileName, default)
            default
        } catch (e: Exception) {
            Log.e(TAG, "readJsonFile: $fileName read error, overwriting with default", e)
            val default = defaultFactory()
            writeJsonFile(fileName, default)
            default
        }
    }

    fun <T> writeJsonFile(fileName: String, data: T) {
        try {
            val dir = getStorageDir()
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Created dir ${dir.absolutePath}: $created")
            }
            val file = File(dir, fileName)
            val json = gson.toJson(data)
            file.writeText(json)
            // 验证写入成功
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "OK: Wrote $fileName (${json.length} bytes) → ${file.absolutePath}")
            } else {
                Log.e(TAG, "Write appeared to succeed but file missing or empty: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "FAILED to write $fileName → ${getStorageDir().absolutePath}", e)
            Log.e(TAG, "  dir exists=${getStorageDir().exists()}, writable=${getStorageDir().canWrite()}")
        }
    }

    // ── 防抖写入 ──

    fun <T> debounceWrite(key: String, fileName: String, data: T) {
        debounceJobs[key]?.cancel()
        pendingWrites[key] = { writeJsonFile(fileName, data) }
        debounceJobs[key] = scope.launch {
            try {
                delay(DEBOUNCE_MS)
                pendingWrites.remove(key)?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Debounce write failed for $key/$fileName", e)
            }
        }
        Log.d(TAG, "Scheduled debounce write for $key -> $fileName")
    }

    /** 立即写入所有防抖队列中的数据（用于 Activity onPause 等场景） */
    fun flushAll() {
        debounceJobs.values.forEach { it.cancel() }
        debounceJobs.clear()
        pendingWrites.forEach { (key, write) ->
            try {
                write()
            } catch (e: Exception) {
                Log.e(TAG, "Flush failed for $key", e)
            }
        }
        pendingWrites.clear()
        Log.d(TAG, "Flushed all pending writes")
    }

    // ── 便捷方法 ──

    fun readTheme(): ThemeJson = readJsonFile(FILE_THEME, ThemeJson::class.java) { ThemeJson.default() }
    fun writeTheme(data: ThemeJson) = writeJsonFile(FILE_THEME, data)
    fun debounceWriteTheme(data: ThemeJson) = debounceWrite("theme", FILE_THEME, data)

    fun readDurationFilter(): DurationFilterJson = readJsonFile(FILE_DURATION_FILTER, DurationFilterJson::class.java) { DurationFilterJson.default() }
    fun writeDurationFilter(data: DurationFilterJson) = writeJsonFile(FILE_DURATION_FILTER, data)
    fun debounceWriteDurationFilter(data: DurationFilterJson) = debounceWrite("duration_filter", FILE_DURATION_FILTER, data)

    fun readFavorites(): FavoritesJson = readJsonFile(FILE_FAVORITES, FavoritesJson::class.java) { FavoritesJson.default() }
    fun writeFavorites(data: FavoritesJson) = writeJsonFile(FILE_FAVORITES, data)
    fun debounceWriteFavorites(data: FavoritesJson) = debounceWrite("favorites", FILE_FAVORITES, data)

    fun readIgnored(): IgnoredJson = readJsonFile(FILE_IGNORED, IgnoredJson::class.java) { IgnoredJson.default() }
    fun writeIgnored(data: IgnoredJson) = writeJsonFile(FILE_IGNORED, data)
    fun debounceWriteIgnored(data: IgnoredJson) = debounceWrite("ignored", FILE_IGNORED, data)

    fun readPlaylists(): PlaylistsJson = readJsonFile(FILE_PLAYLISTS, PlaylistsJson::class.java) { PlaylistsJson.default() }
    fun writePlaylists(data: PlaylistsJson) = writeJsonFile(FILE_PLAYLISTS, data)
    fun debounceWritePlaylists(data: PlaylistsJson) = debounceWrite("playlists", FILE_PLAYLISTS, data)
}
