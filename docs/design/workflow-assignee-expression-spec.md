# 工作流办理人表达式规范（v1 草案）

- **日期**：2026-09-25
- **状态**：**已定稿**；§3 静态词汇与 §4 动态词汇已在 `carlos-workflow-bus` 实现（2026-09-25，三个此前不可用的词汇已补齐）。
  词汇表由 `WorkflowAssigneeVocabulary` 唯一定义（展开侧与校验侧共用）；**违反本规范的表达式不再静默失效**——
  保存期 `POST /api/workflow/definition` 会以 `10705` 拒绝（仅覆盖该入口，设计器直连的 `/warm-flow/save-json` 绕过它），
  运行期由 `WorkflowAssigneeFailFastListener` 在创建待办前以 `50702` 拦下。
- **适用**：`carlos-workflow` 的节点办理人配置（设计器 + 引擎 `flow_node.permission_flag`）

---

## 一、为什么需要这份规范

Warm-Flow **不提供任何内置的办理人词汇**——`permission_flag` 里写什么完全由使用方自定义。不定规范，下游每个业务系统都会发明自己的写法，`OrgPermissionHandler` 就得兼容所有变体，最终谁也用不明白。

---

## 二、语法载体（引擎机制决定的硬约束）

`flow_node.permission_flag` 是 **`@@` 分隔的表达式列表**，逐个求值。引擎按**前缀**分派策略：

| 前缀 | 策略 | 谁实现 |
|---|---|---|
| `#` | SpEL（沙箱：类型/方法白名单） | Warm-Flow 自带 |
| `$` | 流程变量取值 | Warm-Flow 自带 |
| 无前缀 | **字面量，原样返回** | Warm-Flow 自带 |

所以**本规范定义的词汇一律用无前缀的字面量**（形如 `role:5`），不与引擎的 `#`/`$` 冲突，且能被引擎正确透传给我们的解析器。

多个表达式用 `@@` 连接，语义是**并集**：

```
user:1001@@role:5@@deptLeader:12
```

---

## 三、静态词汇（在 `convertPermissions` 内展开）

这些**不需要上下文**（只依赖 org 服务数据），因此可以在 Warm-Flow 的
`PermissionHandler#convertPermissions(List<String>)` 里直接展开为 userId 列表。

语法：`类型:参数[:附加参数]`，类型名小写。

| 写法 | 含义 | 展开结果 | 实现状态（2026-09-25） |
|---|---|---|---|
| `user:1001` | 指定用户 | `[1001]` | 已实现 |
| `user:1001,1002` | 多个指定用户（逗号分隔） | `[1001,1002]` | 已实现 |
| `role:5` | 角色 5 下的**全部启用用户** | 展开 | 已实现 |
| `dept:12` | 部门 12 下的用户（**不含**子部门） | 展开 | 已实现 |
| `dept:12:rec` | 部门 12 **及其所有子部门**下的用户 | 展开 | **本次实现** |
| `deptLeader:12` | 部门 12 的**负责人**（`org_department.leader_id`） | `[leaderId]` | 已实现 |
| `deptLeader:12:2` | 部门 12 **向上 2 级**的部门负责人（超时升级常用） | 逐级解析 | 已实现 |
| `position:7` | 岗位 7 下的全部用户 | 展开 | **本次实现** |
| `roleInDept:5:12` | 部门 12 内持有角色 5 的人（"部门内某角色"极常见） | 展开 | 已实现 |

**词汇表实现**：类型名、参数形状与合法性判定只在 `WorkflowAssigneeVocabulary` 里定义一次——
运行期展开（`OrgPermissionHandler#convertPermissions`）与保存期校验（`WorkflowAssigneeValidator`）
共用同一份，避免两处类型表漂移（新增一个类型只改了展开侧，保存侧就会把它判成非法）。
表达式的三种形态：`KNOWN`（类型在表内，参数是否合法由 `malformed` 给出）、
`UNKNOWN_TYPE`（有 `类型:参数` 形状但类型不在表内，如 `manager:5`）、`LITERAL`（无冒号，
即引擎无前缀策略的透传：`#`/`$` 求值后的用户 id 或裸用户 id）。

