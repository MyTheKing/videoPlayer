package com.joshua.videoplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.ui.theme.ThemeColorManager
import kotlin.math.roundToInt

/**
 * 颜色板选择器
 * 包含色相条、饱和度-亮度面板和透明度条
 */
@Composable
fun ColorPicker(
    initialColor: Color = Color(0xFF4F46E5),
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    // 转换初始颜色为 HSV
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    // 当外部颜色变化时，同步更新内部状态
    LaunchedEffect(initialColor) {
        val newHsv = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), newHsv)
        hue = newHsv[0]
        saturation = newHsv[1]
        value = newHsv[2]
        alpha = initialColor.alpha
    }

    fun updateColor(h: Float, s: Float, v: Float, a: Float = alpha) {
        hue = h
        saturation = s
        value = v
        alpha = a
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))
        val color = Color(argb).copy(alpha = a)
        onColorChanged(color)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 预览当前颜色
        val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
            .copy(alpha = alpha)

        // 饱和度-亮度面板
        SaturationValuePanel(
            hue = hue,
            saturation = saturation,
            value = value,
            onSaturationValueChanged = { s, v -> updateColor(hue, s, v) },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Spacer(Modifier.height(16.dp))

        // 色相条
        HueBar(
            hue = hue,
            onHueChanged = { h -> updateColor(h, saturation, value) },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp)),
        )

        Spacer(Modifier.height(16.dp))

        // 透明度条
        AlphaBar(
            alpha = alpha,
            color = currentColor.copy(alpha = 1f),
            onAlphaChanged = { a -> updateColor(hue, saturation, value, a) },
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp)),
        )

        Spacer(Modifier.height(16.dp))

        // 颜色预览和十六进制值
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 预览色块（带棋盘格背景表示透明度）
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                // 棋盘格背景
                CheckerboardBackground(modifier = Modifier.matchParentSize())
                // 颜色
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(currentColor)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "当前颜色",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
                // 显示 ARGB 十六进制值
                val hexValue = if (alpha < 1f) {
                    val alphaHex = (alpha * 255).roundToInt().toString(16).padStart(2, '0').uppercase()
                    "#${alphaHex}${Integer.toHexString(currentColor.toArgb()).substring(2).uppercase()}"
                } else {
                    "#${Integer.toHexString(currentColor.toArgb()).substring(2).uppercase()}"
                }
                Text(
                    text = hexValue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF181445),
                )
                Text(
                    text = "透明度: ${(alpha * 100).roundToInt()}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                )
            }
        }
    }
}

/**
 * 棋盘格背景（用于表示透明度）
 */
@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    val checkerColor1 = Color(0xFFFFFFFF)
    val checkerColor2 = Color(0xFFE0E0E0)
    val squareSize = 8

    Box(modifier = modifier.clip(RoundedCornerShape(8.dp))) {
        // 简化版棋盘格：使用渐变模拟
        Column {
            repeat(6) { row ->
                Row {
                    repeat(6) { col ->
                        Box(
                            modifier = Modifier
                                .size(squareSize.dp)
                                .background(
                                    if ((row + col) % 2 == 0) checkerColor1 else checkerColor2
                                ),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 饱和度-亮度面板
 * X 轴：饱和度 (0-1)
 * Y 轴：明度 (1-0，从上到下)
 */
@Composable
private fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var panelSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier.onGloballyPositioned { coordinates ->
        panelSize = coordinates.size
    }) {
        // 背景渐变：从白色到当前色相的纯色
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.White,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))),
                        ),
                    ),
                ),
        )
        // 从上到下的黑色渐变（控制明度）
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black,
                        ),
                    ),
                ),
        )

        // 选择器指示器
        val indicatorOffsetPx = with(density) { 12.dp.toPx() }.roundToInt()
        val indicatorX = (saturation * panelSize.width).roundToInt() - indicatorOffsetPx
        val indicatorY = ((1f - value) * panelSize.height).roundToInt() - indicatorOffsetPx

        Box(
            modifier = Modifier
                .offset { IntOffset(x = indicatorX, y = indicatorY) }
                .size(24.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
        )

        // 触摸/拖动手势
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val s = (change.position.x / panelSize.width).coerceIn(0f, 1f)
                        val v = 1f - (change.position.y / panelSize.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val s = (offset.x / panelSize.width).coerceIn(0f, 1f)
                        val v = 1f - (offset.y / panelSize.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(s, v)
                    }
                },
        )
    }
}

