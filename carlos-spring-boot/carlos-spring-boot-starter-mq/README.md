# carlos-spring-boot-starter-mq

Carlos Framework 的消息队列 Starter：提供**传输中立**的统一发送 / 消费 API。当前唯一实现为
**Redis Stream**（`MqType.REDIS`，传输名 `redis-stream`）。

业务代码只依赖 `MqTemplate`（发送）与 `@MqListener`（消费），**不感知底层传输**。

> **关于历史版本**
>
> 本模块在 2026-09 经历了一次完整的契约重塑。旧版本按 RocketMQ 形状设计，包含
> `DelayLevel`（18 级量化延迟）、`SendType`、`sendOrdered`、`sendTransaction`、
> `MqTransactionListener`、`MqUtil` 静态门面、`@EnableMq`、发送/消费拦截器，以及
> Kafka / RabbitMQ / RocketMQ 三个传输实现。
>
> **上述内容已全部移除。** 若你的代码引用了其中任何符号，说明它停留在 3.0.0 之前，
> 需要按本文档迁移。

## 特性

| 特性 | 说明 |
|---|---|
| 传输中立 | `MqTransport` SPI + `MqTemplate` 模板；发送侧接入新传输**零改动** |
| 能力显式声明 | `MqCapability` 声明真正支持的能力；**不支持即显式失败**，绝不静默降级 |
| 失败即数据 | 发送族失败通过 `SendResult` 表达，**不抛异常**（异常只用于启动期配置错误） |
| 启动期校验 | `@MqListener` 声明的能力组合若传输不支持，**拒绝启动**而非运行到某条消息才暴露 |
| 可靠消费 | 消费者组 + **先处理成功再确认** + 待处理认领重投 + 死信队列（有界） |
| 注解驱动 | `@MqListener` 支持 4 种处理器形状，按形参类型自动反序列化 |
| 可观测 | Actuator 健康检查（**真实往返**）+ Micrometer 指标（含 PENDING 积压） |

## 架构总览

### 分层

```
业务代码
  │  只依赖这两样，不感知底层传输
  ├── MqTemplate          发送：send / send(topic, payload, options)
  │                             sendDelayed / sendAsync / sendOneWay
  └── @MqListener         消费：注解驱动，启动期自动注册消费者与方法绑定
        │
        ▼
  DefaultMqTemplate       传输无关的模板：主题校验、序列化、体积校验、能力门控、
  NoOpMqTemplate          异步（虚拟线程）                    ← type=none 时的兜底
        │
        ▼
  MqTransport  (SPI)      传输契约：能力声明 / 生命周期 / 收发
  └── RedisMqTransport    当前唯一实现（MqType.REDIS）
        │
        ├─ 生产 ─ MqRecordMapper.toFields(...) → XADD
        └─ 消费 ─ RedisMqConsumerContainer   （Spring Data 容器 + 消费组 + 先处理后确认）
                  RedisMqPendingReclaimer    （XPENDING → XCLAIM：重投 / 转死信）
                  RedisMqDelayDueHandler     （延时到期 → 投回原主题）
                  RedisMqDeadLetterHandler   （按主题独立的死信 Stream）
        │
        ▼
  RedisStreamTemplate             原语层（carlos-spring-boot-starter-redis）
  RedissonDelayedQueueManager     只认识 key / group / recordId，不含任何 MQ 语义
        │
        ▼
  Redis 8.x（Streams + Redisson 延时结构）
```

### 两条链路最终汇成一条

| | 路径 |
|---|---|
| **即时** | `send` → `XADD` → 目标 Stream → 消费组 `XREADGROUP` → 处理成功 `XACK` |
| **延时** | `sendDelayed` → Redisson `RDelayedQueue`（按 bucket）→ 到期转移线程 → `RedisMqDelayDueHandler` → **投回原主题的 Stream**，此后与即时投递**同一条**链路 |

