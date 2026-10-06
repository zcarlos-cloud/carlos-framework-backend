# Carlos APM 链路追踪使用指南

## 概述

本模块整合了 **Micrometer Tracing**（Spring Cloud Sleuth 的继任者）和 **SkyWalking**，提供完整的分布式链路追踪能力。

## ID 类型说明

| ID 类型              | MDC Key     | 来源                    | 用途               |
|--------------------|-------------|-----------------------|------------------|
| **Request ID**     | `requestId` | 网关生成或前端传入             | 单次请求唯一标识（业务追踪）   |
| **Trace ID**       | `traceId`   | Micrometer Tracing 生成 | 分布式链路追踪（跨服务）     |
| **Span ID**        | `spanId`    | Micrometer Tracing 生成 | 单个服务内的操作单元       |
| **SkyWalking TID** | `tid`       | SkyWalking Agent 注入   | SkyWalking 可视化追踪 |

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-apm</artifactId>
</dependency>
```

### 2. 配置日志

将 `logback-spring-example.xml` 复制到业务模块的 `src/main/resources/logback-spring.xml`：

```bash
cp carlos-spring-boot-starter-apm/src/main/resources/logback-spring-example.xml \
   your-service/src/main/resources/logback-spring.xml
```

### 3. 配置文件

```yaml
# application.yml
carlos:
  apm:
    enabled: true
    mdc:
      enabled: true
      add-to-response: true
      request-id-header-name: X-Request-Id
      response-header-name: X-Trace-Id
      auto-generate-request-id: true
    sleuth:
      enabled: true
      probability: 1.0  # 采样率，1.0 表示全量
    skywalking:
      enabled: true

# Micrometer Tracing 配置
management:
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 4. 日志输出示例

```
2025-03-26 14:30:25.123 [http-nio-8080-exec-1] INFO  [a1b2c3d4e5f67890] [abc123def4567890-xyz789abc123def4] c.c.s.controller.UserController - 用户查询成功
                              ^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^
                                   Request ID       Trace ID          Span ID
```

## API 使用

### 在代码中获取 ID

```java
import com.carlos.apm.mdc.MdcUtil;

@Service
public class UserService {
    
    public void queryUser(Long id) {
        // 获取业务 Request ID
        String requestId = MdcUtil.getRequestId();
        
        // 获取链路追踪 Trace ID
        String traceId = MdcUtil.getTraceId();
        
        // 获取 Span ID
        String spanId = MdcUtil.getSpanId();
        
        log.info("[requestId={}] 查询用户，traceId={}", requestId, traceId);
    }
}
```

### 设置自定义 Request ID

```java
// 在请求开始时设置（通常在网关或 Filter 中）
MdcUtil.setRequestId("custom-request-id");

// 或者在 Filter 中从 Header 读取
String requestId = httpRequest.getHeader("X-Request-Id");
MdcUtil.setRequestId(requestId);  // 如果为空会自动生成
```

### Feign 调用

Feign 调用会自动透传：

- `X-Request-Id`: 业务 Request ID
- `b3`: Micrometer Tracing B3 头（包含 traceId-spanId-sampled）
- `sw8`: SkyWalking 上下文（如果启用）

```java
@FeignClient(name = "order-service")
public interface OrderClient {
    @GetMapping("/orders/{id}")
    Order getOrder(@PathVariable Long id);
}

// 调用时会自动携带追踪信息
Order order = orderClient.getOrder(123L);
```

## 请求头说明

### 传入头（客户端 → 服务端）

| Header         | 说明                 | 示例                                    |
|----------------|--------------------|---------------------------------------|
| `X-Request-Id` | 业务自定义请求 ID         | `a1b2c3d4e5f67890`                    |
| `b3`           | B3 单头格式            | `abc123def4567890-xyz789abc123def4-1` |
| `X-B3-TraceId` | B3 Trace ID（多头部格式） | `abc123def4567890`                    |
| `X-B3-SpanId`  | B3 Span ID（多头部格式）  | `xyz789abc123def4`                    |
| `X-B3-Sampled` | 是否采样               | `1`                                   |
| `sw8`          | SkyWalking 上下文     | `...`                                 |

### 响应头（服务端 → 客户端）

| Header         | 说明              |
|----------------|-----------------|
| `X-Request-Id` | 返回业务 Request ID |
| `X-Trace-Id`   | 返回链路追踪 Trace ID |

## 与旧版 Request ID 兼容

如果你之前使用自定义的 `requestId`（通过 MDC 设置），新方案完全兼容：

1. **原有代码无需修改**：已有的 `MDC.put("requestId", xxx)` 继续有效
2. **日志格式升级**：在 logback.xml 中添加 `%X{traceId}-%X{spanId}` 即可显示链路追踪信息
3. **自动兜底**：调用 `MdcUtil.getRequestId()` 时，如果没有自定义 requestId，会自动返回 traceId 作为兜底

### 兼容方案示例

```java
// 你的旧代码
String requestId = UUID.randomUUID().toString();
MDC.put("requestId", requestId);

// 新方案：使用 MdcUtil（推荐）
String requestId = MdcUtil.setRequestId(null);  // 自动生成

// 或者保持原有方式，MdcUtil.getRequestId() 会自动读取
String requestId = MdcUtil.getRequestId();
```

## 线程池上下文传递

如果使用线程池，需要确保 MDC 上下文正确传递：

```java
import com.alibaba.ttl.TtlRunnable;

// 使用 TransmittableThreadLocal 包装 Runnable
executor.execute(TtlRunnable.get(() -> {
    // 在线程池中也能获取到 requestId 和 traceId
    log.info("处理任务，requestId={}", MdcUtil.getRequestId());
}));
```

## Zipkin 集成

启动 Zipkin 服务：

```bash
docker run -d -p 9411:9411 openzipkin/zipkin
```

访问 `http://localhost:9411` 查看链路追踪。

## SkyWalking 集成

1. 下载并启动 SkyWalking OAP 和 UI
2. 配置应用 agent：`-javaagent:/path/to/skywalking-agent.jar`
3. 日志会自动上报到 SkyWalking

## 常见问题

### Q: Request ID 和 Trace ID 有什么区别？

**A:**

- **Request ID**: 业务层面的请求标识，可由前端或网关生成，用于业务追踪
- **Trace ID**: 分布式链路追踪标识，由 Micrometer Tracing 生成，用于跨服务调用追踪

### Q: 如果只想要 Request ID，不需要链路追踪？

**A:** 可以关闭链路追踪，只保留 MDC 功能：

```yaml
carlos:
  apm:
    sleuth:
      enabled: false
    mdc:
      enabled: true
      auto-generate-request-id: true
```

### Q: 日志中没有显示 traceId？

**A:** 检查以下几点：

1. 确认 logback-spring.xml 中包含 `%X{traceId}` 和 `%X{spanId}`
2. 确认 `carlos.apm.sleuth.enabled=true`
3. 确认 `management.tracing.enabled=true`

### Q: Feign 调用没有透传 traceId？

**A:**

1. 确认 `carlos.apm.feign.enabled=true`
2. 确认消费方引入了 `carlos-spring-boot-starter-apm`
3. 检查是否有其他 Feign 拦截器覆盖了 header

## 参考

- [Micrometer Tracing Documentation](https://micrometer.io/docs/tracing)
- [Brave B3 Propagation](https://github.com/openzipkin/b3-propagation)
- [SkyWalking Documentation](https://skywalking.apache.org/docs/)
