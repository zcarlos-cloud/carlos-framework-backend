# Carlos Framework 流程引擎选型调研报告

- **日期**：2026-09-24
- **适用范围**：carlos-framework-backend 及其下游业务系统
- **需求范围（2026-09-24 确认）**：主要用于集成**人工审批（OA 审批流）**；明确不需要系统间 Saga 编排；**目前不涉及子流程**
- **当前技术基线**：JDK 21 / Spring Boot 4.0.6 / Spring Cloud 2025.1.1 / MyBatis-Plus 3.5.16 / 达梦 DmJdbcDriver18 8.1.3.62
- **结论性质**：技术调研 + **选型决策已定（Warm-Flow 1.8.9）**，落地仍需 POC 验证

---

## 一、结论速览

### 1. 先说三个必须知道的事实

**事实 1：现有 `carlos-spring-boot-starter-flowable` 处于"孤儿模块"状态，且版本不被支持。**

- 它锁定 **Flowable 7.0.1**（`carlos-dependencies/pom.xml:82`），而 Flowable 7.0.1 是按 **Spring Boot 3.1.6** 构建的。
- 全仓库**没有任何生产代码 import `com.carlos.flowable`**，唯一引用方是 `carlos-sample-flowable`，而 `carlos-samples` 在根 pom 里是**被注释掉的**（`pom.xml:38`）。
- 模块 README 里写着 "Spring Boot: 3.5.9"，已经过期。
- 结论：**不存在"存量 Flowable 迁移成本"这件事，选型是真正开放的**，但也意味着这个模块正在成为技术债。

**事实 2：Flowable 7.0.1 在 Spring Boot 4 上是"未支持"状态，不是"必然崩溃"。**
对 `flowable-spring-boot-autoconfigure-7.0.1.jar` 做字节码核验后：它对已迁移的 Boot 3 类（`KafkaAutoConfiguration`/`HibernateJpaAutoConfiguration`/`SecurityAutoConfiguration`）的引用**只出现在 `@AutoConfigureAfter` 注解的字符串值里**，不是硬引用，因此不会 `NoClassDefFoundError`。但这意味着：

- 这些自动配置**排序提示被静默丢弃** → Bean 装配顺序变成未定义，可能出现间歇性启动/选择问题；
- 它依赖 Spring Boot 4 中已 `@Deprecated` 的旧 `spring.factories` 键，**未来 4.x 小版本随时可能移除**；
- 官方定位就是"Flowable 8 起才支持 Boot 4，且 8.x 不再支持 Boot 3"，7.x 无修复路径。

**事实 3：要在 Spring Boot 4.0.6 上用 BPMN，唯一可选项是 Flowable 8.0.0。**
Flowable **8.0.0（2026-02-27）** 是官方明确支持 Spring Boot 4 / Spring Framework 7 / Jackson 3 的首版，其 `flowable-spring-boot-autoconfigure` POM 已改为依赖 Boot 4 的模块化构件（`spring-boot-jdbc`/`spring-boot-kafka`/`spring-boot-jackson2` 等），是 Spring Boot 4 的硬证据。
**但由于本次需求不涉及子流程、也不需要 BPMN 标准互通，这条路线未被采用。**

### 2. 决策

> **✅ 选定：Warm-Flow 1.8.9（Apache-2.0，国产轻量审批流引擎）**
> **不采用：Flowable 8.0.0**（保留为"若将来必须支持 BPMN 标准互通"时的备选）

范围确认后，选型重心从**"引擎能力天花板"**转移到**"审批语义与设计器是否开箱可用"**。原因是人工审批场景的成本大头不在引擎本身，而在会签/加签/减签/驳回/自由跳转/转办/委托/表单/流程设计器这些**审批语义**上——

- **Warm-Flow 已把它们内置**；
- **Flowable 需要框架自己从零构建**（现有 `com.carlos.flowable.*` 封装是纯 API 透传，**零审批语义**）。

加权打分因此从原来的 7.05 : 7.05 平手，变为 **5.50 : 7.65**，倾向明确。决策依据与残留风险详见 6.2 / 6.3。

### 3. 接下来三件事

