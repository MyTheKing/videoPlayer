package com.joshua.videoplayer.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.FileStorageManager
import com.joshua.videoplayer.data.ThemeJson

/**
 * 主题颜色管理器
 * 负责主题颜色的持久化和动态切换
 */
object ThemeColorManager {
    private const val PREFS_NAME = "videoPlayer_theme"
    private const val KEY_PRIMARY_COLOR = "primary_color"
    private const val KEY_PRESET_NAME = "preset_name"

    // 当前主题颜色状态
    var currentPrimary by mutableStateOf(PresetTheme.Purple.primary)
        private set

    var currentPrimaryContainer by mutableStateOf(PresetTheme.Purple.container)
        private set

    var currentPresetName by mutableStateOf("紫色")
        private set

    private var prefs: SharedPreferences? = null

    // 预设主题颜色
    enum class PresetTheme(
        val displayName: String,
        val primary: Color,
        val container: Color,
    ) {
        Purple("紫色", Color(0xFF3525CD), Color(0xFF4F46E5)),
        Blue("蓝色", Color(0xFF1E40AF), Color(0xFF3B82F6)),
        Green("绿色", Color(0xFF166534), Color(0xFF22C55E)),
        Red("红色", Color(0xFF991B1B), Color(0xFFEF4444)),
        Orange("橙色", Color(0xFF9A3412), Color(0xFFF97316)),
        Cyan("青色", Color(0xFF155E75), Color(0xFF06B6D4)),
        Black("黑色", Color(0xFF1C1C1C), Color(0xFF333333));

        /** 获取本地化的显示名称 */
        @Composable
        fun localizedName(): String {
            return when (this) {
                Purple -> stringResource(R.string.theme_preset_purple)
                Blue -> stringResource(R.string.theme_preset_blue)
                Green -> stringResource(R.string.theme_preset_green)
                Red -> stringResource(R.string.theme_preset_red)
                Orange -> stringResource(R.string.theme_preset_orange)
                Cyan -> stringResource(R.string.theme_preset_cyan)
                Black -> stringResource(R.string.theme_preset_black)
            }
        }
    }

