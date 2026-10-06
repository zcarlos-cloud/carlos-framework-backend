# API 设计文档 - carlos-org 远程查询接口

> **状态**: 已确认
> **版本**: v1.2.0
> **作者**: Carlos Team
> **日期**: 2026-09-25

---

## 1. 接口概览

### 1.1 服务信息

| 项目   | 内容                              |
|------|---------------------------------|
| 服务名  | `carlos-org`（`ServiceNameConstant.SERVICE_NAME`） |
| 协议   | HTTP（Spring Cloud OpenFeign，服务名直连） |
| 鉴权方式 | 网关/OAuth2 Bearer（内部服务间调用）        |
| 响应包装 | `Result<T>`（`success/code/msg/data/timestamp/details`） |
| 接口所在模块 | `carlos-org-api` |

> **模块归属变更（2026-09-24）**：position 的 `api` / `ao` / `param` / `fallback` 已从 `carlos-org-bus`
> 迁入 `carlos-org-api`，包名 `com.carlos.org.position.api.*` **保持不变**（因此无任何 import 需要改动）。
> 目的是让只依赖 `carlos-org-api` 的消费方（如嵌入式工作流 starter）**不必依赖 `carlos-org-bus`**
> 就能调用 `listSubordinatesByUserId`，从而不破坏 api/bus 隔离。
> `entity` / `mapper` / `manager` / `service` / `convert` / `apiimpl` / `controller` 仍留在 `carlos-org-bus`。
>
> **行为影响**：`carlos-org-api` 的消费方现在会**额外注册 6 个 Feign 客户端**
> （contextId：`position` / `positionCategory` / `positionLevel` / `positionHistory` / `positionRole` / `userPosition`；
> 已核验全模块共 13 个 contextId **无重复**）。若消费方使用 `@EnableFeignClients` 按包扫描，会因此多出这 6 个客户端实例。

### 1.2 通用约定

1. **类级路径前缀**由 Spring Cloud `FeignClientFactoryBean` 按 `@FeignClient(path = "...")` 在客户端拼接，
   服务端 Controller 的 `@RequestMapping` 与之对齐；
2. **参数缺失返回 400**（`@RequestParam` 必填，与既有接口一致）：
   `?id=`、`?deptId=`、`?roleId=`、`?userId=` 这类「只有参数名、没有值」的请求同样按缺失处理（Long 转 null）；
   唯一例外是集合参数 `ids`：`?ids`（无值）会被转成**空集合**并按空集合语义返回 200；
3. **空集合参数**：Feign 客户端在集合为空时发送 `?ids`（无值），服务端返回 200 + 空数组，不查库；
4. **枚举降级**：AO 中的所有枚举字段（状态、性别等）均降级为 `Integer`。

### 1.3 接口清单

本次新增（远程查询能力，供嵌入式工作流、消息中心等外部模块解析办理人）：

| 序号 | 接口名                           | 路径                                                  | 方法  | 状态  |
|----|-------------------------------|-----------------------------------------------------|-----|-----|
| 1  | 根据用户ID查询用户                     | `/api/org/user/user/by-id`                           | GET | 已确认 |
| 2  | 根据用户ID集合批量查询用户                 | `/api/org/user/user/list-by-ids`                     | GET | 已确认 |
| 3  | 查询用户的直属下级                      | `/api/org/user/position/listSubordinatesByUserId`    | GET | 已确认 |
| 4  | 根据部门ID查询部门下的用户                 | `/api/org/department/listUsersByDeptId`              | GET | 已确认 |
| 5  | 根据角色ID查询持有该角色的用户               | `/api/org/role/listUsersByRoleId`                    | GET | 已确认 |

本次同时新增 5 个**显式租户变体**（`...AndTenant`，语义与上表逐一对应，只是把租户条件从
「隐式取当前登录上下文」改为「显式传入」，供无请求上下文的调用方使用，详见 1.4）：

| 序号 | 接口名                                    | 路径                                                             | 方法  | 状态  |
|----|----------------------------------------|----------------------------------------------------------------|-----|-----|
| 6  | 根据用户ID + 租户查询用户                       | `/api/org/user/user/by-id-and-tenant`                          | GET | 已确认 |
| 7  | 根据用户ID集合 + 租户批量查询用户                    | `/api/org/user/user/list-by-ids-and-tenant`                    | GET | 已确认 |
| 8  | 查询用户在指定租户内的直属下级                        | `/api/org/user/position/listSubordinatesByUserIdAndTenant`     | GET | 已确认 |
| 9  | 根据部门ID + 租户查询部门下的用户                    | `/api/org/department/listUsersByDeptIdAndTenant`               | GET | 已确认 |
| 10 | 根据角色ID + 租户查询持有该角色的用户                  | `/api/org/role/listUsersByRoleIdAndTenant`                     | GET | 已确认 |