1. **处置 `starter-flowable` / Flowable 7.0.1**：既然选定 Warm-Flow，该模块应**标记弃用并从 `carlos-dependencies` 受管依赖中移出**（或直接删除模块与 `carlos-sample-flowable`），避免下游误引用一个在 Boot 4 上不受支持的引擎。它当前被技术债报告 #12 点名为 HIGH 风险（重写官方自动配置）。
2. **补 `carlos.mybatis.db-type: dm` 的配置说明**：框架 `MybatisProperties` 默认值是 `DbType.POSTGRE_SQL`，达梦环境下分页 SQL 会按 PostgreSQL 生成（详见第五节）。
3. **做一次限时 POC**（见第七节主路径），验证 Warm-Flow 在 4.0.6 上跑通审批全链路，再进入落地开发。

---

## 二、候选池与淘汰逻辑

按"能否嵌入框架自身微服务进程内 + 生产免费 + 支持 Spring Boot 4"三条硬性约束筛一遍：

| 候选 | 版本 / 状态 | 许可 | 可嵌入 | 生产免费 | Spring Boot 4 | 判定 |
|---|---|---|---|---|---|---|
| **Flowable 8.0.0** | 2026-02-27 | Apache-2.0 | ✅ | ✅ | ✅ 官方支持 | **进入决赛** |
| **Warm-Flow 1.8.9** | 2026-07-13 | Apache-2.0 | ✅ | ✅ 明确承诺无商业版 | ✅ 有 sb4 starter | **进入决赛** |
| Flowable 7.x（现状） | 7.0.1 / 7.2.0 | Apache-2.0 | ✅ | ✅ | ❌ 无修复路径 | 淘汰（必须升级） |
| Camunda 7 CE | 7.24.0，**2025-10 EOL**，仓库 2025-11 归档 | Apache-2.0 | ✅ | ✅ | ❌ 明确"不会修" | **淘汰：已死** |
| Camunda 7 EE | 7.24.x-ee | 商业 | ✅ | ❌ | ✅ | 淘汰：付费 |
| Camunda 8 (Zeebe) | 8.9.x | 8.6 起非 OSI | ❌ **架构上不可嵌入** | ❌ 生产需企业许可 | ✅ 客户端 starter | **淘汰：架构 + 许可** |
| Activiti 8/9 | 9.0.0 | Apache-2.0 | ✅ | ✅ | ⚠️ 未证实 | 淘汰：见下 |
| FlowLong | 1.2.7 | ⚠️ POM 写 AGPL-3.0 | ✅ | ⚠️ | ✅ | **淘汰：许可自相矛盾** |
| AntFlow | 2.1.0 | Apache-2.0 | ✅ | ✅ | ❌ javax + Boot 2.7 | 淘汰 |
| ccflow / JFlow | 活跃 | **GPL-3.0** | ⚠️ | ⚠️ | — | **淘汰：传染性许可** |
| jBPM / Kogito | KIE 10.2.0 | Apache-2.0 | ✅ | ✅ | ❌ 卡在 Boot 3.5.x | 淘汰 |
| Spring StateMachine | 4.0.2，**2026-07-05 归档** | Apache-2.0 | ✅ | ✅ | ❌ | 淘汰：已停维护 |
| Apache Camel | 4.21.0 | Apache-2.0 | ✅ | ✅ | ✅ | 淘汰：不是流程引擎 |
| Temporal | SDK 1.38.0 | MIT | ⚠️ 引擎在集群 | ✅ | ✅ | 淘汰：问题域不同 |
| Conductor OSS | 3.32.4 | Apache-2.0 | ❌ Boot 4 无法内嵌 | ✅ | ❌ | 淘汰 |

### 淘汰理由中值得记录的几条

