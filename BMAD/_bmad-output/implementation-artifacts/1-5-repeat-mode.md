# Story 1.5: 单曲/列表/随机循环模式

Status: done

## Story

As a 用户,
I want 能切换单曲循环、列表循环、随机播放模式,
So that 播放顺序符合我的习惯。

## Acceptance Criteria

1. **Given** 当前有播放列表或单条媒体  
   **When** 用户切换循环模式（单曲/列表/随机）  
   **Then** 行为经 MediaController/ControlDispatcher 生效，与架构约定一致  
   **And** 模式在本次会话内保持，直至用户再次切换

## Tasks / Subtasks

- [x] Task 1：循环模式经 MediaController 生效 (AC: #1)
  - [x] PlayerContent 增加列表/单曲/随机三种模式切换 UI（FilterChip）
  - [x] 点击时调用 MediaController.setRepeatMode、setShuffleModeEnabled
- [x] Task 2：模式状态同步 (AC: #1)
  - [x] 使用 Player.Listener 监听 onRepeatModeChanged、onShuffleModeEnabledChanged，UI 高亮当前模式

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`；控制仅经 MediaController，不直接持 ExoPlayer。
- **映射**：列表循环=REPEAT_MODE_ALL+shuffle false；单曲=REPEAT_MODE_ONE+shuffle false；随机=shuffle true+REPEAT_MODE_ALL。

## Dev Agent Record

### Completion Notes List

- PlayerContent：在 PlayerView 下方增加 Row 三枚 FilterChip（列表/单曲/随机），经 MediaController.setRepeatMode/setShuffleModeEnabled 切换；DisposableEffect+Player.Listener 同步 repeatMode/shuffleModeEnabled 到本地 state 以高亮选中项。

### File List

- `app/src/main/java/com/example/videoplayer/player/ui/PlayerContent.kt`（循环模式栏、Listener、setRepeatMode/setShuffleModeEnabled）
- `app/src/main/res/values/strings.xml`（repeat_mode_list、repeat_mode_one、repeat_mode_shuffle）
