# carlos-spring-boot-starter-disruptor

Disruptor 高性能队列组件，提供基于 LMAX Disruptor 的高性能内存队列实现，适用于高并发、低延迟的事件处理场景。

## 功能特性

- **高性能**: 基于 LMAX Disruptor，提供百万级 TPS 的事件处理能力
- **低延迟**: 无锁设计，减少线程上下文切换
- **事件驱动**: 支持事件发布-订阅模式
- **多消费者**: 支持多个消费者并行或串行处理
- **消费者链 DSL**: 支持流式 API 构建复杂的消费者处理链
- **多实例管理**: 支持管理多个 Disruptor 实例
- **监控指标**: 集成 Micrometer，提供发布延迟、成功率等指标
- **健康检查**: 集成 Spring Boot Actuator
- **Spring Boot 集成**: 自动配置和依赖注入

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-disruptor</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 配置示例

```yaml
carlos:
  disruptor:
    enabled: true
    # RingBuffer 大小，必须是 2 的幂
    buffer-size: 1024
    # 等待策略: BLOCKING, SLEEPING, YIELDING, BUSY_SPIN
    wait-strategy: blocking
    # 线程池配置
    thread-pool:
      core-size: 4
      max-size: 8
      queue-capacity: 100
      thread-name-prefix: "disruptor-"
```

## 使用示例

### 方式一：使用 DisruptorManager（推荐）

```java
@Service
public class OrderService {

    @Autowired
    private DisruptorManager disruptorManager;

    private DisruptorTemplate<OrderEvent> orderDisruptor;

    @PostConstruct
    public void init() {
        // 创建 Disruptor 实例
        orderDisruptor = disruptorManager.create("order",
            new ValidateHandler(),
            new PersistHandler(),
            new NotifyHandler()
        );
    }

    public void createOrder(Order order) {
        OrderEvent event = new OrderEvent(order);
        // 发布事件
        orderDisruptor.publishEvent(event);
    }
}

// 事件处理器
@Component
public class ValidateHandler implements DisruptorEventHandler<OrderEvent> {
    @Override
    public void onEvent(DisruptorEvent<OrderEvent> event, long sequence, boolean endOfBatch) {
        // 验证订单
        System.out.println("验证订单: " + event.getData().getOrderNo());
    }
}
```

### 方式二：使用 DSL 构建消费者链

```java
@Service
public class PipelineService {

    @Autowired
    private DisruptorProperties properties;

    @Autowired
    private DisruptorMetrics metrics;

    private DisruptorTemplate<DataEvent> pipelineDisruptor;

    @PostConstruct
    public void init() {
        // 构建复杂的消费者链
        pipelineDisruptor = EventHandlerDSL.<DataEvent>chain("pipeline", properties, metrics)
            // 第一步：解析（并行）
            .then(new ParseHandler(), new ValidateHandler())
            // 第二步：处理（串行）
            .thenSerial(new TransformHandler())
            // 第三步：持久化（并行）
            .then(new SaveToDbHandler(), new SendToMqHandler())
            .bufferSize(2048)
            .waitStrategy(new SleepingWaitStrategy())
            .build();
    }

    public void processData(DataEvent data) {
        pipelineDisruptor.publishEvent(data);
    }
}
```

### 方式三：发布不同类型的事件

```java
@Service
public class MultiEventService {

    @Autowired
    private DisruptorManager disruptorManager;

    private DisruptorTemplate<Object> eventDisruptor;

    @PostConstruct
    public void init() {
        eventDisruptor = disruptorManager.create("multi-event",
            new UniversalHandler()
        );
    }

    public void publishOrderEvent(Order order) {
        // 发布带类型的事件
        eventDisruptor.publishEvent("ORDER_CREATED", order);
    }

    public void publishPaymentEvent(Payment payment) {
        eventDisruptor.publishEvent("PAYMENT_COMPLETED", payment);
    }
}
```

### 高级用法：条件发布和批量发布

```java
@Service
public class AdvancedService {

    @Autowired
    private DisruptorTemplate<MyEvent> disruptorTemplate;

    public void conditionalPublish(MyEvent event) {
        // 尝试发布，如果队列满则立即返回 false
        boolean success = disruptorTemplate.tryPublishEvent(event);
        if (!success) {
            // 处理队列满的情况
            log.warn("Disruptor is full, event dropped");
        }
    }

    public void publishWithTimeout(MyEvent event) {
        // 带超时时间的发布
        boolean success = disruptorTemplate.tryPublishEvent(event, 100, TimeUnit.MILLISECONDS);
        if (!success) {
            log.warn("Publish timeout");
        }
    }

    public void batchPublish(List<MyEvent> events) {
        // 批量发布
        disruptorTemplate.publishEvents(events.toArray(new MyEvent[0]));
    }

    public void checkCapacity() {
        // 检查队列容量
        long remaining = disruptorTemplate.remainingCapacity();
        long total = disruptorTemplate.getBufferSize();
        double usage = 1.0 - (double) remaining / total;
        log.info("Disruptor usage: {:.2%}", usage);
    }
}
```

