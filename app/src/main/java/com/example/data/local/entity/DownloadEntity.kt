package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "downloads",
    indices = [Index(value = ["url"], unique = false)]
)
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val platform: String = "Direct",
    val mediaType: String, // VIDEO, IMAGE, GIF, AUDIO, DOCUMENT
    val fileLocation: String = "",
    val fileName: String = "",
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: String, // QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
    val createdTimestamp: Long = System.currentTimeMillis(),
    val errorDetails: String? = null,
    val thumbnailUri: String? = null,
    val mimeType: String = ""
)
