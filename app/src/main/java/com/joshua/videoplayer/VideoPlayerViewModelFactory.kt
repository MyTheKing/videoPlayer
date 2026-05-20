package com.joshua.videoplayer

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.joshua.videoplayer.data.LibraryVideoActionsRepository
import com.joshua.videoplayer.data.PlaylistRepository
import com.joshua.videoplayer.library.LibraryViewModel
import com.joshua.videoplayer.playback.MainPlaybackViewModel
import com.joshua.videoplayer.playlists.PlaylistViewModel

/**
 * 为各 [ViewModel] 注入 [VideoPlayerApp] 与仓库依赖。
 */
class VideoPlayerViewModelFactory(
    private val application: Application,
) : ViewModelProvider.Factory {

    private val libraryVideoActionsRepository: LibraryVideoActionsRepository by lazy {
        LibraryVideoActionsRepository((application as VideoPlayerApp).database.libraryVideoActionsDao())
    }

    private val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(
            (application as VideoPlayerApp).database.playlistDao(),
            libraryVideoActionsRepository,
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LibraryViewModel::class.java) ->
                LibraryViewModel(application, libraryVideoActionsRepository, playlistRepository) as T
            modelClass.isAssignableFrom(PlaylistViewModel::class.java) ->
                PlaylistViewModel(playlistRepository, libraryVideoActionsRepository) as T
            modelClass.isAssignableFrom(MainPlaybackViewModel::class.java) ->
                MainPlaybackViewModel() as T
            else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
        }
    }
}
