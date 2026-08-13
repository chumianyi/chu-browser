package com.chumian.browser.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filename: String,
    val url: String,
    val filePath: String,
    val totalSize: Long = 0,
    val downloadedSize: Long = 0,
    val status: Int = STATUS_PENDING,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_PENDING = 0
        const val STATUS_DOWNLOADING = 1
        const val STATUS_COMPLETED = 2
        const val STATUS_FAILED = 3
    }
}
