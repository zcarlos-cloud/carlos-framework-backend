---

Claude Code 从入门到高阶实战手册

---

阶段一：基础入门（1-2周）

学习目标
掌握核心交互与文件操作，建立与 Claude Code 的基本协作关系。

每日训练清单

天数 训练内容 验证标准
Day 1 安装与初始化 成功运行 `claude` 命令，完成登录
Day 2 基础命令掌握 不使用帮助完成 `/clear`, `/compact`, `/exit`
Day 3 文件读取与理解 向 Claude 询问 5 个文件的功能，验证理解正确
Day 4 简单编辑操作 完成 10 次成功的文本替换和插入
Day 5 代码生成 从零生成一个可运行的 Hello World 程序
Day 6 终端命令集成 在 Claude 内完成 `git init` 到 `git commit` 全流程
Day 7 综合练习 独立完成一个小功能（如配置文件读取）

核心命令速查表

```bash
# 会话管理
/help           # 查看所有命令
/clear          # 清空当前会话（保留历史）
/compact        # 压缩会话历史，释放上下文
/exit 或 Ctrl+D # 退出 Claude Code

# 常用操作（自然语言即可）
"读取 src/main.js 文件"
"在第 20 行后添加一个 console.log"
"将所有的 var 替换为 const"
"运行 npm test"
```

实战案例：配置文件的创建与修改

场景：为一个 Node.js 项目创建 `.gitignore` 文件

步骤 1：创建文件

```
我：创建一个 .gitignore 文件，忽略 node_modules、.env 和日志文件
Claude：我来为你创建这个文件
[使用 Write 工具写入内容]
```

步骤 2：验证内容

```
我：读取 .gitignore 确认内容正确
Claude：[使用 Read 工具显示内容]
```

步骤 3：追加规则

```
我：追加忽略 .DS_Store 和 dist 目录
Claude：[使用 Edit 工具在末尾添加]
```

常见错误与解决

- ❌ "文件不存在" → 确认路径正确，或先创建父目录
- ❌ "权限不足" → 检查文件是否被其他程序锁定
- ❌ "修改不符合预期" → 使用行号精确指定位置

阶段一退出标准

- 能够盲打（不看帮助）完成基础命令
- 5 分钟内完成从文件创建到编辑到保存的完整流程
- 成功向 Claude 解释清楚一个陌生代码文件的功能

---

阶段二：工具精通（2-3周）

学习目标
熟练使用所有内置工具，建立高效代码库导航能力。

工具深度掌握清单

工具 核心用法 高级技巧 练习任务
Read 读取文件内容 读取特定行范围 `Read(10-20)`    读取 20 个不同文件
Edit 替换/插入文本 多行替换、正则匹配 完成 30 次精确编辑
Write 创建新文件 批量生成文件结构 创建 10 个文件
Grep 文本搜索 正则搜索、路径过滤 搜索 15 个不同关键词
Glob 文件模式匹配 复杂模式如 `**/*.{ts,tsx}`    列出 10 种不同文件组合
Find 按名称查找 模糊匹配、类型过滤 找到 20 个特定文件
Bash 执行命令 管道组合、环境变量 执行 25 个常用命令
LSP 代码导航 跳转到定义、查找引用 导航 30 个符号
Fetch 获取网页 解析特定内容 抓取 5 个文档页面

实战案例：大型代码库导航

场景：新入职，需要快速理解一个 500+ 文件的 React 项目

步骤 1：项目结构概览

```
我：使用 Glob 查看 src 目录下的所有组件文件
Claude：我来列出所有组件
[使用 Glob 工具匹配 src/**/*.{jsx,tsx}]
```

步骤 2：理解入口

```
我：读取 src/App.jsx 和 src/main.jsx
Claude：[使用 Read 工具读取两个文件]
```

步骤 3：追踪数据流

```
我：搜索 "userState" 在哪些地方被使用
Claude：[使用 Grep 工具搜索]
```

步骤 4：理解组件关系

```
我：跳转到 UserProfile 组件的定义
Claude：[使用 LSP 跳转到定义]
```

步骤 5：查看类型定义

```
我：查找 User 类型的定义
Claude：[使用 Grep 搜索 type User 或 interface User]
```

LSP 高级技巧

