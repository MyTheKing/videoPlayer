package com.joshua.videoplayer.data

/**
 * 启动阶段在后台扫出的本地库快照，供 [com.joshua.videoplayer.library.LibraryViewModel] 首帧填充，减少进入首页后再空列表等待。
 */
object LibraryWarmCache {

    @Volatile
    private var latest: List<LocalVideo>? = null

    fun publish(list: List<LocalVideo>) {
        latest = list
    }

    fun peekLatest(): List<LocalVideo>? = latest

    /** 权限撤销等场景下丢弃预热结果，避免无权限时仍展示旧列表。 */
    fun clear() {
        latest = null
    }
}
