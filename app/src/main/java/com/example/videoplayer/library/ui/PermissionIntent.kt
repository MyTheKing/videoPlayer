package com.example.videoplayer.library.ui

/**
 * 存储/媒体权限 Gate 的用户意图与外部回调，由 UI 或 Activity 传入 [PermissionViewModel.dispatch]。
 */
sealed class PermissionIntent {
    /**
     * 外部完成权限检查后同步结果。
     * @param granted 当前是否已授予
     * @param canAskAgain 若未授予，是否仍可再次请求（对应 !shouldShowRequestPermissionRationale 时为 false 表示「不再询问」）
     */
    data class SyncFromCheck(val granted: Boolean, val canAskAgain: Boolean) : PermissionIntent()

    /** 用户点击「请求权限」后，由 UI 在拿到系统请求结果时调用。@param canAskAgain 若未授予，是否仍可再次请求 */
    data class OnRequestResult(val granted: Boolean, val canAskAgain: Boolean = true) : PermissionIntent()

    /** 用户点击「去设置」。ViewModel 仅记一次事件，由 UI 消费并跳转。 */
    object OpenSettings : PermissionIntent()

    /** 用户关闭/忽略权限说明（不崩溃，可后续再进首屏重试）。 */
    object DismissRationale : PermissionIntent()
}
