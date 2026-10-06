# Carlos Framework S1–S7 安全项复核报告

> **报告类型**: 内部治理计划「安全批」S1–S7 的逐条复核（只读，未改动任何代码）
> **复核日期**: 2026-09-19
> **代码版本**: `ff50ae0b1ffce5ebf2d2eac9ba99c03c202fd69c`（分支 `feat/system-manage-debug`）
> **复核范围**: 计划中编号 S1–S7 的七项，外加复核过程中发现的同源问题
> **复核方式**: 逐条读源码 + 生产库 `information_schema` 复核 + 少量无 token 的 GET 探测
> **改动**: **无**。本报告不修改任何代码；探测只用了 GET，未调用任何写接口
> **报告版本**: v1.0

---

## 1. 执行摘要

内部治理计划把后端安全项列为 S1–S7，并注明「均已逐行确认」。本次逐条复核后，**七项中有两项需要修正结论**，其余五项属实但**严重度排序与计划不同**：

| # | 计划描述的问题 | 复核结论 | 复核后严重度 |
|---|---|---|---|
| S6 | 管理面零 `@PreAuthorize` | ✅ 属实，**后果比计划描述的具体得多** | **1（最该先修）** |
| S7 | `AuditLogDemoController` 是活的 `@RestController` | ✅ 属实，但可达性需修正、**分量应高于计划** | **2** |
| S3 | `OrgUserUpdateParam` 批量赋值 | ✅ 属实，描述准确 | 3 |
| S1 | 硬编码初始令牌守着 `POST /org/init/superAdmin` | ✅ 属实，但**漏了两个守卫**（决定哪些部署不受影响） | 4 |
| S4 | 审计表开放 `add`/`update`/`delete` | ✅ 属实，但**当前影响最小**（改的是空表） | 5 |
| S5 | `initWithTxt` 任意文件读取，**两处** | ⚠️ **只有一处**，另一半已过时 | 6 |
| S2 | 硬编码兜底密钥 `QRCODE_DEFAULT_SECRET` | ❌ **不成立** —— 它是反向哨兵，不是兜底密钥 | 不改 |

### 最关键的三点

1. **S6 不是抽象问题，它有一条「一个请求完成账号接管」的具体路径**：`POST /org/user/resetPassword {id, newPwd}` 只校验目标用户存在与密码复杂度，**既没有「只能改自己」也没有权限校验**（`OrgUserController.java:130` → `OrgUserService.java:254`）。任何已登录用户可以把**任意账号、含超级管理员**的密码改成自己知道的值，然后登录为那个人。同类的还有 `POST /org/user/assignRoles` 与 `POST /org/role/assignPermissions`。

2. **S6 有一个计划未记录的放大器：服务端口自身不做认证**。实测 `http://127.0.0.1:9512/org/user/roles?userId=…` **不带任何 token 返回 200**，且 `netstat` 显示这些端口绑定在 `0.0.0.0`。网关的认证只保护**经过网关**的请求；能连到服务端口的主机可以完全绕开网关（是否真能连取决于主机防火墙与网络隔离，无法从代码确认）。

3. **S7 的真实分量是「可伪造审计记录」，而不是「有个示例 controller」**。它的每个方法都带 `@Log(...)`，而 `@Log` 会把审计记录写进 ClickHouse（`AuditProperties.enabled` 默认 `true`），参数内容由调用者控制。因此任何已登录用户都能向审计系统注入伪造记录 —— 这比 S4（改 Postgres 里的空表）更实际，两者在计划里的分量应对调。

---

## 2. 复核方法

- **读源码**：每条结论都给出 `文件:行`，见文末证据索引。
- **生产库复核**：用 `information_schema` 核对表结构、默认值、唯一索引，避免只信 DDL 脚本（历史上脚本与线上库存在偏差）。
- **可达性探测**：只发 GET、不带 token，用「401 还是 404」区分「路由存在但被认证挡住」与「路由不存在」。**未调用任何写接口**。
- **不做的事**：不改代码、不跑真实利用（例如没有真的去重置任何账号密码、没有真的创建超级管理员）。

