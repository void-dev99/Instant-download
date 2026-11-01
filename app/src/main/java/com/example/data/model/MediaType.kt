package com.example.data.model

enum class MediaType(val displayName: String, val extension: String) {
    VIDEO("Video", "mp4"),
    IMAGE("Image", "jpg"),
    GIF("Animated GIF", "gif"),
    AUDIO("Audio", "mp3"),
    DOCUMENT("Document", "pdf"),
    OTHER("Media", "bin");

    companion object {
        fun fromMimeOrExtension(mime: String?, ext: String?): MediaType {
            val m = mime?.lowercase() ?: ""
            val e = ext?.lowercase() ?: ""
            return when {
                m.contains("gif") || e == "gif" -> GIF
                m.startsWith("video/") || e in listOf("mp4", "webm", "mkv", "mov", "avi", "3gp") -> VIDEO
                m.startsWith("image/") || e in listOf("jpg", "jpeg", "png", "webp", "bmp", "heic") -> IMAGE
                m.startsWith("audio/") || e in listOf("mp3", "m4a", "aac", "ogg", "wav", "flac") -> AUDIO
                m.contains("pdf") || e in listOf("pdf", "doc", "docx", "epub", "txt") -> DOCUMENT
                else -> OTHER
            }
        }
    }
}
