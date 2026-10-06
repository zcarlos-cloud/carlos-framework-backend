# Carlos 人工审批工作流 · 落地方案（基于 Warm-Flow 1.8.9）

- **日期**：2026-09-24
- **决策依据**：`docs/reports/workflow-engine-selection-research-2026-09-24.md`（已选定 Warm-Flow 1.8.9）
- **需求范围**：人工审批（OA 审批流）；**不做**子流程、**不做**系统间 Saga 编排
- **技术基线**：JDK 21 / Spring Boot 4.0.6 / MyBatis-Plus 3.5.16 / 达梦 DmJdbcDriver18 8.1.3.62
- **状态**：设计待评审；**编码前必须先完成 POC**（见第八节）

---

## 一、目标与非目标

### 1.1 目标

给框架加一个**开箱即用的人工审批能力**，让下游业务系统只需：

1. 加一个 workflow 依赖（服务间调用用 `carlos-workflow-api`）；
2. 建 8 张表（**只在审批中心一处建**）；
3. 在页面上用设计器画流程、配办理人；
4. 调用审批中心的待办/审批 API。

> **⚠️ 2026-09-25 改判**：本节与下节的「加 starter 依赖 + 每个应用各建一次表」口径已被 §2.1 的中心化决策取代。
> 下方括号内的兜底说明仅为保留原文，实施请以 §2.1 为准。

即可拥有会签、加签、驳回、自由跳转、转办、委托、超时审批、审批留痕、待办通知。

### 1.2 非目标（明确不做）

| 不做 | 原因 |
|---|---|
| 子流程 | 需求已确认不涉及；Warm-Flow 亦未支持 |
| BPMN 2.0 标准互通 | 选定 JSON 模型即已锁定，属已知取舍 |
| 系统间 Saga 编排 | 与人工审批是两个问题域；框架已有 Seata AT 管道 |
| 自研流程引擎 | 会签/驳回/自由流转/版本管理/设计器等于 6–12 人月，无收益 |
| 中心化审批服务 | ~~不做~~ **已改判（2026-09-25）：中心化即为选定方案**，见 2.1；原「不做」的理由随嵌入式方案一并作废 |

---

## 二、架构决策

### 2.1 决策一：中心化审批服务（2026-09-25 改判；原「嵌入式 Starter」结论作废）

> **⚠️ 本节的早期结论「嵌入式 Starter」已被推翻，请勿据此实施。**
> 早期推理（嵌入式能保证审批与业务变更处于同一数据库事务）在**正确性**上成立，
> 但被两个当时没有计入的代价否决：
> ① 引擎的固定短 Bean 名（§4.3）与依赖排除治理（§4.2）会给**每一个**宿主应用各制造一次启动期故障面；
> ② 待办无法汇总——每个应用各持一份 `flow_task`，做不出跨业务的统一待办中心。
> 相关代码已从 `carlos-spring-boot/carlos-spring-boot-starter-workflow` 迁出，落到
> `carlos-integration/carlos-workflow`（api / bus / boot / cloud 四件套），服务端口 **9518**。

**选定：中心化审批服务。** 新增 `carlos-workflow` 服务，引擎与 8 张 `flow_*` 表集中在该服务自己的 JVM 与数据库内；业务应用通过 Feign 或前端直连调用 `/api/workflow/**`。

改判后的对比口径：

| | 中心化审批服务（**选定**） | 嵌入式 Starter（原方案，已作废） |
|---|---|---|
| 部署 | 一个审批服务 + 一套 `flow_*` 表 | 每个业务应用各带引擎与 7 张表 |
| **待办中心** | **天然集中：跨业务统一待办/已办，做得到** | 每个应用各持一份 `flow_task`，**汇总不了** |
| 引擎风险面 | **收口在一处**：依赖排除（§4.2）、Bean 名冲突（§4.3）、DDL 与升级脚本只维护一份 | 每接入一个应用就复制一遍，撞一次算一次 |
| 审批与业务事务 | 跨服务，**需要回调契约（未定，见下）** | 同一数据库事务，业务逻辑在节点回调中直接执行 |
| 流程定义归属 | 集中管理，需区分租户/应用 | 各业务应用自己的流程 |
| 与 Warm-Flow 设计意图 | 需要额外包一层（引擎本意是库） | 一致 |
| 风险 | 单点 + 分布式回调复杂度 | 每个应用都要建表并重复治理依赖 |

**代价被明确承认**：审批动作与业务数据变更不再处于同一事务。这正是下面这个未决项要解决的问题。

#### ⚠️ 开放项：业务回调契约（未定）

**状态：未决策。实施前必须由业务方拍板。**

审批中心知道「谁在什么时候通过了哪张单据」，但把结果交回业务应用的方式尚未选定。四个候选，各自的代价：

| 候选 | 一致性 | 业务侵入 | 说明 |
|---|---|---|---|
| **Feign 回调** | 弱（需业务方实现幂等 + 重试补偿） | 中：业务方要暴露回调端点 | 审批中心在节点 `finish` 时同步调用业务方 `/inner/...`；`carlos-job` 已有同类「内部端点 + 令牌」先例可复用。失败只能靠重试，无事务保证 |
| **MQ 事件**（RocketMQ，框架已有 `carlos-spring-boot-starter-mq`） | 最终一致 | 低：业务方订阅即可 | 审批中心发领域事件，业务方各自消费；需要本地消息表保证「审批已提交 + 事件已发出」不丢 |
| **轮询** | 最终一致（延迟即轮询周期） | 低但难看 | 业务方定时查自己单据的流程状态；实现最简单、实时性最差，且查询压力随业务方数量线性增长 |
| **Seata AT** | 强（分布式事务） | 高 | 框架已有 Seata 管道，但把「审批人点一下」纳入全局事务语义牵强，且回滚场景（审批已发生、不可撤回）本身不自洽 |

**当前临时口径**：业务方以 `businessId` 关联自己的单据并自行查询流程状态；契约定稿前**不实现任何回调**。

（原方案里「与业务同事务」那条优势在改判后不再成立，本开放项即它的替代方案，且尚未闭合。）

### 2.2 决策二：框架的价值在"缝合"，不在"包装引擎"

> **⚠️ 2026-09-25 改判**：下图中的方框名 `carlos-spring-boot-starter-workflow` 与「业务应用内嵌」的画法已作废——
> 缝合层现在长在 `carlos-integration/carlos-workflow/carlos-workflow-bus` 内，业务应用不在同一进程。
> 本节的核心判断（框架的价值在补齐引擎拿不到的四件事：办理人、通知、留痕、超时）**不受改判影响**，仍然成立；
> 变化的是这四处缝合发生在审批中心进程内，而非每个业务应用进程内。

Warm-Flow 已提供全部审批语义。框架要补的是它**拿不到**的东西——办理人来自组织架构、通知要走消息中心、留痕要进审计、超时要靠调度中心。这四处缝合是本 starter 的核心工作量，也是选 Warm-Flow 之后框架唯一必须自己做的事。

```
                    ┌─────────────────────────────────────┐
  业务应用           │  carlos-spring-boot-starter-workflow │
                    │                                      │
                    │  WorkflowAutoConfiguration           │
                    │  ├─ Warm-Flow 引擎（7 张表）          │
                    │  ├─ OrgPermissionHandler  ─────┐     │
                    │  ├─ WorkflowGlobalListener ────┤     │
                    │  ├─ WorkflowTimeoutScheduler ──┤     │
                    │  └─ 审批 API 封装               │     │
                    └────────────────────────────────┼─────┘
                                                     │
        ┌────────────────────┬───────────────────────┼──────────────────┐
        ▼                    ▼                       ▼                  ▼
  carlos-org(F)         carlos-message(F)      carlos-audit(F)   carlos-job-client
  办理人解析             待办通知                审批留痕            超时扫描
        │
        └─ ⚠️ 需要补 Feign 端点，见第六节
```

> **(F)** = 通过 Feign 调用（跨进程）。

### 2.3 决策三：不改写 Warm-Flow 的自动配置

与已删除的 `FlowableAutoConfiguration` 不同，本 starter **不重新实现引擎配置**——Warm-Flow 的 `FlowAutoConfig` / `BeanConfig` 已正确处理 Bean 装配、MapperScan、ID 生成器。本 starter 只做三件事：**加依赖、放 Bean、暴露 API**。这是对技术债 #12 的正面回应。

---

## 三、模块与包结构

