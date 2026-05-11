# Story 1.1: KotlinDemo 依赖配置与最小可运行环境

Status: done

## Story

As a 开发者,
I want 在 KotlinDemo 中补齐 Media3、Room(KSP)、Navigation、ViewModel Compose、Coil 等依赖并 Sync 通过,
So that 工程可编译运行，为后续播放与数据层实现打好基础。

## Acceptance Criteria

1. **Given** 已存在 KotlinDemo 项目（`Z:\FE\Android\KotlinDemo\`）  
   **When** 在 `libs.versions.toml` 与 `app/build.gradle.kts` 中按要求添加 Media3（exoplayer/session/ui）、Room（runtime/ktx/compiler + KSP）、Navigation Compose、lifecycle-viewmodel-compose、Coil Compose，并修正 `compileSdk` 等配置  
   **Then** `./gradlew assembleDebug` 或 Android Studio Sync 通过，无编译错误  
   **And** 依赖版本在 version catalog 中统一管理

## Tasks / Subtasks

- [x] Task 1：在 `gradle/libs.versions.toml` 中增加版本与依赖别名 (AC: #1)
  - [x] 增加 media3 版本与 media3-exoplayer、media3-session、media3-ui 的 library 条目
  - [x] 增加 room 版本与 room-runtime、room-ktx、room-compiler 的 library 条目
  - [x] 增加 navigation-compose、lifecycle-viewmodel-compose、coil-compose 的 library 条目（若已有 composeBom，可沿用或单独指定版本）
  - [x] 若使用 KSP，在 [plugins] 或根 build.gradle 中增加 ksp 插件版本（与当前 Kotlin 版本匹配）
- [x] Task 2：在根 `build.gradle.kts` 或 `settings.gradle.kts` 中应用 KSP 插件（若本 story 采用 KSP 处理 Room）(AC: #1)
  - [x] 在需要 Room 的模块的 plugins 中加 `id("com.google.devtools.ksp")`
- [x] Task 3：在 `app/build.gradle.kts` 中引入依赖并修正配置 (AC: #1)
  - [x] implementation(media3-exoplayer/session/ui)、implementation(room-runtime/room-ktx)、ksp(room-compiler)
  - [x] implementation(navigation-compose)、implementation(lifecycle-viewmodel-compose)、implementation(coil-compose)
  - [x] 将 `compileSdk` 修正为常规写法（如 `compileSdk = 34` 或 35），与本地 SDK 一致
- [x] Task 4：执行 Sync / `./gradlew assembleDebug`，确认无编译错误 (AC: #1)

## Dev Notes

- **项目路径**：`Z:\FE\Android\KotlinDemo\`（架构文档中的基线项目）。
- **版本管理**：所有新增依赖的版本号写在 `gradle/libs.versions.toml` 的 `[versions]` 与 `[libraries]` 中，在 app 中通过 `libs.xxx` 引用，不在 `app/build.gradle.kts` 内写死版本。
- **KSP**：Room 的 `room-compiler` 必须通过 `ksp(...)` 引入；KSP 插件版本需与当前 Kotlin 版本兼容，常用为 2.0.x 对应 ksp 2.0.x。
- **Media3**：使用 `androidx.media3:media3-exoplayer`、`media3-session`、`media3-ui`，版本可用 1.x 当前稳定版（建议查一次官方或 Maven 确定）。
- **compileSdk**：若当前为 `compileSdk { version = release(36) }` 等非常规写法，改为 `compileSdk = 34` 或 35，与本地已安装的 Android SDK 一致，避免无法解析。

### Project Structure Notes

- 本 story 仅改 Gradle 与依赖，不新增 Kotlin 源码目录；包结构 `com.example.videoplayer` 与现有 `MainActivity`、`ui/theme` 保持不变。
- 架构约定后续代码落在 `data/`、`player/`、`playlist/`、`library/`、`navigation/` 等包下；本 story 不创建这些目录。

### References

- [Source: _bmad-output/planning-artifacts/architecture.md#当前 KotlinDemo 需你手动补充的依赖/组件] — 依赖清单与 compileSdk 说明
- [Source: _bmad-output/planning-artifacts/architecture.md#Complete Project Directory Structure] — 目标目录结构（本 story 仅做依赖，不建完整目录）
- [Source: _bmad-output/planning-artifacts/epics.md#Story 1.1] — 本 Story 的 AC 与上下文
- [Source: _bmad-output/planning-artifacts/architecture.md#Implementation Patterns & Consistency Rules] — 命名与 KDoc 等约定（本 story 无代码，仅配置）

## Dev Agent Record

### Agent Model Used

—

### Debug Log References

—

### Completion Notes List

- ✅ 在 `gradle/libs.versions.toml` 中添加了 Media3 (1.9.0)、Room (2.8.4)、Navigation Compose (2.8.9)、lifecycle-viewmodel-compose (2.10.0)、Coil (3.3.0)、KSP 的版本与库定义
- ✅ 在 `app/build.gradle.kts` 的 plugins 中添加了 KSP 插件
- ✅ 在 `app/build.gradle.kts` 的 dependencies 中添加了所有必需依赖（Media3 exoplayer/session/ui、Room runtime/ktx/compiler、Navigation Compose、lifecycle-viewmodel-compose、Coil Compose）
- ✅ compileSdk 设为 35（满足依赖 minCompileSdk 要求）；Kotlin 2.2.21、KSP 2.2.21-2.0.4（与 Coil metadata 兼容）；jvmTarget 已迁至 kotlin compilerOptions DSL

### File List

- `gradle/libs.versions.toml`（已修改：添加版本与库定义）
- `app/build.gradle.kts`（已修改：添加 KSP 插件、依赖、修正 compileSdk）
