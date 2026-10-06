# carlos-org 文档中心

> 组织服务 - 部门、岗位、人员组织架构管理

---

## 文档导航

### 📋 产品文档 (`01-product/`)

| 目录         | 说明         | 模板/文档                                                      |
|------------|------------|------------------------------------------------------------|
| `PRD/`     | 产品需求文档     | [PRD 模板](./01-product/PRD/README.md)                       |
|            |            | [PRD-用户中心需求文档.md](./01-product/PRD/PRD-用户中心需求文档.md) - 产品需求 |
| `BRD/`     | 商业需求文档     | -                                                          |
| `roadmap/` | 产品路线图与版本规划 | -                                                          |

### 🏗️ 设计文档 (`02-design/`)

| 目录              | 说明             | 模板/文档                                                                  |
|-----------------|----------------|------------------------------------------------------------------------|
| `architecture/` | 架构设计文档         | [架构模板](./02-design/architecture/README.md)                             |
|                 |                | [ARCH-组织模型设计-v1.0.md](./02-design/architecture/ARCH-组织模型设计-v1.0.md) - **组织模型定稿(三轴模型/真相源/结算规则)** |
|                 |                | [ARCH-部门缓存设计方案.md](./02-design/architecture/ARCH-部门缓存设计方案.md) - 部门缓存设计 |
| `api/`          | 接口设计文档         | [API 模板](./02-design/api/README.md)                                    |
|                 |                | [API-org-v1.0.md](./02-design/api/API-org-v1.0.md) - **Feign 远程查询接口(用户按ID/批量/直属下级/部门用户/角色用户)** |
| `database/`     | 数据库设计（ER图、表结构） | [org_postgresql.sql](./02-design/database/org_postgresql.sql) - 组织模块 DDL(2026-07-03 版，唯一权威脚本) |
| `flow/`         | 业务流程图、时序图      | -                                                                      |
| `decision/`     | 架构决策记录 (ADR)   | [ADR 模板](./02-design/decision/README.md)                               |
|                 |                | [ADR-001 主部门真相源与部门归属语义](./02-design/decision/ADR-001-主部门真相源与部门归属语义.md) |
|                 |                | [ADR-002 角色授权三机制语义与结算规则](./02-design/decision/ADR-002-角色授权三机制语义与结算规则.md) |
|                 |                | [ADR-003 权限资源与审计职责边界](./02-design/decision/ADR-003-权限资源与审计职责边界.md) |

### 💻 开发文档 (`03-develop/`)

| 目录       | 说明          |
|----------|-------------|
| `guide/` | 开发指南、本地调试手册 |

### 🧪 测试文档 (`04-test/`)

| 目录        | 说明   | 模板/文档                                                                          |
|-----------|------|--------------------------------------------------------------------------------|
| `plan/`   | 测试计划 | [测试计划模板](./04-test/plan/README.md)                                             |
| `case/`   | 测试用例 | -                                                                              |
| `report/` | 测试报告 | ~~[RPT-20260519-用户中心扫描报告.md](./04-test/report/RPT-20260519-用户中心扫描报告.md)~~ - **已废弃**(2026-09-22，内容过时仅留档) |

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
|             |               | [deploy-dev.yaml](./06-deploy/config/deploy-dev.yaml) - 开发环境部署            |
|             |               | [deploy-master.yaml](./06-deploy/config/deploy-master.yaml) - 主环境部署       |
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
例：PRD-20260520-组织架构批量导入.md

# 架构设计
ARCH-{主题}-{版本}.md
例：ARCH-部门缓存架构-v1.0.md

# 接口设计
API-{模块}-{版本}.md
例：API-org-v1.0.md

# 测试计划
TESTPLAN-{迭代}-{版本}.md
例：TESTPLAN-Sprint3-v1.0.md

# 缺陷报告
BUG-{YYYYMMDD}-{NNN}.md
例：BUG-20260520-001.md

# 架构决策
ADR-{NNN}-{标题}.md
例：ADR-001-组织架构存储选型.md
```

---

## 文档生命周期

```
草稿 → 评审中 → 已确认 → 已归档
         ↓         ↓
       已拒绝    已废弃
```

每篇文档必须在头部标注当前状态、版本、作者和日期。
