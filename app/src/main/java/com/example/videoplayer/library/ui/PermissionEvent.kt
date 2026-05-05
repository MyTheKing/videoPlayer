package com.example.videoplayer.library.ui

/**
 * 权限相关一次性事件，由 [PermissionViewModel] 经 SharedFlow 发出，供 UI 执行「请求权限」「打开设置」等需 Activity 参与的操作。
 */
sealed class PermissionEvent {
    /** 需要执行系统权限请求，由 UI 调用 ActivityResultLauncher。 */
    object RequestPermission : PermissionEvent()

    /** 需要打开应用详情设置页，由 UI 调用 startActivity(ACTION_APPLICATION_DETAILS_SETTINGS)。 */
    object OpenSettings : PermissionEvent()
}
