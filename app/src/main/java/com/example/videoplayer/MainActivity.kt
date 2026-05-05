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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.videoplayer.ui.theme.ContentBackground
import com.example.videoplayer.ui.theme.HeaderGradientEnd
import com.example.videoplayer.ui.theme.HeaderGradientStart
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videoplayer.library.ui.PermissionIntent
import com.example.videoplayer.library.ui.PermissionRationaleContent
import com.example.videoplayer.library.ui.PermissionViewModel
import com.example.videoplayer.player.ui.PlayerContent
import com.example.videoplayer.ui.theme.VideoPlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoPlayerTheme {
                val viewModel: PermissionViewModel = viewModel()
                val state by viewModel.state.collectAsState()

                val permission = remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        android.Manifest.permission.READ_MEDIA_VIDEO
                    } else {
                        android.Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    val canAsk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        shouldShowRequestPermissionRationale(permission)
                    } else true
                    viewModel.dispatch(PermissionIntent.OnRequestResult(granted, canAsk))
                }

                LaunchedEffect(viewModel) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is com.example.videoplayer.library.ui.PermissionEvent.RequestPermission ->
                                launcher.launch(permission)
                            is com.example.videoplayer.library.ui.PermissionEvent.OpenSettings -> {
                                val uri = Uri.fromParts("package", packageName, null)
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = uri })
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(this@MainActivity, permission) == PackageManager.PERMISSION_GRANTED
                    val canAskAgain = granted || (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || shouldShowRequestPermissionRationale(permission))
                    viewModel.dispatch(PermissionIntent.SyncFromCheck(granted, canAskAgain))
                }

                Column(modifier = Modifier.fillMaxSize().background(ContentBackground)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Brush.horizontalGradient(listOf(HeaderGradientStart, HeaderGradientEnd))),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(com.example.videoplayer.R.string.app_name),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent
                        ) { innerPadding ->
                        if (state.granted) {
                            PickAndPlayContent(modifier = Modifier.padding(innerPadding))
                        } else {
                            PermissionRationaleContent(
                                state = state,
                                onRequestPermission = { viewModel.requestPermission() },
                                onOpenSettings = { viewModel.dispatch(PermissionIntent.OpenSettings) },
                                onDismiss = { viewModel.dispatch(PermissionIntent.DismissRationale) },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PickAndPlayContent(modifier: Modifier = Modifier) {
    var playUri by remember { mutableStateOf<Uri?>(null) }
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { playUri = it }
    }
    if (playUri == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        stringResource(com.example.videoplayer.R.string.pick_video),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { openDocLauncher.launch(arrayOf("video/*")) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(com.example.videoplayer.R.string.pick_video))
                    }
                }
            }
        }
    } else {
        PlayerContent(
            playUri = playUri,
            onPlaybackError = { playUri = null },
            modifier = modifier.fillMaxSize()
        )
    }
}