> 延时不是「另一条消费链路」——到期后它就是一条普通消息。
> 这样消费端只需认识一种形态，也不必为延时单独实现确认/重投/死信。
>
> 代价：延时条目在到达「到期」之前**不占用 PEL**，因此**不受确认/重投保护**；
> 到期那一步若写入 Stream 失败则不再重投（见「已知缺口」）。

### 可靠性的支点：一条 Redis 语义

`XREADGROUP` **无论是否 `XACK`，都会把条目放进待处理队列（PEL）**。
于是「不确认」恰好等价于「留在 PEL 等重投」——整个可靠消费设计都建立在这一条上：

| 处理结果 | 框架动作 | PEL |
|---|---|---|
| `void` 返回 / `SUCCESS` | `XACK` | **清空** |
| 显式 `DROP` | `XACK` + 写死信 | **清空** |
| 抛异常 / `RETRY` | **不** `XACK` | **保留** |
| `RECONSUME_LATER` | 不 `XACK` + 投一条延时消息 | **保留** |

配套三点：投递次数**不自己存**，直接用 PEL 自身计数器（`XCLAIM` 自增，显式传 `RETRYCOUNT`
会把计数重置而导致死信永不触发）；认领后**就地处理**而非重新 `XADD`（否则计数归 1、原条目悬挂）；
超过 `max-delivery-count` 由认领器转死信。

### 线程模型

| 线程名 | 职责 |
|---|---|
| `mq-consumer-<topic>-N` | **每个订阅一个**固定线程池（大小 = `@MqListener.concurrency`），Spring Data 容器的轮询任务；读异常只记日志、绝不让轮询退出 |
| `mq-pending-reclaimer` | 单线程定时轮询，按空闲阈值认领 PEL 条目并就地处理；单个订阅失败不阻断其余订阅 |
| `redis-delay-transfer-<bucket>` | Redisson 延时队列的到期转移线程（原语层，每 bucket 一条） |
| 虚拟线程（每次发送一个） | `sendAsync` 的执行体 |

## 实际使用现状

### 使用点登记（以仓库当前代码为准）

生产代码中 `@MqListener` **只有一处**，即消费侧当前只有审计一条链路：

| 场景 | 生产端 | 消费端 | 主题 / 消费组 |
|---|---|---|---|
| **审计日志扇出**<br>（登录 / 登出热路径） | `carlos-auth` · `AuditLogPublisher` | `carlos-audit` · `AuditLogMqConsumer`<br>`concurrency = 2` | `audit:log`<br>`audit:log:writer` |
| **`@Log` 操作日志跨服务落库** | `carlos-audit-api` · `AuditMqLogStorage`<br>（`LogStorage` SPI 实现，`supportBatch() = false`） | **复用上行的消费者** | 同上 |
| **消息中心投递** | `carlos-message` · `RedisStreamMessageQueue`<br>（薄委托，只发不收） | **暂无**——编程式 `consume` 已废弃并显式抛异常，消费端由业务模块自行写 `@MqListener` | `message:{channelCode}`<br>（一渠道一主题） |

已声明依赖本模块的模块：`carlos-agent-bus`、`carlos-audit-api`、`carlos-audit-bus`、
`carlos-auth-service`、`carlos-job-bus`、`carlos-message-bus`、`carlos-org-bus`、
`carlos-system-bus`、`carlos-workflow-bus`、`carlos-sample-mq`。

> **引入即改变启动行为**：本模块硬依赖 `carlos-spring-boot-starter-redis`，且
> `carlos.mq.type` 默认 `auto` ⇒ 只要 classpath 上有 Redis 依赖就会装配真实传输。
> 一个**没有可用 Redis** 的模块加了这个依赖，不是「降级为不发消息」而是**启动失败**。

### 为什么首个场景是审计

`carlos-auth` 原有 **8 处**各自直接调用 `apiAuditLogMain.saveAuditLog(param)`，
且全部位于**登录 / 登出热路径**上、语义为 fire-and-forget：丢一条不影响认证与业务。

用它做首个落地场景，收益最大、风险最小。投递被收敛到单一出口，因此**只有一个切换点、
一个回退点**：

