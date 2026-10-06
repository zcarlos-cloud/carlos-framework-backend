# Carlos Framework - Claude Code 完全指南

> 版本：v2.0 | 项目：carlos-framework-backend | 日期：2026-05-23

本文档汇总了本项目配置的所有 Agent、Skill、Command 的使用方法，以及从入门到精通的完整成长路径。

## 目录

- [导读：如何阅读本书](#导读：如何阅读本书)
- [第一部分 核心工具（每日必用）](#第一部分-核心工具每日必用)
    - [第1章 快捷命令 Commands](#第1章-快捷命令-Commands)
    - [第2章 专用智能体 Agents](#第2章-专用智能体-Agents)
    - [第3章 编码规范 Skills](#第3章-编码规范-Skills)
- [第二部分 能力扩展](#第二部分-能力扩展)
    - [第4章 外部工具集成 MCP](#第4章-外部工具集成-MCP)
    - [第5章 Skills 完整参考手册](#第5章-Skills-完整参考手册)
- [第三部分 实战工作流](#第三部分-实战工作流)
    - [第6章 典型开发场景](#第6章-典型开发场景)
    - [第7章 开发流程速查](#第7章-开发流程速查)
- [第四部分 团队协作](#第四部分-团队协作)
    - [第8章 Agent Team 协作体系](#第8章-Agent-Team-协作体系)
- [第五部分 成长路径](#第五部分-成长路径)
    - [第9章 Claude Code 八阶能力进阶](#第9章-Claude-Code-八阶能力进阶)
    - [第10章 Carlos Framework 项目实战精通计划](#第10章-Carlos-Framework-项目实战精通计划)
- [附录A 一页纸速查卡](#附录A-一页纸速查卡)

---

## 导读：如何阅读本书

### 如果你今天刚开始使用

→ 先读 **第一部分**（5分钟上手），然后翻到 **附录A**（一页纸速查卡）

### 如果你要开发新功能

→ 直接跳到 **第三部分 实战工作流**，按场景执行

### 如果你想系统提升

→ 从 **第五部分** 开始，按阶段逐步进阶

---

# 第一部分 核心工具（每日必用）

---

## 第1章 快捷命令 Commands

Commands 是可以通过 `/命令名` 直接调用的快捷指令，无需 Agent 介入。

| 命令                | 用途       | 调用方式              |
|-------------------|----------|-------------------|
| `/build`          | 构建后端全部模块 | `/build`          |
| `/build-frontend` | 构建前端项目   | `/build-frontend` |
| `/test`           | 运行后端单元测试 | `/test`           |
| `/gen-route`      | 生成前端路由配置 | `/gen-route`      |
| `/new-module`     | 新建业务模块   | `/new-module`     |
| `/new-page`       | 新增前端页面   | `/new-page`       |
| `/update-doc`     | 更新项目文档   | `/update-doc`     |

### 命令详解

#### `/build` - 构建后端

构建 carlos-framework-backend 全部模块（跳过测试）。

```bash
cd D:/ide_project/carlos/carlos-framework
mvn clean install -DskipTests
```

**常用变体：**

```bash
# 包含测试
mvn clean install

# 部署到公共 Nexus（zcarlos.com:8081）
mvn clean deploy -P carlos-public

# 部署到私有 Nexus（192.168.3.30:8081）
mvn clean deploy -P carlos-private

# 构建指定模块
cd carlos-spring-boot/carlos-spring-boot-starter-redis
mvn clean install -DskipTests
```

**构建顺序（失败时参考）：**

1. carlos-dependencies
2. carlos-parent
3. carlos-commons（carlos-utils）
4. carlos-spring-boot（23 个模块，含 carlos-spring-boot-core）
5. carlos-integration（auth → system → org → audit → message → license → tools）
6. carlos-samples

---

#### `/build-frontend` - 构建前端

构建 carlos-ui 前端项目。

**执行步骤：**

1. 进入前端目录：`cd D:/ide_project/carlos/carlos-framework/carlos-ui`
2. 确认依赖已安装（如未安装则运行 `pnpm install`）
3. 执行构建：`pnpm build`
4. 显示构建产物路径

**可选参数：**

- 测试环境构建：`pnpm build:test`
- 仅类型检查：`pnpm typecheck`
- 代码检查：`pnpm lint`

**注意事项：**

- 使用 pnpm 工作空间，禁止使用 npm 或 yarn
- 构建前先运行 `pnpm typecheck` 确保无类型错误
- 构建前先运行 `pnpm lint` 确保代码规范

---

#### `/test` - 运行测试

运行后端单元测试并生成覆盖率报告。

```bash
cd D:/ide_project/carlos/carlos-framework

# 运行全部测试
mvn test

# 运行指定测试类
mvn test -Dtest=OrgUserServiceTest

# 运行指定测试方法
mvn test -Dtest=OrgUserServiceTest#should_create_user_when_params_valid

# 运行集成测试
mvn verify

# 生成 JaCoCo 覆盖率报告
mvn test jacoco:report
# 报告位置：target/site/jacoco/index.html
```

**覆盖率目标：**

- Line Coverage ≥ **80%**
- Branch Coverage ≥ **70%**

---

#### `/gen-route` - 生成前端路由

根据 `src/views/` 目录文件结构，通过 Elegant Router 自动生成路由配置。

**执行步骤：**

1. 进入前端目录：`cd D:/ide_project/carlos/carlos-framework/carlos-ui`
2. 执行：`pnpm gen-route`
3. 说明生成了哪些新路由文件

**使用场景：**

- 在 `src/views/` 下新增了页面文件
- 修改了页面文件的命名或目录结构
- 删除了页面文件

**路由命名规范：**

- `src/views/system/user/index.vue` → 路由名 `system_user`
- `src/views/system/user/[id].vue` → 动态路由 `system_user-detail`

---

#### `/new-module` - 新建业务模块

在 `carlos-integration/` 下创建一个新的业务模块，遵循标准分层架构。

**执行步骤：**

1. 确认模块名称（如 `carlos-order`）
2. 创建目录结构：
   ```
   carlos-integration/carlos-{name}/
   ├── carlos-{name}-api/          # Feign 接口 + AO/Param
   └── carlos-{name}-bus/          # 业务实现
       ├── apiimpl/
       ├── config/
       ├── controller/
       ├── convert/
       ├── manager/impl/
       ├── mapper/
       ├── pojo/dto|entity|enums|param|vo/
       └── service/impl/
   ```
3. 创建 `pom.xml`（继承 `carlos-integration`，版本 `${revision}`）
4. 在 `carlos-integration/pom.xml` 中注册模块
5. 在 `carlos-dependencies/pom.xml` 中添加依赖管理条目

**注意事项：**

- 严格遵守 `.claude/skills/carlos-framework-standard/SKILL.md` 中的编码规范
- 模块命名：`carlos-{功能名}`
- 包路径：`com.carlos.{模块名}`
- 创建后立即编写对应测试类

---

#### `/new-page` - 新增页面

在 `src/views/` 下创建新页面，并生成对应路由和 service 层代码。

**执行步骤：**

1. **确认页面信息**：模块名、页面名、对应后端接口
2. **创建页面文件**：
   ```
   src/views/{module}/{page}/
   ├── index.vue          # 页面主文件
   └── modules/           # 页面子组件（表单、弹窗等）
       ├── search-form.vue
       └── edit-modal.vue
   ```
3. **创建 service 层**：
   ```
   src/service/api/{module}/{page}.ts
   ```
4. **生成路由**：`pnpm gen-route`
5. **类型检查**：`pnpm typecheck`

**页面模板：**

```vue

<script setup lang="ts">
  import {ref, onMounted} from 'vue';

  defineOptions({name: '{ModulePage}'});

  const loading = ref(false);

  onMounted(() => {
    // 初始化
  });
</script>

<template>
  <div class="h-full">
    <!-- 页面内容 -->
  </div>
</template>
```

**注意事项：**

- 文件名使用 kebab-case（如 `user-list`）
- 组件名使用 PascalCase（如 `SystemUserList`）
- 新增页面后必须运行 `pnpm gen-route`
- 后端菜单需同步配置才能在侧边栏显示

---

#### `/update-doc` - 更新项目文档

更新 CLAUDE.md、MEMORY.md 和相关模块文档，确保文档与代码同步。

**执行步骤：**

1. 检查最近的代码变更（git diff）
2. 识别需要更新的文档
3. 调用 @carlos-doc-updater 执行文档更新

**更新范围：**

- **CLAUDE.md**: 项目主文档、架构变更、模块结构
- **memory/MEMORY.md**: 跨会话快速参考、模块列表
- **模块 README**: 各模块的功能说明和配置
- **SKILL.md**: 编码规范文档

---

## 第2章 专用智能体 Agents

Agents 是专业领域的 AI 助手，通过 `@agent-name` 调用，执行复杂任务。

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

### Agent 详解

#### `@carlos-planner` - 功能规划助手

**职责：** 在开发新业务模块或复杂功能前，输出标准 PRD、模块设计方案、分层任务清单。

**调用时机：**

- 开发新业务模块前
- 复杂功能设计前
- 架构调整前

**输出内容：**

- 需求分析
- 模块设计方案
- 分层任务清单
- 依赖关系梳理
- 风险评估

**使用示例：**

```
@carlos-planner 我需要设计一个订单管理模块，包含订单创建、支付、发货、退款等功能
```

---

#### `@carlos-api-designer` - API 设计专家

**职责：** 设计新接口时，输出完整的 Controller/Service/Manager/Mapper 骨架代码及所有 POJO 对象。

**设计原则：**

1. 严格遵守 Controller → Service → Manager → Mapper 分层
2. Service 层只做业务串联，禁止直接引用 Mapper
3. Manager 层继承 BaseServiceImpl，统一处理 CRUD 和缓存
4. 所有 POJO 使用 Lombok，禁止手写 getter/setter

**输出内容：**

- Controller 层代码
- Service 层代码
- Manager 层代码
- Mapper 层代码
- 所有 POJO 对象（Param、DTO、VO、Entity、AO）

**使用示例：**

```
@carlos-api-designer 设计一个商品管理模块的 CRUD 接口，包含商品创建、查询、更新、删除
```

---

#### `@carlos-db-designer` - 数据库设计专家

**职责：** 设计新表结构、审查 Schema 变更、生成 Migration 脚本。

**表设计规范：**

- 表名：`{module}_{entity}` 蛇形命名（如 `org_user`, `sys_menu`）
- 字段名：蛇形命名小写（如 `user_name`, `create_time`）
- 主键：`id` BIGINT，雪花算法

**输出内容：**

- 建表 SQL
- MyBatis-Plus 实体类
- 索引设计建议
- Migration 脚本（Flyway/Liquibase）

**使用示例：**

```
@carlos-db-designer 设计订单表，包含订单基本信息、金额、状态、关联用户等字段
```

---

#### `@carlos-module-scaffolder` - 模块脚手架生成器

**职责：** 新建业务模块时，自动生成符合规范的 api/bus/boot/cloud 四层子模块完整骨架代码。

**生成内容：**

- 完整的目录结构
- pom.xml（继承关系正确）
- 基础配置文件
- 示例代码文件

**使用示例：**

```
@carlos-module-scaffolder 请生成 carlos-order 模块的脚手架代码
```

---

#### `@carlos-build-resolver` - 构建错误修复专家

**职责：** Maven 构建失败时，诊断编译错误、依赖冲突、插件问题，提供最小化修复方案。

**诊断流程：**

1. 定位错误模块和具体错误信息
2. 分析依赖冲突（使用 `mvn dependency:tree`）
3. 检查版本兼容性
4. 提供修复方案

**使用示例：**

```
@carlos-build-resolver mvn clean install 失败了，请帮我诊断
```

---

#### `@carlos-security-reviewer` - 安全审查专家

**职责：** 代码提交前强制调用，检查 SQL 注入、XSS、硬编码凭据、越权访问、敏感数据泄露、CSRF 等安全问题。

**审查维度：**

- SQL 注入检查
- XSS 防护检查
- 硬编码凭据检查
- 越权访问检查
- 敏感数据泄露检查
- CSRF 防护检查

**使用示例：**

```
@carlos-security-reviewer 请审查 org 模块的最新代码
```

---

#### `@carlos-integration-tester` - 集成测试专家

**职责：** 业务模块开发完成后调用，生成 MockMvc 接口测试、Testcontainers 数据库集成测试，验证 80%+ 测试覆盖率。

**测试层次：**

- 单元测试（JUnit 5 + Mockito）
- Controller 集成测试（MockMvc）
- 数据库集成测试（Testcontainers + MySQL）

**使用示例：**

```
@carlos-integration-tester 请为 carlos-order 模块生成集成测试
```

---

#### `@java-reviewer` - Java 代码审查专家

**职责：** 审查 Spring Boot / MyBatis-Plus 代码，检查分层架构、编码规范、安全问题。

**审查维度：**

- 分层架构合规性
- 数据查询规范
- Redis 缓存规范
- 异常处理规范
- Lombok 使用规范

**使用示例：**

```
@java-reviewer 请审查这段 Service 代码
```

---

#### `@vue-reviewer` - 前端代码审查专家

**职责：** 编写或修改 Vue/TypeScript 代码后自动调用，检查类型安全、组件规范、状态管理、API调用规范、样式规范。

**审查维度：**

- TypeScript 类型安全
- 组件规范
- 状态管理规范
- API 调用规范
- 样式规范

**使用示例：**

```
@vue-reviewer 请审查这个 Vue 组件
```

---

#### `@springboot-tdd` - TDD 助手

**职责：** 新增功能或修复 Bug 时自动调用，强制执行 RED→GREEN→IMPROVE 流程，确保测试覆盖率达到 80%+。

**强制流程：**

1. **RED 阶段** — 先写测试（覆盖 BCDE 原则）
2. **GREEN 阶段** — 实现代码使测试通过
3. **IMPROVE 阶段** — 重构代码，提升质量

**BCDE 原则：**

- **B**order：边界值测试
- **C**orrect：正确的输入，并得到预期的结果
- **D**esign：与设计文档相结合
- **E**rror：强制错误信息输入

**使用示例：**

```
@springboot-tdd 我需要实现用户创建功能，请帮我按照 TDD 流程进行
```

---

## 第3章 编码规范 Skills

Skills 是 Claude Code 的知识库，提供编码规范、最佳实践等指导。

| Skill                       | 用途                    | 调用方式      |
|-----------------------------|-----------------------|-----------|
| `carlos-framework-standard` | Carlos Framework 编码规范 | 自动加载或手动引用 |
| `carlos-ui-standard`        | 前端编码规范                | 自动加载或手动引用 |

### Skill 详解

#### `carlos-framework-standard` - Carlos Framework 编码规范

**内容：**

- 架构分层规范
- 命名规范
- 数据查询规范（强制）
- Redis 缓存规范（强制）
- 异常处理规范（强制）
- Lombok 使用规范
- 属性注入规范
- Service 返回值规范
- 枚举类规范

**关键强制规范：**

| 规范              | 要求                                              | 禁止                    |
|-----------------|-------------------------------------------------|-----------------------|
| **数据查询**        | 使用 MyBatis-Plus + mybatis-plus-join 在 Manager 层 | 严禁直接编写 SQL            |
| **Redis 缓存**    | 在 Manager 层统一实现                                 | Service 层禁止直接操作 Redis |
| **异常处理**        | 使用框架定义异常类（ServiceException 等）                   | 严禁使用 RuntimeException |
| **Lombok**      | 使用注解生成 getter/setter                            | 严禁手写 get/set 方法       |
| **属性注入**        | 使用 @ConfigurationProperties                     | 严禁使用 @Value           |
| **Service 返回值** | 返回 DTO 或 Entity                                 | 严禁返回 VO               |
| **枚举类**         | 实现 BaseEnum，使用 @AppEnum                         | 普通枚举定义                |

**文件位置：** `.claude/skills/carlos-framework-standard/SKILL.md`

---

#### `carlos-ui-standard` - 前端编码规范

**内容：**

- 组件规范
- 类型规范
- 状态管理规范
- API 请求规范
- 样式规范
- 路由规范

**强制规范速查：**

| 规范   | 要求                               | 禁止             |
|------|----------------------------------|----------------|
| 组件语法 | `<script setup>` + TypeScript    | Options API    |
| 类型   | 明确类型，禁止 `any`                    | `any`、`as any` |
| 包管理  | pnpm                             | npm、yarn       |
| 样式   | UnoCSS 工具类 + `<style scoped>`    | 内联样式           |
| 状态   | Pinia store                      | 组件间直接传递复杂状态    |
| 请求   | `src/service/api/` + `@sa/axios` | 直接 axios/fetch |

**文件位置：** `.claude/skills/carlos-ui-standard/SKILL.md`

---

# 第二部分 能力扩展

---

## 第4章 外部工具集成 MCP

MCP（Model Context Protocol）用于扩展 Claude Code 的能力，连接外部工具和服务。本项目已配置 6 个 MCP 服务器。

### MCP 服务器列表

| MCP            | 用途                     | 环境变量            |
|----------------|------------------------|-----------------|
| `filesystem`   | 文件系统访问                 | 无               |
| `github`       | GitHub API 访问          | `GITHUB_TOKEN`  |
| `fetch`        | HTTP 请求                | 无               |
| `brave-search` | 网络搜索                   | `BRAVE_API_KEY` |
| `playwright`   | 浏览器自动化                 | 无               |
| `mysql`        | MySQL 数据库访问（OLTP）      | `MYSQL_*`       |
| `clickhouse`   | ClickHouse 数据库访问（OLAP） | `CLICKHOUSE_*`  |

### 配置环境变量

在使用需要认证的 MCP 之前，配置以下环境变量：

```powershell
# GitHub Token（用于 GitHub MCP）
$env:GITHUB_TOKEN = "ghp_xxxxxxxxxxxxxxxx"

# Brave API Key（用于搜索 MCP）
$env:BRAVE_API_KEY = "BSxxxxxxxxxxxxxxxx"

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

### MCP 使用示例

**文件操作：**

```
使用 filesystem MCP 列出 carlos-spring-boot 目录下的所有 starter 模块
```

**GitHub 搜索：**

```
使用 github MCP 搜索 spring-boot-starter 的最佳实践实现
```

**API 测试：**

```
使用 fetch MCP 测试 http://localhost:8080/actuator/health 健康检查接口
```

**网络搜索：**

```
使用 brave-search MCP 搜索 "Spring Boot 3.5 MyBatis-Plus 配置最佳实践"
```

**前端测试：**

```
使用 playwright MCP 访问 http://localhost:5173 并截图登录页面
```

**MySQL 数据库查询（OLTP）：**

```
使用 mysql MCP 查询 org_user 表的结构
```

**ClickHouse 数据分析（OLAP）：**

```
使用 clickhouse MCP 查询日志表的统计数据
```

### 详细文档

完整 MCP 使用指南请参见：[MCP使用指南.md](./MCP使用指南.md)

### 配置文件位置

`.claude/mcp.json`

---

## 第5章 Skills 完整参考手册

> 以下补充本项目中可用的其他 Skills 及其详细使用场景。

### 快速参考：何时用哪个 skill

| 场景                          | 使用的 skill                                     |
|-----------------------------|-----------------------------------------------|
| 新增功能开发                      | `/plan` → `/tdd` → `/code-review` → `/verify` |
| 写 Controller/Service/Mapper | `/springboot-patterns`                        |
| 配置 Spring Security / OAuth2 | `/springboot-security`                        |
| 写单元测试 / 集成测试                | `/springboot-tdd`                             |
| 提交前验证                       | `/springboot-verification`                    |
| 设计 REST API 接口              | `/api-design`                                 |
| 数据库 Schema 变更               | `/database-migrations`                        |
| JPA 实体 / 查询优化               | `/jpa-patterns`                               |
| Docker / 微服务部署              | `/docker-patterns` + `/deployment-patterns`   |
| 安全审查                        | `/security-review`                            |
| 前端 Vue 3 开发                 | `/frontend-patterns` + `/coding-standards`    |
| 会话结束前保存模式                   | `/learn`                                      |

### 日常开发工作流

```
1. /plan          → 制定实现计划，等待确认
2. /search-first  → 搜索现有库/模式，避免重复造轮子
3. /tdd           → 先写测试（RED），再实现（GREEN），再重构
4. /code-review   → 代码审查
5. /springboot-verification → 构建+测试+安全扫描
6. /learn         → 提取本次会话的可复用模式
```

### 后端 Spring Boot Skills

**`/springboot-patterns`** — 写 Controller、Service、Manager、Mapper 任何一层时

```bash
/springboot-patterns 帮我实现用户分页查询接口，包含 Controller → Service → Manager → Mapper 完整链路
```

覆盖内容：分层架构、MyBatis-Plus 查询模式、Redis 缓存（Manager 层）、统一异常处理、分页响应格式

**`/springboot-security`** — 涉及认证、授权、OAuth2、JWT、接口权限时

```bash
/springboot-security 审查 carlos-auth 模块的 OAuth2 配置是否符合安全规范
```

覆盖内容：Spring Security 配置、OAuth2 Authorization Server、JWT token 验证、方法级权限、CSRF / XSS 防护、密钥管理

**`/springboot-tdd`** — 新增功能、修复 Bug、重构代码时

```bash
/springboot-tdd 为 UserService.createUser() 方法编写测试，要求覆盖正常流程和异常流程
```

覆盖内容：JUnit 5 + Mockito 单元测试、MockMvc 接口测试、Testcontainers 集成测试、JaCoCo 覆盖率（目标 80%+）

**`/springboot-verification`** — 提交代码前、PR 前、发版前

执行顺序：`mvn clean install` 构建验证 → 静态分析 → 测试+覆盖率报告 → 安全扫描 → Diff 审查

**`/java-coding-standards`** — 代码审查时，或不确定命名/结构规范时

覆盖内容：命名规范（Param/DTO/VO/Entity/AO）、Lombok 使用规范、Optional 使用、Stream API 最佳实践、不可变对象原则

**`/jpa-patterns`** — 设计实体关系、优化查询、处理事务时

覆盖内容：实体设计与关联关系、mybatis-plus-join 查询、事务边界、分页与索引优化、连接池配置

### API 设计 Skills

**`/api-design`** — 设计新接口、评审接口规范时

覆盖内容：资源命名规范、HTTP 状态码使用、统一 `Result<T>` 响应格式、分页参数设计、错误响应格式、接口版本管理

### 数据库 Skills

**`/database-migrations`** — 修改数据库 Schema、添加字段、创建索引时

覆盖内容：Flyway / Liquibase 迁移脚本、零停机迁移策略、回滚方案、数据迁移安全性

**`/postgres-patterns`** — SQL 优化、索引设计、复杂查询时

### 部署 & 运维 Skills

**`/docker-patterns`** — 编写 Dockerfile、docker-compose、容器化微服务时

覆盖内容：多阶段构建、Spring Boot 容器最佳实践、服务间网络配置、数据卷策略、容器安全

**`/deployment-patterns`** — 配置 CI/CD、设计发布流程、生产就绪检查时

覆盖内容：GitHub Actions / Jenkins 流水线、健康检查配置、回滚策略、生产就绪检查清单、Nexus 制品发布

### 安全 Skills

**`/security-review`** — 实现认证功能、处理用户输入、创建 API 端点时

覆盖内容：SQL 注入防护、XSS 防护、认证授权验证、敏感数据处理、错误信息安全

**`/security-scan`** — 定期安全审计，或怀疑配置有安全问题时

扫描范围：`.claude/` 目录配置、settings.json、MCP 服务器、hooks

### 前端 Vue 3 Skills

**`/frontend-patterns`** — 开发 Vue 3 组件、Pinia store、路由配置时

覆盖内容：Vue 3 Composition API 模式、Pinia 状态管理、VueUse 组合式工具、Ant Design Vue 组件使用、UnoCSS 样式

**`/coding-standards`** — TypeScript 代码审查、前端规范检查时

### 工作流 & 效率 Skills

| Skill                             | 触发时机           | 说明                    |
|-----------------------------------|----------------|-----------------------|
| `/plan`                           | 任何复杂功能开始前      | 必须等用户确认再动代码           |
| `/search-first`                   | 准备写新功能前        | 先搜索是否有现成方案            |
| `/tdd`                            | 所有新功能、Bug 修复   | RED → GREEN → IMPROVE |
| `/learn` / `/continuous-learning` | 每次会话结束前        | 提取本次发现的可复用模式          |
| `/blueprint`                      | 跨多个会话的大型功能规划   | 输出完整 PRD 和任务列表        |
| `/strategic-compact`              | 长会话中途，上下文接近限制时 | 手动压缩上下文               |

### 完整开发场景示例

**新增业务模块（如 carlos-notice）：**

```bash
/plan 实现 carlos-notice 通知模块
/search-first Spring Boot 通知模块最佳实践
/api-design 设计通知模块 REST 接口
/tdd 实现 NoticeService
/security-review 审查通知接口
/springboot-verification
/learn
```

**修复 Bug：**

```bash
/springboot-tdd 为这个 Bug 写复现测试
/springboot-verification
/code-review
```

**数据库变更：**

```bash
/database-migrations 新增 notice 表
/jpa-patterns 设计 Notice 实体和关联关系
/security-review 检查 SQL 注入风险
```

所有 skill 通过斜杠命令触发：`/<skill-name> [可选描述]`

---

# 第三部分 实战工作流

---

## 第6章 典型开发场景

### 场景1：新建业务模块（完整流程）

```
# 1. 规划
@carlos-planner 我需要设计一个订单管理模块

# 2. 生成脚手架
@carlos-module-scaffolder 请生成 carlos-order 模块

# 3. 设计数据库
@carlos-db-designer 设计订单表结构

# 4. 设计 API
@carlos-api-designer 设计订单 CRUD 接口

# 5. TDD 开发
@springboot-tdd 实现订单创建功能

# 6. 生成测试
@carlos-integration-tester 生成订单模块集成测试

# 7. 安全审查
@carlos-security-reviewer 审查订单模块代码

# 8. 更新文档
/update-doc 新增订单模块文档
```

### 场景2：前端新增页面

```
# 1. 创建页面
/new-page

# 2. 生成路由
/gen-route

# 3. 类型检查
pnpm typecheck

# 4. 代码审查
@vue-reviewer 审查新页面代码
```

### 场景3：修复 Bug

```
# 1. TDD 流程修复
@springboot-tdd 修复用户登录时的空指针异常

# 2. 代码审查
@java-reviewer 审查修复代码

# 3. 安全审查（如涉及用户输入）
@carlos-security-reviewer 审查修复代码

# 4. 运行测试
/test
```

### 场景4：构建失败排查

```
# 自动诊断
@carlos-build-resolver mvn clean install 失败了

# 或手动排查
/build
```

---

## 第7章 开发流程速查

### 开发流程速查

```
[需求分析] → @carlos-planner
    ↓
[数据库设计] → @carlos-db-designer
    ↓
[API 设计] → @carlos-api-designer
    ↓
[TDD 开发] → @springboot-tdd
    ↓
[代码审查] → @java-reviewer / @vue-reviewer
    ↓
[安全审查] → @carlos-security-reviewer
    ↓
[集成测试] → @carlos-integration-tester
    ↓
[构建验证] → /build
    ↓
[文档更新] → /update-doc
```

### 常用命令速查

| 操作   | 命令                |
|------|-------------------|
| 构建后端 | `/build`          |
| 构建前端 | `/build-frontend` |
| 运行测试 | `/test`           |
| 生成路由 | `/gen-route`      |
| 新建模块 | `/new-module`     |
| 新建页面 | `/new-page`       |
| 更新文档 | `/update-doc`     |

### 常用 Agent 速查

| 场景        | Agent                        |
|-----------|------------------------------|
| 功能规划      | `@carlos-planner`            |
| API 设计    | `@carlos-api-designer`       |
| 数据库设计     | `@carlos-db-designer`        |
| 模块脚手架     | `@carlos-module-scaffolder`  |
| 构建修复      | `@carlos-build-resolver`     |
| 安全审查      | `@carlos-security-reviewer`  |
| 集成测试      | `@carlos-integration-tester` |
| Java 代码审查 | `@java-reviewer`             |
| Vue 代码审查  | `@vue-reviewer`              |
| TDD 开发    | `@springboot-tdd`            |
| 前后端对接     | `@api-connector`             |

---

# 第四部分 团队协作

---

## 第8章 Agent Team 协作体系

### Agent Team 概述

Agent Team 是一组协同工作的 AI 助手，每个团队专注于特定的开发场景。通过团队协作，可以自动化完成复杂的开发任务。

### 团队组成

每个团队包含：

- **Leader（领导者）**：负责整体协调和决策
- **Members（成员）**：各司其职的专业 agent
- **Workflow（工作流）**：定义 agent 之间的协作顺序
- **Skills（技能）**：团队共享的编码规范和最佳实践
- **MCP（工具）**：团队可用的外部工具和服务

---

### 团队列表

### 1. 后端开发团队（backend-dev）

**职责：** 负责后端模块开发、API 设计、数据库设计

**成员：**

- `carlos-planner` - 规划师（Leader）
- `carlos-db-designer` - 数据库设计师
- `carlos-module-scaffolder` - 脚手架生成器
- `carlos-api-designer` - API 设计师
- `springboot-tdd` - TDD 开发工程师
- `java-reviewer` - 代码审查员

**工作流：**

```
carlos-planner → carlos-db-designer → carlos-module-scaffolder
→ carlos-api-designer → springboot-tdd → java-reviewer
```

**适用场景：**

- 新建业务模块
- API 接口开发
- 数据库设计
- 后端功能实现

---

### 2. 前端开发团队（frontend-dev）

**职责：** 负责前端页面开发、组件开发、UI 实现

**成员：**

- `carlos-planner` - 规划师（Leader）
- `api-connector` - 前后端对接工程师
- `vue-reviewer` - 前端代码审查员
- `e2e-runner` - E2E 测试工程师

**工作流：**

```
carlos-planner → api-connector → vue-reviewer → e2e-runner
```

**适用场景：**

- 新增前端页面
- 组件开发
- 前后端对接
- UI 功能实现

---

### 3. 全栈开发团队（fullstack-dev）

**职责：** 负责前后端联调、完整功能实现

**成员：**

- `carlos-planner` - 全栈规划师（Leader）
- `carlos-db-designer` - 数据库设计师
- `carlos-api-designer` - API 设计师
- `api-connector` - 前后端对接工程师
- `springboot-tdd` - 后端 TDD 工程师
- `vue-reviewer` - 前端审查员
- `java-reviewer` - 后端审查员
- `carlos-integration-tester` - 集成测试工程师

**工作流：**

```
carlos-planner → carlos-db-designer → carlos-api-designer
→ [springboot-tdd, api-connector] (并行)
→ [java-reviewer, vue-reviewer] (并行)
→ carlos-integration-tester
```

**适用场景：**

- 完整功能模块开发
- 前后端联调
- 复杂业务实现

---

### 4. 质量保证团队（qa-team）

**职责：** 负责代码审查、安全审查、测试覆盖

**成员：**

- `carlos-security-reviewer` - 安全审查专家（Leader）
- `java-reviewer` - Java 代码审查员
- `vue-reviewer` - Vue 代码审查员
- `carlos-integration-tester` - 集成测试专家
- `e2e-runner` - E2E 测试专家

**工作流：**

```
[java-reviewer, vue-reviewer] (并行)
→ carlos-security-reviewer
→ carlos-integration-tester
→ e2e-runner
```

**适用场景：**

- 代码提交前审查
- 安全漏洞检查
- 测试覆盖率验证

---

### 5. 运维团队（devops-team）

**职责：** 负责构建、部署、文档更新

**成员：**

- `carlos-build-resolver` - 构建错误修复专家（Leader）
- `build-error-resolver` - 通用构建修复专家
- `carlos-doc-updater` - 文档更新专家
- `doc-updater` - 通用文档专家

**工作流：**

```
carlos-build-resolver → carlos-doc-updater
```

**适用场景：**

- 构建错误修复
- 文档更新
- 部署问题排查

---

### 6. 架构团队（architecture-team）

**职责：** 负责架构设计、技术决策、系统优化

**成员：**

- `architect` - 架构师（Leader）
- `carlos-planner` - 功能规划师
- `carlos-db-designer` - 数据架构师
- `database-reviewer` - 数据库优化专家

**工作流：**

```
architect → carlos-planner → carlos-db-designer → database-reviewer
```

**适用场景：**

- 架构设计
- 技术选型
- 系统重构
- 性能优化

---

### 使用方法

### 方法 1：调用整个团队

**语法：**

```
@team:<team-name> <task-description>
```

**示例：**

```
@team:backend-dev 开发订单管理模块
@team:frontend-dev 创建用户列表页面
@team:qa-team 审查最新代码
@team:devops-team 修复构建错误
```

### 方法 2：执行预定义场景

**语法：**

```
@scenario:<scenario-name> <parameters>
```

**示例：**

```
@scenario:new-module carlos-order
@scenario:new-page user-list
@scenario:bug-fix 用户登录空指针异常
@scenario:fullstack-feature 订单支付功能
```

### 方法 3：直接调用单个 Agent

**语法：**

```
@<agent-name> <task-description>
```

**示例：**

```
@carlos-planner 设计订单管理模块
@carlos-api-designer 设计用户 CRUD 接口
@springboot-tdd 实现订单创建功能
@java-reviewer 审查订单模块代码
```

---

### 预定义场景

### 场景 1：新建业务模块（new-module）

**团队：** backend-dev

**步骤：**

1. `carlos-planner` - 分析需求，输出 PRD 和模块设计方案
2. `carlos-db-designer` - 设计数据库表结构
3. `carlos-module-scaffolder` - 生成模块脚手架
4. `carlos-api-designer` - 设计 REST API 和 Feign 接口
5. `springboot-tdd` - TDD 开发功能
6. `java-reviewer` - 代码审查
7. `carlos-security-reviewer` - 安全审查
8. `carlos-integration-tester` - 生成集成测试
9. `carlos-doc-updater` - 更新文档

**使用示例：**

```
@scenario:new-module carlos-order
```

---

### 场景 2：新增前端页面（new-page）

**团队：** frontend-dev

**步骤：**

1. `carlos-planner` - 页面设计和组件规划
2. `api-connector` - 生成前端 service 层
3. `vue-reviewer` - 代码审查
4. `e2e-runner` - E2E 测试

**使用示例：**

```
@scenario:new-page user-list
```

---

### 场景 3：Bug 修复（bug-fix）

**团队：** qa-team

**步骤：**

1. `springboot-tdd` - TDD 流程修复 Bug
2. `java-reviewer` - 代码审查
3. `carlos-security-reviewer` - 安全审查（如涉及用户输入）

**使用示例：**

```
@scenario:bug-fix 用户登录空指针异常
```

---

### 场景 4：构建修复（build-fix）

**团队：** devops-team

**步骤：**

1. `carlos-build-resolver` - 诊断构建错误
2. `carlos-build-resolver` - 执行修复

**使用示例：**

```
@scenario:build-fix
```

---

### 场景 5：全栈功能开发（fullstack-feature）

**团队：** fullstack-dev

**步骤：**

1. `carlos-planner` - 整体架构设计
2. `carlos-db-designer` - 数据库设计
3. `carlos-api-designer` - 后端 API 设计
4. `[springboot-tdd, api-connector]` - 并行开发：后端实现 + 前端 service 层
5. `[java-reviewer, vue-reviewer]` - 并行审查：后端 + 前端
6. `carlos-security-reviewer` - 安全审查
7. `carlos-integration-tester` - 集成测试

**使用示例：**

```
@scenario:fullstack-feature 订单支付功能
```

---

### 场景 6：架构评审（architecture-review）

**团队：** architecture-team

**步骤：**

1. `architect` - 架构分析和设计
2. `carlos-planner` - 功能模块规划
3. `carlos-db-designer` - 数据架构设计
4. `database-reviewer` - 数据库优化建议

**使用示例：**

```
@scenario:architecture-review
```

---

### 最佳实践

### 1. 选择合适的团队

| 任务类型   | 推荐团队              |
|--------|-------------------|
| 新建后端模块 | backend-dev       |
| 新建前端页面 | frontend-dev      |
| 完整功能开发 | fullstack-dev     |
| 代码审查   | qa-team           |
| 构建问题   | devops-team       |
| 架构设计   | architecture-team |

### 2. 并行执行

当多个 agent 可以并行工作时，使用并行执行提高效率：

```
# 并行执行后端和前端开发
@springboot-tdd 实现订单创建功能
@api-connector 生成订单前端 service 层
```

### 3. 遵循工作流

按照团队定义的工作流顺序执行，确保每个步骤的输出可以作为下一步的输入。

### 4. 使用预定义场景

对于常见任务，使用预定义场景可以自动化整个流程：

```
# 使用场景而不是手动调用每个 agent
@scenario:new-module carlos-order
```

### 5. 代码审查必须执行

在代码提交前，必须执行以下审查：

- `java-reviewer` 或 `vue-reviewer` - 代码质量审查
- `carlos-security-reviewer` - 安全审查（强制）

---

### 示例

### 示例 1：开发订单管理模块（完整流程）

```bash
# 方法 1：使用预定义场景（推荐）
@scenario:new-module carlos-order

# 方法 2：手动调用团队
@team:backend-dev 开发订单管理模块，包含订单创建、支付、发货、退款等功能

# 方法 3：手动调用每个 agent（不推荐，除非需要精细控制）
@carlos-planner 设计订单管理模块
@carlos-db-designer 设计订单表结构
@carlos-module-scaffolder 生成 carlos-order 模块脚手架
@carlos-api-designer 设计订单 CRUD 接口
@springboot-tdd 实现订单创建功能
@java-reviewer 审查订单模块代码
@carlos-security-reviewer 审查订单模块安全性
@carlos-integration-tester 生成订单模块集成测试
@carlos-doc-updater 更新订单模块文档
```

### 示例 2：创建用户列表页面

```bash
# 使用预定义场景
@scenario:new-page user-list

# 或使用团队
@team:frontend-dev 创建用户列表页面，包含搜索、分页、编辑、删除功能
```

### 示例 3：修复登录 Bug

```bash
# 使用预定义场景
@scenario:bug-fix 用户登录时出现空指针异常

# 或使用团队
@team:qa-team 修复用户登录空指针异常
```

### 示例 4：全栈开发订单支付功能

```bash
# 使用预定义场景
@scenario:fullstack-feature 订单支付功能

# 或使用团队
@team:fullstack-dev 开发订单支付功能，包含后端支付接口和前端支付页面
```

### 示例 5：架构评审

```bash
# 使用预定义场景
@scenario:architecture-review

# 或使用团队
@team:architecture-team 评审订单模块的架构设计
```

---

### 配置文件

Agent Team 配置文件位置：`.claude/agent-teams.json`

配置文件包含：

- **teams**：所有团队的定义
- **scenarios**：预定义场景的步骤
- **usage**：使用方法说明

---

### 附录：快速参考卡

一页纸速查表，帮助你快速选择和使用合适的 Agent Team。

场景速查表

| 我要做什么  | 使用场景                  | 快捷命令                                |
|--------|-----------------------|-------------------------------------|
| 新建后端模块 | `new-module`          | `@scenario:new-module carlos-order` |
| 新建前端页面 | `new-page`            | `@scenario:new-page user-list`      |
| 修复 Bug | `bug-fix`             | `@scenario:bug-fix 登录空指针异常`         |
| 构建失败   | `build-fix`           | `@scenario:build-fix`               |
| 全栈功能   | `fullstack-feature`   | `@scenario:fullstack-feature 订单支付`  |
| 架构评审   | `architecture-review` | `@scenario:architecture-review`     |

团队速查表

| 团队                    | 适用场景 | Leader                   | 成员数 |
|-----------------------|------|--------------------------|-----|
| **backend-dev**       | 后端开发 | carlos-planner           | 6   |
| **frontend-dev**      | 前端开发 | carlos-planner           | 4   |
| **fullstack-dev**     | 全栈开发 | carlos-planner           | 8   |
| **qa-team**           | 质量保证 | carlos-security-reviewer | 5   |
| **devops-team**       | 运维部署 | carlos-build-resolver    | 4   |
| **architecture-team** | 架构设计 | architect                | 4   |

三种调用方式

**1. 场景调用（推荐）**

```bash
@scenario:new-module carlos-order
```

✅ 自动执行完整流程
✅ 无需记忆步骤
✅ 最佳实践内置

**2. 团队调用**

```bash
@team:backend-dev 开发订单管理模块
```

✅ 灵活描述需求
✅ 团队自动协作
⚠️ 需要清晰描述

**3. Agent 调用**

```bash
@carlos-planner 设计订单模块
@carlos-api-designer 设计 CRUD 接口
```

✅ 精细控制
⚠️ 需要手动协调
⚠️ 容易遗漏步骤

各团队流程速查

**后端开发（backend-dev）**

```
1. carlos-planner          → 需求分析、模块设计
2. carlos-db-designer      → 数据库表设计
3. carlos-module-scaffolder → 生成脚手架
4. carlos-api-designer     → API 设计
5. springboot-tdd          → TDD 开发
6. java-reviewer           → 代码审查
7. carlos-security-reviewer → 安全审查
8. carlos-integration-tester → 集成测试
9. carlos-doc-updater      → 更新文档
```

快捷命令：`@scenario:new-module carlos-order`

**前端开发（frontend-dev）**

```
1. carlos-planner  → 页面设计、组件规划
2. api-connector   → 生成 service 层
3. vue-reviewer    → 代码审查
4. e2e-runner      → E2E 测试
```

快捷命令：`@scenario:new-page user-list`

**全栈开发（fullstack-dev）**

```
1. carlos-planner      → 整体架构设计
2. carlos-db-designer  → 数据库设计
3. carlos-api-designer → 后端 API 设计
4. [并行] springboot-tdd + api-connector
5. [并行] java-reviewer + vue-reviewer
6. carlos-security-reviewer → 安全审查
7. carlos-integration-tester → 集成测试
```

快捷命令：`@scenario:fullstack-feature 订单支付功能`

**质量保证（qa-team）**

```
1. [并行] java-reviewer + vue-reviewer
2. carlos-security-reviewer  → 安全审查（强制）
3. carlos-integration-tester → 集成测试
4. e2e-runner                → E2E 测试
```

使用时机：代码提交前（强制）、PR 创建前、发布前

**运维（devops-team）**

```
1. carlos-build-resolver → 诊断构建错误
2. carlos-doc-updater    → 更新文档
```

快捷命令：`@scenario:build-fix`

**架构评审（architecture-team）**

```
1. architect           → 架构分析和设计
2. carlos-planner      → 功能模块规划
3. carlos-db-designer  → 数据架构设计
4. database-reviewer   → 数据库优化建议
```

快捷命令：`@scenario:architecture-review`

最佳实践速查

**✅ DO（推荐做法）**

1. **使用场景调用**：`@scenario:new-module carlos-order`
2. **遵循工作流顺序**：先规划，后实现；先测试，后审查；先审查，后提交
3. **强制安全审查**：`@carlos-security-reviewer 审查代码`
4. **并行执行独立任务**：后端开发和前端 service 层可以并行

**❌ DON'T（避免做法）**

1. ❌ 直接提交代码 → ✅ 先审查，后提交
2. ❌ 忽略安全检查 → ✅ 强制执行安全审查
3. ❌ 逐个调用 9 个 agent → ✅ 使用 `@scenario:new-module`
4. ❌ 不写测试 → ✅ 确保 80%+ 覆盖率

学习路径

| 天数    | 内容                                                                                               |
|-------|--------------------------------------------------------------------------------------------------|
| 第 1 天 | `@scenario:new-module carlos-demo` / `@scenario:new-page demo-list` / `@scenario:bug-fix 修复登录问题` |
| 第 2 天 | `@team:backend-dev 开发用户管理模块` / `@team:frontend-dev 创建用户管理页面` / `@team:qa-team 审查用户管理模块`          |
| 第 3 天 | `@scenario:fullstack-feature 用户权限管理功能`                                                           |
| 第 4 天 | `@scenario:architecture-review`                                                                  |

常见问题

**Q1: 场景和团队有什么区别？**

- **场景（Scenario）**：预定义的完整流程，固定的步骤顺序，适合标准化任务
- **团队（Team）**：灵活的协作方式，根据需求调整，适合复杂任务

**Q2: 什么时候用场景，什么时候用团队？**

| 情况             | 推荐方式  |
|----------------|-------|
| 标准化任务（新建模块、页面） | 场景    |
| 复杂需求（需要详细描述）   | 团队    |
| 快速执行（不想思考步骤）   | 场景    |
| 精细控制（需要调整流程）   | Agent |

**Q3: 可以跳过某些步骤吗？**

可以，但不推荐。如果必须跳过，直接调用需要的 agent：

```bash
@carlos-api-designer 设计 API
@springboot-tdd 实现功能
@java-reviewer 审查代码
```

**Q4: 如何并行执行？**

在一条消息中调用多个 agent：

```bash
@springboot-tdd 实现订单创建功能
@api-connector 生成订单前端 service 层
```

记住这些

1. **场景优先**：`@scenario:new-module` 比手动调用 9 个 agent 更快
2. **安全强制**：`@carlos-security-reviewer` 必须在提交前执行
3. **测试覆盖**：确保 80%+ 测试覆盖率
4. **并行执行**：独立任务可以并行，提高效率
5. **遵循流程**：规划 → 设计 → 开发 → 审查 → 测试 → 文档

*打印此页，贴在显示器旁边！*

---

# 第五部分 成长路径

---

## 第9章 Claude Code 八阶能力进阶

> 以下提供从入门到高阶的 Claude Code 系统学习路径，与本文档主体内容互为补充。

---

### 阶段一：基础入门（1-2周）

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

### 阶段三：Sub-agent 协作 L1（3-4周）

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
      "args": [
        "-y",
        "@modelcontextprotocol/server-puppeteer"
      ]
    },
    "postgres": {
      "command": "npx",
      "args": [
        "-y",
        "@modelcontextprotocol/server-postgres"
      ],
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
      "args": [
        "-y",
        "@supabase/mcp-server-postgrest"
      ],
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
      "args": [
        "{{tool_name}}",
        "{{file_path}}"
      ]
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
  -
    id: E1-S1
    title: 设计数据库 Schema
    owner: DBSpecialist
    status: ready
    files: [ ]

  -
    id: E1-S2
    title: 实现后端 API
    owner: BackendDev
    status: blocked  # 依赖 E1-S1
    depends_on: E1-S1
    files: [ ]

  -
    id: E1-S3
    title: 实现前端界面
    owner: FrontendDev
    status: blocked
    depends_on: E1-S2
    files: [ ]
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
  -
    id: E1-S2
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
      "depends_on": [
        "architect"
      ]
    },
    {
      "name": "dev-2",
      "type": "codex",
      "role": "execute",
      "worktree": "worktree-2",
      "depends_on": [
        "architect"
      ]
    }
  ],
  "stages": [
    "discuss",
    "specify",
    "execute",
    "compound"
  ]
}
```

CI/CD 集成

GitHub Actions 配置

```yaml
# .github/workflows/ai-review.yml
name: AI Code Review

on: [ pull_request ]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      -
        uses: actions/checkout@v4

      -
        name: Setup Claude Code
        run: |
          npm install -g @anthropic-ai/claude-code
          echo "${{ secrets.CLAUDE_API_KEY }}" > ~/.claude/auth.json

      -
        name: AI Review
        run: |
          claude --agent code-reviewer \
                 --pr ${{ github.event.pull_request.number }} \
                 --output review-comment.md

      -
        name: Post Review
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

> 建议学习节奏：每周专注一个阶段，完成退出标准后再进入下一阶段。阶段 3-5 可以并行推进，阶段 6-7 建议在遇到实际复杂需求时再深入。

---

## 第10章 Carlos Framework 项目实战精通计划

> 以下内容为 carlos-framework 项目专属实战计划，将上述通用技能应用到实际项目开发中。

### 总体路线图

```
阶段一：基础入门（第1-2周）
  ↓
阶段二：工作流精通（第3-5周）
  ↓
阶段三：多代理协作（第6-8周）
  ↓
阶段四：自动化与精通（第9-12周）
```

---

### 阶段一：基础入门（项目实战）

**目标：** 掌握 Claude Code 的核心交互方式，完成 carlos-framework 首个端到端功能。

#### 任务 1.1 — 项目探索（理解代码库）

**练习场景：** 让 Claude 帮你理解 carlos-framework 项目结构

```bash
# 进入项目，直接向 Claude 提问：
"解释 carlos-spring-boot-starter-web 模块的作用和核心配置"
"carlos-framework 中 Manager 层和 Service 层的职责区别是什么？"
"列出所有实现了 BaseEnum 的枚举类"
```

**学会使用的工具：**

- `Read` / `Grep` / `Glob` — Claude 读文件的方式
- `Explore` agent — 代码库探索

#### 任务 1.2 — 第一个 TDD 功能

**练习场景：** 在 `carlos-system` 模块中添加一个简单的字典查询 API

```bash
# 触发 TDD 工作流：
/tdd
# 或直接说：
"使用TDD方式在carlos-system中添加一个按类型查询字典的接口"
```

**学会使用的工具：**

- `tdd-guide` agent（自动触发）
- `/tdd` skill
- `springboot-tdd` skill

**里程碑：** 提交第一个测试覆盖率 ≥80% 的功能 PR。

#### 任务 1.3 — 规范代码提交

```bash
# 完成代码后运行：
/code-review    # 代码审查
/verify         # 验证循环（lint + test + security）
/commit         # 生成规范提交信息
```

---

### 阶段二：工作流精通（项目实战）

**目标：** 掌握 Plan → TDD → Review → Security 的完整开发流水线。

#### 任务 2.1 — 使用 Blueprint 规划功能

**练习场景：** 实现 `carlos-audit` 审计日志模块的查询功能

```bash
/blueprint
# 告诉 Claude：
"实现审计日志的分页查询和导出Excel功能，
包括前端查询页面和后端API，遵循carlos-framework规范"
```

`/blueprint` 会生成：PRD → 架构设计 → 系统设计 → 技术文档 → 任务列表

#### 任务 2.2 — 后端技能组合

实现一个完整 CRUD 模块时，按顺序使用：

| 顺序 | Skill/Agent               | 作用     |
|----|---------------------------|--------|
| 1  | `/plan`                   | 制定实现计划 |
| 2  | `springboot-patterns`     | 遵循框架模式 |
| 3  | `/tdd` → `tdd-guide`      | 测试先行   |
| 4  | `code-reviewer`           | 代码审查   |
| 5  | `security-reviewer`       | 安全审查   |
| 6  | `springboot-verification` | 综合验证   |

#### 任务 2.3 — 前端技能组合

实现一个查询页面：

| 顺序 | Skill/Agent         | 作用        |
|----|---------------------|-----------|
| 1  | `frontend-patterns` | Vue3 模式参考 |
| 2  | `api-design`        | 接口设计验证    |
| 3  | `code-reviewer`     | TS 代码审查   |
| 4  | `verification-loop` | 完整验证      |

#### 任务 2.4 — 掌握 CLAUDE.md 优化

学习向 CLAUDE.md 添加项目专属规则，让 Claude 每次都能精准遵循：

```bash
# 在 carlos-framework/CLAUDE.md 中补充：
- 项目特有的命名规范
- 常用模块的路径地图
- 禁止使用的反模式清单
```

---

### 阶段三：多代理并行协作（项目实战）

**目标：** 同时驱动多个 Agent 并行工作，大幅提升 carlos-framework 开发效率。

#### 任务 3.1 — 并行代码分析

**练习场景：** 对 `carlos-auth` OAuth2 模块进行全面分析

```bash
# 在一个请求中同时启动3个Agent：
"请并行分析carlos-auth模块：
1. architect agent: 评估架构设计
2. security-reviewer agent: 安全漏洞扫描
3. code-reviewer agent: 代码质量审查"
```

#### 任务 3.2 — 前后端联调工作流

使用 `TeamCreate` 组建团队完成前后端对接：

```
Team Lead (你) → 拆分任务
  ├── 前端 Agent：接口对接、类型定义
  └── 后端 Agent：API实现、文档生成
```

#### 任务 3.3 — 使用 `/autonomous-loops`

设置自动化循环，让 Claude 持续优化某个模块直到达标：

```bash
# 适用场景：
- 持续运行测试直到覆盖率达到80%
- 循环修复 lint 问题直到0 warning
- 迭代优化SQL查询性能
```

#### 任务 3.4 — 使用 `continuous-learning` 沉淀知识

```bash
/learn    # 从当前会话提取可复用模式
/learn-eval  # 自评质量后保存
```

---

### 阶段四：自动化与精通（项目实战）

**目标：** 构建自己的 Skill、自动化工作流，并成为团队 Claude Code 传教士。

#### 任务 4.1 — 创建 `carlos-framework-standard` Skill

```bash
/skill-create
# 让 Claude 分析你的开发历史，生成专属技能文件：
# .claude/skills/carlos-framework-standard/SKILL.md
```

内容应包含：

- carlos-framework 命名规范自动检查
- 分层架构合规验证
- 常用代码片段模板

#### 任务 4.2 — 构建评估体系 (Eval-Driven Development)

```bash
/eval-harness
# 建立评估框架，衡量 Claude Code 输出质量：
- API 设计合规率
- 测试覆盖率趋势
- 代码审查通过率
```

#### 任务 4.3 — 利用 `instinct` 系统积累经验

```bash
/instinct-status   # 查看已积累的直觉规则
/learn-eval        # 每次会话后提炼新直觉
/promote           # 将项目级直觉提升为全局
```

#### 任务 4.4 — 构建 carlos-framework 专属 Agent

参考 `~/.claude/agents/` 下的现有 Agent 格式，创建：

- `carlos-arch-reviewer.md` — 专门验证分层架构合规性
- `carlos-api-designer.md` — 生成符合项目规范的 API

---

### 能力成熟度里程碑

| 里程碑                 | 验证方式                                          |
|---------------------|-----------------------------------------------|
| L1：能用 Claude 完成简单功能 | 提交第一个 Claude 辅助的 PR                           |
| L2：掌握完整工作流          | `blueprint → tdd → review → commit` 全程 Claude |
| L3：并行多代理协作          | 同时驱动 3+ Agent 完成复杂任务                          |
| L4：产出自定义技能          | 发布 `carlos-framework-standard` skill          |
| L5：建立团队规范           | 编写 Claude Code 使用指南供团队使用                      |

---

### 立即开始的第一步

现在就可以开始，选择一个你最想完成的功能模块，执行：

```bash
/blueprint
```

然后告诉 Claude 你要实现什么，它会引导你走完整个流程。**这既是学习，也是真实的功能交付。**

---

**计划版本：** v1.0
**生成日期：** 2026-03-20
**适合项目：** carlos-framework 企业级全栈框架
**计划周期：** 12周进阶体系

---

*最后更新：2026-05-23*

---

# 附录

---

## 附录A 一页纸速查卡

> 将本页打印，贴在显示器旁，每日开发对照使用。

### 常用命令

| 操作   | 命令                |
|------|-------------------|
| 构建后端 | `/build`          |
| 构建前端 | `/build-frontend` |
| 运行测试 | `/test`           |
| 生成路由 | `/gen-route`      |
| 新建模块 | `/new-module`     |
| 新建页面 | `/new-page`       |
| 更新文档 | `/update-doc`     |

### 常用 Agent

| 场景      | Agent                        |
|---------|------------------------------|
| 功能规划    | `@carlos-planner`            |
| API 设计  | `@carlos-api-designer`       |
| 数据库设计   | `@carlos-db-designer`        |
| 模块脚手架   | `@carlos-module-scaffolder`  |
| TDD 开发  | `@springboot-tdd`            |
| Java 审查 | `@java-reviewer`             |
| Vue 审查  | `@vue-reviewer`              |
| 安全审查    | `@carlos-security-reviewer`  |
| 构建修复    | `@carlos-build-resolver`     |
| 集成测试    | `@carlos-integration-tester` |
| 前后端对接   | `@api-connector`             |

### 标准开发流程

```
[规划] → @carlos-planner
    ↓
[数据库设计] → @carlos-db-designer
    ↓
[API 设计] → @carlos-api-designer
    ↓
[TDD 开发] → @springboot-tdd
    ↓
[代码审查] → @java-reviewer / @vue-reviewer
    ↓
[安全审查] → @carlos-security-reviewer
    ↓
[集成测试] → @carlos-integration-tester
    ↓
[构建验证] → /build
    ↓
[文档更新] → /update-doc
```

### 提交前强制检查清单

- [ ] 代码审查通过（`@java-reviewer` / `@vue-reviewer`）
- [ ] 安全审查通过（`@carlos-security-reviewer`，强制）
- [ ] 测试覆盖率 >= 80%
- [ ] Maven 构建成功（`/build`）
- [ ] 无硬编码凭据、SQL 注入、XSS 风险
