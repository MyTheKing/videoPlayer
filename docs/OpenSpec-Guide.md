# OpenSpec 使用指南

OpenSpec 是一个 AI 驱动的规范驱动开发系统，帮助你在 Claude Code 中进行结构化的变更管理。

## 安装

```bash
npm install -g @fission-ai/openspec@latest
```

## 初始化

```bash
openspec init --tools claude
```

初始化后会在项目中创建：
- `.claude/commands/opsx/` — 4 个斜杠命令
- `.claude/skills/` — 4 个 AI Skills

---

## 核心概念

| 概念 | 说明 |
|------|------|
| **Change** | 一次变更提案，包含 proposal、design、tasks 等 artifact |
| **Artifact** | 变更中的文档产物（proposal.md、design.md、tasks.md 等） |
| **Schema** | 工作流模板，定义 artifact 的生成顺序和依赖关系 |
| **Spec** | 规范文档，描述系统的能力和行为 |

### 工作流概览

```
  Propose          Apply           Archive
  (提出变更)  ──▶  (实施任务)  ──▶  (归档完成)
     │                │                │
     ▼                ▼                ▼
  proposal.md     逐个完成 tasks    移动到 archive/
  design.md       标记 [x]         同步 specs
  tasks.md
```

---

## CLI 命令

### 基础命令

```bash
# 查看版本
openspec --version

# 查看帮助
openspec --help

# 初始化项目
openspec init --tools claude

# 列出所有变更和规范
openspec list
openspec list --json          # JSON 格式输出
openspec list --specs         # 列出规范

# 查看交互式仪表板
openspec view

# 查看状态
openspec status
openspec status --change "<name>"
openspec status --change "<name>" --json

# 显示变更或规范详情
openspec show <item-name>

# 验证变更和规范
openspec validate
openspec validate <item-name>
```

### 变更管理

```bash
# 创建新变更
openspec new change "<name>"

# 获取生成 artifact 的指令
openspec instructions <artifact-id> --change "<name>" --json
openspec instructions apply --change "<name>" --json

# 归档已完成的变更
openspec archive <change-name>
```

### 规范管理

```bash
# 管理规范
openspec spec

# 更新指令文件
openspec update [path]
```

### 配置

```bash
# 查看/修改全局配置
openspec config

# 关闭匿名使用统计
OPENSPEC_TELEMETRY=0
```

---

## Claude Code 斜杠命令

初始化后可用 4 个 `/opsx:` 命令：

### /opsx:propose — 提出变更

**用途：** 描述你想构建什么，自动生成完整的变更提案。

**生成的 artifact：**
- `proposal.md` — 做什么 & 为什么
- `design.md` — 怎么做
- `tasks.md` — 实施步骤

**用法：**
```
/opsx:propose "添加用户认证功能"
/opsx:propose add-dark-mode
/opsx:propose                    # 会询问你想构建什么
```

**工作流程：**
1. 创建变更目录 `openspec/changes/<name>/`
2. 按依赖顺序生成所有 artifact
3. 所有 artifact 就绪后提示运行 `/opsx:apply`

---

### /opsx:apply — 实施任务

**用途：** 按 tasks.md 中的任务列表逐个实施代码变更。

**用法：**
```
/opsx:apply                      # 自动推断或选择变更
/opsx:apply add-auth             # 指定变更名
```

**工作流程：**
1. 读取变更的上下文文件（proposal、design、tasks）
2. 逐个执行 pending 任务
3. 完成后标记 `- [x]`
4. 遇到问题时暂停并询问

**输出示例：**
```
## Implementing: add-auth (schema: spec-driven)

Working on task 3/7: Implement OAuth flow
[...implementation...]
✓ Task complete

Working on task 4/7: Add session middleware
[...implementation...]
✓ Task complete
```

---

### /opsx:explore — 探索模式

**用途：** 进入思考模式，探索想法、调查问题、澄清需求。**不会写代码。**

**用法：**
```
/opsx:explore                         # 自由探索
/opsx:explore "实时协作功能"           # 探索特定主题
/opsx:explore add-auth                # 在变更上下文中探索
```

**可以做的事：**
- 提出澄清性问题
- 用 ASCII 图表可视化架构
- 调查代码库
- 比较方案（如 Postgres vs SQLite）
- 挑战假设、发现风险

**不会做的事：**
- 写代码或实现功能
- 强制结构化输出
- 自动捕获决策（会询问你）

**洞察捕获位置：**

| 洞察类型 | 保存位置 |
|----------|----------|
| 新发现的需求 | `specs/<capability>/spec.md` |
| 需求变更 | `specs/<capability>/spec.md` |
| 设计决策 | `design.md` |
| 范围变更 | `proposal.md` |
| 新增工作 | `tasks.md` |

---

### /opsx:archive — 归档变更

**用途：** 将已完成的变更归档到 `openspec/changes/archive/`。

**用法：**
```
/opsx:archive                         # 选择要归档的变更
/opsx:archive add-auth                # 指定变更名
```

**工作流程：**
1. 检查 artifact 完成状态（未完成会警告）
2. 检查 task 完成状态（未完成会警告）
3. 评估 delta spec 同步状态
4. 移动到 `openspec/changes/archive/YYYY-MM-DD-<name>/`

---

## AI Skills

除了斜杠命令，还有 4 个底层 Skill 可被 Claude 自动调用：

| Skill | 说明 |
|-------|------|
| `openspec-propose` | 生成变更提案和所有 artifact |
| `openspec-apply-change` | 实施变更中的任务 |
| `openspec-explore` | 进入探索/思考模式 |
| `openspec-archive-change` | 归档已完成的变更 |

---

## 典型工作流

### 完整流程

```
1. 探索想法
   /opsx:explore "我想添加视频播放列表功能"

2. 提出变更
   /opsx:propose "add-video-playlist"

3. 审查生成的文档
   openspec/changes/add-video-playlist/
   ├── proposal.md    ← 做什么、为什么
   ├── design.md      ← 怎么做
   └── tasks.md       ← 实施步骤

4. 开始实施
   /opsx:apply add-video-playlist

5. 归档完成
   /opsx:archive add-video-playlist
```

### 项目目录结构

```
project/
├── .claude/
│   ├── commands/opsx/       # 斜杠命令
│   │   ├── propose.md
│   │   ├── apply.md
│   │   ├── explore.md
│   │   └── archive.md
│   └── skills/              # AI Skills
│       ├── openspec-propose/
│       ├── openspec-apply-change/
│       ├── openspec-explore/
│       └── openspec-archive-change/
└── openspec/
    ├── changes/             # 活跃变更
    │   ├── <change-name>/
    │   │   ├── .openspec.yaml
    │   │   ├── proposal.md
    │   │   ├── design.md
    │   │   └── tasks.md
    │   └── archive/         # 已归档变更
    └── specs/               # 规范文档
```

---

## 常用场景速查

| 场景 | 命令 |
|------|------|
| 想讨论一个想法 | `/opsx:explore "你的想法"` |
| 要开始一个新功能 | `/opsx:propose "功能描述"` |
| 继续实施任务 | `/opsx:apply <change-name>` |
| 实施中遇到问题想讨论 | `/opsx:explore <change-name>` |
| 完成后归档 | `/opsx:archive <change-name>` |
| 查看当前变更列表 | `openspec list` |
| 查看变更状态 | `openspec status --change "<name>"` |

---

## 参考链接

- GitHub: https://github.com/Fission-AI/OpenSpec
- 文档: https://lzw.me/docs/OpenSpec-Docs-zh/
- 反馈: https://github.com/Fission-AI/OpenSpec/issues