> **⚠️ 2026-09-25 改判**：下图的模块路径 `carlos-spring-boot/carlos-spring-boot-starter-workflow/` 已**不存在**。
> 实际落点是 `carlos-integration/carlos-workflow/`，分为 `carlos-workflow-api`（Feign + AO/Param + 降级工厂）、
> `carlos-workflow-bus`（apiimpl / Service / convert / 引擎装配，`WorkflowAutoConfiguration` 在此）、
> `carlos-workflow-boot`（端口 9518）与 `carlos-workflow-cloud`。包名 `com.carlos.workflow` 保持不变；
> 包内的 `config` / `dto` / `service` / `exception` 位置与下图基本一致，差异是：
> 控制器从 `controller/` 改名为 `apiimpl/`（因为它们是 Feign 接口的实现），
> 出入参从 `param/` 迁到 api 模块的 `api/pojo/param/`（`ApiWorkflow*Param`），
> 响应对象在 api 模块的 `api/pojo/ao/`（`Workflow*AO`，bus 侧仍有对应的 `Workflow*DTO`），
> 办理人处理器 `handler/` 迁为**测试作用域**。
> 下方内容作为原始设计保留，不再逐条修订。

```
carlos-spring-boot/carlos-spring-boot-starter-workflow/
├── pom.xml
├── README.md
├── docs/
│   └── sql/
│       ├── workflow_postgresql.sql      # 7 张表，从 Warm-Flow v1.8.9 官方脚本落库
│       └── workflow_dm.sql              # 达梦版本（人工转换，见第七节）
└── src/main/
    ├── java/com/carlos/workflow/
    │   ├── config/
    │   │   ├── WorkflowAutoConfiguration.java       # 唯一 @AutoConfiguration
    │   │   ├── WorkflowProperties.java              # @ConfigurationProperties("carlos.workflow")
    │   │   └── package-info.java
    │   ├── handler/                                 # ← 核心：与框架能力的缝合层
    │   │   ├── OrgPermissionHandler.java            # implements PermissionHandler（办理人解析）
    │   │   ├── WorkflowGlobalListener.java          # implements GlobalListener（通知 + 审计 + 扩展点）
    │   │   ├── CarlosDataFillHandler.java           # implements DataFillHandler（createBy/updateBy）
    │   │   ├── CarlosTenantHandler.java             # implements TenantHandler（多租户）
    │   │   └── package-info.java
    │   ├── service/                                 # 对业务暴露的 API（薄封装，不含引擎逻辑）
    │   │   ├── WorkflowDefinitionService.java       # 定义：导入/发布/复制/停用/查图
    │   │   ├── WorkflowInstanceService.java         # 实例：发起/撤销/终止/查询
    │   │   ├── WorkflowTaskService.java             # 任务：通过/驳回/自由跳转/转办/委托/加签/减签
    │   │   ├── WorkflowTodoService.java             # 待办/已办/抄送（业务最常用）
    │   │   └── package-info.java
    │   ├── integration/                             # 对外部服务的防腐层，便于测试与替换
    │   │   ├── OrgUserResolver.java                 # 调 carlos-org-api 的 Feign
    │   │   ├── WorkflowNotifier.java                # 调 carlos-message 的 MessageSendApi
    │   │   ├── WorkflowAuditRecorder.java           # 调 carlos-audit 的 ApiAuditLogMain
    │   │   └── package-info.java
    │   ├── scheduler/
    │   │   ├── WorkflowTimeoutScheduler.java        # @ScheduleJob + @AutoRegisterJob
    │   │   └── package-info.java
    │   ├── dto/ param/ vo/                          # 遵循框架分层：Service 返回 DTO/Entity
    │   ├── enums/
    │   │   ├── WorkflowErrorCode.java               # implements ErrorCode，模块位 07
    │   │   ├── ApprovalStatusEnum.java              # implements BaseEnum + @AppEnum
    │   │   ├── ApprovalActionEnum.java
    │   │   └── package-info.java
    │   └── exception/
    │       ├── WorkflowException.java               # extends ComponentException
    │       └── package-info.java
    └── resources/
        ├── META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
        └── workflow-default.yml                     # 参考配置（非自动加载，与 sms/encrypt 惯例一致）
```

**命名**：模块 `carlos-spring-boot-starter-workflow`，包 `com.carlos.workflow`（与 `com.carlos.sms`/`com.carlos.encrypt` 一致）。

---

## 四、依赖与版本治理（本方案风险最高的部分）

### 4.1 依赖引入

```xml
<!-- 引擎（该 artifact 是仅含 POM 的 2KB 空壳，只会传递依赖） -->
<dependency>
  <groupId>org.dromara.warm</groupId>
  <artifactId>warm-flow-mybatis-plus-sb4-starter</artifactId>
</dependency>

<!-- 流程设计器（可选，但人工审批几乎必需；不含在上面那个 starter 里，必须显式加） -->
<dependency>
  <groupId>org.dromara.warm</groupId>
  <artifactId>warm-flow-plugin-ui-sb-web</artifactId>
</dependency>
```

### 4.2 依赖治理（已由 POC 实测修正）

原先基于已发布 POM 推导的三条隐患，经 `mvn dependency:tree` 实测**只有一条成立**：

| # | 原预测 | 实测结果 |
|---|---|---|
| 1 | mybatis-plus 会被拽到 3.5.15 / 3.5.12 | ❌ **误判**。`carlos-dependencies` 已 import `mybatis-plus-bom:3.5.16`，dependencyManagement 优先于传递版本声明，实测全部解析为 **3.5.16**。**无需额外处理** |
| 2 | `slf4j-api` 会解析为 1.7.36 | ❌ **误判**。实测 **2.0.17**；Spring 构件实测全部为 `4.0.6` / `7.0.7`，**无 2.7.18 或 5.3.31 泄漏**。**无需额外处理** |
| 3 | 双 `JsonConvert` SPI 提供者导致 JSON 实现不确定 | ✅ **成立，且必须处理** |
| 4 | Jackson 3 在 Warm-Flow 侧为 `optional` 不传递 | ✅ 成立，starter 需显式声明 `spring-boot-starter-json` |

第 3 条的实证：

```
warm-flow-plugin-json-jackson3-1.8.9.jar → JsonConvertJackson3                          （1 个提供者）
warm-flow-plugin-json-v1-1.8.9.jar       → JsonConvertSnack / Snack4 / Jackson / FastJson / Gson（5 个）
```

两者都声明在 `META-INF/services/org.dromara.warm.flow.core.json.JsonConvert`，`FlowEngine.spiLoad()` 用 `ServiceLoaderUtil.loadFirst()` 取第一个可链接者——**顺序取决于 jar 顺序**，在遗留应用里可能静默降级到 Jackson 2 / FastJson；若全部不可链接则 `jsonConvert` 为 `null`，首次序列化即 NPE。

> **⚠️ 关键且非直觉：必须排除两次。** `warm-flow-plugin-json-v1` 有**两条独立引入路径**：
>
> ```
> warm-flow-mybatis-plus-sb4-starter → …-sb-starter → plugin-modes-sb → plugin-json-v1
> warm-flow-plugin-ui-sb-web        → plugin-modes-sb → plugin-json-v1     ← 只排除第一处无效
> ```
>
> 实测只排除第一处时 `dependency:tree` 里 `plugin-json-v1` **仍然存在**；**两处都排除后才归零**。所以两个 `<dependency>` 各自都要带 `<exclusion>`。

### 4.3 ⚠️ Bean 名冲突（会导致启动失败）— 已由 POC 实测修正

> **原文列的 20 个名字不完整。** 上下文启动测试在**运行时实测到 34 个** Bean 名（`WorkflowContextStartupTest` 断言 28 个固定名 + 报告派生名）。实测清单：

**固定短名（27 个，最易撞名）**

```
chartService        definitionDao       definitionService   flowUserDao
flowUserService     formDao             hisTaskDao          hisTaskService
initFlow            instanceDao         instanceService     nodeDao
nodeService         skipDao             skipService         taskDao
taskService         warmFlowSpringUtil  warmMapper
flowDefinitionMapper flowFormMapper     flowHisTaskMapper   flowInstanceMapper
flowNodeMapper      flowSkipMapper      flowTaskMapper      flowUserMapper
```

**Spring 派生的全限定名（4 个）**——这些前缀是 FQN，撞名概率低，但**说明它们不是短名**：

