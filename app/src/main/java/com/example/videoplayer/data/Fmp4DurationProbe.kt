package com.example.videoplayer.data

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlin.math.min

/**
 * 分段 MP4（fMP4）：[mvhd]/[mdhd] 常为 0，时长在多个 [moof]/[trun] 中。
 * 系统 [android.media.MediaMetadataRetriever] 等对这类文件常返回 0，此处按 ISO 14496-12 累加视频轨时长。
 */
@Suppress("TooGenericExceptionCaught")
internal object Fmp4DurationProbe {

    fun durationMsFromUri(context: Context, uri: Uri): Long {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { raw ->
                durationMsFromStream(BufferedInputStream(raw, 1 shl 16))
            } ?: 0L
        }.getOrDefault(0L)
    }

    fun durationMsFromFilePath(path: String): Long {
        return runCatching {
            FileInputStream(File(path)).use { durationMsFromStream(BufferedInputStream(it, 1 shl 16)) }
        }.getOrDefault(0L)
    }

    fun durationMsFromStream(input: InputStream): Long {
        return runCatching {
            val din = DataInputStream(input)
            var videoTrackId = -1
            var videoTimescale = 0L
            var moovOk = false
            var accumulatedUnits = 0L

            while (true) {
                val hdr = readBoxHeader(din) ?: break
                val totalSize = hdr.first
                val type = hdr.second
                val headerLen = hdr.third
                val payloadLen = totalSize - headerLen
                if (payloadLen < 0L || payloadLen > 120_000_000L) break

                when (type) {
                    "moov" -> {
                        val payload = readFully(din, payloadLen.toInt())
                        MoovVideoInfo.parse(payload)?.let {
                            videoTrackId = it.trackId
                            videoTimescale = it.timescale
                            moovOk = true
                        }
                    }
                    "moof" -> {
                        if (moovOk && videoTrackId >= 0 && videoTimescale > 0L) {
                            val payload = readFully(din, payloadLen.toInt())
                            accumulatedUnits += sumVideoUnitsInMoof(payload, videoTrackId)
                        } else {
                            skipFully(din, payloadLen)
                        }
                    }
                    else -> skipFully(din, payloadLen)
                }
            }

            if (!moovOk || videoTimescale <= 0L || accumulatedUnits <= 0L) 0L
            else (accumulatedUnits * 1000L) / videoTimescale
        }.getOrDefault(0L)
    }
}

