package com.joshua.videoplayer.ui.playlists

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.joshua.videoplayer.data.PlaylistRepository
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.local.PlaylistEntity
import com.joshua.videoplayer.data.local.PlaylistWithCount
import com.joshua.videoplayer.playlists.PlaylistViewModel
import com.joshua.videoplayer.ui.media.VideoThumbnailImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 歌单网格（对齐 aether-audio「我的歌单」双列封面墙）。
 */
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistViewModel,
    onOpenPlaylist: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val defaultPlaylistName = stringResource(R.string.default_playlist_name)
    var coverDialogRow by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var pickCoverTargetId by remember { mutableLongStateOf(0L) }
    var contextMenuRow by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var deleteConfirmRow by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var renameRow by remember { mutableStateOf<PlaylistWithCount?>(null) }
    var renameText by remember { mutableStateOf("") }

    val pickCoverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        val id = pickCoverTargetId
        pickCoverTargetId = 0L
        if (id != 0L && uri != null) {
            viewModel.setPlaylistCover(id, uri)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.screen_playlists_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 96.dp),
            ) {
                items(
                    playlists.filter {
                        it.playlist.kind != PlaylistEntity.KIND_IGNORED || it.itemCount > 0
                    },
                    key = { it.playlist.id },
                ) { row ->
                    val isSystemPlaylist = row.playlist.kind == PlaylistEntity.KIND_LIKED || row.playlist.kind == PlaylistEntity.KIND_IGNORED
                    PlaylistGridCard(
                        row = row,
                        onClick = { onOpenPlaylist(row.playlist.id, row.playlist.name) },
                        onLongClick = {
                            if (!isSystemPlaylist) {
                                contextMenuRow = row
                            }
                        },
                    )
                }
            }
        }
        FloatingActionButton(
            onClick = {
                newName = ""
                showCreate = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_new_playlist))
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text(stringResource(R.string.dialog_new_playlist_title)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    placeholder = { Text(stringResource(R.string.dialog_new_playlist_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newName.ifBlank { defaultPlaylistName })
                        showCreate = false
                    },
                ) { Text(stringResource(R.string.action_create)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreate = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    val coverRow = coverDialogRow
    if (coverRow != null) {
        val hasCustomCover = !coverRow.playlist.coverImageUri.isNullOrBlank()
        AlertDialog(
            onDismissRequest = { coverDialogRow = null },
            title = { Text(stringResource(R.string.playlist_cover_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.playlist_cover_dialog_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = {
                            pickCoverTargetId = coverRow.playlist.id
                            pickCoverLauncher.launch("image/*")
                            coverDialogRow = null
                        },
                    ) { Text(stringResource(R.string.playlist_cover_pick_image)) }
                    if (hasCustomCover) {
                        TextButton(
                            onClick = {
                                viewModel.setPlaylistCover(coverRow.playlist.id, null)
                                coverDialogRow = null
                            },
                        ) { Text(stringResource(R.string.playlist_cover_clear_custom)) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { coverDialogRow = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Context menu on long-press
    val ctxRow = contextMenuRow
    if (ctxRow != null) {
        val isNormal = ctxRow.playlist.kind == PlaylistEntity.KIND_NORMAL
        AlertDialog(
            onDismissRequest = { contextMenuRow = null },
            title = { Text(stringResource(R.string.playlist_context_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            contextMenuRow = null
                            coverDialogRow = ctxRow
                        },
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(stringResource(R.string.playlist_context_cover), modifier = Modifier.padding(start = 8.dp))
                    }
                    if (isNormal) {
                        TextButton(
                            onClick = {
                                contextMenuRow = null
                                renameText = ctxRow.playlist.name
                                renameRow = ctxRow
                            },
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                            Text(stringResource(R.string.playlist_context_rename), modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    if (isNormal) {
                        TextButton(
                            onClick = {
                                contextMenuRow = null
                                deleteConfirmRow = ctxRow
                            },
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
                            Text(stringResource(R.string.playlist_context_delete), modifier = Modifier.padding(start = 8.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuRow = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Delete confirmation dialog
    val deleteRow = deleteConfirmRow
    if (deleteRow != null) {
        AlertDialog(
            onDismissRequest = { deleteConfirmRow = null },
            title = { Text(stringResource(R.string.playlist_delete_confirm_title)) },
            text = { Text(stringResource(R.string.playlist_delete_confirm_message, deleteRow.playlist.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePlaylist(deleteRow.playlist.id)
                        deleteConfirmRow = null
                    },
                ) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmRow = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    // Rename dialog
    val renameTarget = renameRow
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameRow = null },
            title = { Text(stringResource(R.string.playlist_rename_title)) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    placeholder = { Text(stringResource(R.string.playlist_rename_hint)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = renameText.trim()
                        if (name.isNotBlank()) {
                            viewModel.renamePlaylist(renameTarget.playlist.id, name)
                        }
                        renameRow = null
                    },
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { renameRow = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistGridCard(
    row: PlaylistWithCount,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val isLiked = row.playlist.kind == PlaylistEntity.KIND_LIKED
    val isIgnored = row.playlist.kind == PlaylistEntity.KIND_IGNORED
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (!isLiked && !isIgnored) {
                    onLongClick
                } else {
                    null
                },
            ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp,
        ) {
            PlaylistCardArtwork(row = row)
        }
        Text(
            text = PlaylistRepository.getLocalizedPlaylistName(LocalContext.current, row.playlist.name),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.playlist_track_count, row.itemCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaylistCardArtwork(row: PlaylistWithCount) {
    val seed = row.playlist.id xor row.playlist.createdAtMillis
    val gradient = remember(seed) { softPlaylistGradient(seed) }
    val isLiked = row.playlist.kind == PlaylistEntity.KIND_LIKED
    val isIgnored = row.playlist.kind == PlaylistEntity.KIND_IGNORED
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
    ) {
        when {
            isLiked -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = stringResource(R.string.cd_playlist_liked_artwork),
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
            isIgnored -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Block,
                        contentDescription = null,
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
            !row.playlist.coverImageUri.isNullOrBlank() -> {
                PlaylistUserCoverImage(uriString = row.playlist.coverImageUri!!)
            }
            !row.firstItemContentUri.isNullOrBlank() -> {
                VideoThumbnailImage(
                    videoUri = row.firstItemContentUri.toUri(),
                    contentScale = ContentScale.Crop,
                    requestWidth = 512,
                    requestHeight = 512,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun PlaylistUserCoverImage(uriString: String) {
    val context = LocalContext.current
    var frame by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        frame = withContext(Dispatchers.IO) {
            runCatching {
                val uri = uriString.toUri()
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
    if (frame != null) {
        Image(
            bitmap = frame!!,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private fun softPlaylistGradient(seed: Long): Brush {
    val rng = java.util.Random(seed)
    val hue1 = rng.nextInt(360).toFloat()
    val hue2 = rng.nextInt(360).toFloat()
    val c0 = Color.hsl(hue1, 0.42f, 0.90f)
    val c1 = Color.hsl(hue2, 0.32f, 0.94f)
    return Brush.linearGradient(colors = listOf(c0, c1))
}
