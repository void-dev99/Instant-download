# Changelog

All notable changes to **SMART MEDIA SAVER** by **HARIOM** are documented in this file.

## [1.0.1] - 2026-11-01

### Added
- **Automated GitHub Release Workflow**: Added `.github/workflows/android-release.yml` for automated tag triggers, testing, APK building, SHA-256 checksum calculation, artifact upload, and GitHub Release asset creation.
- **Theme Mode Switching**: Support for Light Theme, Dark Theme, and System Default Theme with persistent user preference in Room Database.
- **Universal Color System**: Implemented Deep Navy, Secondary Navy, Primary Accent (#6366F1), Secondary Accent (#8B5CF6), Light/Dark surfaces, and semantic indicators.
- **Share-to-App Guidance**: Dedicated instruction card on Home Screen guiding users on sharing directly from social media applications.
- **Webpage Live Media Sniffer**: Floating badge button in built-in browser providing 1-tap download for stream resources detected on active webpage.

### Changed
- Redesigned Home Screen with clean application header, prominent paste button, and minimal visual clutter.
- Enhanced Downloads screen with real-time transfer speed calculation (KB/s, MB/s), progress percentages, and pause/resume/retry/cancel actions.
- Upgraded Gallery screen with 2-column responsive grid, media type filters, and system share intent integration.
- Updated Settings screen with developer attribution to HARIOM, storage cleanup tools, and remote configuration platform status.

### Fixed
- Fixed layout clipping and ensured compliant touch targets across all screen sizes down to 320dp width.
- Corrected status bar and navigation bar insets across light and dark themes.

---

## [1.0.0] - Initial Release

- Core media detection engine (Direct URLs, Reddit, Pinterest, Instagram, X, Facebook, OpenGraph).
- OkHttp streaming download engine with temporary file buffering and duplicate prevention.
- Room database integration for downloads, search history, and settings.
- Android Share Sheet intent filters (`ACTION_SEND`, `ACTION_SEND_MULTIPLE`).
- Built-in Android WebView browser with safe browsing settings.
