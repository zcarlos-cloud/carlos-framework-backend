# Carlos Job 定时任务调度中心

> 基于 Quartz（JDBC Store 集群模式）的分布式定时任务平台：调度集中在 carlos-job，执行分散在各业务服务。

---

## 架构

```
管理后台 → 网关 /job/** → carlos-job（Quartz 调度 + 管理接口 + 执行日志）
                              │ 到点触发
                              ▼ LoadBalancerClient + Nacos 服务发现
                     POST http://{serviceName}/inner/job/execute（内网直连，不过网关）
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        carlos-audit    carlos-message    任意业务服务
        (JobHandler)      (JobHandler)
```

- **统一管理**：任务 CRUD、启停、手动触发、执行日志全部在 carlos-job
- **集群安全**：Quartz JDBC Store 数据库锁，carlos-job 多实例部署同一触发器只触发一次
- **业务无侵入**：业务模块不依赖 Quartz，只实现 `JobHandler` 接口

## 模块结构

```
carlos-job/
├── carlos-job-api/       # Feign 接口定义（编程式任务管理）、AO/Param
├── carlos-job-client/    # 业务模块接入端：JobHandler + 内部执行端点（自动装配）
├── carlos-job-bus/       # 调度中心服务端（Quartz + 管理接口 + 执行日志）
├── carlos-job-boot/      # 单体启动入口（端口 9516）
├── carlos-job-cloud/     # 微服务启动入口（Nacos）
└── docs/sql/             # 建表脚本
```

## 部署前准备

按顺序执行建表脚本（PostgreSQL）：

```bash
psql -d carlos_framework -f docs/sql/quartz_tables_postgres.sql   # Quartz 集群表
psql -d carlos_framework -f docs/sql/job_execution_log.sql        # 执行日志表
psql -d carlos_framework -f docs/sql/job_info.sql                 # 任务控制面元数据表
```

> **双平面架构**：`job_info`（控制面，任务元数据唯一事实源）+ Quartz `qrtz_*`（执行面）。
> 所有写操作先落库后操作 Quartz，失败自动回滚；启动时 `JobReconcileRunner` 以 job_info 为准
> 对账修复漂移（Quartz 缺失重建 / 调度不一致重排 / 库里缺失补建）。

## 业务模块接入（注解式，推荐）

对标 xxl-job 的 `@XxlJob`：**代码即任务定义**，不必实现接口、也不必手工去后台建任务。

### 1. 引入依赖并配置调度中心地址

```xml
<dependency>
    <groupId>com.carlos.job</groupId>
    <artifactId>carlos-job-client</artifactId>
</dependency>
```

```yaml
carlos:
  job:
    client:
      # 调度中心地址；未配置则跳过自动注册（只登记处理器，任务仍需后台手工建）
      admin-url: http://carlos-job:9516
      access-token: ${CARLOS_JOB_ACCESS_TOKEN:}
```

### 2. 在方法上标注注解

```java
@Component
public class AuditArchiveJob {

    @ScheduleJob("auditArchiveHandler")
    @AutoRegisterJob(cron = "0 0 2 * * ?", description = "审计日志每日归档",
        misfireStrategy = 0, concurrentMode = 1, timeoutSeconds = 300, failRetryCount = 2,
        author = "carlos", alarmReceivers = "admin@example.com")
    public JobResult archive(String params) {
        // params 为任务配置中的 JSON 字符串（未配置时为 null）
        return JobResult.ok("归档完成");
    }
}
```

允许的方法签名（**其余签名启动即报错**，`carlos.job.client.fail-fast-on-invalid=false` 可降级为记 ERROR 并跳过）：

| 签名 | 行为 |
|---|---|
| `void m()` / `void m(String params)` | 正常返回即成功 |
| `JobResult m()` / `JobResult m(String params)` | 返回值作为执行结果 |

`@AutoRegisterJob` 的整型属性用 `-1`/`0` 表示"未指定"，由调度中心补默认值（`scheduleType`/`scheduleConf`/`jobName`/`jobGroup`/`serviceName`/`params`/`description`/`misfireStrategy`/`concurrentMode`/`timeoutSeconds`/`failRetryCount`/`status`/`author`/`alarmReceivers` 与后台建任务一一对应）。`serviceName` 留空回退本机 `spring.application.name`（业务服务必须能被派发，故不允许留空）。

### 3. 启动即注册（语义务必读完）

