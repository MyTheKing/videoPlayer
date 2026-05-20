package com.joshua.videoplayer

import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.room.Room
import com.joshua.videoplayer.data.FileStorageManager
import com.joshua.videoplayer.data.LanguageManager
import com.joshua.videoplayer.data.LegalAgreementCache
import com.joshua.videoplayer.data.LibraryWarmCache
import com.joshua.videoplayer.data.PlaybackCacheManager
import com.joshua.videoplayer.data.SleepTimerManager
import com.joshua.videoplayer.data.local.AppDatabase
import com.joshua.videoplayer.data.scanLocalLibraryWithDurationProbes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 应用入口：提供单例 [AppDatabase]，避免在 Composable 中重复构建 Room。
 */
class VideoPlayerApp : Application() {

    private val applicationJob = SupervisorJob()
    private val applicationScope = CoroutineScope(applicationJob + Dispatchers.Default)

    private var libraryWarmPrefetchJob: Job? = null

    val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, DB_NAME)
            .addMigrations(AppDatabase.MIGRATION_2_3)
            .addCallback(AppDatabase.CALLBACK_SEED_LIKED)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * 在已授予读视频权限时于后台扫本地库并写入 [LibraryWarmCache]，供首页 [LibraryViewModel] 预热。
     * 权限撤销时会取消进行中的任务；再次授予会重新调度。
     */
    fun ensureLibraryWarmPrefetch(readVideoPermission: String) {
        val granted = ContextCompat.checkSelfPermission(
            this,
            readVideoPermission,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            libraryWarmPrefetchJob?.cancel()
            libraryWarmPrefetchJob = null
            LibraryWarmCache.clear()
            return
        }
        if (LibraryWarmCache.peekLatest() != null) return
        if (libraryWarmPrefetchJob?.isActive == true) return
        libraryWarmPrefetchJob = applicationScope.launch {
            val (list, _) = scanLocalLibraryWithDurationProbes(0)
            LibraryWarmCache.publish(list)
        }
    }

    override fun onCreate() {
        super.onCreate()
        FileStorageManager.init(this)
        SleepTimerManager.init(this)
        LanguageManager.init(this)
        LegalAgreementCache.init(this)
        PlaybackCacheManager.init(this)
    }

    companion object {
        private const val DB_NAME = "video_player.db"
    }
}
