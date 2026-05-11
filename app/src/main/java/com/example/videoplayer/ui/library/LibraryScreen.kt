package com.example.videoplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videoplayer.R
import com.example.videoplayer.data.LocalVideo
import com.example.videoplayer.data.local.PlaylistWithCount
import com.example.videoplayer.library.LibraryViewModel
import com.example.videoplayer.playlists.PlaylistViewModel
import com.example.videoplayer.ui.formatDateAddedSeconds
import com.example.videoplayer.ui.formatDurationMs
import com.example.videoplayer.ui.formatSizeBytes
import com.example.videoplayer.ui.media.VideoThumbnailImage

/**
 * 本地视频库：扫描列表、搜索过滤；点标题区域在底部迷你播放器开始播，点缩略图进全屏；「⋯」使用 [ModalBottomSheet]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    playlistViewModel: PlaylistViewModel,
    playlists: List<PlaylistWithCount>,
    /** 使用当前列表（含搜索过滤）作为播放队列，[startIndex] 为所点条目在 [queue] 中的下标。 */
    onPlayInLibrary: (queue: List<LocalVideo>, startIndex: Int) -> Unit,
    onOpenFullLibrary: (queue: List<LocalVideo>, startIndex: Int) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filtered by viewModel.filteredVideos.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val favoriteUris by viewModel.favoriteUriSet.collectAsStateWithLifecycle()

    var menuVideo by remember { mutableStateOf<LocalVideo?>(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.screen_library_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.cd_scan))
            }
        }
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::setSearchQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_videos_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.library_recent_count, filtered.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(filtered, key = { _, v -> v.contentUri.toString() }) { index, video ->
                LibraryVideoRow(
                    video = video,
                    onPlayInMiniPlayer = { onPlayInLibrary(filtered, index) },
                    onOpenFullPlayer = { onOpenFullLibrary(filtered, index) },
                    onOpenMenu = { menuVideo = video },
                )
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (menuVideo != null) {
        val v = menuVideo!!
        val uriStr = v.contentUri.toString()
        val isFavorite = uriStr in favoriteUris
        val membershipFlow = remember(v) {
            playlistViewModel.observePlaylistIdsContainingVideo(uriStr)
        }
        val playlistIdsContainingVideo by membershipFlow.collectAsStateWithLifecycle(initialValue = emptySet())
        ModalBottomSheet(
            onDismissRequest = { menuVideo = null },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
        ) {
            LibraryMoreOptionsSheetContent(
                video = v,
                playlists = playlists,
                playlistIdsContainingVideo = playlistIdsContainingVideo,
                isFavorite = isFavorite,
                onToggleVideoInPlaylist = { playlistId ->
                    playlistViewModel.toggleVideoInPlaylist(playlistId, v)
                },
                onToggleFavorite = {
                    if (isFavorite) {
                        viewModel.removeFavorite(v)
                    } else {
                        viewModel.addFavorite(v)
                    }
                },
                onIgnore = {
                    viewModel.ignoreVideo(v)
                    menuVideo = null
                },
                onNewPlaylist = {
                    menuVideo = null
                    newPlaylistName = ""
                    showNewPlaylistDialog = true
                },
            )
        }
    }

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
                        onCreatePlaylist(
                            newPlaylistName.trim().ifBlank { defaultName },
                        )
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
}

@Composable
private fun LibraryVideoRow(
    video: LocalVideo,
    onPlayInMiniPlayer: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_open_fullscreen_player),
                        onClick = onOpenFullPlayer,
                    ),
            ) {
                VideoThumbnailImage(
                    videoUri = video.contentUri,
                    contentScale = ContentScale.Crop,
                    requestWidth = 320,
                    requestHeight = 180,
                    modifier = Modifier.fillMaxSize(),
                )
                Text(
                    text = formatDurationMs(video.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_play_in_mini_player),
                        onClick = onPlayInMiniPlayer,
                    ),
            ) {
                Text(
                    text = video.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                Text(
                    text = "${formatSizeBytes(video.sizeBytes)} · ${formatDateAddedSeconds(video.dateAddedSec)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            IconButton(onClick = onOpenMenu) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more))
            }
        }
    }
}