```bash
# 跳转到定义
"跳转到 handleSubmit 函数的定义"

# 查找所有引用
"查找所有使用 AUTH_TOKEN 的地方"

# 查看符号信息
"显示 UserInterface 的完整类型定义"

# 代码补全
"补全这个对象缺少的属性"
```

CLAUDE.md 模板

```markdown
# 项目上下文

## 技术栈
- 前端：React 18 + TypeScript + Vite
- 状态管理：Zustand
- 样式：Tailwind CSS
- 测试：Vitest + React Testing Library

## 常用命令
```bash
npm run dev      # 启动开发服务器
npm run build    # 生产构建
npm run test     # 运行测试
npm run lint     # 代码检查
```

代码规范

- 使用函数组件 + Hooks
- 类型定义放在 `types/` 目录
- 组件文件使用 PascalCase
- 工具函数使用 camelCase

架构决策

- API 调用封装在 `services/` 目录
- 全局状态仅存储用户信息，其他用本地状态
- 路由配置在 `router/index.ts`

```

### 阶段二退出标准
- [ ] 30 秒内找到任意函数的所有引用
- [ ] 熟练使用 LSP 进行代码导航（无需鼠标）
- [ ] 为当前项目编写完整的 CLAUDE.md
- [ ] 能够向新成员用 5 分钟讲清楚项目结构

---

## 阶段三：Sub-agent 协作 L1（3-4周）

### 学习目标
掌握智能体委托，实现并行工作流，保持主会话清爽。

### Sub-agent 类型详解

| 类型 | 最佳场景 | 模型建议 | 权限特点 |
|------|---------|---------|---------|
| **Explore** | 代码库搜索、文件发现 | Haiku（最快） | 只读，无修改权限 |
| **Plan** | 设计方案、架构规划 | Sonnet/Opus | 只读，专注规划 |
| **general-purpose** | 复杂多步骤任务 | 继承主会话 | 全权限，可修改 |
| **Bash** | 运行命令、处理日志 | 继承主会话 | 仅 Bash 工具 |
| **code-reviewer** | 代码审查 | Sonnet | 只读，审查专用 |

### 调用方式训练

**方式 1：自然语言委托（自动识别）**
```

我：探索 utils 目录下所有的辅助函数
Claude：我将派生 Explore agent 来完成这个任务
[自动使用 Task 工具创建 Explore agent]

```

**方式 2：@提及强制指定**
```

我：@"Explore (agent)" 彻底搜索所有 API 端点
Claude：使用 Explore agent 执行深度搜索

```

**方式 3：整会话模式**
```bash
# 命令行启动
claude --agent code-reviewer
```

实战案例：并行代码审查

场景：需要审查 5 个新提交的组件文件

步骤 1：主 Agent 规划

```
我：我需要审查 5 个新组件，分别是 UserCard、ProductList、
    CartItem、CheckoutForm、OrderSummary。请并行审查它们。

Claude：我将派生 5 个 code-reviewer agent 并行审查这些组件。
```

步骤 2：派生 Sub-agents（自动并行）

```
Claude：[使用 Task 工具创建 5 个并行的 code-reviewer agent]

Task 1: @"code-reviewer (agent)" 审查 UserCard 组件
Task 2: @"code-reviewer (agent)" 审查 ProductList 组件
Task 3: @"code-reviewer (agent)" 审查 CartItem 组件
Task 4: @"code-reviewer (agent)" 审查 CheckoutForm 组件
Task 5: @"code-reviewer (agent)" 审查 OrderSummary 组件
```

步骤 3：收集结果

```
Claude：所有审查完成，综合结果如下：

UserCard: ✓ 通过，建议添加 loading 状态
ProductList: ⚠️ 发现性能问题，建议使用虚拟列表
CartItem: ✓ 通过
CheckoutForm: ⚠️ 表单验证逻辑需要简化
OrderSummary: ✓ 通过
```

步骤 4：主 Agent 决策

```
我：针对 ProductList 的性能问题，派 general-purpose agent 实现虚拟列表

Claude：[派生 general-purpose agent 执行重构]
```

实战案例：高容量操作隔离

场景：运行完整测试套件，但只关心失败项

低效做法（主会话直接运行）

```
我：运行 npm test
Claude：[运行测试，大量输出填满上下文]
```

