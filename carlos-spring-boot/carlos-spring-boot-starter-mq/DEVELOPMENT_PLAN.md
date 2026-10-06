# Carlos MQ 模块开发计划

## 项目概述

- **模块名称**: carlos-spring-boot-starter-mq
- **当前状态**: 已有基础结构，需要重构完善
- **目标版本**: 3.0.0-SNAPSHOT
- **计划工期**: 4 周

---

## 一、现状分析

### 1.1 现有代码

```
carlos-spring-boot-starter-mq
├── pom.xml                          # 依赖已引入 RocketMQ、Kafka
├── src/main/java/com/carlos/mq
│   ├── config
│   │   └── MessageConfig.java       # 空配置类，需重构
│   ├── exception
│   │   ├── MessageException.java    # 基础异常
│   │   ├── MessageListenException.java
│   │   └── MessageSendException.java
│   ├── support
│   │   ├── DelayLevel.java          # 延迟级别枚举（RocketMQ）
│   │   └── SendType.java            # 发送类型枚举
│   └── utils                        # 空目录，需创建工具类
└── resources/META-INF/spring
    └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

### 1.2 存在问题

1. ❌ 缺少统一的 `MqTemplate` 接口
2. ❌ 缺少自动检测机制
3. ❌ 缺少 RabbitMQ 实现
4. ❌ 工具类为空
5. ❌ 配置属性不完善
6. ❌ 缺少消息监听注解

---

## 二、开发阶段规划

### 阶段一：核心抽象层搭建（Week 1）

**目标**：建立统一的 MQ 抽象接口和基础实体

#### Week 1 Day 1-2: 接口定义

| 任务                    | 文件路径                      | 说明      |
|-----------------------|---------------------------|---------|
| ✅ 设计 MqTemplate 接口    | `core/MqTemplate.java`    | 统一发送接口  |
| ✅ 设计 MqClient 接口      | `core/MqClient.java`      | 底层客户端抽象 |
| ✅ 设计 MqMessage 实体     | `core/MqMessage.java`     | 统一消息格式  |
| ✅ 设计 SendResult 实体    | `core/SendResult.java`    | 发送结果    |
| ✅ 设计 ConsumeResult 实体 | `core/ConsumeResult.java` | 消费结果    |

#### Week 1 Day 3-4: 支持类与枚举

| 任务                            | 文件路径                                 | 说明       |
|-------------------------------|--------------------------------------|----------|
| 🔄 重构 DelayLevel 枚举           | `support/DelayLevel.java`            | 扩展通用延迟级别 |
| 🔄 重构 SendType 枚举             | `support/SendType.java`              | 统一发送类型   |
| ✅ 创建 MqType 枚举                | `support/MqType.java`                | MQ 类型枚举  |
| ✅ 创建 ConsumeMode 枚举           | `support/ConsumeMode.java`           | 消费模式     |
| ✅ 创建 LocalTransactionState 枚举 | `support/LocalTransactionState.java` | 事务状态     |

#### Week 1 Day 5: 回调与监听接口

| 任务                            | 文件路径                                    | 说明    |
|-------------------------------|-----------------------------------------|-------|
| ✅ 创建 SendCallback 接口          | `core/SendCallback.java`                | 发送回调  |
| ✅ 创建 MqTransactionListener 接口 | `core/MqTransactionListener.java`       | 事务监听  |
| ✅ 创建 MqSendInterceptor 接口     | `interceptor/MqSendInterceptor.java`    | 发送拦截器 |
| ✅ 创建 MqConsumeInterceptor 接口  | `interceptor/MqConsumeInterceptor.java` | 消费拦截器 |

**阶段一产出物**：

```
com.carlos.mq
├── core
│   ├── MqTemplate.java
│   ├── MqClient.java
│   ├── MqMessage.java
│   ├── SendResult.java
│   ├── ConsumeResult.java
│   ├── SendCallback.java
│   └── MqTransactionListener.java
├── support
│   ├── MqType.java
│   ├── SendType.java
│   ├── DelayLevel.java
│   ├── ConsumeMode.java
│   └── LocalTransactionState.java
└── interceptor
    ├── MqSendInterceptor.java
    └── MqConsumeInterceptor.java
