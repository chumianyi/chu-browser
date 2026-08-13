package com.chubrowser.app.core

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chubrowser.app.password.PasswordDao
import com.chubrowser.app.password.PasswordEntity
import com.chubrowser.app.bookmark.BookmarkDao
import com.chubrowser.app.bookmark.BookmarkEntity
import com.chubrowser.app.bookmark.BookmarkFolderEntity
import com.chubrowser.app.download.DownloadDao
import com.chubrowser.app.download.DownloadEntity
import com.chubrowser.app.history.HistoryDao
import com.chubrowser.app.history.HistoryEntity
import com.chubrowser.app.privacy.PrivacySpaceDao
import com.chubrowser.app.privacy.PrivacySpaceEntity

@Database(
    entities = [
        PasswordEntity::class,
        BookmarkEntity::class,
        BookmarkFolderEntity::class,
        DownloadEntity::class,
        HistoryEntity::class,
        PrivacySpaceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadDao(): DownloadDao
    abstract fun historyDao(): HistoryDao
    abstract fun privacySpaceDao(): PrivacySpaceDao
}
