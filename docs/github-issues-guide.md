# GitHub Issues 待办管理指南

本文档介绍如何使用 GitHub Issues 管理 Carlos Framework 项目的待办事项。

---

## 📋 目录

1. [快速开始](#快速开始)
2. [Issue 模板](#issue-模板)
3. [标签体系](#标签体系)
4. [工作流自动化](#工作流自动化)
5. [最佳实践](#最佳实践)
6. [批量导入工具](#批量导入工具)

---

## 快速开始

### 1. 安装 GitHub CLI

```bash
# macOS
brew install gh

# Windows (PowerShell)
winget install --id GitHub.cli

# Linux
curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo apt update
sudo apt install gh
```

### 2. 登录 GitHub

```bash
gh auth login
```

### 3. 初始化标签

```bash
# Linux/Mac
./scripts/create-todos.sh create-labels

# Windows PowerShell
.\scripts\create-todos.ps1 create-labels
```

### 4. 导入待办事项

```bash
# Linux/Mac
./scripts/create-todos.sh create-all

# Windows PowerShell
.\scripts\create-todos.ps1 create-all
```

---

## Issue 模板

本项目提供以下 Issue 模板：

### 🐛 Bug Report

用于报告框架中的 Bug 或问题。

**使用场景：**

- 功能异常
- 性能问题
- 安全漏洞

### ✨ Feature Request

用于提交新功能建议或改进意见。

**使用场景：**

- 新模块提议
- 功能增强
- API 改进

### 📋 Task

用于创建项目待办任务。

**使用场景：**

- 开发任务
- 文档编写
- 测试覆盖
- 代码重构

---

## 标签体系

### 优先级标签

| 标签                  | 颜色 | 说明          | 响应时间  |
|---------------------|----|-------------|-------|
| `priority-critical` | 🔴 | 阻塞发布或严重 Bug | 24小时内 |
| `priority-high`     | 🟠 | 重要功能或明显问题   | 本周内   |
| `priority-medium`   | 🟡 | 一般改进或优化     | 本月内   |
| `priority-low`      | 🟢 | 可选改进或低优先级   | 按需处理  |

### 状态标签

| 标签             | 颜色 | 说明             |
|----------------|----|----------------|
| `status-todo`  | 🔵 | 待处理，尚未开始       |
| `status-doing` | 🟡 | 进行中，已分配或有关联 PR |
| `status-done`  | 🟢 | 已完成，已关闭        |

### 类别标签

| 标签                  | 颜色 | 说明     |
|---------------------|----|--------|
| `category-core`     | 🔵 | 核心功能开发 |
| `category-test`     | 🟣 | 测试相关   |
| `category-docs`     | 🔵 | 文档完善   |
| `category-perf`     | ⚪  | 性能优化   |
| `category-security` | 🔴 | 安全相关   |

### 类型标签

| 标签              | 颜色 | 说明     |
|-----------------|----|--------|
| `bug`           | 🔴 | Bug 修复 |
| `enhancement`   | 🟢 | 功能增强   |
| `task`          | 🟢 | 待办任务   |
| `documentation` | 🔵 | 文档     |

---

## 工作流自动化

### 自动标签管理

当 Issue 状态变化时，系统会自动更新标签：

| 触发条件     | 自动操作                    |
|----------|-------------------------|
| Issue 创建 | 添加 `status-todo` 和默认优先级 |
| Issue 分配 | 更新为 `status-doing`      |
| Issue 关闭 | 更新为 `status-done`       |

### PR 与 Issue 关联

在 PR 描述中使用以下关键字可自动关联 Issue：

```markdown
Fixes #123          # PR 合并时自动关闭 Issue 123
Closes #456         # PR 合并时自动关闭 Issue 456
Resolves #789       # PR 合并时自动关闭 Issue 789

Related to #100     # 仅关联，不自动关闭
```

关联后，系统会自动：

1. 更新 Issue 状态为 `status-doing`
2. PR 合并时自动关闭 Issue
3. 在 Issue 中添加完成评论

---

## 最佳实践

### 创建 Issue

**标题格式：**

```
[TASK] 简短描述任务内容
[BUG] 模块名: 问题简述
[FEATURE] 新功能名称
```

**描述模板：**

```markdown
## 任务描述
清晰描述需要完成的工作

## 完成标准
- [ ] 标准 1
- [ ] 标准 2
- [ ] 标准 3

## 相关链接
- 相关 Issue: #123
- 相关 PR: #456
- 设计文档: [链接]

## 补充说明
其他需要说明的信息
```

### Issue 管理

1. **及时更新状态**
    - 开始处理时分配给自己
    - 完成后关闭 Issue
    - 定期清理过期 Issue

2. **合理使用标签**
    - 创建时选择正确的优先级
    - 根据进展更新状态标签
    - 使用类别标签便于筛选

3. **关联相关信息**
    - 关联相关的 Issue 和 PR
    - 引用相关文档或设计
    - 记录决策和讨论

### 查询和筛选

**常用查询：**

```bash
# 查看高优先级未完成任务
gh issue list --label "priority-high,status-todo"

# 查看我负责的任务
gh issue list --assignee @me --state open

# 查看测试相关任务
gh issue list --label "category-test"

# 查看本周计划
gh issue list --label "priority-high" --limit 20
```

**GitHub Web 界面搜索：**

```
is:issue is:open label:priority-high label:status-todo
is:issue assignee:username is:open
is:issue label:category-docs created:>2024-01-01
```

---

## 批量导入工具

### 命令参考

```bash
# 显示帮助
./scripts/create-todos.sh help

# 列出 TODO.md 中的所有任务
./scripts/create-todos.sh list-todos

# 创建标签（初始化）
./scripts/create-todos.sh create-labels

# 批量创建所有待办（交互式）
./scripts/create-todos.sh create-all

# 批量创建所有待办（自动确认）
./scripts/create-todos.sh create-all -y

# 指定仓库
./scripts/create-todos.sh -r owner/repo create-all
```

### 环境变量

```bash
# 设置目标仓库
export GITHUB_REPO="yourusername/carlos-framework"

# 设置访问令牌（可选，gh CLI 已登录时可不设置）
export GITHUB_TOKEN="ghp_xxxxxxxxxxxx"

# 然后运行脚本
./scripts/create-todos.sh create-all
```

---

## 项目看板（可选）

GitHub Projects 可用于可视化任务管理：

### 设置步骤

1. 进入仓库主页
2. 点击 **Projects** 标签
3. 点击 **New Project**
4. 选择 **Board** 模板
5. 配置列：
    - 📋 Todo (`status-todo`)
    - 🔄 In Progress (`status-doing`)
    - ✅ Done (`status-done`)

### 自动化规则

1. 点击项目设置 ⚙️
2. 进入 **Workflows**
3. 启用以下自动化：
    - "Auto-add to project" - 自动添加新 Issue
    - "Auto-archive items" - 自动归档已完成任务
    - "Item added to project" - 设置默认状态

---

## 常见问题

### Q: 如何修改 Issue 模板？

编辑 `.github/ISSUE_TEMPLATE/` 目录下的 `.yml` 文件。

### Q: 如何添加自定义标签？

```bash
gh label create "自定义标签" --color "ff0000" --description "标签描述"
```

### Q: 如何批量更新 Issue 标签？

```bash
# 为所有高优先级任务添加关注
gh issue list --label "priority-high" --json number | \
  jq -r '.[].number' | \
  xargs -I {} gh issue edit {} --add-label "needs-attention"
```

### Q: 如何导出 Issue 列表？

```bash
gh issue list --state all --limit 100 --json number,title,state,labels | \
  jq -r '.[] | "[#\(.number)] \(.title) [\(.state)]"'
```

---

## 参考链接

- [GitHub Issues 文档](https://docs.github.com/en/issues)
- [GitHub CLI 手册](https://cli.github.com/manual/)
- [GitHub Projects 指南](https://docs.github.com/en/issues/planning-and-tracking-with-projects)
- [项目 TODO.md](../TODO.md)