private data class MoovVideoInfo(val trackId: Int, val timescale: Long) {
    companion object {
        fun parse(moovInner: ByteArray): MoovVideoInfo? {
            var found: MoovVideoInfo? = null
            forEachChildBox(moovInner) { typ, p0, p1 ->
                if (typ != "trak") return@forEachChildBox
                val trak = moovInner.copyOfRange(p0, p1)
                var trackId = -1
                var timescale = 0L
                var hasVideo = false
                forEachChildBox(trak) { t2, a, b ->
                    when (t2) {
                        "tkhd" -> {
                            if (a + 20 <= b) {
                                val ver = trak[a].toInt() and 0xff
                                trackId = if (ver == 1) {
                                    readU32(trak, a + 20)
                                } else {
                                    readU32(trak, a + 12)
                                }
                            }
                        }
                        "mdia" -> {
                            val mdia = trak.copyOfRange(a, b)
                            forEachChildBox(mdia) { t3, c, d ->
                                when (t3) {
                                    "mdhd" -> {
                                        if (c + 20 <= d) {
                                            val ver = mdia[c].toInt() and 0xff
                                            timescale = if (ver == 1) {
                                                readU32(mdia, c + 20).toLong() and 0xffffffffL
                                            } else {
                                                readU32(mdia, c + 12).toLong() and 0xffffffffL
                                            }
                                        }
                                    }
                                    "minf" -> {
                                        val minf = mdia.copyOfRange(c, d)
                                        forEachChildBox(minf) { t4, e, f ->
                                            if (t4 == "stbl") {
                                                val stbl = minf.copyOfRange(e, f)
                                                forEachChildBox(stbl) { t5, g, h ->
                                                    if (t5 == "stsd" && g + 16 <= h) {
                                                        val stsd = stbl.copyOfRange(g, h)
                                                        val n = readU32(stsd, 4)
                                                        if (n > 0 && 8 + 12 <= stsd.size) {
                                                            val fmt = read4cc(stsd, 12)
                                                            if (fmt in VIDEO_TYPES) hasVideo = true
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (hasVideo && trackId >= 0 && timescale > 0L && found == null) {
                    found = MoovVideoInfo(trackId, timescale)
                }
            }
            return found
        }
    }
}

private val VIDEO_TYPES = setOf("avc1", "avc3", "hvc1", "hev1", "dvhe", "dvh1", "av01", "vp09")

private fun sumVideoUnitsInMoof(moofInner: ByteArray, videoTrackId: Int): Long {
    var sum = 0L
    forEachChildBox(moofInner) { typ, a, b ->
        if (typ != "traf") return@forEachChildBox
        val traf = moofInner.copyOfRange(a, b)
        var tfhd: ByteArray? = null
        var trun: ByteArray? = null
        forEachChildBox(traf) { t2, c, d ->
            when (t2) {
                "tfhd" -> tfhd = traf.copyOfRange(c, d)
                "trun" -> trun = traf.copyOfRange(c, d)
            }
        }
        val tf = tfhd ?: return@forEachChildBox
        val tr = trun ?: return@forEachChildBox
        if (parseTfhdTrackId(tf) != videoTrackId) return@forEachChildBox
        val def = parseTfhdDefaultSampleDuration(tf)
        sum += parseTrunDurationUnits(tr, def)
    }
    return sum
}

/**
 * track_ID 紧跟 FullBox 的 version(1)+flags(3)，见 ISO 14496-12 TrackFragmentHeaderBox。
 * 必须从本数组偏移 **4** 读 u32；偏移 8 起为 flags 决定的**可选字段**（常与 [parseTfhdDefaultSampleDuration] 一致），误读会导致轨道对不上、累加时长为 0。
 * 文档：`docs/playback-seek-duration-compatibility.md` §5.1。
 */
private fun parseTfhdTrackId(tfhdInner: ByteArray): Int {
    if (tfhdInner.size < 8) return -1
    return readU32(tfhdInner, 4)
}

private fun parseTfhdDefaultSampleDuration(tfhdInner: ByteArray): Long {
    if (tfhdInner.size < 8) return 0L
    val flags = readU24(tfhdInner, 1)
    var o = 8
    if (flags and 0x01 != 0) o += 8
    if (flags and 0x02 != 0) o += 4
    return if (flags and 0x08 != 0 && o + 4 <= tfhdInner.size) {
        readU32(tfhdInner, o).toLong() and 0xffffffffL
    } else {
        0L
    }
}

private fun parseTrunDurationUnits(trunInner: ByteArray, defaultDur: Long): Long {
    if (trunInner.size < 8) return 0L
    val flags = readU24(trunInner, 1)
    val sampleCount = readU32(trunInner, 4).toLong() and 0xffffffffL
    var o = 8
    if (flags and 0x01 != 0) o += 4
    if (flags and 0x04 != 0) o += 4
    val hasDur = flags and 0x100 != 0
    val hasSize = flags and 0x200 != 0
    val hasFlags = flags and 0x400 != 0
    val hasCto = flags and 0x800 != 0
    if (!hasDur) {
        return if (defaultDur > 0L) defaultDur * sampleCount else 0L
    }
    var ex = 0L
    var i = 0L
    while (i < sampleCount && o + 4 <= trunInner.size) {
        ex += readU32(trunInner, o).toLong() and 0xffffffffL
        o += 4
        if (hasSize) o += 4
        if (hasFlags) o += 4
        if (hasCto) o += 4
        i++
    }
    return ex
}

private fun forEachChildBox(parentPayload: ByteArray, fn: (type: String, start: Int, end: Int) -> Unit) {
    var p = 0
    val end = parentPayload.size
    while (p + 8 <= end) {
        var size = readU32(parentPayload, p).toLong() and 0xffffffffL
        val typ = read4cc(parentPayload, p + 4)
        var h = 8
        if (size == 1L) {
            if (p + 16 > end) break
            size = readU64(parentPayload, p + 8)
            h = 16
        }
        if (size < h) break
        val boxEnd = (p + size).toInt()
        if (boxEnd > end) break
        fn(typ, p + h, boxEnd)
        p = boxEnd
    }
}

private fun readBoxHeader(din: DataInputStream): Triple<Long, String, Long>? {
    return try {
        val szLo = readU32be(din)
        val typ = read4ccDin(din)
        if (szLo < 8L) return null
        if (szLo == 1L) {
            val szHi = readU64be(din)
            Triple(szHi, typ, 16L)
        } else {
            Triple(szLo, typ, 8L)
        }
    } catch (_: EOFException) {
        null
    }
}

private fun readU32be(din: DataInputStream): Long {
    return din.readInt().toLong() and 0xffffffffL
}

private fun readU64be(din: DataInputStream): Long {
    val hi = din.readInt().toLong() and 0xffffffffL
    val lo = din.readInt().toLong() and 0xffffffffL
    return (hi shl 32) or lo
}

private fun read4ccDin(din: DataInputStream): String {
    val b = ByteArray(4)
    din.readFully(b)
    return b.toString(Charsets.US_ASCII)
}

private fun readFully(din: DataInputStream, len: Int): ByteArray {
    if (len == 0) return ByteArray(0)
    val b = ByteArray(len)
    din.readFully(b)
    return b
}

private fun skipFully(din: DataInputStream, n: Long) {
    var left = n
    while (left > 0L) {
        val k = min(left, 1 shl 20).toInt()
        val s = din.skipBytes(k)
        if (s <= 0) {
            if (din.read() < 0) break
            left--
        } else {
            left -= s.toLong()
        }
    }
}

private fun readU32(buf: ByteArray, off: Int): Int {
    return ((buf[off].toInt() and 0xff) shl 24) or
        ((buf[off + 1].toInt() and 0xff) shl 16) or
        ((buf[off + 2].toInt() and 0xff) shl 8) or
        (buf[off + 3].toInt() and 0xff)
}

private fun readU24(buf: ByteArray, off: Int): Int {
    return ((buf[off].toInt() and 0xff) shl 16) or
        ((buf[off + 1].toInt() and 0xff) shl 8) or
        (buf[off + 2].toInt() and 0xff)
}

private fun readU64(buf: ByteArray, off: Int): Long {
    val hi = readU32(buf, off).toLong() and 0xffffffffL
    val lo = readU32(buf, off + 4).toLong() and 0xffffffffL
    return (hi shl 32) or lo
}

private fun read4cc(buf: ByteArray, off: Int): String {
    return buf.copyOfRange(off, off + 4).toString(Charsets.US_ASCII)
}
