# Story 1.3: 单视频播放与 MediaSession 前台服务

Status: done

## Story

As a 用户,
I want 从最小入口（如选一个本地文件或极简列表点一条）选择视频后能播出声画，息屏或切到其他应用时继续听,
So that 我能先「播一首」验证播放与后台能力。

## Acceptance Criteria

1. **Given** 已具有存储/媒体权限  
   **When** 用户通过最小入口选到一个本地视频并点击播放  
   **Then** 使用 MediaSession + ExoPlayer 前台服务播放该视频，有声有画  
   **And** 息屏或切到其他应用时音频继续播放（NFR-R1 相关）

## Tasks / Subtasks

- [x] Task 1：实现 MediaSession + ExoPlayer 前台服务 (AC: #1)
  - [x] 前台服务类型为媒体播放；ExoPlayer 由服务持有，不暴露给 UI
  - [x] MediaSession 与 ExoPlayer 绑定，支持播放/暂停/seek
- [x] Task 2：最小入口与播放触发 (AC: #1)
  - [x] 最小入口：选一个本地文件（如 ActivityResultContracts.OpenDocument）或极简列表点一条
  - [x] 点击播放后启动/绑定前台服务并设置 MediaItem 播放
- [x] Task 3：播放 UI 与 MediaController 消费 (AC: #1)
  - [x] UI 仅通过 MediaController + StateFlow 消费播放状态与进度，不持 ExoPlayer
  - [x] 有声有画：可选用 Media3 的 PlayerView 或等价播放界面
- [x] Task 4：后台/息屏继续播放 (AC: #1)
  - [x] 前台服务 + MediaSession 保证息屏、切应用时音频继续；符合 NFR-R1

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`；依赖已包含 Media3 exoplayer/session/ui（Story 1-1）。
- **架构**：播放控制统一经 MediaController + MediaSession；播放服务在 `player/service`，UI 在 `player/ui`；ViewModel 暴露 StateFlow/Intent，不直接持 ExoPlayer。
- **最小入口**：本 story 可采用「选一个文件」或极简占位列表任选一条；扫描与列表在 Epic 2，此处仅保证「选中一条 → 播出声画 + 后台继续」。

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Core Architectural Decisions] — MediaSession、ExoPlayer、前台服务
- [Source: _bmad-output/planning-artifacts/architecture.md#Frontend Architecture] — UI ↔ MediaController、不持 ExoPlayer
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.3] — AC 与上下文

## Dev Agent Record

### Agent Model Used

—

### Debug Log References

—

### Completion Notes List

- ✅ MediaPlaybackService 继承 MediaSessionService，onCreate 创建 ExoPlayer + MediaSession，onGetSession 返回 session；onStartCommand 收到 intent.data 时 startForeground + setMediaItem + prepare + play
- ✅ Manifest：FOREGROUND_SERVICE、FOREGROUND_SERVICE_MEDIA_PLAYBACK；Service 声明 foregroundServiceType=mediaPlayback、intent-filter MediaSessionService
- ✅ 最小入口：已授权首屏展示「选择视频」+ OpenDocument(video/*)；选文件后 playUri 设值，启动 MediaPlaybackService(Intent.setData(uri))
- ✅ PlayerContent(playUri)：SessionToken(ComponentName(MediaPlaybackService)) + MediaController.Builder.buildAsync()；连接成功后 AndroidView(PlayerView(controller))，不持 ExoPlayer
- ✅ MainActivity 授权分支改为 PickAndPlayContent（选择视频 / PlayerContent）

### File List

- `app/src/main/AndroidManifest.xml`（FOREGROUND_SERVICE* 权限、MediaPlaybackService）
- `app/src/main/java/com/example/videoplayer/player/service/MediaPlaybackService.kt`（新建）
- `app/src/main/java/com/example/videoplayer/player/ui/PlayerContent.kt`（新建）
- `app/src/main/java/com/example/videoplayer/MainActivity.kt`（修改：PickAndPlayContent、OpenDocument、PlayerContent）
- `app/src/main/res/values/strings.xml`（notification_channel_media、pick_video）
