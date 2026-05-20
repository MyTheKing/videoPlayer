package com.joshua.videoplayer.player.service

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors

/**
 * 部分 fMP4 在 Exo 上不可 seek；用系统 [MediaExtractor]+[MediaMuxer] **流复制** 重封装为渐进式 MP4
 *（不经过 FFmpeg；Maven 上 arthenica 包已不可用）。
 */
internal object SeekableMp4Remuxer {

    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SeekableMp4Remuxer").apply { isDaemon = true }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    fun ensureRemuxed(appContext: Context, input: File, callback: (Result<File>) -> Unit) {
        io.execute {
            try {
                val parent = input.parentFile ?: appContext.cacheDir
                val base = input.name.substringBeforeLast('.')
                val out = File(parent, "${base}_exo.mp4")
                if (out.exists() && out.length() > 0L && out.lastModified() >= input.lastModified()) {
                    mainHandler.post { callback(Result.success(out)) }
                    return@execute
                }
                if (out.exists()) out.delete()
                PlaybackSeekDiagnostics.log("remux start in=${input.absolutePath} out=${out.absolutePath}")
                remuxInterleaved(input, out)
                if (!out.exists() || out.length() == 0L) {
                    throw IllegalStateException("remux output empty")
                }
                PlaybackSeekDiagnostics.log("remux ok bytes=${out.length()}")
                mainHandler.post { callback(Result.success(out)) }
            } catch (e: Exception) {
                PlaybackSeekDiagnostics.log("remux error: ${e.message}")
                mainHandler.post { callback(Result.failure(e)) }
            }
        }
    }

    private fun maxInputSize(format: MediaFormat): Int {
        return if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
            format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(64 * 1024)
        } else {
            4 * 1024 * 1024
        }
    }

    /**
     * 每轨独立 [MediaExtractor]，按 presentationTime 交错写入 muxer（音视频同步）。
     */
    private fun remuxInterleaved(input: File, output: File) {
        val path = input.absolutePath
        val probe = MediaExtractor()
        probe.setDataSource(path)
        val trackCount = probe.trackCount
        probe.release()
        if (trackCount <= 0) throw IllegalStateException("no tracks")

        val extractors = Array(trackCount) { ti ->
            MediaExtractor().apply {
                setDataSource(path)
                selectTrack(ti)
            }
        }
        var muxer: MediaMuxer? = null
        try {
            muxer = MediaMuxer(output.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outIndex = IntArray(trackCount) { i ->
                muxer.addTrack(extractors[i].getTrackFormat(i))
            }
            muxer.start()
            val maxBuf = extractors.indices.maxOf { maxInputSize(extractors[it].getTrackFormat(it)) }
            val buffer = ByteBuffer.allocateDirect(maxBuf)
            val info = MediaCodec.BufferInfo()
            val active = BooleanArray(trackCount) { true }
            var guard = 0L
            val guardMax = 50_000_000L
            while (active.any { it }) {
                var bestT = Long.MAX_VALUE
                var best = -1
                for (i in 0 until trackCount) {
                    if (!active[i]) continue
                    val t = extractors[i].sampleTime
                    if (t < bestT) {
                        bestT = t
                        best = i
                    }
                }
                if (best < 0) break
                buffer.clear()
                val size = extractors[best].readSampleData(buffer, 0)
                if (size < 0) {
                    active[best] = false
                    continue
                }
                info.offset = 0
                info.size = size
                info.presentationTimeUs = extractors[best].sampleTime
                info.flags = extractors[best].sampleFlags
                muxer.writeSampleData(outIndex[best], buffer, info)
                if (!extractors[best].advance()) {
                    active[best] = false
                }
                guard++
                if (guard > guardMax) {
                    throw IllegalStateException("remux guard exceeded")
                }
            }
            muxer.stop()
        } finally {
            runCatching { muxer?.release() }
            extractors.forEach { runCatching { it.release() } }
        }
    }
}