```

---

### 阶段二：配置与自动装配（Week 2）

**目标**：实现配置属性与自动装配机制

#### Week 2 Day 1-2: 配置属性

| 任务                      | 文件路径                             | 说明          |
|-------------------------|----------------------------------|-------------|
| ✅ 创建 MqProperties       | `config/MqProperties.java`       | 主配置属性       |
| ✅ 创建 RabbitMqProperties | `config/RabbitMqProperties.java` | RabbitMQ 配置 |
| ✅ 创建 RocketMqProperties | `config/RocketMqProperties.java` | RocketMQ 配置 |
| ✅ 创建 KafkaMqProperties  | `config/KafkaMqProperties.java`  | Kafka 配置    |

#### Week 2 Day 3-4: 自动配置

| 任务                             | 文件路径                                    | 说明            |
|--------------------------------|-----------------------------------------|---------------|
| ✅ 重构 MqAutoConfiguration       | `config/MqAutoConfiguration.java`       | 主自动配置         |
| ✅ 创建 RabbitMqAutoConfiguration | `config/RabbitMqAutoConfiguration.java` | RabbitMQ 自动配置 |
| ✅ 创建 RocketMqAutoConfiguration | `config/RocketMqAutoConfiguration.java` | RocketMQ 自动配置 |
| ✅ 创建 KafkaMqAutoConfiguration  | `config/KafkaMqAutoConfiguration.java`  | Kafka 自动配置    |
| ✅ 创建 MqClientSelector          | `config/MqClientSelector.java`          | MQ 客户端选择器     |

#### Week 2 Day 5: 条件装配与检测

| 任务                       | 文件路径                                          | 说明        |
|--------------------------|-----------------------------------------------|-----------|
| ✅ 创建 ConditionalOnMqType | `config/conditional/ConditionalOnMqType.java` | MQ 类型条件注解 |
| ✅ 创建 OnMqTypeCondition   | `config/conditional/OnMqTypeCondition.java`   | MQ 类型条件判断 |
| ✅ 更新 spring.factories    | `resources/META-INF/spring/...imports`        | 注册自动配置    |

**阶段二产出物**：

```
com.carlos.mq.config
├── MqProperties.java
├── RabbitMqProperties.java
├── RocketMqProperties.java
├── KafkaMqProperties.java
├── MqAutoConfiguration.java
├── RabbitMqAutoConfiguration.java
├── RocketMqAutoConfiguration.java
├── KafkaMqAutoConfiguration.java
├── MqClientSelector.java
└── conditional
    ├── ConditionalOnMqType.java
    └── OnMqTypeCondition.java
