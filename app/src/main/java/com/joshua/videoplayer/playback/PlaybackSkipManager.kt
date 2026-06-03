package com.joshua.videoplayer.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放跳过管理器
 * 用于在 MediaPlaybackService 和 MainPlaybackViewModel 之间共享跳过状态
 *
 * 当视频被忽略或从歌单移除时，如果该视频正在播放，会标记为"播完后跳过"。
 * MediaPlaybackService 在切歌时检查，如果下一首是待跳过的，自动跳到下下首。
 */
object PlaybackSkipManager {
    private val _skipUris = MutableStateFlow<Set<String>>(emptySet())
    val skipUris: StateFlow<Set<String>> = _skipUris.asStateFlow()

    /**
     * 检查指定 URI 是否应该被跳过
     */
    fun shouldSkip(uri: String): Boolean = uri in _skipUris.value

    /**
     * 添加待跳过的 URI
     */
    fun addSkipUri(uri: String) {
        _skipUris.value = _skipUris.value + uri
    }

    /**
     * 移除已跳过的 URI（跳过完成后调用）
     */
    fun removeSkipped(uri: String) {
        _skipUris.value = _skipUris.value - uri
    }

    /**
     * 获取所有待跳过的 URI
     */
    fun getSkipUris(): Set<String> = _skipUris.value

    /**
     * 清空所有待跳过的 URI（播放停止时调用）
     */
    fun clear() {
        _skipUris.value = emptySet()
    }
}
