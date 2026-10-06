# 架构设计文档 - carlos-org - 组织模型设计

> **状态**: 已确认
> **版本**: v1.0.0
> **作者**: Carlos Team
> **日期**: 2026-09-22
> **关联PRD**: [PRD-用户中心需求文档](../../01-product/PRD/PRD-用户中心需求文档.md)
> **关联ADR**: [ADR-001](../decision/ADR-001-主部门真相源与部门归属语义.md) / [ADR-002](../decision/ADR-002-角色授权三机制语义与结算规则.md) / [ADR-003](../decision/ADR-003-权限资源与审计职责边界.md)

---

## 1. 设计概述

### 1.1 背景

carlos-org 承担着"部门、岗位、人员统一管理与用户中心"的职责，数据模型历经两轮演进：
初版只有 用户/部门/角色/权限 两轴（对齐若依），后引入完整的职位体系（职系/职级/岗位/任职）。
演进过程中产生了若干**结构性暧昧**——同一事实存在两条记录路径、同一语义存在三套机制，
且 PRD 与实现发生漂移（`org_permission` 未落地、审计需求已由 carlos-audit 接管）。
本文档对当前模型做正式定稿：明确每条关系路径的语义、每个事实的唯一真相源、各机制间的结算规则。

### 1.2 设计目标

- 用户/部门/职位/角色四者关系中，**每个事实有且只有一个真相源**，冗余字段必须声明其派生属性与收口写路径；
- 权限授权的三套机制（部门默认角色/岗位默认角色/显式授权）有明确的**结算规则与优先级**;
- 与 carlos-auth（认证）、carlos-system（权限资源）、carlos-audit（审计）的职责边界清晰无重叠。

### 1.3 约束条件

- 表结构以 [org_postgresql.sql](../database/org_postgresql.sql)（2026-07-03 版）为准，本文档不做破坏性表结构变更；
- 数据权限的运行时实现已建立在 `org_user.main_dept_id` 之上（`OrgDataScopeHelper`)，真相源收口必须兼容该现状；
- 认证链路已生产化：carlos-auth 经 Feign(`ApiOrgUser`/`ApiOrgUserRole`/`ApiOrgRole`）读取 org 数据并签发 JWT。

---

## 2. 总体架构

### 2.1 三轴模型

carlos-org 采用业界标准（SAP OM / Workday / 钉钉）的**三轴正交模型**:

| 轴 | 回答的问题 | 核心实体 | 业界对标 |
|---|---|---|---|
| **组织轴**（行政归属） | 这人归哪管？ | `org_department` 部门树、`org_user_department` 归属 | 所有系统必备 |
| **人事轴**（HR 任职） | 这人干什么、什么级别、向谁汇报？ | 职系→职级→岗位→任职 四层 | SAP OM 的 O/C/S/P |
| **权限轴**（授权） | 这人能看什么、能做什么？ | `org_role`、`org_role_operation`、数据范围 | RBAC + data scope |

三轴的设计原则：

1. **正交**：三轴各自独立演进，通过桥接表关联，不互相内嵌字段（岗位不内嵌权限、部门不内嵌角色）;
2. **人岗分离**：岗位是组织的"坑"，人是填坑的"萝卜"——人员离职/调岗不影响岗位定义与编制；
3. **桥接收敛**：人事轴与权限轴之间的桥是**岗位默认角色**（"人因岗而有权")，详见 ADR-002。

### 2.2 模块边界

```
┌─────────────────┐   Feign 读用户/角色    ┌─────────────────┐
│  carlos-auth    │ ◄──────────────────── │   carlos-org    │
│  认证授权(IdP)   │                       │  组织与用户中心   │
│  - 签发 JWT      │                       │  - 三轴数据模型   │
│  - 不落库存用户   │                       │  - 授权事实表     │
└─────────────────┘                       └────────┬────────┘
                                                   │ operation_id 引用
                                          ┌────────▼────────┐
                                          │  carlos-system  │
                                          │  权限资源定义     │
                                          │  sys_menu/       │
                                          │  sys_operation   │
                                          └─────────────────┘
┌─────────────────┐
│  carlos-audit   │  操作/登录/审计日志统一由本模块承载(ClickHouse)
└─────────────────┘  carlos-org 不再建设 org_*_log 表
```