```
carlos-auth 的 8 个审计点
    └─► AuditLogPublisher                      ← 单一出口
            ├─ mode = MQ     （默认）→ MqTemplate.send("audit:log", ...)
            └─ mode = FEIGN  （回退）→ ApiAuditLogMain   ← 改造前的同步 Feign 路径
                                        配置键：carlos.auth.audit.mode
```

`AuditLogPublisher` **绝不抛异常**：投递失败只记 WARN 并继续（热路径上不能因审计阻塞登录）。
出问题时把 `mode` 改回 `FEIGN` 即可整体回退到改造前行为。

消费端 `AuditLogMqConsumer` 与 Feign 入口**复用同一段 service 代码**（行为一致性靠复用保证，
而非两处各自维护），并用**信封的 `messageId`** 做本地去重窗（10 万条 / 10 分钟）——
`messageId` 由发送端生成且**重投时不变**，恰好覆盖真实的重复场景。

### 主题命名约定

`{模块}:{业务}`（与项目 Redis Key 约定一致），物理键由框架加前缀：
`audit:log` → `framework:mq:stream:audit:log`。

**主题常量放在两端共享的位置**（如 `carlos-audit-api` 的 `AuditMqTopics`），
避免生产端与消费端各写一遍字符串而漂移 —— 漂移的后果是消息发出去没人消费、
**双方都不报错**，属最难排查的一类问题。

### 当前部署配置：全部为默认值

已核实所有 Nacos 配置（`common.yml` / `redis.yml` / 各模块 yml）中**都没有**
`carlos.mq.*` 与 `carlos.redis.stream.*`，即当前运行在框架默认值上：

| 配置 | 生效值 | 含义 |
|---|---|---|
| `carlos.mq.type` | `auto` | 容器中只有一个传输 ⇒ 自动选中 Redis，无需显式配置 |
| `carlos.redis.stream.key-prefix` | `framework:mq:stream:` | 实际键形如 `framework:mq:stream:audit:log` |
| `carlos.redis.stream.max-length` | `20000` | 每主题 Stream 上限——共用 Redis 实例下的核心保护 |
| `carlos.mq.consumer.max-delivery-count` | `3` | 第 3 次投递后转死信 |
| `carlos.mq.consumer.pending-claim-interval` / `-min-idle` | `30s` / `60s` | 重投节拍 |

Redis 连接本身来自 Nacos `redis.yml`，用的是 **`spring.data.redis.*`**（Boot 4 的正确前缀）。
⚠️ 但各模块 `application-boot.yml`（**boot 模式**，不导 Nacos）里写的是 `spring.redis.*` ——
**Boot 4 不读取该前缀**，boot 模式下会回落到 `localhost:6379`。详见「已知缺口」。

## 快速开始

### 1. 引入依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mq</artifactId>
</dependency>
```

本模块**已硬依赖** `carlos-spring-boot-starter-redis`（刻意不设 `optional`）—— 引入即带
Redis Stream 与 Redisson 延时原语，不需要额外声明。

### 2. 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379

carlos:
  mq:
    type: redis                 # auto(自动检测) / redis / none
    topics:
      "order-topic":
        max-length: 20000       # 本主题 Stream 上限；不写则用 carlos.redis.stream.max-length
        bucket: normal          # 延时消息落哪个桶，必须出现在 carlos.redis.delay.buckets 中

  # 物理量配置（原语层，与 MQ 语义无关）
  redis:
    stream:
      enabled: true
      max-length: 20000
      block-timeout: 2s
    delay:
      enabled: true
      buckets: [ normal ]
      max-delay: 1h
```

