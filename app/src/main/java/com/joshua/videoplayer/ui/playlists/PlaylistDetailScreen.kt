package com.joshua.videoplayer.ui.playlists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.LocalVideo
import com.joshua.videoplayer.data.PlaylistRepository
import com.joshua.videoplayer.data.local.PlaylistItemEntity
import com.joshua.videoplayer.data.local.PlaylistWithCount
import com.joshua.videoplayer.playback.MainPlaybackViewModel
import com.joshua.videoplayer.playback.PlaybackQueueOrigin
import com.joshua.videoplayer.playlists.PlaylistViewModel
import com.joshua.videoplayer.ui.components.VideoListItem
import com.joshua.videoplayer.ui.formatDurationMs
import com.joshua.videoplayer.ui.theme.HeaderGradientEnd
import com.joshua.videoplayer.ui.theme.HeaderGradientStart
import com.joshua.videoplayer.data.LegalAgreementCache

/**
 * 单个歌单：渐变顶栏、从本地资源库多选添加、删除条目。
 * 列表项：点序号/标题区在底部迷你播放器起播；仅点缩略图进入全屏播放页（与本地库一致）。
 * （顶区「播放全部 / 顺序 / 随机」已注释：点击列表即按当前列表顺序起播；列表循环与单曲循环在迷你条切换。）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: MainPlaybackViewModel,
    playlists: List<PlaylistWithCount>,
    favoriteUriSet: Set<String>,
    onToggleFavorite: (LocalVideo) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    libraryVideos: List<LocalVideo>,
    onNavigateToLegal: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val itemsFlow = remember(playlistId) { playlistViewModel.playlistItemsFlow(playlistId) }
    val playlistItems by itemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val queueOrigin = remember(playlistId, playlistName) {
        PlaybackQueueOrigin.UserPlaylist(playlistId, playlistName)
    }
    var menuVideo by remember { mutableStateOf<PlaylistItemEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }

    // 多选模式状态
    var isInSelectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateMapOf<Long, Boolean>() }
    var showBatchRemoveConfirm by remember { mutableStateOf(false) }

    // 法律协议确认弹窗状态
    var showLegalDialog by remember { mutableStateOf(false) }
    var pendingPlayAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    BackHandler(enabled = isInSelectionMode) {
        isInSelectionMode = false
        selectedIds.clear()
    }

    val missingDurationCount = playlistItems.count { it.durationMs <= 0L }
    LaunchedEffect(playlistId, missingDurationCount) {
        if (playlistItems.isNotEmpty() && missingDurationCount > 0) {
            playlistViewModel.probeMissingDurationsInPlaylist(context, playlistId)
        }
    }

    val existingContentUris = remember(playlistItems) {
        playlistItems.map { it.contentUri }.toSet()
    }

    /*
    fun playWithMode(
        shuffle: Boolean,
        repeatMode: Int,
        startIndex: Int = 0,
    ) {
        if (playlistItems.isEmpty()) return
        playbackViewModel.playQueue(
            uriStrings = playlistItems.map { item -> item.contentUri },
            titles = playlistItems.map { item -> item.displayTitle },
            durationMsList = playlistItems.map { item -> item.durationMs },
            startIndex = startIndex,
            shuffleEnabled = shuffle,
            repeatMode = repeatMode,
            queueOrigin = queueOrigin,
        )
        playbackViewModel.closeFullPlayer()
    }
    */

    Column(modifier = modifier.fillMaxSize()) {
        if (isInSelectionMode) {
            // 选择模式工具栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HeaderGradientStart, HeaderGradientEnd),
                        ),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        isInSelectionMode = false
                        selectedIds.clear()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_exit_selection),
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = stringResource(R.string.selection_count, selectedIds.size),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    )
                    IconButton(
                        onClick = { showBatchRemoveConfirm = true },
                        enabled = selectedIds.isNotEmpty(),
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = if (selectedIds.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.4f),
                        )
                    }
                }
            }
        } else {
            // 正常顶栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(HeaderGradientStart, HeaderGradientEnd),
                        ),
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = PlaylistRepository.getLocalizedPlaylistName(context, playlistName),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.playlist_track_count, playlistItems.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.88f),
                        )
                    }
                    IconButton(
                        onClick = {
                            isInSelectionMode = true
                        },
                    ) {
                        Icon(
                            Icons.Default.Checklist,
                            contentDescription = stringResource(R.string.cd_select_items),
                            tint = Color.White,
                        )
                    }
                    IconButton(
                        onClick = { showAddSheet = true },
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.cd_add_videos),
                            tint = Color.White,
                        )
                    }
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            /*
            item {
                PlaylistPlayModesCard(
                    enabled = playlistItems.isNotEmpty(),
                    onPlayAll = { playWithMode(shuffle = false, repeatMode = Player.REPEAT_MODE_ALL) },
                    onSequential = { playWithMode(shuffle = false, repeatMode = Player.REPEAT_MODE_ALL) },
                    onShuffle = { playWithMode(shuffle = true, repeatMode = Player.REPEAT_MODE_ALL) },
                )
            }
            */
            itemsIndexed(
                items = playlistItems,
                key = { _, item: PlaylistItemEntity -> item.id },
            ) { index: Int, row: PlaylistItemEntity ->
                val isSelected = selectedIds.containsKey(row.id)
                PlaylistItemRow(
                    index = index + 1,
                    item = row,
                    isSelected = isSelected,
                    isSelectionMode = isInSelectionMode,
                    onPlayInMiniPlayer = {
                        val idx = playlistItems.indexOf(row)
                        if (idx >= 0) {
                            val action = {
                                playbackViewModel.playQueue(
                                    uriStrings = playlistItems.map { item -> item.contentUri },
                                    titles = playlistItems.map { item -> item.displayTitle },
                                    durationMsList = playlistItems.map { item -> item.durationMs },
                                    startIndex = idx,
                                    queueOrigin = queueOrigin,
                                )
                                playbackViewModel.closeFullPlayer()
                            }
                            if (LegalAgreementCache.hasAgreed) {
                                action()
                            } else {
                                pendingPlayAction = action
                                showLegalDialog = true
                            }
                        }
                    },
                    onOpenFullPlayer = {
                        val idx = playlistItems.indexOf(row)
                        if (idx >= 0) {
                            val action = {
                                playbackViewModel.playQueue(
                                    uriStrings = playlistItems.map { item -> item.contentUri },
                                    titles = playlistItems.map { item -> item.displayTitle },
                                    durationMsList = playlistItems.map { item -> item.durationMs },
                                    startIndex = idx,
                                    queueOrigin = queueOrigin,
                                )
                                playbackViewModel.openFullPlayer()
                            }
                            if (LegalAgreementCache.hasAgreed) {
                                action()
                            } else {
                                pendingPlayAction = action
                                showLegalDialog = true
                            }
                        }
                    },
                    onOpenMenu = { menuVideo = row },
                    onLongClick = {
                        if (!isInSelectionMode) {
                            isInSelectionMode = true
                            selectedIds[row.id] = true
                        }
                    },
                    onToggleSelect = {
                        if (isSelected) {
                            selectedIds.remove(row.id)
                            if (selectedIds.isEmpty()) {
                                isInSelectionMode = false
                            }
                        } else {
                            selectedIds[row.id] = true
                        }
                    },
                )
            }
        }
    }

    // 底部菜单
    if (menuVideo != null) {
        val v = menuVideo!!
        val uriStr = v.contentUri
        val isFavorite = uriStr in favoriteUriSet
        val membershipFlow = remember(v) {
            playlistViewModel.observePlaylistIdsContainingVideo(uriStr)
        }
        val playlistIdsContainingVideo by membershipFlow.collectAsStateWithLifecycle(initialValue = emptySet())

        ModalBottomSheet(
            onDismissRequest = { menuVideo = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            PlaylistMoreOptionsSheetContent(
                item = v,
                playlists = playlists,
                playlistIdsContainingVideo = playlistIdsContainingVideo,
                isFavorite = isFavorite,
                onToggleVideoInPlaylist = { pid ->
                    playlistViewModel.toggleVideoInPlaylist(pid, LocalVideo(
                        id = 0,
                        contentUri = android.net.Uri.parse(v.contentUri),
                        displayName = v.displayTitle,
                        durationMs = v.durationMs,
                        sizeBytes = 0,
                        dateAddedSec = 0,
                    ))
                },
                onToggleFavorite = {
                    onToggleFavorite(LocalVideo(
                        id = 0,
                        contentUri = android.net.Uri.parse(v.contentUri),
                        displayName = v.displayTitle,
                        durationMs = v.durationMs,
                        sizeBytes = 0,
                        dateAddedSec = 0,
                    ))
                },
                onRemove = { playlistViewModel.removePlaylistItem(v.id) },
                onNewPlaylist = {
                    menuVideo = null
                    newPlaylistName = ""
                    showNewPlaylistDialog = true
                },
                onDismiss = { menuVideo = null },
            )
        }
    }

    // 新建歌单对话框
    if (showNewPlaylistDialog) {
        val defaultName = stringResource(R.string.default_playlist_name)
        AlertDialog(
            onDismissRequest = { showNewPlaylistDialog = false },
            title = { Text(stringResource(R.string.dialog_new_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    placeholder = { Text(stringResource(R.string.dialog_new_playlist_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreatePlaylist(newPlaylistName.trim().ifBlank { defaultName })
                        showNewPlaylistDialog = false
                        newPlaylistName = ""
                    },
                ) {
                    Text(stringResource(R.string.action_create))
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylistDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // 批量移除确认对话框
    if (showBatchRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchRemoveConfirm = false },
            title = { Text(stringResource(R.string.delete_playlist_items_confirm_title)) },
            text = { Text(stringResource(R.string.delete_playlist_items_confirm_message, selectedIds.size)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        playlistViewModel.removePlaylistItems(selectedIds.keys.toList())
                        selectedIds.clear()
                        isInSelectionMode = false
                        showBatchRemoveConfirm = false
                    },
                ) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showBatchRemoveConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
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
                    onNavigateToLegal()
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

    // 从资源库添加视频
    if (showAddSheet) {
        AddFromLibrarySheet(
            libraryVideos = libraryVideos,
            existingContentUris = existingContentUris,
            onConfirm = { selected ->
                selected.forEach { video ->
                    playlistViewModel.addVideoToPlaylist(playlistId, video)
                }
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false },
        )
    }
}

/*
@Composable
private fun PlaylistPlayModesCard(
    enabled: Boolean,
    onPlayAll: () -> Unit,
    onSequential: () -> Unit,
    onShuffle: () -> Unit,
) {
    val corner = RoundedCornerShape(14.dp)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPlayAll,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp),
                    shape = corner,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.play_all),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                OutlinedButton(
                    onClick = onSequential,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp),
                    shape = corner,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.playlist_play_sequential),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
                OutlinedButton(
                    onClick = onShuffle,
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp),
                    shape = corner,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.playlist_play_shuffle),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.playlist_play_mode_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}
*/

@Composable
private fun PlaylistItemRow(
    index: Int,
    item: PlaylistItemEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onPlayInMiniPlayer: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenMenu: () -> Unit,
    onLongClick: () -> Unit,
    onToggleSelect: () -> Unit,
) {
    VideoListItem(
        title = item.displayTitle,
        subtitle = formatDurationMs(item.durationMs),
        thumbnailUri = item.contentUri,
        durationMs = item.durationMs,
        onPlayInMiniPlayer = onPlayInMiniPlayer,
        onOpenFullPlayer = onOpenFullPlayer,
        onOpenMenu = onOpenMenu,
        showDurationOnThumbnail = false,
        isSelected = isSelected,
        isSelectionMode = isSelectionMode,
        onLongClick = onLongClick,
        onToggleSelect = onToggleSelect,
    )
}
