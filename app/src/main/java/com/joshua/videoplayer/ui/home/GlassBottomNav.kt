package com.joshua.videoplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.R

/**
 * 底部主导航：贴底全宽；选中态为图标胶囊底 + 主色强调（原玻璃条上的交互样式）。
 */
enum class AppTab {
    Library,
    Playlists,
    Settings,
}

@Composable
fun GlassBottomNav(
    current: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(
                selected = current == AppTab.Library,
                onClick = { onSelect(AppTab.Library) },
                label = stringResource(R.string.tab_library),
                icon = Icons.Outlined.Folder,
            )
            NavItem(
                selected = current == AppTab.Playlists,
                onClick = { onSelect(AppTab.Playlists) },
                label = stringResource(R.string.tab_playlists),
                icon = Icons.AutoMirrored.Outlined.QueueMusic,
            )
            NavItem(
                selected = current == AppTab.Settings,
                onClick = { onSelect(AppTab.Settings) },
                label = stringResource(R.string.tab_settings),
                icon = Icons.Outlined.Settings,
            )
        }
    }
}

@Composable
private fun NavItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent,
                )
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint)
        }
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            letterSpacing = 0.6.sp,
        )
    }
}