v1.2 新增（**直属上级 / 递归部门用户 / 岗位用户**，供工作流办理人词汇
`upLeader`、`dept:{id}:rec`、`position:{id}` 解析使用）：

| 序号 | 接口名                        | 路径                                                          | 方法  | 状态  |
|----|----------------------------|-------------------------------------------------------------|-----|-----|
| 11 | 查询用户的直属上级                  | `/api/org/user/position/getSuperiorByUserId`                 | GET | 已确认 |
| 12 | 根据部门ID查询该部门及其全部子孙部门下的用户      | `/api/org/department/listUsersByDeptIdRecursive`             | GET | 已确认 |
| 13 | 根据岗位ID查询任职该岗位的用户           | `/api/org/user/position/listUsersByPositionId`               | GET | 已确认 |
| 14 | 查询用户在指定租户内的直属上级            | `/api/org/user/position/getSuperiorByUserIdAndTenant`        | GET | 已确认 |
| 15 | 根据部门ID + 租户递归查询子孙部门下的用户     | `/api/org/department/listUsersByDeptIdRecursiveAndTenant`    | GET | 已确认 |
| 16 | 根据岗位ID + 租户查询任职该岗位的用户       | `/api/org/user/position/listUsersByPositionIdAndTenant`      | GET | 已确认 |

14~16 是 11~13 的**显式租户变体**，差异与选用规则同 1.4。

已有接口（本次未改动，仅列出便于对照）：

| 接口                        | 路径                                       | 方法  |
|---------------------------|------------------------------------------|-----|
| `ApiOrgUser.getUserByIdentifier`        | `/api/org/user/user/by-identifier`                   | GET |
| `ApiOrgUser.getUserByIdentifierAndTenant` | `/api/org/user/user/by-identifier-and-tenant`        | GET |
| `ApiOrgUser.changeState`     | `/api/org/user/user/changeState`         | POST |
| `ApiOrgUser.updatePassword`  | `/api/org/user/user/updatePassword`      | POST |
| `ApiOrgDepartment.getDepartmentById` | `/api/org/department/getById`            | GET |
| `ApiOrgRole.getRoleById`     | `/api/org/role/getById`                  | GET |
| `ApiOrgRole.getRoleByCode`   | `/api/org/role/getByCode`                | GET |
| `ApiOrgUserDepartment.listByUserId` | `/api/org/user/department/listByUserId`  | GET |
| `ApiOrgUserRole.listByUserId` | `/api/org/user/role/listByUserId`        | GET |
| `ApiOrgDepartmentRole.listByDeptId` | `/api/org/department/role/listByDeptId` | GET |
| `ApiOrgTenant.getByCode`     | `/api/org/tenant/tenant/by-code`         | GET |

> 说明：`ApiOrgUser` 的 Feign 基础路径为 `/api/org/user`，其方法路径历史上带 `/user/` 前缀，
> 因此实际地址出现 `/api/org/user/user/...` 的重复段；本次新增接口沿用该既有风格，不做修正（避免破坏已上线调用方）。

### 1.4 何时使用 `AndTenant` 变体

上表两组接口（1~5 与 6~10）是**同一批查询**，差别只在租户条件怎么来：

| | 普通变体（1~5） | `AndTenant` 变体（6~10） |
|---|---|---|
| 租户来源 | `OrgTenantLineHandler` 隐式读取 `SecurityUtils.getCurrentUser().getTenantId()` | 调用方**显式传入**，绕过租户拦截器后按 `tenant_id` 过滤 |
| 适用链路 | 用户请求（浏览器 → 网关 → 服务） | 后台任务、系统间调用（**无请求上下文**） |
| 无上下文时的行为 | 静默回落到 `carlos.org.tenant.platform-tenant-id`（默认平台租户 `0`） | 由调用方指定租户；`tenantId` 为空则快速失败（不查库） |

原因：框架的 `FeignRequestInterceptor` 只会从**当前 `HttpServletRequest`** 透传
`X-Tenant-Id` / `X-User-Id`。

