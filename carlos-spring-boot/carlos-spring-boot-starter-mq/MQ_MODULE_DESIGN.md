# Carlos MQ 模块功能设计方案

## 1. 设计目标

为 Carlos Framework 提供一个统一的消息队列封装模块，实现以下目标：

1. **统一接口**：屏蔽底层 MQ 实现差异，提供统一的消息发送和接收接口
2. **多 MQ 支持**：支持 RabbitMQ、RocketMQ、Kafka 等主流消息队列
3. **自动适配**：根据项目实际依赖自动选择 MQ 实现，无需修改业务代码
4. **配置驱动**：通过配置灵活切换 MQ 类型和参数
5. **功能完善**：支持普通消息、延迟消息、顺序消息、事务消息等特性

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         业务层 (Application)                      │
│                    使用 MqTemplate 发送/接收消息                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     抽象层 (mq-core)                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │ MqTemplate  │  │MqProperties │  │ Message / MqCallback    │  │
│  │ (统一操作接口)│  │ (配置属性)   │  │ (消息实体/回调接口)       │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    实现层 (mq-impl)                               │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐ │
│  │  RabbitMQImpl    │ │  RocketMQImpl    │ │   KafkaImpl      │ │
│  │  (AMQP协议)       │ │  (阿里云MQ)       │ │  (高吞吐)         │ │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    基础设施层                                     │
│            RabbitMQ / RocketMQ / Kafka 客户端                    │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 核心组件

| 组件                    | 职责       | 说明              |
|-----------------------|----------|-----------------|
| `MqTemplate`          | 统一消息操作接口 | 定义发送、接收、延迟消息等方法 |
| `MqClient`            | MQ 客户端抽象 | 底层 MQ 操作封装      |
| `MqProperties`        | 配置属性     | 支持多 MQ 配置       |
| `MqMessage`           | 消息实体     | 统一消息格式          |
| `MqAutoConfiguration` | 自动配置     | 根据条件自动装配        |

### 2.3 包结构设计

```
com.carlos.mq
├── core                              # 核心抽象层
│   ├── MqTemplate.java               # 统一操作接口
│   ├── MqClient.java                 # 客户端抽象接口
│   ├── MqMessage.java                # 消息实体
│   ├── MqCallback.java               # 发送回调接口
│   ├── MqListener.java               # 消息监听接口
│   └── MqTransactionListener.java    # 事务监听接口
├── config                            # 配置层
│   ├── MqProperties.java             # MQ 配置属性
│   ├── MqAutoConfiguration.java      # 自动配置类
│   └── MqClientConfiguration.java    # 客户端配置
├── support                           # 支持类
│   ├── MqType.java                   # MQ 类型枚举
│   ├── SendType.java                 # 发送方式枚举
│   ├── DelayLevel.java               # 延迟级别枚举
│   └── MessageStatus.java            # 消息状态枚举
├── client                            # 客户端实现
│   ├── rabbitmq                      # RabbitMQ 实现
│   │   ├── RabbitMqClient.java
│   │   ├── RabbitMqTemplate.java
│   │   └── RabbitMqListenerAdapter.java
│   ├── rocketmq                      # RocketMQ 实现
│   │   ├── RocketMqClient.java
│   │   ├── RocketMqTemplate.java
│   │   └── RocketMqListenerAdapter.java
│   └── kafka                         # Kafka 实现
│       ├── KafkaMqClient.java
│       ├── KafkaMqTemplate.java
│       └── KafkaMqListenerAdapter.java
├── util                              # 工具类
│   └── MqUtil.java                   # MQ 工具类
├── exception                         # 异常定义
│   ├── MqException.java
│   ├── MqSendException.java
│   ├── MqConsumeException.java
│   └── MqTimeoutException.java
└── annotation                        # 注解
    ├── MqListener.java               # 消息监听注解
    ├── MqHandler.java                # 消息处理注解
    └── EnableMq.java                 # 启用 MQ 注解
```

## 3. 核心接口设计

### 3.1 MqTemplate 接口