/**
 * 色相条
 * 水平拖动选择色相 (0-360)
 */
@Composable
private fun HueBar(
    hue: Float,
    onHueChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barWidth by remember { mutableStateOf(0) }

    val hueColors = remember {
        (0..360 step 30).map { h ->
            Color(android.graphics.Color.HSVToColor(floatArrayOf(h.toFloat(), 1f, 1f)))
        }
    }

    Box(modifier = modifier.onGloballyPositioned { coordinates ->
        barWidth = coordinates.size.width
    }) {
        // 色相渐变背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Brush.horizontalGradient(colors = hueColors)),
        )

        // 选择器指示器
        val fraction = hue / 360f
        val indicatorOffsetPx = with(density) { 8.dp.toPx() }.roundToInt()
        val indicatorX = (fraction * barWidth).roundToInt() - indicatorOffsetPx

        Box(
            modifier = Modifier
                .offset { IntOffset(x = indicatorX, y = 0) }
                .size(16.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .align(Alignment.CenterStart),
        )

        // 触摸/拖动手势
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val h = (change.position.x / barWidth * 360f).coerceIn(0f, 360f)
                        onHueChanged(h)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val h = (offset.x / barWidth * 360f).coerceIn(0f, 360f)
                        onHueChanged(h)
                    }
                },
        )
    }
}

/**
 * 透明度条
 * 水平拖动选择透明度 (0-1)
 */
@Composable
private fun AlphaBar(
    alpha: Float,
    color: Color,
    onAlphaChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var barWidth by remember { mutableStateOf(0) }

    Box(modifier = modifier.onGloballyPositioned { coordinates ->
        barWidth = coordinates.size.width
    }) {
        // 棋盘格背景（表示透明度）
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp)),
        ) {
            CheckerboardBackground(modifier = Modifier.matchParentSize())
        }

        // 透明度渐变背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            color.copy(alpha = 0f),
                            color.copy(alpha = 1f),
                        ),
                    ),
                ),
        )

        // 选择器指示器
        val fraction = alpha
        val indicatorOffsetPx = with(density) { 8.dp.toPx() }.roundToInt()
        val indicatorX = (fraction * barWidth).roundToInt() - indicatorOffsetPx

        Box(
            modifier = Modifier
                .offset { IntOffset(x = indicatorX, y = 0) }
                .size(16.dp)
                .shadow(4.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                .align(Alignment.CenterStart),
        )

        // 触摸/拖动手势
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val a = (change.position.x / barWidth).coerceIn(0f, 1f)
                        onAlphaChanged(a)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val a = (offset.x / barWidth).coerceIn(0f, 1f)
                        onAlphaChanged(a)
                    }
                },
        )
    }
}

/**
 * 主题颜色选择器 - 显示预设颜色圆点
 */
@Composable
fun ThemeColorPicker(modifier: Modifier = Modifier) {
    val presets = ThemeColorManager.PresetTheme.entries

    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "主题颜色",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF181445),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            presets.forEach { preset ->
                val isSelected = ThemeColorManager.currentPresetName == preset.displayName
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset.container)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, Color.White, CircleShape)
                                    .border(3.dp, preset.container.copy(alpha = 0.5f), CircleShape)
                            } else {
                                Modifier
                            }
                        )
                        .clickable { ThemeColorManager.applyPreset(preset) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}
