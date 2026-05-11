# Story 1.6: 权限不足/路径失效/扫描失败提示与重试

Status: done

## Story

As a 用户,
I want 在权限不足、路径失效或扫描失败时看到可理解的提示，并在可行时能重试或重新扫描,
So that 我知道出了什么问题并有机会自行恢复。

## Acceptance Criteria

1. **Given** 出现权限不足、路径失效或扫描失败等情况  
   **When** 系统检测到上述情况  
   **Then** 在 UI 展示可理解的文案（非原始异常堆栈）  
   **And** 在可行时提供「重试」或「重新扫描」等入口（FR39）

## Tasks / Subtasks

- [x] Task 1：权限不足提示与重试 (AC: #1)
  - [x] 沿用 PermissionRationaleContent：文案「需要访问本地视频」「为了扫描和播放设备中的视频，请允许访问视频文件」，入口「允许」「去设置」
- [x] Task 2：路径失效提示与重试 (AC: #1)
  - [x] PlayerContent 监听 Player.Listener.onPlayerError，置 playbackError；展示「视频无法播放，可能已被移动或删除」+ 按钮「重新选择」，点击调用 onPlaybackError；PickAndPlayContent 传 onPlaybackError = { playUri = null } 以回到选文件
- [ ] Task 3：扫描失败提示与重试 (AC: #1)
  - 留待 Epic 2 有扫描功能后，在扫描失败时复用可理解文案与「重新扫描」入口

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`。Epic 1 当前无扫描，仅「选一个文件播放」；扫描失败在 2.x 实现时按 1.6 模式补上。

## Dev Agent Record

### Completion Notes List

- 权限不足：已有 PermissionRationaleContent + 文案与「允许」「去设置」，满足 AC。
- 路径失效：PlayerContent(playUri, onPlaybackError, modifier)；Player.Listener.onPlayerError 时 playbackError=true；overlay 展示 error_playback_failed + 按钮 retry_pick_video 调 onPlaybackError；MainActivity PickAndPlayContent 传 onPlaybackError = { playUri = null }。

### File List

- `app/src/main/java/com/example/videoplayer/player/ui/PlayerContent.kt`（onPlaybackError、onPlayerError、错误 overlay）
- `app/src/main/java/com/example/videoplayer/MainActivity.kt`（PlayerContent onPlaybackError）
- `app/src/main/res/values/strings.xml`（error_playback_failed、retry_pick_video）
