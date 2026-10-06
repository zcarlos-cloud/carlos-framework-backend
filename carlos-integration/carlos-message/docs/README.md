# carlos-message 文档中心

> 消息服务 - 站内信、短信、邮件、推送

---

## 文档导航

### 📋 产品文档 (`01-product/`)

| 目录         | 说明         | 模板/文档                                                                          |
|------------|------------|--------------------------------------------------------------------------------|
| `PRD/`     | 产品需求文档     | [PRD 模板](./01-product/PRD/README.md)                                           |
|            |            | [PRD-20260311-消息模块开发方案.md](./01-product/PRD/PRD-20260311-消息模块开发方案.md) - 开发方案规划 |
| `BRD/`     | 商业需求文档     | -                                                                              |
| `roadmap/` | 产品路线图与版本规划 | -                                                                              |

### 🏗️ 设计文档 (`02-design/`)

| 目录              | 说明             | 模板/文档                                                                                           |
|-----------------|----------------|-------------------------------------------------------------------------------------------------|
| `architecture/` | 架构设计文档         | [架构模板](./02-design/architecture/README.md)                                                      |
|                 |                | [ARCH-20260311-消息模块设计规格说明书.md](./02-design/architecture/ARCH-20260311-消息模块设计规格说明书.md) - 设计规格说明书 |
| `api/`          | 接口设计文档         | [API 模板](./02-design/api/README.md)                                                             |
| `database/`     | 数据库设计（ER图、表结构） | [DB-20260311-消息模块数据库设计.md](./02-design/database/DB-20260311-消息模块数据库设计.md) - 数据库设计文档             |
|                 |                | [message_postgresql.sql](./02-design/database/message_postgresql.sql) - 数据库初始化脚本（PostgreSQL）    |
| `flow/`         | 业务流程图、时序图      | -                                                                                               |
| `decision/`     | 架构决策记录 (ADR)   | [ADR 模板](./02-design/decision/README.md)                                                        |

### 💻 开发文档 (`03-develop/`)

| 目录       | 说明          | 文档                                                             |
|----------|-------------|----------------------------------------------------------------|
| `guide/` | 开发指南、本地调试手册 | [DEV-消息模块开发任务.md](./03-develop/guide/DEV-消息模块开发任务.md) - 开发任务清单 |
|          |             | [DEV-消息模块快速开始.md](./03-develop/guide/DEV-消息模块快速开始.md) - 快速开始   |

### 🧪 测试文档 (`04-test/`)

| 目录        | 说明   | 模板                                 |
|-----------|------|------------------------------------|
| `plan/`   | 测试计划 | [测试计划模板](./04-test/plan/README.md) |
| `case/`   | 测试用例 | -                                  |
| `report/` | 测试报告 | -                                  |

### 🐛 问题与缺陷 (`05-issue/`)

| 目录        | 说明        | 模板                               |
|-----------|-----------|----------------------------------|
| `bug/`    | 缺陷报告      | [缺陷模板](./05-issue/bug/README.md) |
| `risk/`   | 风险清单      | -                                |
| `review/` | 设计/代码评审记录 | -                                |

### 🚀 部署运维 (`06-deploy/`)

| 目录          | 说明            | 文档                                                                        |
|-------------|---------------|---------------------------------------------------------------------------|
| `config/`   | 配置说明          | [application.yml](./06-deploy/config/application.yml) - 应用配置              |
|             |               | [application-boot.yml](./06-deploy/config/application-boot.yml) - Boot 配置 |
|             |               | [application-env.yml](./06-deploy/config/application-env.yml) - 环境配置      |
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
例：PRD-20260520-消息推送渠道扩展.md

# 架构设计
ARCH-{主题}-{版本}.md
例：ARCH-消息队列架构-v1.0.md

# 接口设计
API-{模块}-{版本}.md
例：API-message-v1.0.md

# 测试计划
TESTPLAN-{迭代}-{版本}.md
例：TESTPLAN-Sprint3-v1.0.md

# 缺陷报告
BUG-{YYYYMMDD}-{NNN}.md
例：BUG-20260520-001.md

# 架构决策
ADR-{NNN}-{标题}.md
例：ADR-001-消息存储选型.md
```

---

## 文档生命周期

```
草稿 → 评审中 → 已确认 → 已归档
         ↓         ↓
       已拒绝    已废弃
```

每篇文档必须在头部标注当前状态、版本、作者和日期。
