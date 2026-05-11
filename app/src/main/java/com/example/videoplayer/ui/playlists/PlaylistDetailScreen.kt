package com.example.videoplayer.ui.playlists

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.example.videoplayer.R
import com.example.videoplayer.data.local.PlaylistItemEntity
import com.example.videoplayer.data.readDisplayNameForUri
import com.example.videoplayer.playback.MainPlaybackViewModel
import com.example.videoplayer.playback.PlaybackQueueOrigin
import com.example.videoplayer.playlists.PlaylistViewModel
import com.example.videoplayer.ui.formatDurationMs
import com.example.videoplayer.ui.media.VideoThumbnailImage
import com.example.videoplayer.ui.theme.HeaderGradientEnd
import com.example.videoplayer.ui.theme.HeaderGradientStart

/**
 * 单个歌单：渐变顶栏、播放全部与顺序/随机、从文件选择器批量添加、删除条目。
 * 列表项：点序号/标题区在底部迷你播放器起播；仅点缩略图进入全屏播放页（与本地库一致）。
 */
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistName: String,
    onBack: () -> Unit,
    playlistViewModel: PlaylistViewModel,
    playbackViewModel: MainPlaybackViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val itemsFlow = remember(playlistId) { playlistViewModel.playlistItemsFlow(playlistId) }
    val playlistItems by itemsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val queueOrigin = remember(playlistId, playlistName) {
        PlaybackQueueOrigin.UserPlaylist(playlistId, playlistName)
    }

    val missingDurationCount = playlistItems.count { it.durationMs <= 0L }
    LaunchedEffect(playlistId, missingDurationCount) {
        if (playlistItems.isNotEmpty() && missingDurationCount > 0) {
            playlistViewModel.probeMissingDurationsInPlaylist(context, playlistId)
        }
    }

    val pickVideos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
    ) { uris ->
        uris.forEach { uri ->
            val title = context.readDisplayNameForUri(uri)
            playlistViewModel.addContentUriToPlaylist(playlistId, uri, title)
        }
    }

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

    Column(modifier = modifier.fillMaxSize()) {
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
                        text = playlistName,
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
                    onClick = { pickVideos.launch("video/*") },
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.cd_add_videos),
                        tint = Color.White,
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 28.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                PlaylistPlayModesCard(
                    enabled = playlistItems.isNotEmpty(),
                    onPlayAll = { playWithMode(shuffle = false, repeatMode = Player.REPEAT_MODE_ALL) },
                    onSequential = { playWithMode(shuffle = false, repeatMode = Player.REPEAT_MODE_ALL) },
                    onShuffle = { playWithMode(shuffle = true, repeatMode = Player.REPEAT_MODE_ALL) },
                )
            }
            itemsIndexed(
                items = playlistItems,
                key = { _, item: PlaylistItemEntity -> item.id },
            ) { index: Int, row: PlaylistItemEntity ->
                PlaylistItemRow(
                    index = index + 1,
                    item = row,
                    onPlayInMiniPlayer = {
                        val idx = playlistItems.indexOf(row)
                        if (idx >= 0) {
                            playbackViewModel.playQueue(
                                uriStrings = playlistItems.map { item -> item.contentUri },
                                titles = playlistItems.map { item -> item.displayTitle },
                                durationMsList = playlistItems.map { item -> item.durationMs },
                                startIndex = idx,
                                queueOrigin = queueOrigin,
                            )
                            playbackViewModel.closeFullPlayer()
                        }
                    },
                    onOpenFullPlayer = {
                        val idx = playlistItems.indexOf(row)
                        if (idx >= 0) {
                            playbackViewModel.playQueue(
                                uriStrings = playlistItems.map { item -> item.contentUri },
                                titles = playlistItems.map { item -> item.displayTitle },
                                durationMsList = playlistItems.map { item -> item.durationMs },
                                startIndex = idx,
                                queueOrigin = queueOrigin,
                            )
                            playbackViewModel.openFullPlayer()
                        }
                    },
                    onRemove = { playlistViewModel.removePlaylistItem(row.id) },
                )
            }
        }
    }
}

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

@Composable
private fun PlaylistItemRow(
    index: Int,
    item: PlaylistItemEntity,
    onPlayInMiniPlayer: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.5.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 54.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_open_fullscreen_player),
                        onClick = onOpenFullPlayer,
                    ),
            ) {
                VideoThumbnailImage(
                    videoUri = item.contentUri.toUri(),
                    contentScale = ContentScale.Crop,
                    requestWidth = 240,
                    requestHeight = 135,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_play_in_mini_player),
                        onClick = onPlayInMiniPlayer,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = index.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(22.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        formatDurationMs(item.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_remove))
            }
        }
    }
}
