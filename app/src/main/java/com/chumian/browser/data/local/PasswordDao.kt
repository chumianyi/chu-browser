package com.chumian.browser.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.chumian.browser.data.model.PasswordItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY timestamp DESC")
    fun getAll(): Flow<List<PasswordItem>>

    @Query("SELECT * FROM passwords ORDER BY timestamp DESC")
    suspend fun getAllList(): List<PasswordItem>

    @Insert
    suspend fun insert(password: PasswordItem)

    @Delete
    suspend fun delete(password: PasswordItem)

    @Query("DELETE FROM passwords")
    suspend fun clearAll()

    @Query("SELECT * FROM passwords WHERE site LIKE :site LIMIT 1")
    suspend fun findBySite(site: String): PasswordItem?
}