```

---

### 阶段三：MQ 实现层（Week 3）

**目标**：实现三种 MQ 的具体客户端

#### Week 3 Day 1-2: RabbitMQ 实现

| 任务                           | 文件路径                                           | 说明             |
|------------------------------|------------------------------------------------|----------------|
| ✅ 创建 RabbitMqClient          | `client/rabbitmq/RabbitMqClient.java`          | RabbitMQ 客户端实现 |
| ✅ 创建 RabbitMqTemplate        | `client/rabbitmq/RabbitMqTemplate.java`        | RabbitMQ 模板实现  |
| ✅ 创建 RabbitMqListenerAdapter | `client/rabbitmq/RabbitMqListenerAdapter.java` | 监听器适配器         |
| ✅ 创建 RabbitMqConfigHelper    | `client/rabbitmq/RabbitMqConfigHelper.java`    | 配置辅助类          |

#### Week 3 Day 3-4: RocketMQ 实现

| 任务                           | 文件路径                                           | 说明             |
|------------------------------|------------------------------------------------|----------------|
| ✅ 创建 RocketMqClient          | `client/rocketmq/RocketMqClient.java`          | RocketMQ 客户端实现 |
| ✅ 创建 RocketMqTemplate        | `client/rocketmq/RocketMqTemplate.java`        | RocketMQ 模板实现  |
| ✅ 创建 RocketMqListenerAdapter | `client/rocketmq/RocketMqListenerAdapter.java` | 监听器适配器         |
| ✅ 创建 RocketMqConfigHelper    | `client/rocketmq/RocketMqConfigHelper.java`    | 配置辅助类          |

#### Week 3 Day 5: Kafka 实现

| 任务                          | 文件路径                                       | 说明          |
|-----------------------------|--------------------------------------------|-------------|
| ✅ 创建 KafkaMqClient          | `client/kafka/KafkaMqClient.java`          | Kafka 客户端实现 |
| ✅ 创建 KafkaMqTemplate        | `client/kafka/KafkaMqTemplate.java`        | Kafka 模板实现  |
| ✅ 创建 KafkaMqListenerAdapter | `client/kafka/KafkaMqListenerAdapter.java` | 监听器适配器      |
| ✅ 创建 KafkaMqConfigHelper    | `client/kafka/KafkaMqConfigHelper.java`    | 配置辅助类       |

**阶段三产出物**：

```
com.carlos.mq.client
├── rabbitmq
│   ├── RabbitMqClient.java
│   ├── RabbitMqTemplate.java
│   ├── RabbitMqListenerAdapter.java
│   └── RabbitMqConfigHelper.java
├── rocketmq
│   ├── RocketMqClient.java
│   ├── RocketMqTemplate.java
│   ├── RocketMqListenerAdapter.java
│   └── RocketMqConfigHelper.java
└── kafka
    ├── KafkaMqClient.java
    ├── KafkaMqTemplate.java
    ├── KafkaMqListenerAdapter.java
    └── KafkaMqConfigHelper.java
```

---

### 阶段四：注解与工具类（Week 4）

**目标**：提供便捷的注解支持和工具类

#### Week 4 Day 1-2: 注解支持

| 任务                                         | 文件路径                                                    | 说明       |
|--------------------------------------------|---------------------------------------------------------|----------|
| ✅ 创建 MqListener 注解                         | `annotation/MqListener.java`                            | 消息监听注解   |
| ✅ 创建 EnableMq 注解                           | `annotation/EnableMq.java`                              | 启用 MQ 注解 |
| ✅ 创建 MqListenerAnnotationBeanPostProcessor | `annotation/MqListenerAnnotationBeanPostProcessor.java` | 注解处理器    |
| ✅ 创建 MqListenerEndpoint                    | `annotation/MqListenerEndpoint.java`                    | 监听端点     |

#### Week 4 Day 3: 工具类

| 任务                      | 文件路径                           | 说明        |
|-------------------------|--------------------------------|-----------|
| ✅ 创建 MqUtil             | `util/MqUtil.java`             | MQ 工具类    |
| ✅ 创建 MqMessageBuilder   | `util/MqMessageBuilder.java`   | 消息构建器     |
| ✅ 创建 MessageIdGenerator | `util/MessageIdGenerator.java` | 消息 ID 生成器 |

#### Week 4 Day 4: 异常体系完善

| 任务                           | 文件路径                                | 说明               |
|------------------------------|-------------------------------------|------------------|
| 🔄 重构 MessageException       | `exception/MqException.java`        | 重命名为 MqException |
| 🔄 重构 MessageSendException   | `exception/MqSendException.java`    | 重命名并扩展           |
| 🔄 重构 MessageListenException | `exception/MqConsumeException.java` | 重命名并扩展           |
| ✅ 创建 MqTimeoutException      | `exception/MqTimeoutException.java` | 超时异常             |
| ✅ 创建 MqConfigException       | `exception/MqConfigException.java`  | 配置异常             |

#### Week 4 Day 5: 监控与扩展

| 任务                       | 文件路径                              | 说明            |
|--------------------------|-----------------------------------|---------------|
| ✅ 创建 MqMetricsCollector  | `metrics/MqMetricsCollector.java` | 指标收集器         |
| ✅ 创建 MqHealthIndicator   | `health/MqHealthIndicator.java`   | 健康检查          |
| ✅ 创建 CompositeMqTemplate | `core/CompositeMqTemplate.java`   | 组合模板（多 MQ 支持） |

**阶段四产出物**：

```
com.carlos.mq
├── annotation
│   ├── MqListener.java
│   ├── EnableMq.java
│   ├── MqListenerAnnotationBeanPostProcessor.java
│   └── MqListenerEndpoint.java
├── util
│   ├── MqUtil.java
│   ├── MqMessageBuilder.java
│   └── MessageIdGenerator.java
├── exception
│   ├── MqException.java
│   ├── MqSendException.java
│   ├── MqConsumeException.java
│   ├── MqTimeoutException.java
│   └── MqConfigException.java
├── metrics
│   └── MqMetricsCollector.java
├── health
│   └── MqHealthIndicator.java
└── core
    └── CompositeMqTemplate.java
