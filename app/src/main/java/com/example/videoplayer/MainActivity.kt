package com.example.videoplayer

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videoplayer.library.ui.PermissionIntent
import com.example.videoplayer.library.ui.PermissionRationaleContent
import com.example.videoplayer.library.ui.PermissionViewModel
import com.example.videoplayer.ui.home.AppHomeScreen
import com.example.videoplayer.ui.theme.ContentBackground
import com.example.videoplayer.ui.theme.VideoPlayerTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                val permPrefs = remember { getSharedPreferences(PREF_PERMISSION_FILE, MODE_PRIVATE) }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    permPrefs.edit().putBoolean(KEY_SYSTEM_PROMPT_ATTEMPTED, true).apply()
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
                            is com.example.videoplayer.library.ui.PermissionEvent.RequestPermission ->
                                launcher.launch(permission)
                            is com.example.videoplayer.library.ui.PermissionEvent.OpenSettings -> {
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
                        val systemPromptAttempted = permPrefs.getBoolean(KEY_SYSTEM_PROMPT_ATTEMPTED, false)
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

                        if (!granted && canAskAgain && !systemPromptAttempted) {
                            delay(400)
                            permissionVm.requestPermission()
                        }
                    }
                }

                val factory = remember { VideoPlayerViewModelFactory(application) }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(ContentBackground)
                        .statusBarsPadding(),
                ) {
                    if (state.granted) {
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

    companion object {
        private const val PREF_PERMISSION_FILE = "video_player_permission_gate"
        private const val KEY_SYSTEM_PROMPT_ATTEMPTED = "system_prompt_attempted"
    }
}