- **Camunda 7 CE 是"陷阱式 EOL"**：社区版 2025-10 停更且**不再发安全补丁**，企业版却支持到 2030 年（可延至 2032）。Boot 4 支持只在 `7.24.3-ee+`，即"想上 Boot 4 就得买"。
- **Camunda 8 被架构直接排除**：Zeebe 引擎**永远**是远程集群，"嵌入模式不支持"。代价是不共享 ACID 事务、无引擎插件、变量仅限基础类型 + FEEL。8.6 起自托管生产也需付费许可（8.5 及之前是 Apache-2.0，这是一次许可收紧）。
- **Activiti 的隐患是分发**：Maven Central 上 `org.activiti:activiti-spring-boot-starter` 最新只到 **7.1.0.M6（2020-02-06）**，8.x/9.x 只发在 Alfresco Nexus。对一个要进内网 Nexus 的企业框架，这是供应链风险。另有 CVE-2026-3967（流程变量反序列化）。
- **FlowLong 的许可必须先澄清**：Gitee 元数据与官网说"默认 Apache-2.0"，但 **Maven POM 里 `flowlong-core`/`flowlong-spring-boot4-starter` 声明的是 AGPL-3.0**，官网授权页还有"不扩散原则"条目。AGPL-3.0 的网络传染性对闭源内部框架不可接受，**在拿到书面澄清前不要采用**。
- **Temporal 不是本题答案**：它解决的是"系统间长事务编排/补偿"，没有待办列表、办理人、会签、表单这些人工审批原语。人工环节要靠 Signal + `Workflow.await` 自己搭。若框架未来需要跨服务 Saga 编排，再单独评估它。

---

## 三、决赛选手逐项对比

| 维度 | Flowable 8.0.0 | Warm-Flow 1.8.9 |
|---|---|---|
| **许可** | Apache-2.0 | Apache-2.0，README 明确"过去、现在和未来都不会有商业版" |
| **发布时间** | 2026-02-27 | 2026-07-13 |
| **发版节奏** | 约 1 次/年，**Central 上无补丁版本** | 约月/季度级，含里程碑版 |
| **JDK 基线** | 编译基线 17，支持 17/21/25 | 兼容 8/17/21 |
| **Spring Boot 4** | ✅ 官方（Boot 4.0.2 / Framework 7.0.3 / Jackson 3） | ✅ `warm-flow-mybatis-plus-sb4-starter` |
| **流程定义格式** | **BPMN 2.0 XML**（标准） | **自定义 JSON**（v1.6.6 起移除 XML 导入导出） |
| **流程设计器** | 需自行集成 bpmn-js（Vue 有现成封装） | **内置 jar 包，含经典版 + 仿钉钉版**，静态页 iframe 即用 |
| **表数量** | 约 36–40 张（engine/history/idm/job/task/variable...），启用 CMMN/DMN 更多 | **7 张** |
| **ORM 耦合** | 引擎自带 MyBatis，与 MP 3.5.16 并行（需验证冲突） | **原生 MyBatis-Plus**，与框架强制规范一致 |
| **子流程** | ✅ 支持 | ❌ **未实现，排期 v2.0（原定 2026-06 未交付）** |
| **边界定时器 / 超时升级** | ✅ BPMN 原生 | ❌ 无一等支持，需自己做超时任务 |
| **DMN 决策表 / CMMN** | ✅ | ❌ |
| **会签 / 加签 / 或签 / 票签** | 需自行在 BPMN 上实现（用多实例等） | ✅ **一等公民**（顺序会签"开发中") |
| **驳回 / 自由跳转 / 转办 / 委托** | 需自行实现（框架现有封装已做了部分） | ✅ 内置 |
| **多租户** | ✅ | ✅ |
| **流程定义版本管理** | ✅ | ✅（复制时自增） |
| **达梦 DM8** | ❌ 官方不支持，需改 36 张表 + 处理 Liquibase | ❌ 官方脚本只含 MySQL/Oracle/PG/SQLServer，但**只有 7 张表需转换** |
| **社区规模** | GitHub 9,556 ★，412 open issues，周级提交 | Gitee 8,751 ★，GitHub 镜像仅 610 ★ |
| **生产验证** | 大量 | RuoYi-Vue-Plus 6.0.0（**Boot 4.1 + JDK 21 + warm-flow 1.8.9**，2026-07-30） |
| **已知安全问题** | 见下 | 未发现 |

### Flowable 8.0.0 的风险清单（必须知情）

