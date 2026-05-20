package com.joshua.videoplayer.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.joshua.videoplayer.BuildConfig
import com.joshua.videoplayer.R
import com.joshua.videoplayer.data.DurationFilterManager
import com.joshua.videoplayer.data.DurationUnit
import com.joshua.videoplayer.data.FileStorageManager
import com.joshua.videoplayer.data.LanguageManager
import com.joshua.videoplayer.data.SleepTimerManager
import com.joshua.videoplayer.data.LegalAgreementCache
import com.joshua.videoplayer.ui.components.ThemeColorPicker
import com.joshua.videoplayer.ui.theme.BlueAccent
import com.joshua.videoplayer.ui.theme.ContentBackground
import com.joshua.videoplayer.ui.theme.ThemeColorManager

// Glass-card colors matching the HTML template
private val GlassCardBg = Color(0x66FFFFFF)          // rgba(255,255,255,0.4)
private val GlassCardBorder = Color(0x4DFFFFFF)      // rgba(255,255,255,0.3)
private val SectionLabelColor: Color
    @Composable get() = BlueAccent
private val TextOnSurface = Color(0xFF181445)        // on-surface
private val TextOnSurfaceVariant = Color(0xFF464555)  // on-surface-variant
private val OutlineColor = Color(0xFF777587)          // outline
private val OutlineVariant = Color(0xFFC7C4D8)        // outline-variant
private val PrimaryContainer: Color
    @Composable get() = ThemeColorManager.currentPrimaryContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onDurationFilterChanged: () -> Unit = {},
    onStoragePathChanged: () -> Unit = {},
    onPermissionGranted: () -> Unit = {},
    initialShowLegal: Boolean = false,
    onLegalShown: () -> Unit = {},
    onBeforeLanguageChange: () -> Unit = {},
) {
    val context = LocalContext.current
    // State for filter toggle
    var filterEnabled by remember { mutableStateOf(DurationFilterManager.filterEnabled) }
    // State for duration inputs
    val (initialMinValue, initialMinUnit) = DurationFilterManager.msToDisplayValue(DurationFilterManager.minDurationMs, DurationUnit.SECONDS)
    val (initialMaxValue, initialMaxUnit) = DurationFilterManager.msToDisplayValue(DurationFilterManager.maxDurationMs, DurationUnit.MINUTES)
    var minDuration by remember { mutableStateOf(initialMinValue) }
    var maxDuration by remember { mutableStateOf(initialMaxValue) }
    var minUnit by remember { mutableStateOf(initialMinUnit) }
    var maxUnit by remember { mutableStateOf(initialMaxUnit) }
    // State for legal screen
    var showLegal by remember { mutableStateOf(initialShowLegal) }
    var showAuthor by remember { mutableStateOf(false) }

    // 监听 initialShowLegal 变化：为 true 时打开并标记已阅读，为 false 时关闭
    LaunchedEffect(initialShowLegal) {
        showLegal = initialShowLegal
        if (initialShowLegal) {
            LegalAgreementCache.markAgreed()
        }
    }
    // State for storage location
    var storagePath by remember { mutableStateOf(FileStorageManager.getStorageDirPath()) }
    var showPathDialog by remember { mutableStateOf(false) }
    // 存储权限状态（每次页面恢复时重新检查）
    var needsPermission by remember { mutableStateOf(FileStorageManager.needsStoragePermission()) }
    var prevNeedsPermission by remember { mutableStateOf(needsPermission) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val nowNeeds = FileStorageManager.needsStoragePermission()
                needsPermission = nowNeeds
                storagePath = FileStorageManager.getStorageDirPath()
                // 权限从未授权 → 已授权：触发同步读取文件数据
                if (prevNeedsPermission && !nowNeeds) {
                    onPermissionGranted()
                }
                prevNeedsPermission = nowNeeds
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // State for language selection
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(LanguageManager.getCurrentLanguage()) }

    // State for sleep timer
    val timerEnabled = SleepTimerManager.timerEnabled
    val initMs = if (SleepTimerManager.timerDurationMs > 0) SleepTimerManager.timerDurationMs else 30 * 60 * 1000L
    var timerDurationMs by remember { mutableStateOf(initMs) }
    val (initialTimerValue, initialTimerUnit) = DurationFilterManager.msToDisplayValue(initMs)
    var timerDurationValue by remember { mutableStateOf(initialTimerValue) }
    var timerDurationUnit by remember { mutableStateOf(initialTimerUnit) }

    Box(modifier = modifier.fillMaxSize()) {
        // 设置页面内容始终保持在组合树中，保留滚动状态
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))

        // ── 通用 ──
        SectionLabel(stringResource(R.string.settings_section_general))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            ThemeColorPicker()
            DividerLine()
            SettingsRow(
                icon = Icons.Outlined.Language,
                label = stringResource(R.string.settings_language),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = LanguageManager.getCurrentLanguageDisplayName(context),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = OutlineColor,
                        )
                    }
                },
                onClick = { showLanguageDialog = true },
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 数据存储 ──
        SectionLabel(stringResource(R.string.settings_section_storage))
        Spacer(Modifier.height(16.dp))

        // 存储权限提示（Android 11+ 需要 MANAGE_EXTERNAL_STORAGE）
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val bgColor = if (needsPermission) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
            val textColor = if (needsPermission) Color(0xFFE65100) else Color(0xFF2E7D32)
            val icon = if (needsPermission) Icons.Outlined.Info else Icons.Outlined.Shield
            val message = if (needsPermission) {
                stringResource(R.string.settings_storage_permission_needed)
            } else {
                stringResource(R.string.settings_storage_permission_granted)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = needsPermission) {
                        FileStorageManager.requestManageStoragePermission(context)
                    },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        GlassCard {
            SettingsRow(
                icon = Icons.Outlined.FolderOpen,
                label = stringResource(R.string.settings_storage_location),
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = storagePath,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        // 编辑图标
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.cd_edit_path),
                            tint = OutlineColor,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { showPathDialog = true },
                        )
                    }
                },
                onClick = {
                    // 点击行 → 跳转到文件夹
                    openFolder(context, storagePath)
                },
            )
        }

        // 路径编辑对话框
        if (showPathDialog) {
            var editPath by remember { mutableStateOf(storagePath) }
            var pathError by remember { mutableStateOf<String?>(null) }
            AlertDialog(
                onDismissRequest = { showPathDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.settings_storage_location_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.settings_storage_path_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = editPath,
                            onValueChange = { editPath = it; pathError = null },
                            textStyle = TextStyle(
                                fontSize = 14.sp,
                                color = TextOnSurface,
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(PrimaryContainer),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(12.dp),
                        )
                        if (pathError != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = pathError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_storage_path_default),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = editPath.trim()
                            if (trimmed.isBlank()) {
                                pathError = context.getString(R.string.settings_storage_path_empty)
                                return@TextButton
                            }
                            val success = if (trimmed == FileStorageManager.getStorageDirPath()) {
                                true
                            } else {
                                FileStorageManager.setStorageDir(trimmed)
                            }
                            if (success) {
                                storagePath = FileStorageManager.getStorageDirPath()
                                showPathDialog = false
                                onStoragePathChanged()
                            } else {
                                pathError = context.getString(R.string.settings_storage_path_create_failed)
                            }
                        },
                    ) {
                        Text(stringResource(R.string.action_confirm), color = PrimaryContainer)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPathDialog = false },
                    ) {
                        Text(stringResource(R.string.action_cancel), color = TextOnSurfaceVariant)
                    }
                },
            )
        }

        // 语言选择对话框
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                text = {
                    Column {
                        LanguageManager.Language.entries.forEach { language ->
                            val displayName = when (language) {
                                LanguageManager.Language.CHINESE -> stringResource(R.string.language_chinese)
                                LanguageManager.Language.ENGLISH -> stringResource(R.string.language_english)
                            }
                            val isSelected = language == currentLanguage
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentLanguage = language
                                        onBeforeLanguageChange()
                                        (context as? Activity)?.let { LanguageManager.setLanguage(it, language) }
                                        showLanguageDialog = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        currentLanguage = language
                                        onBeforeLanguageChange()
                                        (context as? Activity)?.let { LanguageManager.setLanguage(it, language) }
                                        showLanguageDialog = false
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextOnSurface,
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── 文件扫描 ──
        SectionLabel(stringResource(R.string.settings_section_scanning))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.FilterList,
                            contentDescription = null,
                            tint = TextOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.settings_filter_short),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurface,
                        )
                    }
                    Switch(
                        checked = filterEnabled,
                        onCheckedChange = {
                            filterEnabled = it
                            DurationFilterManager.updateFilterEnabled(it)
                            onDurationFilterChanged()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryContainer,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = OutlineVariant,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }

                // Duration range section (visible when filter is enabled)
                if (filterEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_duration_range),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.1.sp,
                        ),
                        color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(start = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DurationInput(
                            value = minDuration,
                            onValueChange = {
                                minDuration = it
                                DurationFilterManager.setMinDuration(it, minUnit)
                                onDurationFilterChanged()
                            },
                            placeholder = stringResource(R.string.settings_duration_min),
                            selectedUnit = minUnit,
                            onUnitChange = { newUnit ->
                                minUnit = newUnit
                                DurationFilterManager.setMinDuration(minDuration, newUnit)
                                onDurationFilterChanged()
                            },
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = stringResource(R.string.settings_duration_to),
                            color = TextOnSurfaceVariant.copy(alpha = 0.5f),
                        )
                        DurationInput(
                            value = maxDuration,
                            onValueChange = {
                                maxDuration = it
                                DurationFilterManager.setMaxDuration(it, maxUnit)
                                onDurationFilterChanged()
                            },
                            placeholder = stringResource(R.string.settings_duration_max),
                            selectedUnit = maxUnit,
                            onUnitChange = { newUnit ->
                                maxUnit = newUnit
                                DurationFilterManager.setMaxDuration(maxDuration, newUnit)
                                onDurationFilterChanged()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 定时播放 ──
        SectionLabel(stringResource(R.string.settings_section_timer))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = TextOnSurfaceVariant,
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = stringResource(R.string.settings_timer_enabled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextOnSurface,
                        )
                    }
                    Switch(
                        checked = timerEnabled,
                        onCheckedChange = {
                            if (it) {
                                if (timerDurationMs <= 0L) {
                                    timerDurationMs = 30 * 60 * 1000L
                                    timerDurationValue = "30"
                                    timerDurationUnit = DurationUnit.MINUTES
                                    SleepTimerManager.setDuration(timerDurationMs)
                                }
                                SleepTimerManager.setEnabled(true)
                                SleepTimerManager.start()
                            } else {
                                SleepTimerManager.setEnabled(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = PrimaryContainer,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = OutlineVariant,
                            uncheckedBorderColor = Color.Transparent,
                        ),
                    )
                }

                if (timerEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_timer_duration),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.1.sp,
                        ),
                        color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 40.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.padding(start = 40.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DurationInput(
                            value = timerDurationValue,
                            onValueChange = {
                                timerDurationValue = it
                                val ms = parseDurationToMs(it, timerDurationUnit)
                                timerDurationMs = ms
                                SleepTimerManager.setDuration(ms)
                                if (SleepTimerManager.isRunning && ms > 0) {
                                    SleepTimerManager.stop()
                                    SleepTimerManager.start()
                                }
                            },
                            placeholder = "0",
                            selectedUnit = timerDurationUnit,
                            onUnitChange = { newUnit ->
                                timerDurationUnit = newUnit
                                val ms = parseDurationToMs(timerDurationValue, newUnit)
                                timerDurationMs = ms
                                SleepTimerManager.setDuration(ms)
                                if (SleepTimerManager.isRunning && ms > 0) {
                                    SleepTimerManager.stop()
                                    SleepTimerManager.start()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    // 倒计时状态显示
                    if (SleepTimerManager.isRunning) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.settings_timer_remaining,
                                SleepTimerManager.formatRemaining(SleepTimerManager.remainingMs),
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = PrimaryContainer,
                            modifier = Modifier.padding(start = 40.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryContainer.copy(alpha = 0.1f),
                            modifier = Modifier
                                .padding(start = 40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    SleepTimerManager.stop()
                                    SleepTimerManager.setEnabled(false)
                                },
                        ) {
                            Text(
                                text = stringResource(R.string.settings_timer_cancel),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                ),
                                color = PrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 隐私与安全 ──
        SectionLabel(stringResource(R.string.settings_section_privacy))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            // 顶部装饰区域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PrimaryContainer.copy(alpha = 0.15f),
                                PrimaryContainer.copy(alpha = 0.05f),
                            )
                        )
                    )
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 图标圆形背景
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = PrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_privacy_local_processing),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = TextOnSurface,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_privacy_local_processing_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant,
                        )
                    }
                }
            }

            // 隐私承诺列表
            Column(modifier = Modifier.padding(20.dp)) {
                PrivacyFeatureItem(
                    icon = Icons.Outlined.Lock,
                    text = stringResource(R.string.settings_privacy_text_1),
                )
                Spacer(Modifier.height(16.dp))
                PrivacyFeatureItem(
                    icon = Icons.Outlined.CloudOff,
                    text = stringResource(R.string.settings_privacy_text_2),
                )
                Spacer(Modifier.height(16.dp))
                PrivacyFeatureItem(
                    icon = Icons.Outlined.WifiOff,
                    text = stringResource(R.string.settings_privacy_text_3),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 提供建议 ──
        SectionLabel(stringResource(R.string.settings_section_feedback))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                // 反馈图标和标题
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PrimaryContainer.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = PrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.settings_feedback_help_improve),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = TextOnSurface,
                        )
                        Text(
                            text = stringResource(R.string.settings_feedback_opinion_matters),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextOnSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 反馈描述
                Text(
                    text = stringResource(R.string.settings_feedback_hint),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = TextOnSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                // 说明文字
                Text(
                    text = stringResource(R.string.settings_feedback_note),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = PrimaryContainer,
                )

                Spacer(Modifier.height(20.dp))

                // 联系按钮 - 渐变效果
                val contactEmail = stringResource(R.string.settings_contact_email)
                val contactSubject = stringResource(R.string.settings_contact_subject)
                val contactBody = stringResource(R.string.settings_contact_body)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val uri = Uri.parse(
                                "mailto:$contactEmail?subject=${Uri.encode(contactSubject)}&body=${Uri.encode(contactBody)}"
                            )
                            val intent = Intent(Intent.ACTION_SENDTO, uri)
                            try {
                                context.startActivity(Intent.createChooser(intent, null))
                            } catch (_: Exception) {
                                // 没有邮件客户端
                            }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        PrimaryContainer,
                                        PrimaryContainer.copy(alpha = 0.8f),
                                    )
                                )
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                Icons.Outlined.MailOutline,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.settings_contact_author),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = Color.White,
                            )
                        }
                    }
                }

                // 邮箱地址
                Text(
                    text = stringResource(R.string.settings_contact_email),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextOnSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── 关于 ──
        SectionLabel(stringResource(R.string.settings_section_about))
        Spacer(Modifier.height(16.dp))
        GlassCard {
            SettingsRow(
                icon = Icons.Outlined.Info,
                label = stringResource(R.string.settings_version_info),
                trailing = {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} (Stable)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextOnSurfaceVariant,
                    )
                },
            )
            DividerLine()
            SettingsRow(
                icon = Icons.Outlined.Person,
                label = stringResource(R.string.settings_about_author),
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OutlineColor,
                    )
                },
                onClick = { showAuthor = true },
            )
            DividerLine()
            SettingsRow(
                icon = Icons.Outlined.Description,
                label = stringResource(R.string.settings_legal),
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = OutlineColor,
                    )
                },
                onClick = {
                    LegalAgreementCache.markAgreed()
                    showLegal = true
                },
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Footer ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = PrimaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.settings_footer_brand).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp,
                ),
                color = PrimaryContainer.copy(alpha = 0.4f),
            )
        }

        Spacer(Modifier.height(24.dp)) // Space for bottom nav
        }
    }

        // 子页面叠加显示，设置页面保留在组合树中以保留滚动状态
        if (showLegal) {
            BackHandler {
                showLegal = false
                onLegalShown()
            }
            LegalScreen(onBack = {
                showLegal = false
                onLegalShown()
            })
        } else if (showAuthor) {
            BackHandler { showAuthor = false }
            AuthorScreen(onBack = { showAuthor = false })
        }
    }
}

