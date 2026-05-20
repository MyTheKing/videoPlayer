package com.joshua.videoplayer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.formatDurationMs
import com.joshua.videoplayer.ui.media.VideoThumbnailImage

/**
 * 通用视频列表项组件，支持本地库和歌单两种模式。
 *
 * @param title 视频标题
 * @param subtitle 副标题（本地库显示大小·日期，歌单显示时长）
 * @param thumbnailUri 缩略图 URI
 * @param durationMs 时长（毫秒），用于在缩略图上显示时长标签
 * @param onPlayInMiniPlayer 点击标题区域在迷你播放器播放
 * @param onOpenFullPlayer 点击缩略图进入全屏播放
 * @param onOpenMenu 点击3点菜单按钮
 * @param showDurationOnThumbnail 是否在缩略图上显示时长标签
 * @param isSelected 是否被选中（多选模式）
 * @param isSelectionMode 是否处于多选模式
 * @param onLongClick 长按回调（用于进入多选模式）
 * @param onToggleSelect 切换选中状态回调
 * @param modifier Modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListItem(
    title: String,
    subtitle: String,
    thumbnailUri: String,
    durationMs: Long,
    onPlayInMiniPlayer: () -> Unit,
    onOpenFullPlayer: () -> Unit,
    onOpenMenu: () -> Unit,
    showDurationOnThumbnail: Boolean = true,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onToggleSelect: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isSelectionMode) {
                    Modifier.combinedClickable(
                        onClick = { onToggleSelect?.invoke() },
                        onLongClick = onLongClick,
                    )
                } else {
                    Modifier
                },
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = 0.5.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .size(width = 112.dp, height = 64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (isSelectionMode) Modifier
                        else Modifier.clickable(
                            onClickLabel = stringResource(R.string.cd_open_fullscreen_player),
                            onClick = onOpenFullPlayer,
                        ),
                    ),
            ) {
                VideoThumbnailImage(
                    videoUri = thumbnailUri.toUri(),
                    contentScale = ContentScale.Crop,
                    requestWidth = 320,
                    requestHeight = 180,
                    modifier = Modifier.fillMaxSize(),
                )
                if (showDurationOnThumbnail && durationMs > 0) {
                    Text(
                        text = formatDurationMs(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            // 标题和副标题
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isSelectionMode) Modifier
                        else Modifier.clickable(
                            onClickLabel = stringResource(R.string.cd_play_in_mini_player),
                            onClick = onPlayInMiniPlayer,
                        ),
                    ),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            // 右侧图标：选择模式显示 checkbox，否则显示菜单按钮
            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .size(26.dp)
                        .padding(end = 4.dp),
                )
            } else {
                IconButton(onClick = onOpenMenu) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more),
                    )
                }
            }
        }
    }
}
