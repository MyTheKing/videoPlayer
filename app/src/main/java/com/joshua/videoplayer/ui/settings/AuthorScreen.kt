package com.joshua.videoplayer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.VolunteerActivism
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.theme.ContentBackground
import com.joshua.videoplayer.ui.theme.ThemeColorManager

private val GlassCardBg = Color(0x66FFFFFF)
private val TextOnSurface = Color(0xFF181445)
private val TextOnSurfaceVariant = Color(0xFF464555)
private val OutlineVariant = Color(0xFFC7C4D8)
private val PrimaryContainer: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer

@Composable
fun AuthorScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(ContentBackground)) {
        AuthorTopBar(onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            // ── Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp, bottom = 24.dp),
            ) {
                Text(
                    text = "Joshua Wang",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                    ),
                    color = TextOnSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.author_creator_label),
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        fontSize = 13.sp,
                    ),
                    color = TextOnSurfaceVariant,
                )
            }

            HorizontalDivider(
                color = OutlineVariant.copy(alpha = 0.5f),
                thickness = 1.dp,
            )

            Spacer(Modifier.height(24.dp))

            // ── 一封信 ──
            AuthorLetterCard()

            Spacer(Modifier.height(32.dp))

            // ── 底部 ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.author_free_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = OutlineVariant.copy(alpha = 0.3f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.VolunteerActivism,
                            contentDescription = null,
                            tint = TextOnSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.author_free_badge),
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.5.sp,
                            ),
                            color = TextOnSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun AuthorLetterCard() {
    val gradientColors = listOf(
        PrimaryContainer.copy(alpha = 0.8f),
        PrimaryContainer.copy(alpha = 0.3f),
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassCardBg,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        PrimaryContainer.copy(alpha = 0.3f),
                        Color.White.copy(alpha = 0.1f),
                    ),
                ),
                shape = RoundedCornerShape(16.dp),
            ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Brush.horizontalGradient(gradientColors)),
            )

            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.legal_author_content),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 26.sp,
                    ),
                    color = TextOnSurfaceVariant,
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(R.string.legal_author_signature),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontStyle = FontStyle.Italic,
                    ),
                    color = PrimaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun AuthorTopBar(onBack: () -> Unit) {
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
                text = stringResource(R.string.settings_about_author),
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