### 3. 发送

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MqTemplate mqTemplate;

    public void createOrder(Order order) {
        SendResult result = mqTemplate.send("order-topic", order);
        if (!result.isSuccess()) {
            // 失败是数据不是异常：按 result.isRetryable() 决定是否补偿
            log.error("投递失败：status={}, code={}", result.getStatus(), result.getCode());
        }
    }

    /** 带业务键与消息头 */
    public void payOrder(Order order) {
        MqSendOptions options = MqSendOptions.ofKey(order.getOrderNo())
            .withHeader("source", "order-service");
        mqTemplate.send("order-topic", order, options);
    }

    /** 延时 5 分钟（任意精度） */
    public void scheduleCheck(Order order) {
        mqTemplate.sendDelayed("order-check-topic", order, Duration.ofMinutes(5));
    }

    /** 异步：回调携带完整结果，成功失败同一入口 */
    public void asyncNotice(Notice notice) {
        mqTemplate.sendAsync("notice-topic", notice,
            r -> log.info("投递完成：success={}, status={}", r.isSuccess(), r.getStatus()));
    }
}
```

### 4. 消费

```java
@Component
@Slf4j
public class OrderMessageConsumer {

    /** 形状一：只接业务负载（推荐）—— 框架按形参类型反序列化 */
    @MqListener(topic = "order-topic", group = "order-group", concurrency = 5)
    public void onOrder(Order order) {
        log.info("收到订单：{}", order.getOrderNo());
    }

    /** 形状二：负载 + 信封（需要投递次数等元信息时） */
    @MqListener(topic = "order-created-topic", group = "order-created-group")
    public void onCreated(Order order, MqMessage envelope) {
        log.info("第 {} 次投递，messageId={}", envelope.deliveryCount(), envelope.messageId());
    }

    /** 形状三：只接信封（负载类型不定时） */
    @MqListener(topic = "order-paid-topic", group = "order-paid-group")
    public void onPaid(MqMessage envelope) {
        log.info("payloadType={}, payload={}", envelope.payloadType(), envelope.payload());
    }

    /** 形状四：无参（探活/占位） */
    @MqListener(topic = "tick-topic", group = "tick-group")
    public void onTick() { }

