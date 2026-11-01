package com.example.data.detector

import com.example.data.model.MediaItem
import com.example.data.model.MediaType

sealed class ContentSupportStatus {
    object Supported : ContentSupportStatus()
    data class Partial(val message: String) : ContentSupportStatus()
    data class Unsupported(val reason: String) : ContentSupportStatus()
}

interface PlatformAdapter {
    val platformName: String
    val iconKey: String

    fun canHandle(url: String): Boolean
    fun getSupportStatus(): ContentSupportStatus
    suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem>
}

class DirectPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Direct Link"
    override val iconKey: String = "link"

    override fun canHandle(url: String): Boolean {
        val clean = url.lowercase().split("?").first()
        val directExtensions = listOf(
            "mp4", "webm", "mkv", "mov", "avi", "3gp",
            "jpg", "jpeg", "png", "webp", "gif", "bmp",
            "mp3", "m4a", "aac", "ogg", "wav", "flac",
            "pdf", "doc", "docx", "txt"
        )
        return directExtensions.any { clean.endsWith(".$it") }
    }

    override fun getSupportStatus(): ContentSupportStatus = ContentSupportStatus.Supported

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        val clean = url.split("?").first()
        val ext = clean.substringAfterLast(".", "").lowercase()
        val fileName = clean.substringAfterLast("/").ifEmpty { "media_$ext" }
        val type = MediaType.fromMimeOrExtension(null, ext)
        val title = fileName.substringBeforeLast(".")

        return listOf(
            MediaItem(
                url = url,
                title = title,
                type = type,
                mimeType = when (type) {
                    MediaType.VIDEO -> "video/$ext"
                    MediaType.IMAGE -> "image/$ext"
                    MediaType.GIF -> "image/gif"
                    MediaType.AUDIO -> "audio/$ext"
                    MediaType.DOCUMENT -> "application/$ext"
                    MediaType.OTHER -> "application/octet-stream"
                },
                platformName = platformName,
                detectedFromUrl = url,
                originalFilename = fileName
            )
        )
    }
}

class RedditPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Reddit"
    override val iconKey: String = "reddit"

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("reddit.com") || lower.contains("redd.it") || lower.contains("v.redd.it") || lower.contains("i.redd.it")
    }

    override fun getSupportStatus(): ContentSupportStatus = ContentSupportStatus.Supported

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        // v.redd.it or i.redd.it direct media
        if (url.contains("i.redd.it")) {
            val filename = url.substringAfterLast("/").substringBefore("?")
            val ext = filename.substringAfterLast(".", "jpg")
            return listOf(
                MediaItem(
                    url = url,
                    title = "Reddit Image $filename",
                    type = MediaType.fromMimeOrExtension(null, ext),
                    thumbnailUri = url,
                    platformName = platformName,
                    detectedFromUrl = url,
                    originalFilename = filename
                )
            )
        }

        // Reddit JSON API
        val jsonUrl = if (!url.endsWith(".json")) {
            url.split("?").first().trimEnd('/') + ".json"
        } else url

        try {
            val request = okhttp3.Request.Builder()
                .url(jsonUrl)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; SmartMediaSaver/1.0)")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val items = mutableListOf<MediaItem>()

                    // Extract fallback_url for video
                    val fallbackRegex = """"fallback_url"\s*:\s*"([^"]+)"""".toRegex()
                    val fallbackMatch = fallbackRegex.find(body)
                    val titleRegex = """"title"\s*:\s*"([^"]+)"""".toRegex()
                    val titleMatch = titleRegex.find(body)
                    val title = titleMatch?.groupValues?.get(1) ?: "Reddit Media"

                    if (fallbackMatch != null) {
                        val videoUrl = fallbackMatch.groupValues[1].replace("\\u0026", "&")
                        items.add(
                            MediaItem(
                                url = videoUrl,
                                title = title,
                                type = MediaType.VIDEO,
                                mimeType = "video/mp4",
                                platformName = platformName,
                                detectedFromUrl = url,
                                originalFilename = "${title.take(30).replace("[^a-zA-Z0-9]".toRegex(), "_")}.mp4"
                            )
                        )
                    }

                    // Extract image url
                    val imgRegex = """"url_overridden_by_dest"\s*:\s*"([^"]+)"""".toRegex()
                    val imgMatch = imgRegex.find(body)
                    if (imgMatch != null) {
                        val imgUrl = imgMatch.groupValues[1].replace("\\u0026", "&")
                        val ext = imgUrl.substringAfterLast(".").substringBefore("?")
                        val type = MediaType.fromMimeOrExtension(null, ext)
                        if (type != MediaType.OTHER) {
                            items.add(
                                MediaItem(
                                    url = imgUrl,
                                    title = title,
                                    type = type,
                                    thumbnailUri = imgUrl,
                                    platformName = platformName,
                                    detectedFromUrl = url,
                                    originalFilename = "${title.take(30).replace("[^a-zA-Z0-9]".toRegex(), "_")}.$ext"
                                )
                            )
                        }
                    }

                    if (items.isNotEmpty()) return items
                }
            }
        } catch (_: Exception) {}

        return emptyList()
    }
}

class PinterestPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Pinterest"
    override val iconKey: String = "pinterest"

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("pinterest.com") || lower.contains("pin.it") || lower.contains("pinimg.com")
    }

    override fun getSupportStatus(): ContentSupportStatus = ContentSupportStatus.Supported

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        // Direct pinimg url
        if (url.contains("pinimg.com")) {
            val filename = url.substringAfterLast("/").substringBefore("?")
            val ext = filename.substringAfterLast(".", "jpg")
            return listOf(
                MediaItem(
                    url = url,
                    title = "Pinterest Pin $filename",
                    type = MediaType.fromMimeOrExtension(null, ext),
                    thumbnailUri = url,
                    platformName = platformName,
                    detectedFromUrl = url,
                    originalFilename = filename
                )
            )
        }
        return emptyList() // will be handled by OpenGraph metadata parser
    }
}

class InstagramPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Instagram"
    override val iconKey: String = "instagram"

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("instagram.com") || lower.contains("instagr.am")
    }

    override fun getSupportStatus(): ContentSupportStatus =
        ContentSupportStatus.Partial("Public posts and reels supported via OpenGraph and public metadata.")

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        // Handled via webpage OpenGraph parser with Instagram-compatible User-Agent
        return emptyList()
    }
}

class XPlatformAdapter : PlatformAdapter {
    override val platformName: String = "X (Twitter)"
    override val iconKey: String = "twitter"

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("twitter.com") || lower.contains("x.com") || lower.contains("t.co") || lower.contains("twimg.com")
    }

    override fun getSupportStatus(): ContentSupportStatus =
        ContentSupportStatus.Partial("Public media tweets supported via OpenGraph and syndicated media endpoints.")

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        if (url.contains("twimg.com")) {
            val filename = url.substringAfterLast("/").substringBefore("?")
            val ext = filename.substringAfterLast(".", "jpg")
            return listOf(
                MediaItem(
                    url = url,
                    title = "X Media $filename",
                    type = MediaType.fromMimeOrExtension(null, ext),
                    thumbnailUri = url,
                    platformName = platformName,
                    detectedFromUrl = url,
                    originalFilename = filename
                )
            )
        }
        return emptyList()
    }
}

class FacebookPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Facebook"
    override val iconKey: String = "facebook"

    override fun canHandle(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com")
    }

    override fun getSupportStatus(): ContentSupportStatus =
        ContentSupportStatus.Partial("Public videos & photos supported via metadata extraction.")

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        return emptyList()
    }
}

class GenericWebPlatformAdapter : PlatformAdapter {
    override val platformName: String = "Webpage"
    override val iconKey: String = "web"

    override fun canHandle(url: String): Boolean = url.startsWith("http://") || url.startsWith("https://")

    override fun getSupportStatus(): ContentSupportStatus = ContentSupportStatus.Supported

    override suspend fun extractMedia(url: String, client: okhttp3.OkHttpClient): List<MediaItem> {
        return emptyList()
    }
}