**身份数据归属**:`org_user`（含 `pwd`）是认证身份的唯一存储，carlos-auth 只读不复制。

---

## 3. 详细设计

### 3.1 组织轴：部门树与行政归属

#### 3.1.1 部门树（org_department)

- 结构：`parent_id`（父指针）+ `path`（物化路径，如 `/1/5/23/`)+ `level`（层级深度）三件套；
- 子孙查询走 `path` 前缀匹配 + Redis 部门树缓存（`OrgDepartmentManagerImpl.getDescIdsFromCache`)，不做递归 SQL;
- 移动部门时必须批量重写所有子孙节点的 `path`;
- `leader_id`（负责人）：当前 DDL 为 NOT NULL，**计划调整为可空**（先搭部门架子后配负责人是常态；PRD"负责人必须是本部门成员"的规则存在鸡生蛋问题，同步废弃）。

#### 3.1.2 用户-部门归属（org_user_department)

- 一人多部门，多对多；`is_main_dept` 标识主部门，**每用户有且仅有一条主部门记录**;
- **真相源规则（ADR-001)**:`org_user_department` 是归属关系的唯一真相源；`org_user.main_dept_id` 是**派生冗余列**，服务于数据权限与用户列表过滤，写路径统一收口于 `OrgUserService.assignDepartments`，禁止绕过；
- 行政归属驱动：数据权限（`OrgDataScopeHelper`)、通讯录/部门人员列表、登录时 `UserContext.deptId`。

### 3.2 人事轴：职位体系与任职

#### 3.2.1 四层结构

```
org_position_category (职系: M管理/T技术/P专业/S销售)
    │ 1-N (category_id)
    ▼
org_position_level (职级: T1/T2-1/M3, level_seq 排序, 薪资带)
    ▲ default_level_id / level_range
org_position (岗位: 编制 headcount, 部门 department_id, 汇报 parent_id)
    │ 1-N
    ▼
org_user_position (任职: user_id + position_id + level_id + department_id)
```

对齐 SAP OM：`org_position_level` ≈ Job(C),`org_position` = Position(S),`org_user_position` = Holder(P 与 S 的任职关系）。

#### 3.2.2 任职（org_user_position)

- 是**任职记录**而非简单关联表：承载主岗/兼岗（`is_main`)、任职方式（`appoint_type`)、任职日期、试用期、成本分摊（`cost_ratio`)、任职状态机（`position_status`);
- 唯一约束 `(user_id, position_id, department_id)`：允许同岗跨部门兼职；
- 主岗互斥由应用层保证（`assignPositions`/`clearMainPosition`);
- **任职部门 vs 行政归属（ADR-001)**:`department_id` 是 HR 事实记录，驱动编制统计与任职历史；**不直接驱动数据权限**。任命时校验：任职部门必须在用户行政归属集合内，否则自动补充一条非主部门归属记录。

#### 3.2.3 汇报线（双层）

- **岗位汇报线** `org_position.parent_id`：静态默认，定义"这个岗向哪个岗汇报";
- **任职汇报线** `org_user_position.report_to / dotted_report`：人的实线/虚线汇报（矩阵管理），可覆盖岗位默认；
- 规则：任命时若未显式指定 `report_to`，沿岗位汇报线取上级岗位的现任者作为默认值；查询汇报关系时以任职汇报线为准，岗位汇报线兜底。

#### 3.2.4 编制管理

- 编制在**岗位级**(`headcount`,`0`=不限）,`current_count`/`vacancy_count` 为冗余统计，由 `refreshPositionStats` 在任命/卸任后重算；
- 任命时超编拦截（"该岗位编制已满");
- 已知风险：拦截与计数非原子，并发任命存在超编竞态（见 §6 风险）;
- PRD 的"部门级编制"未实现，部门编制 = 部门下各岗位编制之和（派生值，不落字段）。

### 3.3 权限轴：角色、授权与数据范围

#### 3.3.1 角色（org_role)

- `role_type`：系统角色(1)/自定义角色(2)；系统角色不可删除；
- `data_scope`：数据范围五档（全部/本部及子部门/仅本部门/仅本人/自定义），运行时由 `OrgDataScopeHelper` 取用户所有角色中**最宽**的一档生效。

