# Story 1.4: 通知栏控制（酷狗/QQ 风格）

Status: done

## Story

As a 用户,
I want 播放时在通知栏看到播放控制，并能控制播放/暂停、上一首/下一首,
So that 不需亮屏即可操作播放。

## Acceptance Criteria

1. **Given** 当前正在播放  
   **When** 用户下拉通知栏  
   **Then** 展示酷狗/QQ 风格媒体通知，含封面/标题、播放/暂停、上一首/下一首  
   **And** 操作在 1s 内生效（NFR-P3）

## Tasks / Subtasks

- [x] Task 1：媒体通知样式与 MediaSession 绑定 (AC: #1)
  - [x] 使用 MediaStyle 或 Media3 MediaNotification.Provider，通知含封面/标题
  - [x] 通知与 MediaSession 关联，确保操作路由到 Session
- [x] Task 2：通知栏操作 (AC: #1)
  - [x] 播放/暂停、上一首/下一首按钮，点击经 MediaController/MediaSession 生效
- [x] Task 3：前台服务通知更新 (AC: #1)
  - [x] 播放开始/切换时更新通知；与现有 MediaPlaybackService 集成

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`；MediaPlaybackService 已有前台通知与 MediaSession（Story 1-3）。
- **架构**：控制仅经 MediaController + MediaSession；通知栏操作不直接碰 ExoPlayer。
- **酷狗/QQ 风格**：媒体通知展开含封面、标题、播放/暂停、上一首/下一首；单视频场景下上一首/下一首可占位或跳首/尾。

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#API & Communication Patterns] — 播放控制经 MediaController
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.4] — AC 与上下文

## Dev Agent Record

### Agent Model Used

—

### Debug Log References

—

### Completion Notes List

- ✅ MediaPlaybackService：onCreate 中 setMediaNotificationProvider(DefaultMediaNotificationProvider(this))；ensureNotificationChannel 提前；保留 startForeground(minimal) 再播
- ✅ MediaItem 设置 MediaMetadata 标题：uri.lastPathSegment 或 notification_playing；便于通知展示
- ✅ DefaultMediaNotificationProvider 提供媒体样式通知，含播放/暂停、上一首/下一首，与 MediaSession 绑定，操作经 Session 生效

### File List

- `app/src/main/java/com/example/videoplayer/player/service/MediaPlaybackService.kt`（DefaultMediaNotificationProvider、MediaMetadata 标题）
- `app/src/main/res/values/strings.xml`（notification_playing）
