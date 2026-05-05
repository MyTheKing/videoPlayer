package com.example.videoplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val UiColorScheme = lightColorScheme(
    primary = BlueAccent,
    onPrimary = OnPrimary,
    secondary = BlueAccentLight,
    onSecondary = OnPrimary,
    tertiary = BlueAccentLight,
    onTertiary = OnPrimary,
    background = ContentBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary
)

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
    MaterialTheme(
        colorScheme = UiColorScheme,
        typography = Typography,
        shapes = UiShapes,
        content = content
    )
}