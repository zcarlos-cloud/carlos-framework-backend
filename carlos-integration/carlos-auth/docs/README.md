# carlos-auth 文档中心

> 认证服务 - 用户认证、授权、Token管理

---

## 文档导航

### 📋 产品文档 (`01-product/`)

| 目录         | 说明         | 模板                                   |
|------------|------------|--------------------------------------|
| `PRD/`     | 产品需求文档     | [PRD 模板](./01-product/PRD/README.md) |
| `BRD/`     | 商业需求文档     | -                                    |
| `roadmap/` | 产品路线图与版本规划 | -                                    |

### 🏗️ 设计文档 (`02-design/`)

| 目录              | 说明             | 模板/文档                                                                                                  |
|-----------------|----------------|--------------------------------------------------------------------------------------------------------|
| `architecture/` | 架构设计文档         | [架构模板](./02-design/architecture/README.md)                                                             |
|                 |                | [ARCH-OAuth2-SSO架构澄清-v1.0.md](./02-design/architecture/ARCH-OAuth2-SSO架构澄清-v1.0.md) - OAuth2 SSO 架构澄清  |
|                 |                | [ARCH-统一身份源架构设计-v1.0.md](./02-design/architecture/ARCH-统一身份源架构设计-v1.0.md) - 统一身份源架构设计                  |
|                 |                | [ARCH-20260416-认证中心技术设计深度分析.md](./02-design/architecture/ARCH-20260416-认证中心技术设计深度分析.md) - 认证中心技术设计深度分析 |
| `api/`          | 接口设计文档         | [API 模板](./02-design/api/README.md)                                                                    |
|                 |                | [scope.json](./02-design/api/scope.json) - OAuth2 Scope 定义                                             |
| `database/`     | 数据库设计（ER图、表结构） | [auth_postgresql.sql](./02-design/database/auth_postgresql.sql) - 认证授权数据库脚本（PostgreSQL，含 Portal 应用门户）  |
| `flow/`         | 业务流程图、时序图      | -                                                                                                      |
| `decision/`     | 架构决策记录 (ADR)   | [ADR 模板](./02-design/decision/README.md)                                                               |

### 💻 开发文档 (`03-develop/`)

| 目录       | 说明          | 文档                                                                      |
|----------|-------------|-------------------------------------------------------------------------|
| `guide/` | 开发指南、本地调试手册 | [DEV-登录认证接口调试指南.md](./03-develop/guide/DEV-登录认证接口调试指南.md) - 登录认证接口调试指南  |
|          |             | [DEV-认证中心MFA开发指南.md](./03-develop/guide/DEV-认证中心MFA开发指南.md) - 多因素认证开发指南 |
|          |             | [DEV-认证模块开发任务清单.txt](./03-develop/guide/DEV-认证模块开发任务清单.txt) - 开发任务清单    |

### 🧪 测试文档 (`04-test/`)

| 目录        | 说明   | 模板/文档                                                                              |
|-----------|------|------------------------------------------------------------------------------------|
| `plan/`   | 测试计划 | [测试计划模板](./04-test/plan/README.md)                                                 |
| `case/`   | 测试用例 | -                                                                                  |
| `report/` | 测试报告 | [RPT-20260519-认证模块代码扫描报告.md](./04-test/report/RPT-20260519-认证模块代码扫描报告.md) - 代码扫描报告 |

### 🐛 问题与缺陷 (`05-issue/`)

| 目录        | 说明        | 文档                                                                           |
|-----------|-----------|------------------------------------------------------------------------------|
| `bug/`    | 缺陷报告      | [缺陷模板](./05-issue/bug/README.md)                                             |
|           |           | [BUG-20260417-认证模块问题分析.md](./05-issue/bug/BUG-20260417-认证模块问题分析.md) - 问题分析报告 |
| `risk/`   | 风险清单      | [RISK-认证模块待确认问题.txt](./05-issue/risk/RISK-认证模块待确认问题.txt) - 待确认问题             |
| `review/` | 设计/代码评审记录 | -                                                                            |

### 🚀 部署运维 (`06-deploy/`)

| 目录          | 说明            | 文档                                                                        |
|-------------|---------------|---------------------------------------------------------------------------|
| `config/`   | 配置说明          | [application.yml](./06-deploy/config/application.yml) - 应用配置              |
|             |               | [application-boot.yml](./06-deploy/config/application-boot.yml) - Boot 配置 |
|             |               | [application-env.yml](./06-deploy/config/application-env.yml) - 环境配置      |
|             |               | [application-auth.yml](./06-deploy/config/application-auth.yml) - 认证配置    |
|             |               | [deploy.yaml](./06-deploy/config/deploy.yaml) - K8s 部署配置                  |
| `playbook/` | 部署手册、SOP、应急预案 | [运维手册模板](./06-deploy/playbook/README.md)                                  |

### 🎨 资源文件 (`assets/`)

| 目录          | 说明              |
|-------------|-----------------|
| `diagrams/` | 架构图、流程图、原型图     |
| `mock/`     | Mock 数据、JSON 示例 |

---

## 命名规范

### 文件命名

```
# 产品需求
PRD-{YYYYMMDD}-{需求简称}.md
例：PRD-20260520-登录流程优化.md

# 架构设计
ARCH-{主题}-{版本}.md
例：ARCH-OAuth2架构-v1.0.md

# 接口设计
API-{模块}-{版本}.md
例：API-auth-v1.0.md

# 测试计划
TESTPLAN-{迭代}-{版本}.md
例：TESTPLAN-Sprint3-v1.0.md

# 缺陷报告
BUG-{YYYYMMDD}-{NNN}.md
例：BUG-20260520-001.md

# 架构决策
ADR-{NNN}-{标题}.md
例：ADR-001-Token存储选型.md
```

---

## 文档生命周期

```
草稿 → 评审中 → 已确认 → 已归档
         ↓         ↓
       已拒绝    已废弃
```

每篇文档必须在头部标注当前状态、版本、作者和日期。