- **用户请求链路是安全的**：请求头由网关带进来，租户自动传播，用普通变体即可；
- **后台 / 无请求上下文的链路不安全**：例如 `carlos-job` 调度线程执行「超时升级上级」时，
  线程内没有 `HttpServletRequest`，什么都不会透传，租户条件于是静默回落到平台租户 ——
  在多租户部署下会解析出**错误租户的用户**，而且不报错（这正是引入 `AndTenant` 变体的原因）。

因此：

1. 有请求上下文 → 用普通变体（1~5），**不要**手写 `tenantId`；
2. 无请求上下文 → 用 `AndTenant` 变体（6~10），由调度方/调用方提供 `tenantId`；
3. `AndTenant` 变体**不是**普通变体的冗余副本，请勿以「看起来重复」为由删除。

行为约定（5 个变体一致）：

- `tenantId` 为 `null` → 快速失败，不查库：单对象返回 `data = null`，列表返回 200 + 空数组；
- 空 `ids` 集合 → 返回 200 + 空数组，不查库（与普通变体一致）；
- 除租户条件外的语义（过期授权过滤、非递归部门语义、只查直接下级、按 `user_id` 去重等）
  与对应的普通变体**完全一致**；
- 注意 `org_user_department` 表自身没有 `tenant_id` 列，变体 9 的租户条件落在关联的
  `org_user` 行上。

---

## 2. 接口详情

> 2.1~2.5 为普通变体（1~5），租户条件隐式取自当前登录上下文；每个接口都有一个显式租户的
> `AndTenant` 兄弟接口（6~10），差异与选用规则见 1.4，合并说明见 2.6。

### 2.1 根据用户ID查询用户

#### 基础信息

- **接口路径**: `GET /api/org/user/user/by-id`
- **Feign 客户端**: `ApiOrgUser#getUserById(Long id)`
- **接口说明**: 按主键查询单个用户，替代原先只能按账号/手机号查询的限制。

#### 请求参数

| 参数名 | 类型   | 必填 | 说明   |
|-----|------|----|------|
| id  | Long | 是  | 用户ID |

#### 响应参数

```json
{
  "success": true,
  "code": "00000",
  "msg": "操作成功",
  "data": {
    "id": 1958372912345678901,
    "account": "zhangsan",
    "nickname": "张三",
    "phone": "13800000000",
    "email": "zhangsan@example.com",
    "gender": 1,
    "userStatus": 1,
    "mainDeptId": 1958372912345678000,
    "tenantId": 1
  },
  "timestamp": 1758691200000
}
```

#### 业务规则

- `id` 为必填，缺失或空值返回 400；
- 用户不存在或已逻辑删除时 `data = null`（由调用方按 `null` 降级处理）；
- 走租户拦截器，仅返回当前调用上下文租户内的用户；
  无请求上下文时会回落到平台租户，此类调用方请改用变体 6（见 1.4）。

---

### 2.2 根据用户ID集合批量查询用户

#### 基础信息

- **接口路径**: `GET /api/org/user/user/list-by-ids`
- **Feign 客户端**: `ApiOrgUser#listUsersByIds(Collection<Long> ids)`
- **接口说明**: 批量解析用户，供待办列表、消息路由等场景使用，避免逐条调用（N+1）。

#### 请求参数

| 参数名 | 类型            | 必填 | 说明                       |
|-----|---------------|----|--------------------------|
| ids | Collection\<Long\> | 是  | 用户ID集合，重复查询参数传递 `?ids=1&ids=2` |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1）。

#### 业务规则

- Feign 在集合为空时发送 `?ids`（无值），服务端按空集合处理：返回 200 + 空数组，**不产生数据库查询**；
- 完全不携带 `ids` 参数（手写 HTTP）返回 400；
- id 先 `distinct()` 去重后一次 `selectBatchIds` 查询，返回条数 ≤ 入参去重后的条数；
- 不存在的 id 静默忽略，不抛异常（调用方需自行按 id 对齐结果）；
- 也支持逗号分隔写法 `?ids=1,2`；
- 建议单次不超过 500 个 id（GET 查询串长度限制）。

---

### 2.3 查询用户的直属下级

#### 基础信息

- **接口路径**: `GET /api/org/user/position/listSubordinatesByUserId`
- **Feign 客户端**: `ApiOrgUserPosition#listSubordinatesByUserId(Long userId)`
- **接口说明**: 按汇报关系（`org_user_position.report_to`）返回直接下级，支撑「审批人 = 直属上级」「超时升级上级」。

