# Carlos Integration 文档管理体系

> **版本**: v1.0.0
> **更新日期**: 2026-05-23
> **适用范围**: carlos-integration 下所有微服务模块

---

## 设计理念

将每个微服务模块视为一个**独立的产品**，按照互联网产品全生命周期管理文档，覆盖从需求、设计、开发、测试到运维的完整闭环。

核心原则：

- **Markdown 优先**：所有文档使用 Markdown，便于版本控制和 Code Review
- **模板驱动**：统一模板确保文档质量，降低编写门槛
- **模块自治**：每个模块独立管理自己的文档，不依赖外部系统
- **Git 追踪**：文档随代码一起迭代，保留完整变更历史

---

## 模块清单

| 模块               | 说明   | 文档入口                           |
|------------------|------|--------------------------------|
| `carlos-audit`   | 审计服务 | [docs](./carlos-audit/docs/)   |
| `carlos-auth`    | 认证服务 | [docs](./carlos-auth/docs/)    |
| `carlos-gateway` | 网关服务 | [docs](./carlos-gateway/docs/) |
| `carlos-license` | 授权服务 | [docs](./carlos-license/docs/) |
| `carlos-message` | 消息服务 | [docs](./carlos-message/docs/) |
| `carlos-org`     | 组织服务 | [docs](./carlos-org/docs/)     |
| `carlos-system`  | 系统服务 | [docs](./carlos-system/docs/)  |
| `carlos-tools`   | 工具服务 | [docs](./carlos-tools/docs/)   |

---

## 目录结构总览

每个模块的 `docs/` 目录遵循以下统一结构：

```
docs/
├── 01-product/              # 产品层：为什么做（Why）
│   ├── PRD/                 #   产品需求文档
│   ├── BRD/                 #   商业需求文档
│   └── roadmap/             #   产品路线图
├── 02-design/               # 设计层：怎么做（How）
│   ├── architecture/        #   架构设计
│   ├── api/                 #   接口设计
│   ├── database/            #   数据库设计
│   ├── flow/                #   流程/时序图
│   └── decision/            #   架构决策记录 (ADR)
├── 03-develop/              # 开发层：具体实现
│   └── guide/               #   开发指南、本地调试
├── 04-test/                 # 测试层：质量保障
│   ├── plan/                #   测试计划
│   ├── case/                #   测试用例
│   └── report/              #   测试报告
├── 05-issue/                # 问题层：缺陷与风险
│   ├── bug/                 #   缺陷报告
│   ├── risk/                #   风险清单
│   └── review/              #   评审记录
├── 06-deploy/               # 运维层：上线与维护
│   ├── config/              #   配置说明
│   └── playbook/            #   运维手册 / SOP / 应急预案
└── assets/                  # 公共资源
    ├── diagrams/            #   图片、架构图、流程图
    └── mock/                #   Mock 数据、JSON 示例
```

### 编号规则 (`01-` ~ `06-`)

编号对应产品交付的**阶段顺序**，方便按阶段检索和权限管理：

| 编号   | 阶段 | 主要参与者     | 关键产出            |
|------|----|-----------|-----------------|
| `01` | 产品 | 产品经理      | PRD、BRD、Roadmap |
| `02` | 设计 | 架构师/后端/前端 | 架构设计、API 文档、ADR |
| `03` | 开发 | 开发工程师     | 开发指南、技术方案       |
| `04` | 测试 | 测试工程师     | 测试计划、用例、报告      |
| `05` | 问题 | 全员        | 缺陷报告、风险评估       |
| `06` | 运维 | 运维/SRE    | 部署手册、应急预案       |

---

## 文档模板

统一模板位于 `.doc-templates/`，各模块可复制使用：

| 模板       | 路径                                                  | 用途     |
|----------|-----------------------------------------------------|--------|
| PRD 模板   | `.doc-templates/product/PRD_TEMPLATE.md`            | 产品需求文档 |
| 架构设计模板   | `.doc-templates/design/ARCHITECTURE_TEMPLATE.md`    | 架构设计文档 |
| API 设计模板 | `.doc-templates/design/API_DESIGN_TEMPLATE.md`      | 接口设计文档 |
| ADR 模板   | `.doc-templates/design/ADR_TEMPLATE.md`             | 架构决策记录 |
| 测试计划模板   | `.doc-templates/test/TEST_PLAN_TEMPLATE.md`         | 测试计划   |
| 缺陷报告模板   | `.doc-templates/issue/BUG_TEMPLATE.md`              | 缺陷报告   |
| 运维手册模板   | `.doc-templates/deploy/DEPLOY_PLAYBOOK_TEMPLATE.md` | 部署运维手册 |

---

## 命名规范

### 文件命名

```
# 产品需求
PRD-{YYYYMMDD}-{需求简称}.md
例: PRD-20260520-操作日志导出.md

# 架构设计
ARCH-{主题}-{版本}.md
例: ARCH-审计存储架构-v1.0.md

# 接口设计
API-{模块}-{版本}.md
例: API-audit-v1.0.md

# 测试计划
TESTPLAN-{迭代}-{版本}.md
例: TESTPLAN-Sprint3-v1.0.md

# 缺陷报告
BUG-{YYYYMMDD}-{NNN}.md
例: BUG-20260520-001.md

# 架构决策
ADR-{NNN}-{标题}.md
例: ADR-001-审计日志存储选型.md
```

---

## 文档生命周期

```
草稿 → 评审中 → 已确认 → 已归档
         ↓         ↓
       已拒绝    已废弃
```

每篇文档头部必须包含：

- **状态**：当前所处阶段
- **版本**：语义化版本 (v1.0.0)
- **作者**：主要编写人
- **日期**：创建或最后更新日期

---

## 使用流程

### 新建一个需求

1. **产品阶段**：在 `01-product/PRD/` 复制模板，编写 PRD，提交评审
2. **设计阶段**：在 `02-design/architecture/` 和 `02-design/api/` 编写设计文档
3. **决策记录**：如有技术选型，在 `02-design/decision/` 记录 ADR
4. **开发阶段**：在 `03-develop/guide/` 补充开发相关说明
5. **测试阶段**：在 `04-test/plan/` 编写测试计划，测试完成后在 `04-test/report/` 归档报告
6. **问题跟踪**：发现问题在 `05-issue/bug/` 记录，修复后更新状态
7. **上线运维**：在 `06-deploy/playbook/` 维护部署和应急手册

### 评审规范

- PRD 和架构设计必须经过至少 1 人评审
- 评审意见直接记录在文档评论或 `05-issue/review/` 中
- 重大技术决策必须编写 ADR

---

## 最佳实践

1. **随代码一起提交**：文档修改与代码修改放在同一个 PR 中，保持同步
2. **及时归档**：已废弃的文档不要删除，移动到归档状态并说明原因
3. **图片管理**：所有图片统一放在 `assets/diagrams/`，使用相对路径引用
4. **空目录保留**：空目录通过 `.gitkeep` 保留，确保结构完整
5. **定期回顾**：每个 Sprint 结束检查文档是否与实际代码一致
