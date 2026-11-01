package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String = "",
    val platform: String = "Web",
    val mediaCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
