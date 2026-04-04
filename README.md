# LocalShare

![Logo](raw_assets/logo.svg)

LocalShare is a small Android app for sharing files with other devices
on the same local network.

It runs a lightweight HTTP server on your phone, so files can be
accessed from any browser — no cables, no cloud, no internet required.

## Why this exists

I originally built this to stream movies from my phone to a tablet or
laptop without copying them first.

While there are many file-sharing solutions out there, none quite fit my specific workflow:

- **SHTTPS:** I previously used [Simple HTTP Server](https://github.com/truefedex/shttps). While it's a great tool, it serves an entire folder at a time. I wanted a more surgical approach: picking specific files via the Android **Share menu** and serving only those.
- **LocalSend:** I am a big fan of [LocalSend](https://github.com/localsend/localsend) because of its excellent usability and "one-tap" feel. However, LocalSend is primarily designed for file transfers, not for direct media streaming with full seeking support in a browser or external video player.
- **Others** Many other "simple" apps are cluttered with intrusive ads or lock features behind a subscription.

**LocalShare is the hybrid:** It combines the seamless usability of LocalSend's share-sheet integration with the universal accessibility of a web server.

## My Goal for LocalShare
1. **Share via System Share Sheet:** Select files in any app and serve them instantly.
2. **Universal Access:** Access files from any browser or directly in video players (like VLC or MPC) on other devices.
3. **Multi-File Support:** Share one or many files at once without extra steps.

## Features

-   **Easy sharing**
    -   Share files directly via the Android share sheet
    -   Generate a link or QR code for quick access
    -   **100% Ad-free and Open Source**
-   **Multiple files support**
    -   Share multiple files at once
    -   **Smart Zipping:** Generates a ZIP archive **on-the-fly**. It uses zero extra storage on your phone, as the archive is streamed directly to the requester.
    -   Or browse files individually in the browser and download/stream them one by one
-   **Optimized for Streaming**
    -   Stream video/audio directly in any modern browser or external player.
    -   **Seeking support** (HTTP Range requests), allowing you to skip ahead in a movie without downloading the entire file first.
-   **Access control**
    -   Token-based access protection
    -   Optional approval for new devices via notification
    -   IP whitelist & blacklist with timeouts
-   **Visibility**
    -   Built-in log viewer to monitor activity in real time
-   **UI**
    -   Built with Jetpack Compose & Material 3
    -   Dark mode support
    -   Simple, functional interface

## How it works

1.  Add files inside the app or share them from another app
2.  Choose the network you want to use (e.g. Wi-Fi, mobile data, or hotspot)
3.  Start the server
4.  Open the shown link on another device (or scan the QR code)
5.  Approve the connection if required
6.  Download or stream the files

## Security note

> [!IMPORTANT]
> Files are transferred over plain HTTP.

LocalShare is designed for **trusted local networks only** (e.g. your home Wi-Fi).
Avoid using it on public or untrusted networks, especially for sensitive data.

## Permissions

-   **Notifications**
    Required because the server runs as a foreground service.
    Also used to prompt you when new devices try to connect.

## Building the project

### Requirements

-   Android SDK / Android Studio
-   Python 3 (used by a small build script)

### Steps

1.  Clone the repository
2.  Open it in Android Studio
3.  Make sure Python 3 is available in your system PATH
4.  Build and run

> [!NOTE]
> The Papirus icon set is not included in the repo.
> It will be downloaded and processed automatically during the build.

## License & Credits

-   **License**: GNU General Public License v3.0 (GPL-3.0)

-   **Icons**: Papirus Icon Theme
    Licensed under GPL-3.0
    Maintained by the Papirus Development Team
    https://github.com/PapirusDevelopmentTeam/papirus-icon-theme