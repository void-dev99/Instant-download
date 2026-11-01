package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.detector.DetectionResult
import com.example.data.local.AppDatabase
import com.example.data.local.entity.DownloadEntity
import com.example.data.local.entity.HistoryEntity
import com.example.data.model.MediaItem
import com.example.data.model.RemoteConfig
import com.example.data.repository.MediaRepository
import com.example.data.repository.RemoteConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiDetectionState {
    object Idle : UiDetectionState()
    data class Loading(val url: String) : UiDetectionState()
    data class Success(val originalUrl: String, val platform: String, val items: List<MediaItem>) : UiDetectionState()
    data class Error(val message: String, val url: String? = null) : UiDetectionState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val mediaRepository = MediaRepository(
        context = application,
        downloadDao = database.downloadDao(),
        historyDao = database.historyDao(),
        appSettingsDao = database.appSettingsDao()
    )

    val remoteConfigRepository = RemoteConfigRepository(
        context = application,
        appSettingsDao = database.appSettingsDao()
    )

    // Screens: "home", "browser", "downloads", "gallery", "settings"
    private val _currentScreen = MutableStateFlow("home")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    private val _inputUrl = MutableStateFlow("")
    val inputUrl: StateFlow<String> = _inputUrl.asStateFlow()

    private val _clipboardSuggestion = MutableStateFlow<String?>(null)
    val clipboardSuggestion: StateFlow<String?> = _clipboardSuggestion.asStateFlow()

    private var lastDismissedClipboardUrl: String? = null

    private val _detectionState = MutableStateFlow<UiDetectionState>(UiDetectionState.Idle)
    val detectionState: StateFlow<UiDetectionState> = _detectionState.asStateFlow()

    private val _browserUrl = MutableStateFlow("https://www.google.com")
    val browserUrl: StateFlow<String> = _browserUrl.asStateFlow()

    val downloads: StateFlow<List<DownloadEntity>> = mediaRepository.allDownloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = mediaRepository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloadProgress = mediaRepository.downloadProgressMap

    val remoteConfig: StateFlow<RemoteConfig> = remoteConfigRepository.config
    val isOffline = remoteConfigRepository.isOffline

    private val _autoDownload = MutableStateFlow(false)
    val autoDownload: StateFlow<Boolean> = _autoDownload.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    init {
        viewModelScope.launch {
            remoteConfigRepository.initialize()
            _autoDownload.value = mediaRepository.isAutoDownloadEnabled()
            _themeMode.value = mediaRepository.getThemeMode()
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        viewModelScope.launch {
            mediaRepository.setThemeMode(mode)
        }
    }

    fun navigateTo(screen: String) {
        _currentScreen.value = screen
    }

    fun setInputUrl(url: String) {
        _inputUrl.value = url
    }

    fun checkClipboard() {
        val clipUrl = mediaRepository.getClipboardUrl()
        if (clipUrl != null && clipUrl != lastDismissedClipboardUrl && clipUrl != _inputUrl.value) {
            _clipboardSuggestion.value = clipUrl
        }
    }

    fun applyClipboardSuggestion() {
        _clipboardSuggestion.value?.let { url ->
            _inputUrl.value = url
            _clipboardSuggestion.value = null
            detectMedia(url)
        }
    }

    fun dismissClipboardSuggestion() {
        lastDismissedClipboardUrl = _clipboardSuggestion.value
        _clipboardSuggestion.value = null
    }

    fun handleSharedIntentUrl(url: String) {
        _inputUrl.value = url
        _currentScreen.value = "home"
        detectMedia(url)
    }

    fun detectMedia(rawUrl: String) {
        val target = rawUrl.trim()
        if (target.isEmpty()) {
            _detectionState.value = UiDetectionState.Error("Please enter or paste a valid link.")
            return
        }

        _detectionState.value = UiDetectionState.Loading(target)
        viewModelScope.launch {
            when (val result = mediaRepository.detectMedia(target)) {
                is DetectionResult.Success -> {
                    _detectionState.value = UiDetectionState.Success(
                        originalUrl = result.originalUrl,
                        platform = result.platformName,
                        items = result.mediaList
                    )
                    // Auto-download preference
                    if (_autoDownload.value && result.mediaList.size == 1) {
                        startDownload(result.mediaList.first())
                    }
                }
                is DetectionResult.Error -> {
                    _detectionState.value = UiDetectionState.Error(result.message, result.originalUrl)
                }
                is DetectionResult.Empty -> {
                    _detectionState.value = UiDetectionState.Error("No media found at this URL.")
                }
            }
        }
    }

    fun startDownload(item: MediaItem) {
        viewModelScope.launch {
            mediaRepository.startDownload(item)
            _currentScreen.value = "downloads"
        }
    }

    fun pauseDownload(id: Long) = mediaRepository.pauseDownload(id)
    fun cancelDownload(id: Long) = mediaRepository.cancelDownload(id)
    fun retryDownload(id: Long) = mediaRepository.retryDownload(id)
    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            mediaRepository.deleteDownload(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearAllHistory()
        }
    }

    fun clearDownloads() {
        viewModelScope.launch {
            mediaRepository.clearAllDownloads()
        }
    }

    fun toggleAutoDownload(enabled: Boolean) {
        _autoDownload.value = enabled
        viewModelScope.launch {
            mediaRepository.setAutoDownloadEnabled(enabled)
        }
    }

    fun openInBrowser(url: String) {
        _browserUrl.value = url
        _currentScreen.value = "browser"
    }

    fun clearDetection() {
        _detectionState.value = UiDetectionState.Idle
    }
}
