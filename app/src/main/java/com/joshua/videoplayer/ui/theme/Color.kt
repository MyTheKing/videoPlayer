package com.joshua.videoplayer.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/* Aether / stitch 设计稿：靛蓝主色 + 浅紫灰底 */
val HeaderGradientStart: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer
val HeaderGradientEnd: Color
    @Composable get() = ThemeColorManager.currentPrimary
val ContentBackground: Color
    @Composable get() = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer).background
val CardSurface = Color(0xFFFFFFFF)
val TextPrimary: Color
    @Composable get() = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer).onBackground
val TextSecondary: Color
    @Composable get() = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer).onSurfaceVariant
val BlueAccent: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer
val BlueAccentLight: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer.copy(alpha = 0.8f)
val OnPrimary = Color(0xFFFFFFFF)

val PlayerGradientTop: Color
    @Composable get() = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer).surfaceDim
val PlayerGradientBottom: Color
    @Composable get() = ThemeColorManager.generateColorScheme(ThemeColorManager.currentPrimaryContainer).surfaceVariant
val GlassBarTint = Color(0x66FFFFFF)
val GlassNavBackground = Color(0xB3FFFFFF)
val GlassNavBorder = Color(0x33FFFFFF)
val MiniPlayerBackground = Color(0xE6FFFFFF)

/* 播放页深色沉浸配色 */
val PlayerBackground = Color(0xFF0D0D0D)
val PlayerSurface = Color(0xFF1A1A2E)
val PlayerSurfaceAlpha = Color(0xCC1A1A2E) // 80% opacity
val PlayerOnSurface = Color(0xFFFFFFFF)
val PlayerOnSurfaceDim = Color(0x99FFFFFF)  // 60% white
val PlayerAccent: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer.copy(alpha = 0.8f)
val PlayerAccentDim = Color(0xFF3A3A5C)     // 循环按钮关闭态