```

---

## 三、详细任务清单

### 3.1 第一周任务清单

| 序号  | 任务                          | 优先级 | 预估工时 | 负责人 | 状态  |
|-----|-----------------------------|-----|------|-----|-----|
| 1.1 | 设计 MqTemplate 核心接口          | P0  | 4h   | -   | 待开始 |
| 1.2 | 设计 MqClient 抽象接口            | P0  | 4h   | -   | 待开始 |
| 1.3 | 设计 MqMessage 消息实体           | P0  | 4h   | -   | 待开始 |
| 1.4 | 设计 SendResult/ConsumeResult | P0  | 2h   | -   | 待开始 |
| 1.5 | 重构 DelayLevel 枚举            | P1  | 2h   | -   | 待开始 |
| 1.6 | 创建 MqType/ConsumeMode 枚举    | P1  | 2h   | -   | 待开始 |
| 1.7 | 设计回调与拦截器接口                  | P1  | 4h   | -   | 待开始 |
| 1.8 | 编写接口文档和注释                   | P2  | 2h   | -   | 待开始 |

### 3.2 第二周任务清单

| 序号  | 任务                    | 优先级 | 预估工时 | 负责人 | 状态  |
|-----|-----------------------|-----|------|-----|-----|
| 2.1 | 创建 MqProperties 配置    | P0  | 4h   | -   | 待开始 |
| 2.2 | 创建 RabbitMqProperties | P0  | 4h   | -   | 待开始 |
| 2.3 | 创建 RocketMqProperties | P0  | 4h   | -   | 待开始 |
| 2.4 | 创建 KafkaMqProperties  | P0  | 4h   | -   | 待开始 |
| 2.5 | 实现自动配置类               | P0  | 8h   | -   | 待开始 |
| 2.6 | 实现 MQ 类型检测逻辑          | P1  | 4h   | -   | 待开始 |
| 2.7 | 注册自动配置到 SPI           | P0  | 2h   | -   | 待开始 |

### 3.3 第三周任务清单

| 序号  | 任务                         | 优先级 | 预估工时 | 负责人 | 状态  |
|-----|----------------------------|-----|------|-----|-----|
| 3.1 | 实现 RabbitMqClient          | P0  | 8h   | -   | 待开始 |
| 3.2 | 实现 RabbitMqTemplate        | P0  | 6h   | -   | 待开始 |
| 3.3 | 实现 RabbitMqListenerAdapter | P0  | 4h   | -   | 待开始 |
| 3.4 | 实现 RocketMqClient          | P0  | 8h   | -   | 待开始 |
| 3.5 | 实现 RocketMqTemplate        | P0  | 6h   | -   | 待开始 |
| 3.6 | 实现 RocketMqListenerAdapter | P0  | 4h   | -   | 待开始 |
| 3.7 | 实现 KafkaMqClient           | P1  | 6h   | -   | 待开始 |
| 3.8 | 实现 KafkaMqTemplate         | P1  | 4h   | -   | 待开始 |

### 3.4 第四周任务清单

| 序号  | 任务               | 优先级 | 预估工时 | 负责人 | 状态  |
|-----|------------------|-----|------|-----|-----|
| 4.1 | 实现 MqListener 注解 | P0  | 6h   | -   | 待开始 |
| 4.2 | 实现注解处理器          | P0  | 8h   | -   | 待开始 |
| 4.3 | 创建 MqUtil 工具类    | P0  | 4h   | -   | 待开始 |
| 4.4 | 重构异常体系           | P1  | 4h   | -   | 待开始 |
| 4.5 | 实现监控指标收集         | P2  | 4h   | -   | 待开始 |
| 4.6 | 实现健康检查           | P2  | 2h   | -   | 待开始 |
| 4.7 | 编写单元测试           | P1  | 8h   | -   | 待开始 |
| 4.8 | 更新 README 文档     | P1  | 4h   | -   | 待开始 |

---

## 四、技术要点

### 4.1 自动检测实现

```java
@Slf4j
public class MqClientSelector {