---

## 3. 逐条复核

### 3.1 S1 — 硬编码初始令牌

**结论：属实，但计划漏掉了两个守卫；正因为有它们，不少部署其实不受影响。**

证据：

| 事实 | 位置 |
|---|---|
| 令牌默认值是一个写死的字面量 | `carlos-org-bus/.../config/OrgInitProperties.java:25`（本报告不重复其值） |
| `enabled` 字段初始化为 `true` | 同上 `:19` |
| 端点 | `OrgInitController.java:53` `POST /org/init/superAdmin` |
| 守卫①开关 | `OrgInitController.java:60-62` |
| 守卫②请求头 Token 常量时间比较 | `:63-68` + `constantTimeEquals`（`MessageDigest.isEqual`，确实防时序） |
| 守卫③空库才允许 | `OrgInitService.java:124` `if (userManager.count() > 0) throw …`（在 Redisson 锁内二次校验） |

⚠️ **注释与代码互相矛盾**（这本身就是应当修的问题）：

- `enabled` 的 javadoc 写「**默认关闭**。初始化完成后应置为 false」，字段初始化却是 `true`。
- `token` 的 javadoc 写「**必须配置后接口才可用**」，但它有默认值，不配置也照样可用。

**实际影响**：不是「知道字面量就能建超管」，而是「**空库** + 开关默认开 + token 未被覆盖」三条同时成立时，知道字面量且能到达该端点的人可以**抢注超级管理员**（密码自选）。本机配置未覆盖 `carlos.org.init.*`（org-boot / org-cloud 的 yml 里都没有这一段），所以生效值就是 `enabled=true` + 该字面量。本库已有用户 ⇒ 当前不可利用；**全新部署的空库条件天然成立** ⇒ 可利用，前提是端点可达（见 3.8 的端口问题）。

**建议方向**：去掉 `token` 的默认值并在启动时校验（配置注释自己就是这么写的），或让 `enabled` 真的默认 `false` 以与 javadoc 一致。
⚠️ 动这条需要连带确认部署侧会显式配置 —— 否则**全新部署将完全无法初始化**（那时还没有任何账号可以登录）。

### 3.2 S2 — 「硬编码兜底密钥」 —— 不成立

**结论：不是缺陷。** `QRCODE_DEFAULT_SECRET` 的唯一用途是**反向拦截**。

```java
// LoginMethodValidator.java:120-123
if (QRCODE_DEFAULT_SECRET.equals(signSecret)) {
    log.warn("扫码登录开关已启用，但 …sign-secret 仍为默认值，请配置正式密钥");
    return false;
}
```

同方法内，`signSecret` 为空、或含未解析占位符（`${`）时同样 `return false`。即它是**配置健康检查**的一部分：值仍是默认值就拒绝启用扫码登录并告警。全仓只有这一处使用（另一处是 `:32` 的声明），没有任何路径把它当成实际签名密钥。

**建议方向**：不改。若追求可读性，可把常量改名为 `QRCODE_PLACEHOLDER_SECRET` 之类，去掉「DEFAULT」的兜底歧义。

### 3.3 S3 — `OrgUserUpdateParam` 批量赋值

**结论：属实，计划描述准确。**

证据链：`OrgUserController.java:80` `@RequestBody @Validated OrgUserUpdateParam` → `OrgUserConvert.toDTO(OrgUserUpdateParam)`（`OrgUserConvert.java:55`，**纯 MapStruct 同名映射，无 `@Mapping` 限制**）→ `OrgUserService.updateOrgUser:164-192`。

`OrgUserUpdateParam` 的 18 个字段里**只有 `id` 有 `@NotNull`**。`updateOrgUser` 只校验三件事（用户存在、`account` 唯一、`phone` 唯一），随后 `userManager.modify(dto)`，而 MyBatis-Plus 的 `updateById` 跳过 `null`、写入非 `null` ⇒ **调用方带上哪个字段就改哪个**：