**空值语义**：展开结果为空时该表达式视为无人。**注意 2026-09-25 起这条不再是「静默」的**：
节点 `permission_flag` 非空（即配置过办理人）而最终解析不到任何人时，
`WorkflowAssigneeFailFastListener` 会在创建待办任务之前直接抛异常（`50702`，消息里带节点编码/名称与出错的表达式），
而不是让 `flow_user.processed_by` 空着——因为引擎的 `checkAuth` 对「办理人为空」的任务整段短路，
该节点会变成**无人鉴权**（对所有人开放）且不出现在任何人的待办里。
只有**空的 `permission_flag`**（引擎语义上的「本节点不做鉴权」）仍然放行。
`carlos.workflow.assignee.fail-fast=false` 可降级为仅 WARN 日志，代价是回到上述无人鉴权状态，仅限过渡期使用。

**去重**：多个表达式并集后按 `userId` 去重。

**实现说明（2026-09-25）**

| 写法 | 展开方式 | 依赖的 org 端点 |
|---|---|---|
| `dept:12:rec` | 部门集合（自身 + 全部子孙）由 **org 侧展开**，调用方只发**一次**远程调用；不要为每个子部门各调一次 `listUsersByDeptId` | `ApiOrgDepartment#listUsersByDeptIdRecursive` |
| `position:7` | 按 `org_user_position.position_id` 取 `user_id` 去重后批量取用户 | `ApiOrgUserPosition#listUsersByPositionId` |

两者的任职状态口径与既有的 `listSubordinatesByUserId` **一致**：org 侧**不过滤** `position_status`
（已卸任的记录若未清理仍会返回），过滤只发生在办理人处理器里——只剔除明确禁用的用户账号（`user_status = 0`）。
理由是 `position_status = 2`（试用期）属于正常在职状态，按「仅在职」过滤会漏人。

> ⚠️ 与 `position:{id}` 并列的一个易混口径：`OrgUserPositionManager#countOccupiedByPositionId`
> （岗位编制占用人数）**只数在职**，与 `position:{id}` 的展开结果本就不等，两者不可互相校验。

**保存期校验（尽力而为，不是硬闸门）**

`POST /api/workflow/definition` 在保存前会把每个节点的 `permissionFlag` 过一遍词汇表：
未知类型（`manager:5`）、参数非法（`role:abc`、`dept:12:xxx`、`deptLeader:12:21`）、
以及裸的非数字字面量（`zhangsan`）一律以 **10705** 拒绝，错误消息列出全部出错节点与表达式；
`#`/`$` 开头的动态表达式与裸数字用户 id 视为合法（前者保存期无从判断，只能运行期兜）。

> ⚠️ **不要把它当成定义入库的唯一防线**：设计器 SPA 直接调引擎自带的 `POST /warm-flow/save-json`，
> 不经过本模块，定义可以绕过这一步到达引擎；DB 直改同样绕不过去。
> 真正保证「配了办理人却解析不到人」不会静默变成无人鉴权节点的是运行期的 fail-fast 监听器（见上文空值语义）。

---

## 四、动态词汇（必须走 SpEL，不能用静态语法）

有一个**引擎机制上的硬限制**必须说清：

> `PermissionHandler#convertPermissions(List<String>)` 的签名**没有上下文参数**——
> 它拿不到"当前流程实例是谁发起的"。

因此**依赖发起人的语义无法用静态词汇表达**。这类只能走 SpEL（`#` 前缀）+ 流程变量：

| 需要 | 静态写法能否 | 正确做法 |
|---|---|---|
| 发起人本人 | ❌ | 引擎在实例上记了 `create_by`；用 SpEL 取，或发起时把发起人写入流程变量 |
| **申请人的直属上级** | ❌ | SpEL + 流程变量：`#{@workflowAssignees.upLeader(#applicantId)}` |
| 申请人上 N 级 | ❌ | `#{@workflowAssignees.upLeader(#applicantId, 2)}` |

**配套要求**：

1. 助手 Bean `workflowAssignees` **已实现**（`carlos-workflow-bus`，暴露 `upLeader(Long)` / `upLeader(Long,int)` / `subordinates(Long)`）。
2. **发起流程时必须把发起人写进流程变量**（如 `applicantId`），否则 SpEL 无从取值。这是 `POST /api/workflow/instance/start` 的约定。

