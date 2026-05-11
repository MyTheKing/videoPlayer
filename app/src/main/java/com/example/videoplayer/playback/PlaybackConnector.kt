package com.example.videoplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.videoplayer.player.service.MediaPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.coroutineContext

/**
 * 根据 [MainPlaybackViewModel] 的队列请求启动前台服务并异步连接 [MediaController]。
 */
@Composable
fun PlaybackConnector(viewModel: MainPlaybackViewModel) {
    val context = LocalContext.current
    val request by viewModel.playbackRequest.collectAsStateWithLifecycle()
    val generation by viewModel.sessionGeneration.collectAsStateWithLifecycle()
    val latestVm = rememberUpdatedState(viewModel)

    LaunchedEffect(request, generation) {
        val req = request ?: return@LaunchedEffect
        MediaPlaybackService.startQueue(
            context = context,
            uriStrings = req.uriStrings,
            titles = req.titles,
            startIndex = req.startIndex,
            durationMs = req.durationMsList,
            shuffleEnabled = req.shuffleEnabled,
            repeatMode = req.repeatMode,
        )

        val token = SessionToken(context, ComponentName(context, MediaPlaybackService::class.java))
        val mainExecutor = ContextCompat.getMainExecutor(context)

        var connected: MediaController? = null
        try {
            repeat(35) { attempt ->
                if (!coroutineContext.isActive) return@LaunchedEffect
                if (attempt > 0) delay(120)
                val future = MediaController.Builder(context, token).buildAsync()
                try {
                    connected = suspendCancellableCoroutine { cont ->
                        future.addListener(
                            {
                                try {
                                    val c = future.get()
                                    runCatching {
                                        c.setShuffleModeEnabled(req.shuffleEnabled)
                                        c.setRepeatMode(req.repeatMode)
                                    }
                                    if (cont.isActive) {
                                        cont.resume(c) { c.release() }
                                    } else {
                                        c.release()
                                    }
                                } catch (e: Exception) {
                                    if (cont.isActive) {
                                        cont.resumeWith(Result.failure(e))
                                    }
                                }
                            },
                            mainExecutor,
                        )
                    }
                    latestVm.value.setMediaController(connected)
                    connected = null
                    return@LaunchedEffect
                } catch (_: Exception) {
                    connected?.release()
                    connected = null
                }
            }
        } finally {
            connected?.release()
        }
    }
}