    private static final Map<MqType, Supplier<Boolean>> DETECTORS = Map.of(
        MqType.ROCKETMQ, () -> ClassUtils.isPresent(
            "org.apache.rocketmq.spring.core.RocketMQTemplate", 
            MqClientSelector.class.getClassLoader()),
        MqType.RABBITMQ, () -> ClassUtils.isPresent(
            "org.springframework.amqp.rabbit.core.RabbitTemplate",
            MqClientSelector.class.getClassLoader()),
        MqType.KAFKA, () -> ClassUtils.isPresent(
            "org.springframework.kafka.core.KafkaTemplate",
            MqClientSelector.class.getClassLoader())
    );

    public static MqType detectMqType() {
        for (Map.Entry<MqType, Supplier<Boolean>> entry : DETECTORS.entrySet()) {
            if (entry.getValue().get()) {
                log.info("Detected MQ type: {}", entry.getKey());
                return entry.getKey();
            }
        }
        throw new MqConfigException("No supported MQ implementation found in classpath");
    }
}
```

### 4.2 延迟消息实现策略

| MQ 类型    | 实现方式       | 说明                                           |
|----------|------------|----------------------------------------------|
| RabbitMQ | 死信队列 + TTL | 通过 x-dead-letter-exchange 和 x-message-ttl 实现 |
| RocketMQ | 原生延迟级别     | 使用内置的 18 个延迟级别                               |
| Kafka    | 时间轮算法      | 自定义实现，通过定时任务扫描                               |

### 4.3 关键设计决策

1. **统一消息格式**：所有 MQ 统一使用 `MqMessage<T>` 作为消息载体
2. **自动配置策略**：根据 classpath 自动检测，优先级 RocketMQ > RabbitMQ > Kafka
3. **异常转换**：底层 MQ 异常统一转换为 `MqException` 体系
4. **事务消息**：优先保证 RocketMQ 原生事务，RabbitMQ 使用 Confirm 机制模拟

---

## 五、测试计划

### 5.1 单元测试

| 测试类                  | 测试内容        | 覆盖率目标 |
|----------------------|-------------|-------|
| MqMessageTest        | 消息实体序列化     | 90%   |
| MqPropertiesTest     | 配置属性绑定      | 90%   |
| MqClientSelectorTest | 自动检测逻辑      | 95%   |
| RabbitMqTemplateTest | RabbitMQ 发送 | 85%   |
| RocketMqTemplateTest | RocketMQ 发送 | 85%   |

### 5.2 集成测试

| 测试类                     | 测试内容    | 环境要求           |
|-------------------------|---------|----------------|
| RabbitMqIntegrationTest | 完整收发流程  | Testcontainers |
| RocketMqIntegrationTest | 完整收发流程  | Testcontainers |
| KafkaMqIntegrationTest  | 完整收发流程  | Testcontainers |
| MqSwitchTest            | MQ 切换测试 | 多环境配置          |

### 5.3 性能测试

| 测试项   | 指标   | 目标          |
|-------|------|-------------|
| 发送吞吐量 | 消息/秒 | > 10000 TPS |
| 消费延迟  | P99  | < 100ms     |
| 内存占用  | 堆内存  | < 512MB     |

---

## 六、依赖更新

### 6.1 carlos-dependencies/pom.xml 更新

```xml
<!-- 添加 RabbitMQ 版本管理 -->
<properties>
    <rabbitmq-client.version>5.18.0</rabbitmq-client.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- RabbitMQ -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
        
        <!-- RocketMQ 版本已在管理 -->
        
        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
            <version>${spring-boot.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 6.2 carlos-spring-boot-starter-mq/pom.xml 更新

```xml
<dependencies>
    <!-- 核心依赖 -->
    <dependency>
        <groupId>com.carlos</groupId>
        <artifactId>carlos-spring-boot-core</artifactId>
    </dependency>
    
    <!-- RabbitMQ - provided，用户按需引入 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
    
    <!-- RocketMQ - provided -->
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
    
    <!-- Kafka - provided -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 七、风险与应对

| 风险          | 影响 | 概率 | 应对措施                      |
|-------------|----|----|---------------------------|
| 不同 MQ 语义差异大 | 高  | 高  | 定义清晰的抽象层，文档说明限制           |
| 延迟消息实现复杂    | 中  | 中  | RabbitMQ 使用插件，Kafka 使用时间轮 |
| 事务消息支持不一    | 高  | 中  | 优先支持 RocketMQ，其他模拟        |
| 测试环境搭建复杂    | 中  | 高  | 使用 Testcontainers         |
| 版本兼容性问题     | 中  | 中  | CI 矩阵测试多版本                |

---

## 八、交付物清单

### 8.1 代码交付物

- [ ] 核心抽象层代码（接口 + 实体）
- [ ] 配置与自动装配代码
- [ ] RabbitMQ 实现代码
- [ ] RocketMQ 实现代码
- [ ] Kafka 实现代码
- [ ] 注解与工具类
- [ ] 监控与健康检查

### 8.2 文档交付物

- [ ] 功能设计文档（MQ_MODULE_DESIGN.md）
- [ ] 开发计划文档（DEVELOPMENT_PLAN.md）
- [ ] API 使用文档（README.md）
- [ ] 配置参考文档（CONFIG_REFERENCE.md）
- [ ] 迁移指南（MIGRATION_GUIDE.md）

### 8.3 测试交付物

- [ ] 单元测试代码
- [ ] 集成测试代码
- [ ] 性能测试报告
- [ ] 覆盖率报告

---

## 九、里程碑

| 里程碑 | 日期        | 交付内容       | 验收标准      |
|-----|-----------|------------|-----------|
| M1  | Week 1 结束 | 核心接口定义完成   | 代码评审通过    |
| M2  | Week 2 结束 | 自动配置完成     | 可切换 MQ 类型 |
| M3  | Week 3 结束 | 三种 MQ 实现完成 | 单元测试通过    |
| M4  | Week 4 结束 | 完整功能交付     | 集成测试通过    |

---

## 十、附录

### 10.1 参考资料

- [RabbitMQ Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.amqp)
- [RocketMQ Spring Boot](https://github.com/apache/rocketmq-spring)
- [Kafka Spring Boot](https://docs.spring.io/spring-boot/docs/current/reference/html/messaging.html#messaging.kafka)

### 10.2 命名约定

| 类型  | 命名规范          | 示例                   |
|-----|---------------|----------------------|
| 接口  | 名词，Mq 前缀      | MqTemplate, MqClient |
| 实现类 | 接口名 + Impl    | RabbitMqTemplate     |
| 配置类 | Properties 后缀 | RabbitMqProperties   |
| 异常类 | Exception 后缀  | MqSendException      |
| 工具类 | Util 后缀       | MqUtil               |
| 枚举类 | 名词，Enum 后缀    | MqTypeEnum           |

### 10.3 提交规范

```
feat(mq): 添加 RabbitMQ 客户端实现

- 实现 RabbitMqClient 接口
- 实现延迟消息（死信队列方式）
- 添加单元测试

Closes #123
```