高效做法（Sub-agent 隔离）

```
我：派 Sub-agent 运行完整测试套件，只返回失败的测试名称和错误信息

Claude：[派生 Bash agent 运行测试]
结果：仅返回 3 个失败项，主会话清爽
```

链式工作流模板

```
我：执行以下流水线：
1. 派 Explore agent 搜索所有认证相关代码
2. 基于结果，派 Plan agent 设计优化方案  
3. 最后派 general-purpose agent 实现方案
```

阶段三退出标准

- 成功执行 10+ 次自然语言触发的 Sub-agent 调用
- 设计并执行一次包含 3+ 并行 Sub-agent 的工作流
- 使用 Sub-agent 完成一次高容量操作（测试/日志处理）
- 主会话始终保持简洁，关键决策清晰可见

---

阶段四：MCP 生态集成（2-3周）

学习目标
扩展 Claude Code 能力边界，连接数据库、浏览器、API 等外部系统。

MCP 核心概念

```
┌─────────────┐      MCP Protocol       ┌─────────────┐
│  Claude Code │  ◄──────────────────►  │  MCP Server │
│   (Client)   │   标准化工具调用接口      │  (Puppeteer │
└─────────────┘                         │   Postgres  │
                                        │   GitHub...) │
                                        └─────────────┘
```

常用 MCP Server 清单

类别 MCP Server 用途 安装命令
浏览器 Puppeteer 网页抓取、截图、测试    `claude mcp add puppeteer`
数据库 PostgreSQL SQL 查询、Schema 管理    `claude mcp add postgres`
版本控制 GitHub PR 管理、Issue 追踪    `claude mcp add github`
搜索 Brave Search 网络搜索    `claude mcp add brave-search`
文件 Filesystem 增强文件操作    `claude mcp add filesystem`

配置管理实战

全局配置（/.claude/claude.json）

```json
{
  "mcpServers": {
    "puppeteer": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
    },
    "postgres": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-postgres"],
      "env": {
        "DATABASE_URL": "postgresql://localhost/mydb"
      }
    }
  }
}
```

项目级配置（.claude/claude.json）

```json
{
  "mcpServers": {
    "supabase": {
      "command": "npx",
      "args": ["-y", "@supabase/mcp-server-postgrest"],
      "env": {
        "SUPABASE_URL": "${SUPABASE_URL}",
        "SUPABASE_SERVICE_ROLE_KEY": "${SUPABASE_SERVICE_ROLE_KEY}"
      }
    }
  }
}
```

实战案例：数据库 + 代码联动

场景：需要理解数据库 Schema 并生成对应 TypeScript 类型

步骤 1：查询数据库结构

```
我：使用 PostgreSQL MCP 查看 users 表的列定义

Claude：[调用 MCP 工具 query]
结果：id(uuid), email(varchar), created_at(timestamp)...
```

步骤 2：生成 TypeScript 类型

```
我：基于这个表结构，生成对应的 TypeScript 接口

Claude：[生成代码]
interface User {
  id: string;
  email: string;
  created_at: Date;
}
```

步骤 3：写入类型文件

```
我：将类型写入 src/types/database.ts

Claude：[使用 Write 工具创建文件]
```

实战案例：浏览器自动化测试

场景：验证登录页面的错误提示样式

步骤 1：抓取页面

```
我：使用 Puppeteer MCP 打开 http://localhost:3000/login 并截图

Claude：[调用 browser_navigate 和 browser_screenshot]
```

步骤 2：模拟用户操作

```
我：在邮箱输入框输入 invalid-email，点击登录按钮，截图查看错误提示

Claude：[调用 browser_click, browser_type, browser_screenshot]
```

步骤 3：对比设计稿

```
我：获取设计稿中错误状态的截图，对比当前实现

Claude：[使用 Fetch 获取设计稿，对比分析]
```

MCP 工具调用技巧

```bash
# 自然语言调用（Claude 自动选择）
"查询最近 7 天的订单数据"
"截图当前页面"
"创建一个新的 GitHub Issue"

# 精确调用（知道具体工具名）
"使用 puppeteer_navigate 打开页面"
"使用 postgres_query 执行 SQL"
```

阶段四退出标准