| 字段 | 后果 |
|---|---|
| `tenantId` | 跨租户改写（租户越权） |
| `userStatus` | 启停任意账号 |
| `loginCount` / `loginTime` / `loginIp` | **伪造登录审计字段** |
| `pwdLastModify` | 重置改密时钟（绕过密码过期策略） |
| `pwd` | 直接写库（需自行构造与存储格式一致的哈希，如 `{argon2}…`） |

**建议方向**：另立一个只含允许修改字段的 UpdateParam，或在 `updateOrgUser` 内显式白名单拷贝。**不能只靠前端收敛** —— 前端本就不该是安全边界。

### 3.4 S4 — 审计表开放写接口

**结论：属实，但当前影响最小。**

`AuditLogMainController` `@RequestMapping("/audit/log/main")`，`:42 add`、`:50 delete`、`:57 update`。该控制器走 MyBatis-Plus → Postgres `audit_log_main`，而**该表当前 0 行**；真实审计数据在 ClickHouse。经网关 `/audit/**` 只需认证即可访问。

所以「审计只应追加、不应可改写」这条原则成立，但今天改它动不到真实数据。**严重度低于 S7**（后者能写进 ClickHouse）。

**建议方向**：移除这三个写端点（或至少移除 `delete`/`update`）。

### 3.5 S5 — `initWithTxt` 任意文件读取

**结论：只有一处；计划里的另一半已过时。**

- ❌ **计划称 `SysRegionController.java:99-105` 也有** —— 实测**不存在** `initWithTxt`。该控制器只走 `MultipartFile`（`SysRegionController.java:119 importData`、`:138 importDataByParam`，配 `FileNameUtil.getName`），**没有任何基于路径的读取**。
- ✅ `SysDictController.java:97-101` 确实存在：

```java
@GetMapping("initWithTxt")
@Operation(summary = "使用txt文本初始化")
public void initWithTxt(String path) { dictService.initWithTxt(path); }
```

→ `SysDictService.java:170` → `FileUtil.readUtf8Lines(path)`。

**实际影响比「任意文件读取」更具体**：读到的内容会被**解析成字典并写库**（`addDict`），而字典本身有查询端点 ⇒ 文件内容可以**通过 `/sys/dict/page` 读出来**。即「任意文件读取 → 经字典表外带 → 同时污染字典数据」。它还是 **GET**，可被日志、缓存、爬虫触发。

经网关 `/sys/**` 需要认证，但结合 S6（认证即全权）⇒ **任何已登录用户可以读取服务器上的任意文件**。

**建议方向**：删除该端点（它是当年的一次性初始化脚本），或改为只允许读 classpath 资源并改用 POST。

### 3.6 S6 — 管理面零授权

**结论：属实，且是本次复核中后果最严重的一条。**

量化（`grep -c "@\(Get\|Post\|Put\|Delete\|Patch\)Mapping"`）：

| 模块 | controller | 端点 |
|---|---|---|
| carlos-org | 9 | 61 |
| carlos-system | 2 | 11 |
| carlos-audit | 6 | 28 |
| carlos-message | 10 | 50 |
| **合计** | **27** | **150** |

在这四个模块内，`@PreAuthorize` / `hasAuthority` / `SecurityFilterChain` / `@EnableMethodSecurity` **grep 零命中**。网关上 `authorization-enabled: false`，且配置自带说明为何不能直接打开：

> RBAC 路径级鉴权依赖 `auth:permissions:{userId}`（METHOD:path 格式）权限数据；当前权限数据按角色存储（`auth:permissions:{roleId}`，权限码为 `resource:action` 格式），且 `org_role_operation` 无数据，**开启会拦截所有业务请求**。

网关认证白名单仅 10 条，**不含任何管理面路径**：`/public/**`、`/static/**`、`/auth/public-key`、`/auth/user/login|pre-check|logout|refreshToken`、`/auth/captcha/**`、`/auth/mfa/step-up-verify`、`/sys/i18n/messages`。

