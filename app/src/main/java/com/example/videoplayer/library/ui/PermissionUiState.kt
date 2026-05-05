package com.example.videoplayer.library.ui

/**
 * 存储/媒体权限 Gate 的 UI 状态，由 [PermissionViewModel] 暴露，供首屏或 library 入口消费。
 *
 * @param granted 是否已授予访问本地视频所需权限
 * @param showRationale 是否展示「为何需要权限」说明与操作入口
 * @param canAskAgain 用户拒绝后是否仍可再次弹出系统请求（否则仅能「去设置」）
 * @param error 可展示给用户的错误/说明文案，null 表示无
 */
data class PermissionUiState(
    val granted: Boolean = false,
    val showRationale: Boolean = false,
    val canAskAgain: Boolean = true,
    val error: String? = null
)