- 安装并配置 5+ MCP Server，能在对话中自然调用
- 完成一次数据库 Schema 到 TypeScript 类型的自动生成
- 使用 Puppeteer MCP 完成一次视觉回归测试
- 为不同项目配置专属的 MCP 工具集

---

阶段五：自定义 Skills（2-3周）

学习目标
创建领域专属知识库，提升团队专业场景效能。

Skills 文件结构

```
.claude/skills/
├── my-team-skill/
│   ├── SKILL.md           # 技能定义（必需）
│   ├── AGENTS.md          # Agent 配置（可选）
│   └── rules/
│       ├── 01-coding.md   # 编码规范
│       ├── 02-testing.md  # 测试规则
│       └── 03-security.md # 安全规则
```

SKILL.md 模板

```markdown
---
name: react-enterprise
description: React 企业级开发最佳实践
tools: Read, Edit, Write, Bash
---

## 技术栈规范

### 组件定义
- 使用函数组件 + TypeScript
- Props 接口必须导出
- 复杂组件使用 Container/Presentational 模式

### 状态管理
- 全局状态：Zustand（存储在 stores/）
- 服务端状态：React Query
- 表单状态：React Hook Form

### 性能优化
- 列表使用虚拟滚动（react-window）
- 图片懒加载（loading="lazy"）
- 路由懒加载（React.lazy）

## 文件组织
```

src/
├── components/ # 通用组件
│ ├── ui/ # 基础 UI（Button, Input）
│ └── composite/ # 复合组件（UserCard）
├── features/ # 功能模块
│ ├── auth/ # 认证功能
│ └── checkout/ # 支付功能
├── stores/ # Zustand 状态
├── hooks/ # 自定义 Hooks
└── utils/ # 工具函数

```

## 常用命令
```bash
npm run test:unit      # 单元测试
npm run test:e2e       # E2E 测试
npm run lint:strict    # 严格模式检查
npm run type-check     # TypeScript 检查
```

```

### AGENTS.md 模板

```markdown
---
name: react-architect
description: React 架构设计专家
tools: Read, Plan, Explore
---

## 职责
- 分析需求并设计组件架构
- 定义数据流和状态管理方案
- 评估技术选型（仅建议，不实现）

## 约束
- 不直接修改代码文件
- 不生成具体实现代码
- 只输出设计文档和决策记录

## 输出格式
所有设计方案必须包含：
1. 组件树结构图
2. 数据流图示
3. 文件创建清单
4. 潜在风险评估
```

渐进式披露配置

```markdown
## 上下文加载策略

### 始终加载
- 技术栈规范
- 文件组织约定

### 条件加载
- 当检测到路由文件时，加载 `rules/routing.md`
- 当检测到表单时，加载 `rules/forms.md`
- 当检测到 API 调用时，加载 `rules/api-integration.md`

### 显式请求加载
- 性能优化指南（用户提及性能时）
- 安全规范（用户提及认证/授权时）
- 测试策略（用户提及测试时）
```

实战案例：创建团队 React Skill

步骤 1：初始化 Skill 结构

```bash
mkdir -p .claude/skills/react-enterprise/rules
touch .claude/skills/react-enterprise/SKILL.md
touch .claude/skills/react-enterprise/AGENTS.md
touch .claude/skills/react-enterprise/rules/01-components.md
touch .claude/skills/react-enterprise/rules/02-state.md
```

步骤 2：编写核心规范（SKILL.md）

```markdown
---
name: react-enterprise
description: 企业级 React 开发规范
---

## 强制规则

### 组件创建
1. 所有组件必须有对应的 .test.tsx 文件
2. Props 接口必须在组件文件内定义并导出
3. 复杂逻辑必须抽取为自定义 Hook

### 代码风格
- 使用单引号字符串
- 缩进 2 空格
- 最大行宽 100 字符
```

步骤 3：测试 Skill

```
我：创建一个用户卡片组件

Claude：[自动加载 react-enterprise skill]
Claude：我将按照规范创建 UserCard 组件，包含测试文件和类型定义...
```

实战案例：Skill 版本管理

版本控制策略

```markdown
## 变更日志

### v1.2.0 (2024-03-15)
- 新增：React Server Components 支持
- 修改：状态管理从 Redux 迁移到 Zustand
- 废弃：class 组件支持（迁移期 3 个月）

### v1.1.0 (2024-02-01)
- 新增：测试覆盖率要求（最低 80%）
- 修复：API 错误处理规范
```