#### 具体后果：一个请求完成账号接管

`POST /org/user/resetPassword {id, newPwd}`（`OrgUserController.java:130` → `OrgUserService.resetPassword:254`）只校验：①目标用户存在 ②新密码复杂度。**没有「只能改自己」，也没有任何权限校验。**

> 任何已登录用户可以用一个请求，把**任意账号（含超级管理员）**的密码改成自己知道的值，然后登录为那个人。

同类零校验端点：`POST /org/user/assignRoles`（`OrgUserController.java:180`）、`POST /org/role/assignPermissions`（`OrgRoleController.java:135`）。

**建议方向（重要）**：**不能只加注解**。网关配置注释已警告，直接打开 `authorization-enabled` 会拦掉所有业务请求。可行的分步做法是：先给最危险的几个**单体端点**加显式越权校验（`resetPassword` / `assignRoles` / `assignPermissions` 至少要有「本人或管理员」判断），再谈全面 RBAC。

### 3.7 S7 — 审计 demo 控制器

**结论：属实，可达性需要修正，且严重度应高于计划给它的分量。**

`AuditLogDemoController` 是活的 `@RestController`，`@RequestMapping("/api/audit/demo")`：`/login`（`:44` 返回硬编码 `"mock-jwt-token"`）、`/order/pay`、`/export`、`/config/update`、`/file/upload`。

**可达性（计划未说清的部分）**：

- 经网关 `/audit/**` **不可达** —— 映射是 `/api/audit/demo`，不以 `/audit` 开头。计划只写「活的 controller」，容易被读成公网可达。
- 但网关的**服务发现 locator 是开着的**（`spring.cloud.gateway.server.webflux.discovery.locator.enabled: true` + `lower-case-service-id: true`）⇒ 经 **`/carlos-audit/api/audit/demo/**` 可达**。实测无 token 返回 **401**（= 路由存在、只被认证挡住），而非 404。
- **不是认证绕过**：那个假 token 不是 IdP 签发的，网关内省不会认它。

**真正的分量**：它的每个方法都带 `@Log(...)`，而 `@Log` 会把审计记录写进 ClickHouse（`AuditProperties.enabled` 默认 `true`）。因此**任何已登录用户可以伪造审计记录** —— `logType` / `operation` / `riskLevel` 由注解固定，而参数内容（`orderNo`、金额、`username` 等）由调用者控制。这对审计系统是**完整性缺口**。

顺带印证：`AuditProperties` 有 `localBackupOnFailure = true` + `localBackupPath = ./audit-log-backup`，这解释了仓库根目录未跟踪的 `audit-log-backup/` 目录（本机 ClickHouse 未启动时，审计写入走本地备份）。

**建议方向**：删除该控制器；若要保留 `@Log` 用法示例，迁到 `carlos-samples` 或加 profile 隔离。

### 3.8 与 S 条目同源、但计划未列的问题

1. **服务端口自身不做认证**：实测 `http://127.0.0.1:9512/org/user/roles?userId=…` 无 token 返回 200；`netstat` 显示服务端口绑定在 `0.0.0.0`。网关认证只保护经过网关的请求。
2. **discovery locator 让每个服务的每个端点额外暴露在 `/{服务名}/**` 下** ⇒「某服务只暴露在 `/audit/**`」这类路径假设不成立。
3. **`LicenseCreatorController` 也是活的 `@RestController`**（`carlos-spring-boot-starter-license-generate`），而项目 CLAUDE.md 明确写「**切勿在生产制品中包含 license-generate 模块**」。需要确认它是否被打进了任何部署。
4. `/org/user/resetPassword` 的越权已并入 S6，但它是本次唯一「一个请求完成账号接管」的路径，值得单列跟踪。
5. `carlos-tools` 的 `CodeGeneratorController` / `ProjectGeneratorController` 属于 Swing 桌面工具，不是服务端点，可忽略。

---

## 4. 无法静态确认的部分

