package com.joshua.videoplayer.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首屏或 library 入口的存储/媒体权限 Gate ViewModel。
 * 仅负责状态与意图转换；实际检查与请求由 UI/Activity 完成并透过 [PermissionIntent] 回传。
 *
 * @see PermissionUiState
 * @see PermissionIntent
 * @see PermissionEvent
 */
class PermissionViewModel : ViewModel() {

    private val _state = MutableStateFlow(PermissionUiState())
    val state: StateFlow<PermissionUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PermissionEvent>()
    val events: SharedFlow<PermissionEvent> = _events.asSharedFlow()

    /**
     * 处理权限相关意图。
     * @param intent 用户操作或外部回调
     */
    fun dispatch(intent: PermissionIntent) {
        when (intent) {
            is PermissionIntent.SyncFromCheck -> {
                _state.update {
                    it.copy(
                        granted = intent.granted,
                        showRationale = !intent.granted,
                        canAskAgain = intent.canAskAgain,
                        error = if (!intent.granted) null else it.error
                    )
                }
            }
            is PermissionIntent.OnRequestResult -> {
                _state.update {
                    it.copy(
                        granted = intent.granted,
                        showRationale = !intent.granted,
                        canAskAgain = intent.canAskAgain,
                        error = null
                    )
                }
            }
            is PermissionIntent.OpenSettings -> {
                viewModelScope.launch { _events.emit(PermissionEvent.OpenSettings) }
            }
            is PermissionIntent.DismissRationale -> {
                _state.update { it.copy(showRationale = false, error = null) }
            }
        }
    }

    /**
     * 由 UI 在用户点击「请求权限」时调用，ViewModel 发出一次性事件，由 UI 执行 Launcher。
     */
    fun requestPermission() {
        viewModelScope.launch { _events.emit(PermissionEvent.RequestPermission) }
    }
}