阶段五退出标准

- 创建 1 个包含 SKILL.md + AGENTS.md 的完整 Skill
- Skill 被团队成员实际使用（至少 3 人）
- 建立 Skill 版本管理机制
- 为 Skill 编写使用文档和示例

---

阶段六：Hooks 自动化（1-2周）

学习目标
实现会话生命周期自动化，减少重复操作。

Hooks 类型详解

类型 触发时机 用途 作用域
PreToolUse 工具执行前 验证、拦截、强制委托 全局/Agent
PostToolUse 工具执行后 通知、格式化、后续处理 全局/Agent
Stop 会话结束时 清理、提交、报告 全局/Agent

配置位置

```bash
# 全局 Hooks（所有会话生效）
~/.claude/hooks/

# Agent 级 Hooks（仅特定 Agent 生效）
.claude/agents/{agent-name}/hooks/
```

实战案例：自动格式化代码

场景：每次保存文件后自动运行 Prettier

配置（/.claude/hooks/pre-tool-use.sh）

```bash
#!/bin/bash
# PreToolUse Hook: 自动格式化

TOOL_NAME=$1
FILE_PATH=$2

if [ "$TOOL_NAME" = "Write" ] || [ "$TOOL_NAME" = "Edit" ]; then
  # 检查文件是否为 JS/TS
  if [[ "$FILE_PATH" =~ \.(js|ts|jsx|tsx)$ ]]; then
    echo "Running prettier on $FILE_PATH"
    npx prettier --write "$FILE_PATH"
  fi
fi
```

注册 Hook

```json
// ~/.claude/claude.json
{
  "hooks": {
    "PreToolUse": {
      "command": "~/.claude/hooks/pre-tool-use.sh",
      "args": ["{{tool_name}}", "{{file_path}}"]
    }
  }
}
```

实战案例：强制 Sub-agent 委托

场景：确保 Research Agent 只能读取，不能修改

配置（.claude/agents/researcher/hooks/pre-tool-use.sh）

```bash
#!/bin/bash
# Research Agent 权限控制

TOOL_NAME=$1

# 禁止写入操作
if [ "$TOOL_NAME" = "Write" ] || [ "$TOOL_NAME" = "Edit" ]; then
  echo "ERROR: Research agent cannot modify files"
  echo "SUGGESTION: Delegate to general-purpose agent for modifications"
  exit 1
fi

# 允许读取和探索
exit 0
```

实战案例：会话结束自动提交

配置（/.claude/hooks/stop.sh）

```bash
#!/bin/bash
# Stop Hook: 自动 Git 提交

# 检查是否有变更
if git diff --quiet HEAD; then
  exit 0
fi

# 生成提交信息
COMMIT_MSG="Auto-commit: $(date '+%Y-%m-%d %H:%M:%S')"

# 提交变更
git add -A
git commit -m "$COMMIT_MSG"
echo "Changes committed: $COMMIT_MSG"

# 可选：推送到远程
# git push origin $(git branch --show-current)
```

条件触发技巧

```bash
# 仅对特定文件类型触发
if [[ "$FILE_PATH" =~ \.test\.(js|ts)$ ]]; then
  npm test -- "$FILE_PATH"
fi

# 仅对特定目录触发
if [[ "$FILE_PATH" =~ ^src/components/ ]]; then
  npm run storybook:build
fi

# 仅对生产环境操作触发确认
if [[ "$FILE_PATH" =~ ^prod-config/ ]]; then
  read -p "Confirm production change? (y/n) " confirm
  if [ "$confirm" != "y" ]; then
    exit 1
  fi
fi
```

阶段六退出标准

- 配置至少 1 个 PreToolUse Hook 实现自动化
- 配置至少 1 个 Stop Hook 实现会话结束处理
- 为敏感操作配置双重确认机制
- 团队内共享 Hooks 配置，新成员自动继承

---

阶段七：Agent Team 协作 L2+（3-4周）

学习目标
掌握多智能体并行协作，处理复杂工程任务。

前置条件检查清单

- `export CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1` 已设置
- Claude Code 为最新版本
- 项目已 Git 初始化
- 理解 Git Worktree 基本操作
- 具备 32GB+ RAM（推荐）

