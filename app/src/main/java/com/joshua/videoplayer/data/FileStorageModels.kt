package com.joshua.videoplayer.data

data class ThemeJson(
    val primaryColor: Int,
    val presetName: String,
) {
    companion object {
        fun default() = ThemeJson(
            primaryColor = 0xFF4F46E5.toInt(),
            presetName = "紫色",
        )
    }
}

data class DurationFilterJson(
    val filterEnabled: Boolean,
    val minDurationMs: Long,
    val maxDurationMs: Long,
) {
    companion object {
        fun default() = DurationFilterJson(
            filterEnabled = false,
            minDurationMs = 0L,
            maxDurationMs = 0L,
        )
    }
}

data class FavoritesJson(
    val uris: List<String>,
) {
    companion object {
        fun default() = FavoritesJson(emptyList())
    }
}

data class IgnoredJson(
    val uris: List<String>,
) {
    companion object {
        fun default() = IgnoredJson(emptyList())
    }
}

data class PlaylistItemJson(
    val contentUri: String,
    val displayTitle: String,
    val durationMs: Long,
    val sortOrder: Int,
)

data class PlaylistJson(
    val name: String,
    val kind: Int,
    val coverImageUri: String?,
    val items: List<PlaylistItemJson>,
)

data class PlaylistsJson(
    val playlists: List<PlaylistJson>,
) {
    companion object {
        fun default() = PlaylistsJson(emptyList())
    }
}
