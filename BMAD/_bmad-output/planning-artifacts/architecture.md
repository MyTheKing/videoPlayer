---
stepsCompleted: [1, 2, 3, 4, 5, 6, 7, 8]
inputDocuments: ['prd.md']
workflowType: 'architecture'
lastStep: 8
status: 'complete'
completedAt: '2026-01-27'
project_name: 'BMAD'
user_name: 'cursorAgent'
date: '2026-01-27'
---

# Architecture Decision Document

_This document builds collaboratively through step-by-step discovery. Sections are appended as we work through each architectural decision together._

---

## Project Context Analysis

### Requirements Overview

**Functional Requirements:**
- 共 39 个功能需求（FR），覆盖 9 个能力域：本地视频与扫描、播放列表与歌单、播放控制与息屏、收藏与分类、搜索与导航、看与听、歌词、基础信息与展示、数据持久化与存储、权限与错误
- 核心能力：本地视频扫描、歌单管理、息屏播放、通知栏控制、多标签分类、搜索、歌词展示

**Non-Functional Requirements:**
- **Performance**：扫描≤5000 个视频≤30s，播放启动≤3s，切歌/通知栏≤1s
- **Reliability**：后台/息屏播放稳定，进程恢复后数据不丢失
- **Security & Privacy**：当前自用，数据仅本地；后续服务端/上架时再补合规

**Scale & Complexity:**
- **复杂度**：低（个人项目、纯本地、无服务端）
- **技术域**：移动端（Android 原生）
- **架构组件预估**：播放引擎、媒体会话、本地存储、UI 层、数据层（约 5-7 个主要模块）

### Technical Constraints & Dependencies

- **平台**：Android 6.0+（按 ExoPlayer/MediaSession 要求）
- **播放引擎**：优先使用 ExoPlayer + MediaSession
- **存储**：Room（播放列表、收藏、标签、历史）
- **UI**：Material Design
- **组件策略**：优先现成库，仅在缺陷时自研
- **架构可扩展**：数据层抽象预留服务端对接

### Cross-Cutting Concerns Identified

- **后台播放**：MediaSession + 前台服务，保证息屏/切应用时播放
- **数据持久化**：Room 数据库设计需支持后续服务端对接
- **权限管理**：存储/媒体权限的申请与引导
- **错误处理**：路径失效、权限回收、扫描失败的恢复机制
- **性能优化**：大量视频扫描与列表展示的性能

---

## Starter Template Evaluation

### Primary Technology Domain

Android 原生（Kotlin），基于 PRD 与 Project Context 分析；项目形态为本地视频播放器 APP，无服务端。

### Starter Options Considered