```
org.dromara.warm.flow.spring.boot.config.FlowAutoConfig
org.dromara.warm.flow.ui.config.WarmFlowUiConfig
org.dromara.warm.flow.ui.controller.WarmFlowController
org.dromara.warm.flow.ui.controller.WarmFlowUiController
org.dromara.warm.plugin.modes.sb.helper.SpelHelper
warm-flow-org.dromara.warm.plugin.modes.sb.config.WarmFlowProperties
```

三处对原文的修正：

1. **委托 Bean 名是 `instanceService`，不是 `insService`**（接口叫 `InsService`，Bean 名不同，`javap BeanConfig` 与运行时一致）。
2. **`spelHelper` 不是短名**：`SpelHelper` 是 `@Configuration` + `@Import` 注册，名字是 FQN。短名里只有 `warmFlowSpringUtil`（`@Component("warmFlowSpringUtil")`）。
3. **漏了 9 个 Mapper**（`@MapperScan("org.dromara.warm.flow.orm.mapper")` 注册的 `warmMapper` + 8 个 `flow*Mapper`）和 `WarmFlowProperties` 的 `<prefix>-<FQN>` 派生名。

**风险性质**：Warm-Flow 的 `BeanConfig` **完全没有 `@ConditionalOnMissingBean`**，所以宿主应用一旦占用上述名字，`spring.main.allow-bean-definition-overriding=false`（Boot 默认）下就是**硬启动失败**，没有回退。

> **⚠️ 2026-09-25 改判后的影响**：集中式部署下这些名字只存在于 `carlos-workflow` **一个进程**里，
> **不再对每个业务应用生效**——业务应用不引入引擎，也就不会撞名。本节保留的意义收窄为两条：
> ① `carlos-workflow-bus` 内部不得新增同名 Bean（`WorkflowContextStartupTest` 持续断言）；
> ② 保留名清单是该服务自身的维护约束，不再是「宿主应用接入须知」。
> 原计划里的「负向测试」（构造同名宿主 Bean 复现 `BeanDefinitionOverrideException`）风险等级随之从 P0 降级。

**处理**：
- ✅ 已写 `WorkflowContextStartupTest`（H2，无需外部库）断言上下文可启动 + 这些 Bean 存在；
- ⚠️ **但尚未写"负向测试"**——即真正构造一个同名宿主 Bean 去复现 `BeanDefinitionOverrideException`。当前只证明了"名字存在"（必要条件），没证明"撞名必失败"（充分条件）。鉴于 `BeanConfig` 无 `@ConditionalOnMissingBean` 是代码事实，风险结论不变，但该项仍是 P0 的未闭合点。
- 完整保留名清单已写入模块 `README.md`。

**另一个实测发现（独立于撞名）**：上下文在**空库**上就能正常启动——Warm-Flow 启动时不做 schema 校验，Mapper 与 DAO 都是惰性的。这意味着 POC 不依赖数据库即可验证上下文与撞名，但也意味着**表缺失只会在首次调用时暴露**。

### 4.4 BOM 与模块注册位置（与现有惯例一致）

> **⚠️ 2026-09-25 改判后的影响**：本节 1~4 条描述的是「把 `carlos-spring-boot-starter-workflow` 注册进 starter 体系」的做法，
> 该模块**已删除**（`carlos-spring-boot/pom.xml` 不再包含它），因此这几条作为历史记录保留，不再照此实施。
> 实际落点：`carlos-integration/pom.xml` 新增 `carlos-workflow` 聚合模块，并在其
> `dependencyManagement` 登记 `carlos-workflow` / `carlos-workflow-api` / `carlos-workflow-bus`；
> `carlos-dependencies/pom.xml` 用 `${revision}` 登记上述三个坐标（聚合模块为 `<type>pom</type>`），
> 并**移除**原 `carlos-spring-boot-starter-workflow` 条目；
> `<!-- Workflow -->` 的 `<warm-flow.version>1.8.9</warm-flow.version>` 与两个 `org.dromara.warm`
> 受管条目**保持不变**（引擎版本治理的结论不受部署形态影响）。
> 「不在子模块里写版本号」这条依然有效。

1. `carlos-dependencies/pom.xml` `<properties>`：在 `<!-- SMS -->` 块之后新增 `<!-- Workflow -->` 块，放 `<warm-flow.version>1.8.9</warm-flow.version>`。
2. `carlos-dependencies/pom.xml` `<dependencyManagement>`：`carlos-spring-boot-starter-workflow` 用 `${revision}` 放进「核心模块」簇；`org.dromara.warm:warm-flow-mybatis-plus-sb4-starter` 与 `warm-flow-plugin-ui-sb-web` 放进第三方块（原 `<!-- Workflow Engine -->` 槽位）。
3. `carlos-spring-boot/pom.xml`：`<module>` 加在 `carlos-spring-boot-starter-web` 之后（字母序）。
4. **不要**在 starter 的 pom 里写版本号（BOM 统一管理），**不要**重复声明 `spring-boot-configuration-processor`（父 pom `carlos-spring-boot/pom.xml` 已为所有子模块声明）。

### 4.5 POC 验证状态与环境限制

- ✅ 依赖树已实测，结论见 4.2；两处排除均已在 `carlos-spring-boot-starter-workflow/pom.xml` 落地，`dependency:tree` 中 `plugin-json-v1` **为 0**。
- ✅ 另有**类路径级**回归测试守护该排除（断言 SPI 提供者只有 1 个且为 `JsonConvertJackson3`，并断言其类可加载），避免未来升级时静默回退。
- ✅ **环境可达性（已更正 2026-09-25）**：项目 PostgreSQL **从本机构建环境完全可达**。
  - 真实端点：**`100.108.187.89:15432`**（Tailscale 地址 + 映射端口），已用 `Test-NetConnection` 验证 `TcpTestSucceeded=True`；`carlos_framework` 库（14 MB）、`nacos`、`postgres` 三个库均 `datallowconn=true`。
  - **⚠️ 更正记录**：本方案初稿曾断言"PG 不可达"——该结论**错误**。错因是我只探测了自己猜的几个地址（`localhost:5432`、`127.0.0.1:5433`、`192.168.3.40:5432`、`172.27.0.2:5432`），**从未探测实际正在被使用的那条连接**。`172.27.0.2` 只是 PG **从服务端视角**看到的自身地址（容器内网 IP），并非客户端应拨的地址。事实来源：`pg_stat_activity` 的 `client_addr = 100.85.166.80`（本机）与 `Test-NetConnection` 对本机 node 进程实际建立的连接 `100.85.166.80 → 100.108.187.89:15432` 的观测。
  - **教训记入本方案**：判断"环境是否可达"必须依据**正在生效的连接**，而不是猜测候选地址。
- ✅ **8 张表 DDL 已在真实 PostgreSQL 17.10 上执行并验证通过（2026-09-25）**（官方 7 张 + 补建的 `flow_form`）。
  - 执行位置：**`carlos_framework` 库的 `public` schema**——与框架所有服务的连接方式一致（各服务数据源均为 `jdbc:postgresql://${DB_URL}?currentSchema=public&stringtype=unspecified`）。
  - **⚠️ 过程中的一次修正**：初次执行时我把表建在了独立 schema `workflow_poc`，出发点只是"最小影响面"。这是**过度谨慎且有害的**——表不在框架服务实际查找的 schema 里，会造成 POC 验证环境与真实部署环境错位。已用 `ALTER TABLE ... SET SCHEMA public` 全部迁入 `public`，`workflow_poc` schema 已删除。
  - 验证结果：`public` 下 **8 张 `flow_*` 表**全部建立（官方 7 张 + 补建的 `flow_form`）；`public` 总表数 **72**（原 64 + 8）；**注释 144 条**（官方脚本 128 + `flow_form` 16）；**索引 10 个**（8 个主键 + `flow_user` 上 2 个显式索引）；迁移后注释与索引全部保留。
  - 列数逐表核对与官方脚本一致：`flow_definition` 19 / `flow_node` 21 / `flow_skip` 16 / `flow_instance` 17 / `flow_task` 15 / `flow_his_task` 23 / `flow_user` 10；补建的 `flow_form` 为 **15 列**（官方 MySQL 脚本只有 13 列，缺 `create_by` / `update_by`，详见 7.2）。
  - 建表前已确认 `public` 无 `flow_*` 表、约束/索引名**零冲突**。
  - **这条"从未执行过的代码"现已执行验证。** 回滚方式：`DROP TABLE flow_definition, flow_node, flow_skip, flow_instance, flow_task, flow_his_task, flow_user, flow_form CASCADE;`
