package com.example.data.model

data class MediaItem(
    val url: String,
    val title: String,
    val type: MediaType,
    val mimeType: String = "",
    val sizeBytes: Long = 0L,
    val formattedSize: String = "",
    val thumbnailUri: String? = null,
    val platformName: String = "Direct",
    val durationText: String? = null,
    val author: String? = null,
    val detectedFromUrl: String = url,
    val originalFilename: String = ""
)