核心角色定义

角色 职责 工具权限 沟通方式
Team Lead 规划、分配、同步、合并 全部 向 teammates 广播消息
Researcher 代码分析、技术调研 只读 向 Lead 汇报发现
Developer 代码实现、单元测试 读写（分配的文件） 向 Lead 汇报进度
QA Engineer 测试验证、Bug 报告 只读 + 测试工具 向 Lead 汇报测试结果

启动流程

```bash
# 1. 启用实验性功能
export CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1

# 2. 确保项目已 Git 初始化
git status  # 确认有提交历史

# 3. 创建状态同步文件
touch sprint-status.yaml

# 4. 启动 Claude Code
claude
```

实战案例：3 人团队开发新功能

场景：开发"用户权限管理"功能，涉及前端、后端、数据库

步骤 1：Team Lead 规划（主会话）

```
我：作为 Team Lead，规划用户权限管理功能的开发。
    需要 3 个 teammates：FrontendDev、BackendDev、DBSpecialist。

Claude：我将初始化 Agent Team 并分配任务。

[创建 sprint-status.yaml]
```

sprint-status.yaml 初始内容

```yaml
sprint: E1-UserAuth
status: planning
tasks:
  - id: E1-S1
    title: 设计数据库 Schema
    owner: DBSpecialist
    status: ready
    files: []
  
  - id: E1-S2  
    title: 实现后端 API
    owner: BackendDev
    status: blocked  # 依赖 E1-S1
    depends_on: E1-S1
    files: []
  
  - id: E1-S3
    title: 实现前端界面
    owner: FrontendDev
    status: blocked
    depends_on: E1-S2
    files: []
```

步骤 2：启动 Teammates（并行）

```
我：启动所有 teammates，DBSpecialist 先开始 E1-S1

Claude：[启动 3 个独立的 teammate 会话]

Teammate 1: DBSpecialist @ worktree-1/ 领取 E1-S1
Teammate 2: BackendDev @ worktree-2/ 等待 E1-S1 完成
Teammate 3: FrontendDev @ worktree-3/ 等待 E1-S2 完成
```

步骤 3：Teammates 自主工作（独立会话）

DBSpecialist 会话内：

```
DBSpecialist: 我领取了 E1-S1，设计用户权限数据库 Schema
[独立执行：探索现有代码 -> 设计表结构 -> 实现迁移文件]
完成后更新 sprint-status.yaml: E1-S1 完成，E1-S2 解除阻塞
```

BackendDev 会话内：

```
BackendDev: 检测到 E1-S1 完成，开始 E1-S2
[独立执行：设计 API -> 实现端点 -> 编写测试]
```

步骤 4：Lead 监控与协调

```
我：查看当前进度

Claude：[读取 sprint-status.yaml]
E1-S1: completed (DBSpecialist)
E1-S2: in-progress (BackendDev, 60%)
E1-S3: ready (FrontendDev 等待中)
```

步骤 5：合并与验证

```
我：所有任务完成，合并到主分支并验证

Claude：[协调 teammates 提交代码，Team Lead 执行最终集成测试]
```

Git Worktree 隔离配置

```bash
# 为每个 teammate 创建独立工作目录
git worktree add ../project-worktree-1 teammate-1
git worktree add ../project-worktree-2 teammate-2  
git worktree add ../project-worktree-3 teammate-3

# 每个 teammate 在独立目录启动
# worktree-1/: DBSpecialist
# worktree-2/: BackendDev  
# worktree-3/: FrontendDev
```

沟通协议

Teammates 之间直接通信（无需经过 Lead）：

```
FrontendDev -> BackendDev: "E1-S2 的 API 响应格式能否调整为 {...}？"
BackendDev -> FrontendDev: "已调整，请拉取最新代码"
```

向 Lead 汇报：

```
DBSpecialist -> Lead: "E1-S1 完成，发现潜在问题：..."
```

故障恢复策略

```yaml
# sprint-status.yaml 中的重试配置
tasks:
  - id: E1-S2
    retry_count: 0
    max_retries: 3
    last_error: "API test timeout"
    recovery_strategy: "increase timeout, check DB connection"
```

阶段七退出标准