- ⚠️ **剩余未验证仅两项**：
  1. **Java 侧的 gated 测试 `WorkflowPostgreSqlDdlTest` 仍未执行**——它的 SQL 本身已被证明可执行（上面），但该测试**自己的语句切分器与 `information_schema` 断言代码**仍是未执行代码，需要口令才能跑；
  2. **端到端流程链路**（导入定义 → 发起 → 审批 → 驳回）——需要口令 + 一份流程定义 JSON。
  两者都卡在同一处：**JDBC 口令在 `.claude/mcp.local.env`（敏感文件，不得读取）**。DDL 执行可绕过（走 MCP，无需凭据），但进程内 Spring 测试绕不开。
- ✅ **零冲突确认**：建表前已确认 `public` 下无任何 `flow_*` 表、无 `workflow_poc` schema。
- ❌ **达梦**：本机无 DM 实例，DDL 转换与分页验证均**未执行**。

---

## 五、核心设计：办理人解析（本方案的技术核心）

### 5.1 要实现的接口

```java
package org.dromara.warm.flow.core.handler;

public interface PermissionHandler {
    /** 当前用户拥有的权限标识集合，用于校验办理权限 */
    List<String> permissions();
    /** 当前用户唯一标识，写入 create_by/update_by/审批人 */
    String getHandler();
    /** 把设计器里预设的"角色/部门"标识转换成真实用户 id */
    default List<String> convertPermissions(List<String> permissions) { return permissions; }
}
```

### 5.2 ⚠️ 注册方式：必须用 Spring Bean，绝不能配路径

`FlowEngine.initBean` 的解析顺序是：

1. `warm-flow.permission-handler-path` 指定的 FQN → **`newInstance()` 无参构造，绕过 Spring**；
2. `FrameInvoker.getBean(PermissionHandler.class)` → **取 Spring Bean**；
3. 兜底（`PermissionHandler` 无兜底，为 `null`）。

我们的实现要调 `carlos-org` 的 Feign（需要注入），**走路径配置会因无参构造而失去依赖注入**。因此：

- 实现类标注 `@Component` / 由 `@Bean` 注册；
- **`warm-flow.permission-handler-path` 留空**（`TenantHandler` / `GlobalListener` / `DataFillHandler` 同理）。

### 5.3 权限校验语义（决定 convertPermissions 怎么写）

`TaskServiceImpl.checkAuth` 做的是**交集判定**：`当前用户的 tokens ∩ 该任务 flow_user.processed_by 列表`，且发生在 `convertPermissions` **之后**。

因此有两种自洽写法，**必须二选一**：

| 方案 | `permissions()` 返回 | `convertPermissions()` | 说明 |
|---|---|---|---|
| **A. 全量转用户 id（推荐）** | **用户 id 列表** | 把角色/部门标识展开成用户 id | 校验词汇统一为 userId，与 `flow_user.processed_by` 一致；权限判断在框架侧可控 |
| B. 保留标识 | 用户/角色/部门的**标识**（如 `role:1`） | 恒等（不覆写） | 词汇与设计器 `storageId` 一致，但校验逻辑分散在表达式里 |

**选方案 A**，因为角色/部门→用户的展开必须走 `carlos-org`，放在 `convertPermissions` 是唯一能拿到 org 数据的位置。

### 5.4 办理人表达式约定（写进框架规范）

`flow_node.permission_flag` 用 `@@` 分隔多个表达式，逐个求值：

| 前缀 | 策略 | 形式 | 用途 |
|---|---|---|---|
| `#` | SpEL（BeanConfig 注册） | `#{@bean.method(#handler)}` | 动态计算，如"取申请人的直属上级" |
| `$` | 流程变量 | `${flag}` | 从流程变量取，如 `${approvers}` |
| 无 | 字面量 | `role:1` / `dept:7` / `1001` | 静态指定，由 `convertPermissions` 展开 |

**框架需固化一套约定**并写进规范文档，例如 `role:{roleId}`、`dept:{deptId}`、`deptLeader:{deptId}`、`user:{userId}`、`upLeader`（动态取上级）。目前 Warm-Flow **不提供任何内置前缀词汇**，全靠使用方自定义——不定约定，下游会各自发明写法。

### 5.5 ⚠️ 六项能力缺口：`carlos-org` 必须补 Feign 端点

实测 `carlos-org` 目前**无法支撑**办理人解析。这是本方案**最大的外部依赖风险**：

| 需要的能力 | 现状 | 影响 |
|---|---|---|
| 按 **userId 查用户**（取姓名/手机号用于通知） | **NOT FOUND**（无 Feign 端点；同 JVM 才有 `OrgUserManager#getDtoById`）。`carlos-auth` 已因同一缺口降级并打 warn | 通知发不出去 / 待办列表无姓名 |
| 按 **userId 批量查用户** | **NOT FOUND** | 待办列表 N+1 |
| **用户的直属上级**（`reportTo`） | **NOT FOUND**：数据在 `org_user_position.report_to`，但 `ApiOrgUserPosition` 是**空接口**、`ApiOrgUserPositionImpl` 是**空实现**，且无任何按 `reportTo` 反查的方法；全仓库 `reportTo` 无一处读取逻辑 | "审批人=直属上级"、"超时升级上级" 均不可用 |
| 按**部门**取用户列表 | 仅同 JVM `OrgDepartmentManager#getUsersByDeptId`（且不含子部门） | 候选组解析不可用 |
| 按**角色**取用户列表 | 仅同 JVM `OrgRoleManager#getRoleUsers` | 候选组解析不可用 |
| 部门树缓存快路径 | 仅在 `carlos-org` 进程内 | 跨服务退化为多次查询 |

**结论**：需要在 `carlos-org` 新增 5 个 Feign 端点（按 id 查、批量按 id 查、按 reportTo 查下属、按部门查用户、按角色查用户），并补 `OrgUserPositionManager#listByReportTo`。

**这项收益超出工作流本身**：它同时修好了 `carlos-auth` 早已抱怨的用户查询缺口。

### 5.6 P1 实施结果（已完成）

5 项能力全部落地，改 `carlos-org` 下 20 个文件 + 新增 1 份 API 文档。落地清单：

| # | Feign 方法 | Manager 支撑 |
|---|---|---|
| 1 | `ApiOrgUser#getUserById(Long)` | 复用 `OrgUserManager#getDtoById` |
| 2 | `ApiOrgUser#listUsersByIds(Collection<Long>)` | 新增 `OrgUserManager#listDtoByIds` |
| 3 | `ApiOrgUserPosition#listSubordinatesByUserId(Long)` | **新增 `OrgUserPositionManager#listByReportTo`（全仓库第一处读取 `report_to`）** |
| 4 | `ApiOrgDepartment#listUsersByDeptId(Long)` | 复用 `OrgDepartmentManager#getUsersByDeptId` |
| 5 | `ApiOrgRole#listUsersByRoleId(Long)` | 新增 `OrgRoleManager#listUserIdsByRoleId` + 复用 `listDtoByIds` |

实施中发现并确认的三点：

1. **✅ `ApiOrgUserPosition` 原不在 `carlos-org-api`，而在 `carlos-org-bus`——已于 2026-09-24 决策并完成迁移。**
   原路径：`carlos-org-bus/src/main/java/com/carlos/org/position/api/`——整个 `position` 子树（api / ao / param / apiimpl / fallback / manager / mapper / entity / service / controller）都长在 bus 模块里。
   当时后果：只依赖 `carlos-org-api` 的消费方**看不到这个接口**；工作流 starter 要"按 `reportTo` 查下属"就得依赖 `carlos-org-bus`，破坏 api/bus 隔离。
   **处置（A2 决策选 (a)）**：把 `position/api` 整个子树（**28 个文件**：6 接口 + 6 fallback + 6 AO + 6 Param + 4 package-info）迁入 `carlos-org-api`。
   - **包名 `com.carlos.org.position.api.*` 保持不变** → **零 import 改动**。已核验该子树全部 import 在 api 模块均可解析、无 bus 专有类型泄漏；bus 侧仅 6 个 `apiimpl` + 1 个 `package-info` 引用它，经 api 依赖正常解析。
   - 迁移时该接口**零消费方**（只被自身 fallback 与 apiimpl 引用），因此**零破坏**。
   - `entity` / `mapper` / `manager` / `service` / `convert` / `apiimpl` / `controller` 仍留在 bus。
   - **行为影响**：`carlos-org-api` 的消费方现在会额外注册 **6 个 Feign 客户端**（contextId：`position` / `positionCategory` / `positionLevel` / `positionHistory` / `positionRole` / `userPosition`）。已核验全模块 13 个 contextId **无重复**。
   - 构建验证：`carlos-org-api` + `carlos-org-bus` **BUILD SUCCESS**。