#### 3.3.2 授权三机制与结算规则（ADR-002)

| 机制 | 表 | 语义 | 角色 |
|---|---|---|---|
| 显式授权 | `org_user_role` | 管理员直接授予，**唯一生效的授权事实表** | 结算终点 |
| 岗位默认角色 | `org_position_role.is_default` | 任职时自动写入 `org_user_role`（主派生路径） | 派生来源① |
| 部门默认角色 | `org_department_role.is_default_role` | 加入部门且岗位无默认角色时的兜底授予 | 派生来源② |

结算规则：**生效角色集 = org_user_role 中未过期（expire_time 未过）且作用域匹配（dept_id 为空或匹配当前部门）的角色集合**。
入职/调岗/调部门时按 ①→② 顺序结算并落 `org_user_role`；卸任/调出不自动收回显式授权（避免误删管理员手工授权），派生授权通过来源标记回收（来源字段为后续增强项）。

#### 3.3.3 权限资源（ADR-003)

- 权限资源（菜单/按钮/API）定义归 carlos-system(`sys_menu`/`sys_operation`);org 只持有 `org_role_operation` 授权关联；
- PRD 的"数据字段"级权限不落地为授权类型，敏感字段脱敏由框架 translation/encrypt 能力承担；
- 菜单可见性当前不受角色控制（动态路由模式下 `sys_menu.meta.roles` 为死字段）——**已知缺口**，角色级菜单可见性为后续专项。

#### 3.3.4 数据权限运行时

- `OrgDataScopeHelper` 基于 `UserContext`(JWT）中的角色集取最宽 `data_scope`;
- 部门条件基于 `org_user.main_dept_id`（派生冗余列）与部门子孙缓存展开；
- CUSTOM(5）暂按"仅本人"兜底，自定义规则引擎未实现；
- 当前仅覆盖用户/部门两类查询，业务模块接入需复用同一 Helper。

### 3.4 实体关系总图

```
        组织轴                              人事轴
┌───────────────────┐            ┌────────────────────────┐
│  org_department   │            │ org_position_category  │
│  (parent_id/path) │            │        ↓ 1-N           │
└───────┬───────────┘            │  org_position_level    │
        │                        └───────────┬────────────┘
        │ department_id                      │ default_level_id
        │                         ┌──────────▼────────────┐
        └────────────────────────►│    org_position       │◄── parent_id 岗位汇报线
        │                         │  (headcount 编制)      │
        │                         └──────────┬────────────┘
        │ department_id(任职归属)            │ 1-N
        │                         ┌──────────▼────────────┐
┌───────┴───────────┐   user_id   │  org_user_position    │ report_to/dotted_report
│     org_user      │◄────────────│  (任职记录+状态机)      │ 任职汇报线
└───────┬───────────┘             └───────────────────────┘
        │
        │ org_user_department (is_main_dept) ──► org_department   [行政归属]
        │
        │ org_user_role (dept_id 作用域, expire_time 失效) ──► org_role
        │                                                          │
        │              org_position_role (岗位默认角色) ────────────┤
        │              org_department_role (部门默认角色) ──────────┤
        │                                                          ▼
        │                              org_role_operation ──► sys_operation (carlos-system)
        │
        └── org_user.main_dept_id = is_main_dept 的派生冗余(数据权限消费)
```

### 3.5 真相源与派生冗余总表

| 事实 | 唯一真相源 | 派生冗余 | 收口写路径 |
|---|---|---|---|
| 用户主部门 | `org_user_department.is_main_dept` | `org_user.main_dept_id` | `OrgUserService.assignDepartments` |
| 用户部门归属 | `org_user_department` | 无 | `assignDepartments` |
| 用户任职 | `org_user_position` | `org_position.current_count/vacancy_count` | `assignPositions`/`assignPosition`/`resignPosition` |
| 用户生效角色 | `org_user_role`（未过期且作用域匹配） | JWT 中的 roleCodes | 结算规则（ADR-002) |
| 汇报关系 | `org_user_position.report_to/dotted_report` | `org_position.parent_id`（默认来源） | 任命/调岗流程 |
| 部门树祖先 | `org_department.path` | Redis 部门树缓存 | 新增/移动部门 |
| 权限资源 | `sys_menu`/`sys_operation`(carlos-system) | 无 | carlos-system 管理面 |

---

## 4. 关键技术决策

| 决策点 | 选项A | 选项B | 最终选择 | 理由 |
|---|---|---|---|---|
| 主部门存储 | 仅用户表字段 | 仅关联表标记 | 关联表为真相源+用户表冗余 | 见 ADR-001 |
| 部门归属主从 | 行政归属为准 | 任职归属为准 | 行政归属驱动权限，任职驱动编制 | 见 ADR-001 |
| 角色派生路径 | 部门驱动 | 岗位驱动 | 岗位为主、部门兜底、显式授权为终点 | 见 ADR-002 |
| 权限资源归属 | org 自建 org_permission | 复用 carlos-system | 复用 sys_operation | 见 ADR-003 |
| 审计日志 | org 自建三张日志表 | 复用 carlos-audit | 复用 carlos-audit | 见 ADR-003 |

---

## 5. 非功能设计

### 5.1 性能

- 部门树：Caffeine + Redis 多级缓存（见 [ARCH-部门缓存设计方案](./ARCH-部门缓存设计方案.md));
- 子孙展开走缓存（`getDescIdsFromCache`)，不做递归 SQL;
- 角色码在登录时结算进 JWT，请求期零数据库往返。