    /** 显式控制消费状态：返回 ConsumeResult */
    @MqListener(topic = "order-shipped-topic", group = "order-shipped-group")
    public ConsumeResult onShipped(Order order) {
        if (order.getOrderNo() == null) {
            // 重试也不会变好 -> 放弃（写入死信便于排查）
            return ConsumeResult.drop(null, "order-shipped-topic", "缺少 orderNo");
        }
        return ConsumeResult.success(null, "order-shipped-topic");
    }
}
```

## 配置项

### `carlos.mq.*` —— 业务语义量

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `enabled` | `true` | 模块总开关 |
| `type` | `auto` | `auto` / `redis` / `none`；见下方「传输选择」 |
| `max-payload-bytes` | `65536` | 单条负载上限；超限返回 `PAYLOAD_TOO_LARGE`，**不截断** |
| `topics.<topic>.max-length` | 取全局 | 本主题 Stream 长度上限（`MAXLEN`） |
| `topics.<topic>.bucket` | 跟随原语层 | 延时桶名；留空即用 `carlos.redis.delay.buckets` 的第一个；**显式填写时启动期会校验其存在** |
| `consumer.max-delivery-count` | `3` | 最大投递次数，达到后由认领器转入死信 |
| `consumer.pending-claim-interval` | `30s` | 待处理认领轮询间隔；`0` 表示关闭认领 |
| `consumer.pending-claim-min-idle` | `60s` | 认领空闲阈值，**必须显著大于单条处理耗时 P99**，否则会抢走正在处理的消息 |
| `consumer.dlq-enabled` | `true` | 关闭后超限消息只计数不落死信 |
| `consumer.dlq-max-length` | `5000` | 死信 Stream 上限（死信也必须有界） |
| `consumer.close-timeout` | `10s` | 关闭时等待消费者退出的上限 |
| `capability.fail-fast` | `true` | 能力校验不通过时拒绝启动；仅迁移期可临时关闭 |

### `carlos.redis.*` —— 物理量（原语层）

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `carlos.redis.stream.key-prefix` | `framework:mq:stream:` | Stream key 前缀 |
| `carlos.redis.stream.max-length` | `20000` | 全局 `XADD MAXLEN` 上限 |
| `carlos.redis.stream.approximate-trim` | `true` | 使用 `MAXLEN ~` 近似修剪 |
| `carlos.redis.stream.block-timeout` | `2s` | `XREADGROUP BLOCK` 时长 |
| `carlos.redis.stream.claim-batch-size` | `50` | 单轮认领条数 |
| `carlos.redis.delay.buckets` | `[normal]` | 延时桶列表 |
| `carlos.redis.delay.max-delay` | `1h` | 单条延时上限 |
| `carlos.redis.delay.queue-prefix` | `framework:mq:delay:` | 延时结构 key 前缀 |

> **为什么分两层**：MQ 层只放业务语义（topic、消费组、重试、死信），原语层只放物理量
> （key 前缀、`MAXLEN`、阻塞时长）。同一默认值只在一处定义。

### 传输选择（`carlos.mq.type`）

| 取值 | 行为 |
|---|---|
| `auto` | 容器中**恰好一个**传输实现 -> 选它；**多个** -> 启动失败并提示显式配置（不猜） |
| `redis` | 使用 Redis Stream 传输 |
| `none` | 显式声明「本次不接 MQ」，装配 `NoOpMqTemplate`；**任何发送都返回明确失败** |

**没有任何传输实现时不会静默降级**，而是启动失败（`MQ_TRANSPORT_NOT_FOUND`）。
`MqType` 中的 `rabbitmq` / `rocketmq` / `kafka` 是**预留类型位，当前无实现**——
配成它们会直接启动失败。

## 核心 API

### `MqTemplate`（业务侧唯一依赖）

| 方法 | 说明 |
|---|---|
| `send(topic, payload)` | 同步发送 |
| `send(topic, payload, options)` | 同步发送（带业务键 / 消息头） |
| `sendAsync(topic, payload, callback)` | 异步发送（虚拟线程，回调携带完整结果） |
| `sendOneWay(topic, payload)` | 单向发送，不等待结果 |
| `sendDelayed(topic, payload, Duration)` | 延时发送（**任意精度**，非正数即立即投递） |
| `capabilities()` / `supports(cap)` | 查询当前传输能力集 |
| `ping()` | 健康探测（**真实往返**） |
| `getMqType()` / `getTransportName()` | 当前传输标识 |

`MqSendOptions` **只有两个字段**：`key`（业务键）与 `headers`（消息头）。
长度上限是**按主题**的物理量，配置入口是 `carlos.mq.topics.<topic>.max-length`，
不做成逐消息选项——那既无法生效，也要求把 `MAXLEN` 这个 Redis Stream 概念塞进传输中立的消息信封。

### `MqCapability` 与 Redis 传输的实际能力

`MqCapability` 枚举声明了 11 项能力，但**传输只声明它真正兑现的那些**：

| 能力 | Redis 传输 | 说明 |
|---|---|---|
| `basic` | ✅ | 基础发送 |
| `async-send` | ✅ | 由 `DefaultMqTemplate` 的虚拟线程提供 |
| `delayed` | ✅（需延时管理器已启动） | Redisson `RDelayedQueue` + `RedisMqDelayDueHandler` 投递回原主题 |
| `consume` | ✅（需消费容器在场） | 消费者组 |
| `pending-claim` | ✅ | `XPENDING` -> `XCLAIM` 认领重投 |
| `dead-letter` | ✅ | 按主题独立的死信 Stream |
| `batch-consume` | ✅ | `batchSize > 1` |
| `broadcast` | ✅ | 每实例独立消费组 |
| `ordered` | ❌ | 同组多消费者会交错，**不声明** |
| `tag-filter` | ❌ | Redis Stream 无服务端过滤，用「一个事件类型一个主题」替代 |
| `transactional` | ❌ | 无原生事务消息，走 outbox（见下） |

调用未声明的能力会抛 `MqCapabilityNotSupportedException`（码 `20002`），**不会**退化成
一个语义不同的操作。

### `SendStatus`（发送结果状态）

| 状态 | 码 | HTTP | 可重试 |
|---|---|---|---|
| `SUCCESS` | `00000` | 200 | — |
| `DROPPED` | `20009` | 200 | 否（计为成功） |
| `REJECTED` | `20005` | 429 | 是 |
| `CAPABILITY_NOT_SUPPORTED` | `20002` | 501 | 否 |
| `INVALID_ARGUMENT` | `10002` | 400 | 否 |
| `PAYLOAD_TOO_LARGE` | `10003` | 413 | 否 |
| `DELAY_EXCEEDS_MAX` | `20003` | 400 | 否 |
| `DELAY_QUEUE_FULL` | `20004` | 429 | 否 |
| `SEND_FAILED` | `30003` | 500 | 是 |
| `SEND_TIMEOUT` | `30004` | 504 | 是 |
| `TRANSPORT_UNAVAILABLE` | `30006` | 503 | 是 |
| `SERIALIZATION_ERROR` | `50008` | 500 | 否 |
| `NOT_INITIALIZED` | `50010` | 500 | 否 |

### `@MqListener`（7 个属性）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `topic` | — | **必填**，可多个（一个方法监听多主题） |
| `group` | `""` | 消费组；留空按 `{topic}:default` 派生 |
| `consumerId` | `""` | 显式消费者名；**常规场景留空**（框架按实例派生唯一名） |
| `consumeMode` | `CLUSTER` | `CLUSTER` 同组分摊 / `BROADCAST` 每实例各收一份 |
| `concurrency` | `1` | 同组内并发消费者数 |
| `batchSize` | `1` | 单次拉取条数 |
| `maxDeliveryCount` | `-1` | 最大投递次数；非正数用全局配置 |

**处理器形状**：`(负载)` / `(负载, MqMessage)` / `(MqMessage)` / `()`；
**返回类型**：`void` 或 `ConsumeResult`。违反在**启动期**报错（`MQ_LISTENER_CONFIG_INVALID`）。

### `ConsumeResult`（消费结果 -> 可靠性出口）

| 状态 | 行为 |
|---|---|
| `SUCCESS` | 确认（`XACK`） |
| `DROP` | 确认 + 写死信 |
| `RETRY` | **不确认**，留在待处理队列等认领重投 |
| `RECONSUME_LATER` | 不确认 + 立即投递一条延时消息 |

**返回值与异常映射**：返回 `void` / 其它对象视为成功；业务抛异常一律转 `RETRY`（保守优先，
不丢消息，由投递次数上限兜住）；**负载反序列化失败转 `DROP`** —— 这类消息无法被任何重试修复。

## 可靠性语义

1. **先处理成功、再确认**。`XREADGROUP` 无论是否确认都会把条目放入待处理队列，因此
   「不确认」恰好等价于「留在待处理队列等重投」。框架只在处理成功时 `XACK`。
2. **投递次数不自己存**，直接用待处理队列自身的计数器（`XCLAIM` 自增）。显式传 `RETRYCOUNT`
   会把计数重置，导致死信永不触发——框架刻意不传。
3. **认领是「就地处理」而非「重新投递」**。重新 `XADD` 会产生新 recordId、计数归 1，
   使死信判定失效。
4. **死信有界**：`dlq-max-length` 防止「毒消息无限增长」只是换个地方继续。
5. **消费者名进程内稳定、进程间唯一**（含 hostname + pid + 随机串）。多副本共用同一名字会
   导致消息被瓜分、待处理队列归属混乱。
6. **广播模式**靠消费组加实例后缀实现（每实例独立消费组），不依赖传输特性。

## 观测

**健康检查**（需 Actuator）：`GET /actuator/health`，明细暴露 `transport` / `type` /
`capabilities`。`ping()` 做一次真实的 `XLEN` 往返，不是「bean 非空」的恒真判定。

**指标**（需 Micrometer），前缀 `carlos.mq.`：

| 指标 | 类型 | 说明 |
|---|---|---|
| `carlos.mq.consume.pending` | Gauge | 每（流, 消费组）待处理条目数，**同时反映积压与死消费者遗留** |
| `carlos.mq.consume.total` | Counter | 按 `topic` + `status` 维度 |
| `carlos.mq.consume.error` | Counter | 处理链自身异常 |
| `carlos.mq.dlq` | Counter | 转入死信 |
| `carlos.mq.reclaim` | Counter | 认领重投 |

## 键名规约

| 用途 | 形态 | 示例（默认前缀） |
|---|---|---|
| Stream key | `{carlos.redis.stream.key-prefix}{topic}` | `framework:mq:stream:order-topic` |
| 死信 Stream | 上述 + `:dlq` | `framework:mq:stream:order-topic:dlq` |
| 延时结构 | `{carlos.redis.delay.queue-prefix}{bucket}` | `framework:mq:delay:normal` |
| 默认消费组 | `{topic}:default` | `order-topic:default` |

## 已知缺口与边界

以下为**当前实现如实存在的限制**，使用前需知悉：

### 功能缺口

1. **延时投递的丢失边界**：条目到期时已从延时结构取出，若此时写入目标 Stream 失败，
   **不会重投**（与原语层转移线程的取舍一致），只记录 ERROR 日志并继续处理同桶的后续条目。
   需要「到期必达」的场景应在业务侧做补偿。

   > **该链路曾整体不可用**，2026-09 起修复。原因：`RedissonDelayedQueue` 必须先有
   > `DelayDueHandler` 才会启动转移线程，而该接口此前**全仓无实现**（其 javadoc 声明
   > 「由 `com.carlos.mq` 提供」，本模块却从未提供）→ 一个桶都不启动 → 入队必失败，
   > 且错误被误报为**可重试**，调用方无限重试。
   >
   > 现已补齐 `RedisMqDelayDueHandler`（把到期条目投递回其原主题的 Stream）与载荷格式
   > `MqDelayPayload`（**目标主题 + Stream 字段**——早期只入队裸负载，topic 会丢失，
   > 仅凭桶名无法路由，因为桶与主题是多对多）。
   >
   > ✅ **该链路已通过真机集成测试**：`MqIntegrationTest` 启动完整 Spring 上下文，
   > 实测了「延时能力确实被声明（说明到期处理器被原语层取到）」与
   > 「到期后条目回到原主题的 Stream」。

2. **消费侧的「建组引导」曾摧毁消费组（2026-09-26 修复）**。原语层 `createGroupIfAbsent`
   曾用「写入占位条目 → 建组 → 删掉占位条目」绕开「空 key 不能建组」的限制。但占位条目是
   流里**唯一**一条，删掉它会让 Redis **移除整个 stream key**，**消费组随之消失**——
   而该方法仍返回 `true`。

   后果：此后每一次 `XREADGROUP` 都报 `NOGROUP`，**消费链路整体不可用**，且表面无异常
   （容器只记日志并继续轮询）；可靠性语义（确认 / 重投 / 死信）一条都不会发生。
   已改为 `XGROUP CREATE ... MKSTREAM` 一步建流建组。

   > ✅ **消费侧已通过真机集成测试**（`MqConsumerIntegrationTest`），实测四条语义：
   > 成功 → 确认（PEL 清空）、失败 → **不**确认（PEL 保留）并被认领重投、
   > 投递次数随重投**递增**、达上限转死信、显式 `DROP` → 确认 + 死信。
   >
   > 同一轮实测还修掉两处：认领器把 `XCLAIM` **之前**的投递次数交给处理器（计数永远
   > 滞后一次，依赖它做退避/上限判断的业务会整体偏移）；认领循环把「一个订阅失败」
   > 放大成「后续订阅本轮全部不被认领」。

3. **只有 Redis Stream 一个传输**。`MqTransport` SPI 的「接入新传输零改动」目前
   **仅对发送侧成立**——`MqConsumerCoordinator` 仍直接依赖 Redis 专属的待处理认领器，
   消费侧不中立。

4. **顺序消费、标签过滤、事务消息均不支持**。事务最终一致性请按
   `docs/design/DESIGN-20260925-MQ技术选型.md` §4.3 走本地消息表（outbox）+ 重试，
   MQ 仅承担投递加速。

### 未做启动期校验的配置

5. **`MqMessage.payloadType` 只记录不校验**：消费端按处理器形参类型反序列化，不比对声明类型。
6. **`carlos.redis.stream.group-start-offset` 不被消费容器读取**（容器硬编码 `lastConsumed`）。
7. **`topic` 名合法性、`max-payload-bytes` 与 `carlos.redis.stream.max-length` 的关系**
   等仍**未做启动期校验**，失败发生在首次发送时。

   > 延时桶已补上启动期校验：`carlos.mq.topics.<topic>.bucket` **显式填写**却不在
   > `carlos.redis.delay.buckets` 中即拒绝启动（码 `20010`）。留空不校验——它跟随原语层默认桶。

### 运维边界

8. **共有 Redis 实例有硬约束**：MQ 与缓存 / token 共用实例。实测该实例（2026-09-26）：
   `maxmemory=0`（**未设上限**）、`maxmemory-policy=noeviction`、**`aof_enabled=0`**：
   - 未设上限 ⇒「写满即全站登录失败」的触发点不确定，但也意味着**无界增长**；
   - AOF 关闭 ⇒ **Redis 崩溃即丢失最近写入**（含未消费的 MQ 消息与在线 token）。

   生产部署必须显式设置 `maxmemory`、开启 AOF，并保持 `XADD MAXLEN` 有界化（本模块默认已带）。
9. **无声明的丢失边界**：`MAXLEN ~` 修剪掉尚未消费的条目；Redis 崩溃且未开 AOF 时丢失
   最近写入。
10. **测试覆盖**：88 例单元测试 + 8 例真机集成测试（发送侧 4 + 消费侧 4）。
    - **已真机验证**：自动配置装配（含 `DelayDueHandler` 跨自动配置注入）、即时投递落盘与字段、
      延时到期回投原主题、消费侧四条可靠性语义（见上）；
    - **尚未验证**：多消费者并发（`concurrency > 1`）、广播模式、`batchSize > 1`、
      故障场景（Redis 抖动 / 崩溃 / 重启 / 网络分区）。

    集成测试**未设 `CARLOS_TEST_REDIS_HOST` 时整类跳过**，CI 无需 Redis 也不会红；
    本地运行需额外提供 `CARLOS_TEST_REDIS_PORT` / `CARLOS_TEST_REDIS_PASSWORD`。
    固定使用 **db 3**（与 db0 的缓存 / token、db1 / db5、样例库 db2 隔离）、键前缀
    `framework:mq:ittest:`，并在结束后**先停消费者与认领器**再按前缀清理——否则二者会继续轮询
    已删的键、刷出误导性的 `NOGROUP`。
12. **boot 模式下 Redis 配置前缀错误（跨模块，未修）**。cloud 模式从 Nacos `redis.yml` 取
    **`spring.data.redis.*`**（Boot 4 的正确前缀，连接正常）；但多个模块的
    `application-boot.yml`（boot 模式，不导 Nacos）里写的是 `spring.redis.*` ——
    **Boot 4 已改用 `spring.data.redis.*`，不再读取旧前缀**，boot 模式会静默回落到
    `localhost:6379`。

    受影响：`carlos-job` / `carlos-message` / `carlos-gateway` / `carlos-sample-mq` 等；
    `carlos-auth` / `carlos-audit` 用的是正确前缀（故线上 token、审计流都正常）。
    属框架级配置问题，影响面超出本模块，建议单独修。

## 环境要求

- JDK 21+
- Spring Boot 4.0.6+
- Redis 5.0+（用到 Streams 与消费组；认领刻意只用 `XPENDING` + `XCLAIM`，
  未使用 6.2 才有的 `XAUTOCLAIM`）