1. **OSS 安全补丁滞后于商业版**：官方发行说明写明，Flowable Work/Design/Engage 使用的 Flowable 依赖是"C 端 Maven Central 尚未发布的 bugfix 版本"，付费客户才能从客户仓库取到（`8.0.0.6` … `8.0.0.22`）。即 **OSS 8.0.0 落后付费版约 7 个月的修复**。
2. **未修复的开放安全 issue**（2026-09 时点）：#4279 / #4280（REST 变量反序列化 RCE，CVSS 8.1 / 8.8）、#4272（CVSS 8.0）。
3. **CVE-2026-91197**：`ProcessDiagramLayoutFactory.parseXml()` XXE，影响 `flowable-engine <= 8.0.0`，CVSS 7.1，**截至本报告日期无已发布修复版本**。对要做安全评审的信创环境，这一条一定会被提出。
4. **Jackson 2 → Jackson 3 是真实迁移成本**：8.0.0 默认 Jackson 3（`tools.jackson.*`），BPMN 表达式/脚本里凡是碰 `com.fasterxml.jackson.databind.JsonNode` 的都要改。逃生开关：`flowable.variable-json-mapper=jackson2`。
5. **现有自定义配置会坏**：`FlowableAutoConfiguration` 直接 `new org.flowable.engine.impl.db.DbIdGenerator()`（内部类）并手写 `SpringProcessEngineConfiguration`。大版本升级时这些内部 API 最易变——这正是技术债报告 #12 已经点名的 HIGH 风险。
6. **容器要求提升**：8.0.0 需 Jakarta EE 11 / Servlet 6.1+，**最低 Tomcat 11**。

### Warm-Flow 1.8.9 的风险清单

1. **无 BPMN 标准**：JSON 模型，v1.6.6 起已移除 XML 导入导出 → **无法导入既有 BPMN 模型，也无法与外部工具互通**，锁定该引擎。
2. **子流程缺失**：官方路线图列为 v2.0.0，原定 2026-06 未交付。**很多企业审批场景（如"费用报销触发采购子流程"）会直接卡在这里。**
3. **顺序会签仍在"开发中"**。
4. **升级脚本只提供 MySQL 版**，其他库需自己从 MySQL 脚本转换（含达梦）。
5. **社区与持续性**：Gitee 8.7k ★ 但 GitHub 镜像仅 610 ★，主力维护者单一，bus factor 偏低。
6. **达梦是"能做"而非"已支持"**：官方 `sql/` 目录只有 mysql/oracle/postgresql/sqlserver 四个，**没有 `sql/dm`**；README 里"支持达梦/人大金仓/OceanBase"的说法来自旧版快照，现版本已移除。官方 FAQ 的口径是"自己挑最相近的脚本转换"。

---

## 四、现有 starter 代码资产的复用评估

现有 `carlos-spring-boot-starter-flowable` 提供了：

- `FlowableProcessService` / `FlowableRuntimeService` / `FlowableTaskService` / `FlowableHistoryService` / `FlowableService`（统一入口）
- `FlowableHealthIndicator`
- 12 个 DTO / VO / Param + 2 个枚举

**评估**：这些封装**没有业务逻辑，基本是 Flowable 原生 API 的透传**（组件分析报告也判定"利用较基础"）。因此：

- 若选 **Flowable 8.0.0**：这层封装可保留，但要改 `FlowableAutoConfiguration`（Jackson 3、内部 API、Boot 4 模块化依赖）。改动量中等但可控。
- 若选 **Warm-Flow**：这层封装**整体作废**，需按 Warm-Flow 的 `DefService`/`InsService`/`TaskService` API 重写。
- **无论哪条路，都建议放弃重写官方自动配置的做法**，改用官方 `flowable.*` / `warm-flow.*` 标准配置 + 仅保留薄封装层。这是技术债 #12 的选项 C，目前看是最优解。

---

## 五、达梦 / 信创影响（可能是一票否决项）

### 5.1 关键发现：框架的数据层已经是达梦就绪的

- MyBatis-Plus 的 `DbType` 枚举**原生包含 `DM("dm", "达梦数据库")`**，并且 `DbType.oracleSameType()` 对 `DM` 返回 `true`，`DialectFactory` 会走 `OracleDialect` → **分页开箱可用，无需自定义方言**。
- 还有 `DmKeyGenerator`（达梦自增/序列主键回填）。
- 达梦官方文档明确："mybatis-plus 3.0 之后的版本才支持 DM 数据库"。

