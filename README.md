# VideoPlayer

一个基于 Jetpack Compose 的现代 Android 视频播放器。

语言：简体中文 · [English](#english)

## 预览

| 暗色 | 亮色 |
|------|------|
| 预览截图 | 预览截图 |

## 安装

请到发行版页面下载对应的安装包：[Release page](https://github.com/MyTheKing/videoPlayer/releases)

| 版本 | 特征 | 链接 |
|------|------|------|
| Stable | 正式版，高可靠性，适合日常使用。 | [Release](https://github.com/MyTheKing/videoPlayer/releases) |
| Direct Download | 直接下载最新版本。 | [下载 APK](https://www.joshu.xin/download/VideoPlayer_v1.0.01.apk) |

支持 Android 7.0 (API 24) 及以上版本。

## Features

- 基于 Kotlin 和 Jetpack Compose 构建
- 使用 Media3 ExoPlayer 作为播放引擎
- 支持本地视频文件播放
- Material Design 3 风格界面
- 支持视频文件管理和浏览
- 支持多种视频格式
- 简洁美观的用户界面

## 开发

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 11
- Android SDK 35

### 构建

```bash
# 克隆项目
git clone https://github.com/MyTheKing/videoPlayer.git

# 打开项目
# 使用 Android Studio 打开项目目录

# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease
```

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **播放器**: Media3 ExoPlayer
- **架构**: MVVM
- **数据库**: Room
- **导航**: Navigation Compose
- **最低 SDK**: API 24 (Android 7.0)
- **目标 SDK**: API 36

## 贡献

欢迎提交 Issue 和 PR！

## 许可证

MIT License. 详见 [LICENSE](LICENSE)。

---

## English

A modern Android video player based on Jetpack Compose.

### Installation

Please go to the Release page to download the corresponding installation package: [Release page](https://github.com/MyTheKing/videoPlayer/releases)

| Version | Feature | Link |
|---------|---------|------|
| Stable | Official version, high reliability, suitable for daily use. | [Release](https://github.com/MyTheKing/videoPlayer/releases) |
| Direct Download | Download the latest version directly. | [Download APK](https://www.joshu.xin/download/VideoPlayer_v1.0.01.apk) |

Supports Android 7.0 (API 24) and above.

### Features

- Built with Kotlin and Jetpack Compose
- Uses Media3 ExoPlayer as playback engine
- Local video file playback support
- Material Design 3 style interface
- Video file management and browsing
- Multiple video format support
- Clean and beautiful user interface

### Development

#### Requirements

- Android Studio Hedgehog (2023.1.1) or higher
- JDK 11
- Android SDK 35

#### Build

```bash
# Clone the project
git clone https://github.com/MyTheKing/videoPlayer.git

# Open project
# Open project directory with Android Studio

# Build Debug version
./gradlew assembleDebug

# Build Release version
./gradlew assembleRelease
```

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Player**: Media3 ExoPlayer
- **Architecture**: MVVM
- **Database**: Room
- **Navigation**: Navigation Compose
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 36

### Contributing

Issue and PR welcome!

### License

MIT License. See [LICENSE](LICENSE) for details.