- **`job_info` 是唯一事实源**：任务不存在时按注解创建；**已存在则跳过、绝不覆盖** —— 运维在后台调整过的 cron/参数不会被下次发布改回去；不一致时服务端打 WARN 并把 `drifted` 回给客户端，客户端在启动汇总里以 ERROR 提示"需人工确认"（本框架 logback 不把 WARN 写进 info/error 日志文件，只靠服务端 WARN 会没人看见）。
- **删掉注解不会删除任务**：任务继续按库中配置调度，到点因处理器不存在而失败（`20603`）。要停用请到管理后台暂停/删除。
- **漂移态不自动重建**：Quartz 已有同名任务但 `job_info` 缺失时跳过并 WARN（重建会覆盖执行面配置）。
- **多实例并发安全**：由唯一索引 `uk_job_info_name_group` 仲裁，撞键方收敛为"已存在"，不会建出两条任务。
- **处理器名冲突即启动失败**：注解名撞注解名、或注解名撞 `JobHandler` Bean 名（注解名是手写常量，重名必是事故；接口式的 Bean 名冲突仍保持"先注册者 + WARN"）。
- **`status = "PAUSED"`**：建完立即暂停，适合先入库、确认后再到后台放开。
- **调度中心未启动不影响业务服务启动**：注册走守护线程 + 3 次线性退避重试（4xx 与响应体明确的拒绝都不重试），失败只记 ERROR，并按「调度中心不可达 / 注册请求被拒 / 被调度中心拒绝 / 其他」分类写明原因（连不上调度中心时**不要**去后台手工建任务，调度中心恢复后重启本服务即自动补上）。
- 注解里的 cron 与后台手工创建同样受最小间隔 10s 限制。
- 示例见 `carlos-job-bus/src/main/java/com/carlos/job/handler/AnnotationSampleJob.java`。

> 与 xxl-job 的差异：上游 `@XxlJob` 只声明处理器名，任务仍需在调度中心手工创建；本框架的
> `@AutoRegisterJob` 额外承担"播种"，但真相源仍是库，绝不做反向覆盖 —— 社区同类方案（如
> `@XxlRegister`）普遍只新增不更新、且无并发防护（会因缺唯一索引建出重复任务），本实现刻意避开了这两点。

### 4. 已知边界（当前实现不覆盖）

- **漂移比对只覆盖 4 个字段**：`scheduleConf` / `handlerName` / `serviceName` / `params`。改 `misfireStrategy`、`concurrentMode`、`timeoutSeconds`、`failRetryCount`、`description`、`status` 不会告警（也不覆盖），要改请到管理后台。
- **客户端只做形状校验**（正则、长度、取值区间）：`scheduleType` 与 `scheduleConf` 是否自洽、cron 是否合法由调度中心判定。因此**未配 `admin-url` 时注解写错不会暴露**——任务从未被创建，也没有任何 WARN。
- **处理器 Bean 不能是懒加载/@Lazy/原型作用域**：`JobHandlerRegistry` 在 `afterSingletonsInstantiated()` 做一次快照，晚于该时点创建的 Bean 进不了注册表（对接口式处理器同样成立，是既有行为）。
- 删掉注解只影响"是否继续播种"，不会停用任务；要停用请到管理后台。

## 业务模块接入（接口式，3 步）

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.carlos.job</groupId>
    <artifactId>carlos-job-client</artifactId>
</dependency>
```

### 2. 实现任务处理器

```java
@Component
public class AuditArchiveJobHandler implements JobHandler {