**但有一个实际配置陷阱**：框架 `MybatisProperties` 的默认值是 `DbType.POSTGRE_SQL`，`MyBatisPlusConfig` 直接把它塞进 `PaginationInnerInterceptor`。**达梦环境必须显式设置 `carlos.mybatis.db-type: dm`**，否则分页 SQL 按 PostgreSQL 生成。建议补进 starter-mybatis 的 README。

### 5.2 两个引擎在达梦上的真实成本

| | Flowable 8.0.0 | Warm-Flow 1.8.9 |
|---|---|---|
| 官方支持 | ❌ 官方支持列表仅 h2/mysql/oracle/postgres/db2/mssql | ❌ 仅 4 种脚本 |
| 需移植的表 | **约 36 张**（engine 7 / history 5 / idm 9 / job 6 / ...） | **7 张** |
| Schema 管理干扰 | **Liquibase**：Flowable BPMN 引擎仍用 Liquibase 建表，Liquibase 4.x 探测 `DBMS_UTILITY.DB_VERSION` 在 DM 上失败，需影子类覆盖 `OracleDatabase` 或直接禁用 | 无（纯 DDL，自己执行） |
| 社区成熟方案 | 有：**`compatibleMode=oracle`** 连接参数即可让 Flowable 识别为 Oracle 并加载 Oracle DDL / `OracleDialect` | 无专门方案，官方 FAQ 说"自己转换" |
| 每次升级的负担 | **重**：每个 Flowable 大版本都要重新合并 DM 建表 + 升级脚本（Flowable 按方言提供 `upgradestep`） | 轻：7 张表的升级脚本自己从 MySQL 转 |
| 状态 | 官方不支持，社区可解，**属于长期维护负债** | 常规配置工作 |

**达梦适配的额外坑（两条都要注意）**：
- `compatibleMode=oracle` 会改变 `'' = NULL` 语义，可能影响流程变量与查询条件；
- 连接必须用 DM 驱动 ≥ 8.1.2.192（规避 Liquibase 探测问题）；
- **生产环境绝不要开 `database-schema-update=true`**（现有代码默认已是 `false`，这点是对的）。

**KingbaseES 提醒**：`com.dameng:DmJdbcDriver18` 在公共 Central 上可取，可预置进内网 Nexus；但**人大金仓的驱动不在公共 Maven**，需自行提供。若金仓未来是目标环境，要提前准备。

---

## 六、决策矩阵与建议

### 6.1 决策规则的执行结果

原决策规则的两个问题，现在 Q1 已有答案：

```
Q1. 需求范围是人工审批，还是系统间编排？
    └─ 【已确认】人工审批（OA 审批流），不需要 Saga 编排
       → 落到 Q2

Q2. 达梦等信创库是否为主要交付环境，且追求最小表结构 + 开箱即用设计器？
    ├─ 是 → 【Warm-Flow 1.8.9】
    └─ 否 → 仍需比较"超时/子流程"两项硬需求，见 6.3
```

补充规则仍然成立：**不要同时引入两套引擎。** 若未来真的出现跨服务编排需求，正确做法是 审批引擎管人工任务 + 编排引擎管系统调用，通过事件解耦，而不是让一个引擎干两件事。

### 6.2 打分对照（按"人工审批"范围重新加权）

| 评分维度（权重） | Flowable 8.0.0 | Warm-Flow 1.8.9 |
|---|---|---|
| **审批语义开箱程度（25%）**<br>会签/加签/减签/驳回/自由跳转/转办/委托 | **3** — 需自行实现；现有封装零审批语义 | **9** — 全部内置 |
| **流程设计器交付（20%）** | **3** — 需自建 bpmn-js 设计器 | **9** — jar 内置，含仿钉钉版 |
| Spring Boot 4 适配确定性（15%） | 9 — 官方支持 | 7 — starter 存在，未在 4.0.6 实测 |
| 与框架规范契合度（10%）— MyBatis-Plus | 6 — 引擎自带 MyBatis，与 MP 并行 | 9 — 原生 MyBatis-Plus |
| 达梦/信创适配成本（10%） | 4 — 36 张表 + Liquibase 改造 | 8 — 7 张表常规转换 |
| 超时 / 时限审批（10%）<br>3 天未审批自动通过、超时升级上级 | 9 — 边界定时器原生 | 5 — 无一等支持，需定时扫描自建（但 API 支持任意跳转/转办，工作量可控） |
| 子流程（5%） | 9 | 1 — 未实现，排期 v2.0 |
| 生态与人才可得性（5%） | 9 | 6 |
| **加权总分** | **5.50** | **7.65** |