### 5.2 多租户

- `OrgTenantLineHandler` 对核心 10 表注入 `tenant_id` 条件；
- **已知缺陷**:`org_position_category/level/position_role/position_history` 有 `tenant_id` 列但未纳入拦截清单；`org_user_department` 无 `tenant_id` 列却在清单中（见 §6 R-1)。

### 5.3 安全

- 密码由 carlos-auth 认证链路使用，org 侧不落明文、不明文传输；
- 敏感字段（手机号/证件/邮箱）脱敏为 VO 层待办项（PRD §5.2);
- 数据权限见 §3.3.4。

---

## 6. 风险与已知缺陷

| 编号 | 级别 | 内容 | 处置 |
|---|---|---|---|
| R-1 | 高 | 租户拦截清单与表结构不一致（§5.2) | ✅ 已修复（2026-09-22)：移除无 tenant_id 列的 org_user_department/org_docking_mapping，补入职位体系 4 张表 |
| R-2 | 高 | `org_user_role` 生效判定未过滤 `expire_time`/`dept_id` | ✅ expire_time 已修复（2026-09-22,listByUserId 过滤、管理面走 listAllByUserId);dept_id 作用域消费待 ADR-002 实施项 |
| R-3 | 高 | `org_position_history` 三列 NOT NULL 但 `assignPositions` 写历史时未赋值 | ✅ 已修复（2026-09-22)：补 changeReason/operatorId/operatorName 赋值与 system 兜底 |
| R-4 | 中 | 编制超编拦截与计数重算非原子，并发任命可超编 | 整改任务（悲观锁/原子计数） |
| R-5 | 中 | 任职汇报线无维护逻辑，任命不写 `report_to`，字段空转 | 整改任务 |
| R-6 | 中 | 任职状态枚举顺序与生命周期不一致，状态机流转校验未实现 | 整改任务 |
| R-7 | 低 | `org_user_role` 缺 `(user_id, role_id, dept_id)` 唯一索引，可重复授权 | 下次 migration |
| R-8 | 低 | `org_user_department` 物理删除、无审计字段，与其他关联表风格不一 | 下次 migration |
| R-9 | 低 | `org_department.leader_id` NOT NULL 不合理 | 下次 migration |

回滚：本文档为设计定稿，不涉及代码变更；整改任务按项独立提交、独立回滚。

---

## 7. 附录

### 7.1 与 PRD 的关系

PRD v1.1.0 起，§2.2 实体关系图、§3.3.2 权限管理、§3.5 审计模块以本文档与 ADR-003 为准；PRD 保留功能需求与业务规则层面的描述。

### 7.2 变更记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| v1.0.0 | 2026-09-22 | 初始版本：三轴模型定稿、真相源总表、三篇 ADR 索引、已知缺陷清单 | Carlos Team |
