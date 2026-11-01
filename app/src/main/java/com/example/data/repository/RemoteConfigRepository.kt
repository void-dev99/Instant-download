package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.AppSettingsDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.model.PlatformCapability
import com.example.data.model.RemoteConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteConfigRepository(
    private val context: Context,
    private val appSettingsDao: AppSettingsDao,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    private val _config = MutableStateFlow(RemoteConfig())
    val config: StateFlow<RemoteConfig> = _config.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        // Load cached config from DB if present
        val cachedJson = appSettingsDao.getSettingValue("remote_config_json")
        if (cachedJson != null) {
            parseConfigJson(cachedJson)?.let {
                _config.value = it
            }
        }
    }

    suspend fun refreshConfig(endpointUrl: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (endpointUrl.isNullOrBlank()) {
            _isOffline.value = false
            return@withContext true
        }

        try {
            val request = Request.Builder()
                .url(endpointUrl)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val parsed = parseConfigJson(body)
                    if (parsed != null) {
                        _config.value = parsed
                        _isOffline.value = false
                        appSettingsDao.setSetting(AppSettingsEntity("remote_config_json", body))
                        return@withContext true
                    }
                }
            }
        } catch (_: Exception) {
            _isOffline.value = true
        }
        return@withContext false
    }

    private fun parseConfigJson(jsonStr: String): RemoteConfig? {
        return try {
            val obj = JSONObject(jsonStr)
            val version = obj.optInt("version", 1)
            val minAppVersion = obj.optString("min_app_version", "1.0")
            val status = obj.optString("status", "ACTIVE")
            val announcement = obj.optString("announcement", "Welcome to SMART MEDIA SAVER")
            val maxSizeBytes = obj.optLong("max_download_size_bytes", 1024L * 1024L * 1024L * 2L)

            val platformsList = mutableListOf<PlatformCapability>()
            val arr = obj.optJSONArray("platforms") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                platformsList.add(
                    PlatformCapability(
                        name = item.getString("name"),
                        description = item.optString("description", ""),
                        isEnabled = item.optBoolean("is_enabled", true)
                    )
                )
            }

            RemoteConfig(
                version = version,
                minAppVersion = minAppVersion,
                status = status,
                announcement = announcement,
                supportedPlatforms = if (platformsList.isNotEmpty()) platformsList else _config.value.supportedPlatforms,
                maxDownloadSizeBytes = maxSizeBytes
            )
        } catch (_: Exception) {
            null
        }
    }
}
