我来为您生成一份完整的 Kimi Code 使用手册。

---

Kimi Code 完整使用手册

版本：v1.0

适用产品：Kimi Code CLI + Kimi Code for VS Code

更新日期：2026-02-24

---

目录

1. [产品概述](#一产品概述)
2. [安装与配置](#二安装与配置)
3. [核心功能详解](#三核心功能详解)
4. [命令参考大全](#四命令参考大全)
5. [会话管理与继续任务](#五会话管理与继续任务)
6. [额度管理与计费](#六额度管理与计费)
7. [IDE 集成与插件](#七ide-集成与插件)
8. [编码与文件处理](#八编码与文件处理)
9. [故障排查与优化](#九故障排查与优化)
10. [最佳实践](#十最佳实践)

---

一、产品概述

1.1 什么是 Kimi Code

Kimi Code 是由 月之暗面（Moonshot AI） 推出的 AI 编程助手，包含两种形态：

形态 定位 适用场景
Kimi Code CLI 终端命令行工具 自动化脚本、服务器环境、重度终端用户
Kimi Code for VS Code 编辑器插件 日常开发、可视化编码、IDE 用户

核心能力：

- 代码生成、重构、审查
- 项目理解与架构分析
- 自动化文件操作与终端执行
- 多轮对话与上下文管理
- MCP（Model Context Protocol）工具集成

1.2 与 Kimi 智能助手的区别

特性 Kimi 智能助手 Kimi Code
交互界面 网页/APP 终端/IDE 插件
代码能力 通用问答 深度编程专用（Agent 模式）
文件操作 不支持 支持读写、执行命令
项目理解 单文件 整个代码库索引
计费方式 免费/打赏加速 会员订阅制

---

二、安装与配置

2.1 系统要求

- 操作系统：macOS 12+ / Windows 10+ / Linux
- Python：3.12-3.14（推荐 3.13）
- 内存：建议 8GB+
- 网络：需连接 Moonshot 服务器

2.2 安装 Kimi Code CLI

方式一：自动安装脚本（推荐）

```bash
# macOS / Linux
curl -LsSf https://code.kimi.com/install.sh | bash

# Windows (PowerShell)
Invoke-RestMethod https://code.kimi.com/install.ps1 | Invoke-Expression
```

方式二：通过 uv 安装

```bash
uv tool install --python 3.13 kimi-cli
```

验证安装：

```bash
kimi --version
# 输出：kimi, version 1.6
```

2.3 首次配置

```bash
# 进入项目目录
cd your-project

# 启动 Kimi Code
kimi

# 登录配置（首次使用）
/login
```

登录选项：

- Kimi Code：OAuth 浏览器授权（推荐，需会员订阅）
- 其他平台：输入 API Key（支持 Moonshot API 或其他兼容平台）

2.4 VS Code 插件安装

1. 打开 VS Code 扩展市场
2. 搜索 "Kimi Code"
3. 点击安装
4. 按提示完成登录授权

---

三、核心功能详解

3.1 双模式交互

模式 切换方式 用途
Agent 模式（默认）    `Ctrl-X`    与 AI 对话，发送自然语言指令
Shell 模式    `Ctrl-X`    直接执行系统命令

Shell 模式特点：

- 每个命令独立执行
- `cd`、`export` 等环境变更不影响后续命令
- 支持 `/help`、`/exit`、`/version` 等基础命令

3.2 上下文引用（@ 语法）

```bash
# 引用当前文件
帮我优化 @current 中的这个函数

# 引用特定文件
分析 @src/utils/api.ts 的错误处理

# 引用文件夹
重构 @src/components/ 下的所有组件

# 引用代码行范围
检查 @src/app.ts:10-20 这段逻辑

# 引用多个文件
对比 @src/old.ts 和 @src/new.ts 的差异
```

VS Code 快捷操作：

- `Alt+K`：快速插入当前文件或选中代码作为引用

3.3 代码修改与确认

流程：

1. 描述修改需求
2. AI 生成修改方案（Diff 预览）
3. 用户确认：
    - 允许：执行本次修改
    - 本会话允许：当前会话内自动批准同类操作
    - 拒绝：取消操作

YOLO 模式（自动批准所有操作）：

```bash
# 启动时启用
kimi --yolo

# 运行中切换
/yolo
```

> ⚠️ 警告：YOLO 模式会跳过所有确认，请仅在安全环境中使用。

3.4 Thinking 模式（深度思考）

启用方式：

- 通过 `/model` 命令切换模型时开启
- 启动时添加参数：`kimi --thinking`
- 部分模型（如 `kimi-k2-thinking-turbo`）始终开启

特点：

- AI 在回答前展示思考过程
- 适合复杂架构设计、算法优化等任务
- 思考步骤默认折叠，可展开查看

---

四、命令参考大全

4.1 启动命令（CLI 参数）

命令 简写 功能
`kimi`    - 启动交互式会话
`kimi --continue`    `-C`    继续最近会话
`kimi --session <id>`    `-S <id>`    指定会话 ID 启动
`kimi --work-dir <path>`    `-w <path>`    指定工作目录
`kimi --yolo`    - 启动并开启 YOLO 模式
`kimi --thinking`    - 启动并开启 Thinking 模式
`kimi -c "<prompt>"`    - 非交互式单次执行
`kimi --print -c "<prompt>"`    - 执行并输出到 stdout
`kimi --version`    - 查看版本
`kimi --help`    - 查看帮助

4.2 斜杠命令（交互式）

帮助与信息

命令 别名 功能
`/help`    `/h`, `/?`    显示帮助信息
`/version`    - 显示版本
`/changelog`    `/release-notes`    显示更新日志
`/feedback`    - 提交反馈

账号与配置

命令 功能
`/login`    登录/配置 API 平台
`/logout`    登出当前平台
`/model`    切换模型和 Thinking 模式
`/reload`    重新加载配置
`/debug`    显示上下文调试信息
`/usage`    显示 API 用量和配额
`/mcp`    显示已连接的 MCP 服务器

会话管理

命令 别名 功能
`/sessions`    `/resume`    列出并切换会话
`/clear`    `/reset`    清空当前会话上下文
`/compact`    - 压缩上下文，减少 Token 消耗

项目与执行

命令 功能
`/init`    分析项目并生成 AGENTS.md
`/yolo`    切换 YOLO 模式

Skills 与 Flows

命令 功能
`/skill:<name>`    加载指定 Skill
`/flow:<name>`    执行指定 Flow

4.3 快捷键

快捷键 功能
`Ctrl-X`    切换 Agent / Shell 模式
`Ctrl-J` / `Alt-Enter`    插入换行（多行输入）
`Ctrl-V`    粘贴剪贴板（支持图片）
`Enter`    发送消息
`Ctrl-E`    展开查看完整内容
`Ctrl-C`    取消当前操作
`Tab` / `Enter`    选择补全项

---

五、会话管理与继续任务

5.1 会话持久化机制

Kimi Code 自动保存会话数据到：

```
~/.kimi/sessions/
├── <workdir_hash>/
│   ├── <session_id>/
│   │   ├── context.jsonl    # 消息历史
│   │   └── wire.jsonl       # 事件记录
```

5.2 继续任务的 4 种方式

方式 1：继续最近会话

```bash
kimi --continue
```

方式 2：指定会话 ID

```bash
kimi --session abc123def456
```

方式 3：交互式选择

```bash
kimi
/sessions
# 使用方向键选择，Enter 确认
```

方式 4：VS Code 历史面板

- 点击面板顶部历史下拉菜单
- 选择历史会话继续

5.3 会话管理最佳实践

```bash
# 长任务定期压缩上下文
/compact 已完成数据库优化，接下来处理缓存层

# 多任务并行（不同终端）
# 终端 1
kimi --session backend-api

# 终端 2  
kimi --session frontend-ui
```

---

六、额度管理与计费

6.1 额度体系（2026年1月30日后）

套餐 5小时限额 每周限额 每月限额
Lite 1,200 次 9,000 次 18,000 次
Pro 6,000 次 45,000 次 90,000 次

刷新规则：

- 5小时额度：滚动窗口，请求后 5 小时恢复
- 每周额度：以订阅日 D1 为起点，D1-D7、D8-D14...
- 每月额度：订阅日次月同日重置

6.2 额度用完后的处理

方案 操作 时效
等待刷新 关闭 Kimi，等待 5小时/下周 免费
升级 Pro 控制台订阅管理 → 升级 即时
购买 API platform.moonshot.cn 充值 即时
第三方平台 OpenRouter / 阿里云百炼 即时

6.3 查看额度

```bash
/usage
# 或
/status
```

---

七、IDE 集成与插件

7.1 VS Code 插件（官方）

安装：扩展市场搜索 "Kimi Code"

独有功能：

- 可视化 Diff 预览
- `Alt+K` 快速文件引用
- 侧边栏聊天面板
- 完整的斜杠命令支持

7.2 JetBrains 系列（ACP 协议）

配置步骤：

1. 确保 Kimi Code CLI 已安装
2. 获取 kimi 完整路径：`which kimi`
3. IDE 中配置 ACP：

```json
   {
     "agent_servers": {
       "Kimi Code CLI": {
         "command": "/Users/username/.local/bin/kimi",
         "args": ["acp"]
       }
     }
   }
   ```

已知限制：

- `/sessions` 命令可能不可用
- 历史记录功能受限
- 建议复杂会话管理使用 CLI

7.3 其他编辑器

通过 Claude Code + Kimi 模型 间接使用：

```bash
export ANTHROPIC_BASE_URL=https://api.moonshot.cn/anthropic
export ANTHROPIC_AUTH_TOKEN=sk-your-key
export ANTHROPIC_MODEL=kimi-k2.5
```

---

八、编码与文件处理

8.1 预防中文乱码

项目级配置（`.editorconfig`）：

```ini
root = true

[*.java]
charset = utf-8
end_of_line = lf
insert_final_newline = true
```

AGENTS.md 规范：

```markdown
## 编码规范
- 所有文件使用 UTF-8 编码（无 BOM）
- 保持原始文件编码，禁止转换
- 中文内容使用 UTF-8 存储
```

环境变量（Windows）：

```powershell
$env:JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8"
```

8.2 修复已乱码文件

VS Code：

1. 右下角编码 → "通过编码重新打开" → 尝试 GBK/UTF-8
2. 中文正常后 → "通过编码保存" → UTF-8

批量修复：

```bash
# Linux/Mac
find . -name "*.java" -exec iconv -f GBK -t UTF-8 {} -o {} \;
```

---

九、故障排查与优化

9.1 常见问题

问题 原因 解决
额度用完无法继续 达到订阅限额 等待刷新/升级 Pro/购买 API
会话无法恢复 会话 ID 错误或过期 使用 `/sessions` 选择有效会话
中文乱码 编码不一致 统一为 UTF-8，配置 .editorconfig
MCP 连接失败 配置错误或服务器未启动 检查 `~/.kimi/mcp.json`
启动缓慢（macOS） 安全检查 系统设置 → 隐私与安全性 → 开发者工具 → 添加终端

9.2 性能优化

```bash
# 压缩上下文（长会话）
/compact

# 开启 YOLO 模式（减少确认交互）
kimi --yolo

# 使用轻量模型（简单任务）
/model  # 选择非-thinking 模型
```

---

十、最佳实践

10.1 项目初始化流程

```bash
cd new-project

# 1. 生成项目文档
kimi
/init

# 2. 继续会话开发
kimi --continue
```

10.2 日常开发工作流

```bash
# 快速启动
kimi -C  # 继续最近会话

# 引用文件提问
优化 @src/utils/api.ts 的错误处理

# 定期压缩
/compact 已完成 API 层优化，接下来处理数据库层

# 查看额度
/usage
```

10.3 多项目并行

```bash
# 项目 A
kimi -w ~/projects/backend -S backend-api

# 项目 B  
kimi -w ~/projects/frontend -S frontend-ui
```

10.4 与版本控制结合

```bash
# 提交前让 AI 检查
检查 @src/ 下的修改，生成提交信息

# 或使用 Skill
/skill:git-commits 修复用户登录问题
```

---

附录

A. 官方资源

- 官网：https://kimi.com/code
- 文档：https://www.kimi.com/code/docs
- 开放平台：https://platform.moonshot.cn

B. 快捷键速查表

操作 快捷键
切换模式    `Ctrl-X`
多行输入    `Ctrl-J` / `Alt-Enter`
粘贴    `Ctrl-V`
发送    `Enter`
展开内容    `Ctrl-E`
取消    `Ctrl-C`

C. 环境变量参考

变量 用途
`ANTHROPIC_BASE_URL`    第三方 API 地址
`ANTHROPIC_AUTH_TOKEN`    API Key
`ANTHROPIC_MODEL`    默认模型
`JAVA_TOOL_OPTIONS`    JVM 编码设置
`PYTHONIOENCODING`    Python IO 编码

---

本手册基于 Kimi Code v1.6 编写，后续版本功能可能有所调整，请以官方文档为准。