以下需要部署信息或环境访问，代码层面无法定论：

1. **生产 / 测试部署是否覆盖了 `carlos.org.init.token`** —— 直接决定 S1 的真实风险。
2. **服务端口是否被防火墙隔离、是否只在内网** —— 决定 3.8 第 1、2 条的真实暴露面。本报告只能证明「绑 `0.0.0.0`」+「本机 127.0.0.1 无认证可达」。
3. **`org_role_operation` 是否真的无数据** —— 决定 `authorization-enabled` 能否打开。
4. 本机 ClickHouse 未启动（8123/9000 无监听），因此 S7 的审计伪造在**此处**只落本地备份；真实部署会写进 ClickHouse。

---

## 5. 建议处理顺序（供决策，尚未实施）

| 顺序 | 项 | 理由 |
|---|---|---|
| 1 | **S6 的越权三连**（`resetPassword` / `assignRoles` / `assignPermissions`） | 收益最大、改动最小、可单独回滚；先加调用者身份校验，不动全局 RBAC |
| 2 | **S7 删除 demo 控制器** | 一处删除即消除「伪造审计记录」通道 |
| 3 | **S3 收敛 UpdateParam** | 消除租户越权与审计字段伪造 |
| 4 | **S1** 去掉 token 默认值 + `enabled` 与 javadoc 对齐 | 连带确认部署侧会显式配置，否则全新部署无法初始化 |
| 5 | **S5** 去掉 `initWithTxt` | 一次性脚本，直接删 |
| 6 | **S4** 移除审计表写端点 | 当前影响小，但原则问题 |
| 7 | **S2** 不改 | 不成立 |
| 单列 | **服务端口认证 + discovery locator** | 不属于 S1–S7 任何一条，却是多条问题的公共放大器 |

---

## 6. 附录：复核证据索引

| 主题 | 位置 |
|---|---|
| S1 令牌默认值 / `enabled` | `carlos-org/carlos-org-bus/.../config/OrgInitProperties.java:19,25` |
| S1 端点与守卫 | `.../controller/OrgInitController.java:53,60-68,72`；`.../service/OrgInitService.java:97,124` |
| S2 哨兵 | `carlos-auth/carlos-auth-service/.../oauth2/web/LoginMethodValidator.java:32,110-123` |
| S3 参数 / 校验 | `carlos-org/carlos-org-bus/.../pojo/param/OrgUserUpdateParam.java:23-59`；`.../controller/OrgUserController.java:80`；`.../convert/OrgUserConvert.java:55`；`.../service/OrgUserService.java:164-192` |
| S4 写端点 | `carlos-audit/carlos-audit-bus/.../controller/AuditLogMainController.java:30,42,50,57` |
| S5 字典端点 | `carlos-system/carlos-system-bus/.../dict/controller/SysDictController.java:97-101`；`.../dict/service/SysDictService.java:170` |
| S5 区域控制器（无该端点） | `carlos-system/carlos-system-bus/.../region/controller/SysRegionController.java:119,138` |
| S6 网关配置 | `carlos-gateway/src/main/resources/application.yml:99-101`（locator）、`:218-221`（`authorization-enabled: false`）、`:226-235`（白名单） |
| S6 越权端点 | `carlos-org/carlos-org-bus/.../controller/OrgUserController.java:130,180`；`.../controller/OrgRoleController.java:135`；`.../service/OrgUserService.java:254-275` |
| S7 demo 控制器 | `carlos-audit/carlos-audit-bus/.../controller/AuditLogDemoController.java:23-24,38,44,62,83,104,123` |
| 审计配置 | `carlos-spring-boot/carlos-spring-boot-starter-log/.../config/AuditProperties.java`（`enabled=true`、`localBackupOnFailure`、`localBackupPath`） |
| license-generate 控制器 | `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/.../LicenseCreatorController.java` |

---

**复核人**: Claude Code（只读复核）
**说明**: 本报告不代表任何修复已实施。所有「建议方向」均未落地，需要单独确认后再动手。
