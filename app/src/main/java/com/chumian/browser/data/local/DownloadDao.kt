package com.chumian.browser.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.chumian.browser.data.model.DownloadItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAll(): Flow<List<DownloadItem>>

    @Insert
    suspend fun insert(download: DownloadItem): Long

    @Update
    suspend fun update(download: DownloadItem)

    @Delete
    suspend fun delete(download: DownloadItem)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadItem?
}
