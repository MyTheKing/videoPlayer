package com.example.videoplayer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.videoplayer.data.LibraryVideoActionsRepository
import com.example.videoplayer.data.PlaylistRepository
import com.example.videoplayer.library.LibraryViewModel
import com.example.videoplayer.playback.MainPlaybackViewModel
import com.example.videoplayer.playlists.PlaylistViewModel

/**
 * 为各 [ViewModel] 注入 [VideoPlayerApp] 与仓库依赖。
 */
class VideoPlayerViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    private val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository((application as VideoPlayerApp).database.playlistDao())
    }

    private val libraryVideoActionsRepository: LibraryVideoActionsRepository by lazy {
        LibraryVideoActionsRepository((application as VideoPlayerApp).database.libraryVideoActionsDao())
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(application, libraryVideoActionsRepository) as T
            modelClass.isAssignableFrom(PlaylistViewModel::class.java) ->
                PlaylistViewModel(playlistRepository) as T
            modelClass.isAssignableFrom(MainPlaybackViewModel::class.java) ->
                MainPlaybackViewModel() as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
