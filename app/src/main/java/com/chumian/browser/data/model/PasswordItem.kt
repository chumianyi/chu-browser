package com.chumian.browser.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val site: String,
    val username: String,
    val encryptedPassword: String,
    val timestamp: Long = System.currentTimeMillis()
)
