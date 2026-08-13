package com.chumian.browser.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.chumian.browser.data.model.Bookmark
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    suspend fun getAllList(): List<Bookmark>

    @Insert
    suspend fun insert(bookmark: Bookmark)

    @Delete
    suspend fun delete(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM bookmarks WHERE url = :url")
    suspend fun exists(url: String): Int
}