2. **`OrgRoleManager#getRoleUsers` 不是列表原语**——真实签名是分页的 `Paging<OrgRoleUserDTO> getRoleUsers(Serializable, OrgRolePageParam)`，且只投影 userId/account/name/phone/mainDeptName，无法直接支撑 `List<OrgUserAO>`。因此第 5 项另建了 `listUserIdsByRoleId`。
3. **✅ 租户过滤约束——已决策并实现显式租户变体（B2，2026-09-25）**
   - 事实：这些查询经过 `OrgTenantLineHandler`（`@Component`；`carlos-org-boot/application.yml` 里 `tenant: true`，拦截器**默认启用**），租户取自 `SecurityUtils.getCurrentUser().getTenantId()`，取不到回落 `platform-tenant-id`。
   - **关键区分（已核实）**：框架的 `FeignRequestInterceptor` **确实透传 `X-Tenant-Id`/`X-User-Id`**，但**只从当前 `HttpServletRequest` 的请求头取**。所以：
     - 用户请求链路（发起/审批）→ 租户自动透传，**用原方法即可**
     - **后台线程（`carlos-job` 超时扫描触发"升级上级"）→ 没有 HttpServletRequest，什么都不透传 → 按平台租户解析办理人 → 多租户下解析错人**
   - **决策**：为 5 个端点各加一个 `...AndTenant` 变体，**专供无请求上下文的路径**（后台任务、系统间调用）。**已实现**：4 个接口 + 4 个 fallback + 4 个 apiimpl + 4 组 Manager 方法（共 8 文件）+ 6 个测试文件，纯增量（原 5 个方法未动）。
   - 实现沿用既有先例 `getUserByAccountAndTenant` 的写法：`IgnoreStrategy.builder().tenantLine(true)` 绕过租户拦截器 + wrapper 里显式 `.eq(Xxx::getTenantId, tenantId)`。已核验该写法对 `IService` 路径同样有效（反编译 MP 3.5.16 确认 `InterceptorIgnoreHelper` 是纯 ThreadLocal 作用域）。
   - **实施中发现的两处与预期不符**：
     - `org_user_department` **没有 `tenant_id` 列**，且租户拦截器**显式忽略**该表（`OrgProperties` 的 `tables` 清单 + 专门测试）。因此"按部门取用户"的租户过滤只能落在 `org_user` 上，关系表查询本身无需绕过。
     - fallback 的降级约定**不统一**：`ApiOrgUserFallbackFactory` 的列表方法用 `Result.error(...)`，其余三个用 `Result.success(Collections.emptyList())`。已按**文件局部约定**分别处理，未强行统一。
   - 验证：`carlos-org-bus` 测试 **107 通过 / 0 失败**（基线 83，新增 24）；含 5 个端点的真实 Spring MVC 派发测试（路径 + 参数绑定）。
   - **⚠️ 尚未闭环**：**`carlos-job` 的升级路径尚未切换到 `AndTenant` 变体**——本次只交付了"使其可用的 API"，真正修掉"超时升级解析错租户"需要在该任务里传入租户。这项工作归入 P6（超时审批实现）。

已选定的语义（均写入 javadoc 与 API 文档）：
- 按部门取用户：**不递归**子部门（与同 JVM 的 `getUsersByDeptId` 行为保持一致，避免远程/本地语义分叉）
- 按角色取用户：**排除已过期授权**（`expire_time IS NULL OR > now()`），与权限链路一致；**不按 `dept_id` 收窄**。注意这会与该角色详情里的 `userCount`（不带过期过滤）对不上
- 按 `reportTo` 查下属：**仅直接下属**，按 user_id 去重，**不过滤 `position_status`**（已离职但 `report_to` 残留的记录仍会返回）

验证方式：**无可用数据库**，因此是用真实类 + mock 跑通的（28/28 断言通过），覆盖 4 个 `*ApiImpl` 的 5 个 URL、Feign 客户端编码、以及 Manager 生成的 MyBatis-Plus wrapper SQL。**未经真实 schema/Redis 端到端验证。**

---

## 六、外围能力缝合

### 6.1 通知（carlos-message）

用 `MessageSendApi#sendAsync(MessageSendParam)`。三点约束：

1. **模板驱动，无裸内容重载**：`templateCode` 必填，内容全部来自消息中心的 `message_template`。**必须先设计并预置审批类模板**（待办提醒 / 超时提醒 / 审批结果），否则发不出去。
2. **`sendBatch` 是空壳**（直接返回"批量发送功能待实现"）→ 批量通知要**循环 `sendAsync`**。
3. **`receiverType=2/3`（部门/角色）不会被展开**——`carlos-message` 不依赖 `carlos-org`。候选人集合必须**由我们在 `OrgPermissionHandler` 里解析成 userId 列表**后逐个发。

触发点：`WorkflowGlobalListener` 的 `create`（新待办）/ `finish`（审批结果）/ 超时扫描。

### 6.2 审计留痕（carlos-audit）

用 `ApiAuditLogMain#saveAuditLog(ApiAuditLogMainParam)`，**异步 fire-and-forget**（禁止依赖它已落库；需强一致用 `saveAuditLogSync(param, timeoutMs)`）。

好消息：`ApiAuditLogMainParam` **已有审批专用字段**，无需扩展：

```
processId（流程实例）、taskId（任务）、approverId（审批人）、
approvalComment（审批意见）、logStatus（SUCCESS/FAIL...）、
targetId/targetType（业务单据）、eventTime、riskLevel、bizScene
```

> **⚠️ 2026-09-26 更正**：原文写「`@Log` 跨服务不可用」——该结论当时成立，但**已过时**。
> 现已补上跨服务链路：`carlos-audit-api` 里新增 `AuditMqLogStorage`，`@Log` → `LogStorage` →
> MQ 主题 `audit:log` → carlos-audit 既有消费端 → ClickHouse。业务服务只需同时依赖
> `carlos-audit-api` + `carlos-spring-boot-starter-log` + `carlos-spring-boot-starter-mq`
> 即自动生效（三者缺一会**静默**退化成只打应用日志，需注意）。carlos-workflow 已按此接入 `@Log`。
>
> 仍然成立的两点：框架里**不存在 `@AuditLog`**；以及经 Feign API（`ApiAuditLogMain`）直投
> 依然是**可用且适合**的路径 —— 需要精细控制非 HTTP 上下文（如定时任务、后台作业）时优先用它。
> 本 starter 若要在无 Redis 的宿主中使用，仍应走 Feign API 而非 `@Log` 的 MQ 链路。

### 6.3 超时审批（carlos-job）

> **⚠️ 2026-09-25 改判**：下面的 `@ConditionalOnClass(carlos-job-client)` 保护是「starter 必须能在未引入调度客户端时启动」这一嵌入式前提下的产物。
> 审批中心是独立服务，`carlos-job-client` 是否引入由它自己决定（不引入则没有超时能力），
> 该条件注解作为「可裁剪」的写法仍可沿用，但**不再承担「保护宿主应用启动」的职责**。

Warm-Flow **无一等定时器**，用框架已有的 `carlos-job` 实现：

```java
@Component
@ConditionalOnClass(name = "com.carlos.job.client.annotation.ScheduleJob")
public class WorkflowTimeoutScheduler {

    @ScheduleJob("workflowTimeoutHandler")
    @AutoRegisterJob(cron = "0 */5 * * * ?", description = "审批超时扫描（自动通过/升级上级）")
    public void scanTimeout() { ... }
}
```

逻辑：扫描超时未办的 `flow_task` → 按流程配置的 `timeoutAction` 执行「自动通过」或「转办上级」→ 写审计 + 发通知。

**关键设计点**：`@ConditionalOnClass` 保护——`carlos-job-client` 是**可选依赖**，未引入时 starter 必须仍可启动（降级为无超时能力）。

### 6.4 多租户

`TenantHandler#getTenantId()` 是接口，Warm-Flow 的 7 张表都带 `tenant_id` 列，但**它不注册 MyBatis-Plus 的 `TenantLineInnerInterceptor`**。需由 `CarlosTenantHandler` 提供租户值，并确认与 `carlos-spring-boot-starter-datacenter` 的多租户机制不冲突。

### 6.5 逻辑删除（坑）

