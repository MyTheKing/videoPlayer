package com.example.videoplayer.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videoplayer.VideoPlayerViewModelFactory
import com.example.videoplayer.library.LibraryViewModel
import com.example.videoplayer.playback.MainPlaybackViewModel
import com.example.videoplayer.playback.PlaybackConnector
import com.example.videoplayer.playback.PlaybackQueueOrigin
import com.example.videoplayer.player.ui.PlayerContent
import com.example.videoplayer.playlists.PlaylistViewModel
import com.example.videoplayer.ui.library.LibraryScreen
import com.example.videoplayer.ui.playlists.PlaylistsScreen
import com.example.videoplayer.ui.playlists.PlaylistDetailScreen
import com.example.videoplayer.ui.settings.SettingsScreen
import com.example.videoplayer.ui.theme.ContentBackground

/**
 * 应用主壳：底部导航 + 迷你播放器 + 全屏播放层（文档 MVP 主路径）。
 */
@Composable
fun AppHomeScreen(
    viewModelFactory: VideoPlayerViewModelFactory,
    modifier: Modifier = Modifier,
) {
    val libraryVm: LibraryViewModel = viewModel(factory = viewModelFactory)
    val playlistVm: PlaylistViewModel = viewModel(factory = viewModelFactory)
    val playbackVm: MainPlaybackViewModel = viewModel(factory = viewModelFactory)

    PlaybackConnector(playbackVm)

    var tab by remember { mutableStateOf(AppTab.Library) }
    var playlistDetail by remember { mutableStateOf<Pair<Long, String>?>(null) }

    val controller by playbackVm.mediaController.collectAsStateWithLifecycle()
    val fullPlayer by playbackVm.fullPlayerVisible.collectAsStateWithLifecycle()
    val playlists by playlistVm.playlists.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ContentBackground,
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    val c = controller
                    if (c != null && !fullPlayer) {
                        MiniPlayerBar(
                            controller = c,
                            onExpand = { playbackVm.openFullPlayer() },
                            onPrevious = { c.seekToPrevious() },
                            onNext = { c.seekToNext() },
                        )
                    }
                    GlassBottomNav(
                        current = tab,
                        onSelect = {
                            tab = it
                            playlistDetail = null
                        },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                when (val detail = playlistDetail) {
                    null -> when (tab) {
                        AppTab.Library -> LibraryScreen(
                            viewModel = libraryVm,
                            playlistViewModel = playlistVm,
                            playlists = playlists,
                            onPlayInLibrary = { queue, startIndex ->
                                playbackVm.playQueue(
                                    uriStrings = queue.map { it.contentUri.toString() },
                                    titles = queue.map { it.displayName },
                                    durationMsList = queue.map { it.durationMs },
                                    startIndex = startIndex,
                                    queueOrigin = PlaybackQueueOrigin.LocalLibrary,
                                )
                                playbackVm.closeFullPlayer()
                            },
                            onOpenFullLibrary = { queue, startIndex ->
                                playbackVm.playQueue(
                                    uriStrings = queue.map { it.contentUri.toString() },
                                    titles = queue.map { it.displayName },
                                    durationMsList = queue.map { it.durationMs },
                                    startIndex = startIndex,
                                    queueOrigin = PlaybackQueueOrigin.LocalLibrary,
                                )
                                playbackVm.openFullPlayer()
                            },
                            onCreatePlaylist = { name -> playlistVm.createPlaylist(name) },
                        )

                        AppTab.Playlists -> PlaylistsScreen(
                            viewModel = playlistVm,
                            onOpenPlaylist = { id, name -> playlistDetail = id to name },
                        )

                        AppTab.Settings -> SettingsScreen()
                    }

                    else -> PlaylistDetailScreen(
                        playlistId = detail.first,
                        playlistName = detail.second,
                        onBack = { playlistDetail = null },
                        playlistViewModel = playlistVm,
                        playbackViewModel = playbackVm,
                    )
                }
            }
        }

        if (fullPlayer) {
            PlayerContent(
                controller = controller,
                onClose = { playbackVm.closeFullPlayer() },
                onPlaybackError = { playbackVm.stopAndClearSession() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