#### 请求参数

| 参数名    | 类型   | 必填 | 说明     |
|--------|------|----|--------|
| userId | Long | 是  | 上级用户ID |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1），按 `user_id` 去重。

#### 业务规则

- 数据源是**任职记录**而非部门树：`report_to = userId` 的全部任职记录（含兼岗）→ 取 `user_id` 去重 → 批量查用户；
- **仅直接下级，不递归**；追多层需要调用方循环调用；
- **不按任职状态过滤**：`position_status = 5`（已卸任）的记录若 `report_to` 未清理也会返回，调用方如需活跃人员请结合业务判断；
- `userId` 为必填，缺失返回 400；
- 该接口是仓库内 `report_to` 的**第一个读取路径**（此前字段只有写入、没有查询）。

---

### 2.4 根据部门ID查询部门下的用户

#### 基础信息

- **接口路径**: `GET /api/org/department/listUsersByDeptId`
- **Feign 客户端**: `ApiOrgDepartment#listUsersByDeptId(Long deptId)`
- **接口说明**: 返回通过 `org_user_department` 直接归属该部门的用户。

#### 请求参数

| 参数名    | 类型   | 必填 | 说明   |
|--------|------|----|------|
| deptId | Long | 是  | 部门ID |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1）。

#### 业务规则

- **语义：非递归**。只返回直接归属该部门的用户，**不包含子部门**用户；需要子树人员时由调用方先解析部门层级再逐级调用；
- 该语义与同 JVM 的 `OrgDepartmentManager#getUsersByDeptId` 完全一致，不做「远端递归、本地非递归」的行为分叉；
- 具备多部门归属的用户可能出现在多个部门的返回结果中；
- `deptId` 为必填，缺失返回 400。

---

### 2.5 根据角色ID查询持有该角色的用户

#### 基础信息

- **接口路径**: `GET /api/org/role/listUsersByRoleId`
- **Feign 客户端**: `ApiOrgRole#listUsersByRoleId(Long roleId)`
- **接口说明**: 按角色解析用户集合，供 **不依赖 carlos-org 的模块**（如 `carlos-message`、嵌入式工作流）在调用自身逻辑前完成候选人员解析。

#### 请求参数

| 参数名    | 类型   | 必填 | 说明   |
|--------|------|----|------|
| roleId | Long | 是  | 角色ID |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1），按 `user_id` 去重。

#### 业务规则

- 数据源为 `org_user_role`（ADR-002 定义的角色授权唯一事实表）；
- **仅生效授权**：`expire_time IS NULL OR expire_time > now()`，已过期的临时授权不计入，口径与 `OrgUserRoleManager#listByUserId`（token 签发链路）一致；
- **不做 `dept_id` 作用域过滤**：部门内生效的授权（`dept_id` 非空）同样计入返回结果；
- 与 `OrgRoleManager#getUserCountByRoleId`（角色详情页的「用户数」，不做过期过滤）口径不同，两者数值可能不一致；
- 一次查询取回 id 集合后批量取用户，无 N+1；
- `roleId` 为必填，缺失返回 400。

---

### 2.6 `AndTenant` 变体的接口详情（6~10）

5 个变体的路径、参数与响应只需在 2.1~2.5 的基础上追加一个 `tenantId` 查询参数，其余完全一致，
因此合并说明：

| 序号 | 接口路径 | Feign 客户端 | 请求参数 | 响应 |
|---|---|---|---|---|
| 6 | `GET /api/org/user/user/by-id-and-tenant` | `ApiOrgUser#getUserByIdAndTenant(Long id, Long tenantId)` | `id`、`tenantId` | 同 2.1，`data` 为 `OrgUserAO`；`tenantId` 为空返回 `data = null` |
| 7 | `GET /api/org/user/user/list-by-ids-and-tenant` | `ApiOrgUser#listUsersByIdsAndTenant(Collection<Long> ids, Long tenantId)` | `ids`（重复参数或逗号分隔）、`tenantId` | 同 2.2 |
| 8 | `GET /api/org/user/position/listSubordinatesByUserIdAndTenant` | `ApiOrgUserPosition#listSubordinatesByUserIdAndTenant(Long userId, Long tenantId)` | `userId`、`tenantId` | 同 2.3 |
| 9 | `GET /api/org/department/listUsersByDeptIdAndTenant` | `ApiOrgDepartment#listUsersByDeptIdAndTenant(Long deptId, Long tenantId)` | `deptId`、`tenantId` | 同 2.4 |
| 10 | `GET /api/org/role/listUsersByRoleIdAndTenant` | `ApiOrgRole#listUsersByRoleIdAndTenant(Long roleId, Long tenantId)` | `roleId`、`tenantId` | 同 2.5 |

