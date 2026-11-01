# SMART MEDIA SAVER

> Universal Media Downloader & Smart Media Manager for Android  
> **Created & Developed by HARIOM**

[![Android Build & Release](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Platform](https://img.shields.io/badge/Platform-Android%208.0+-3DDC84.svg?logo=android)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF.svg?logo=kotlin)]()
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4.svg?logo=jetpackcompose)]()
[![Creator](https://img.shields.io/badge/Developer-HARIOM-6366F1.svg)]()

---

## 📱 Overview

**SMART MEDIA SAVER** is a lightweight, universal Android media downloader and manager built by **HARIOM**. It allows users to quickly save videos, images, animated GIFs, audio files, and documents from any public website or compatible platform using three intuitive entry points:

1. **Android Share Sheet Integration**: Share a link or reel from Instagram, Reddit, X (Twitter), Pinterest, or any web browser directly to Smart Media Saver.
2. **Built-in Web Browser**: Browse any website with an integrated live media sniffer and 1-tap download floating button.
3. **Smart Clipboard Detection**: Automatically detects copied media links when opening the app and presents a non-intrusive 1-tap download prompt.

---

## ✨ Features

- **Universal Media Engine**:
  - Direct video files: MP4, WebM, MKV, MOV
  - Images & animated media: JPG, PNG, WebP, Animated GIF
  - Audio: MP3, M4A, AAC, OGG, WAV
  - Documents: PDF
  - OpenGraph & HTML5 metadata extraction for public social links.
- **Reliable Download Engine**:
  - Live speed calculation (KB/s, MB/s) and progress percentages.
  - Resume & pause support with HTTP Range headers.
  - Automatic duplicate detection preventing repeated downloads.
  - Automatic media registration with Android MediaStore / scoped storage.
- **Built-in Media Gallery**:
  - Organized tabs: Videos, Images, GIFs, Audio, Documents.
  - Thumbnail previews powered by Coil.
  - 1-tap Open with external media players and System Share (`ACTION_SEND`).
  - Sort by Newest, Oldest, Name, and File Size.
- **Built-in Web Browser**:
  - Address bar with instant search.
  - SSL/HTTPS security indicator.
  - Automatic stream & media sniffing with floating badge.
- **Theme Support**:
  - **Light Theme**: Clean, high-contrast light surfaces (`#F8FAFC`, `#FFFFFF`).
  - **Dark Theme**: Premium deep navy surfaces (`#0B1120`, `#151E2E`).
  - **System Default Theme**: Seamlessly mirrors system appearance.
- **Privacy & Performance**:
  - Zero analytics or tracking.
  - No background clipboard spying (only checked while app is active).
  - Optimized for low-RAM devices (tested on Android 11+ with 3GB RAM).

---

## 🎨 Color System

| Token | Hex | Usage |
|---|---|---|
| **Deep Navy** | `#111827` | Brand Primary Navy |
| **Secondary Navy** | `#1F2937` | Surface Containers / Card Borders |
| **Primary Accent** | `#6366F1` | Primary Interactive Buttons & Highlights |
| **Secondary Accent** | `#8B5CF6` | Secondary Badges & Accents |
| **Background Light** | `#F8FAFC` | Light Theme Canvas |
| **Surface Light** | `#FFFFFF` | Light Theme Cards |
| **Background Dark** | `#0B1120` | Dark Theme Canvas |
| **Surface Dark** | `#151E2E` | Dark Theme Cards |
| **Success** | `#22C55E` | Completed Status & Verified Badges |
| **Warning** | `#F59E0B` | Paused Downloads & Notices |
| **Error** | `#EF4444` | Failed Downloads & Errors |
| **Secondary Text** | `#94A3B8` | Subtitles & Metadata |

---

## 📥 Download APK

### Latest Release
Download the latest pre-compiled, installable APK directly from GitHub Releases:

👉 **[Download Latest APK from GitHub Releases](https://github.com/OWNER/REPOSITORY/releases/latest)**

*(Replace `OWNER/REPOSITORY` with your GitHub username and repository name once pushed, e.g., `https://github.com/hariom125055/smart-media-saver/releases/latest`)*

### Direct Workflow Artifacts
Every commit and release tag automatically triggers `.github/workflows/android-release.yml`, building:
- `SmartMediaSaver-v*.apk`
- `SmartMediaSaver-v*.apk.sha256`

To download:
1. Navigate to the **Actions** tab in your GitHub repository.
2. Select the latest **Android Build & Release** run.
3. Download the **SmartMediaSaver-APK** artifact zip.

---

## 🛠️ Build from Source

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34/36

### Local Compilation
```bash
# Clone the repository
git clone https://github.com/OWNER/REPOSITORY.git
cd smart-media-saver

# Run Unit & Robolectric Tests
gradle :app:testDebugUnitTest

# Build Debug APK
gradle :app:assembleDebug
```
The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🏗️ Architecture & Tech Stack

- **Architecture**: Clean Architecture + MVVM (Model-View-ViewModel)
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Local Persistence**: Room Database (`DownloadEntity`, `HistoryEntity`, `AppSettingsEntity`)
- **Networking**: OkHttp 4 with streaming I/O and range request support
- **Image Loading**: Coil Compose 2.7.0
- **Asynchrony**: Kotlin Coroutines & `StateFlow`
- **Testing**: Robolectric + JUnit 4

---

## ⚠️ Known Limitations & Legal Compliance

1. **Public Content Only**: Smart Media Saver exclusively extracts publicly accessible media streams. It does not defeat DRM, bypass login gates, or access private social accounts.
2. **Platform Terms**: The application respects platform rate limits and terms of service.
3. **HTTP Range Support**: Pause and resume functionality requires server-side support for `Accept-Ranges: bytes`.

---

## 👤 Author & Attribution

**Developer**: HARIOM  
**Project**: Smart Media Saver  
**Contact**: hariom125055@gmail.com
