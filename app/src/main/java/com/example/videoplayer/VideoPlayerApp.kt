package com.example.videoplayer

import android.app.Application
import androidx.room.Room
import com.example.videoplayer.data.local.AppDatabase

/**
 * 应用入口：提供单例 [AppDatabase]，避免在 Composable 中重复构建 Room。
 */
class VideoPlayerApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    companion object {
        private const val DB_NAME = "video_player.db"
    }
}