- **Web/跨端 starter**（如 React Native、Exo、Flutter）：不采用；需求为原生 Android、MediaSession/ExoPlayer 生态。
- **Android 官方模板**：采用「手动创建的 Kotlin 项目」作为基线，路径 `Z:\FE\Android\KotlinDemo\`；未选用第三方 Android boilerplate，以便完全按 PRD 技术栈堆叠。

### Selected Starter: 现有 Kotlin 项目（KotlinDemo）

**选择理由：**
- 已存在手动创建的 Kotlin + Gradle(KTS) 项目，包名 `com.example.videoplayer`，含 MainActivity、主题与基础 res。
- 在此基线上增加 Media3 ExoPlayer、MediaSession、Room、Material 等依赖即可满足 MVP，无需引入额外「starter」脚手架。

**项目基线路径：** `Z:\FE\Android\KotlinDemo\`

**由本选型确立的技术决策：**
- **语言与运行时**：Kotlin 1.x，JVM/Android Runtime，Gradle Kotlin DSL
- **播放**：AndroidX Media3 ExoPlayer + MediaSession
- **存储**：Room
- **UI**：Material Design（沿用现有 theme/Color/Type 结构）
- **构建与工程**：现有 `build.gradle.kts`、`libs.versions.toml`，在其上追加上述依赖

**说明：** 首条实现类故事应为「在 KotlinDemo 项目中完成依赖配置与最小可运行播放链路」。

---

## Core Architectural Decisions

### Decision Priority Analysis

**Critical Decisions (Block Implementation):**
- 数据层：Room 版本与 DAO/Entity 设计；播放层：Media3 ExoPlayer + MediaSession 集成方式；UI：Compose 已由 Starter 确定，状态与导航方式需定。
- 权限与前台服务：存储/媒体权限、前台服务类型（媒体播放）为启动与扫描/播放的前置条件。

**Important Decisions (Shape Architecture):**
- 应用内分层：UI → ViewModel → Repository/UseCase → Room / MediaSession；扫描与播放分别抽象便于测试与扩展。
- 通知栏与 MediaSession 的 ControlDispatcher、MediaController 与 UI 的同步方式。

**Deferred Decisions (Post-MVP):**
- CI/CD、上架渠道、合规与隐私文案；服务端对接时的鉴权与同步协议。

### Data Architecture

- **数据库**：Room（AndroidX）；版本随 `androidx.room:room-*` 当前稳定版，KTS 依赖在 KotlinDemo 的 `libs.versions.toml` 中统一管理。
- **建模**：实体包含播放列表(Playlist)、列表项(PlaylistItem)、视频元数据(VideoMedia)、收藏/标签(Favorite/Tag)、播放历史(PlayHistory)；关系与索引按「歌单↔视频、标签↔视频、历史」查询需求设计。
- **迁移**：Room Migration 内置于代码库，随 schema 变更增补；导出 schema 到 `schemas/` 以便版本追踪。
- **缓存**：内存中仅保留「当前播放列表/队列、当前媒体元数据」；其余均经 Repository 从 Room 读，必要时用 Kotlin Flow 做 reactive 列表与详情。
- **校验**：写入前在 Repository 或 UseCase 做路径存在性、必填字段非空等校验；不引入独立校验框架，MVP 内手写即可。

### Authentication & Security

- **身份认证**：MVP 无登录、无账号；设备本地单用户使用。
- **权限**：存储/媒体权限（READ_MEDIA_VIDEO、READ_EXTERNAL_STORAGE 等按 targetSdk 选用）；首次进入或扫描前请求，拒绝时予引导文案与入口。
- **数据**：全部落本地（Room + 应用私有目录）；不做备份/云同步时无额外加密策略；若后续做导出/同步再考虑字段级或库级加密。
- **沟通安全**：无 API、无网络请求，此项不适用。

### API & Communication Patterns

- **对外 API**：无；纯本地应用，无 REST/GraphQL 等。
- **应用内通信**：
  - UI ↔ 逻辑：Compose + ViewModel；ViewModel 暴露 StateFlow/Flow，UI 用 collectAsState/collectAsStateWithLifecycle 消费。
  - ViewModel 调用 Repository 或 UseCase，不直接持有一个以上的数据源句柄。
  - 播放控制：通过 MediaController 连接 MediaSession，UI/通知栏/锁屏共用同一 MediaSession；ControlDispatcher 由应用注入，便于「切歌/循环/随机」等逻辑统一。
- **错误与边界**：Repository/UseCase 返回 Result/ sealed class 或抛受检异常，由 ViewModel 转成 UI 状态（加载中/成功/错误文案）；扫描失败、权限被回收、路径失效等均在 UI 有明确提示与可操作入口。

### Frontend Architecture

- **技术栈**：Jetpack Compose + Material3（由 KotlinDemo Starter 确定）；主题沿用现有 `Color.kt` / `Theme.kt` / `Type.kt`。
- **状态**：ViewModel + StateFlow/Flow；单屏状态尽量单一 State 数据类，避免零散 LiveData/多 StateFlow 混用。
- **导航**：Navigation Compose；路由以 sealed class 或 String 常量集中定义；深度链接与「通知栏进详情」预留路由与参数。
- **结构**：按功能切包（如 `player`、`playlist`、`library`、`settings`）；每功能内 `ui/`（Compose 与 ViewModel）、必要时 `domain/`（UseCase）。列表用 LazyColumn；大列表考虑 Paging3 若后续数据量上来再引入。
- **性能**：尽量在 ViewModel/Repository 层做 IO 与计算，Compose 内不做重逻辑；图片/封面用 Coil 等库异步加载，不阻塞主线程。

### Infrastructure & Deployment

- **托管与运行时**：仅本机构建与安装；无云托管、无服务端。
- **构建**：Gradle KTS；debug / release 通过 buildTypes 区分；ProGuard 在 release 中已开启，Keep 规则随 Media3/Room 等文档补全。
- **环境**：无需多环境；若后续加「内测/正式」再考虑 buildConfig 或 flavor。
- **CI/CD**：MVP 不要求；本地 `./gradlew assembleDebug` 或 Android Studio 运行即可。
- **监控与日志**：开发期可用 Log/ Timber；不上报远端；若后期加崩溃收集再选型。

### Decision Impact Analysis

**Implementation Sequence:**
1. 在 KotlinDemo 中加入 Media3、Room、Navigation、Coil 等依赖与版本约束。
2. 定义 Room schema（Entity/DAO）、做最小迁移与 Repository 封装。
3. 实现 MediaSession + ExoPlayer 的播放服务与 MediaController 连接，再实现「单视频播放 + 通知栏控制」。
4. 实现扫描与本地视频列表、再接入歌单 CRUD 与「从列表加入播放队列」。
5. 用 Compose 做主流程界面（库/歌单/播放/设置等）并接上 ViewModel 与导航。
6. 权限申请与失败提示、播放生命周期与进程恢复后的状态恢复。

**Cross-Component Dependencies:**
- 播放服务依赖 Room（当前播放列表/队列来源）与 MediaSession；UI 与通知栏均通过 MediaController 与控制逻辑交互，不直接依赖 ExoPlayer 实例。
- 扫描与写入视频元数据需存储权限与 Room；歌单/收藏/标签依赖 Room 与统一 Repository 抽象，便于后续扩展「服务端同步」时替换或包装数据源。

---

## Implementation Patterns & Consistency Rules

### Pattern Categories Defined

**Critical Conflict Points Identified:** 命名、包结构、状态/错误形态、播放与 UI 的边界等，多 Agent 实现时易产生不一致。

### Naming Patterns

**Database (Room) 命名：**
- 表名：snake_case、复数表示集合，如 `playlists`、`playlist_items`、`video_media`、`play_history`。
- 列名：snake_case，如 `created_at`、`video_id`、`playlist_id`；主键统一 `id`（或 `_id` 若与业务 id 区分）。
- 索引名：`idx_<table>_<columns>`，如 `idx_playlist_items_playlist_id`。
- 外键列名与关联表列一致，如 `playlist_id` 指向 `playlists.id`。

**Kotlin/Compose 命名：**
- 文件名与顶层类/对象一致，PascalCase：`PlaylistViewModel.kt`、`VideoCard.kt`。
- 包名全小写、按功能与层级：`com.example.videoplayer.player.ui`、`com.example.videoplayer.library.domain`。
- 函数/变量/属性：camelCase；常量为 `UPPER_SNAKE` 或 `object { const val X }`。
- State 数据类：以 `UiState` 或 `State` 结尾，如 `PlaylistUiState`、`PlayerState`；事件/意图以 `Intent` 或 `Event` 结尾并统一选一种，如 `PlaylistIntent`。

**无对外 API：** 本应用无 REST/GraphQL，故无 API 路径、请求体命名规范；若后续加内部 Deep Link，路径格式在导航一节统一。

### Structure Patterns

**工程与包组织：**
- 功能按包划分：`player`、`playlist`、`library`、`settings` 等；每功能下 `ui/`（Compose 与 ViewModel）、可选 `domain/`（UseCase）。
- 共享类型与工具放在公共包，如 `data`（Entity、DTO、Repository 接口）、`di`（若用 Hilt）、`util`（不散落各处）。
- 测试：单元测试于 `src/test/` 下与 main 同包或 `*.test` 包；仪器测试于 `src/androidTest/`，路径与 main 对应。

**文件与资源：**
- 各模块 `build.gradle.kts` 仅声明本模块依赖；版本与共用在根 `libs.versions.toml` 统一。
- 资源：`res/values/`、`res/drawable/` 等沿用 Android 惯例；若 Compose 只用 Material 与 programmatic 资源，可最小化 res。
- 导航路由定义集中在一处（sealed class 或路由常量），便于多 Agent 复用同一路由集。

### Format Patterns

**应用内数据与状态：**
- UI 状态：不可变 `data class`，含 `isLoading`、`error: String?`（或 `sealed class Error`）、业务数据；命名统一含 `UiState`/`State`。
- 错误表达：在 Repository/UseCase 层用 `Result<T>` 或 `sealed class Outcome`；ViewModel 转成 `error: String?` 或 `UiState` 的 error 分支，不在 UI 层直接抛异常。
- 时间：持久化与内存统一用 UTC epochMillis 或 ISO-8601 字符串；展示层在 UI 或 ViewModel 中格式化为本地文案。

**无 API 响应格式：** 无服务端；若有后续导出/同步，再单独约定 JSON 字段命名（建议 camelCase 与 Kotlin 一致）。

### Communication Patterns

**状态与事件：**
- ViewModel 对外只暴露 `StateFlow<UiState>` 与「一次性事件」流（如 `SharedFlow<Event>` 或 `Channel`）；不在 State 里混入「仅用一次的 toasts/导航」。
- 用户意图：以函数入参或密封类表示，如 `sealed class PlaylistIntent { object Refresh : PlaylistIntent(); data class Remove(val id: Long) : PlaylistIntent() }`；命名统一用 `Intent` 或 `Event` 其一，全工程一致。
- 播放状态与队列：仅通过 MediaController + MediaSession 与「当前队列/当前索引」的 StateFlow 同步；不在各界面各自维护一份「播放中」可变的副本。

**日志：** 使用同一 TAG 命名方式，如 `"VideoPlayer/${class.simpleName}"`；级别：开发 debug、异常 error；不写敏感路径或用户内容。

### Process Patterns

**加载与错误：**
- 各屏 loading：在对应 `UiState` 中 `isLoading: Boolean`（或 `isRefreshing`）；不在全局维持单一 loading 状态。
- 错误：Repository/UseCase 返回 `Result`/sealed；ViewModel 映射为 `error: String?` 或 `UiState.Error(msg)`；UI 仅展示文案与重试/关闭入口，不负责「是否重试」的业务判断，该逻辑在 ViewModel。
- 权限被拒、路径失效、扫描失败：均在各自用例中转化为明确错误类型与文案，由 UI 统一展示风格（如 Snackbar 或内联提示）。

**播放相关：**
- 切歌/循环/随机等逻辑只写在 MediaSession/ControlDispatcher 或单一「播放控制」用例中；UI 与通知栏仅发「下一首/切换循环模式」等指令，不直接改 ExoPlayer 或队列顺序。

### Documentation Patterns（代码文档规范，按 KDoc 写清楚）

本工程使用 **KDoc**（Kotlin 的文档注释，类似 JSDoc）对公开 API 写清用途、参数、返回值与异常；格式为 `/** ... */`，支持 `@param`、`@return`、`@throws`、`@see` 等标签。

**必须写 KDoc 的符号：**
- 所有 **public** 或 **internal** 的类、接口、对象：首行一句话说明职责；若与其它组件强依赖，用 `@see` 标出。
- 所有 **public/internal** 的函数与构造参数：`@param 参数名 说明`；有返回值时 `@return 说明`；会抛异常时 `@throws 异常类型 说明`。
- ViewModel 的 `state`、`dispatch`、UiState 数据类、Intent/Event 密封类：至少一句话说明「在什么场景下用、谁消费」。
- Repository、UseCase、DAO 的对外方法：说明「做什么、入参/出参含义、在何种条件下抛错或返回空」。

**KDoc 写法约定（与 JSDoc 对齐）：**
- 第一段：简要说明，结尾不必加句号。
- 空一行后可选第二段：细节、约束、调用方注意点。
- 使用标准标签：`@param name 说明`、`@return 说明`、`@throws X 说明`、`@see Class` 或 `@see 方法`。
- 示例（保持简洁即可，不必每处都写）：
  - 类：`/** 某屏的 UI 状态，由 [XxxViewModel] 暴露，供 [XxxScreen] 消费。 */`
  - 方法：`/** 根据歌单 id 拉取列表；id 不存在时返回空列表。 @param playlistId 歌单主键 @return 该歌单下的条目流，按顺序 */`

**豁免：** 仅用于 UI 的私有 Composable、私有扩展、单元测试内部函数，可只写简短单行说明或省略，但对外暴露的 API 必须按上表写全。

### Enforcement Guidelines

**All AI Agents MUST:**
- 新增或修改 **对外暴露** 的类、接口、函数时，补全 KDoc（用途、`@param`/`@return`/`@throws` 等），写清楚「做什么、谁用、在什么条件下会怎样」，风格与上文约定一致。
- 新增 Entity/DAO 时使用上述 Room 表名列名约定，并更新或新增 Migration、导出 schema。
- 新增界面或 ViewModel 时，State 命名为 `XxxUiState`，一次性事件与意图统一用 `XxxIntent` 或 `XxxEvent` 且全工程一致。
- 播放控制与队列变更只通过 MediaController/MediaSession 或已约定的 UseCase，不新增绕过 ControlDispatcher 的调用路径。

**Pattern Enforcement:** 在 code review 或实现故事完成前，对照本小节核对命名与分层；若发现冲突，以本文档为准并补充到「Anti-Patterns」示例中。

### Pattern Examples

**Good Examples:**
- Room: `@Entity(tableName = "playlist_items")`，列 `playlist_id`、`video_id`、`created_at`；DAO 方法名 `getItemsByPlaylistId(playlistId: Long): Flow<List<PlaylistItem>>`。
- ViewModel: `data class PlaylistUiState(val items: List<Item>, val isLoading: Boolean, val error: String?)`；`fun dispatch(intent: PlaylistIntent)`；对外 `val state: StateFlow<PlaylistUiState>`。
- 播放：`MediaControllerCompat.getMediaController(activity)` 调 `transportControls.next()`，不直接拿 `ExoPlayer` 引用在 UI 层调 `seekTo`/`next`。
- KDoc：公开方法带完整 `@param`/`@return`/`@throws`，类首行一句话说明职责；例：`/** 按歌单 id 拉取条目流。 @param playlistId 歌单主键 @return 该歌单下的条目，按顺序 */`

**Anti-Patterns:**
- 表名或列名用 camelCase 或 PascalCase（如 `playListId`），与 Room 惯例不符且易与 Kotlin 属性混淆。
- 在 Composable 或 Activity 内直接持有 `ExoPlayer` 或 `MediaSession` 并调 control 方法，导致与通知栏/锁屏控制不同步。
- 多屏各自维护 `currentPlayingId` 等可变状态，而未通过单一 MediaController/StateFlow 消费。
- 对外暴露的类、接口、函数无 KDoc 或仅有空注释，导致「做什么、谁用、何时出错」不清楚。

---

## Project Structure & Boundaries

### 当前 KotlinDemo 需你手动补充的依赖/组件

你现有项目已有：Compose、Material3、Activity Compose、lifecycle-runtime-ktx、core-ktx、基础测试依赖。

**还未有的、需要你在 `libs.versions.toml` 和 `app/build.gradle.kts` 里自己加的：**

| 用途 | 依赖（在 version catalog 里加好后再在 app 里 implementation） |
|------|----------------------------------------------------------------|
| 播放 | **Media3**：`androidx.media3:media3-exoplayer`、`media3-session`、`media3-ui`（版本用当前稳定，如 1.x） |
| 数据库 | **Room**：`androidx.room:room-runtime`、`room-ktx`、`room-compiler`；并加 **KSP** 插件：`com.google.devtools.ksp`，给 Room 做注解处理 |
| 导航 | **Navigation Compose**：`androidx.navigation:navigation-compose` |
| ViewModel | **ViewModel Compose**：`androidx.lifecycle:lifecycle-viewmodel-compose` |
| 图片/封面 | **Coil**：`io.coil-kt:coil-compose`（按需） |
| 协程 | 通常由上述库带出；若需显式：`org.jetbrains.kotlinx:kotlinx-coroutines-android` |

**工程配置需你核对/修正的：**
- `app/build.gradle.kts` 里 `compileSdk`：目前若写成 `compileSdk { version = release(36) }` 之类异常，改为常规写法如 `compileSdk = 34` 或 35（与本地 SDK 一致）。
- 使用 Room 时在 `app/build.gradle.kts` 的 `plugins` 里加 `id("com.google.devtools.ksp") version "x.y.z"`（与 Kotlin 版本匹配），并在 `dependencies` 里加 `ksp(libs.androidx.room.compiler)`（或你 catalog 里对应的 room-compiler）。

加完上述依赖并 sync 通过后，即可按下面目录结构开始实现。

### Complete Project Directory Structure

```
KotlinDemo/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/videoplayer/
│       │   │   ├── MainActivity.kt
│       │   │   ├── di/                    # 若用 Hilt，放 AppModule 等
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── dao/            # PlaylistDao, PlaylistItemDao, VideoMediaDao, PlayHistoryDao, TagDao...
│       │   │   │   │   ├── entity/         # Playlist, PlaylistItem, VideoMedia, PlayHistory, Tag...
│       │   │   │   │   ├── Migration.kt    # Room migrations
│       │   │   │   │   └── AppDatabase.kt
│       │   │   │   └── repository/         # PlaylistRepository, VideoRepository, PlayHistoryRepository...
│       │   │   ├── player/
│       │   │   │   ├── domain/             # 可选：PlaybackUseCase, QueueUseCase
│       │   │   │   ├── service/            # MediaPlaybackService, MediaSessionCallback
│       │   │   │   └── ui/                 # PlayerScreen, PlayerViewModel, PlayerIntent
│       │   │   ├── playlist/
│       │   │   │   ├── ui/                 # PlaylistListScreen, PlaylistDetailScreen, ViewModels, Intent
│       │   │   │   └── domain/             # 可选：AddToPlaylistUseCase
│       │   │   ├── library/
│       │   │   │   ├── ui/                 # LibraryScreen, VideoListScreen, ViewModels
│       │   │   │   └── domain/             # 可选：ScanVideosUseCase
│       │   │   ├── settings/
│       │   │   │   └── ui/                 # SettingsScreen, ViewModel
│       │   │   ├── navigation/             # NavGraph, Routes (sealed or constants)
│       │   │   ├── ui/theme/               # Color, Theme, Type（已有）
│       │   │   └── util/                   # 通用扩展、TAG 常量等
│       │   └── res/
│       │       ├── drawable/
│       │       ├── values/
│       │       └── xml/
│       ├── test/                           # 单元测试，与 main 同包或 *.test
│       └── androidTest/                    # 仪器测试
├── gradle/libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── ...
```

（根目录下 `build.gradle.kts`、`settings.gradle.kts`、`gradlew` 等保持现有即可。）

### Architectural Boundaries

**API Boundaries:** 无对外 API；纯本地应用。

**Component Boundaries:**
- UI 仅通过 ViewModel 的 StateFlow/Intent 与业务交互；不直接访问 Room 或 MediaSession。
- 播放控制统一经 MediaController + MediaSession；播放服务在 `player/service`，UI 在 `player/ui` 及其他功能包的 ui 中消费 MediaController/State。

**Data Boundaries:**
- 所有持久化经 Repository → DAO → Room；扫描结果写入 `video_media` 等表后再经 Repository 暴露 Flow。
- 当前播放队列与进度由 MediaSession/MediaController 暴露，UI 只读不写队列顺序。

### Requirements to Structure Mapping

| 能力域（PRD/FR） | 主要落点 |
|------------------|----------|
| 本地视频与扫描 | `library/` + `data/local/entity/VideoMedia`、`data/repository/`、扫描用例 |
| 播放列表与歌单 | `playlist/` + `data/local/entity/Playlist, PlaylistItem`、PlaylistRepository |
| 播放控制与息屏 | `player/service/`、`player/ui/`、MediaSession + 前台服务 |
| 收藏与分类、标签 | `data/local/entity/`（Favorite/Tag）、对应 Repository、`library/` 或 `playlist/` 内 UI |
| 搜索与导航 | `library/ui/` 搜索栏、Navigation Compose 在 `navigation/` |
| 歌词、看与听、基础信息 | `player/ui/`、`library/ui/` 详情与卡片；歌词路径存实体或表 |
| 数据持久化 | `data/local/`、所有 Repository |
| 权限与错误 | 各屏 ViewModel 中 error 态、权限请求在入口或 library 首屏 |

### Integration Points

- **内部通信**：Compose 调 ViewModel.state / ViewModel.dispatch；ViewModel 调 Repository/UseCase；播放服务通过 MediaSession 与系统/通知栏/锁屏交互。
- **外部**：仅系统（MediaSession、ContentResolver 扫描媒体）、无第三方服务。
- **数据流**：用户操作 → Intent → ViewModel → Repository/UseCase → Room 或 MediaController；Room/MediaController 变更 → Flow/回调 → ViewModel → UiState → UI。

---

## Architecture Validation Results

### Coherence Validation ✅

**Decision Compatibility:**
- Kotlin + Compose + Material3（Starter）+ Media3 ExoPlayer + MediaSession + Room + Navigation 无冲突；版本均由 `libs.versions.toml` 统一管理，技术栈一致。
- 无登录、无 API、纯本地与「数据仅本地、权限仅存储/媒体」等决策一致；播放控制统一经 MediaController 与「不直接持 ExoPlayer」一致。

**Pattern Consistency:**
- 命名：Room snake_case、Kotlin camelCase/PascalCase；State/Intent 后缀与 Implementation Patterns 一致；KDoc 要求覆盖对外 API，与「写清楚用途与参数」一致。
- 结构：按功能包（player/playlist/library/settings）+ data/ 分层，与 Core Decisions 中「UI → ViewModel → Repository/UseCase → Room/MediaSession」一致。
- 通信：仅 StateFlow/Intent + MediaController，无额外事件总线，与文档约定一致。

**Structure Alignment:**
- 目录树中的 `data/local/`、`player/`、`playlist/`、`library/`、`navigation/` 等与「Requirements to Structure Mapping」及边界定义一致；集成点（ViewModel↔Repository、播放↔MediaSession）在文档中有明确边界。

### Requirements Coverage Validation ✅

**Functional Requirements Coverage:**
- 9 个能力域、39 个 FR 均在「Project Context」「Core Architectural Decisions」「Project Structure & Boundaries」及「Requirements to Structure Mapping」中有对应落点（data 实体、Repository、player/playlist/library 的 ui/service、权限与错误态）。
- 本地视频扫描、歌单、息屏播放、通知栏控制、多标签、搜索、歌词等均有组件归属与数据流说明。

**Non-Functional Requirements Coverage:**
- **Performance**：扫描/启动/切歌指标在 Context 与 Decisions 中已作为约束；实现时需在 Repository/扫描用例与 MediaController 侧落实（文档已指明职责边界）。
- **Reliability**：后台播放、MediaSession、Room 持久化与迁移策略已在 Decisions 与 Patterns 中覆盖。
- **Security & Privacy**：当前自用、数据仅本地、权限类型已在 Decisions 中明确；上架/合规推迟到后续。

**Cross-Cutting:** 权限、错误处理、加载态在 Process Patterns 与 UiState 约定中已覆盖；KDoc 要求支持多 Agent 协作时的理解一致。

### Implementation Readiness Validation ✅

**Decision Completeness:**
- 数据、安全、API/通信、前端、基础设施均有用到即写明的决策；技术选型与版本策略（toml 统一）已给出。
- 实现顺序（依赖 → Room → 播放服务 → 扫描与歌单 → Compose 主流程 → 权限与恢复）已列出。

**Structure Completeness:**
- 完整目录树已给出；`data/local/entity|dao|repository`、`player|playlist|library|settings` 的 ui/service/domain、`navigation/`、`util/` 等已定义；当前 KotlinDemo 需补依赖表已列出。

**Pattern Completeness:**
- 命名、结构、格式、通信、过程、文档（KDoc）均有约定；Enforcement 与 Good/Anti-Patterns 示例已写；潜在冲突点（多 Agent 命名、播放边界、State/Intent）已约束。

### Gap Analysis Results

- **Critical Gaps:** 无；依赖补充清单与 `compileSdk` 等已在「当前 KotlinDemo 需你手动补充的依赖/组件」中说明，补齐即可开工。
- **Important Gaps:** Media3/Room/Navigation 等在 `libs.versions.toml` 中的具体版本号需在首次实现时查当前稳定版并写入；schema 导出路径（如 `schemas/`）在首次建库时落实即可。
- **Nice-to-Have:** Paging3、Hilt、Timber 等可在实现中按需引入；文档已预留扩展点（如数据层抽象便于服务端对接）。

### Validation Issues Addressed

- 无需修正的硬性冲突；仅提醒：实现时严格按「播放控制只经 MediaController/MediaSession」「对外 API 写全 KDoc」执行，以避免与 Patterns 不一致。

### Architecture Completeness Checklist

**✅ Requirements Analysis**
- [x] Project context thoroughly analyzed
- [x] Scale and complexity assessed
- [x] Technical constraints identified
- [x] Cross-cutting concerns mapped

**✅ Architectural Decisions**
- [x] Critical decisions documented with versions
- [x] Technology stack fully specified
- [x] Integration patterns defined
- [x] Performance considerations addressed

**✅ Implementation Patterns**
- [x] Naming conventions established
- [x] Structure patterns defined
- [x] Communication patterns specified
- [x] Process patterns documented
- [x] Documentation (KDoc) rules specified

**✅ Project Structure**
- [x] Complete directory structure defined
- [x] Component boundaries established
- [x] Integration points mapped
- [x] Requirements to structure mapping complete

### Architecture Readiness Assessment

**Overall Status:** READY FOR IMPLEMENTATION

**Confidence Level:** high；需求与决策、模式、结构、校验均已对齐，缺项仅为「版本号具体值」与「首次建库时的 schema 路径」，可在实现第一步补齐。

**Key Strengths:**
- 技术栈单一、无服务端，决策与边界清晰；播放与数据分层明确，便于多 Agent 分模块实现。
- 命名、KDoc、State/Intent、播放边界等均有可执行约定与示例，冲突点已显式约束。

**Areas for Future Enhancement:**
- 上架时的合规与隐私文案；若引入服务端，再补鉴权与同步协议；可选的 Paging3、依赖注入、崩溃收集等。

### Implementation Handoff

**AI Agent Guidelines:**
- 严格按本架构文档中的决策、模式与目录边界实现；所有对外 API 写清 KDoc（用途、@param/@return/@throws）。
- 播放控制与队列变更只通过 MediaController/MediaSession 或已约定 UseCase；不新增绕过 ControlDispatcher 的调用。
- 新增 Entity/DAO 时遵守 Room 表名列名约定并维护 Migration 与 schema。

**First Implementation Priority:**
1. 在 KotlinDemo 的 `libs.versions.toml` 与 `app/build.gradle.kts` 中补齐 Media3、Room（含 KSP）、Navigation Compose、ViewModel Compose、Coil 等依赖并 Sync 通过；
2. 在此基础上完成「最小可运行播放链路」：MediaSession + ExoPlayer 前台服务 → 单视频播放 → 通知栏控制。
