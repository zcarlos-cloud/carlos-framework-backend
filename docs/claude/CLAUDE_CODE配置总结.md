# Carlos Framework - 配置总结

本文档总结了为 Carlos Framework 项目推荐和安装的所有 MCP 和 Skill 配置。

---

## 📦 已安装的 Skills

### 项目专用 Skills（2个）

| Skill                       | 用途                      | 文件位置                                        |
|-----------------------------|-------------------------|---------------------------------------------|
| `carlos-framework-standard` | Carlos Framework 后端编码规范 | `.claude/skills/carlos-framework-standard/` |
| `carlos-ui-standard`        | Carlos Framework 前端编码规范 | `.claude/skills/carlos-ui-standard/`        |

### 通用 Skills（6个）

| Skill                   | 用途                                                 | 来源  |
|-------------------------|----------------------------------------------------|-----|
| `java-coding-standards` | Java 编码规范（命名、不可变性、Optional、Stream）                 | ECC |
| `springboot-patterns`   | Spring Boot 架构模式、REST API 设计、分层服务                  | ECC |
| `springboot-security`   | Spring Security 最佳实践（认证/授权、CSRF、安全头）               | ECC |
| `springboot-tdd`        | Spring Boot TDD 开发（JUnit 5、Mockito、Testcontainers） | ECC |
| `docker-patterns`       | Docker 和 Docker Compose 模式                         | ECC |
| `e2e-testing`           | Playwright E2E 测试模式                                | ECC |

### Skills 使用场景

#### 后端开发

```
# 编写 Java 代码时自动应用
- carlos-framework-standard（项目规范）
- java-coding-standards（通用规范）
- springboot-patterns（架构模式）

# 安全相关代码
- springboot-security

# TDD 开发
- springboot-tdd

# 容器化部署
- docker-patterns
```

#### 前端开发

```
# 编写 Vue/TypeScript 代码时自动应用
- carlos-ui-standard

# E2E 测试
- e2e-testing
```

---

## 🔌 已配置的 MCP 服务器

### MCP 列表（7个）

| MCP                   | 状态     | 用途                     | 环境变量           |
|-----------------------|--------|------------------------|----------------|
| `filesystem`          | ✅ 可用   | 文件系统访问                 | 无              |
| `github`              | ⚠️ 需配置 | GitHub API 访问          | `GITHUB_TOKEN` |
| `fetch`               | ✅ 可用   | HTTP 请求                | 无              |
| `sequential-thinking` | ✅ 可用   | 复杂问题分析和思维链             | 无              |
| `puppeteer`           | ✅ 可用   | 浏览器自动化                 | 无              |
| `playwright`          | ✅ 可用   | 浏览器自动化（备选）             | 无              |
| `mysql`               | ⚠️ 需配置 | MySQL 数据库访问（OLTP）      | `MYSQL_*`      |
| `clickhouse`          | ⚠️ 需配置 | ClickHouse 数据库访问（OLAP） | `CLICKHOUSE_*` |

### 环境变量配置

**Windows PowerShell（临时）：**

```powershell
# GitHub Token（在 https://github.com/settings/tokens 生成）
$env:GITHUB_TOKEN = "ghp_your_token_here"

# MySQL 数据库连接（用于 OLTP 业务数据）
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

**Windows PowerShell（永久）：**

```powershell
[Environment]::SetEnvironmentVariable("GITHUB_TOKEN", "ghp_your_token_here", "User")

