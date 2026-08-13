package com.chubrowser.app.password

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val domain: String,
    val username: String,
    val encryptedPassword: String,
    val title: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isFavorite: Boolean = false
)

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE domain = :domain ORDER BY updatedAt DESC")
    suspend fun getByDomain(domain: String): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getById(id: Long): PasswordEntity?

    @Query("SELECT * FROM passwords WHERE url LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun search(query: String): List<PasswordEntity>

    @Insert
    suspend fun insert(password: PasswordEntity): Long

    @Update
    suspend fun update(password: PasswordEntity)

    @Delete
    suspend fun delete(password: PasswordEntity)

    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM passwords")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM passwords")
    suspend fun getCount(): Int

    @Query("SELECT * FROM passwords WHERE domain = :domain AND username = :username LIMIT 1")
    suspend fun findExisting(domain: String, username: String): PasswordEntity?
}