#### 业务规则

- `tenantId` 必填；为 `null` 时快速失败且不查库（单对象 `data = null`，列表空数组）；
- 除租户条件外，业务规则与 2.1~2.5 完全一致（含 ADR-002 过期授权过滤、非递归部门语义、
  「仅直接下级、不递归」、按 `user_id` 去重等）；
- 变体 9 先按 `org_user_department.dept_id` 取直属用户ID（该表无 `tenant_id` 列），
  再按 `org_user.tenant_id` + 用户ID 批量取用户；
- 变体 8、10 采用两段查询（先取 id 集合，再批量取用户），两段使用**同一个** `tenantId`。

---

### 2.7 查询用户的直属上级（11）

#### 基础信息

- **接口路径**: `GET /api/org/user/position/getSuperiorByUserId`
- **Feign 客户端**: `ApiOrgUserPosition#getSuperiorByUserId(Long userId)`
- **接口说明**: 读该用户**自己**任职记录上的 `org_user_position.report_to`，返回其直接上级；
  支撑「申请人 → 直属上级」的审批路由（工作流动态词汇 `upLeader`）。

#### 请求参数

| 参数名    | 类型   | 必填 | 说明   |
|--------|------|----|------|
| userId | Long | 是  | 用户ID |

#### 响应参数

`data` 为单个 `OrgUserAO`（结构同 2.1）。

#### 业务规则

- **方向**：与 2.3（`listSubordinatesByUserId`）**相反**。2.3 查 `report_to = userId` 返回**下级**；
  本接口查 `user_id = userId` 取该用户自己的 `report_to` 后再按 id 取用户。
  两者不可互相替代——这是初稿里被写错、现已更正的一点；
- **兼岗取数**：一个用户有多条任职记录（兼岗）时，优先主岗位（`is_main = true`）记录的 `report_to`；
  主岗位未配置 `report_to` 或没有主岗位记录时，回退为**任意一条已配置** `report_to` 的记录；
- **空值不是错误**：没有任职记录、或 `report_to` 未配置时返回 `data = null`（如组织架构顶层）。
  这与「用户不存在」不区分，调用方按 `null` 降级处理；
- 不做任职状态过滤（`position_status` 不参与条件），口径与 2.3 一致；
- `userId` 为必填，缺失返回 400；
- 用户被逻辑删除时同样返回 `data = null`（按 id 取用户的既有语义）。

---

### 2.8 递归查询部门及其子孙部门下的用户（12）

#### 基础信息

- **接口路径**: `GET /api/org/department/listUsersByDeptIdRecursive`
- **Feign 客户端**: `ApiOrgDepartment#listUsersByDeptIdRecursive(Long deptId)`
- **接口说明**: 返回该部门**及其全部子孙部门**下的用户；支撑工作流静态词汇 `dept:{id}:rec`。

#### 请求参数

| 参数名    | 类型   | 必填 | 说明        |
|--------|------|----|-----------|
| deptId | Long | 是  | 部门ID（子树的根） |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1），按 `user_id` 去重。

#### 业务规则

- **语义：递归**，且**部门集合在服务端展开**：调用方只发**一次**远程调用。
  刻意**不允许**调用方「先查子部门、再逐个子部门调用 2.4」——那会产生 N+1 次远程往返；
- 展开方式：优先命中部门的「全部子孙」缓存（`dept:desc:{id}`，由 `initCache` 预热、跨级移动时重建）；
  缓存里没有该部门时，再看该部门本体是否在缓存里——不在则逐层下探数据库补齐，避免缓存未预热时**静默漏人**
  （漏人会让审批节点少一批办理人，甚至无人可办）；
- 命中的部门集合 = 传入部门 **自身 + 全部子孙**；
- 归属判定用 `org_user_department.dept_id`，一个人可能同时属于子树里的多个部门，结果按 `user_id` 去重；
- 与 2.4（非递归）**语义不同**，2.4 保持不变，两者不可混用；
- `deptId` 为必填，缺失返回 400。

---

### 2.9 查询任职某岗位的用户（13）

