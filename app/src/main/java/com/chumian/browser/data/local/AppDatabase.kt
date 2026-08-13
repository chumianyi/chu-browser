package com.chumian.browser.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chumian.browser.data.model.Bookmark
import com.chumian.browser.data.model.DownloadItem
import com.chumian.browser.data.model.HistoryItem
import com.chumian.browser.data.model.PasswordItem

@Database(
    entities = [Bookmark::class, HistoryItem::class, DownloadItem::class, PasswordItem::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun passwordDao(): PasswordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "chu_browser_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
