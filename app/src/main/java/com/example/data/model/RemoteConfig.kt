package com.example.data.model

data class RemoteConfig(
    val version: Int = 1,
    val minAppVersion: String = "1.0",
    val status: String = "ACTIVE",
    val announcement: String = "Welcome to SMART MEDIA SAVER by HARIOM",
    val supportedPlatforms: List<PlatformCapability> = listOf(
        PlatformCapability("Direct Link", "Direct video, image, audio & file URLs", true),
        PlatformCapability("Instagram", "Reels, Posts & Story links (public)", true),
        PlatformCapability("Facebook", "Public video & photo links", true),
        PlatformCapability("Pinterest", "Pins & idea pin images/videos", true),
        PlatformCapability("Reddit", "Reddit video & image posts", true),
        PlatformCapability("X (Twitter)", "Public media tweets", true),
        PlatformCapability("Web Browser", "Universal media sniffer on any webpage", true)
    ),
    val maxDownloadSizeBytes: Long = 1024L * 1024L * 1024L * 2L, // 2GB
    val userAgent: String = "Mozilla/5.0 (Linux; Android 11; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36 SmartMediaSaver/1.0"
)

data class PlatformCapability(
    val name: String,
    val description: String,
    val isEnabled: Boolean
)
