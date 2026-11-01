package com.example.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.dao.DownloadDao
import com.example.data.local.entity.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class DownloadProgressInfo(
    val downloadId: Long,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    val status: DownloadStatus
)

class MediaDownloadManager(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val _progressMap = MutableStateFlow<Map<Long, DownloadProgressInfo>>(emptyMap())
    val progressMap: StateFlow<Map<Long, DownloadProgressInfo>> = _progressMap.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    suspend fun enqueueDownload(mediaItem: MediaItem): Long = withContext(Dispatchers.IO) {
        // Check duplicate
        val existing = downloadDao.getDownloadByUrl(mediaItem.url)
        if (existing != null && existing.status == DownloadStatus.COMPLETED.name) {
            val file = File(existing.fileLocation)
            if (file.exists()) {
                return@withContext existing.id
            }
        }

        val entity = DownloadEntity(
            url = mediaItem.url,
            title = mediaItem.title.ifEmpty { "Media_${System.currentTimeMillis()}" },
            platform = mediaItem.platformName,
            mediaType = mediaItem.type.name,
            fileLocation = "",
            fileName = mediaItem.originalFilename.ifEmpty { "download_${System.currentTimeMillis()}.${mediaItem.type.extension}" },
            fileSizeBytes = mediaItem.sizeBytes,
            downloadedBytes = 0L,
            status = DownloadStatus.QUEUED.name,
            thumbnailUri = mediaItem.thumbnailUri,
            mimeType = mediaItem.mimeType
        )

        val id = downloadDao.insert(entity)
        startDownload(id)
        return@withContext id
    }

    fun startDownload(downloadId: Long) {
        if (downloadJobs[downloadId]?.isActive == true) return

        val job = scope.launch {
            val entity = downloadDao.getDownloadById(downloadId) ?: return@launch
            executeDownload(entity)
        }
        downloadJobs[downloadId] = job
    }

    fun pauseDownload(downloadId: Long) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)
        scope.launch {
            val current = _progressMap.value[downloadId]
            val downloaded = current?.downloadedBytes ?: 0L
            val total = current?.totalBytes ?: 0L
            downloadDao.updateProgress(downloadId, DownloadStatus.PAUSED.name, downloaded, total)
            updateProgress(downloadId, downloaded, total, 0L, DownloadStatus.PAUSED)
        }
    }

    fun cancelDownload(downloadId: Long) {
        downloadJobs[downloadId]?.cancel()
        downloadJobs.remove(downloadId)
        scope.launch {
            val entity = downloadDao.getDownloadById(downloadId)
            if (entity != null) {
                // Delete partial file
                val tempFile = getTempFile(entity.fileName)
                if (tempFile.exists()) tempFile.delete()
                downloadDao.updateProgress(downloadId, DownloadStatus.CANCELLED.name, 0, entity.fileSizeBytes)
                updateProgress(downloadId, 0, entity.fileSizeBytes, 0, DownloadStatus.CANCELLED)
            }
        }
    }

    fun retryDownload(downloadId: Long) {
        startDownload(downloadId)
    }

    private suspend fun executeDownload(entity: DownloadEntity) = withContext(Dispatchers.IO) {
        val downloadId = entity.id
        val tempFile = getTempFile(entity.fileName)
        var downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L

        downloadDao.updateProgress(downloadId, DownloadStatus.DOWNLOADING.name, downloadedBytes, entity.fileSizeBytes)
        updateProgress(downloadId, downloadedBytes, entity.fileSizeBytes, 0L, DownloadStatus.DOWNLOADING)

        try {
            val requestBuilder = Request.Builder()
                .url(entity.url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 11; SmartMediaSaver/1.0)")

            // Support resume if temp file already has bytes
            if (downloadedBytes > 0) {
                requestBuilder.header("Range", "bytes=$downloadedBytes-")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                if (response.code == 416) { // Range not satisfiable, restart
                    tempFile.delete()
                    downloadedBytes = 0L
                } else {
                    throw IllegalStateException("HTTP ${response.code}: ${response.message}")
                }
            }

            val body = response.body ?: throw IllegalStateException("Empty response body")
            val isPartial = response.code == 206
            val serverContentLength = body.contentLength()
            val totalBytes = if (isPartial) downloadedBytes + serverContentLength else (if (serverContentLength > 0) serverContentLength else entity.fileSizeBytes)

            if (!isPartial && downloadedBytes > 0) {
                // Server doesn't support range, restart download
                tempFile.delete()
                downloadedBytes = 0L
            }

            val inputStream: InputStream = body.byteStream()
            val output = if (downloadedBytes > 0) {
                RandomAccessFile(tempFile, "rw").apply { seek(downloadedBytes) }
            } else {
                FileOutputStream(tempFile)
            }

            val buffer = ByteArray(8192)
            var bytesRead: Int
            var lastTime = System.currentTimeMillis()
            var bytesSinceLastTime = 0L
            var currentSpeed = 0L

            try {
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (!isActive) {
                        return@withContext
                    }
                    if (output is RandomAccessFile) {
                        output.write(buffer, 0, bytesRead)
                    } else if (output is FileOutputStream) {
                        output.write(buffer, 0, bytesRead)
                    }

                    downloadedBytes += bytesRead
                    bytesSinceLastTime += bytesRead

                    val now = System.currentTimeMillis()
                    val timeDiff = now - lastTime
                    if (timeDiff >= 500) {
                        currentSpeed = (bytesSinceLastTime * 1000) / timeDiff
                        lastTime = now
                        bytesSinceLastTime = 0L
                        updateProgress(downloadId, downloadedBytes, totalBytes, currentSpeed, DownloadStatus.DOWNLOADING)
                        downloadDao.updateProgress(downloadId, DownloadStatus.DOWNLOADING.name, downloadedBytes, totalBytes)
                    }
                }
            } finally {
                if (output is RandomAccessFile) output.close()
                if (output is FileOutputStream) output.close()
                inputStream.close()
                response.close()
            }

            // Move completed file to media storage
            val finalFile = moveToStorage(tempFile, entity.fileName, entity.mediaType, entity.mimeType)
            val finalLocation = finalFile.absolutePath

            downloadDao.update(
                entity.copy(
                    status = DownloadStatus.COMPLETED.name,
                    fileLocation = finalLocation,
                    downloadedBytes = downloadedBytes,
                    fileSizeBytes = downloadedBytes,
                    errorDetails = null
                )
            )
            updateProgress(downloadId, downloadedBytes, downloadedBytes, 0L, DownloadStatus.COMPLETED)
            downloadJobs.remove(downloadId)

        } catch (e: Exception) {
            val errorMsg = e.message ?: "Download failed"
            downloadDao.updateProgress(downloadId, DownloadStatus.FAILED.name, downloadedBytes, entity.fileSizeBytes, errorMsg)
            updateProgress(downloadId, downloadedBytes, entity.fileSizeBytes, 0L, DownloadStatus.FAILED)
            downloadJobs.remove(downloadId)
        }
    }

    private fun getTempFile(fileName: String): File {
        val cacheDir = File(context.cacheDir, "downloads")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        return File(cacheDir, "$fileName.part")
    }

    private fun moveToStorage(tempFile: File, originalName: String, mediaTypeStr: String, mime: String): File {
        val type = try { MediaType.valueOf(mediaTypeStr) } catch (_: Exception) { MediaType.OTHER }
        val targetDirName = when (type) {
            MediaType.VIDEO -> Environment.DIRECTORY_MOVIES
            MediaType.IMAGE, MediaType.GIF -> Environment.DIRECTORY_PICTURES
            MediaType.AUDIO -> Environment.DIRECTORY_MUSIC
            else -> Environment.DIRECTORY_DOWNLOADS
        }

        val baseDir = context.getExternalFilesDir(targetDirName) ?: context.filesDir
        var targetFile = File(baseDir, originalName)
        var counter = 1
        val baseName = originalName.substringBeforeLast(".")
        val ext = originalName.substringAfterLast(".", "")

        while (targetFile.exists()) {
            val candidate = if (ext.isNotEmpty()) "${baseName}_$counter.$ext" else "${baseName}_$counter"
            targetFile = File(baseDir, candidate)
            counter++
        }

        tempFile.copyTo(targetFile, overwrite = true)
        tempFile.delete()

        // Also notify MediaStore if applicable
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, targetFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime.ifEmpty { "application/octet-stream" })
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
            }
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } catch (_: Exception) {}

        return targetFile
    }

    private fun updateProgress(id: Long, downloaded: Long, total: Long, speed: Long, status: DownloadStatus) {
        val current = _progressMap.value.toMutableMap()
        current[id] = DownloadProgressInfo(id, downloaded, total, speed, status)
        _progressMap.value = current
    }
}