// ── Helper composables ──

@Composable
private fun SettingsTopBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ContentBackground.copy(alpha = 0.6f))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = BlueAccent,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "videoPlayer",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.05.sp,
                    ),
                    color = BlueAccent,
                )
            }
            Text(
                text = stringResource(R.string.screen_settings_title),
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
private fun SectionLabel(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
                fontSize = 14.sp,
            ),
            color = SectionLabelColor,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GlassCardBg,
        modifier = modifier
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
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextOnSurfaceVariant,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextOnSurface,
            )
        }
        trailing()
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = 0.1f)),
    )
}

/**
 * 隐私功能项组件
 */
@Composable
private fun PrivacyFeatureItem(
    icon: ImageVector,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(PrimaryContainer.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PrimaryContainer,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = TextOnSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DurationInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    selectedUnit: DurationUnit,
    onUnitChange: (DurationUnit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val unitEntries = listOf(
        DurationUnit.SECONDS to stringResource(R.string.settings_unit_seconds),
        DurationUnit.MINUTES to stringResource(R.string.settings_unit_minutes),
        DurationUnit.HOURS to stringResource(R.string.settings_unit_hours),
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
        modifier = modifier
            .border(
                width = 1.dp,
                color = OutlineVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { newValue ->
                    // 只允许数字输入
                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                        onValueChange(newValue)
                    }
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = TextOnSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(PrimaryContainer),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = TextOnSurfaceVariant.copy(alpha = 0.4f),
                                ),
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )

            // 单位选择器 - 用 Box 包裹使 DropdownMenu 锚定在按钮位置
            var expanded by remember { mutableStateOf(false) }
            Box {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryContainer.copy(alpha = 0.1f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expanded = true },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = unitEntries.first { it.first == selectedUnit }.second,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                            ),
                            color = PrimaryContainer,
                        )
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = PrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer(rotationZ = 90f),
                        )
                    }
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                ) {
                    unitEntries.forEach { (unit, displayText) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = displayText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (unit == selectedUnit) PrimaryContainer else TextOnSurface,
                                )
                            },
                            onClick = {
                                onUnitChange(unit)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun parseDurationToMs(value: String, unit: DurationUnit): Long {
    val numValue = value.toLongOrNull() ?: return 0L
    return numValue * unit.multiplier
}

/** 打开文件夹：依次尝试多种 Intent 方式 */
private fun openFolder(context: android.content.Context, path: String) {
    val dir = java.io.File(path)
    if (!dir.exists()) dir.mkdirs()

    // 方式 1：vnd.android.document/directory
    try {
        val uri = android.net.Uri.parse("file://$path")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "vnd.android.document/directory")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {}

    // 方式 2：resource/folder
    try {
        val uri = android.net.Uri.parse("file://$path")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "resource/folder")
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {}

    // 方式 3：通过 Documents Provider content URI
    try {
        val uri = android.provider.DocumentsContract.buildDocumentUri(
            "com.android.externalstorage.documents",
            "primary:${path.removePrefix("/storage/emulated/0/")}",
        )
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            data = uri
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return
    } catch (_: Exception) {}

    // 方式 4：用 SAF 选择器作为 fallback
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
