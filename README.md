# LocalShare

![Logo](raw_assets/logo.svg)

**LocalShare** is a lightweight Android app to share files over your local network via a browser - no cloud, no cables, no extra apps required.

<!--
[![F-Droid](https://img.shields.io/badge/F--Droid-Download-blue)](https://f-droid.org/packages/com.defname.localshare)
[![Google Play](https://img.shields.io/badge/Google%20Play-Download-green)](https://play.google.com/store/apps/details?id=com.defname.localshare)
-->

## Features

- Share files directly via the Android share sheet
- Access files through a clean browser-based web interface
- Live-updating web interface for the receiver (no refresh needed)
- Stream video/audio with seeking support (HTTP Range)
- Share multiple files or download as ZIP (on-the-fly)
- Token-based access + optional device approval
- 100% ad-free & open source

## Quick Start

1. Share files to LocalShare  
2. Tap **Start Sharing**  
3. Open the shown URL (or scan QR code) on another device  
4. Download or stream  

## Security

> [!IMPORTANT]
> Files are transferred over plain HTTP.

LocalShare is designed for **trusted local networks only** (e.g. your home Wi-Fi).
Avoid using it on public or untrusted networks, especially for sensitive data.

## Documentation

See the full [documentation](docs/index.md) in the `docs/` folder.

## Permissions

### Notifications

Required because the server runs as a foreground service.
Also used to prompt you when new devices try to connect.

## Building the project

### Requirements

- Android SDK / Android Studio
- Python 3 (used by a small build script)
- npm (needed for Tailwind CSS)

### Steps

1. Clone the repository (recursively to include the icon theme!)
3. Make sure JAVA_HOME and ANDROID_SDK_ROOT (or ANDROID_HOME) is set correctly
3. Run `./gradlew assembleDebug`
4. Find the APK in `app/build/outputs/apk/debug/`

## License & Credits

-   [![GPL-3.0-or-later](https://img.shields.io/badge/License-GPL--3.0--or--later-blue.svg)](https://spdx.org/licenses/GPL-3.0-or-later.html)

-   **Icons**: Papirus Icon Theme
    Licensed under GPL-3.0
    Maintained by the Papirus Development Team
    https://github.com/PapirusDevelopmentTeam/papirus-icon-theme