    @Override
    public JobResult execute(String params) {
        // 业务逻辑，params 为任务配置中的 JSON 字符串
        return JobResult.ok("归档完成");
    }
}
```

处理器名称默认取 Spring Bean 名（如 `auditArchiveJobHandler`），可重写 `name()` 自定义。

### 3. 创建任务

管理后台「定时任务」页面或调用 `POST /job/create`：

```json
{
  "jobName": "auditArchiveJob",
  "serviceName": "carlos-audit",
  "handlerName": "auditArchiveJobHandler",
  "scheduleType": 1,
  "scheduleConf": "0 0 2 * * ?",
  "misfireStrategy": 0,
  "concurrentMode": 1,
  "timeoutSeconds": 300,
  "failRetryCount": 2,
  "author": "carlos",
  "alarmReceivers": "admin@example.com",
  "description": "审计日志每日归档"
}
```

**调度类型 scheduleType**：

| 值 | 类型 | scheduleConf 格式 |
|---|---|---|
| 1 | Cron 表达式 | `0 0 2 * * ?`（Quartz 6 段） |
| 2 | 固定间隔 | 毫秒数（最小 10000） |
| 3 | 一次性 | ISO 时间 `2026-09-24T02:00:00` |

**调度策略**：

- `misfireStrategy` 错过触发策略：0 智能（Quartz 默认）/ 1 忽略错过（补偿全部）/ 2 立即执行一次 / 3 等待下次
- `concurrentMode` 并发模式：1 串行（`@DisallowConcurrentExecution`，集群内互斥）/ 2 并行
- `timeoutSeconds` 超时秒数（`InterruptableJob` 线程中断实现，0 不限制）
- `failRetryCount` 失败重试次数（`JobRetryListener` 派发一次性 SimpleTrigger 重试，集群故障转移安全）

失败且重试配额耗尽时，`JobAlarmListener` 经 carlos-message 发送报警（可降级，不影响调度）。

## 管理接口（/job/**）

| 端点 | 说明 |
|---|---|
| `GET /job/page` | 任务分页（名称/分组/服务/处理器/状态筛选） |
| `POST /job/create` | 新建任务 |
| `POST /job/update` | 修改调度配置/策略/参数/描述 |
| `POST /job/pause` / `job/resume` | 暂停 / 恢复 |
| `POST /job/runOnce` | 手动触发一次 |
| `POST /job/delete` | 删除（双平面任一存在即可删） |
| `GET /job/detail` | 任务详情（含上/下次触发时间、策略字段） |
| `GET /job/previewFireTimes` | 预览未来 N 次触发时间 |
| `GET /job/log/page` | 执行日志分页 |
| `POST /job/log/clean?retainDays=30` | 清理历史日志 |
| `GET /job/dashboard/overview?days=7` | 运行报表（任务数/在线节点/今日统计/趋势/状态分布） |
| `GET /job/monitor/schedulerMeta` | 调度器元数据（Quartz SchedulerMetaData） |
| `GET /job/monitor/nodes` | 集群节点列表（QRTZ_SCHEDULER_STATE 心跳视图） |
| `GET /job/monitor/services` / `serviceInstances?serviceName=` | 目标服务/实例（Nacos 服务发现） |
| `POST /job/monitor/standby` / `start` / `pauseAll` / `resumeAll` | 调度器全局控制 |

服务间编程式管理：Feign 接口 `ApiJob`（`/api/job/**`）。

## 配置

```yaml
carlos:
  job:
    # 调度中心回调业务端携带的令牌，与业务端 carlos.job.client.access-token 配对
    # 均不配置则不校验（内部网络部署时）；建议生产环境配置
    access-token: ${CARLOS_JOB_ACCESS_TOKEN:}
    connect-timeout-ms: 5000
    # 长耗时任务需调大
    read-timeout-ms: 300000
    # 启动时以 job_info 为准对账 Quartz 漂移（默认开启）
    reconcile-on-startup: true
    # 失败报警（重试耗尽后经 carlos-message 通知，carlos-message 不可用时自动降级跳过）
    alarm:
      enabled: false
      template-code: JOB_FAIL_ALARM
      sender-system: carlos-job
```

业务端（引入 client 的模块）：

```yaml
carlos:
  job:
    client:
      # 内部执行端点开关（/inner/job/execute）
      enabled: true
      # 与调度中心 carlos.job.access-token 配对，均空则不校验
      access-token: ${CARLOS_JOB_ACCESS_TOKEN:}
      # 注解任务自动注册（默认开启）；未配置 admin-url 时自动跳过
      auto-register: true
      admin-url: http://carlos-job:9516
      # 注册请求超时（独立于任务回调的 300s，注册是短请求）
      register-connect-timeout-ms: 3000
      register-read-timeout-ms: 10000
      # 单个任务的注册尝试次数与线性退避基准
      register-max-attempts: 3
      register-retry-backoff-ms: 2000
      # 注解声明非法时是否终止启动（默认 true；false = 记 ERROR 跳过）
      fail-fast-on-invalid: true
```

## 注意事项

- `/inner/job/execute`（回调业务端执行）与 `/inner/job/register`（业务端注册任务）都是内部端点，**不要**加入网关路由（Feign/服务发现直连不经过网关）
- 两个内部端点共用 `X-Job-Token`：**令牌为空时一律放行**（框架全局如此，调度中心启动时会打醒目 WARN）。空令牌下集群内任意进程都能 `POST /inner/job/register` 创建任意任务（`serviceName` 可任选，之后调度中心会带着自己的令牌去调该服务的执行端点），生产环境务必配置 `carlos.job.access-token`
- 业务服务若开启 `carlos.resource-server.enabled`，需把 `/inner/**` 加入放行白名单 —— 该开关默认的链是 `anyRequest().authenticated()`，否则回调与自动注册都会被 401 拦掉（当前框架内管理面服务均为关闭状态）
- boot 单体模式下任务优先本机执行，不产生 HTTP 回环
- 并发模式默认串行（`@DisallowConcurrentExecution`，JDBC 模式下集群级互斥）；并行模式需任务自身幂等
- `qrtz_*` 表不要被租户拦截器等 MyBatis 插件拦截
- `job_info` 是任务元数据的唯一事实源，Quartz 侧的漂移由启动对账自动修复；勿手工直改 `qrtz_*` 表

## License

Copyright (c) Carlos Team
