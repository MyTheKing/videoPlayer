package com.joshua.videoplayer.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 法律协议阅读状态持久化存储。
 * 清除 app 数据/缓存或重新安装后会重置。
 */
object LegalAgreementCache {
    private const val PREFS_NAME = "videoPlayer_legal_agreement"
    private const val KEY_HAS_AGREED = "has_agreed"

    private var prefs: SharedPreferences? = null
    private var _hasAgreed = false

    val hasAgreed: Boolean get() = _hasAgreed

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hasAgreed = prefs?.getBoolean(KEY_HAS_AGREED, false) ?: false
    }

    fun markAgreed() {
        _hasAgreed = true
        prefs?.edit()?.putBoolean(KEY_HAS_AGREED, true)?.apply()
    }
}