```java
/**
 * 统一消息队列操作模板
 */
public interface MqTemplate {

    /* ==================== 普通消息 ==================== */

    /**
     * 发送消息（同步）
     *
     * @param topic   主题/队列名称
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult send(String topic, Object message);

    /**
     * 发送消息（带标签）
     *
     * @param topic   主题
     * @param tag     标签（用于消息过滤）
     * @param message 消息内容
     * @return 发送结果
     */
    SendResult send(String topic, String tag, Object message);

    /**
     * 发送消息（异步）
     *
     * @param topic    主题
     * @param message  消息内容
     * @param callback 回调函数
     */
    void sendAsync(String topic, Object message, SendCallback callback);

    /**
     * 发送单向消息（不等待响应）
     *
     * @param topic   主题
     * @param message 消息内容
     */
    void sendOneWay(String topic, Object message);

    /* ==================== 延迟消息 ==================== */

    /**
     * 发送延迟消息
     *
     * @param topic      主题
     * @param message    消息内容
     * @param delayLevel 延迟级别
     * @return 发送结果
     */
    SendResult sendDelayed(String topic, Object message, DelayLevel delayLevel);

    /**
     * 发送延迟消息（自定义延迟时间）
     *
     * @param topic      主题
     * @param message    消息内容
     * @param delayTime  延迟时间
     * @param timeUnit   时间单位
     * @return 发送结果
     */
    SendResult sendDelayed(String topic, Object message, long delayTime, TimeUnit timeUnit);

    /* ==================== 顺序消息 ==================== */

    /**
     * 发送顺序消息
     *
     * @param topic     主题
     * @param message   消息内容
     * @param hashKey   分片键（用于保证顺序）
     * @return 发送结果
     */
    SendResult sendOrdered(String topic, Object message, String hashKey);

    /* ==================== 事务消息 ==================== */

    /**
     * 发送事务消息
     *
     * @param topic   主题
     * @param message 消息内容
     * @param arg     事务参数
     * @return 发送结果
     */
    SendResult sendTransaction(String topic, Object message, Object arg);
}
```

### 3.2 MqMessage 消息实体

```java
/**
 * 统一消息实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqMessage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息唯一ID
     */
    private String messageId;

    /**
     * 消息主题
     */
    private String topic;

    /**
     * 消息标签
     */
    private String tag;

    /**
     * 消息键（用于查询）
     */
    private String key;

    /**
     * 消息体
     */
    private T body;

    /**
     * 消息头
     */
    private Map<String, String> headers;

    /**
     * 延迟级别
     */
    private DelayLevel delayLevel;

    /**
     * 自定义延迟时间（毫秒）
     */
    private Long delayTime;

    /**
     * 消息发送时间
     */
    private Long sendTime;

    /**
     * 消息创建时间
     */
    private Long createTime;

    /**
     * 重试次数
     */
    private Integer retryTimes;
}
```

### 3.3 MqListener 注解

```java
/**
 * 消息监听注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MqListener {

    /**
     * 主题/队列名称
     */
    String topic();

    /**
     * 标签（用于消息过滤，RocketMQ 支持）
     */
    String tag() default "*";

    /**
     * 消费组
     */
    String group() default "";

    /**
     * 消费模式：集群/广播
     */
    ConsumeMode consumeMode() default ConsumeMode.CLUSTER;

    /**
     * 最大重试次数
     */
    int maxRetryTimes() default 3;

    /**
     * 是否顺序消费
     */
    boolean ordered() default false;

    /**
     * 并发线程数
     */
    int concurrency() default 1;
}
```

## 4. 配置设计

### 4.1 配置文件示例

```yaml
# application.yml
carlos:
  mq:
    # 启用的 MQ 类型：auto(自动检测) / rabbitmq / rocketmq / kafka
    type: auto
    
    # 是否启用 MQ
    enabled: true
    
    # 全局配置
    global:
      # 默认发送超时时间（秒）
      send-timeout: 3
      # 默认最大重试次数
      max-retry-times: 3
      # 是否启用消息轨迹
      enable-trace: true
    
    # RabbitMQ 配置
    rabbitmq:
      host: localhost
      port: 5672
      username: guest
      password: guest
      virtual-host: /
      # 确认模式：none / simple / correlated
      publisher-confirm-type: correlated
      # 是否启用返回回调
      publisher-returns: true
      # 消费者配置
      listener:
        # 并发消费者数
        concurrency: 1
        # 最大并发消费者数
        max-concurrency: 10
        # 每次预取数量
        prefetch: 1
        # 是否自动确认
        auto-ack: false
        # 重试配置
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000ms
    
    # RocketMQ 配置
    rocketmq:
      name-server: localhost:9876
      # 生产者配置
      producer:
        group: default-producer-group
        send-timeout: 3000
        retry-times-when-send-failed: 2
        retry-times-when-send-async-failed: 2
      # 消费者配置
      consumer:
        group: default-consumer-group
        consume-thread-min: 5
        consume-thread-max: 20
        max-reconsume-times: 3
        consume-timeout: 15
    
    # Kafka 配置
    kafka:
      bootstrap-servers: localhost:9092
      # 生产者配置
      producer:
        acks: all
        retries: 3
        batch-size: 16384
        buffer-memory: 33554432
        key-serializer: org.apache.kafka.common.serialization.StringSerializer
        value-serializer: org.apache.kafka.common.serialization.StringSerializer
      # 消费者配置
      consumer:
        group-id: default-consumer-group
        auto-offset-reset: earliest
        enable-auto-commit: false
        max-poll-records: 500
```

