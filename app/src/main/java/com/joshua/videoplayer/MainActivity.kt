package com.joshua.videoplayer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joshua.videoplayer.library.ui.PermissionIntent
import com.joshua.videoplayer.library.ui.PermissionRationaleContent
import com.joshua.videoplayer.library.ui.PermissionViewModel
import com.joshua.videoplayer.ui.home.AppHomeScreen
import com.joshua.videoplayer.ui.theme.ContentBackground
import com.joshua.videoplayer.ui.theme.ThemeColorManager
import com.joshua.videoplayer.ui.theme.VideoPlayerTheme
import com.joshua.videoplayer.data.DurationFilterManager
import com.joshua.videoplayer.data.FileStorageManager
import com.joshua.videoplayer.data.LanguageManager
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = LanguageManager.getCurrentLanguage()
        val locale = Locale.forLanguageTag(language.localeTag)
        val config = Configuration(newBase.resources.configuration)
        config.setLocales(LocaleList(locale))
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onPause() {
        super.onPause()
        // 应用进入后台时，立即写入所有防抖队列中的数据
        FileStorageManager.flushAll()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化主题颜色管理器
        ThemeColorManager.init(this)
        // 初始化时长过滤管理器
        DurationFilterManager.init(this)
        enableEdgeToEdge()
        setContent {
            VideoPlayerTheme {
                val permissionVm: PermissionViewModel = viewModel()
                val state by permissionVm.state.collectAsState()

                val permission = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                }

                val permPrefs = remember { getSharedPreferences(VideoPermissionGate.PREF_FILE, MODE_PRIVATE) }
                var hasReceivedSync by remember { mutableStateOf(false) }
                val instantGranted = remember {
                    ContextCompat.checkSelfPermission(this@MainActivity, permission) ==
                        PackageManager.PERMISSION_GRANTED
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    permPrefs.edit().putBoolean(VideoPermissionGate.KEY_SYSTEM_PROMPT_ATTEMPTED, true).apply()
                    val canAsk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        granted || shouldShowRequestPermissionRationale(permission)
                    } else {
                        true
                    }
                    permissionVm.dispatch(PermissionIntent.OnRequestResult(granted, canAsk))
                }

                LaunchedEffect(permissionVm) {
                    permissionVm.events.collect { event ->
                        when (event) {
                            is com.joshua.videoplayer.library.ui.PermissionEvent.RequestPermission ->
                                launcher.launch(permission)
                            is com.joshua.videoplayer.library.ui.PermissionEvent.OpenSettings -> {
                                val uri = Uri.fromParts("package", packageName, null)
                                startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = uri },
                                )
                            }
                        }
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(lifecycleOwner, permission) {
                    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            permission,
                        ) == PackageManager.PERMISSION_GRANTED
                        val systemPromptAttempted = permPrefs.getBoolean(VideoPermissionGate.KEY_SYSTEM_PROMPT_ATTEMPTED, false)
                        val rationale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            shouldShowRequestPermissionRationale(permission)
                        } else {
                            false
                        }
                        // 首次安装 rationale 恒为 false，必须用「是否已弹过系统框」区分「可请求」与「仅去设置」
                        val canAskAgain = granted ||
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                            rationale ||
                            !systemPromptAttempted
                        permissionVm.dispatch(PermissionIntent.SyncFromCheck(granted, canAskAgain))
                        hasReceivedSync = true

                        (application as VideoPlayerApp).ensureLibraryWarmPrefetch(permission)

                        if (!granted && canAskAgain && !systemPromptAttempted) {
                            delay(400)
                            permissionVm.requestPermission()
                        }
                    }
                }

                val factory = remember { VideoPlayerViewModelFactory(application) }

                val gateGranted = if (hasReceivedSync) state.granted else instantGranted

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ContentBackground)
                        .statusBarsPadding(),
                ) {
                    if (gateGranted) {
                        AppHomeScreen(viewModelFactory = factory)
                    } else {
                        PermissionRationaleContent(
                            state = state,
                            onRequestPermission = { permissionVm.requestPermission() },
                            onOpenSettings = { permissionVm.dispatch(PermissionIntent.OpenSettings) },
                            onDismiss = { permissionVm.dispatch(PermissionIntent.DismissRationale) },
                        )
                    }
                }
            }
        }
    }
}
