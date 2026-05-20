package com.joshua.videoplayer.data

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import android.util.Log
import com.joshua.videoplayer.R
import java.util.Locale

/**
 * 语言管理器
 * 负责应用内语言切换和偏好持久化
 */
object LanguageManager {
    private const val TAG = "LanguageManager"
    private const val PREFS_NAME = "videoPlayer_language"
    private const val KEY_LANGUAGE = "selected_language"

    /** 语言选项 */
    enum class Language(val localeTag: String) {
        CHINESE("zh-Hans"),
        ENGLISH("en");

        companion object {
            fun fromLocaleTag(tag: String): Language {
                return entries.find { tag.startsWith(it.localeTag) || it.localeTag.startsWith(tag) } ?: CHINESE
            }

            /** 根据系统语言检测默认语言：中文(简/繁)→中文，其他→英文 */
            fun detectFromSystem(): Language {
                val sysTag = Locale.getDefault().language ?: ""
                return if (sysTag.startsWith("zh")) CHINESE else ENGLISH
            }
        }
    }

    private var prefs: SharedPreferences? = null
    private var currentLanguage: Language = Language.CHINESE

    /**
     * 初始化语言管理器
     * 首次使用（存储无记录）时跟随系统语言：中文(简/繁)→简体中文，其他→英文
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedTag = prefs?.getString(KEY_LANGUAGE, null)
        Log.d(TAG, "init: savedTag=$savedTag")
        if (savedTag != null) {
            currentLanguage = Language.fromLocaleTag(savedTag)
        } else {
            currentLanguage = Language.detectFromSystem()
            prefs?.edit()?.putString(KEY_LANGUAGE, currentLanguage.localeTag)?.apply()
            Log.d(TAG, "init: no saved language, detected system=$currentLanguage")
        }
        Log.d(TAG, "init: currentLanguage=$currentLanguage")
        applyLocale(context, currentLanguage)
    }

    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): Language = currentLanguage

    /**
     * 获取当前语言的显示名称
     */
    fun getCurrentLanguageDisplayName(context: Context): String {
        return when (currentLanguage) {
            Language.CHINESE -> context.getString(R.string.language_chinese)
            Language.ENGLISH -> context.getString(R.string.language_english)
        }
    }

    /**
     * 切换语言
     * 调用方应在切换前自行停止播放（如 playbackVm.stopAndClearSession()）
     */
    fun setLanguage(activity: Activity, language: Language) {
        Log.d(TAG, "setLanguage: $language (${language.localeTag})")
        currentLanguage = language
        prefs?.edit()?.putString(KEY_LANGUAGE, language.localeTag)?.apply()
        applyAndRecreate(activity, language)
    }

    /**
     * 获取当前语言标签
     */
    fun getCurrentLocaleTag(): String = currentLanguage.localeTag

    /**
     * 应用语言设置并重建 Activity
     * locale 的实际应用由 MainActivity.attachBaseContext() 完成
     */
    private fun applyAndRecreate(activity: Activity, language: Language) {
        val locale = Locale.forLanguageTag(language.localeTag)
        Locale.setDefault(locale)

        Log.d(TAG, "Locale set: ${locale.toLanguageTag()}, recreating activity")
        activity.recreate()
    }

    /**
     * 应用语言设置（初始化时使用，不重建）
     */
    private fun applyLocale(context: Context, language: Language) {
        val locale = Locale.forLanguageTag(language.localeTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
        Log.d(TAG, "Locale applied: ${locale.toLanguageTag()}")
    }
}