- 成功作为 Team Lead 完成一次 3+ 模块的复杂功能交付
- 熟练使用 sprint-status.yaml 进行任务分配和进度同步
- 处理过一次 teammate 故障并自动恢复
- 团队形成稳定的协作节奏（每日 standup via 状态文件）

---

阶段八：高级编排与生态（持续进阶）

学习目标
构建可复用的 AI 工程流水线，形成团队规范。

Workflow JSON 编排

claude-code-workflow 配置

```json
{
  "name": "feature-development",
  "agents": [
    {
      "name": "pm",
      "type": "claude",
      "role": "discuss",
      "prompt": "澄清需求，定义验收标准"
    },
    {
      "name": "architect", 
      "type": "claude",
      "role": "specify",
      "prompt": "设计技术方案，输出 ADR"
    },
    {
      "name": "dev-1",
      "type": "claude", 
      "role": "execute",
      "worktree": "worktree-1",
      "depends_on": ["architect"]
    },
    {
      "name": "dev-2",
      "type": "codex",
      "role": "execute", 
      "worktree": "worktree-2",
      "depends_on": ["architect"]
    }
  ],
  "stages": ["discuss", "specify", "execute", "compound"]
}
```

CI/CD 集成

GitHub Actions 配置

```yaml
# .github/workflows/ai-review.yml
name: AI Code Review

on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
    
      - name: Setup Claude Code
        run: |
          npm install -g @anthropic-ai/claude-code
          echo "${{ secrets.CLAUDE_API_KEY }}" > ~/.claude/auth.json
        
      - name: AI Review
        run: |
          claude --agent code-reviewer \
                 --pr ${{ github.event.pull_request.number }} \
                 --output review-comment.md
               
      - name: Post Review
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const review = fs.readFileSync('review-comment.md', 'utf8');
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: review
            });
```

使用分析仪表盘

追踪指标

```yaml
# metrics.yaml
daily_usage:
  sessions: 15
  total_tokens: 2500000
  tool_calls:
    Read: 120
    Edit: 45
    Task: 30
  mcp_calls:
    puppeteer: 15
    postgres: 20
  
cost_analysis:
  estimated_cost: $45.50
  cost_per_feature: $3.20
  roi: "减少开发时间 40%"
```

决策日志规范

```markdown
# decisions/ADR-001-use-zustand.md

## 背景
需要为 React 应用选择状态管理方案

## 考虑选项
1. Redux Toolkit - 成熟，但样板代码多
2. Zustand - 轻量，TypeScript 友好
3. Jotai - 原子化，适合细粒度状态

## AI 建议来源
- Claude (Sonnet 4): 推荐 Zustand，理由...
- 团队讨论: 同意轻量级方案

## 决策
采用 Zustand，理由：
- 减少 60% 样板代码
- 更好的 TypeScript 支持
- 符合团队技术债务控制目标

## 后续行动
- [ ] 编写 Zustand Skill
- [ ] 迁移现有 Redux 代码（Q3）
```

开源贡献路径

贡献类型 内容 平台
Skills 领域最佳实践 GitHub + Claude 官方商店
MCP Server 工具集成 npm + MCP 官方 registry
Hooks 自动化工作流 团队仓库 + 博客分享
Workflows 编排模板 claude-code-workflow 仓库

阶段八退出标准

- 建立团队 AI 工程规范文档
- 新成员 1 周内达到阶段 4 水平
- 完成 1 次开源贡献（Skill/MCP/Workflow）
- 形成可量化的 AI 效能指标（时间节省、质量提升）

---

## 附录 A：Carlos Framework 实战项目清单

按顺序完成这些项目，全面提升 Claude Code 在 carlos-framework 项目中的实战技能：

### 入门级

- [ ] 添加一个简单查询接口（单表查询）
- [ ] 修改前端页面样式
- [ ] 添加一个工具类方法

### 进阶级

- [ ] 开发完整的 CRUD 模块（含前后端）
- [ ] 实现多表关联查询
- [ ] 添加 Redis 缓存功能
- [ ] 集成第三方 API

### 高级

- [ ] 重构一个现有模块
- [ ] 实现复杂业务流程
- [ ] 添加权限控制功能
- [ ] 性能优化专项

---

## 附录 B：推荐工具链与每日检查清单

### 必备技能使用频率