#### 基础信息

- **接口路径**: `GET /api/org/user/position/listUsersByPositionId`
- **Feign 客户端**: `ApiOrgUserPosition#listUsersByPositionId(Long positionId)`
- **接口说明**: 按 `org_user_position.position_id` 解析岗位上的用户；支撑工作流静态词汇 `position:{id}`。

#### 请求参数

| 参数名        | 类型   | 必填 | 说明   |
|------------|------|----|------|
| positionId | Long | 是  | 岗位ID |

#### 响应参数

`data` 为 `OrgUserAO` 数组（结构同 2.1），按 `user_id` 去重。

#### 业务规则

- 先取岗位上的 `user_id`（去重，含兼岗产生的多条记录），再批量取用户，无 N+1；
- **刻意不按 `position_status` 过滤**，口径与 2.3（`listSubordinatesByUserId`）一致：
  已卸任（`position_status = 5`）的记录若未清理也会返回。理由：`position_status = 2`（试用期）
  属于正常在职状态，按「仅在职（1）」过滤会漏人；是否排除已卸任由调用方按业务判断；
- ⚠️ 与 `OrgUserPositionManager#countOccupiedByPositionId`（岗位编制占用人数，**只数在职**）
  口径**不同**，两者数值本就不应一致，不可互相校验；
- 岗位不存在、或无人在岗时返回 200 + 空数组（不报错）；
- `positionId` 为必填，缺失返回 400。

---

### 2.10 本轮新增的 `AndTenant` 变体（14~16）

路径、参数与响应只需在 2.7~2.9 的基础上追加一个 `tenantId` 查询参数，其余完全一致：

| 序号 | 接口路径 | Feign 客户端 | 请求参数 | 响应 |
|---|---|---|---|---|
| 14 | `GET /api/org/user/position/getSuperiorByUserIdAndTenant` | `ApiOrgUserPosition#getSuperiorByUserIdAndTenant(Long userId, Long tenantId)` | `userId`、`tenantId` | 同 2.7；`tenantId` 或 `userId` 为空返回 `data = null` |
| 15 | `GET /api/org/department/listUsersByDeptIdRecursiveAndTenant` | `ApiOrgDepartment#listUsersByDeptIdRecursiveAndTenant(Long deptId, Long tenantId)` | `deptId`、`tenantId` | 同 2.8，按 `user_id` 去重 |
| 16 | `GET /api/org/user/position/listUsersByPositionIdAndTenant` | `ApiOrgUserPosition#listUsersByPositionIdAndTenant(Long positionId, Long tenantId)` | `positionId`、`tenantId` | 同 2.9，按 `user_id` 去重 |

#### 业务规则

- `tenantId` 必填；为 `null` 时快速失败且不查库（单对象 `data = null`，列表空数组）；
- 除租户条件外，业务规则与 2.7~2.9 完全一致（优先主岗位取上级、递归部门集合、不过滤 `position_status`、
  按 `user_id` 去重）；
- 变体 14、16 采用两段查询（先取 `report_to` / 用户ID，再批量取用户），两段使用**同一个** `tenantId`，
  否则会解析出另一个租户的用户；
- 变体 15 的部门集合展开复用部门缓存 / `org_department` 表（**不分租户**），
  租户条件只落在 `org_user.tenant_id` 上——即使部门集合跨了租户，用户查询也会按租户收窄。

---

## 3. 数据字典

### 3.1 枚举定义

| 枚举名                     | 枚举值                                        | 说明           |
|-------------------------|--------------------------------------------|--------------|
| `OrgUserStateEnum`      | 0禁用 / 1启用 / 2锁定                           | AO 中以 `Integer` 返回 |
| `OrgUserGenderEnum`     | 0保密 / 1男 / 2女                              | AO 中以 `Integer` 返回 |
| `org_user_position.position_status` | 1在职 / 2试用期 / 3待入职 / 4待离职 / 5已卸任 | 任职状态 |

---

## 4. 实现与验证记录（2026-09-24）