> 与原版（7.05 : 7.05 平手）的差异，全部来自权重从"通用流程底座"改成"人工审批"：**Flowable 的加分项（BPMN 标准、子流程、DMN）在只做审批时变现有限，而它的减分项（无审批语义、无设计器）恰好是审批场景的刚需。**

### 6.3 决策结论与残留条件

**✅ 决策（2026-09-24）：选定 Warm-Flow 1.8.9。**

理由三条：

1. **人工审批的成本大头是审批语义 + 设计器，不是引擎**。Warm-Flow 把这两块都做完了；选 Flowable 相当于选完引擎再从零补这两块，等于重走 Warm-Flow 已经走过的路。
2. **与框架强制规范天然契合**：原生 MyBatis-Plus（框架规定 Manager 层必须 MP），7 张表，无 Liquibase 干扰。
3. **现有 `com.carlos.flowable.*` 封装无沉没成本**：它本来就是孤儿模块、纯透传、零审批语义，作废它几乎不损失什么。

原列的两个"翻盘条件"处置如下：

| 翻盘条件 | 状态 | 说明 |
|---|---|---|
| **子流程** | ✅ **已排除**（2026-09-24 确认"目前不涉及"） | 若未来出现，见下方"逃生方案" |
| **BPMN 2.0 标准互通** | ⚠️ 未提出需求，按不需要处理 | Warm-Flow 自定义 JSON 模型，v1.6.6 起已移除 XML 导入导出，**选定即锁定** |

**残留风险与逃生方案（不需重新选型）：**

- **子流程将来若出现**：引擎级子流程确实没有，但**业务侧可用"发起第二个流程实例 + 业务表关联父子关系"实现**，回写结果后再推进主流程。代价是跨流程的变量传递和异常回滚要自己管，但足以覆盖多数场景，**不构成重新选型的理由**。
- **顺序会签尚未交付**（官方标"开发中"）：并行会签/票签已可用。若需要"同一节点上 A→B→C 依次审批"，可退化为**串行多节点建模**——功能上等价，只是失去"单节点多审批人"的配置体验。这是当前唯一需要在建模规范里约定规避方式的功能缺口。
- **超时审批**：Warm-Flow 虽无一等定时器，但其 API 支持任意跳转/转办/自动完成，用框架已有的定时任务扫描超期待办即可实现，属可控工作量。这一项从"Flowable 优势"降级为"双方接近"，**不构成风险**。

---

## 七、建议的 POC 验证清单（限时，别做成大工程）

### 主路径：Warm-Flow 1.8.9（✅ 已选定）

- [ ] 引入 `warm-flow-mybatis-plus-sb4-starter`，在 BOM 里强制 MyBatis-Plus 为 **3.5.16**（starter 硬编码 3.5.15；`mybatis-plus-spring-boot4-starter:3.5.16` 已发布，可覆盖），确认 `mybatis-plus-extension` / `mybatis-plus-jsqlparser` 无冲突。
- [ ] 在 4.0.6（而非 starter 声明的 4.0.2）上实测启动。
- [ ] 跑通人工审批全链路：流程定义导入 → 发起 → 待办 → 审批通过 → **会签** → **驳回** → **自由跳转** → **转办/委托** → 已办/历史 → 流程图渲染。
- [ ] 验证设计器集成方式：iframe 挂载 `/warm-flow-ui/index.html` 是否满足 UI 侧（Vue 3 + Ant Design Vue）的鉴权/样式/路由要求，或是否需移植源码。
- [ ] 补"超时审批"实现：用定时任务扫描 `flow_task` 超期待办 → 调 Warm-Flow API 自动通过/转办上级，验证可行性。
- [ ] 验证**顺序会签**的退化建模（串行多节点）是否可接受——子流程已确认不需要，这是当前唯一的功能缺口。
- [ ] 检查设计器 jar 内的静态资源是否有外链 CDN（内网/信创环境可能访问不到）。
- [ ] 达梦专项：把 7 张表的 DDL 从 Oracle/MySQL 脚本转换到达梦并跑通，含 `carlos.mybatis.db-type: dm`。