Warm-Flow 的实体上硬编码了 `@TableLogic(value = "0", delval = "1")`，所以 **`warm-flow.logic-delete*` 配置在 MyBatis-Plus 路径下完全无效**，删除值是 `"1"` 而非配置里的 `"2"`。DDL 默认值是 `'0'`。**不要依赖这两项配置**。

---

## 七、数据库

### 7.1 表清单（官方 7 张 + 补建 1 张 = 实际 8 张）

官方 7 张：

`flow_definition` / `flow_node` / `flow_skip` / `flow_instance` / `flow_task` / `flow_his_task` / `flow_user`

补建 1 张（官方脚本缺失，见 7.2）：

`flow_form`

> **⚠️ 口径说明**：本方案下文及早期章节中出现的"7 张表"，均指**官方 7 表脚本**（`workflow_postgresql.sql`）。
> 本项目**实际部署为 8 张表**（含补建的 `flow_form`，脚本见 `docs/sql/workflow_form_postgresql.sql`）。

- `flow_node.permission_flag varchar(200)` — 办理人表达式（见 5.4）
- `flow_user` — 任务办理人关联（`type` 1审批人 2转办人 3委托人；`processed_by`；`associated`=task.id）
- `flow_his_task` — 审批历史，含 `approver`、`collaborator`、`message`（审批意见）、`target_node_code`（驳回目标）；**注意它没有 `create_by`/`update_by`**，审批人是 `approver`
- 全部带 `tenant_id` 与 `del_flag`

状态字典（用于枚举与前端）：`FlowStatus` = 0待提交 1审批中 2审批通过 3自动完成 4终止 5作废 6撤销 7取回 8已完成 9已退回 10失效 11拿回 12重启 13暂存；`PublishStatus` = 0未发布 1已发布 9已失效；`ActivityStatus` = 0挂起 1激活。

### 7.2 ⚠️ DDL 不在 jar 里，必须自己搬

Warm-Flow 的**任何 Maven 构件都不含 `.sql`**（21 个 jar 全部核验过）。脚本只在 GitHub 仓库 `dromara/warm-flow` 的 tag `v1.8.9` 的 `sql/` 目录，且**只有 mysql / oracle / postgresql / sqlserver 四种**。

按框架现有惯例（`docs/db/README.md`：各模块建表脚本放 `carlos-integration/{module}/docs/.../database/`，**手工执行，不自动应用**）：

- `PostgreSQL` 版：直接落库为 `carlos-integration/carlos-workflow/docs/sql/workflow_postgresql.sql`
  （**2026-09-25 改判**：脚本已随模块从 `carlos-spring-boot-starter-workflow/docs/sql/` 迁到 `carlos-integration/carlos-workflow/docs/sql/`，
  与 `carlos-job` 一致；补建的 `flow_form` 脚本 `workflow_form_postgresql.sql` 同目录）
- `达梦` 版：人工从 Oracle 或 MySQL 脚本转换（7 张表，无 Liquibase 干扰，属常规工作量）
- **不要**打进 jar，**不要**挂进 `starter-migration` 的 changelog（现无任何业务模块这样做）

另外两个注意：

- **✅ `flow_form` 不在官方 7 表脚本里，但 1.8.9 会映射它——已决策"补建"并完成（2026-09-25）**：
  - 原始事实（P0 实测）：`FlowForm` 实体的 `@TableName` 值就是 `flow_form`；运行时上下文确实注册了 `formDao` 与 `flowFormService` 两个 Bean；但 `docs/sql/workflow_postgresql.sql` 中 `flow_form` 出现 **0 次**；该表 DDL 只存在于仓库的 `sql/mysql/v1-upgrade/warm-flow_form.sql`，头部写着 `-- TODO form 开发中`，**且无 Oracle/PG/达梦版本**。
  - 后果：走到表单功能就"表不存在"。**启动不暴露**（Warm-Flow 不做 schema 校验、Mapper/DAO 惰性），属**延迟到首次调用才炸**的坑。
  - **决策**：采用 Warm-Flow 内置表单设计器（需求方确认）→ **走"补建"**。
  - **⚠️ 补建过程中的关键发现：官方脚本与实体不一致。** 官方 MySQL 脚本 `flow_form` 只有 **13 列**，**缺 `create_by` / `update_by`**；但 1.8.9 实体 `FlowForm`（`warm-flow-mybatis-plus-core-1.8.9-sources.jar`）映射了 `private String createBy; private String updateBy;`，MyBatis-Plus 会解析为 `create_by` / `update_by`。**照抄官方脚本建表，`flow_form` 的 INSERT/SELECT 仍会失败**（`column "create_by" does not exist`）。因此本项目版本按实体补齐为 **15 列**。
  - 落地产物：新增 `docs/sql/workflow_form_postgresql.sql`（15 列，文件头写明与官方 13 列的差异及原因）；已在 `carlos_framework.public` 执行并验证：**15 列、类型与注释齐全**。
  - 达梦版本仍待补（与 7 表脚本一并转）。
- **升级脚本只有 MySQL 版**（20 个增量脚本），其他库要自己从 MySQL 脚本转。**记录到维护手册**：以后每次升级 Warm-Flow 都要重做达梦升级脚本，并**复检 `flow_form` 的列集合是否又变动**（该表上游标着 TODO，结构未定型）。
- **升级脚本只有 MySQL 版**（20 个增量脚本），其他库要自己从 MySQL 脚本转。**记录到维护手册**：以后每次升级 Warm-Flow 都要重做达梦升级脚本。

### 7.3 与 MyBatis-Plus 的配合

- Warm-Flow 用 `@MapperScan("org.dromara.warm.flow.orm.mapper")` 注册 8 个 Mapper，全部继承 MP `BaseMapper`；**自带的 XML 是空壳**，SQL 全由 `QueryWrapper`/`LambdaQueryWrapper` 生成 → **不违反框架"严禁直接写 SQL"的规范**。
- 它**不注册** `PaginationInnerInterceptor`，但 `UserService#page` / `FormService#publishedPage` 依赖它。框架的 `MyBatisPlusConfig` 已注册（用 `carlos.mybatis.db-type`），**达梦环境必须显式设 `carlos.mybatis.db-type: dm`**（默认是 `POSTGRE_SQL`，否则分页 SQL 生成错误）。
- Warm-Flow 的实体/服务层级**独立于**框架的 `BaseService`/`BaseServiceImpl`，无类层次冲突。
- ID 生成走 `MybatisPlusIdGen`，优先用宿主的 MP `IdentifierGenerator` → 会复用框架的雪花 ID，无需额外配置。

---

## 八、API 设计（对业务暴露）

遵循框架分层与强制规范：Service 返回 **DTO/Entity**（严禁 VO）、异常用 `BusinessException`/`ComponentException`（注意：**`SystemException` 与 `ServiceException` 在代码中并不存在**，AGENTS.md 与规范 skill 里的描述有误，本 starter 不受其影响）、枚举实现 `BaseEnum` + `@AppEnum`、属性用 `@ConfigurationProperties`、POJO 用 Lombok。

```java
// 发起
Long start(WorkflowStartParam param);              // flowCode + businessId + variable + 首节点办理人

// 待办 / 已办
Paging<WorkflowTodoDTO> pageTodo(WorkflowTodoQueryParam param);
Paging<WorkflowDoneDTO> pageDone(WorkflowDoneQueryParam param);

// 审批动作（全部收敛到 TaskService，参数用 FlowParams 风格的 Param 对象）
void approve(WorkflowApproveParam param);          // 通过（可携带下一节点办理人）
void reject(WorkflowRejectParam param);            // 驳回（可指定目标节点 = 自由驳回）
void transfer(WorkflowTransferParam param);        // 转办
void depute(WorkflowDeputeParam param);            // 委托
void addSignature(WorkflowSignParam param);        // 加签
void reduceSignature(WorkflowSignParam param);     // 减签
void revoke(Long instanceId, String reason);       // 撤销（发起人）
void terminate(Long instanceId, String reason);    // 终止（管理员）

// 查询
WorkflowInstanceDTO getInstance(Long instanceId);
List<WorkflowHistoryDTO> listHistory(Long instanceId);
WorkflowChartDTO getChart(Long instanceId);        // 流程图 + 各节点状态着色
```

### 8.1 错误码

`CommonErrorCode` **已有**工作流码段（2-07-xx）：`WORKFLOW_DEFINITION_NOT_FOUND("20701")` / `WORKFLOW_INSTANCE_NOT_FOUND("20702")` / `WORKFLOW_TASK_NOT_FOUND("20703")` / `WORKFLOW_OPERATION_NOT_ALLOWED("20704")`。新增 `WorkflowErrorCode implements ErrorCode`（模块位 **07**），沿用 `JobErrorCode` 的写法（`exception()` / `exception(String)`）。

