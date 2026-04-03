# SendFile

SendFile is a simple utility for sharing files from your Android device to other devices on the same local network. It runs a local HTTP server, allowing anyone on the network to access shared files through a standard web browser—no cloud services or external accounts required.

> [!IMPORTANT]
> **Security Notice**: Files are transferred over unencrypted HTTP. This application is intended for use within **trusted local networks** only. Avoid sharing sensitive information or using the app on public Wi-Fi.

## Features

- **Direct Sharing**: Start a local server to share files instantly with PCs, tablets, or other phones.
- **Transfer Modes**:
  - **Download**: Save individual files or multiple files as a dynamically generated ZIP archive.
  - **Streaming**: Stream video and audio files directly in the browser with seeking support (Partial Content).
- **Security & Access Control**:
  - **Tokens**: Protect access with auto-generated or custom security tokens.
  - **Permissions**: Every new connection must be manually approved via system notifications.
  - **IP Management**: Easily whitelist trusted devices or block specific IP addresses.
- **Modern UI**:
  - Built with **Jetpack Compose** and **Material 3**.
  - **Navigation Drawer** for quick access to logs and settings.
  - **QR Codes**: Instant connection by scanning a code.
  - **Dark Mode**: Full support for system themes.
- **Monitoring**: Built-in log viewer to track server activity in real-time.

## How to Use

1. **Select Files**: Use the "Add Files" button or share files from other Android apps into SendFile.
2. **Setup**: Choose the network interface (IP) and configure a token if needed.
3. **Start**: Tap "Run Server". A foreground service will keep the server active.
4. **Connect**: Other devices can open the displayed URL or scan the QR code.
5. **Authorize**: Tap "Allow" on the system notification when a device attempts to connect.

## Tech Stack

- **UI**: Jetpack Compose & Material 3
- **Server**: [Ktor](https://ktor.io/) (Netty engine)
- **Navigation**: Compose Navigation
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/) (SVG support via Papirus icons)
- **Utilities**: ZXing (QR Codes), Coroutines & Flow

## Build & Installation

1. Clone the repository.
2. Open the project in **Android Studio**.
3. **Requirement**: Python 3 must be installed on your system (a build task uses it to map file icons).
4. Build and deploy to your device.

## License & Credits

- **License**: This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.
- **Icons**: This app uses icons from the **Papirus Icon Theme**, licensed under **GPL-3.0**.
  - Authors: Papirus Development Team
  - Source: [GitHub](https://github.com/PapirusDevelopmentTeam/papirus-icon-theme)