### 备路径：Flowable 8.0.0（仅在将来必须支持 BPMN 标准互通时重新评估）

- [ ] 把 `carlos-dependencies` 的 `flowable.version` 从 `7.0.1` 升到 `8.0.0`，确认依赖树干净（重点看 Jackson 2/3 双版本共存、MyBatis 版本冲突）。
- [ ] 改造 `FlowableAutoConfiguration`：去掉对内部类 `org.flowable.engine.impl.db.DbIdGenerator` 的直接 `new`，改用官方配置项；确认 `HealthIndicator` 在 Boot 4 下的包路径；确认 `resources/META-INF/spring/...AutoConfiguration.imports` 无重复注册。
- [ ] 在 **PostgreSQL** 上先跑通 `carlos-sample-flowable` 全链路：部署 → 启动 → 待办 → 签收 → 完成 → 驳回 → 历史查询 → 流程图。
- [ ] 评估审批语义层工作量：在 BPMN 上实现会签（多实例）、驳回（回退到指定节点）、自由跳转，需自建多少代码——**这是选它之前必须先算清楚的账**。
- [ ] 评估 bpmn-js 设计器的前端工作量（或直接参考 RuoYi-Flowable-Plus 的集成方式，注意该项目 2024-09 后已停更）。
- [ ] BPMN 表达式里是否存在 Jackson 2 的 `JsonNode` 用法；若存在，评估 `flowable.variable-json-mapper=jackson2` 或改写。
- [ ] 达梦专项：`compatibleMode=oracle` 连接下执行建表脚本；处理 Liquibase 兼容（禁用或影子类）；用一条分页+一条复杂查询验证 Oracle 方言语义差异。
- [ ] 验证 `ProcessDiagramLayoutFactory` 的 XXE（CVE-2026-91197）是否可配置缓解。

### 共同项

- [ ] 中文字体：两个引擎画流程图都会用到中文字体，容器/信创镜像里需预置字体（`/usr/share/fonts`），否则流程图中文变方框。

---

## 八、待澄清 / 未验证事项（诚实清单）

**需要用户确认的**：
1. ~~流程引擎的业务定位~~ — **已确认（2026-09-24）：人工审批，不需要 Saga 编排。**
2. ~~是否需要"子流程"~~ — **已确认（2026-09-24）：目前不涉及。** 逃生方案见 6.3。
3. **顺序会签的建模约定**——Warm-Flow 的顺序会签尚未交付，需在建模规范里约定用"串行多节点"退化实现。这是当前唯一的功能缺口。
4. **是否长期需要 BPMN 2.0 标准互通**（导入既有模型、与外部工具交换）——Warm-Flow 非 BPMN，选定即锁定。
5. 是否存在**信创（达梦/金仓）硬性要求**，以及是否为验收项。
6. 是否已有前端资源投入流程设计器——若无，Warm-Flow 的 jar 内置设计器优势进一步放大。

**本报告未验证的部分**：
- Flowable **8.0.0** 的 jar 内部（本报告对 7.0.1 做了字节码核验，"8.0.0 无 dm 方言"依据的是官方文档 + 第三方 2026-09 源码分析，未逐一核验 jar）。
- Warm-Flow 在 **Spring Boot 4.0.6**（而非其声明的 4.0.2）上的实际运行情况——只确认了 sb4 starter 已发布。
- Warm-Flow 设计器静态资源在完全断网环境下的表现。
- **Operaton**（Camunda 7 CE 的社区 fork，路线图显示 2.1 版对齐 Spring Boot 4.0.6）与 **CIB seven** 的 Maven 坐标未能在 Central 上核实，故未纳入正式对比。若未来倾向"保留 Camunda 7 嵌入式模型"这条路线，值得单独调研。
- Camunda 8 的具体报价（无官方公开价格，网上流传数字来自第三方聚合站）。
- Activiti 8.8/9.0 对 Spring Boot 4 的支持（Java 25 基线已确认，Boot 4 为推断）。

---

## 九、关键证据来源

