package com.example.data.detector

import android.webkit.URLUtil
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class DetectionResult {
    data class Success(
        val originalUrl: String,
        val platformName: String,
        val mediaList: List<MediaItem>
    ) : DetectionResult()

    data class Error(val message: String, val originalUrl: String? = null) : DetectionResult()
    object Empty : DetectionResult()
}

class MediaDetector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {

    private val adapters: List<PlatformAdapter> = listOf(
        DirectPlatformAdapter(),
        RedditPlatformAdapter(),
        PinterestPlatformAdapter(),
        InstagramPlatformAdapter(),
        XPlatformAdapter(),
        FacebookPlatformAdapter(),
        GenericWebPlatformAdapter()
    )

    companion object {
        private val URL_REGEX = Pattern.compile(
            """https?://[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+"""
        )

        fun extractUrlsFromText(text: String): List<String> {
            val matcher = URL_REGEX.matcher(text)
            val urls = mutableListOf<String>()
            while (matcher.find()) {
                var url = matcher.group()
                // Clean trailing punctuation
                url = url.trimEnd('.', ',', ')', ']', ';', '>', '"', '\'')
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    urls.add(url)
                }
            }
            return urls
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "Unknown size"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                kb >= 1.0 -> String.format("%.0f KB", kb)
                else -> "$bytes B"
            }
        }
    }

    suspend fun detectMedia(rawInput: String): DetectionResult = withContext(Dispatchers.IO) {
        val urls = extractUrlsFromText(rawInput)
        val url = if (urls.isNotEmpty()) urls.first() else rawInput.trim()

        if (!URLUtil.isValidUrl(url) || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            return@withContext DetectionResult.Error("Invalid URL: Please provide a valid HTTP or HTTPS link.")
        }

        val adapter = adapters.firstOrNull { it.canHandle(url) } ?: GenericWebPlatformAdapter()

        try {
            // 1. Try adapter specific extraction
            val adapterMedia = adapter.extractMedia(url, client)
            if (adapterMedia.isNotEmpty()) {
                // Enrich items with headers validation
                val enriched = adapterMedia.map { item -> enrichMediaItem(item) }
                return@withContext DetectionResult.Success(
                    originalUrl = url,
                    platformName = adapter.platformName,
                    mediaList = enriched
                )
            }

            // 2. Perform HTTP HEAD to check if direct media
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; SmartMediaSaver/1.0)")
                .build()

            var headResponse: Response? = null
            try {
                headResponse = client.newCall(headRequest).execute()
            } catch (_: Exception) {}

            if (headResponse != null && headResponse.isSuccessful) {
                val contentType = headResponse.header("Content-Type")?.lowercase() ?: ""
                val contentLength = headResponse.header("Content-Length")?.toLongOrNull() ?: 0L
                val disposition = headResponse.header("Content-Disposition") ?: ""
                headResponse.close()

                if (isDirectMediaMime(contentType)) {
                    val filename = extractFilename(url, disposition, contentType)
                    val ext = filename.substringAfterLast(".", "")
                    val type = MediaType.fromMimeOrExtension(contentType, ext)
                    val title = filename.substringBeforeLast(".")

                    val item = MediaItem(
                        url = url,
                        title = title,
                        type = type,
                        mimeType = contentType,
                        sizeBytes = contentLength,
                        formattedSize = formatBytes(contentLength),
                        thumbnailUri = if (type == MediaType.IMAGE || type == MediaType.GIF) url else null,
                        platformName = adapter.platformName,
                        detectedFromUrl = url,
                        originalFilename = filename
                    )
                    return@withContext DetectionResult.Success(
                        originalUrl = url,
                        platformName = adapter.platformName,
                        mediaList = listOf(item)
                    )
                }
            }

            // 3. Fallback: Parse HTML for OpenGraph / media tags
            val getRequest = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "facebookexternalhit/1.1 (+http://www.facebook.com/externalhit_uatext.php)")
                .build()

            client.newCall(getRequest).execute().use { getResponse ->
                if (!getResponse.isSuccessful) {
                    return@withContext DetectionResult.Error("HTTP request returned error code: ${getResponse.code}")
                }

                val html = getResponse.body?.string() ?: ""
                val extracted = parseHtmlMedia(url, html, adapter.platformName)
                if (extracted.isNotEmpty()) {
                    return@withContext DetectionResult.Success(
                        originalUrl = url,
                        platformName = adapter.platformName,
                        mediaList = extracted
                    )
                } else {
                    return@withContext DetectionResult.Error("No downloadable media found at this URL. You can open it in the built-in browser.")
                }
            }
        } catch (e: Exception) {
            return@withContext DetectionResult.Error("Failed to access link: ${e.message ?: "Network error"}")
        }
    }

    private fun isDirectMediaMime(mime: String): Boolean {
        return mime.startsWith("video/") ||
                mime.startsWith("image/") ||
                mime.startsWith("audio/") ||
                mime.contains("pdf") ||
                mime.contains("octet-stream")
    }

    private fun extractFilename(url: String, disposition: String, mime: String): String {
        // Try Content-Disposition
        val filenameRegex = """filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?""".toRegex(RegexOption.IGNORE_CASE)
        val match = filenameRegex.find(disposition)
        if (match != null) {
            val name = match.groupValues[1].trim()
            if (name.isNotEmpty()) return name
        }

        // Try URL path
        try {
            val path = URL(url).path
            val candidate = path.substringAfterLast("/").substringBefore("?")
            if (candidate.isNotEmpty() && candidate.contains(".")) {
                return candidate
            }
        } catch (_: Exception) {}

        // Fallback using extension from mime
        val ext = when {
            mime.contains("mp4") -> "mp4"
            mime.contains("webm") -> "webm"
            mime.contains("gif") -> "gif"
            mime.contains("png") -> "png"
            mime.contains("webp") -> "webp"
            mime.contains("jpeg") || mime.contains("jpg") -> "jpg"
            mime.contains("mp3") -> "mp3"
            mime.contains("pdf") -> "pdf"
            else -> "media"
        }
        return "download_${System.currentTimeMillis()}.$ext"
    }

    private fun parseHtmlMedia(baseUrl: String, html: String, platformName: String): List<MediaItem> {
        val items = mutableListOf<MediaItem>()

        // 1. Title
        val ogTitleRegex = """<meta\s+property=["']og:title["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val titleTagRegex = """<title[^>]*>([^<]+)</title>""".toRegex(RegexOption.IGNORE_CASE)
        val title = ogTitleRegex.find(html)?.groupValues?.get(1)
            ?: titleTagRegex.find(html)?.groupValues?.get(1)?.trim()
            ?: "Media from $platformName"

        // 2. OpenGraph Video
        val ogVideoRegex = """<meta\s+(?:property|name)=["'](?:og:video|og:video:secure_url|twitter:player:stream)["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val videoMatches = ogVideoRegex.findAll(html)
        for (m in videoMatches) {
            val vUrl = resolveUrl(baseUrl, m.groupValues[1])
            if (vUrl.isNotEmpty() && items.none { it.url == vUrl }) {
                items.add(
                    MediaItem(
                        url = vUrl,
                        title = "$title (Video)",
                        type = MediaType.VIDEO,
                        mimeType = "video/mp4",
                        platformName = platformName,
                        detectedFromUrl = baseUrl,
                        originalFilename = "${sanitizeFilename(title)}.mp4"
                    )
                )
            }
        }

        // 3. OpenGraph Image
        val ogImageRegex = """<meta\s+(?:property|name)=["'](?:og:image|og:image:secure_url|twitter:image)["']\s+content=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        val imageMatches = ogImageRegex.findAll(html)
        for (m in imageMatches) {
            val imgUrl = resolveUrl(baseUrl, m.groupValues[1])
            if (imgUrl.isNotEmpty() && items.none { it.url == imgUrl }) {
                val ext = imgUrl.substringAfterLast(".").substringBefore("?").lowercase()
                val type = if (ext == "gif") MediaType.GIF else MediaType.IMAGE
                items.add(
                    MediaItem(
                        url = imgUrl,
                        title = "$title (Image)",
                        type = type,
                        mimeType = if (type == MediaType.GIF) "image/gif" else "image/jpeg",
                        thumbnailUri = imgUrl,
                        platformName = platformName,
                        detectedFromUrl = baseUrl,
                        originalFilename = "${sanitizeFilename(title)}.$ext"
                    )
                )
            }
        }

        // 4. HTML5 video tag sources
        val videoSrcRegex = """<video[^>]*src=["']([^"']+)["']""".toRegex(RegexOption.IGNORE_CASE)
        for (m in videoSrcRegex.findAll(html)) {
            val vUrl = resolveUrl(baseUrl, m.groupValues[1])
            if (vUrl.isNotEmpty() && items.none { it.url == vUrl }) {
                items.add(
                    MediaItem(
                        url = vUrl,
                        title = "$title (Video)",
                        type = MediaType.VIDEO,
                        mimeType = "video/mp4",
                        platformName = platformName,
                        detectedFromUrl = baseUrl,
                        originalFilename = "${sanitizeFilename(title)}.mp4"
                    )
                )
            }
        }

        return items
    }

    private fun resolveUrl(base: String, relative: String): String {
        return try {
            val cleaned = relative.replace("&amp;", "&").trim()
            if (cleaned.startsWith("http://") || cleaned.startsWith("https://")) {
                cleaned
            } else {
                URL(URL(base), cleaned).toString()
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.take(40).replace("""[^a-zA-Z0-9_\-]""".toRegex(), "_").trim('_').ifEmpty { "download" }
    }

    private fun enrichMediaItem(item: MediaItem): MediaItem {
        try {
            val req = Request.Builder().url(item.url).head().build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val len = resp.header("Content-Length")?.toLongOrNull() ?: item.sizeBytes
                    val mime = resp.header("Content-Type") ?: item.mimeType
                    return item.copy(
                        sizeBytes = len,
                        formattedSize = formatBytes(len),
                        mimeType = mime
                    )
                }
            }
        } catch (_: Exception) {}
        return item
    }
}