> ## ⚠️ 本规范初稿的一处错误（已更正 2026-09-25）
>
> 初稿在上一句里写"`upLeader` 依赖 `carlos-org` 的 `listSubordinatesByUserId` 系列端点"——**这是错的**。
>
> 那个端点方向**相反**：它查 `report_to = userId`，返回的是**下属**，不是**上级**。
> 取"某人的直属上级"要读**他自己的** `org_user_position.report_to`，而 `carlos-org-api` **没有这个查询**。
> （顺带：`ApiOrgUserPosition` 自己的 javadoc 声称它"支撑『审批人 = 直属上级』"——**同样是误导**，已一并修正。）
>
> 实现方**刻意没有用部门负责人替代**——部门负责人 ≠ 汇报线上的上级，且申请人自己就是部门负责人时会**把审批指派给自己**。

> ## ✅ `upLeader` 已可用（2026-09-25 实现）
>
> 该端点已补齐：`ApiOrgUserPosition#getSuperiorByUserId(Long)`（另有显式租户变体
> `getSuperiorByUserIdAndTenant(Long, Long)`，供无请求上下文的后台任务使用）。
>
> - 取数：读该用户**自己**任职记录的 `report_to`；兼岗时**优先主岗位**（`is_main = true`）的记录，
>   主岗位未配置时回退为任意一条已配置的记录；没有任职记录或未配置时返回 `data = null`（**不是错误**）。
> - `upLeader(applicantId, n)` 由 `workflowAssignees` **逐层向上查** n 次实现。链断（某人没有上级）、
>   上级账号被禁用、或汇报线成环时返回**空列表**——不放宽成"随便找个人"，让节点解析不到人比指派错的人安全。
> - org 侧能力复用 `OrgUserPositionManager`（先按 `user_id` 取 `report_to`，再批量取用户），
>   不复用 `listByUserId(userId)` 的全量加载。
>
> ⚠️ 仍需注意：静态词汇表里**没有也不会有** `upLeader` 字面量（原因见本节开头的引擎硬限制），
> 只能用 SpEL 写法。

> ⚠️ 因此"申请人上级"这类需求**不是设计器里点一下就能配好的**，需要在发起时传变量。
> 这是刻意的取舍：把上下文依赖显式化，而不是发明一个 `upLeader` 字面量然后在 `convertPermissions` 里无上下文地猜。

---

## 五、为什么这套覆盖约 90% 中式审批场景

上面的静态词汇覆盖了最常见的几类：**指定人 / 角色 / 部门（含下级）/ 部门负责人（含上级 N 级）/ 岗位 / 部门内某角色**。
动态部分（发起人及其上级）用 SpEL + 变量覆盖。

**不在本规范内、属于节点属性而非办理人**：

- 会签 / 或签 / 票签 —— 由 `flow_node.node_ratio` 与节点类型表达，不写进 `permission_flag`
- 超时升级 —— 由超时任务计算目标人，不在办理人表达式里

**扩展方式**：未来新增类型只需在本模块的 `WorkflowAssigneeVocabulary`（类型表唯一实现，
展开侧 `OrgPermissionHandler` 与保存期校验 `WorkflowAssigneeValidator` 共用）里加一项，
并同步设计器的选择器（`HandlerSelectService`）。词汇本身不做版本化，按需增量。

---

## 六、设计器侧配套（后续工作，不在本规范实现范围）

要让设计器里"点选"能产出上述字面量，需要实现 Warm-Flow 的设计器 SPI：

- `HandlerSelectService` —— 办理人选择弹窗，返回树（部门/角色/岗位/用户），`storageId` 即上文写法（如 `role:5`）
- `handlerFeedback` —— `storageId` → 显示名的回显（否则设计器里显示的是一串 `role:5`）
- `HandlerDictService` —— 预设表达式模板

**这三项不做，规范里的写法就得手填**，可用但体验差。建议列入 `carlos-workflow` 的后续计划。

---

## 七、待办

- [x] `OrgPermissionHandler`（`carlos-workflow-bus`）已按本规范实现，§3 静态词汇**全部支持**、§4 动态词汇由 `@workflowAssignees` 提供（2026-09-25）
- [x] **配了办理人却解析不到人不再静默失效**（2026-09-25）：`WorkflowAssigneeFailFastListener`
  在创建待办任务前拦截并抛 `50702`（`carlos.workflow.assignee.fail-fast` 默认 true，可降级为仅 WARN）；
  `WorkflowAssigneeVocabulary` 成为词汇表唯一实现，展开与校验不再各持一份类型表
- [ ] 在 `POST /api/workflow/instance/start` 的契约里明确"发起人必须写入流程变量"
- [ ] 实现设计器三个 SPI（见第六节）
- [ ] 把本文并入落地方案的「办理人解析」一节（当前该节待 `carlos-workflow` 重构完成后统一更新）