| 频率   | 技能/命令                     | 用途     |
|------|---------------------------|--------|
| 每次   | carlos-framework-standard | 后端编码规范 |
| 每次   | /checkpoint               | 保存进度   |
| 新功能  | /plan                     | 计划模式   |
| 新功能  | /tdd                      | 测试驱动开发 |
| 写完代码 | /review                   | 代码审查   |
| 安全相关 | security-review           | 安全检查   |
| 提交前  | /verify                   | 验证流程   |
| 每天   | /learn                    | 提取模式   |

### 快捷键

```bash
Ctrl+C          # 中断当前操作
Ctrl+O          # 查看思考过程
Alt+T / Opt+T   # 切换思考模式
```

### 每日检查清单

每天使用 Claude Code 时，检查以下事项：

#### 开始工作

- [ ] 读取 CLAUDE.md 了解项目上下文
- [ ] 确认当前任务范围
- [ ] 必要时设置 checkpoint

#### 开发过程

- [ ] 使用 carlos-framework-standard 技能
- [ ] 遵循分层架构规范
- [ ] 写代码前先写测试（TDD）
- [ ] 及时运行验证

#### 代码完成

- [ ] 使用 code-reviewer 审查
- [ ] 检查安全规范
- [ ] 验证测试覆盖率 80%+
- [ ] 保存会话 /save-session

---

## 附录 C：进阶技巧汇总

### 1. 高效搜索

```markdown
# 使用 Grep 而不是 Bash grep
❌ grep -r "pattern" .
✅ Grep pattern

# 使用 Glob 查找文件
❌ find . -name "*.java"
✅ Glob **/*.java
```

### 2. 并行处理

```markdown
# 同时读取多个相关文件
Read A.java
Read B.java
Read C.java

# 然后统一分析
```

### 3. 增量修改

```markdown
# 大文件分块处理
Read file.java limit 50
# 修改前50行
Read file.java offset 51 limit 50
# 修改接下来的50行
```

### 4. Agent 协作

```markdown
# 复杂任务拆分给多个 Agent
Agent 1: 研究现有代码
Agent 2: 设计新架构
Agent 3: 编写测试

# 综合结果
```

---

## 附录 D：成功指标

完成此学习计划后，你应该能够：

- [ ] 独立完成前后端完整功能开发
- [ ] 熟练使用 TDD 流程
- [ ] 高效使用 Agent 协作模式
- [ ] 建立个人开发工作流
- [ ] 提取并复用代码模式
- [ ] 处理复杂重构任务
- [ ] 优化大项目开发效率

---

## 附录 E：学习资源

### 内部文档

- `CLAUDE.md` - 项目级指导
- `carlos-framework/CLAUDE.md` - 后端详细规范
- `carlos-framework-standard` 技能 - 编码标准

### 命令参考

- `/help` - 查看所有命令
- `/skills` - 查看可用技能

### 最佳实践来源

- 每个阶段结束后，总结经验
- 使用 /learn 提取个人最佳实践
- 持续更新此计划文档

---

附录：快速参考卡片

命令速查

```bash
# 基础
claude                      # 启动
claude --help               # 查看帮助
claude --version            # 查看版本

# Agent 模式
claude --agent explore      # 以 Explore 模式启动
claude --agent code-reviewer # 以审查模式启动

# 实验性功能
export CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1
claude --worktree           # 使用 worktree 隔离

# MCP 管理
claude mcp list             # 列出 MCP Servers
claude mcp add <name>       # 添加 MCP
claude mcp remove <name>    # 移除 MCP
```

会话内命令

```
/help                       # 查看所有命令
/clear                      # 清空当前会话
/compact                    # 压缩历史
/cost                       # 查看 Token 消耗
/debug                      # 查看系统提示
/terminal                   # 打开系统终端
/exit                       # 退出
```

自然语言触发词

意图 触发词 效果
探索代码    "探索...", "搜索...", "查找..."    自动使用 Explore agent
设计方案    "设计...", "规划...", "架构..."    自动使用 Plan agent
审查代码    "审查...", "检查...", "review..."    自动使用 code-reviewer
并行任务    "并行...", "同时...", "分别..."    创建多个 Sub-agents
高容量操作    "运行测试", "处理日志"    使用 Sub-agent 隔离

---