### 8.2 状态枚举自动成为前端字典

`ApprovalStatusEnum` 只要实现 `BaseEnum` 并加 `@AppEnum(code = "ApprovalStatus")`，`EnumDictService` 就会自动扫描，前端通过 `GET /app/enum?name=ApprovalStatus` 取到。需业务应用开启 `carlos.web.enum-dict.enabled: true` + `scan-packages: com.carlos`（**默认关闭**）。

### 8.3 ✅ 引擎端点收敛——已决策（2026-09-25）

**问题**：Warm-Flow 的 `WarmFlowController`（`/warm-flow/**`）返回自己的 `ApiResult<T>`（`{code:int, msg, data}`），**不是框架的 `Result<T>`**。

**先说一个无法回避的前提**：设计器 SPA **硬依赖 `/warm-flow/**` 的原生信封**，所以**这些端点必须保持引擎原样，不能改**。也就是说"系统里存在两套信封"这件事**消不掉**——能消掉的只是"业务代码会不会看到第二套"。

曾考虑过三个方案，均已否决：

| 被否决方案 | 否决理由 |
|---|---|
| 接受差异 + 前端加路径白名单 | **前端根本没有路径级机制**：`carlos-framework-ui/src/service/request/index.ts:59-63` 的成功判定是纯 `String(data.code) === VITE_SERVICE_SUCCESS_CODE`（`"00000"`），没有"某些路径跳过"的开关 |
| 自写 Controller 复制 15 个端点改用 `Result<T>` | 会**破坏设计器**（它按 `ApiResult` 解析），除非连那个 900KB 的 SPA 一起 fork |
| 通用重包代理（新前缀转发到 `/warm-flow/**` 再重包信封） | 只统一了外壳、**没统一数据模型**——前端拿到的仍是引擎 DTO；且多一层"为美观而存在"的代理要维护 |

**最终决策（A）**：**在 starter 内原生实现业务所需端点**，路径前缀 **`/api/workflow/**`**，返回 `Result<T>`。

关键点：**不是代理引擎的 HTTP 端点，而是进程内直接调用 Warm-Flow 的 Java 服务**（`TaskService`/`InsService`/`DefService`/`FormService`/`ChartService`）并返回我们自己的 DTO。好处是无额外跳转、不让引擎 DTO 形状漏到前端。

**端点收敛表——哪些封装、哪些不封**：

| 业务需要 | 引擎对应端点 | 我们的原生实现 |
|---|---|---|
| 审批页加载（任务详情 + 表单数据） | `execute/load/{taskId}`、`execute/hisLoad/{taskId}` | `GET /api/workflow/task/{taskId}`、`GET /api/workflow/his-task/{hisTaskId}` |
| 提交审批动作 | `execute/handle` | `POST /api/workflow/task/handle`（动作枚举分派） |
| 发起流程 | （无对应，引擎由业务侧调 `InsService`） | `POST /api/workflow/instance/start` |
| 流程图（含节点着色） | `query-flow-chart/{id}` | `GET /api/workflow/instance/{instanceId}/chart` |
| 流程定义列表 | `query-def` | `GET /api/workflow/definition/published` |
| 保存/发布定义 | `save-json` | `POST /api/workflow/definition` |
| **设计器内部**：`handler-type` / `handler-result` / `handler-feedback` / `handler-dict` / `node-ext` / `listener-list` / `published-form` / `form-content` | — | **不封装**，仅设计器使用 |

**实现纪律**：封装层必须**薄**——只做 DTO 转换与错误码包装，**不复刻引擎逻辑**，否则上游升级时会分叉。

**前端的连带结论**：既然业务代码不再直调引擎端点，前端**不需要**引擎专用的 axios 实例。此前一度新增的 `warmFlowRequest`（含 `App.Service.WarmFlowApiResult` 类型）**已删除**，`git diff` 已回到与 HEAD 逐字节一致。将来若真有"扩展需要直调引擎端点"，再加回来成本不变。

**前端保留的唯一改动是 token 续期修复**，见 §9。

---

## 九、设计器接入

- 静态资源：`warm-flow-plugin-ui-sb-web` 提供 `/warm-flow-ui/**` → `classpath:/META-INF/resources/warm-flow-ui/`，**另有 `classpath:/warm-flow-ui/` 作为覆盖位**（这是自定义/内网化的官方钩子）。
- 鉴权：`GET /warm-flow-ui/config` 返回 `{tokenNameList, framework}`，默认 `tokenNameList=["Authorization"]`。可配 `warm-flow.token-name`。
- **✅ 设计器 token 机制与续期修复（2026-09-25，已实施）**
  - **机制（从打包 bundle 反推，已核验）**：设计器有自己的 axios 实例，其请求拦截器**每个请求**都从 `localStorage` 读：
    `localStorage["Warm-TokenName"]` = header 名列表（本应用恒为 `Authorization`）；
    `localStorage["Warm-" + 名]`（即 `Warm-Authorization`）= header 值 `Bearer <accessToken>`。
    它自己**只在加载时从 URL query 参数**写这两个 key，**没有任何续期逻辑**。
  - **因此的后果**：access token 过期后设计器的保存/查询会**持续 401 且无提示**，用户**丢失编辑内容**；且它拿不到主应用的 `VITE_SERVICE_EXPIRED_TOKEN_CODES` 续期链路（`service/request/index.ts:110-121`）。
  - **修复方式（前端唯一保留的改动）**：主应用在**登录 / 续期 / 登出**时把那两个 key 同步过去——
    新增 `carlos-framework-ui/src/utils/warm-flow.ts`（`syncWarmFlowToken` / `clearWarmFlowToken`），
    收口到**单一写入点** `store/modules/auth/shared.ts` 的 `saveAuthStorage()`（登录与续期共用它；全仓库只剩一处 `localStg.set('token', …)`，新写入点不可能漏同步），
    登出清理挂在 `clearAuthStorage()`（← `resetStore()`，覆盖头像登出/不可恢复 401/路由守卫/MFA/OAuth2 回调），
    另在 auth store 创建时补一次**冷启动同步**（老会话 localStorage 里可能有 token 但还没有 `Warm-*` 副本）。
  - **设计器每次请求都重读** ⇒ **改值即可，无需重载 iframe、不丢编辑状态**。
  - **两个必须防的坑（已写进代码注释）**：不能用 `localStg`——① 它会加 `VITE_STORAGE_PREFIX` 前缀，设计器按裸 key 读永远读不到；② 它的 value 会 `JSON.stringify`，写进去是**带引号**的 `"Bearer xxx"`，而设计器把取到的字符串**原样**塞进 header，多一对引号就是非法 token。
  - **顺带的收益**：设计器原本要求把 token 作为 **URL query 参数**传入，那会经**浏览器历史、`Referer` 头、网关访问日志**泄露 token。改为主应用预置 localStorage 后，**iframe 可以完全不带 token 参数加载**。
  - **⚠️ 尚未实跑验证**：正确性建立在"key/值与反推的拦截器契约一致 + 写入无前缀无引号"上（两条已核验），但**没有跑起真实设计器验证它确实读到了**。需接设计器时实测一次。
- **`WarmFlowUiController` 与 `WarmFlowController` 共用 `warm-flow.ui` 开关**（同一 `@Import`）。关掉 UI 会连带关掉 REST 端点。
- **离线（信创）环境核验结论：设计器开箱可离线**。已逐条审计打包后的 JS/CSS：没有任何 CSS/JS 从 CDN 加载。唯一的第三方运行时调用是 form-create 的 AI 助手默认端点 `https://api.form-create.com/...`（**仅当用户打开 AI 面板**）和一张示例图默认值，可用上面那个覆盖位屏蔽。
- 集成方式：**先按 iframe 挂载 `/warm-flow-ui/index.html` 验证**（鉴权/样式/路由），确认不可接受再考虑移植源码到 `carlos-framework-ui`。

---

## 十、配置项

```yaml
carlos:
  workflow:
    enabled: true                      # 本 starter 开关
    # 引擎自带开关（由本 starter 透传/约定，不建议业务直接改）
    engine:
      enabled: true                    # → warm-flow.enabled
      ui-enabled: true                 # → warm-flow.ui
      token-name: Authorization        # → warm-flow.token-name
    timeout:
      enabled: true
      cron: "0 */5 * * * ?"            # 超时扫描周期
    notify:
      enabled: true
      todo-template-code: WF_TODO      # 消息中心模板编码（必填才能生效）
      timeout-template-code: WF_TIMEOUT
      result-template-code: WF_RESULT
    audit:
      enabled: true
```

