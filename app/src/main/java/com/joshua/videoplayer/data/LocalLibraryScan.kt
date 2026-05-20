package com.joshua.videoplayer.data

import android.app.Application

/**
 * 与 [com.joshua.videoplayer.library.LibraryViewModel] 中刷新逻辑一致：查询 [queryLocalVideos] 并做多轮 [withProbedDurations]。
 *
 * @return 列表与下一轮探测应使用的 [rotateStart]（与 ViewModel 内 [durationProbeRotate] 语义一致）
 */
suspend fun Application.scanLocalLibraryWithDurationProbes(
    durationProbeRotateStart: Int = 0,
): Pair<List<LocalVideo>, Int> {
    val ctx = applicationContext
    var list = ctx.queryLocalVideos()
    var rotate = durationProbeRotateStart
    repeat(5) {
        if (list.none { it.durationMs <= 0L }) return@repeat
        val (nextList, nextRotate) = list.withProbedDurations(ctx, rotate)
        list = nextList
        rotate = nextRotate
    }
    return list to rotate
}
