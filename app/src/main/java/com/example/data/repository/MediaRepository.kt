package com.example.data.repository

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import com.example.data.detector.DetectionResult
import com.example.data.detector.MediaDetector
import com.example.data.download.DownloadProgressInfo
import com.example.data.download.MediaDownloadManager
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.dao.HistoryDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val historyDao: HistoryDao,
    private val appSettingsDao: AppSettingsDao,
    val downloadManager: MediaDownloadManager = MediaDownloadManager(context, downloadDao),
    val mediaDetector: MediaDetector = MediaDetector()
) {
    val allDownloads: Flow<List<DownloadEntity>> = downloadDao.getAllDownloads()
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val downloadProgressMap: StateFlow<Map<Long, DownloadProgressInfo>> = downloadManager.progressMap

    suspend fun detectMedia(url: String): DetectionResult {
        val result = mediaDetector.detectMedia(url)
        if (result is DetectionResult.Success) {
            historyDao.insert(
                HistoryEntity(
                    url = result.originalUrl,
                    title = result.mediaList.firstOrNull()?.title ?: "Media Link",
                    platform = result.platformName,
                    mediaCount = result.mediaList.size,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
        return result
    }

    suspend fun startDownload(mediaItem: MediaItem): Long {
        return downloadManager.enqueueDownload(mediaItem)
    }

    fun pauseDownload(id: Long) = downloadManager.pauseDownload(id)
    fun cancelDownload(id: Long) = downloadManager.cancelDownload(id)
    fun retryDownload(id: Long) = downloadManager.retryDownload(id)

    suspend fun deleteDownload(id: Long) = withContext(Dispatchers.IO) {
        val entity = downloadDao.getDownloadById(id)
        if (entity != null) {
            if (entity.fileLocation.isNotEmpty()) {
                val f = File(entity.fileLocation)
                if (f.exists()) f.delete()
            }
            downloadDao.deleteById(id)
        }
    }

    suspend fun clearAllHistory() = withContext(Dispatchers.IO) {
        historyDao.clearAll()
    }

    suspend fun clearAllDownloads() = withContext(Dispatchers.IO) {
        downloadDao.clearAll()
    }

    fun getClipboardUrl(): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return null

        if (clipboard.hasPrimaryClip()) {
            val clipDesc = clipboard.primaryClipDescription ?: return null
            if (clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                clipDesc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString() ?: item?.uri?.toString() ?: ""
                val urls = MediaDetector.extractUrlsFromText(text)
                return urls.firstOrNull()
            }
        }
        return null
    }

    suspend fun isAutoDownloadEnabled(): Boolean = withContext(Dispatchers.IO) {
        val setting = appSettingsDao.getSettingValue("pref_auto_download")
        return@withContext setting == "true"
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        appSettingsDao.setSetting(AppSettingsEntity("pref_auto_download", enabled.toString()))
    }

    suspend fun getThemeMode(): String = withContext(Dispatchers.IO) {
        appSettingsDao.getSettingValue("pref_theme_mode") ?: "system"
    }

    suspend fun setThemeMode(mode: String) = withContext(Dispatchers.IO) {
        appSettingsDao.setSetting(AppSettingsEntity("pref_theme_mode", mode))
    }
}