> `warm-flow.permission-handler-path` / `global-listener-path` / `tenant-handler-path` / `data-fill-handler-path` **一律留空**（见 5.2）。

---

## 十一、实施计划

| 阶段 | 内容 | 产出 | 前置 |
|---|---|---|---|
| **P0** | **POC（必须先做）** | 见第十二节验收 | — |
| **P1** | 补 `carlos-org-api` Feign 端点（按 id 查/批量查、按 reportTo 查下属、按部门查用户、按角色查用户）+ `OrgUserPositionManager#listByReportTo` | org 侧可用 | 独立于工作流，可先行 |
| **P2** | starter 骨架：pom / BOM 治理 / AutoConfiguration / Properties / imports / package-info / README | 可启动 | P0 |
| **P3** | 7 张表 DDL（PG + 达梦）+ 与 MP/`db-type` 打通 | 可建表 | P0 |
| **P4** | `OrgPermissionHandler` + 表达式约定 + `carlos-org` 接入 | 办理人可解析 | P1、P3 |
| **P5** | 审批 API 封装（定义/实例/任务/待办）+ 错误码 + 枚举 | 业务可调用 | P4 |
| **P6** | 缝合：通知 / 审计 / 超时 / 多租户 | 闭环可用 | P5 |
| **P7** | 设计器接入 + `carlos-sample-workflow` 示例 + README/规范文档 | 端到端演示 | P6 |
| **P8** | 达梦全链路验证 + 依赖树与 Bean 冲突回归测试 | 可交付 | P7 |

**先行建议**：P1（org Feign 补齐）与 P0（POC）可并行，且 P1 有独立价值，不必等选型收尾。

---

## 十二、POC 验收清单（P0，限时）

> **⚠️ 验收口径说明（A1 决策，2026-09-24）**
>
> 仓库当前的 **`mvn clean install`（带测试）在 `carlos-auth-service` 是红的**，与工作流无关，且**已决策为「已知、暂不跟进」**。事实：
>
> - 失败 3 个测试类：`AppPortalSSOTest`（5 错误，`oauth2Properties is null` + `ApplicationContext failure threshold exceeded`）、`LoginControllerTest`（2 错误，同样 NPE）、`ConsentControllerTest`（1 失败，期望 `unnamed-client` 实得 `id`）
> - `carlos-auth/` 下**无任何未提交改动**；`AppPortalSSOTest` 的最后改动是 **6 天前**的一次 auth 重构（`refactor(auth): 重构认证服务的令牌配置和缓存`），`LoginControllerTest` / `ConsentControllerTest` 是 4 个月前
> - 因此**不能**把"`mvn clean install` 全绿"作为本次工作的验收标准
>
> **本方案的验收命令一律使用 `mvn install -Dmaven.test.skip=true`（全 reactor）**，加上工作流模块自身的测试 `mvn -o -pl carlos-integration/carlos-workflow/carlos-workflow-bus test`。若将来有人修好 auth 测试，再把带测试的全量构建纳入门禁。
>
> **⚠️ 2026-09-25 路径更新**：模块已从 `carlos-spring-boot/carlos-spring-boot-starter-workflow` 迁到
> `carlos-integration/carlos-workflow/carlos-workflow-bus`，上面的 `-pl` 路径按新位置书写；
> 原先「本模块 classpath 上没有 Servlet API，所以接口测试只能用反射」的限制**已消失**，
> `WorkflowControllerResultTest` 已升级为真实 MockMvc。

- [x] `mvn dependency:tree` 确认无 **mybatis-plus 3.5.12** 泄漏、无 **slf4j-api 1.7.36**、`JsonConvert` 只有一个提供者（Jackson 3）　→ **已实测**（前两项本就不存在，第三项已通过两处排除解决）
- [x] 在 **Spring Boot 4.0.6** 上启动成功，无 Bean 名冲突　→ **已实测**（H2 空库即可启动；实测 34 个 Warm-Flow Bean 名）
- [x] **建 7 张表（PostgreSQL）**　→ **已完成并验证**：建于 `carlos_framework.public`（与框架各服务 `currentSchema=public` 一致），7 表 / 总表数 71 / 128 列注释 / 9 索引，列数逐表与官方脚本一致（详见 4.5）
- [ ] 跑通：**导入定义 → 发起 → 待办 → 通过 → 会签 → 驳回 → 自由跳转 → 转办/委托 → 已办/历史 → 流程图渲染**　→ **未执行：缺流程定义 JSON + 进程内测试需 JDBC 凭据**
- [ ] 用桩实现 `PermissionHandler` 验证 `convertPermissions` 与 `checkAuth` 的交集语义符合预期（**这是最容易做错的地方**）
- [ ] 验证 `GlobalListener` 四个回调（start/assignment/finish/create）的触发时机与 `assignment` 改派生效
- [ ] 设计器 iframe 挂载 + 鉴权头透传 + 确认无外链请求（断言设计器加载期间零第三方向请求）
- [ ] 超时扫描：手工构造超时 `flow_task`，验证 `@ScheduleJob` 触发自动通过/升级上级
- [ ] 达梦：7 张表 DDL 转换 + `carlos.mybatis.db-type: dm` 下分页正确
- [ ] 顺序会签退化建模（串行多节点）评审，确认业务可接受
- [ ] 中文字体：容器镜像预置字体，确认流程图中文不出现方框

---

## 十三、风险与未决项

| # | 风险/未决 | 严重度 | 处置 |
|---|---|---|---|
| 1 | **`carlos-org` 缺 Feign 端点**，办理人解析与通知无法跨进程工作 | **高** | P1 前置补齐；已确认 `carlos-auth` 也受同一缺口困扰 |
| 2 | 依赖版本治理（MP 3.5.12 / slf4j 1.7.36 / 双 JsonConvert） | **高** | 已给出方案，POC 实测确认 |
| 3 | Bean 名冲突（`taskService` 等固定短名） | **高** | POC 冒烟测试 + README 保留名清单 |
| 4 | 达梦升级脚本需每次手工重做 | 中 | 写入维护手册 |
| 5 | `ApiResult` 与 `Result<T>` 响应信封不一致 | 中 | 建议选项 A，POC 定论 |
| 6 | **顺序会签尚未交付** | 中 | 退化建模（串行多节点）；写进建模规范 |
| 7 | 零 `#`/`$` 前缀词汇约定，下游易各自发明 | 中 | P4 固化并文档化 |
| 8 | 逻辑删除值被硬编码为 `"1"`，配置无效 | 低 | 文档说明，不依赖配置 |
| 9 | Warm-Flow Spring 基线仍是 2.7.18 | 中 | 靠框架 BOM 压制；升级 Warm-Flow 时需复测 |
| 10 | 消息模板必须先预置 | 中 | P6 与消息中心对齐模板设计 |
| 11 | `carlos-samples` 未纳入根 pom 构建 | 低 | 示例模块按现状放置即可 |

### 未验证项（诚实声明）

- 4.2 节的依赖传递版本是从**已发布 POM 推导**，未经 `dependency:tree` 实测。
- 未在 **Spring Boot 4.0.6** 上实际运行过 Warm-Flow（其 sb4 starter 对标 4.0.2）。
- 达梦 DDL 转换尚未实际执行。
- Bean 名冲突是**静态分析结论**（依据 `BeanConfig` 的固定 Bean 名），未在真实上下文复现。

---

## 附：关键事实与证据来源

- Warm-Flow 1.8.9（2026-07-13，Apache-2.0）Maven Central：https://repo1.maven.org/maven2/org/dromara/warm/
- sb4 starter POM（硬编码 `mybatis-plus.version=3.5.15`）：https://repo1.maven.org/maven2/org/dromara/warm/warm-flow-mybatis-plus-sb4-starter/1.8.9/
- 引擎 SQL（4 种方言，无达梦、无 jar 内）：https://github.com/dromara/warm-flow/tree/v1.8.9/sql
- 官方 FAQ（数据库支持口径）：https://www.warm-flow.com/master/other/troubleshooting.html
- 框架 DDL 惯例：`docs/db/README.md`
- 选型与决策依据：`docs/reports/workflow-engine-selection-research-2026-09-24.md`
