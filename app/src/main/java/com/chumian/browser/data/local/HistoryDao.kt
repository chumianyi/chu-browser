package com.chumian.browser.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.chumian.browser.data.model.HistoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<HistoryItem>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAllList(): List<HistoryItem>

    @Insert
    suspend fun insert(history: HistoryItem)

    @Delete
    suspend fun delete(history: HistoryItem)

    @Query("DELETE FROM history")
    suspend fun clearAll()
}