## 等待策略说明

| 策略                   | 说明                   | CPU 占用 | 适用场景                  |
|----------------------|----------------------|--------|-----------------------|
| BlockingWaitStrategy | 使用锁和条件变量阻塞等待         | 低      | 低延迟要求不高，需要低 CPU 占用的场景 |
| SleepingWaitStrategy | 先自旋，后 yield，最后 sleep | 中      | 平衡性能和 CPU 占用，推荐默认使用   |
| YieldingWaitStrategy | 先自旋，后 yield          | 较高     | 低延迟场景，可接受较高 CPU 占用    |
| BusySpinWaitStrategy | 持续自旋                 | 极高     | 极低延迟，专用 CPU 核心        |

## 核心 API

### DisruptorManager

| 方法                                                            | 说明              |
|---------------------------------------------------------------|-----------------|
| `create(String, DisruptorEventHandler...)`                    | 创建并启动 Disruptor |
| `create(String, int, WaitStrategy, DisruptorEventHandler...)` | 创建并启动（自定义参数）    |
| `getTemplate(String)`                                         | 获取已创建的 Template |
| `shutdown(String)`                                            | 停止指定实例          |
| `shutdownAll()`                                               | 停止所有实例          |
| `getStats(String)`                                            | 获取统计信息          |

### DisruptorTemplate

| 方法                                        | 说明                |
|-------------------------------------------|-------------------|
| `publishEvent(T)`                         | 发布事件（阻塞直到成功）      |
| `publishEvent(String, T)`                 | 发布带类型的事件          |
| `tryPublishEvent(T)`                      | 尝试发布（队列满返回 false） |
| `tryPublishEvent(T, long, TimeUnit)`      | 带超时的尝试发布          |
| `publishEvents(T...)`                     | 批量发布              |
| `remainingCapacity()` / `getBufferSize()` | 获取容量信息            |
| `getCursor()`                             | 获取当前游标            |
| `hasAvailableCapacity(int)`               | 检查是否有足够空间         |

### DisruptorEventHandler

```java
public interface DisruptorEventHandler<T> {
    // 处理事件（必须实现）
    void onEvent(DisruptorEvent<T> event, long sequence, boolean endOfBatch) throws Exception;

    // 获取处理器名称（可选）
    default String getHandlerName() { ... }

    // 是否忽略异常继续处理（可选）
    default boolean ignoreException() { ... }

    // 异常处理回调（可选）
    default void onException(DisruptorEvent<T> event, long sequence, Throwable exception) { ... }
}
```

## 消费者链模式

### 串行处理

```java
EventHandlerDSL.<MyEvent>chain("serial", properties)
    .thenSerial(new Handler1())
    .thenSerial(new Handler2())
    .thenSerial(new Handler3())
    .build();
// 执行顺序: Handler1 -> Handler2 -> Handler3
```

### 并行处理

```java
EventHandlerDSL.<MyEvent>chain("parallel", properties)
    .then(new Handler1(), new Handler2(), new Handler3())
    .build();
// 执行顺序: Handler1, Handler2, Handler3 同时执行
```

### 菱形模式（分流-合并）

```java
EventHandlerDSL.<MyEvent>chain("diamond", properties)
    .thenSerial(new SplitHandler())
    .then(new HandlerA(), new HandlerB())  // 并行处理
    .thenSerial(new MergeHandler())
    .build();
// 执行顺序: Split -> [A || B] -> Merge
```

## 监控指标

当引入 `micrometer-registry-prometheus` 后，会自动暴露以下指标：

| 指标名                            | 类型      | 说明              |
|--------------------------------|---------|-----------------|
| `disruptor_publish_total`      | Counter | 发布事件总数（按结果分类）   |
| `disruptor_publish_latency`    | Timer   | 发布延迟分布          |
| `disruptor_remaining_capacity` | Gauge   | RingBuffer 剩余容量 |
| `disruptor_buffer_size`        | Gauge   | RingBuffer 总容量  |

## 性能优化建议

1. **RingBuffer 大小**: 设置为 2 的幂，建议 1024-65536，根据实际场景调整
2. **等待策略**: 根据延迟要求选择合适的策略，推荐 `SLEEPING`
3. **对象池复用**: DisruptorEvent 对象会自动复用，避免在事件中持有大对象引用
4. **批处理**: 利用 `endOfBatch` 标志进行批量数据库写入等操作
5. **异常处理**: 设置 `ignoreException=true` 避免单条数据错误影响整体处理
6. **背压处理**: 使用 `tryPublishEvent` 避免队列无限增长导致 OOM

## 注意事项

- RingBuffer 大小必须是 2 的幂次方
- 事件处理器中避免阻塞操作，否则会阻塞整个消费者线程
- 生产环境建议使用 `DisruptorManager` 管理生命周期
- 注意监控队列积压情况，及时处理背压

## 版本要求

- JDK 17+
- Spring Boot 3.x
- Disruptor 4.0.x
