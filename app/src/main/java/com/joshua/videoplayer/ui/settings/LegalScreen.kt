package com.joshua.videoplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.theme.ContentBackground
import com.joshua.videoplayer.ui.theme.ThemeColorManager

private val GlassCardBg = Color(0x66FFFFFF)
private val TextOnSurface = Color(0xFF181445)
private val TextOnSurfaceVariant = Color(0xFF464555)
private val PrimaryContainer: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer

@Composable
fun LegalScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(ContentBackground)) {
        LegalTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            // ── 免责声明 ──
            LegalSection(
                icon = Icons.Outlined.Gavel,
                title = stringResource(R.string.legal_section_disclaimer),
                content = stringResource(R.string.legal_disclaimer_content),
            )

            Spacer(Modifier.height(20.dp))

            // ── 数据丢失 ──
            LegalSection(
                icon = Icons.Outlined.Storage,
                title = stringResource(R.string.legal_section_data),
                content = stringResource(R.string.legal_data_content),
            )

            Spacer(Modifier.height(20.dp))

            // ── 责任限制 ──
            LegalSection(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.legal_section_liability),
                content = stringResource(R.string.legal_liability_content),
            )

            Spacer(Modifier.height(20.dp))

            // ── 用户协议 ──
            LegalSection(
                icon = Icons.Outlined.Gavel,
                title = stringResource(R.string.legal_section_agreement),
                content = stringResource(R.string.legal_agreement_content),
            )

            Spacer(Modifier.height(28.dp))

            // ── 生效日期 ──
            Text(
                text = stringResource(R.string.legal_effective_date),
                style = MaterialTheme.typography.labelSmall,
                color = TextOnSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            )

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun LegalTopBar(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ContentBackground.copy(alpha = 0.6f))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = TextOnSurface,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.legal_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                ),
                color = TextOnSurface,
            )
        }
        HorizontalDivider(
            color = Color.White.copy(alpha = 0.1f),
            thickness = 1.dp,
        )
    }
}

@Composable
private fun LegalSection(icon: ImageVector, title: String, content: String) {
    // Section label with icon
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextOnSurface,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
                fontSize = 14.sp,
            ),
            color = TextOnSurface,
        )
    }

    // Content card
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 24.sp,
                ),
                color = TextOnSurfaceVariant,
            )
        }
    }
}
