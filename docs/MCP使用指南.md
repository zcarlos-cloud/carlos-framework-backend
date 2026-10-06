# Carlos Framework - MCP 使用指南

本文档介绍项目配置的 MCP (Model Context Protocol) 服务器及其使用方法。

---

## 目录

- [MCP 简介](#mcp-简介)
- [配置环境变量](#配置环境变量)
- [MCP 服务器列表](#mcp-服务器列表)
- [使用示例](#使用示例)
- [故障排查](#故障排查)

---

## MCP 简介

MCP (Model Context Protocol) 是 Anthropic 推出的开放协议，用于标准化 AI 助手与外部工具、数据源和服务的连接。通过 MCP，Claude
Code 可以：

- 访问文件系统
- 调用外部 API
- 查询数据库
- 执行浏览器自动化
- 搜索网络信息

---

## 配置环境变量

在使用 MCP 之前，需要配置以下环境变量。在 Windows PowerShell 中：

```powershell
# GitHub Token（用于 GitHub MCP）
# 在 https://github.com/settings/tokens 生成，需要 repo 权限
$env:GITHUB_TOKEN = "ghp_xxxxxxxxxxxxxxxx"

# Brave API Key（用于搜索 MCP）
# 在 https://brave.com/search/api/ 申请
$env:BRAVE_API_KEY = "BSxxxxxxxxxxxxxxxx"

# MySQL 数据库连接（用于 OLTP 业务数据查询）
$env:MYSQL_HOST = "localhost"
$env:MYSQL_PORT = "3306"
$env:MYSQL_USER = "root"
$env:MYSQL_PASSWORD = "your_password"
$env:MYSQL_DATABASE = "carlos"

# ClickHouse 数据库连接（用于 OLAP 数据分析）
$env:CLICKHOUSE_HOST = "localhost"
$env:CLICKHOUSE_PORT = "8123"
$env:CLICKHOUSE_USER = "default"
$env:CLICKHOUSE_PASSWORD = "your_password"
$env:CLICKHOUSE_DATABASE = "default"
```

或者在 Windows 系统环境变量中永久设置：

1. 打开"系统属性" → "环境变量"
2. 在用户变量或系统变量中添加以上变量

---

## MCP 服务器列表

### 1. filesystem - 文件系统访问

**用途：** 代码生成、文件操作、项目结构浏览

**适用场景：**

- 批量生成代码文件
- 读取/写入配置文件
- 遍历项目目录结构
- 检查文件内容

**使用示例：**

```
请使用 filesystem MCP 列出 carlos-spring-boot 目录下的所有 starter 模块
```

```
使用 filesystem MCP 读取 carlos-dependencies/pom.xml 中的版本号
```

---

### 2. github - GitHub API 访问

**用途：** 代码搜索、PR 管理、Issue 追踪

**适用场景：**

- 搜索开源项目的实现参考
- 查看 GitHub 上的代码示例
- 管理项目 Issues 和 PRs
- 查看代码审查历史

**使用示例：**

```
使用 github MCP 搜索 spring-boot-starter 的最佳实践实现
```

```
使用 github MCP 查看本项目的最近提交历史
```

**需要环境变量：** `GITHUB_TOKEN`

---

### 3. fetch - HTTP 请求

**用途：** 测试后端 API、获取外部资源

**适用场景：**

- 测试 Controller 接口
- 调用第三方 API
- 获取在线文档
- 验证服务健康状态

**使用示例：**

```
使用 fetch MCP 测试 http://localhost:8080/actuator/health 健康检查接口
```

```
使用 fetch MCP 获取 https://start.spring.io/actuator/info 查看 Spring Boot 版本信息
```

---

### 4. brave-search - 网络搜索

**用途：** 技术调研、查找最佳实践、解决问题

**适用场景：**

- 搜索技术问题的解决方案
- 查找框架的最新版本信息
- 调研开源工具
- 查询 API 文档

**使用示例：**

```
使用 brave-search MCP 搜索 "Spring Boot 3.5 MyBatis-Plus 配置最佳实践"
```

```
使用 brave-search MCP 搜索 "MyBatis-Plus join 查询用法"
```

**需要环境变量：** `BRAVE_API_KEY`

---

### 5. playwright - 浏览器自动化

**用途：** 前端 E2E 测试、页面截图、UI 验证

**适用场景：**

- 测试前端页面功能
- 生成页面截图
- 验证 UI 布局
- 自动化浏览器操作

**使用示例：**

```
使用 playwright MCP 访问 http://localhost:5173 并截图登录页面
```

```
使用 playwright MCP 测试前端登录流程：访问页面 -> 输入用户名密码 -> 点击登录 -> 验证跳转
```

---

### 6. mysql - MySQL 数据库访问（OLTP）

**用途：** 业务数据查询、表结构检查、数据验证

**适用场景：**

- 查询业务数据库中的数据
- 检查表结构和索引
- 验证数据一致性
- 执行数据迁移验证
- CRUD 操作验证

**使用示例：**

```
使用 mysql MCP 查询 org_user 表的结构
```

```
使用 mysql MCP 执行 SQL：SELECT count(*) FROM sys_menu WHERE status = 1
```

```
使用 mysql MCP 检查 org_user 表的索引
```

**需要环境变量：**

- `MYSQL_HOST`（默认: localhost）
- `MYSQL_PORT`（默认: 3306）
- `MYSQL_USER`（默认: root）
- `MYSQL_PASSWORD`（必需）
- `MYSQL_DATABASE`（默认: carlos）

---

### 7. clickhouse - ClickHouse 数据库访问（OLAP）

**用途：** 数据分析、日志查询、报表统计

**适用场景：**

- 大数据量聚合查询
- 日志分析和统计
- 用户行为分析
- 实时报表生成
- 时序数据查询

**使用示例：**

```
使用 clickhouse MCP 查询日志表的统计数据
```

```
使用 clickhouse MCP 执行聚合查询：
SELECT date, count(*) as cnt
FROM user_behavior_log
WHERE date >= today() - 7
GROUP BY date
ORDER BY date
```

```
使用 clickhouse MCP 分析用户访问趋势
```

**需要环境变量：**

- `CLICKHOUSE_HOST`（默认: localhost）
- `CLICKHOUSE_PORT`（默认: 8123）
- `CLICKHOUSE_USER`（默认: default）
- `CLICKHOUSE_PASSWORD`（必需）
- `CLICKHOUSE_DATABASE`（默认: default）

---

## 使用示例

### 场景1：技术调研 - 查找 MyBatis-Plus 最佳实践

```
使用 brave-search MCP 搜索 "MyBatis-Plus 3.5 多租户实现方案"
```

### 场景2：代码参考 - 查看开源实现

```
使用 github MCP 搜索代码 "BaseServiceImpl MyBatis-Plus"，找到优秀的基类实现参考
```

### 场景3：API 测试 - 验证后端接口

```
使用 fetch MCP 测试本地接口：
POST http://localhost:8080/api/v1/users
Content-Type: application/json

{
  "username": "test",
  "email": "test@example.com"
}
```

### 场景4：前端测试 - 验证页面功能

```
使用 playwright MCP 完成以下测试：
1. 访问 http://localhost:5173/login
2. 输入用户名 admin，密码 admin123
3. 点击登录按钮
4. 验证是否跳转到首页
5. 截图保存结果
```

### 场景5：文件操作 - 批量生成代码

```
使用 filesystem MCP：
1. 列出 carlos-integration/ 下的所有模块
2. 为每个模块生成 README.md 文件（如果不存在）
```

### 场景6：MySQL 数据库检查 - 验证表结构

```
使用 mysql MCP 查询数据库：
1. 列出所有表
2. 检查 org_user 表的字段
3. 验证是否有缺失的索引
```

### 场景7：ClickHouse 数据分析 - 统计报表

```
使用 clickhouse MCP 分析用户行为：
1. 查询最近7天的用户访问量
2. 统计各页面的访问次数
3. 生成访问趋势报表
```

---

## 故障排查

### MCP 无法启动

**问题：** MCP 服务器启动失败

**解决方案：**

1. 检查 Node.js 是否安装：`node --version`
2. 检查 npx 是否可用：`npx --version`
3. 检查 mcp.json 格式是否正确

### 环境变量未设置

**问题：** 提示 `GITHUB_TOKEN not set` 或类似错误

**解决方案：**

```powershell
# 临时设置（当前窗口有效）
$env:GITHUB_TOKEN = "your_token_here"

# 永久设置（需要重启终端）
[Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "your_token_here", "User")
```

### 权限不足

**问题：** filesystem MCP 无法访问某些目录

**解决方案：**

- 确保 Claude Code 有权限访问项目目录
- 检查目录权限设置
- 以管理员身份运行终端

### 网络问题

**问题：** fetch 或 brave-search MCP 超时

**解决方案：**

- 检查网络连接
- 检查代理设置
- 确认 API Key 是否有效

---

## MCP 配置文件

配置文件位置：`.claude/mcp.json`

```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "D:/ide_project/carlos/carlos-framework"]
    },
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": { "GITHUB_PERSONAL_ACCESS_TOKEN": "${GITHUB_TOKEN}" }
    },
    "fetch": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-fetch"]
    },
    "brave-search": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-brave-search"],
      "env": { "BRAVE_API_KEY": "${BRAVE_API_KEY}" }
    },
    "playwright": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-playwright"]
    },
    "mysql": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-mysql"],
      "env": {
        "MYSQL_HOST": "${MYSQL_HOST}",
        "MYSQL_PORT": "${MYSQL_PORT}",
        "MYSQL_USER": "${MYSQL_USER}",
        "MYSQL_PASSWORD": "${MYSQL_PASSWORD}",
        "MYSQL_DATABASE": "${MYSQL_DATABASE}"
      }
    },
    "clickhouse": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-clickhouse"],
      "env": {
        "CLICKHOUSE_HOST": "${CLICKHOUSE_HOST}",
        "CLICKHOUSE_PORT": "${CLICKHOUSE_PORT}",
        "CLICKHOUSE_USER": "${CLICKHOUSE_USER}",
        "CLICKHOUSE_PASSWORD": "${CLICKHOUSE_PASSWORD}",
        "CLICKHOUSE_DATABASE": "${CLICKHOUSE_DATABASE}"
      }
    }
  }
}
```

---

## 快速参考

| MCP          | 用途                   | 必需环境变量          |
|--------------|----------------------|-----------------|
| filesystem   | 文件操作                 | 无               |
| github       | GitHub API           | `GITHUB_TOKEN`  |
| fetch        | HTTP 请求              | 无               |
| brave-search | 网络搜索                 | `BRAVE_API_KEY` |
| playwright   | 浏览器自动化               | 无               |
| mysql        | MySQL 数据库（OLTP）      | `MYSQL_*`       |
| clickhouse   | ClickHouse 数据库（OLAP） | `CLICKHOUSE_*`  |

---

*最后更新：2026-03-22*