### 4.2 自动检测机制

当 `carlos.mq.type=auto` 时，按照以下优先级自动检测：

1. **RocketMQ**: 检查类路径是否存在 `org.apache.rocketmq.spring.core.RocketMQTemplate`
2. **RabbitMQ**: 检查类路径是否存在 `org.springframework.amqp.rabbit.core.RabbitTemplate`
3. **Kafka**: 检查类路径是否存在 `org.springframework.kafka.core.KafkaTemplate`

## 5. 功能特性矩阵

| 功能特性 | RabbitMQ  | RocketMQ      | Kafka       |
|------|-----------|---------------|-------------|
| 普通消息 | ✅         | ✅             | ✅           |
| 异步发送 | ✅         | ✅             | ✅           |
| 单向发送 | ✅         | ✅             | ✅           |
| 延迟消息 | ✅ (插件/死信) | ✅ (原生支持)      | ⚠️ (需要手动实现) |
| 顺序消息 | ✅         | ✅             | ✅ (分区有序)    |
| 事务消息 | ✅         | ✅             | ✅           |
| 消息过滤 | ✅ (路由键)   | ✅ (Tag/SQL92) | ❌           |
| 消息轨迹 | ⚠️ (需插件)  | ✅             | ⚠️ (需配置)    |
| 死信队列 | ✅         | ✅             | ⚠️ (需配置)    |
| 广播消费 | ✅         | ✅             | ✅           |

## 6. 使用示例

### 6.1 发送消息

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MqTemplate mqTemplate;

    public void createOrder(Order order) {
        // 保存订单...
        
        // 发送订单创建消息
        mqTemplate.send("order-topic", order);
        
        // 发送延迟消息（5分钟后检查订单状态）
        mqTemplate.sendDelayed("order-check-topic", order, DelayLevel.MINUTE_5);
        
        // 发送带标签的消息
        mqTemplate.send("order-topic", "PAID", order);
        
        // 异步发送
        mqTemplate.sendAsync("order-topic", order, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("消息发送成功: {}", sendResult.getMessageId());
            }
            
            @Override
            public void onException(Throwable e) {
                log.error("消息发送失败", e);
            }
        });
    }
}
```

### 6.2 接收消息

```java
@Component
@Slf4j
public class OrderMessageListener {

    @MqListener(topic = "order-topic", group = "order-consumer-group")
    public void onOrderMessage(Order order) {
        log.info("收到订单消息: {}", order.getOrderId());
        // 处理订单...
    }

    @MqListener(topic = "order-topic", tag = "PAID", group = "order-paid-consumer")
    public void onOrderPaidMessage(Order order) {
        log.info("收到订单支付消息: {}", order.getOrderId());
        // 处理支付...
    }

    @MqListener(topic = "order-check-topic", group = "order-check-consumer")
    public void onOrderCheckMessage(Order order) {
        log.info("检查订单状态: {}", order.getOrderId());
        // 检查订单超时...
    }
}
```

### 6.3 事务消息

```java
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final MqTemplate mqTemplate;

    @Transactional
    public void processPayment(Payment payment) {
        // 保存支付记录
        paymentMapper.insert(payment);
        
        // 发送事务消息
        mqTemplate.sendTransaction("payment-topic", payment, payment.getOrderId());
    }
}

@Component
@Slf4j
public class PaymentTransactionListener implements MqTransactionListener {

