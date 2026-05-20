package com.joshua.videoplayer.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.PlaylistRepository
import com.joshua.videoplayer.data.local.PlaylistEntity
import com.joshua.videoplayer.data.local.PlaylistItemEntity
import com.joshua.videoplayer.data.local.PlaylistWithCount

/**
 * 歌单详情「⋯」菜单：Material 3 底部表内容区（与本地库菜单一致，忽略换成移除）。
 */
@Composable
fun PlaylistMoreOptionsSheetContent(
    item: PlaylistItemEntity,
    playlists: List<PlaylistWithCount>,
    playlistIdsContainingVideo: Set<Long>,
    isFavorite: Boolean,
    onToggleVideoInPlaylist: (Long) -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    onNewPlaylist: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 28.dp),
    ) {
        Text(
            text = stringResource(R.string.sheet_more_options_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = item.displayTitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )

        // —— 添加到歌单 + 嵌套列表（过滤掉系统歌单）
        val normalPlaylists = playlists.filter {
            it.playlist.kind == PlaylistEntity.KIND_NORMAL
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.sheet_add_to_playlist),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (normalPlaylists.isEmpty()) {
            Text(
                text = stringResource(R.string.no_playlists_create_first),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 36.dp, bottom = 16.dp),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .height(IntrinsicSize.Min)
                    .padding(start = 8.dp, bottom = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Column(
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    normalPlaylists.forEach { row ->
                        val inPlaylist = row.playlist.id in playlistIdsContainingVideo
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .clickable {
                                    onToggleVideoInPlaylist(row.playlist.id)
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = PlaylistRepository.getLocalizedPlaylistName(context, row.playlist.name),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            RadioButton(
                                selected = inPlaylist,
                                onClick = { onToggleVideoInPlaylist(row.playlist.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        // 喜欢
        SheetActionRow(
            icon = {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                )
            },
            title = stringResource(
                if (isFavorite) R.string.sheet_remove_favorite else R.string.sheet_add_favorite,
            ),
            onClick = onToggleFavorite,
        )

        // 移除（代替忽略）
        SheetActionRow(
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = stringResource(R.string.sheet_remove_from_playlist),
            onClick = {
                onRemove()
                onDismiss()
            },
        )

        HorizontalDivider(Modifier.padding(vertical = 12.dp))

        // 新建歌单
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onNewPlaylist)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.sheet_new_playlist),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SheetActionRow(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            icon()
        }
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}