| 能力 | Feign 方法 | 支撑的 Manager 方法 | 生成的查询条件（实测） |
|---|---|---|---|
| 1 | `ApiOrgUser#getUserById` | `OrgUserManager#getDtoById`（复用） | `selectById` |
| 2 | `ApiOrgUser#listUsersByIds` | `OrgUserManager#listDtoByIds`（新增） | 去重后 `selectByIds` 一次 |
| 3 | `ApiOrgUserPosition#listSubordinatesByUserId` | `OrgUserPositionManager#listByReportTo`（新增） | `(report_to = ?)` |
| 4 | `ApiOrgDepartment#listUsersByDeptId` | `OrgDepartmentManager#getUsersByDeptId`（复用） | `org_user_department.dept_id` 直连用户 |
| 5 | `ApiOrgRole#listUsersByRoleId` | `OrgRoleManager#listUserIdsByRoleId`（新增）+ `OrgUserManager#listDtoByIds` | `(role_id = ? AND (expire_time IS NULL OR expire_time > ?))` |

验证方式：standalone MockMvc（4 个 `*ApiImpl`） + Mockito 桩 Manager + Feign `SpringMvcContract` 真实请求编码捕获 + MyBatis-Plus wrapper SQL 片段断言，共 28 项断言全部通过。
未覆盖：真实数据库/Redis/Nacos 环境下的端到端联调（本模块默认跳过测试，未在本次改动中开启）。

---

## 5. 实现与验证记录（v1.1 · 2026-09-25 · 显式租户变体）

tenant 拦截器绕过使用 `IgnoreStrategy.builder().tenantLine(true).build()` +
`InterceptorIgnoreHelper.execute(...)`，随后在 wrapper 中显式 `eq(...::getTenantId, tenantId)`。

| 能力 | Feign 方法 | 支撑的 Manager 方法 | 生成的查询条件（实测） |
|---|---|---|---|
| 6 | `ApiOrgUser#getUserByIdAndTenant` | `OrgUserManager#getDtoByIdAndTenant`（新增） | `(id = ? AND tenant_id = ? LIMIT 1)`，绕过租户拦截器 |
| 7 | `ApiOrgUser#listUsersByIdsAndTenant` | `OrgUserManager#listDtoByIdsAndTenant`（新增） | 去重后 `(id IN (?,?) AND tenant_id = ?)`，绕过租户拦截器 |
| 8 | `ApiOrgUserPosition#listSubordinatesByUserIdAndTenant` | `OrgUserPositionManager#listByReportToAndTenant`（新增）+ `OrgUserManager#listDtoByIdsAndTenant` | `(report_to = ? AND tenant_id = ?)` → `(id IN (?) AND tenant_id = ?)` |
| 9 | `ApiOrgDepartment#listUsersByDeptIdAndTenant` | `OrgDepartmentManager#getUsersByDeptIdAndTenant`（新增） | `org_user_department.dept_id`（无租户列）→ `(id IN (?) AND tenant_id = ?)` |
| 10 | `ApiOrgRole#listUsersByRoleIdAndTenant` | `OrgRoleManager#listUserIdsByRoleIdAndTenant`（新增）+ `OrgUserManager#listDtoByIdsAndTenant` | `(role_id = ? AND tenant_id = ? AND (expire_time IS NULL OR expire_time > ?))` → `(id IN (?) AND tenant_id = ?)` |

`mvn -o -pl carlos-integration/carlos-org/carlos-org-api,carlos-integration/carlos-org/carlos-org-bus -Dmaven.test.skip=true install`
→ BUILD SUCCESS；`mvn -o -pl carlos-integration/carlos-org/carlos-org-bus test` → Tests run: 107, Failures: 0, Errors: 0。

新增/扩展的单元测试：

- `OrgUserManagerImplTest`（新增）：租户 wrapper 断言 + 空入参不查库；
- `OrgDepartmentManagerImplTest`（新增）：`org_user` wrapper 租户断言 + 无关联用户时不二次查询；
- `OrgUserPositionManagerImplTest`（新增）：`report_to` + `tenant_id` 断言；
- `OrgRoleManagerImplTest`（扩展）：`tenant_id` 断言，并回归确认既有方法**不**追加 `tenant_id`；
- `ApiOrgTenantLookupImplTest`（新增）：4 个 apiimpl 的 tenantId 透传与快速失败；
- `ApiOrgTenantEndpointPathTest`（新增）：standalone MockMvc 真实走一遍 5 条新路径的映射与参数绑定。

未覆盖：`AndTenant` 变体未与普通变体做「同租户结果一致」的对比集成测试；
`tenantId` 的合法性（租户是否存在/是否启用）由调用方保证，接口不做校验；
无真实数据库/Redis/Nacos 环境下的端到端联调。

---

## 6. 实现与验证记录（v1.2 · 2026-09-25 · 直属上级 / 递归部门 / 岗位用户）

