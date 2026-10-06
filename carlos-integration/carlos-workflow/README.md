# carlos-workflow 工作流审批中心

基于 [Warm-Flow](https://www.warm-flow.com/) 1.8.9 的**集中式人工审批中心**，为 Carlos Framework 提供会签、加签、驳回、自由跳转、转办、委托、超时等审批能力。

> **当前进度：POC 骨架 + 审批 API（含待办/已办分页、全部审批动作、实例级动作）+ 办理人解析。**
> 已交付：引擎装配、两套命名空间的接口——前端管理端点 `/workflow/**`（经网关）与
> Feign 契约 `/api/workflow/**`（服务间，不经网关），AO / Param / 枚举 / 错误码齐备、
> 单体与微服务两个启动入口、办理人表达式解析
> （`OrgPermissionHandler` + SpEL 助手 `@workflowAssignees`，词汇见 `docs/design/workflow-assignee-expression-spec.md`）、
> 办理人配置错误时的 **fail-fast 拦截**（`WorkflowAssigneeFailFastListener`，见「已知限制」）、
> **待办 / 已办分页**（Manager 层用 MyBatis-Plus 条件构造器实现，不写 SQL）、**全部 9 个审批动作**
> （含退回上一节点与拿回）、**实例级动作**（撤回 / 终止 / 挂起 / 激活）、**审批记录与可选跳转节点**、
> **「我发起的」列表与按实例拿回**、**流程定义的导入 / 导出 / 挂起 / 激活**（导出走 `@NoWrap`，
> 直接下载 JSON 文件而非 `Result` 信封）、
> **写操作事务**（`@Transactional(rollbackFor = Exception.class)`）、十九组验证用例。
> 尚未交付：超时调度、通知/审计集成、业务回调契约、设计器侧的办理人选择 SPI。

## 模块结构

```
carlos-integration/carlos-workflow/
├── carlos-workflow-api/      # Feign 接口定义 + AO/Param + 降级工厂（服务间契约）
├── carlos-workflow-bus/      # 服务端：controller（前端 /workflow/**）/ apiimpl（Feign /api/workflow/**）/ Service / convert / handler（办理人解析） / 引擎装配
├── carlos-workflow-boot/     # 单体启动入口（端口 9518）
├── carlos-workflow-cloud/    # 微服务启动入口（Nacos）
└── docs/sql/                 # 建表脚本（手工执行，不自动应用）
```

**定位**：审批中心是一个**独立部署的中央服务**，而不是各业务应用内嵌的库。

| | 集中式审批中心（本模块，选定） | 嵌入式 Starter（原方案，已废弃） |
|---|---|---|
| 部署 | 一个审批服务 + 一套 `flow_*` 表 | 每个业务应用各带引擎与 7 张表 |
| 引擎风险 | 依赖冲突 / Bean 名冲突 / DDL 归口在**一处** | 每个宿主应用各撞一次 |
| 待办中心 | 天然集中，跨业务统一待办 | 每个应用各有一份待办，无法汇总 |
| 审批与业务事务 | 跨进程，需要回调契约（**未定**，见设计文档 §2.1） | 同一事务 |

## 功能特性

- **集中式引擎**：引擎在一个 JVM 里运行，`flow_*` 表集中在一处，依赖冲突、Bean 名冲突与 DDL 归口都只此一份
- **唯一 JSON 实现**：排除了 `warm-flow-plugin-json-v1`（内含 Snack/Snack4/Jackson2/FastJson/Gson 五个 SPI 实现），classpath 上只保留 `warm-flow-plugin-json-jackson3`，避免 `ServiceLoader` 竞争导致 JSON 实现不确定
- **自带设计器**：`warm-flow-plugin-ui-sb-web` 提供的静态资源与接口随依赖引入，设计器入口挂载在 `/warm-flow-ui/**`
- **两套端点命名空间**：前端管理接口在 `/workflow/**`（`com.carlos.workflow.controller` 的普通 `@RestController`，经网关转发），Feign 契约在 `/api/workflow/**`（`apiimpl` 的 `@FeignClient` 实现，**网关不路由 `/api/**`**）——两套路径不重叠，管理接口不会经内部命名空间对外暴露
- **待办 / 已办中心**：集中式审批中心的待办与已办分页（按实例去重的已办口径），姓名批量解析走组织机构中心，不在本地保存用户主数据
- **「我发起的」列表**：`flow_instance.create_by` 等于当前登录用户的实例分页（`flowName` 模糊取自定义表、`flowStatus` / `businessId` 等值、创建时间倒序），发起人由服务端从登录上下文取，不接受前端传入；配 `POST /workflow/instance/take-back` 按实例拿回（引擎 `TaskService#taskBackByInsId`）
- **流程定义导入 / 导出 / 挂起 / 激活**：导入走上传 JSON 文件（引擎 `DefService#importIs`），导出走引擎 `DefService#exportJson` 并**直接下载文件**（`Content-Disposition: attachment; filename="<flowCode>_<version>.json"`）；挂起 / 激活对应引擎的 `DefService#unActive` / `#active`
- **写操作有事务**：`handle`、`start`、撤回/终止/拿回/挂起/激活、定义的保存/发布/撤销/复制/删除/导入/挂起/激活都带 `@Transactional(rollbackFor = Exception.class)`（引擎自身没有事务）
- **开关**：`carlos.workflow.enabled` 控制 Carlos 侧扩展 Bean 是否装配（缺省即生效）
- **分页可用**：已引入 `carlos-spring-boot-starter-mybatis`，Warm-Flow 自带的分页接口（`UserService#page` / `FormService#publishedPage`）拿到框架的 `PaginationInnerInterceptor`，方言由 `carlos.mybatis.db-type` 决定
- **国产数据库就绪**：达梦（DM）驱动 `com.dameng:DmJdbcDriver18` 随 `carlos-spring-boot-starter-mybatis` 传递引入（runtime 作用域）
- **不依赖 Redis**：已排除 `carlos-spring-boot-starter-translation`，避免其连带引入 Redisson；`starter-mybatis` 传入的 `starter-snowflake` 对 `starter-redis` 是 `provided` 作用域（不传递），其 Redis 侧缓存管理器是 `@ConditionalOnClass` 条件装配，本模块实测无任何 Redis/Redisson Bean
- **不依赖 Jackson 2**：**未**引入 `carlos-spring-boot-starter-openapi`——它经 knife4j → springdoc → swagger-core 传递引入 `com.fasterxml.jackson.core:jackson-databind`（Jackson 2）

## 版本信息

| 组件          | 版本                    |
|-------------|-----------------------|
| JDK         | 21                    |
| Spring Boot | 4.0.6                 |
| Warm-Flow   | 1.8.9                 |
| MyBatis-Plus | 3.5.16                |
| Jackson     | 3.x（`tools.jackson`）  |
| 框架版本        | 3.0.0-SNAPSHOT        |
| 端口          | 9518（网关 9510 / 认证 9511 / 组织 9512 / 系统 9513 / 审计 9514 / 消息 9515 / 任务 9516 / AI 助手 9517） |

## 快速开始

### 启动服务

```bash
# 单体模式（内置配置，读环境变量）
cd carlos-integration/carlos-workflow
mvn -pl carlos-workflow-boot spring-boot:run

# 或打包后运行
java -jar carlos-workflow-boot/target/carlos-workflow.jar
```

先按「数据库」章节手工执行两份建表脚本，并准备环境变量：

```bash
DB_URL=localhost:5432/carlos_framework
DB_USERNAME=postgres
DB_PWD=<password>
```

### 服务间调用（Feign）

```xml
<dependency>
    <groupId>com.carlos.workflow</groupId>
    <artifactId>carlos-workflow-api</artifactId>
</dependency>
```

```java
@RequiredArgsConstructor
public class LeaveApplyService {

    private final ApiWorkflowInstance workflowInstance;

    public void submit(String businessId) {
        ApiWorkflowStartParam param = new ApiWorkflowStartParam();
        param.setFlowCode("leave");
        param.setBusinessId(businessId);
        Result<WorkflowInstanceAO> result = workflowInstance.start(param);
    }
}
```

> 依赖 `carlos-workflow-api` 会注册 3 个 Feign 客户端（contextId：`workflowTask` / `workflowInstance` / `workflowDefinition`）；
> 调用方需要在自己的 pom 里显式引入 `spring-cloud-starter-openfeign`（api 模块只依赖 `spring-cloud-openfeign-core`）。

### 最小配置

```yaml
spring:
  application:
    name: carlos-workflow
  datasource:
    # 数据源走框架的动态数据源栈（Druid + dynamic-datasource），主库固定叫 master；
    # 只写 spring.datasource.url 会被静默忽略（启动不报错，但没有可用数据源）。
    dynamic:
      primary: master
      datasource:
        master:
          driver-class-name: org.postgresql.Driver
          url: jdbc:postgresql://${DB_URL}?currentSchema=public&stringtype=unspecified
          username: ${DB_USERNAME}
          password: ${DB_PWD}

carlos:
  workflow:
    # 是否装配 Carlos 侧的服务层 Bean，缺省即生效
    enabled: true
    assignee:
      # 节点配了办理人却解析不到任何人时是否直接拒绝创建待办任务（fail-closed），缺省 true。
      # 置为 false 会降级为 WARN 日志——⚠️ 该节点审批将对所有人开放，仅用于存量流程的过渡期
      fail-fast: true
  mybatis:
    # 必须与真实数据库一致，框架默认 POSTGRE_SQL；达梦填 dm（详见「数据库」章节）
    db-type: POSTGRE_SQL

warm-flow:
  # 必须保持为空，见「注意事项」
  permission-handler-path:
  # 同样必须保持为空：该配置项走无参构造反射实例化，绕过 Spring 注入
  global-listener-path:
  # 建议关闭启动 banner
  banner: false
```

> `spring.application.name` 不是可选项：随 `starter-mybatis` 传入的 `carlos-spring-boot-starter-snowflake`
> 需要 `carlos.snowflake.tag`，缺省回落到 `spring.application.name`，两者都为空会在启动期直接失败。

### 自动配置

自动配置注册文件：`carlos-workflow-bus/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

| 类名                                                  | 说明                                                                                                                                       |
|-----------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `com.carlos.workflow.config.WorkflowAutoConfiguration` | Carlos 侧服务层 Bean 装配。条件：`carlos.workflow.enabled=true`（缺省亦生效）。引擎自身的 DAO/Service/`initFlow` 由 Warm-Flow 的 `BeanConfig` 装配，本类不重复注册 |

本类注册的 Bean：

| Bean 名                      | 类型                          | 说明                                                                       |
|----------------------------|-----------------------------|--------------------------------------------------------------------------|
| `workflowTaskService`      | `WorkflowTaskService`       | 待办任务服务（审批页加载 / 已办详情 / 统一审批动作），构造器注入引擎的 `taskService`、`hisTaskService`          |
| `workflowInstanceService`  | `WorkflowInstanceService`   | 流程实例服务（发起 / 流程图），构造器注入引擎的 `instanceService`、`definitionService`、`chartService` |
| `workflowDefinitionService` | `WorkflowDefinitionService` | 流程定义服务（已发布清单 / 保存发布），构造器注入引擎的 `definitionService`                         |
| `orgPermissionHandler`     | `OrgPermissionHandler`      | 办理人表达式解析（`PermissionHandler`）；条件：`@ConditionalOnMissingBean(PermissionHandler.class)` + org Feign 契约在位 |
| `workflowAssignees`        | `WorkflowAssignees`         | 动态办理人的 SpEL 助手（`#{@workflowAssignees.xxx}`）；**Bean 名来自方法名，改名即毁契约**；条件：`@ConditionalOnMissingBean` + `ApiOrgUserPosition` 在位 |
| `workflowAssigneeFailFastListener` | `WorkflowAssigneeFailFastListener` | 办理人失败即拒（`GlobalListener`）：节点配了办理人却解析不到任何人时，**在创建待办之前**抛 `50702`；条件：`@ConditionalOnMissingBean(GlobalListener.class)` |

**控制器不由本类注册**：六个实现类都是标准的 `@RestController`，由启动类的组件扫描装配——
前端管理端点在 `com.carlos.workflow.controller`（`/workflow/**`），服务间 Feign 端点在
`com.carlos.workflow.apiimpl`（`/api/workflow/**`）。
（旧 starter 时代因为宿主的扫描路径不含 `com.carlos.workflow`，只能以 `@Bean` 注册；现在模块内扫描即可，
若两种方式并存，同一路径会出现两个 MVC handler 并抛 `Ambiguous mapping`。）

由组件扫描（`@Component`）装配的 Bean：

| 类型                            | 说明                                                                       |
|-------------------------------|--------------------------------------------------------------------------|
| `WorkflowTaskManager`（`manager/impl/WorkflowTaskManagerImpl`） | 待办 / 已办分页与 `flow_user` 查询。继承 `BaseServiceImpl<FlowTaskMapper, FlowTask>`，额外注入 `FlowUserMapper` / `FlowHisTaskMapper` / `FlowDefinitionMapper` / `FlowInstanceMapper` 做批量补齐；本类不出现任何 SQL |
| `WorkflowOrgUserManager`（`manager/impl/WorkflowOrgUserManagerImpl`） | 组织机构中心（`carlos-org`）的唯一出口：姓名批量解析与候选办理人分页。`ApiOrgUser` 用 `ObjectProvider` 注入——它由宿主的 `@EnableFeignClients` 装配，本模块不能假定一定存在 |

> 三个 Service（`WorkflowTaskService` / `WorkflowInstanceService` / `WorkflowDefinitionService`）的构造器由
> `WorkflowAutoConfiguration` 给出并**保持不变**；它们新增的 Manager / `PermissionHandler` 依赖用
> `@Autowired(required = false)` 字段注入，缺失时相关端点抛 `50701`，而不是让上下文启动失败
> （针对自动配置的 `ApplicationContextRunner` 切片测试不扫描组件，必需注入会让那些上下文起不来）。

## 审批 API（两套命名空间）

统一返回框架的 `Result<T>`（`success` / `code` / `msg` / `data` / `timestamp` / `details`）。
设计器自身的 `/warm-flow/**` 与 `/warm-flow-ui/**` **保持引擎原样**，返回引擎的 `ApiResult`，不在本模块范围内。

**两套命名空间的分界**（别混）：

| | 前端管理接口（**经网关**） | Feign 契约（服务间，**不经网关**） |
|---|---|---|
| 路径 | `/workflow/**` | `/api/workflow/**` |
| 落点 | `com.carlos.workflow.controller` 的普通 `@RestController`（**不 implements 任何 Api 接口**） | `carlos-workflow-api` 的 `@FeignClient` 接口 + `com.carlos.workflow.apiimpl` 的 `@RestController implements ApiXxx` |
| 谁在调 | 浏览器 → 网关（`/manage-api` 等统一前缀由网关剥掉，业务路径原样透传） | 其他业务服务的 Feign 客户端 |
| 网关 | 路由 `Path=/workflow/**` | **不路由 `/api/**`** |

> 网关的 `SelectStripPrefix` 只对 swagger/doc 路径生效，业务路径原样透传：前端调
> `/manage-api/workflow/task/todo/page`，网关剥掉统一前缀后，服务端收到的就是 `/workflow/task/todo/page`。

### 前端管理接口（`/workflow/**`，经网关）

| 方法   | 路径                                       | 入参                             | 返回 `Result<...>` 的 `data`     |
|------|------------------------------------------|--------------------------------|-------------------------------|
| GET  | `/workflow/task/todo/page`               | `ApiWorkflowTaskPageParam`（query：`current` / `size` / `nodeName` / `flowStatus`） | `Paging<WorkflowTaskAO>` |
| GET  | `/workflow/task/done/page`               | `ApiWorkflowTaskPageParam`（同上） | `Paging<WorkflowHisTaskAO>`   |
| GET  | `/workflow/task/{taskId}/candidates`     | `ApiWorkflowCandidatePageParam`（query：`action` / `deptId` / `current` / `size`） | `Paging<WorkflowCandidateAO>` |
| POST | `/workflow/task/handle`                  | `ApiWorkflowHandleParam`       | `WorkflowHandleResultAO`      |
| GET  | `/workflow/instance/{instanceId}/his-task` | —                         | `List<WorkflowHisTaskAO>`     |
| GET  | `/workflow/instance/{instanceId}/any-node` | —                         | `List<WorkflowNodeAO>`        |
| POST | `/workflow/instance/revoke`              | `ApiWorkflowInstanceActionParam` | `WorkflowInstanceAO`        |
| POST | `/workflow/instance/terminate`           | `ApiWorkflowInstanceActionParam` | `WorkflowInstanceAO`        |
| POST | `/workflow/instance/take-back`           | `ApiWorkflowInstanceActionParam` | `WorkflowInstanceAO`（按实例拿回） |
| GET  | `/workflow/instance/page`                | `ApiWorkflowInstancePageParam`（query：`current` / `size` / `flowName` / `flowStatus` / `businessId`；**只返回当前用户发起的**） | `Paging<WorkflowInstanceAO>` |
| POST | `/workflow/instance/{instanceId}/suspend` | —                        | `Boolean`                     |
| POST | `/workflow/instance/{instanceId}/activate` | —                       | `Boolean`                     |
| POST | `/workflow/definition`                   | `ApiWorkflowDefinitionSaveParam` | `WorkflowDefinitionAO`      |
| POST | `/workflow/definition/basic`             | `ApiWorkflowDefinitionBasicParam`（`definitionId` 为空=新建、非空=更新；`flowCode` / `flowName` / `modelValue` / `category`） | `WorkflowDefinitionAO` |
| POST | `/workflow/definition/import`            | multipart 字段名 `file`（`.json`） | `WorkflowDefinitionAO`      |
| GET  | `/workflow/definition/{id}/export`       | —                              | **文件（非 `Result`）**：引擎 JSON + `Content-Disposition: attachment; filename="<flowCode>_<version>.json"` |
| POST | `/workflow/definition/{id}/activate`     | —                              | `Boolean`                    |
| POST | `/workflow/definition/{id}/suspend`      | —                              | `Boolean`                    |
| GET  | `/workflow/definition/page`              | `ApiWorkflowDefinitionPageParam`（query：`current` / `size` / `flowName` / `flowCode` / `category` / `isPublish`） | `Paging<WorkflowDefinitionAO>` |
| GET  | `/workflow/definition/{id}`              | —                              | `WorkflowDefinitionAO`        |
| POST | `/workflow/definition/publish`           | `ApiWorkflowDefinitionIdsParam` | `Boolean`                    |
| POST | `/workflow/definition/unpublish`         | `ApiWorkflowDefinitionIdsParam` | `Boolean`                    |
| POST | `/workflow/definition/copy`              | `ApiWorkflowDefinitionCopyParam` | `Long`（新定义 id）          |
| DELETE | `/workflow/definition`                 | `ApiWorkflowDefinitionIdsParam` | `Boolean`                    |

> ⚠️ **导出端点是本模块唯一的「不走 `Result` 信封」的业务端点**，靠方法上的 `@NoWrap`
> （`com.carlos.boot.web.annotation.NoWrap`）实现。`ResponseWrapAdvice#beforeBodyWrite` 对**任何**
> 非 `Result` 的返回值都会 `return Result.success(body)`，漏掉该注解时 `ResponseEntity<byte[]>` 会被
> 包成 `{"success":true,"code":"00000",...,"data":"..."}`——HTTP 200、文件名也对，只有下载下来的文件打不开。
> `WorkflowDefinitionExportEndpointTest` 会把「顶层没有 `code` / `success`」钉死，去掉注解该用例必红。
> **不要**改成在 `ResponseWrapAdvice` 里加路径排除：那是框架级配置，会影响所有模块。

### 服务间 Feign 契约（`/api/workflow/**`，网关不路由）

只保留「别的业务服务发起/查询流程」真正需要的 5 个端点：

| 方法   | 路径                                       | 入参                             | 返回 `Result<...>` 的 `data`     | Feign 接口 / contextId |
|------|------------------------------------------|--------------------------------|-------------------------------|----------------------|
| GET  | `/api/workflow/definition/published`     | —                              | `List<WorkflowDefinitionAO>`  | `ApiWorkflowDefinition` / `workflowDefinition` |
| POST | `/api/workflow/instance/start`           | `ApiWorkflowStartParam`        | `WorkflowInstanceAO`          | `ApiWorkflowInstance` / `workflowInstance` |
| GET  | `/api/workflow/instance/{instanceId}/chart` | —                           | `WorkflowChartAO`             | 同上 |
| GET  | `/api/workflow/task/{taskId}`            | —                              | `WorkflowTaskAO`              | `ApiWorkflowTask` / `workflowTask` |
| GET  | `/api/workflow/his-task/{hisTaskId}`     | —                              | `WorkflowHisTaskAO`           | 同上 |

> 管理端接口（分页、发布、审批动作、实例级动作……）**只在前端命名空间**。
> `WorkflowNamespaceBoundaryTest` 会断言两侧端点集合交集为空、前端控制器的类级映射不带 `/api` 前缀、
> 且前端控制器不 implements 任何 `com.carlos.workflow.api` 接口——把这条边界钉死。

分层：`controller` / `apiimpl` → `Workflow*Service`（返回 DTO）→ `WorkflowDtoConvert`（引擎对象 → DTO）→ `WorkflowAoConvert`（MapStruct，DTO → AO）。
引擎类型不会漏到 Controller 之外；Controller 只做「收参数 → 调 Service → 包 `Result`」，不带业务逻辑，也不加 `@Transactional`（事务边界在 Service 层）。

底层引擎调用（路径按所在命名空间标注）：

| 路径 | 底层引擎调用 |
|---|---|
| `/api/workflow/task/{taskId}` | `TaskService#getById` + `TaskService#load` |
| `/api/workflow/his-task/{hisTaskId}` | `HisTaskService#getById` + `TaskService#hisLoad` |
| `/workflow/task/todo/page` | 无引擎调用：`WorkflowTaskManager` 以 `flow_task` 分页（`node_type=1` + 命中当前用户的任务 id 集合），再按 id 集合批量补齐 `flow_definition.flow_name`、`flow_instance.business_id/activity_status` 与 `flow_user` 上的办理人 |
| `/workflow/task/done/page` | 无引擎调用：先取 `SELECT MAX(id) … GROUP BY instance_id` 的历史任务 id（MyBatis-Plus 的 `select("MAX(id) AS id")` + `groupBy`，不是手写 SQL），再按 id 集合分页 |
| `/workflow/task/{taskId}/candidates` | 无引擎调用：`flow_user` 取任务现有办理人，其余数据来自 `carlos-org` 的 `ApiOrgUser#pageCandidates` |
| `/workflow/task/handle` | 见下「审批动作分派」 |
| `/api/workflow/instance/start` | `DefService#getPublishByFlowCode` + `InsService#start` |
| `/api/workflow/instance/{instanceId}/chart` | `InsService#getById` 上引擎维护的 `defJson` + `ChartService#getChartRgb` |
| `/workflow/instance/{instanceId}/his-task` | `HisTaskService#orderById().desc().list(entity)`（引擎链式查询，不分页、无自定义 SQL） |
| `/workflow/instance/{instanceId}/any-node` | `NodeService#previousNodeList` / `#suffixNodeList` / `#getStartNode` + `HisTaskService#getByInsId`，见「可选跳转节点」 |
| `/workflow/instance/revoke` | `TaskService#revoke`（引擎校验 `handler` 必须是实例发起人） |
| `/workflow/instance/terminate` | `TaskService#terminationByInsId` |
| `/workflow/instance/{instanceId}/suspend`、`/activate` | `InsService#unActive` / `#active` |
| `/workflow/instance/page` | 无引擎调用：`WorkflowInstanceManager` 以引擎实体 `FlowInstance` 做 MyBatis-Plus 条件查询（`create_by` 等于当前用户、`flowStatus` / `businessId` 等值、`create_time` 倒序），`flowName` 先经 `flow_definition` 取定义 id 再 `IN` 过滤，名称也由定义表批量补齐 |
| `/workflow/instance/take-back` | `TaskService#taskBackByInsId`（`handler` / `permissionFlag` 取自当前登录用户） |
| `/api/workflow/definition/published` | `DefService#list`（`isPublish=1`） |
| `/workflow/definition/page`、`/workflow/definition/{id}` | 无引擎调用：`WorkflowDefinitionManager` 以引擎实体 `FlowDefinition` 做 MyBatis-Plus 条件查询（`flowName` `like`，其余等值，`create_time` 倒序） |
| `/workflow/definition/publish`、`/workflow/definition/unpublish` | `DefService#publish` / `DefService#unPublish`（逐条，批量入参只是循环；引擎拒绝时翻译成 20704） |
| `/workflow/definition/copy` | `DefService#copyDef`；引擎只回布尔值，新定义 id 由「同一 flowCode 的 id 差集」定位 |
| `/workflow/definition`（DELETE） | `DefService#removeDef`（连同节点与跳转线；已产生流程实例时引擎拒绝，翻译成 20704） |
| 以上查不到定义时 | 统一 `20701 流程定义不存在` |
| `/workflow/definition` | 新增走 `DefService#importJson`，更新走 `DefService#saveDef`，可选 `DefService#publish`；**保存前校验每个节点的办理人表达式（10705）**，见「已知限制」 |
| `/workflow/definition/basic` | `definitionId` 为空走 `DefService#checkAndSave`（补 version 后只写主记录，**不创建节点**），非空走 `DefService#updateById`（只改 `flowCode` / `flowName` / `category`；`modelValue` 与库中不一致直接以 10701 拒绝） |
| `/workflow/definition/import` | `DefService#importIs(InputStream)`（上传的 JSON 先经引擎自己的 `JsonConvert` 判一次「是不是 JSON」） |
| `/workflow/definition/{id}/export` | `DefService#exportJson`，正文原样下载（**不经 `Result` 包装**，见上表下方说明） |
| `/workflow/definition/{id}/activate`、`/suspend` | `DefService#active` / `#unActive` |

### 审批动作分派（`POST /workflow/task/handle`）

`ApiWorkflowHandleParam.action` 取 `WorkflowActionEnum`，服务层按它分派，**不实现引擎自身的流转语义**：

| 动作                   | 引擎调用                                                        |
|----------------------|-------------------------------------------------------------|
| `PASS`               | `TaskService#skip`（`skipType=PASS`，带 `nodeCode` 即「任意通过」）      |
| `REJECT`             | `TaskService#skip`（`skipType=REJECT`，带 `nodeCode` 即「任意驳回」）     |
| `FREE_JUMP`          | `TaskService#skip`（`skipType=PASS` + 必填 `nodeCode`）            |
| `REJECT_LAST`        | `TaskService#rejectLast`（引擎按历史任务的 `target_node_code` 回溯到上一节点） |
| `TAKE_BACK`          | `TaskService#taskBack`（拿回「我上一次办理的任务」；引擎要求 `handler` 与那条历史任务的审批人一致，因此该动作强制使用当前登录用户，忽略入参里的 `handler`） |
| `TRANSFER`           | `TaskService#transfer`（`addHandlers=targetHandlers`）          |
| `DEPUTE`             | `TaskService#depute`（`addHandlers=targetHandlers`）            |
| `ADD_SIGNATURE`      | `TaskService#addSignature`（`addHandlers=targetHandlers`）      |
| `REDUCTION_SIGNATURE` | `TaskService#reductionSignature`（`reductionHandlers=targetHandlers`） |

`handler`（当前办理人唯一标识）由调用方显式传入；缺省时由引擎回落到 Spring 容器里的 `PermissionHandler#getHandler()`。
`permissionFlag`（当前用户的办理人标识集合）由服务层显式写入 `FlowParams`，取 `PermissionHandler#permissions()`；
`flowStatus` / `hisStatus` 不对外开放（`ApiWorkflowHandleParam` 里没有这两个字段），走引擎默认值——
例如撤回写 `6 撤销`、拿回写 `11 拿回`。

写入类动作都在服务层开启事务（`@Transactional(rollbackFor = Exception.class)`）：引擎一次流转会写
`flow_task` / `flow_his_task` / `flow_user` / `flow_instance`，而 Warm-Flow 自身的 Service 没有任何事务注解，
中途失败会留下「任务已转历史、待办未生成」这类脏数据。

### 可选跳转节点（`GET /workflow/instance/{instanceId}/any-node`）

自由跳转与「驳回到指定节点」共用一个候选清单。**参考实现（hh-vue）取的是「开始节点的全部后继」
（`nodeService.suffixNodeList(startNode.getId())`），那会把当前节点、以及已经走过且不可退回的节点一起列出来，
语义是错的**，本实现改用引擎 1.8.9 真正提供的能力：

候选集合 =（`NodeService#previousNodeList(definitionId, nowNodeCode)` ∩ 本实例走过的节点）
∪ `NodeService#suffixNodeList(definitionId, nowNodeCode)`，
再排除开始节点、当前节点与网关节点。其中「本实例走过的节点」取自 `HisTaskService#getByInsId` 的
`node_code` 与 `target_node_code`（后者可能是逗号分隔的多个节点）加上实例当前节点。

与「严格正确的可退回集合」的偏差（**有意保留**）：

1. 它是一份**并集**：后半段（当前节点的后继）只对自由跳转有意义；引擎不拦「任意退回到后置节点」
   （`anyNodeCode` 优先于 `skipType`），前端应只在自由跳转时允许选择后半段；
2. 对自由跳转而言它是**更严格的子集**：引擎实际允许 `anyNodeCode` 指向定义里任意非网关节点
   （包括与本节点没有直接路径关系的节点，例如另一条并行分支上的节点），本实现不会列出这些——
   宁可少给选项，也不给出引擎会拒绝的选项；
3. 开始节点被排除（引擎的 `FIRST_FORBID_BACK` 禁止退回开始节点）。

### 错误码

模块编码 **07**。`CommonErrorCode` 已占用 20701~20704（流程定义/流程实例/任务不存在、当前操作不允许），本模块不重复定义，
`WorkflowErrorCode` 只补充：

| 错误码     | 含义          | HTTP |
|---------|-------------|------|
| `10701` | 工作流请求参数无效   | 400  |
| `10702` | 不支持的审批动作    | 400  |
| `10703` | 流程定义 JSON 无效 | 400  |
| `10704` | 目标办理人不能为空   | 400  |
| `10705` | 流程定义的办理人表达式无效 | 400  |
| `50701` | 工作流引擎执行异常   | 500  |
| `50702` | 节点配置了办理人但未解析到任何人 | 500  |

引擎异常映射规则：数据存在性在调用**前**显式校验（缺失即 20701 / 20702 / 20703）；
调用过程中引擎抛出的 `FlowException` 统一映射为 **20704 当前操作不允许**（保留引擎原始消息）；
流程定义 JSON 解析/保存失败映射为 10703、办理人表达式非法映射为 10705；
`WorkflowAssigneeFailFastListener` 自己抛出的 `WorkflowException`（50702）不属于 `FlowException`，
不会被 20704 覆盖——运维看到的就是「某节点配了办理人却没人能办」，而不是一句含糊的「当前操作不允许」。

### 已知限制

- **办理人配错不会再静默失效，但拦截依赖 `assignment` 钩子。** `OrgPermissionHandler`（`@Bean` +
  `@ConditionalOnMissingBean`，见 `WorkflowAutoConfiguration`）把 `flow_node.permission_flag` 里的表达式
  展开成用户 id，`permissions()` 同步只返回当前用户 id——两侧词汇一致，交集判定才有意义（规范 §3 与 §5.3 方案 A）。
  展开结果为空时，引擎的 `checkAuth` 对「办理人为空」的任务整段短路（`AssertUtil#isTrue` 是「为 true 才抛」，
  而办理人为空让条件直接为 false），该节点等于**对所有人开放**且不出现在任何人的待办里。
  现在由 `WorkflowAssigneeFailFastListener`（`GlobalListener`，`@Bean + @ConditionalOnMissingBean(GlobalListener.class)`）
  在**创建待办之前**拦下：节点 `permission_flag` 非空、却一个办理人都解析不出来时直接抛 `WorkflowException`（`50702`），
  消息里带节点编码/名称与出错的表达式。⚠️ 拦截点是引擎的 `assignment` 事件而不是 `create`：
  `create` 在写库动作之后触发，而引擎的流转服务没有任何 `@Transactional`，那里抛异常拦不住已提交的流转。
  `carlos.workflow.assignee.fail-fast=false` 可降级为仅 WARN（见「最小配置」）。
- **保存期校验不是硬闸门。** `POST /workflow/definition` 会先把每个节点的 `permissionFlag` 过一遍词汇表，
  有问题直接以 `10705` 拒绝并列出节点编码与表达式；但**设计器 SPA 走的是引擎自带的 `POST /warm-flow/save-json`**，
  不经过本接口，因此定义可以绕过这一步到达引擎。运行期的 fail-fast 监听器才是真正的控制手段。
- **`POST /workflow/definition/basic` 只写流程定义主记录，不创建节点；`modelValue` 一经保存不可修改。**
  这是「先填基本信息、再进设计器画节点」的第一步：`definitionId` 为空即新建（`flowCode` / `flowName` 必填，
  `modelValue` 缺省 `CLASSICS`，只接受 `CLASSICS` / `MIMIC`），此时定义还没有流程图，发布会被引擎以 20704
  拒绝（预期行为，不是缺陷）；`definitionId` 非空即更新，只允许改 `flowCode` / `flowName` / `category`
  （前两者留空表示保持原值），**`modelValue` 与库中不一致时直接以 10701 拒绝**——经典模型与仿钉钉模型的
  节点结构不同，放开修改会静默毁掉已有节点的布局（引擎设计器自身也把该值当成只读）。
  节点与跳转线仍由设计器保存时经 `POST /workflow/definition`（`DefService#saveDef`）写入。
- **办理人解析依赖 org 的 Feign 客户端与请求上下文。** 宿主未开启 `@EnableFeignClients`
  （`carlos-spring-cloud-starter` 的 `FeignConfig` 已声明 `basePackages = "com.carlos"`）时
  `OrgPermissionHandler` 不注册，审批回到「配了办理人即 20704 全失败」的状态；
  另外后台任务/超时升级等无请求上下文的链路，租户无法经 `FeignRequestInterceptor` 透传，
  会静默回落到平台租户，需改用 `...AndTenant` 变体。
  （fail-fast 监听器不调 org，因此不受这一条影响。）
- **流程图接口不做二次渲染加工**：直接返回引擎在流转时写入流程实例的 `defJson`，转成本模块的 `WorkflowChartAO`
  （含 `nodes[].status` 与 `chartStatusColor` 三原色），同时保留原文 `sourceJson` 供需要对接设计器渲染器的场景使用。
- **待办 / 已办分页的口径**（`/workflow/task/todo/page`、`/workflow/task/done/page`）：
  - 待办只查 `flow_task.node_type = 1`（中间节点）**且** `flow_user.processed_by` 命中当前用户标识的任务，
    按任务去重（一个任务有多条 `flow_user` 时只出现一次）；`flowStatus` 过滤的是**任务自身**的 `flow_status`；
  - 已办只查 `flow_his_task.approver = 当前用户`，且**每个流程实例只保留最近办理的那一条**
    （`MAX(id)` + `GROUP BY instance_id`）；`flow_his_task.flow_status` 记的是「那一次审批的结论」
    （`2 审批通过` / `9 已退回`），不是实例当前状态，过滤与展示都用它；
  - 列表里的姓名（待办的计划审批人 / 转办人 / 委托人，已办与审批记录的审批人 / 协作人）由
    `WorkflowOrgUserManager` 调用 `carlos-org` 的 `ApiOrgUser#listUsersByIds` **一次批量解析**（不是逐行回查）；
    org 不可用时降级为原始用户标识（而不是留空）；审批记录里的审批人与协作人**替换为姓名**。
- **候选办理人来自 `carlos-org`**：`/workflow/task/{taskId}/candidates` 不查 `flow_user` 也不抄一份 `sys_user` 查询，
  而是调 `carlos-org` 新增的 `ApiOrgUser#pageCandidates`（分页、按 `deptId` 含子树过滤、支持 `includeIds` / `excludeIds`，
  部门名由服务端一次补齐）。减签只列该任务现有办理人，加签 / 转办 / 委托排除这些人。
  ⚠️ **org 不可用时候选清单直接报错（30001），而不是返回空列表**——空列表会让使用者以为流程没人可选。
- **「我发起的」的口径**（`/workflow/instance/page`）：只返回 `flow_instance.create_by` 等于当前登录用户的实例，
  发起人由服务端从 `PermissionHandler#getHandler()` 取，**不接受前端传参**（否则就是一个越权查看他人申请的入口）。
  `flow_instance` 上**没有** `flow_name` 列，因此 `flowName` 模糊过滤是「先按名称取定义 id，再用
  `definition_id IN (...)` 过滤实例」，名称本身也按定义 id 批量补齐（一次查询，不按行回查）。
- **导入 / 导出 / 挂起 / 激活的口径**：导入的文件内容先用引擎自己的 `JsonConvert` 判一次「是不是 JSON」，
  不是 JSON 或文件为空一律 `10701`（**不触碰引擎**），能解析但引擎认为结构不合格（缺 `flowCode`、没有节点等）
  才是 `10703 流程定义 JSON 无效`——前端据此区分「传错文件」与「文件内容有问题」。
  导出前先确认定义存在（引擎的 `exportJson` 在定义不存在时抛的是 `NullPointerException`，
  翻译不出可读错误码），文件名取 `<flowCode>_<version>.json`。定义的挂起 / 激活对应引擎的
  `DefService#unActive` / `#active`，重复挂起或重复激活由引擎拒绝并翻译成 `20704`。
- **`/api/workflow/definition/published` 不带排序**：引擎没有「查询已发布定义列表」的专用方法，本实现以
  `isPublish=1` 作为 MyBatis-Plus 查询条件直接取列表，顺序由数据库决定。
- **`carlos.mybatis.db-type` 必须与实际数据库一致**：框架默认 `POSTGRE_SQL`，达梦（DM）必须显式改成 `dm`，
  否则分页 SQL 按 PostgreSQL 语法拼、引擎自带分页接口报错。详见「数据库」章节。
- **达梦（DM）驱动已随 `starter-mybatis` 引入**（`com.dameng:DmJdbcDriver18`，runtime 作用域），
  但**达梦版建表脚本与 Liquibase 变更集尚未提供**（`docs/sql/` 下只有 PostgreSQL 版），信创部署仍需先转表结构。
- **业务回调契约未定**：审批结果如何通知发起方（Feign 回调 / MQ 事件 / 轮询 / Seata AT）尚未决策。

## 保留 Bean 名清单

Warm-Flow 的 `BeanConfig`（`org.dromara.warm.plugin.modes.sb.config.BeanConfig`）使用**固定短 Bean 名**注册引擎 Bean。
宿主应用在 `allow-bean-definition-overriding=false`（Spring Boot 默认值）下定义同名 Bean，会导致启动直接失败并抛 `BeanDefinitionOverrideException`。

**在集中式部署下这个风险已被收口**：本模块是唯一持有引擎的进程，业务应用不再撞这些名字。
以下清单仍须遵守——**不得在 `carlos-workflow-bus` 内新增同名 Bean**。

清单为 1.8.9 在测试上下文中**运行时实际观测**到的 Bean 名（`WorkflowContextStartupTest.reportWarmFlowRegisteredBeanNames`）。

### 固定短名（高风险，禁止复用）

| Bean 名               | 说明                          |
|---------------------|-----------------------------|
| `definitionDao`     | 流程定义 DAO                    |
| `definitionService` | 流程定义服务（`DefService`）         |
| `nodeDao`           | 流程节点 DAO                    |
| `nodeService`       | 流程节点服务                      |
| `skipDao`           | 跳转线 DAO                     |
| `skipService`       | 跳转线服务                       |
| `instanceDao`       | 流程实例 DAO                    |
| `instanceService`   | 流程实例服务（`InsService`，注意不是 `insService`） |
| `taskDao`           | 待办任务 DAO                    |
| `taskService`       | 待办任务服务                      |
| `hisTaskDao`        | 历史任务 DAO                    |
| `hisTaskService`    | 历史任务服务                      |
| `flowUserDao`       | 流程用户 DAO                    |
| `flowUserService`   | 流程用户服务                      |
| `formDao`           | 表单 DAO（`flow_form` 表，见「数据库」） |
| `flowFormService`   | 表单服务                        |
| `chartService`      | 流程图服务                       |
| `initFlow`          | 引擎初始化入口，加载完成即打印 `【warm-flow】，加载完成` |
| `warmFlowSpringUtil` | Warm-Flow 的 `SpringUtil`，静态持有 `ApplicationContext` |

### 派生名（由 Spring 或 MapperScan 推导，短名不固定）

| Bean 名                                                          | 说明                                            |
|----------------------------------------------------------------|-----------------------------------------------|
| `warm-flow-org.dromara.warm.plugin.modes.sb.config.WarmFlowProperties` | `@ConfigurationProperties("warm-flow")` 绑定对象 |
| `org.dromara.warm.plugin.modes.sb.helper.SpelHelper`           | SpEL 表达式助手（`@Import` 注册，Bean 名为全限定名）           |
| `org.dromara.warm.flow.spring.boot.config.FlowAutoConfig`      | 引擎自动配置类本身                                     |
| `org.dromara.warm.flow.ui.config.WarmFlowUiConfig`             | 设计器 MVC 配置                                     |
| `org.dromara.warm.flow.ui.controller.WarmFlowUiController`     | 设计器配置接口                                       |
| `org.dromara.warm.flow.ui.controller.WarmFlowController`       | 设计器数据接口                                       |
| `warmMapper`                                                   | MyBatis-Plus 通用 Mapper 基类                     |
| `flowDefinitionMapper` / `flowNodeMapper` / `flowSkipMapper` / `flowInstanceMapper` / `flowTaskMapper` / `flowHisTaskMapper` / `flowUserMapper` / `flowFormMapper` | `@MapperScan("org.dromara.warm.flow.orm.mapper")` 生成的 8 个 Mapper |

### Feign contextId 占用

本模块占用 3 个，均不与既有模块重复（仓库内已核验无重复）：

| contextId            | 接口                  |
|----------------------|---------------------|
| `workflowTask`       | `ApiWorkflowTask`   |
| `workflowInstance`   | `ApiWorkflowInstance` |
| `workflowDefinition` | `ApiWorkflowDefinition` |

`WorkflowFeignContractTest` 会断言这三个 contextId 互不重复，断言 Feign 的 `path` + 方法映射与
`apiimpl` 的 `@RequestMapping` 逐条一致（不一致会导致服务间调用 404 且无编译期提示），
并断言 `apiimpl` 上没有契约接口之外的端点。
`WorkflowNamespaceBoundaryTest` 另外钉住与前端命名空间的边界（见「审批 API（两套命名空间）」）。

## 数据库

引擎使用 **8 张表**（官方 7 张 + 本项目补建 1 张）：

| 表名                | 说明       | 来源 |
|-------------------|----------|----|
| `flow_definition` | 流程定义表    | 官方 |
| `flow_node`       | 流程节点表    | 官方 |
| `flow_skip`       | 节点跳转关联表  | 官方 |
| `flow_instance`   | 流程实例表    | 官方 |
| `flow_task`       | 待办任务表    | 官方 |
| `flow_his_task`   | 历史任务记录表  | 官方 |
| `flow_user`       | 流程用户表    | 官方 |
| `flow_form`       | 流程表单表    | **补建**（官方 7 表脚本不含，见下） |

### DDL 不在 jar 内，必须手工执行

两个脚本，**都要执行**：

| 脚本 | 内容 | 来源 |
|---|---|---|
| `docs/sql/workflow_postgresql.sql` | 官方 7 张表 + 注释 + 2 个索引 | Warm-Flow v1.8.9 官方脚本 |
| `docs/sql/workflow_form_postgresql.sql` | `flow_form` 一张表 + 注释 | **本项目补建**（官方缺失） |

**这两个文件只存在于源码仓库，不会打进任何 jar**，框架也不会在启动时自动建表。上线前需手工把脚本导入目标库或导入对应的 Liquibase 变更集。

> **索引提示（「我发起的」列表）**：`GET /workflow/instance/page` 按
> `flow_instance.create_by` 过滤、按 `create_time` 倒序，而官方 7 表脚本只给 `flow_user` 建了两个索引，
> `flow_instance` 上没有任何索引。数据量大时建议自行补一个
> `CREATE INDEX instance_create_by_time_idx ON flow_instance USING btree (create_by, create_time DESC);`。
> 本模块**没有**改官方脚本（`docs/sql/` 保持与上游一致），该索引属于部署侧决策。

> **⚠️ 为什么 `flow_form` 要单独补建，而且列数比官方多**：
> 官方 7 表脚本**不包含** `flow_form`；该表 DDL 只在仓库的 `sql/mysql/v1-upgrade/warm-flow_form.sql`，头部标着 `-- TODO form 开发中`，**没有 PostgreSQL / 达梦版本**。
> 更麻烦的是：**官方那份 MySQL 脚本只有 13 列，缺 `create_by` / `update_by`**，而 1.8.9 实体
> `org.dromara.warm.flow.orm.entity.FlowForm` 明确映射了 `createBy` / `updateBy` 两个字段
> （MyBatis-Plus 会解析为 `create_by` / `update_by`）。**照抄官方脚本建表，`flow_form` 的 INSERT/SELECT 依然会失败**
> （`column "create_by" does not exist`）。
> 因此本项目的 `workflow_form_postgresql.sql` **按实体列集合生成，共 15 列**，并已在真实 PostgreSQL 上验证。
> 该表上游仍标注 TODO、结构未定型，**每次升级 Warm-Flow 都需复检列集合**。

### `carlos.mybatis.db-type` 必须与实际数据库一致

MyBatis-Plus 的分页方言由框架的 `carlos.mybatis.db-type` 决定（`MybatisProperties#dbType`，**框架默认值 `POSTGRE_SQL`**）。使用达梦、MySQL 等其他数据库时必须显式覆盖，否则分页 SQL 会拼错：

```yaml
carlos:
  mybatis:
    db-type: dm     # 达梦；PostgreSQL 保持 POSTGRE_SQL
```

- **boot（单体）**：`carlos-workflow-boot/src/main/resources/application.yml` 写死 `POSTGRE_SQL`，达梦部署把它改成 `dm`
  （同一文件里另有注释提示）。该值经 `MybatisProperties` → `PaginationInnerInterceptor` 生效。
- **cloud（微服务）**：同一配置项写成 `${CARLOS_MYBATIS_DB_TYPE:POSTGRE_SQL}`。**不要写死**：
  `spring.config.import` 场景下本地文档优先级高于 Nacos 导入源，写死会把 Nacos 下发的 `dm` 覆盖回 `POSTGRE_SQL`
  （与 `carlos-job-cloud` 文档里记的 `carlos.job.access-token` 是同一个坑）。

驱动：PostgreSQL 与达梦驱动都已随 `carlos-spring-boot-starter-mybatis` 到位（达梦为 `com.dameng:DmJdbcDriver18`，
runtime 作用域）。达梦连接串形如 `jdbc:dm://<host>:<port>/<schema>`，`driver-class-name` 用 `dm.jdbc.driver.DmDriver`。

> 尚未完成的部分：**达梦版建表脚本**（`docs/sql/` 下只有 PostgreSQL 版）、
> 以及**在真实达梦库上跑通分页**——本仓库没有可达的达梦实例，方言配置只在测试里断言到「配置被读到」这一层。

### 达梦（DM）等国产数据库需要转换

4 种官方脚本中 PostgreSQL 版与达梦最接近，但仍需转换。转换要点：

| PostgreSQL 写法                  | 达梦写法                                      |
|-------------------------------|-------------------------------------------|
| `int8` / `int2`               | `BIGINT` / `SMALLINT`                     |
| `bpchar(1)`                   | `CHAR(1)`                                 |
| `text`                        | `TEXT` / `CLOB`                           |
| `timestamp`                   | `TIMESTAMP`                               |
| `DEFAULT 'N':: character varying` | `DEFAULT 'N'`（PG 专有类型转换语法）              |
| `"version"` / `"type"`        | 字段名是保留字，达梦同样需要引号或改名                       |
| `CREATE INDEX ... USING btree` | 去掉 `USING btree`                          |
| `COMMENT ON TABLE/COLUMN`     | 达梦语法不同，需改写或删除（注释不影响运行）                    |

## 测试

```bash
# 仅构建并运行本模块测试（不要加 clean）
mvn -o -pl carlos-integration/carlos-workflow/carlos-workflow-bus test
```

| 测试类                                     | 依赖       | 说明                                                                 |
|-----------------------------------------|----------|--------------------------------------------------------------------|
| `com.carlos.workflow.JsonConvertSpiTest`     | 无        | 校验 `JsonConvert` 的 SPI 描述文件在 classpath 上只剩一个，唯一提供者是 Jackson 3，且提供者可被 `Class.forName` 加载 |
| `com.carlos.workflow.poc.WorkflowContextStartupTest` | 内存 H2    | 以 MOCK Web 环境启动完整上下文，校验 Warm-Flow 固定 Bean 名全部就位、无 Bean 名冲突、三个服务与**两套共六个控制器**（组件扫描：`apiimpl` 的服务间端点 + `controller` 的前端管理端点）就位、**宿主提供 `PermissionHandler` 时生产实现退让**、测试桩被引擎取到、**fail-fast `GlobalListener` 注册为 Bean 且 `FlowEngine.globalListener()` 真的取到了它**；另校验 **MyBatis 接入面**：数据源是框架的 `DynamicRoutingDataSource` 且 master 真能连上配置的 H2、`PaginationInnerInterceptor` 已注册且方言等于 `carlos.mybatis.db-type`（测试故意配成 `dm`）、拦截器真的进了 `SqlSessionFactory`、达梦与 PostgreSQL 驱动在 classpath 上，并断言上下文里**没有任何** Redis/Redisson Bean |
| `com.carlos.workflow.handler.OrgPermissionHandlerTest` | 无（org 契约 mock） | 逐条覆盖规范 §3 词汇：`user:`（含逗号分隔）/ `role:` / `dept:` / `deptLeader:`（含向上 N 级与链断）/ `roleInDept:`，以及 `@@` 并集去重、未知类型与非法参数、空展开、禁用用户过滤、org 异常只丢一条表达式，和 `permissions()` 与 `convertPermissions()` 的词汇一致性 |
| `com.carlos.workflow.handler.WorkflowAssigneeVocabularyTest` | 无 | 词汇表（展开与校验共用的唯一实现）：`@@` 拆分、类型识别、`KNOWN`/`UNKNOWN_TYPE`/`LITERAL` 三态、参数合法性（含 `deptLeader` 层级上限）、保存期非法表达式的挑取 |
| `com.carlos.workflow.handler.WorkflowAssigneeFailFastListenerTest` | 无（节点/任务 mock） | fail-fast 行为：非空 `permission_flag` + 空解析 → 抛 50702（消息带节点与表达式）、仅剩未求值 SpEL → 同样抛、空 `permission_flag` → 放行、结束节点/无待创建任务 → 放行、`fail-fast=false` → 降级为 WARN；另用引擎自己的 `ListenerUtil#executeAssignment` 验证**异常不会被引擎吞掉** |
| `com.carlos.workflow.handler.WorkflowAssigneeValidatorTest` | 无 | 保存期校验：接受规范 §3 全量写法 + SpEL + 裸用户 id；拒绝未知类型/非法参数/裸字符串，错误消息列出全部出错节点与表达式 |
| `com.carlos.workflow.handler.WorkflowAssigneesTest` | 无（org 契约 mock） | SpEL 助手：`upLeader` 在缺反向端点时返回空列表且不抛异常，`subordinates` 按 `report_to` 取直接下级并过滤禁用用户 |
| `com.carlos.workflow.config.WorkflowAssigneeWiringTest` | 无（`ApplicationContextRunner`） | org 契约在位时注册 `OrgPermissionHandler` 与 Bean 名为 `workflowAssignees` 的 SpEL 助手；宿主自带 `PermissionHandler` 时退让；org 契约缺席时不注册；**fail-fast 全局监听器注册为 Bean（宿主自带 `GlobalListener` 时退让）** |
| `com.carlos.workflow.api.WorkflowControllerResultTest` | 无（服务层 mock） | **真实 MockMvc**（`standaloneSetup`，六个控制器一起装配，走 DispatcherServlet 派发）：服务间 `/api/workflow/**` 的 5 个端点与前端 `/workflow/definition`、`/workflow/definition/basic`、`/workflow/task/handle` 的路径 / HTTP 方法 / 参数绑定 / `Result` 信封，以及 Servlet 与 `carlos.workflow.enabled` 两个条件注解 |
| `com.carlos.workflow.api.WorkflowFeignContractTest` | 无        | 校验 3 个 Feign 接口的 contextId 唯一、服务名正确、`path` + 方法映射与 `apiimpl` 的 `@RequestMapping` 逐条一致，且实现类上没有契约之外的端点 |
| `com.carlos.workflow.controller.WorkflowNamespaceBoundaryTest` | 无 | **命名空间边界守护**：`com.carlos.workflow.controller` 下的类不 implements `com.carlos.workflow.api` 包的接口；前端控制器的类级 `@RequestMapping` 全部以 `/workflow/` 开头且不带 `/api` 前缀；`apiimpl` 三个实现类全部以 `/api/workflow` 开头；两侧「HTTP 方法 + 路径」集合交集为空（剥离 `/api` 前缀后再比一次），且 Feign 侧只剩约定的 5 个端点 |
| `com.carlos.workflow.api.WorkflowApprovalEndpointTest` | 无（服务层 mock） | **真实 MockMvc**：前端命名空间（`/workflow/**`）的待办/已办分页、候选办理人、审批记录、可选跳转节点与四个实例级动作的路径、HTTP 方法、参数绑定与 `Result` 信封（含 `/workflow/task/todo/page` 与 `/workflow/task/{taskId}/candidates` 不会互相抢映射） |
| `com.carlos.workflow.controller.WorkflowInstancePageEndpointTest` | 无（服务层 mock） | **真实 MockMvc**：`GET /workflow/instance/page` 的五个 query 过滤条件绑定与 `Paging` 信封（含 `createBy`）、`POST /workflow/instance/take-back` 的请求体绑定与 `WorkflowInstanceAO` 信封 |
| `com.carlos.workflow.endpoint.WorkflowDefinitionExportEndpointTest` | 无（服务层 mock + 真实 `ResponseWrapAdvice`） | **导出端点的响应包装排除**：`GET /workflow/definition/{id}/export` 的响应顶层**没有** `code` / `success` / `timestamp`，正文与引擎 JSON **逐字节相等**，`Content-Disposition` 为 `attachment; filename="leave_3.json"`；同一次 MockMvc 里的探针端点仍被包成信封（证明 advice 真的在生效，不是「压根没有包装器」而假通过），并直接断言 `ResponseWrapAdvice#supports` 只对带 `@NoWrap` 的导出方法返回 false。⚠️ 去掉 `@NoWrap` 该用例必红 |
| `com.carlos.workflow.manager.WorkflowTaskManagerTest` | 内存 H2 | 待办/已办分页的**真实 SQL**：权限过滤、同一任务多办理人时去重、`node_type=1` 过滤、`nodeName` 模糊 / `flowStatus` 等值、分页、`flowName` / `businessId` / `activityStatus` / 办理人补齐；已办「每个实例只留最近一条」、节点名与状态过滤、只查自己；`listTaskHandlerIds` |
| `com.carlos.workflow.manager.WorkflowInstanceManagerTest` | 内存 H2 | 「我发起的」分页的**真实 SQL**：`create_by` 只认当前用户、`flowStatus` / `businessId` 等值、`flowName` 模糊（先查定义 id 再 `IN` 过滤，命中不到定义时直接返回空分页）、`create_time` 倒序、分页边界、`flowName` 由定义表批量补齐、发起人未知时返回空分页 |
| `com.carlos.workflow.manager.WorkflowOrgUserManagerTest` | 无（org 契约 mock） | 姓名**一次批量**解析（不是逐行）、跳过非用户 id 的标识、昵称为空回退账号、org 不可用时姓名降级为空而候选清单抛 30001、`includeIds` 为空时不发起远程调用 |
| `com.carlos.workflow.service.WorkflowTaskServiceTest` | 无（引擎 Bean 全 mock） | `handle` 按 action 分派：`REJECT_LAST` → `rejectLast`、`TAKE_BACK` → `taskBack`（强制用当前登录用户）、`PASS` → `skip`、`TRANSFER` → `transfer`，以及新分页端点是否原样下沉给 Manager、候选办理人的 include/exclude 口径 |
| `com.carlos.workflow.service.WorkflowInstanceServiceTest` | 无（引擎 Bean 全 mock） | 审批记录（按 id 倒序 + 姓名一次批量解析 + 解析不到时保留原标识）、可选跳转节点（前置 ∩ 已走过 ∪ 后继，排除开始节点/当前节点/网关）、撤回 / 终止 / 挂起 / 激活的引擎调用与错误码翻译 |
| `com.carlos.workflow.service.WorkflowInstanceInitiatedServiceTest` | 无（引擎 Bean 全 mock） | 「我发起的」分页把**当前登录用户**（而非前端入参）交给 Manager、Manager 未装配时报 50701；按实例拿回的参数校验（10701）、实例不存在（20702，不触碰引擎）、`FlowParams` 组装（`handler` / `message` / `permissionFlag`）、引擎拒绝翻译成 20704、引擎返回空翻译成 20702 |
| `com.carlos.workflow.service.WorkflowDefinitionImportExportServiceTest` | 无（引擎 Bean 全 mock） | 导入的两级错误码：空文件 / 空白内容 / 非 JSON / JSON 但不是对象一律 10701 且**不触碰引擎**，引擎的结构校验失败才是 10703；正常导入把上传原文交给 `DefService#importIs` 并回查定义；导出先确认定义存在（20701）再取引擎 JSON，并带上 `flowCode` / `version` 供拼文件名；定义的激活 / 挂起：正常、定义不存在 20701、引擎拒绝与返回 false 均 20704 |
| `com.carlos.workflow.service.WorkflowDefinitionBasicServiceTest` | 无（引擎 Bean 全 mock） | `POST /workflow/definition/basic` 的两条分支：新建走 `DefService#checkAndSave`（`modelValue` 缺省=CLASSICS、显式 MIMIC、`flowCode` / `flowName` 空白与非法 `modelValue` 一律 10701 且不触碰引擎、入参不带 id 交由引擎补主键）；更新走 `DefService#updateById`（正常改名、留空表示保持原值、`category` 可清空、定义不存在 20701、`modelValue` 与库中不一致 10701）；引擎异常翻译成 50701 |
| `com.carlos.workflow.service.WorkflowTransactionalTest` | 内存 H2 | 反射断言写方法都带 `@Transactional(rollbackFor = Exception.class)`（含 `takeBack` / `importDefinition` / 定义的 `activate` / `suspend`）、只读方法（`myPage` / `exportDefinition`）与 Controller 不带事务注解，并断言上下文里**真的存在 `PlatformTransactionManager`**（否则注解只是装饰） |
| `com.carlos.workflow.service.WorkflowServiceErrorCodeTest` | 无（引擎 Bean 全 mock） | 校验服务层把引擎 `FlowException` 与缺失数据翻译成预期错误码（10701/10702/10703/10704/10705/20701/20702/20703/20704/50701），并校验动作分派与 `FlowParams` 组装；另覆盖定义保存的办理人校验（非法表达式 10705 且不触碰引擎，合法定义正常走到 `DefService#saveDef`） |
| `com.carlos.workflow.ddl.WorkflowPostgreSqlDdlTest` | 外部 PostgreSQL | 在专用 schema 中执行官方 DDL 并校验 7 张表全部建立，**默认自动跳过**。⚠️ **尚未覆盖 `workflow_form_postgresql.sql`**（补建的 `flow_form`）——扩展该测试是待办项 |

> 旧位置（`carlos-spring-boot-starter-workflow`）只能靠反射断言注解，因为那时 classpath 上没有 Servlet API；
> 迁到 `-bus` 之后有了真实的 Servlet/Web MVC 环境，接口测试已升级为**真实 MockMvc**（见上表）。

### 运行 PostgreSQL DDL 校验

需要一台可连接的 PostgreSQL。测试通过环境变量（或同名 JVM 系统属性）读取连接信息，**未配置 `POC_PG_URL` 时自动跳过**，因此默认构建不需要数据库：

```bash
POC_PG_URL='jdbc:postgresql://127.0.0.1:5432/postgres' \
POC_PG_USER='postgres' \
POC_PG_PASSWORD='<密码>' \
mvn -o -pl carlos-integration/carlos-workflow/carlos-workflow-bus test
```

| 变量               | 必填 | 说明                                       |
|------------------|----|------------------------------------------|
| `POC_PG_URL`     | 是  | JDBC URL；不配置则测试跳过                       |
| `POC_PG_USER`    | 否  | 连接用户名                                    |
| `POC_PG_PASSWORD` | 否  | 连接密码                                     |
| `POC_PG_SCHEMA`  | 否  | 校验用 schema，默认 `workflow_poc`              |

> **警告**：该测试会先 `DROP SCHEMA IF EXISTS <schema> CASCADE` 再重建，请确保 `POC_PG_SCHEMA` 指向的是专用 schema，不要指向生产库中的已有 schema。
>
> 脚本路径按 `../docs/sql/` 解析（测试工作目录是模块根 `carlos-workflow-bus`，DDL 按模块惯例放在聚合模块的 `docs/sql` 下）。

> 端到端流程（导入流程定义 → 发起 → 审批）**不在本 POC 范围内**：Warm-Flow 的 `DefJson` 设计器 JSON 格式没有公开规范，手工构造容易写出假失败，留作后续阶段在跑通设计器后补测。

## 注意事项

- **`carlos.workflow.enabled` 与 `warm-flow.enabled` 是两个独立开关。** 前者控制 Carlos 侧扩展 Bean（三个服务，以及组件扫描到的两套共六个控制器）是否装配，由 `@ConditionalOnProperty` 直接读取原始配置项、缺省即生效；后者控制 Warm-Flow 引擎（DAO/Service/`initFlow`）是否装配。关闭 `carlos.workflow.enabled` 不会停用引擎，反而会让 `FlowEngine.permissionHandler()` 变为 `null`（引擎对 `PermissionHandler` 缺失是静默降级）。**反过来，只关引擎不关本组件会让服务层因缺少引擎 Bean 而启动失败**——两个开关要么一起开，要么一起关。
- **`warm-flow.permission-handler-path` 必须保持为空。** 该配置项走 `FlowEngine.initBean`，用 `clazz.getConstructor().newInstance()` 反射实例化，完全绕过 Spring，依赖注入的实现会在反射实例化时失败并被静默吞掉；保持为空时引擎改为按类型从 Spring 容器取 `PermissionHandler`。
- **`warm-flow.global-listener-path` 同样必须保持为空。** 同一条 `initBean` 逻辑，`WorkflowAssigneeFailFastListener` 依赖 `WorkflowProperties`、没有无参构造器；保持为空时引擎按类型从 Spring 容器取 `GlobalListener`（即本模块注册的 fail-fast 实现）。**引擎只认一个全局监听器**，配了该路径就会绕过 Bean 装配——即便实例化失败后引擎会回退到 Spring Bean，也不要依赖这条兜底路径。
- **不要给应用开启懒加载。** `spring.main.lazy-initialization=true` 时 `initFlow` 不会被初始化，引擎静态配置（`FlowEngine`）为空，运行期调用会失败。
- **不要依赖 `spring.main.allow-bean-definition-overriding=true` 来绕过 Bean 名冲突。** 打开后冲突会静默覆盖，具体哪个 Bean 生效取决于装配顺序，排障成本远高于改个 Bean 名。
- **设计器返回的是 Warm-Flow 自己的响应体。** `/warm-flow-ui/**`（静态资源与设计器配置）与 `/warm-flow/**`（设计器数据接口）返回 `org.dromara.warm.flow.core.dto.ApiResult`（字段为 `code`/`msg`/`data`），**不是** Carlos 的 `Result<T>`（`success`/`code`/`msg`/`data`/`timestamp`/`details`）。前端与网关的响应处理需按两套结构分别适配。
- **设计器默认匿名访问。** Warm-Flow 的 `WarmFlowUiController` 注释即为「匿名访问」，`/warm-flow-ui/**` 与 `/warm-flow/**` 需要由网关或 Spring Security 自行加白名单/鉴权规则，不能依赖组件本身。
- 本模块依赖 Spring Boot 4 的 Jackson 3（`tools.jackson`）。**不要引入 Jackson 2 的 `jackson-databind`**，它会与 `warm-flow-plugin-json-jackson3` 争用同一套 JSON 语义。已实测的**踩坑路径**：`carlos-spring-boot-starter-openapi` → knife4j → springdoc → `swagger-core-jakarta` → `com.fasterxml.jackson.core:jackson-databind:2.21.2`。所以本模块**故意不引入 OpenAPI/Knife4j**（除非先把这条传递依赖排掉），`JsonConvertSpiTest` 会在 CI 里挡住它。
- **审批与业务事务不在同一事务内。** 审批中心是独立服务，引擎的写操作与业务单据的写操作分处两个进程，一致性只能靠回调契约（未定）。这是选择集中式部署的直接代价。

## 后续计划

- **业务回调契约**（Feign 回调 / MQ 事件 / 轮询 / Seata AT 四选一）——当前最大的未决项
- **办理人解析的剩余能力**：静态词汇（`user:` / `role:` / `dept:` / `deptLeader:` /
  `dept:rec` / `position:` / `roleInDept:`）与动态词汇（`@workflowAssignees.upLeader` /
  `subordinates`）均已接入；还缺设计器侧的 `HandlerSelectService` / `handlerFeedback` /
  `HandlerDictService` 三个 SPI（否则表达式只能手填），以及**设计器产出的定义绕过
  `POST /workflow/definition` 校验**的问题（设计器直连引擎的 `/warm-flow/save-json`）
- 超时任务调度、消息通知与审计日志集成
- 基于设计器产出的真实 `DefJson` 补端到端审批用例（含 `handle` 各动作与流程图状态着色）
- 待办 / 已办列表的姓名解析目前是「一次远程调用取回整页」，超过 500 个 id 时需要分批（org 侧建议单次不超过 500）
- 给 DDL 校验测试补 `workflow_form_postgresql.sql` 覆盖
- 达梦（DM）版建表脚本与 Liquibase 变更集，并在真实达梦库上验证分页 SQL（驱动与 `db-type` 已就位）
