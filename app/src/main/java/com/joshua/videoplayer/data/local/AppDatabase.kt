package com.joshua.videoplayer.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistItemEntity::class,
        IgnoredVideoUriEntity::class,
        FavoriteVideoUriEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun libraryVideoActionsDao(): LibraryVideoActionsDao

    companion object {
        /**
         * v2 → v3：歌单增加 kind / coverImageUri；插入系统「喜欢」歌单并把原 favorite 表迁入该歌单曲目。
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE playlists ADD COLUMN kind INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE playlists ADD COLUMN coverImageUri TEXT")
                val c = db.query("SELECT COUNT(*) FROM playlists WHERE kind = 1")
                c.moveToFirst()
                val existingLiked = c.getInt(0) > 0
                c.close()
                if (!existingLiked) {
                    val now = System.currentTimeMillis()
                    db.execSQL(
                        "INSERT INTO playlists (name, createdAtMillis, kind, coverImageUri) VALUES ('喜欢', $now, 1, NULL)",
                    )
                }
                val c2 = db.query("SELECT id FROM playlists WHERE kind = 1 LIMIT 1")
                if (!c2.moveToFirst()) {
                    c2.close()
                    return
                }
                val likedId = c2.getLong(0)
                c2.close()
                val favs = db.query("SELECT contentUri FROM favorite_video_uris")
                var order = 0
                while (favs.moveToNext()) {
                    val uri = favs.getString(0) ?: continue
                    val cv = ContentValues().apply {
                        put("playlistId", likedId)
                        put("contentUri", uri)
                        put("displayTitle", "")
                        put("durationMs", 0L)
                        put("sortOrder", order++)
                    }
                    db.insert("playlist_items", SQLiteDatabase.CONFLICT_IGNORE, cv)
                }
                favs.close()
            }
        }

        /** 全新安装 v3：在 playlists 表建好后写入系统「喜欢」歌单。 */
        val CALLBACK_SEED_LIKED = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val cv = ContentValues().apply {
                    put("name", "喜欢")
                    put("createdAtMillis", System.currentTimeMillis())
                    put("kind", PlaylistEntity.KIND_LIKED)
                    putNull("coverImageUri")
                }
                db.insert("playlists", SQLiteDatabase.CONFLICT_ABORT, cv)
            }
        }
    }
}
