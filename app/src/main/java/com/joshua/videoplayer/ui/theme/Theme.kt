package com.joshua.videoplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VideoPlayerTheme(
    content: @Composable () -> Unit
) {
    // 从 ThemeColorManager 获取当前主题颜色
    val colorScheme = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer)

    val uiColorScheme = lightColorScheme(
        primary = colorScheme.primary,
        onPrimary = colorScheme.onPrimary,
        secondary = colorScheme.primaryContainer,
        onSecondary = colorScheme.onPrimary,
        tertiary = colorScheme.primaryContainer,
        onTertiary = colorScheme.onPrimary,
        background = colorScheme.background,
        onBackground = colorScheme.onBackground,
        surface = colorScheme.surface,
        onSurface = colorScheme.onSurface,
        surfaceVariant = colorScheme.surfaceVariant,
        onSurfaceVariant = colorScheme.onSurfaceVariant,
    )

    MaterialTheme(
        colorScheme = uiColorScheme,
        typography = Typography,
        shapes = UiShapes,
        content = content
    )
}