**版本与兼容性**
- Flowable 8.0.0 发布说明（Spring Boot 4 / Spring 7 / Jackson 3，不再支持 Boot 3）：https://github.com/flowable/flowable-engine/releases
- Flowable Spring Boot 文档页：https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot
- Flowable 7.2.0 + Spring Boot 4 报错与 PR #4133（Boot 4 模块化引发）：https://forum.flowable.org/t/12478
- Flowable 8.0.0 enterprise 发行说明（OSS bugfix 滞后说明；Jakarta EE 11/Tomcat 11）：https://documentation.flowable.com/latest/admin/release-notes/2025.2.01-release
- Maven Central 元数据（Flowable 最新 8.0.0 / 2026-02-27）：https://repo1.maven.org/maven2/org/flowable/flowable-spring-boot-starter/maven-metadata.xml
- Warm-Flow Maven Central（1.8.9 / 2026-07-13，含 sb4 starter 与 plugin-ui）：https://repo1.maven.org/maven2/org/dromara/warm/warm-flow/maven-metadata.xml
- Warm-Flow 官网与常见问题（数据库支持口径、达梦需自行转换）：https://www.warm-flow.com/master/other/troubleshooting.html
- Warm-Flow 升级指南（仅提供 MySQL 升级脚本）：https://www.warm-flow.com/master/other/upgrade_guide.html
- Activiti 在 Central 上的最后版本（7.1.0.M6 / 2020-02-06）：https://repo1.maven.org/maven2/org/activiti/activiti-spring-boot-starter/maven-metadata.xml

**生命周期**
- Camunda 7 CE EOL 与企业版延期公告：https://camunda.com/blog/2025/02/camunda-7-enterprise-end-of-life-extension/
- Camunda 7 + Spring Boot 4（官方答复"社区版没修复"）：https://forum.camunda.io/t/camunda-7-and-spring-boot-4/67071
- Camunda 8 许可（8.6 起自托管生产需企业许可）：https://docs.camunda.io/docs/reference/licenses/ 与 https://camunda.com/pricing/
- Camunda 7 vs 8 嵌入性差异：https://unsupported.docs.camunda.io/8.1/docs/guides/migrating-from-camunda-7/conceptual-differences/
- Spring StateMachine 归档：https://github.com/spring-attic/spring-statemachine
- Temporal Spring Boot 集成（支持 Boot 2/3/4）：https://docs.temporal.io/develop/java/integrations/spring-boot-integration

**达梦 / 信创**
- 达梦技术社区：Flowable 适配 DM8（`compatibleMode=oracle` 解决 database type 识别；Liquibase 需处理）：https://eco.dameng.com/community/post/20230201152327LJL72HJMUSPHLBM3BC
- 有生云 Flowable 达梦适配文档：https://docs.youshengyun.com/flowable/backend/database-support/Dameng
- 达梦官方 MyBatis-Plus 文档（MP 3.0+ 支持 DM）：https://eco.dameng.com/document/dm/zh-cn/app-dev/java-MyBatis-Plus-frame.html
- Flowable 官方系统要求（支持的数据库清单）：https://documentation.flowable.com/latest/admin/installs/system-requirements

**安全**
- CVE-2026-91197（Flowable ≤ 8.0.0 XXE）：https://www.vulncheck.com/advisories/flowable-flowable-engine-through-8.0.0-xxe-via-processdiagramlayoutfactory
- CVE-2026-3967（Activiti 反序列化）：https://app.opencve.io/cve/?vendor=alfresco&product=activiti

**本地证据**
- `carlos-dependencies/pom.xml:29`（Spring Boot 4.0.6）、`:40`（MyBatis-Plus 3.5.16）、`:82`（Flowable 7.0.1）、`:589`（DmJdbcDriver18）
- `carlos-spring-boot/carlos-spring-boot-starter-flowable/.../FlowableAutoConfiguration.java:76`（`new DbIdGenerator()`）
- `carlos-spring-boot/carlos-spring-boot-starter-flowable/README.md:19`（声明 Spring Boot 3.5.9，已过期）
- `pom.xml:38`（`carlos-samples` 被注释）
- `docs/reports/carlos-spring-boot-tech-debt-analysis.md:183`（技术债 #12：FlowableAutoConfiguration 重写官方配置，HIGH）
- `docs/reports/component-analysis-report-2026-09-11-1.md:652`（Flowable 7.0.1 利用度评估：基础）