# MySQL 环境变量
[Environment]::SetEnvironmentVariable("MYSQL_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("MYSQL_PORT", "3306", "User")
[Environment]::SetEnvironmentVariable("MYSQL_USER", "root", "User")
[Environment]::SetEnvironmentVariable("MYSQL_PASSWORD", "your_password", "User")
[Environment]::SetEnvironmentVariable("MYSQL_DATABASE", "carlos", "User")

# ClickHouse 环境变量
[Environment]::SetEnvironmentVariable("CLICKHOUSE_HOST", "localhost", "User")
[Environment]::SetEnvironmentVariable("CLICKHOUSE_PORT", "8123", "User")
[Environment]::SetEnvironmentVariable("CLICKHOUSE_USER", "default", "User")
[Environment]::SetEnvironmentVariable("CLICKHOUSE_PASSWORD", "your_password", "User")
[Environment]::SetEnvironmentVariable("CLICKHOUSE_DATABASE", "default", "User")
```

### MCP 使用场景

#### 开发调研

```
# 技术搜索（使用 GitHub 搜索代替已弃用的 brave-search）
使用 github MCP 搜索 "spring-boot-starter best practices"

# 复杂问题分析
使用 sequential-thinking MCP 分析微服务架构设计方案
```

#### API 测试

```
# 测试后端接口
使用 fetch MCP 测试 http://localhost:8080/api/v1/users

# 健康检查
使用 fetch MCP 获取 http://localhost:8080/actuator/health
```

#### 前端测试

```
# 页面截图
使用 playwright MCP 访问 http://localhost:5173 并截图

# E2E 测试
使用 puppeteer MCP 测试登录流程
```

#### 文件操作

```
# 列出模块
使用 filesystem MCP 列出 carlos-spring-boot 目录下的所有 starter 模块

# 批量生成代码
使用 filesystem MCP 为每个模块生成 README.md
```

#### 数据库操作

```
# MySQL - OLTP 业务数据查询
使用 mysql MCP 查询 org_user 表的结构
使用 mysql MCP 执行 SELECT count(*) FROM sys_menu

# ClickHouse - OLAP 数据分析
使用 clickhouse MCP 查询日志表的统计数据
使用 clickhouse MCP 执行聚合查询分析用户行为
```

---

## 🤖 已配置的 Agents

### 项目专用 Agents（12个）

| Agent                        | 用途          | 调用时机                     |
|------------------------------|-------------|--------------------------|
| `@carlos-planner`            | 功能规划助手      | 开发新业务模块或复杂功能前            |
| `@carlos-api-designer`       | API 设计专家    | 设计新接口、新实体 CRUD 时         |
| `@carlos-db-designer`        | 数据库设计专家     | 设计新表结构、审查 Schema 变更时     |
| `@carlos-module-scaffolder`  | 模块脚手架生成器    | 新建业务模块时                  |
| `@carlos-build-resolver`     | 构建错误修复专家    | Maven 构建失败时              |
| `@carlos-security-reviewer`  | 安全审查专家      | 代码提交前强制调用                |
| `@carlos-integration-tester` | 集成测试专家      | 业务模块开发完成后                |
| `@carlos-doc-updater`        | 文档更新助手      | 模块重大变更、新功能上线后            |
| `@java-reviewer`             | Java 代码审查专家 | 编写或修改 Java 代码后           |
| `@vue-reviewer`              | 前端代码审查专家    | 编写或修改 Vue/TypeScript 代码后 |
| `@springboot-tdd`            | TDD 助手      | 新增功能或修复 Bug 时            |
| `@api-connector`             | 前后端对接助手     | 新增或修改 API 对接时            |

### 全局 Agents（可用）

| Agent                   | 用途               |
|-------------------------|------------------|
| `@planner`              | 通用规划专家           |
| `@architect`            | 软件架构专家           |
| `@code-reviewer`        | 通用代码审查           |
| `@security-reviewer`    | 通用安全审查           |
| `@tdd-guide`            | 通用 TDD 指导        |
| `@doc-updater`          | 通用文档更新           |
| `@build-error-resolver` | 通用构建错误修复         |
| `@database-reviewer`    | PostgreSQL 数据库专家 |
| `@e2e-runner`           | E2E 测试运行器        |

---

## 📝 快捷命令（8个）

| 命令                | 用途       |
|-------------------|----------|
| `/build`          | 构建后端全部模块 |
| `/build-frontend` | 构建前端项目   |
| `/test`           | 运行后端单元测试 |
| `/gen-route`      | 生成前端路由配置 |
| `/new-module`     | 新建业务模块   |
| `/new-page`       | 新增前端页面   |
| `/update-doc`     | 更新项目文档   |

---

## 🎯 推荐工作流

### 新建业务模块（完整流程）

```bash
# 1. 规划
@carlos-planner 我需要设计一个订单管理模块

# 2. 数据库设计
@carlos-db-designer 设计订单表结构

# 3. 生成脚手架
@carlos-module-scaffolder 请生成 carlos-order 模块

# 4. API 设计
@carlos-api-designer 设计订单 CRUD 接口

# 5. TDD 开发（自动应用 springboot-tdd skill）
@springboot-tdd 实现订单创建功能

# 6. 代码审查（自动应用 java-coding-standards skill）
@java-reviewer 审查订单模块代码

# 7. 安全审查（自动应用 springboot-security skill）
@carlos-security-reviewer 审查订单模块代码

# 8. 集成测试
@carlos-integration-tester 生成订单模块集成测试

# 9. 构建验证
/build

# 10. 更新文档
/update-doc 新增订单模块文档
```

### 前端新增页面

```bash
# 1. 创建页面（自动应用 carlos-ui-standard skill）
/new-page

# 2. 生成路由
/gen-route

# 3. 前后端对接
@api-connector 根据订单接口生成前端 service 层代码

# 4. 代码审查
@vue-reviewer 审查新页面代码

# 5. E2E 测试（自动应用 e2e-testing skill）
使用 playwright MCP 测试订单页面功能

# 6. 构建验证
/build-frontend
```

### Bug 修复

```bash
# 1. TDD 流程修复（自动应用 springboot-tdd skill）
@springboot-tdd 修复用户登录时的空指针异常

# 2. 代码审查（自动应用 java-coding-standards skill）
@java-reviewer 审查修复代码

# 3. 安全审查（如涉及用户输入）
@carlos-security-reviewer 审查修复代码

# 4. 运行测试
/test

# 5. 构建验证
/build
```

---

## 🔧 配置文件位置

| 配置       | 文件路径                    |
|----------|-------------------------|
| MCP 配置   | `.claude/mcp.json`      |
| 权限配置     | `.claude/settings.json` |
| Skills   | `.claude/skills/`       |
| Agents   | `.claude/agents/`       |
| Commands | `.claude/commands/`     |

---

## 📚 相关文档

| 文档                                                                                              | 说明                         |
|-------------------------------------------------------------------------------------------------|----------------------------|
| [CLAUDE_CODE使用指南.md](./CLAUDE_CODE使用指南.md)                                                      | Agent、Skill、Command 完整使用指南 |
| [MCP使用指南.md](./MCP使用指南.md)                                                                      | MCP 详细使用说明和故障排查            |
| [CLAUDE.md](../CLAUDE.md)                                                                       | 项目主文档                      |
| [memory/MEMORY.md](../.claude/projects/D--ide-project-carlos-carlos-framework/memory/MEMORY.md) | 项目记忆文档                     |

---

## ✅ 配置验证清单

### 必需配置

- [x] MCP 配置文件已创建（`.claude/mcp.json`）
- [x] Skills 已安装（8个）
- [x] Agents 已配置（12个项目专用）
- [x] Commands 已配置（8个）
- [x] 权限配置已优化（`.claude/settings.json`）

### 可选配置

- [ ] `GITHUB_TOKEN` 环境变量（用于 GitHub MCP）
- [ ] `MYSQL_*` 环境变量（用于 MySQL MCP）
- [ ] `CLICKHOUSE_*` 环境变量（用于 ClickHouse MCP）

### 测试验证

- [x] MCP 服务器安装测试通过
- [x] Skills 文件完整性检查通过
- [x] Agents 配置格式检查通过
- [x] Commands 配置格式检查通过

---

## 🚀 下一步

1. **设置环境变量**（可选但推荐）：
   ```powershell
   $env:GITHUB_TOKEN = "ghp_your_token_here"
   ```

2. **测试 MCP 功能**：
   ```
   使用 filesystem MCP 列出 carlos-spring-boot 目录下的所有模块
   ```

3. **开始使用**：
    - 新建模块：`/new-module`
    - 新建页面：`/new-page`
    - 构建项目：`/build`

---

*配置完成时间：2026-03-22*
*配置版本：v1.0*
