package com.joshua.videoplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.R
import com.joshua.videoplayer.ui.theme.ThemeColorManager

private val TextOnSurface = Color(0xFF181445)
private val TextOnSurfaceVariant = Color(0xFF464555)
private val OutlineColor = Color(0xFF777587)
private val OutlineVariant = Color(0xFFC7C4D8)

/**
 * 主题颜色选择器组件
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeColorPicker() {
    var expanded by remember { mutableStateOf(false) }
    val currentPresetName = ThemeColorManager.currentPresetName

    // 用于同步颜色板的颜色状态
    var pickerColor by remember { mutableStateOf(ThemeColorManager.currentPrimaryContainer) }

    // 箭头旋转动画
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "arrowRotation",
    )

    Column {
        // 主题选项行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = TextOnSurfaceVariant,
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurface,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = ThemeColorManager.currentPresetLocalizedName(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextOnSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = if (expanded) stringResource(R.string.cd_collapse) else stringResource(R.string.cd_expand),
                    tint = OutlineColor,
                    modifier = Modifier.rotate(arrowRotation),
                )
            }
        }

        // 颜色选择面板
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                // 预设颜色标题
                Text(
                    text = stringResource(R.string.theme_preset_colors),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                    ),
                    color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))

                // 预设颜色网格
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ThemeColorManager.PresetTheme.entries.forEach { preset ->
                        PresetColorItem(
                            preset = preset,
                            isSelected = currentPresetName == preset.displayName,
                            onClick = {
                                ThemeColorManager.applyPreset(preset)
                                pickerColor = preset.container // 同步颜色板
                            },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 自定义颜色选择器
                Text(
                    text = stringResource(R.string.theme_custom_color),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                    ),
                    color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(12.dp))

                // 颜色板选择器
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = OutlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp),
                        ),
                ) {
                    ColorPicker(
                        initialColor = pickerColor,
                        onColorChanged = { color ->
                            ThemeColorManager.applyCustomColor(color)
                            pickerColor = color // 同步颜色板状态
                        },
                        modifier = Modifier.padding(16.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // 手动输入颜色值
                Text(
                    text = stringResource(R.string.theme_input_color_value),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.1.sp,
                    ),
                    color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.theme_color_format_hint),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextOnSurfaceVariant.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(8.dp))
                HexColorInput(
                    onColorApplied = { color ->
                        ThemeColorManager.applyCustomColor(color)
                        pickerColor = color // 同步颜色板状态
                    },
                )
            }
        }
    }
}

/**
 * 预设颜色项
 */
@Composable
private fun PresetColorItem(
    preset: ThemeColorManager.PresetTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (isSelected) ThemeColorManager.currentPrimaryContainer.copy(alpha = 0.1f)
                else Color.Transparent,
            )
            .border(
                width = 2.dp,
                color = if (isSelected) ThemeColorManager.currentPrimaryContainer else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(preset.container),
        ) {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = preset.localizedName(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = if (isSelected) ThemeColorManager.currentPrimaryContainer else TextOnSurface,
        )
    }
}

/**
 * 十六进制颜色输入组件
 */
@Composable
private fun HexColorInput(
    onColorApplied: (Color) -> Unit,
) {
    val context = LocalContext.current
    var hexInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewColor by remember { mutableStateOf<Color?>(null) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 输入框
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = if (errorMessage != null) Color(0xFFEF4444) else OutlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "#",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = TextOnSurfaceVariant.copy(alpha = 0.5f),
                    ),
                )
                BasicTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        // 只允许十六进制字符
                        val filtered = input.filter { it.isLetterOrDigit() }.take(8)
                        hexInput = filtered
                        errorMessage = null

                        // 尝试解析颜色
                        if (filtered.isNotEmpty()) {
                            val color = parseHexColor(filtered)
                            if (color != null) {
                                previewColor = color
                                errorMessage = null
                            } else {
                                previewColor = null
                                if (filtered.length >= 3) {
                                    errorMessage = context.getString(com.joshua.videoplayer.R.string.theme_invalid_color)
                                }
                            }
                        } else {
                            previewColor = null
                        }
                    },
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = TextOnSurface,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(ThemeColorManager.currentPrimaryContainer),
                    decorationBox = { innerTextField ->
                        Box {
                            if (hexInput.isEmpty()) {
                                Text(
                                    text = "9616468E",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = TextOnSurfaceVariant.copy(alpha = 0.3f),
                                    ),
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // 预览色块
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewColor ?: OutlineVariant.copy(alpha = 0.3f))
                .then(
                    if (previewColor != null) {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    } else {
                        Modifier
                    }
                ),
        )

        // 应用按钮
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (previewColor != null) ThemeColorManager.currentPrimaryContainer else OutlineVariant.copy(alpha = 0.3f),
            modifier = Modifier.clickable(enabled = previewColor != null) {
                previewColor?.let { color ->
                    onColorApplied(color)
                }
            },
        ) {
            Text(
                text = stringResource(R.string.action_apply),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = if (previewColor != null) Color.White else TextOnSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    // 错误提示
    if (errorMessage != null) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = Color(0xFFEF4444),
        )
    }
}

/**
 * 解析十六进制颜色值
 * 支持格式: #RGB, #RRGGBB, #AARRGGBB, #RRGGBBAA
 */
private fun parseHexColor(hex: String): Color? {
    return try {
        val cleanHex = hex.removePrefix("#")

        val argb = when (cleanHex.length) {
            3 -> {
                // #RGB -> #FFRRGGBB
                val r = cleanHex[0].toString().repeat(2)
                val g = cleanHex[1].toString().repeat(2)
                val b = cleanHex[2].toString().repeat(2)
                android.graphics.Color.parseColor("#FF$r$g$b")
            }
            6 -> {
                // #RRGGBB -> #FFRRGGBB
                android.graphics.Color.parseColor("#FF$cleanHex")
            }
            8 -> {
                // 判断是 #AARRGGBB 还是 #RRGGBBAA
                // 如果第一个字节看起来像 alpha（透明度），则认为是 #AARRGGBB
                val firstByte = cleanHex.substring(0, 2).toLong(16)
                if (firstByte <= 255) {
                    // #AARRGGBB
                    android.graphics.Color.parseColor("#$cleanHex")
                } else {
                    // #RRGGBBAA - 转换为 #AARRGGBB
                    val rgb = cleanHex.substring(0, 6)
                    val alpha = cleanHex.substring(6, 8)
                    android.graphics.Color.parseColor("#$alpha$rgb")
                }
            }
            else -> return null
        }

        Color(argb)
    } catch (e: Exception) {
        null
    }
}