    /** 获取当前预设的本地化显示名称 */
    @Composable
    fun currentPresetLocalizedName(): String {
        val preset = PresetTheme.entries.find { it.displayName == currentPresetName }
        return preset?.localizedName() ?: stringResource(R.string.theme_preset_custom)
    }

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
        val spHasData = prefs?.contains(KEY_PRIMARY_COLOR) == true
        Log.d("ThemeColorManager", "bidirectionalSync: SP hasData=$spHasData")
        if (spHasData) {
            loadSavedTheme()
            FileStorageManager.writeTheme(
                ThemeJson(primaryColor = currentPrimaryContainer.toArgb(), presetName = currentPresetName)
            )
        } else {
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "theme.json")
            if (!file.exists()) return
            val fileData = try {
                com.google.gson.Gson().fromJson(file.readText(), ThemeJson::class.java)
            } catch (_: Exception) { null }
            if (fileData != null) {
                currentPrimaryContainer = Color(fileData.primaryColor)
                currentPrimary = getPrimaryFromContainer(currentPrimaryContainer)
                currentPresetName = fileData.presetName
                prefs?.edit {
                    putInt(KEY_PRIMARY_COLOR, fileData.primaryColor)
                    putString(KEY_PRESET_NAME, fileData.presetName)
                }
            }
        }
    }

    /** 切换存储路径时调用：新文件夹有数据则读文件覆盖 SP，无数据则 SP 写入文件 */
    fun syncOnPathSwitch(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val dir = FileStorageManager.getStorageDir()
        val file = java.io.File(dir, "theme.json")
        val hasFileData = try {
            if (file.exists()) {
                val raw = file.readText()
                raw.isNotBlank() && raw != "{}"
            } else false
        } catch (_: Exception) { false }

        if (hasFileData) {
            // 新文件夹有数据 → 读文件覆盖 SP
            Log.d("ThemeColorManager", "syncOnPathSwitch: file→SP")
            val fileData = try {
                com.google.gson.Gson().fromJson(file.readText(), com.joshua.videoplayer.data.ThemeJson::class.java)
            } catch (_: Exception) { null }
            if (fileData != null) {
                currentPrimaryContainer = Color(fileData.primaryColor)
                currentPrimary = getPrimaryFromContainer(currentPrimaryContainer)
                currentPresetName = fileData.presetName
                prefs?.edit {
                    putInt(KEY_PRIMARY_COLOR, fileData.primaryColor)
                    putString(KEY_PRESET_NAME, fileData.presetName)
                }
            }
        } else {
            // 新文件夹无数据 → SP 写入文件
            Log.d("ThemeColorManager", "syncOnPathSwitch: SP→file")
            loadSavedTheme()
            FileStorageManager.writeTheme(
                com.joshua.videoplayer.data.ThemeJson(
                    primaryColor = currentPrimaryContainer.toArgb(),
                    presetName = currentPresetName,
                )
            )
        }
    }

    /**
     * 双缓存加载：先读 SP，SP 无数据时从文件补回。
     */
    private fun loadFromFileOrPrefs() {
        // 1. 先读 SP（缓存）
        loadSavedTheme()
        val spHasData = prefs?.contains(KEY_PRIMARY_COLOR) == true
        Log.d("ThemeColorManager", "load: SP hasData=$spHasData, preset=$currentPresetName")

        if (spHasData) {
            // SP 有数据，同步到文件
            FileStorageManager.writeTheme(
                com.joshua.videoplayer.data.ThemeJson(
                    primaryColor = currentPrimaryContainer.toArgb(),
                    presetName = currentPresetName,
                )
            )
            return
        }

        // 2. SP 无数据（升级/重装），从文件补回
        try {
            val dir = FileStorageManager.getStorageDir()
            val file = java.io.File(dir, "theme.json")
            Log.d("ThemeColorManager", "load: SP empty, try file ${file.absolutePath}, exists=${file.exists()}")
            if (!file.exists()) return
            val rawJson = file.readText()
            val fileData = try {
                com.google.gson.Gson().fromJson(rawJson, com.joshua.videoplayer.data.ThemeJson::class.java)
            } catch (e: Exception) {
                Log.e("ThemeColorManager", "load: file parse error", e)
                null
            }
            if (fileData != null) {
                currentPrimaryContainer = Color(fileData.primaryColor)
                currentPrimary = getPrimaryFromContainer(currentPrimaryContainer)
                currentPresetName = fileData.presetName
                Log.d("ThemeColorManager", "load: restored from file, preset=$currentPresetName")
                prefs?.edit {
                    putInt(KEY_PRIMARY_COLOR, fileData.primaryColor)
                    putString(KEY_PRESET_NAME, fileData.presetName)
                }
            }
        } catch (e: Exception) {
            Log.e("ThemeColorManager", "load: file fallback failed", e)
        }
    }

    /**
     * 加载保存的主题
     */
    private fun loadSavedTheme() {
        val savedColor = prefs?.getInt(KEY_PRIMARY_COLOR, PresetTheme.Purple.container.toArgb())
            ?: PresetTheme.Purple.container.toArgb()
        val savedName = prefs?.getString(KEY_PRESET_NAME, "紫色") ?: "紫色"

        currentPrimaryContainer = Color(savedColor)
        currentPrimary = getPrimaryFromContainer(currentPrimaryContainer)
        currentPresetName = savedName
    }

    /**
     * 应用预设主题
     */
    fun applyPreset(preset: PresetTheme) {
        currentPrimary = preset.primary
        currentPrimaryContainer = preset.container
        currentPresetName = preset.displayName
        saveTheme(preset.container, preset.displayName)
    }

    /**
     * 应用自定义颜色
     */
    fun applyCustomColor(containerColor: Color) {
        currentPrimaryContainer = containerColor
        currentPrimary = getPrimaryFromContainer(containerColor)
        currentPresetName = "自定义"
        saveTheme(containerColor, "自定义")
    }

    /**
     * 根据容器色计算主色（加深）
     */
    private fun getPrimaryFromContainer(container: Color): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(container.toArgb(), hsl)
        hsl[2] = (hsl[2] - 0.2f).coerceIn(0f, 1f)
        hsl[1] = (hsl[1] + 0.1f).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(hsl))
    }

    /**
     * 保存主题到 SharedPreferences 和文件存储
     */
    private fun saveTheme(containerColor: Color, presetName: String) {
        prefs?.edit {
            putInt(KEY_PRIMARY_COLOR, containerColor.toArgb())
            putString(KEY_PRESET_NAME, presetName)
        }
        Log.d("ThemeColorManager", "saveTheme: color=${containerColor.toArgb()}, preset=$presetName")
        FileStorageManager.debounceWriteTheme(
            ThemeJson(primaryColor = containerColor.toArgb(), presetName = presetName)
        )
    }

    /**
     * 根据主色调生成完整的色彩方案
     */
    fun generateColorScheme(containerColor: Color): ThemeColorScheme {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(containerColor.toArgb(), hsl)
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]

        return ThemeColorScheme(
            primary = currentPrimary,
            primaryContainer = containerColor,
            onPrimary = Color.White,
            onPrimaryContainer = hslToColor(h, (s - 0.1f).coerceIn(0f, 1f), (l + 0.4f).coerceIn(0f, 1f)),
            surface = hslToColor(h, (s * 0.3f).coerceAtMost(0.3f), 0.98f),
            surfaceDim = hslToColor(h, (s * 0.4f).coerceAtMost(0.4f), 0.85f),
            surfaceBright = hslToColor(h, (s * 0.2f).coerceAtMost(0.2f), 0.98f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hslToColor(h, (s * 0.25f).coerceAtMost(0.25f), 0.96f),
            surfaceContainer = hslToColor(h, (s * 0.35f).coerceAtMost(0.35f), 0.93f),
            surfaceContainerHigh = hslToColor(h, (s * 0.4f).coerceAtMost(0.4f), 0.91f),
            surfaceContainerHighest = hslToColor(h, (s * 0.45f).coerceAtMost(0.45f), 0.89f),
            surfaceVariant = hslToColor(h, (s * 0.4f).coerceAtMost(0.4f), 0.89f),
            background = hslToColor(h, (s * 0.3f).coerceAtMost(0.3f), 0.98f),
            onBackground = hslToColor(h, (s * 0.5f).coerceAtMost(0.5f), 0.12f),
            onSurface = hslToColor(h, (s * 0.5f).coerceAtMost(0.5f), 0.12f),
            onSurfaceVariant = hslToColor(h, (s * 0.3f).coerceAtMost(0.3f), 0.35f),
            outline = hslToColor(h, (s * 0.15f).coerceAtMost(0.15f), 0.55f),
            outlineVariant = hslToColor(h, (s * 0.25f).coerceAtMost(0.25f), 0.8f),
        )
    }

    /**
     * HSL 转 Color
     */
    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        return Color(ColorUtils.HSLToColor(floatArrayOf(h, s, l)))
    }
}

/**
 * 主题色彩方案
 */
data class ThemeColorScheme(
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceBright: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceVariant: Color,
    val background: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
)
