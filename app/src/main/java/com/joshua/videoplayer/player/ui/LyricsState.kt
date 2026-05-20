package com.joshua.videoplayer.player.ui

/**
 * 单行歌词数据。
 *
 * @param timestampMs 歌词对应的时间戳（毫秒）
 * @param text 歌词文本
 */
data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

/**
 * 歌词状态接口。当前返回空列表，后续接入字幕轨解析时实现此接口。
 */
interface LyricsState {
    /** 当前歌词行列表，按时间戳升序排列。 */
    val lines: List<LyricLine>

    /** 根据播放位置 [positionMs] 返回当前应高亮的行索引，-1 表示无匹配。 */
    fun activeLineIndex(positionMs: Long): Int
}

/** 空歌词状态，用于没有歌词数据时的占位。 */
object EmptyLyricsState : LyricsState {
    override val lines: List<LyricLine> = emptyList()
    override fun activeLineIndex(positionMs: Long): Int = -1
}
