# Story 1.2: 存储/媒体权限请求与引导

Status: done

## Story

As a 用户,
I want 在首次进入或需要访问本地视频时被请求存储/媒体权限，拒绝时看到可理解的说明与「去设置」入口,
So that 我能知道为什么需要权限并自行前往设置开启。

## Acceptance Criteria

1. **Given** 应用已安装并首次打开（或尚未授予存储/媒体权限）  
   **When** 进入需访问本地视频的流程（如扫描或选文件）  
   **Then** 系统请求 READ_MEDIA_VIDEO / READ_EXTERNAL_STORAGE 等（按 targetSdk 选用）  
   **And** 若用户拒绝，展示可理解的文案与「去设置」入口，不崩溃

## Tasks / Subtasks

- [x] Task 1：在 AndroidManifest 中声明所需权限 (AC: #1)
  - [x] 按 targetSdk 选用：Android 13+ 用 READ_MEDIA_VIDEO；更早用 READ_EXTERNAL_STORAGE（或兼容声明）
- [x] Task 2：实现权限检查与请求逻辑（ViewModel 或共享 UseCase） (AC: #1)
  - [x] 在进入扫描/选文件等流程前检查权限；未授予时发起请求
  - [x] 逻辑放在 ViewModel，不散落在 UI
- [x] Task 3：用户拒绝时的 UI 处理 (AC: #1)
  - [x] 展示可理解文案（为何需要权限）
  - [x] 提供「去设置」入口，跳转应用设置页
  - [x] 不崩溃，可关闭或重试
- [x] Task 4：与入口/首屏集成 (AC: #1)
  - [x] 在进入需访问本地视频的入口或 library 首屏触发权限请求

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`；当前 targetSdk 36，优先 READ_MEDIA_VIDEO，兼容旧机酌情 READ_EXTERNAL_STORAGE。
- **架构**：权限请求与结果判断在 ViewModel；UI 仅根据 state 展示文案、按钮与「去设置」；错误态按 Process Patterns 映射为 `error: String?` 等。
- **去设置**：使用 `Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)` 等跳转应用详情，用户可手动开启权限。

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#Authentication & Security] — 权限类型、请求时机
- [Source: _bmad-output/planning-artifacts/architecture.md#Process Patterns] — 权限被拒 → 错误类型与文案、UI 展示
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.2] — AC 与上下文

## Dev Agent Record

### Agent Model Used

—

### Debug Log References

—

### Completion Notes List

- ✅ AndroidManifest 声明 READ_MEDIA_VIDEO、READ_EXTERNAL_STORAGE (maxSdkVersion=32)
- ✅ library/ui 下新增 PermissionUiState、PermissionIntent、PermissionEvent、PermissionViewModel；权限判断与事件在 ViewModel，UI 仅消费 state/events
- ✅ MainActivity 首屏：LaunchedEffect 检查权限并 SyncFromCheck；未授予时展示 PermissionRationaleContent（文案 + 允许 + 去设置）；requestPermission/OpenSettings 经 SharedFlow 触发 Launcher 与 ACTION_APPLICATION_DETAILS_SETTINGS
- ✅ targetSdk 36：运行时按 API 33+ 用 READ_MEDIA_VIDEO，否则 READ_EXTERNAL_STORAGE；OnRequestResult 传入 canAskAgain（shouldShowRequestPermissionRationale）以控制是否显示「允许」按钮

### File List

- `app/src/main/AndroidManifest.xml`（新增 uses-permission）
- `app/src/main/java/com/example/videoplayer/library/ui/PermissionUiState.kt`（新建）
- `app/src/main/java/com/example/videoplayer/library/ui/PermissionIntent.kt`（新建）
- `app/src/main/java/com/example/videoplayer/library/ui/PermissionEvent.kt`（新建）
- `app/src/main/java/com/example/videoplayer/library/ui/PermissionViewModel.kt`（新建）
- `app/src/main/java/com/example/videoplayer/library/ui/PermissionRationaleScreen.kt`（新建）
- `app/src/main/java/com/example/videoplayer/MainActivity.kt`（修改：集成权限 Gate、ViewModel、Launcher、events 收集）
- `app/src/main/res/values/strings.xml`（新增 permission_rationale_title/message、permission_request、permission_open_settings）
