package com.joshua.videoplayer.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.joshua.videoplayer.playback.PlaybackQueueOrigin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 播放列表缓存管理器，用于保存播放队列。
 * 使用 SharedPreferences 存储，仅在应用缓存中，不涉及系统存储。
 */
object PlaybackCacheManager {

    private const val PREFS_NAME = "playback_cache"
    private const val KEY_QUEUE_URI_STRINGS = "queue_uri_strings"
    private const val KEY_QUEUE_TITLES = "queue_titles"
    private const val KEY_QUEUE_DURATIONS = "queue_durations"
    private const val KEY_CURRENT_INDEX = "current_index"
    private const val KEY_SHUFFLE_ENABLED = "shuffle_enabled"
    private const val KEY_REPEAT_MODE = "repeat_mode"
    private const val KEY_QUEUE_ORIGIN_TYPE = "queue_origin_type"
    private const val KEY_QUEUE_ORIGIN_PLAYLIST_ID = "queue_origin_playlist_id"
    private const val KEY_QUEUE_ORIGIN_PLAYLIST_NAME = "queue_origin_playlist_name"
    private const val KEY_HAS_CACHED_PLAYBACK = "has_cached_playback"

    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private val _hasCachedPlayback = MutableStateFlow(false)
    val hasCachedPlayback: StateFlow<Boolean> = _hasCachedPlayback.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _hasCachedPlayback.value = prefs.getBoolean(KEY_HAS_CACHED_PLAYBACK, false)
    }

    /**
     * 保存播放队列
     */
    fun savePlaybackState(
        uriStrings: List<String>,
        titles: List<String>,
        durationMsList: List<Long>,
        shuffleEnabled: Boolean,
        repeatMode: Int,
        queueOrigin: PlaybackQueueOrigin,
    ) {
        prefs.edit().apply {
            putString(KEY_QUEUE_URI_STRINGS, gson.toJson(uriStrings))
            putString(KEY_QUEUE_TITLES, gson.toJson(titles))
            putString(KEY_QUEUE_DURATIONS, gson.toJson(durationMsList))
            putBoolean(KEY_SHUFFLE_ENABLED, shuffleEnabled)
            putInt(KEY_REPEAT_MODE, repeatMode)

            when (queueOrigin) {
                is PlaybackQueueOrigin.LocalLibrary -> {
                    putString(KEY_QUEUE_ORIGIN_TYPE, "local_library")
                    remove(KEY_QUEUE_ORIGIN_PLAYLIST_ID)
                    remove(KEY_QUEUE_ORIGIN_PLAYLIST_NAME)
                }
                is PlaybackQueueOrigin.UserPlaylist -> {
                    putString(KEY_QUEUE_ORIGIN_TYPE, "user_playlist")
                    putLong(KEY_QUEUE_ORIGIN_PLAYLIST_ID, queueOrigin.playlistId)
                    putString(KEY_QUEUE_ORIGIN_PLAYLIST_NAME, queueOrigin.name)
                }
            }

            putBoolean(KEY_HAS_CACHED_PLAYBACK, true)
            apply()
        }
        _hasCachedPlayback.value = true
    }

    data class CachedPlaybackState(
        val uriStrings: List<String>,
        val titles: List<String>,
        val durationMsList: List<Long>,
        val currentIndex: Int,
        val shuffleEnabled: Boolean,
        val repeatMode: Int,
        val queueOrigin: PlaybackQueueOrigin,
    )

    fun getCachedPlaybackState(): CachedPlaybackState? {
        if (!prefs.getBoolean(KEY_HAS_CACHED_PLAYBACK, false)) {
            return null
        }

        val uriStringsJson = prefs.getString(KEY_QUEUE_URI_STRINGS, null) ?: return null
        val titlesJson = prefs.getString(KEY_QUEUE_TITLES, null) ?: return null
        val durationsJson = prefs.getString(KEY_QUEUE_DURATIONS, null) ?: return null

        val uriStrings: List<String> = gson.fromJson(uriStringsJson, object : TypeToken<List<String>>() {}.type)
        val titles: List<String> = gson.fromJson(titlesJson, object : TypeToken<List<String>>() {}.type)
        val durations: List<Long> = gson.fromJson(durationsJson, object : TypeToken<List<Long>>() {}.type)

        if (uriStrings.isEmpty()) return null

        val currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0).coerceIn(0, uriStrings.size - 1)
        val shuffleEnabled = prefs.getBoolean(KEY_SHUFFLE_ENABLED, false)
        val repeatMode = prefs.getInt(KEY_REPEAT_MODE, 0)

        val originType = prefs.getString(KEY_QUEUE_ORIGIN_TYPE, "local_library")
        val queueOrigin = when (originType) {
            "user_playlist" -> {
                val playlistId = prefs.getLong(KEY_QUEUE_ORIGIN_PLAYLIST_ID, 0L)
                val playlistName = prefs.getString(KEY_QUEUE_ORIGIN_PLAYLIST_NAME, "") ?: ""
                PlaybackQueueOrigin.UserPlaylist(playlistId, playlistName)
            }
            else -> PlaybackQueueOrigin.LocalLibrary
        }

        return CachedPlaybackState(
            uriStrings = uriStrings,
            titles = titles,
            durationMsList = durations,
            currentIndex = currentIndex,
            shuffleEnabled = shuffleEnabled,
            repeatMode = repeatMode,
            queueOrigin = queueOrigin,
        )
    }

    /**
     * 更新当前播放索引（切歌时调用）
     */
    fun updateCurrentIndex(currentIndex: Int) {
        if (prefs.getBoolean(KEY_HAS_CACHED_PLAYBACK, false)) {
            prefs.edit().apply {
                putInt(KEY_CURRENT_INDEX, currentIndex)
                apply()
            }
        }
    }

    fun clearCachedPlayback() {
        prefs.edit().clear().apply()
        _hasCachedPlayback.value = false
    }
}
