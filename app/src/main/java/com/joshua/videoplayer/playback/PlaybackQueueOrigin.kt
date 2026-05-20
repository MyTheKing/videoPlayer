package com.joshua.videoplayer.playback

/**
 * 当前会话队列来自何处，用于 UI 提示与调试；与 Exo 内实际 [androidx.media3.common.MediaItem] 列表一致。
 */
sealed class PlaybackQueueOrigin {
    /** 来自「本地库」当前列表（含搜索过滤后的列表）。 */
    data object LocalLibrary : PlaybackQueueOrigin()

    /** 来自某个用户歌单。 */
    data class UserPlaylist(
        val playlistId: Long,
        val name: String,
    ) : PlaybackQueueOrigin()
}
