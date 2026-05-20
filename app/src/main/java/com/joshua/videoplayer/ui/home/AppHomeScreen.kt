package com.joshua.videoplayer.ui.home

import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joshua.videoplayer.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import com.joshua.videoplayer.VideoPlayerViewModelFactory
import com.joshua.videoplayer.library.LibraryViewModel
import com.joshua.videoplayer.data.SleepTimerManager
import com.joshua.videoplayer.playback.MainPlaybackViewModel
import com.joshua.videoplayer.playback.PlaybackConnector
import com.joshua.videoplayer.playback.PlaybackQueueOrigin
import com.joshua.videoplayer.player.ui.PlayerContent
import com.joshua.videoplayer.playlists.PlaylistViewModel
import com.joshua.videoplayer.ui.library.LibraryScreen
import com.joshua.videoplayer.ui.playlists.PlaylistDetailScreen
import com.joshua.videoplayer.ui.playlists.PlaylistsScreen
import com.joshua.videoplayer.ui.settings.SettingsScreen
import com.joshua.videoplayer.ui.theme.ContentBackground
import com.joshua.videoplayer.data.FileStorageManager
import com.joshua.videoplayer.data.LegalAgreementCache
import com.joshua.videoplayer.data.PlaybackCacheManager

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
    val favoriteUriSet by libraryVm.favoriteUriSet.collectAsStateWithLifecycle()
    val playbackRequest by playbackVm.playbackRequest.collectAsStateWithLifecycle()
    val libraryVideos by libraryVm.filteredVideos.collectAsStateWithLifecycle()
    val hasCachedPlayback by PlaybackCacheManager.hasCachedPlayback.collectAsStateWithLifecycle()

    // 横竖屏状态
    var isLandscape by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    // 法律协议确认弹窗状态
    var showLegalDialog by remember { mutableStateOf(false) }
    var pendingPlayAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var navigateToLegal by remember { mutableStateOf(false) }

    // 退出全屏时恢复竖屏
    DisposableEffect(fullPlayer) {
        if (!fullPlayer && isLandscape) {
            isLandscape = false
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {}
    }

    // 定时暂停：开启即倒计时，到时间若正在播放则暂停（全屏播放器内不暂停）
    LaunchedEffect(SleepTimerManager.isRunning) {
        while (SleepTimerManager.isRunning) {
            delay(1000L)
            val finished = SleepTimerManager.tick()
            if (finished) {
                if (!fullPlayer) {
                    val c = playbackVm.mediaController.value
                    if (c != null && c.playWhenReady) {
                        c.pause()
                    }
                }
                break
            }
        }
    }

    // 系统返回 / 侧滑返回：先退出歌单详情回到「我的歌单」网格，避免直接 finish 掉 Main 导致冷启动再进启动页
    BackHandler(enabled = playlistDetail != null && !fullPlayer) {
        playlistDetail = null
    }

    // 最外层返回：连续按两次退到后台，保持播放不中断
    var lastBackPressTime by remember { mutableStateOf(0L) }
    BackHandler(enabled = playlistDetail == null && !fullPlayer) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000) {
            activity?.moveTaskToBack(true)
        } else {
            lastBackPressTime = now
            Toast.makeText(activity, activity?.getString(R.string.press_back_again_to_exit) ?: "再按一次返回桌面", Toast.LENGTH_SHORT).show()
        }
    }

    // 自动恢复缓存的播放状态
    LaunchedEffect(hasCachedPlayback, controller) {
        // 只有用户没有主动播放时才自动恢复缓存
        if (hasCachedPlayback && controller == null && !playbackVm.userInitiatedPlay) {
            playbackVm.restoreCachedPlayback()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ContentBackground,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Transparent),
                ) {
                    val c = controller
                    if (c != null && !fullPlayer) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            MiniPlayerBar(
                                controller = c,
                                onExpand = { playbackVm.openFullPlayer() },
                                onPrevious = { c.seekToPrevious() },
                                onNext = { c.seekToNext() },
                            )
                        }
                    }
                    GlassBottomNav(
                        current = tab,
                        onSelect = {
                            tab = it
                            playlistDetail = null
                            navigateToLegal = false
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
                                val action = {
                                    playbackVm.playQueue(
                                        uriStrings = queue.map { it.contentUri.toString() },
                                        titles = queue.map { it.displayName },
                                        durationMsList = queue.map { it.durationMs },
                                        startIndex = startIndex,
                                        queueOrigin = PlaybackQueueOrigin.LocalLibrary,
                                    )
                                    playbackVm.closeFullPlayer()
                                }
                                if (LegalAgreementCache.hasAgreed) {
                                    action()
                                } else {
                                    pendingPlayAction = action
                                    showLegalDialog = true
                                }
                            },
                            onOpenFullLibrary = { queue, startIndex ->
                                val action = {
                                    playbackVm.playQueue(
                                        uriStrings = queue.map { it.contentUri.toString() },
                                        titles = queue.map { it.displayName },
                                        durationMsList = queue.map { it.durationMs },
                                        startIndex = startIndex,
                                        queueOrigin = PlaybackQueueOrigin.LocalLibrary,
                                    )
                                    playbackVm.openFullPlayer()
                                }
                                if (LegalAgreementCache.hasAgreed) {
                                    action()
                                } else {
                                    pendingPlayAction = action
                                    showLegalDialog = true
                                }
                            },
                            onCreatePlaylist = { name -> playlistVm.createPlaylist(name) },
                        )

                        AppTab.Playlists -> PlaylistsScreen(
                            viewModel = playlistVm,
                            onOpenPlaylist = { id, name -> playlistDetail = id to name },
                        )

                        AppTab.Settings -> SettingsScreen(
                            onDurationFilterChanged = { libraryVm.updateDurationFilter() },
                            onStoragePathChanged = { libraryVm.syncOnPathSwitch() },
                            onPermissionGranted = { libraryVm.syncFromFile() },
                            initialShowLegal = navigateToLegal,
                            onLegalShown = { navigateToLegal = false },
                            onBeforeLanguageChange = { playbackVm.stopAndClearSession() },
                        )
                    }

                    else -> PlaylistDetailScreen(
                        playlistId = detail.first,
                        playlistName = detail.second,
                        onBack = { playlistDetail = null },
                        playlistViewModel = playlistVm,
                        playbackViewModel = playbackVm,
                        playlists = playlists,
                        favoriteUriSet = favoriteUriSet,
                        libraryVideos = libraryVideos,
                        onToggleFavorite = { video ->
                            if (video.contentUri.toString() in favoriteUriSet) {
                                libraryVm.removeFavorite(video)
                            } else {
                                libraryVm.addFavorite(video)
                            }
                        },
                        onCreatePlaylist = { name -> playlistVm.createPlaylist(name) },
                        onNavigateToLegal = {
                            playlistDetail = null
                            navigateToLegal = true
                            tab = AppTab.Settings
                        },
                    )
                }
            }
        }

        // 存储权限提醒（Android 11+ 首次进入首页时弹出）
        var showStorageDialog by remember { mutableStateOf(false) }
        var wentToSettings by remember { mutableStateOf(false) }
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

        // 初次进入检查是否需要授权
        LaunchedEffect(Unit) {
            if (FileStorageManager.needsStoragePermission()) {
                showStorageDialog = true
            }
        }

        // 从系统设置返回时，若已授权则从文件恢复数据
        var triggerSync by remember { mutableStateOf(false) }
        var prevNeedsPermission by remember { mutableStateOf(FileStorageManager.needsStoragePermission()) }
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    val nowNeeds = FileStorageManager.needsStoragePermission()
                    if (wentToSettings) {
                        wentToSettings = false
                        if (!nowNeeds) {
                            showStorageDialog = false
                        }
                    }
                    // 权限从未授权 → 已授权：触发同步（覆盖从首页弹窗和设置页两个入口）
                    if (prevNeedsPermission && !nowNeeds) {
                        triggerSync = true
                    }
                    prevNeedsPermission = nowNeeds
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        LaunchedEffect(triggerSync) {
            if (triggerSync) {
                triggerSync = false
                libraryVm.syncFromFile()
            }
        }

        if (showStorageDialog) {
            AlertDialog(
                onDismissRequest = { showStorageDialog = false },
                title = { Text(stringResource(R.string.storage_permission_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.storage_permission_message))
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.storage_permission_instruction))
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        wentToSettings = true
                        FileStorageManager.requestManageStoragePermission(context)
                    }) {
                        Text(stringResource(R.string.storage_permission_grant))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showStorageDialog = false }) {
                        Text(stringResource(R.string.storage_permission_later))
                    }
                },
            )
        }

        // 法律协议确认弹窗
        if (showLegalDialog) {
            AlertDialog(
                onDismissRequest = {
                    showLegalDialog = false
                    pendingPlayAction = null
                },
                title = { Text(stringResource(R.string.legal_agreement_dialog_title)) },
                text = { Text(stringResource(R.string.legal_agreement_dialog_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        showLegalDialog = false
                        navigateToLegal = true
                        tab = AppTab.Settings
                    }) {
                        Text(stringResource(R.string.legal_agreement_dialog_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showLegalDialog = false
                        pendingPlayAction = null
                    }) {
                        Text(stringResource(R.string.legal_agreement_dialog_cancel))
                    }
                },
            )
        }

        if (fullPlayer) {
            // 获取当前视频标题
            val req = playbackRequest
            val currentTitle = if (req != null) {
                val idx = controller?.currentMediaItemIndex ?: req.startIndex
                req.titles.getOrElse(idx) { "" }
            } else {
                ""
            }

            PlayerContent(
                controller = controller,
                videoTitle = currentTitle,
                isLandscape = isLandscape,
                onClose = {
                    isLandscape = false
                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    playbackVm.closeFullPlayer()
                },
                onPlaybackError = {
                    val req = playbackRequest
                    if (req != null) {
                        val idx = controller?.currentMediaItemIndex ?: req.startIndex
                        val uri = req.uriStrings.getOrElse(idx) { "" }
                        if (uri.isNotBlank()) libraryVm.removeInvalidUri(uri)
                    }
                    playbackVm.stopAndClearSession()
                },
                onToggleOrientation = {
                    isLandscape = !isLandscape
                    activity?.requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
