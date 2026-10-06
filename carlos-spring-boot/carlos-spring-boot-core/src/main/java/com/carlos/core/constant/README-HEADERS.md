# HTTP 请求头常量使用指南

## 概述

`HttpHeadersConstant` 接口定义了系统中所有使用的 HTTP 请求头常量，包括链路追踪、认证授权、灰度发布等场景。

## 常量分类

### 1. 链路追踪 - 业务层

| 常量                 | 值                | 说明         |
|--------------------|------------------|------------|
| `X_REQUEST_ID`     | X-Request-Id     | 业务自定义请求标识  |
| `X_TRACE_ID`       | X-Trace-Id       | 分布式链路追踪标识  |
| `X_SPAN_ID`        | X-Span-Id        | 链路追踪跨度标识   |
| `X_PARENT_SPAN_ID` | X-Parent-Span-Id | 父级 Span 标识 |

### 2. B3 传播协议

| 常量                    | 值                 | 说明                |
|-----------------------|-------------------|-------------------|
| `B3`                  | b3                | B3 单头格式（推荐）       |
| `X_B3_TRACE_ID`       | X-B3-TraceId      | B3 Trace ID（多头部）  |
| `X_B3_SPAN_ID`        | X-B3-SpanId       | B3 Span ID（多头部）   |
| `X_B3_PARENT_SPAN_ID` | X-B3-ParentSpanId | B3 Parent Span ID |
| `X_B3_SAMPLED`        | X-B3-Sampled      | B3 采样标志           |
| `X_B3_FLAGS`          | X-B3-Flags        | B3 调试标志           |

### 3. SkyWalking

| 常量                | 值               | 说明                       |
|-------------------|-----------------|--------------------------|
| `SW8`             | sw8             | SkyWalking 8.0+ 传播协议     |
| `SW8_CORRELATION` | sw8-correlation | SkyWalking 关联ID          |
| `SW8_X`           | sw8-x           | SkyWalking 承载头           |
| `SW_TRACE_ID`     | sw-trace-id     | SkyWalking Trace ID（简化版） |

### 4. W3C 标准

| 常量            | 值           | 说明                |
|---------------|-------------|-------------------|
| `TRACEPARENT` | traceparent | W3C Trace Context |
| `TRACESTATE`  | tracestate  | W3C Trace State   |

### 5. 认证授权

| 常量              | 值             | 说明          |
|-----------------|---------------|-------------|
| `AUTHORIZATION` | Authorization | 认证令牌请求头     |
| `BEARER_PREFIX` | Bearer        | Bearer 令牌前缀 |
| `BASIC_PREFIX`  | Basic         | Basic 认证前缀  |
| `X_USER_ID`     | X-User-Id     | 用户 ID       |
| `X_USER_NAME`   | X-User-Name   | 用户名         |
| `X_TENANT_ID`   | X-Tenant-Id   | 租户 ID       |
| `X_USER_ROLES`  | X-User-Roles  | 角色信息        |

### 6. 灰度发布 / 路由

| 常量                 | 值                | 说明     |
|--------------------|------------------|--------|
| `X_GRAY_VERSION`   | X-Gray-Version   | 灰度版本标记 |
| `X_GRAY_TAG`       | X-Gray-Tag       | 灰度流量标记 |
| `X_TARGET_VERSION` | X-Target-Version | 目标服务版本 |

### 7. 网关 / 代理

| 常量                  | 值                 | 说明       |
|---------------------|-------------------|----------|
| `X_FORWARDED_FOR`   | X-Forwarded-For   | 真实客户端 IP |
| `X_FORWARDED_HOST`  | X-Forwarded-Host  | 原始主机名    |
| `X_FORWARDED_PROTO` | X-Forwarded-Proto | 原始协议     |
| `X_FORWARDED_PORT`  | X-Forwarded-Port  | 原始端口     |
| `X_REAL_IP`         | X-Real-IP         | 客户端真实 IP |

## 使用示例

### 1. Gateway 过滤器中使用

```java
import com.carlos.core.constant.HttpHeadersConstant;

@Component
public class SomeFilter implements GlobalFilter {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // 获取 Request ID
        String requestId = request.getHeaders().getFirst(HttpHeadersConstant.X_REQUEST_ID);
        
        // 设置响应头
        exchange.getResponse().getHeaders().set(
            HttpHeadersConstant.X_TRACE_ID, 
            traceId
        );
        
        return chain.filter(exchange);
    }
}
```

### 2. Feign 拦截器中使用

```java
import com.carlos.core.constant.HttpHeadersConstant;

@Component
public class FeignInterceptor implements RequestInterceptor {
    
    @Override
    public void apply(RequestTemplate template) {
        // 透传 Request ID
        template.header(HttpHeadersConstant.X_REQUEST_ID, requestId);
        
        // 透传 B3 头
        template.header(HttpHeadersConstant.B3, b3Value);
    }
}
```

### 3. Servlet 过滤器中使用

```java
import com.carlos.core.constant.HttpHeadersConstant;

@Component
public class MdcFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 获取 Request ID
        String requestId = httpRequest.getHeader(HttpHeadersConstant.X_REQUEST_ID);
        
        // 放入 MDC
        MDC.put("requestId", requestId);
        
        chain.doFilter(request, response);
    }
}
```

### 4. 配置属性中使用

```java
import com.carlos.core.constant.HttpHeadersConstant;

@Data
@ConfigurationProperties(prefix = "app.trace")
public class TraceProperties {
    
    // 使用常量作为默认值
    private String requestIdHeader = HttpHeadersConstant.X_REQUEST_ID;
    
    private String traceIdHeader = HttpHeadersConstant.X_TRACE_ID;
}
```

## 最佳实践

### 1. 始终使用常量，不硬编码

```java
// ❌ 错误
String requestId = request.getHeader("X-Request-Id");

// ✅ 正确
String requestId = request.getHeader(HttpHeadersConstant.X_REQUEST_ID);
```

### 2. 保持一致性

如果一个请求头在多个地方使用，确保使用同一个常量。这样可以避免拼写错误和大小写不一致的问题。

### 3. 新增请求头时的步骤

1. 在 `HttpHeadersConstant` 中添加常量定义
2. 添加 JavaDoc 说明用途和格式
3. 在相关代码中使用该常量
4. 更新本文档

### 4. 向后兼容

如果要修改请求头名称：

1. 保留旧常量，标记 `@Deprecated`
2. 添加新常量
3. 在代码中同时兼容新旧名称
4. 在文档中说明变更

## 变更日志

| 日期         | 变更内容                        | 影响范围 |
|------------|-----------------------------|------|
| 2025-03-26 | 创建 `HttpHeadersConstant` 接口 | 所有模块 |

## 相关文档

- [Gateway 链路追踪指南](../../../../../../carlos-integration/carlos-gateway/README-TRACING.md)
- [APM 链路追踪使用指南](../../../../../carlos-spring-boot-starter-apm/README-TRACING.md)