| 能力 | Feign 方法 | 支撑的 Manager 方法 | 生成的查询条件（按实现断言） |
|---|---|---|---|
| 11 | `ApiOrgUserPosition#getSuperiorByUserId` | `OrgUserPositionManager#getReportToByUserId`（新增）+ `OrgUserManager#getDtoById` | `(user_id = ? AND report_to IS NOT NULL)` → 按 id 取用户 |
| 12 | `ApiOrgDepartment#listUsersByDeptIdRecursive` | `OrgDepartmentManager#getUsersByDeptIdRecursive`（新增，内部展开部门集合） | 部门集合展开（缓存 / 逐层下探）→ `(dept_id IN (自身, 子孙...))` **一次**查询 → `(id IN (...))` |
| 13 | `ApiOrgUserPosition#listUsersByPositionId` | `OrgUserPositionManager#listUserIdsByPositionId`（新增）+ `OrgUserManager#listDtoByIds` | `(position_id = ?)`（**不带** `position_status`）→ `(id IN (...))` |
| 14 | `ApiOrgUserPosition#getSuperiorByUserIdAndTenant` | `OrgUserPositionManager#getReportToByUserIdAndTenant`（新增）+ `OrgUserManager#getDtoByIdAndTenant` | `(user_id = ? AND report_to IS NOT NULL AND tenant_id = ?)`，绕过租户拦截器 |
| 15 | `ApiOrgDepartment#listUsersByDeptIdRecursiveAndTenant` | `OrgDepartmentManager#getUsersByDeptIdRecursiveAndTenant`（新增） | 部门集合同 12 → `(id IN (...) AND tenant_id = ?)`，绕过租户拦截器 |
| 16 | `ApiOrgUserPosition#listUsersByPositionIdAndTenant` | `OrgUserPositionManager#listUserIdsByPositionIdAndTenant`（新增）+ `OrgUserManager#listDtoByIdsAndTenant` | `(position_id = ? AND tenant_id = ?)` → `(id IN (...) AND tenant_id = ?)` |

`mvn -o -pl carlos-integration/carlos-org/carlos-org-api,carlos-integration/carlos-org/carlos-org-bus -Dmaven.test.skip=true install`
→ BUILD SUCCESS；`mvn -o -pl carlos-integration/carlos-org/carlos-org-bus test` → Tests run: 138, Failures: 0, Errors: 0。

新增/扩展的单元测试：

- `ApiOrgLookupImplTest`（新增）：11~16 六个端点的路径绑定（standalone MockMvc）与普通变体的委托、空入参不查库；
- `ApiOrgTenantLookupImplTest`（扩展）：新增 14/15/16 的 `tenantId` 透传与快速失败；
- `OrgUserPositionManagerImplTest`（扩展）：`getReportToByUserId` 的主岗位优先与回退、`position_status` 不过滤断言；
- `OrgDepartmentManagerImplTest`（扩展）：递归查询**一次**归属查询覆盖自身与子孙部门、子树无成员时不查 `org_user`、
  带租户时显式 `tenant_id`。

另修正 `ApiOrgUserPosition` 的 javadoc：原文声称 `listSubordinatesByUserId`「支撑『审批人 = 直属上级』」是**错的**
（该端点返回**下级**），已改为说明方向并指向 `getSuperiorByUserId`。

未覆盖：无真实数据库 / Redis / Nacos 环境下的端到端联调，全部为 mock 与路径级的「契约验证」；
递归部门的部门集合展开在单测中走的是**缓存未命中的逐层下探分支**（单测无 Redis），缓存命中分支未做单测。

---

## 7. 变更记录

| 版本   | 日期         | 变更内容                                        | 作者          |
|------|------------|---------------------------------------------|-------------|
| v1.0 | 2026-09-24 | 初始版本：新增 5 个远程查询接口（用户按ID/批量、直属下级、部门用户、角色用户） | Carlos Team |
| v1.1 | 2026-09-25 | 新增 5 个显式租户变体 `...AndTenant`（供应无请求上下文的后台/系统间调用使用）；补充 1.4「何时使用 AndTenant 变体」 | Carlos Team |
| v1.2 | 2026-09-25 | 新增 3 个查询（直属上级 / 递归部门用户 / 岗位用户）及其 `...AndTenant` 变体；修正 `listSubordinatesByUserId` 的误导性 javadoc（它查的是**下级**，不是上级） | Carlos Team |
