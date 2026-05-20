package com.joshua.videoplayer

/**
 * 读视频权限门控与系统弹窗尝试记录，[SplashActivity] 与 [MainActivity] 共用，避免两套 SharedPreferences 键不一致。
 */
object VideoPermissionGate {
    const val PREF_FILE = "video_player_permission_gate"
    const val KEY_SYSTEM_PROMPT_ATTEMPTED = "system_prompt_attempted"
}