    @Override
    public LocalTransactionState executeLocalTransaction(MqMessage msg, Object arg) {
        try {
            // 执行本地事务
            Long orderId = (Long) arg;
            orderService.updatePaymentStatus(orderId);
            return LocalTransactionState.COMMIT;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public LocalTransactionState checkLocalTransaction(MqMessage msg) {
        // 回查本地事务状态
        Long orderId = msg.getKey();
        boolean paid = orderService.isPaid(orderId);
        return paid ? LocalTransactionState.COMMIT : LocalTransactionState.ROLLBACK;
    }
}
```

## 7. 异常处理

### 7.1 异常体系

```
MqException (基础异常)
├── MqSendException (发送异常)
│   ├── MqTimeoutException (超时异常)
│   └── MqRouteException (路由异常)
├── MqConsumeException (消费异常)
│   └── MqRetryExhaustedException (重试耗尽异常)
└── MqConfigException (配置异常)
```

### 7.2 消费异常处理

```java
@MqListener(topic = "order-topic", maxRetryTimes = 3)
public void onOrderMessage(Order order) {
    try {
        processOrder(order);
    } catch (BusinessException e) {
        // 业务异常，记录日志，不重试
        log.error("业务处理失败: {}", e.getMessage());
        // 对于 RabbitMQ，抛出 AmqpRejectAndDontRequeueException 避免重试
        // 对于 RocketMQ，返回 ConsumeConcurrentlyStatus.RECONSUME_LATER 控制重试
        throw new MqConsumeException(e.getMessage(), e, false);
    }
}
```

## 8. 扩展机制

### 8.1 自定义 MQ 实现

```java
/**
 * 自定义 MQ 客户端实现
 */
@Component
@ConditionalOnProperty(name = "carlos.mq.type", havingValue = "custom")
public class CustomMqClient implements MqClient {

    @Override
    public SendResult send(MqMessage message) {
        // 自定义发送逻辑
    }

    @Override
    public void subscribe(String topic, MqListener listener) {
        // 自定义订阅逻辑
    }
}
```

### 8.2 消息拦截器

```java
/**
 * 消息发送拦截器
 */
public interface MqSendInterceptor {

    /**
     * 发送前拦截
     */
    void beforeSend(MqMessage message);

    /**
     * 发送后拦截
     */
    void afterSend(MqMessage message, SendResult result);
}

/**
 * 消息消费拦截器
 */
public interface MqConsumeInterceptor {

    /**
     * 消费前拦截
     */
    void beforeConsume(MqMessage message);

    /**
     * 消费后拦截
     */
    void afterConsume(MqMessage message, ConsumeResult result);
}
```

## 9. 监控与运维

### 9.1 指标收集

```java
/**
 * MQ 指标收集器
 */
public interface MqMetricsCollector {

    /**
     * 记录发送成功
     */
    void recordSendSuccess(String topic, long latencyMs);

    /**
     * 记录发送失败
     */
    void recordSendFailure(String topic, String errorCode);

    /**
     * 记录消费成功
     */
    void recordConsumeSuccess(String topic, String group, long latencyMs);

    /**
     * 记录消费失败
     */
    void recordConsumeFailure(String topic, String group, String errorCode);
}
```

### 9.2 健康检查

```java
@Component
public class MqHealthIndicator implements HealthIndicator {

    @Autowired
    private MqTemplate mqTemplate;

    @Override
    public Health health() {
        try {
            mqTemplate.ping();
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withException(e).build();
        }
    }
}
```

## 10. 测试策略

### 10.1 单元测试

```java
@SpringBootTest
@AutoConfigureMockMq
public class MqTemplateTest {

    @Autowired
    private MqTemplate mqTemplate;

    @MockBean
    private OrderMessageListener listener;

    @Test
    public void testSendMessage() {
        Order order = new Order();
        order.setOrderId("123");
        
        SendResult result = mqTemplate.send("order-topic", order);
        
        assertTrue(result.isSuccess());
        verify(listener, timeout(5000)).onOrderMessage(any(Order.class));
    }
}
```

### 10.2 集成测试

```java
@SpringBootTest(properties = "carlos.mq.type=rabbitmq")
@Testcontainers
public class RabbitMqIntegrationTest {

    @Container
    public static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.9");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("carlos.mq.rabbitmq.host", rabbitMQ::getHost);
        registry.add("carlos.mq.rabbitmq.port", rabbitMQ::getAmqpPort);
    }
}
```

## 11. 依赖配置

### 11.1 Maven 依赖

```xml
<!-- MQ Starter -->
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mq</artifactId>
</dependency>

<!-- 可选：RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- 可选：RocketMQ -->
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
</dependency>

<!-- 可选：Kafka -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

## 12. 版本兼容性

| MQ 类型    | 支持版本 | Spring Boot 版本 |
|----------|------|----------------|
| RabbitMQ | 3.8+ | 3.x            |
| RocketMQ | 5.x  | 3.x            |
| Kafka    | 3.x  | 3.x            |
