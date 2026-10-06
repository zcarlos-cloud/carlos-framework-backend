# Carlos Log Starter

基于 Disruptor 的高性能异步操作日志框架，支持 SpEL 表达式、多存储器扩展。

## 特性

- **高性能异步处理** - 基于 Disruptor 无锁队列，单机支持 100万+ TPS
- **SpEL 表达式支持** - 注解中支持动态解析方法参数和返回值
- **存储接口化** - 应用可自定义存储实现（DB、ClickHouse、ES、Kafka 等）
- **多存储器支持** - 支持同时向多个存储器写入日志
- **风险评估** - 支持设置风险等级，高 Risk 操作特殊处理
- **条件记录** - 支持 SpEL 条件表达式，按需记录日志
- **批量处理** - 支持批量存储，减少网络开销

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-log</artifactId>
</dependency>
```

### 2. 基本使用

```java
@RestController
public class UserController {

    
    @PostMapping("/users")
    public User createUser(@RequestBody UserCreateParam param) {
        // 业务逻辑
        return userService.create(param);
    }

    @Log(
        title = "订单管理",
        businessType = BusinessType.UPDATE,
        operation = "'支付订单: ' + #param.orderNo",
        targetId = "#param.orderNo",
        targetType = "ORDER",
        riskLevel = 50
    )
    @PostMapping("/orders/pay")
    public Order payOrder(@RequestBody OrderPayParam param) {
        // 业务逻辑
        return orderService.pay(param);
    }
}
```

### 3. 自定义存储器

实现 `LogStorage` 接口：

```java
@Component
public class ElasticsearchLogStorage implements LogStorage {

    @Override
    public boolean store(OperationLog log) {
        // 存储到 ES
        return true;
    }

    @Override
    public boolean storeBatch(List<OperationLog> logs) {
        // 批量存储到 ES
        return true;
    }

    @Override
    public int getPriority() {
        return 10; // 优先级
    }
}
```

## 配置说明

```yaml
carlos:
  log:
    enabled: true          # 是否启用日志记录
    async: true            # 是否异步记录
    disruptor:
      ringBufferSize: 1024 # RingBuffer 大小（必须是2的幂）
      consumerCount: 2     # 消费者线程数
      useWorkPool: true    # 是否使用 WorkPool 模式
      waitStrategy: BLOCKING # 等待策略
    storage:
      defaultStorage: logging # 默认存储器
      batchSize: 100       # 批量大小
      batchTimeout: 100    # 批量超时（毫秒）
```

## @Log 注解详解

> ⚠️ **本表区分「已生效」与「已声明但尚未实现」**。下表中的「生效」列是实测结论，
> 不是设计意图 —— 此前文档把两者混为一谈，导致按文档写注解却得不到预期行为。

| 属性                  | 生效 | 说明                                       | 示例                             |
|---------------------|----|------------------------------------------|--------------------------------|
| `title`             | ✅  | 模块名称                                     | `"用户管理"`                       |
| `businessType`      | ✅  | 业务类型；`logType` 缺省时用作后缀                   | `BusinessType.INSERT`          |
| `logType`           | ✅  | 日志类型标识，建议全局唯一                            | `"USER_CREATE"`                |
| `operation`         | ✅  | 操作描述（SpEL）                                | `"'创建用户: ' + #param.username"` |
| `saveRequestParams` | ✅  | 是否保存请求参数；`MultipartFile` 入参会被自动跳过        | `true`                         |
| `saveResponseData`  | ✅  | 是否保存响应数据；返回文件流/SSE 时应保持 `false`          | `false`                        |
| `async`             | ✅  | 是否异步记录（走 Disruptor）；无 Disruptor 时自动退化为同步 | `true`                         |
| `riskLevel`         | ✅  | 风险等级（0-100）                              | `50`                           |
| `targetId`          | ✅  | 目标对象ID（SpEL）                              | `"#param.orderNo"`             |
| `targetType`        | ✅  | 目标对象类型                                   | `"ORDER"`                      |
| `targetName`        | ✅  | 目标对象名称（SpEL，可用 `#result`）                 | `"#result.name"`               |
| `bizChannel`        | ✅  | 业务渠道（SpEL）                               | `"'WEB'"`                      |
| `bizScene`          | ✅  | 业务场景（SpEL）                               | `"#param.scene"`               |
| `condition`         | ✅  | 记录条件（SpEL），为 false 时不记录且不影响业务            | `"#param.needLog == true"`     |
| `excludeParams`     | ✅  | 请求参数中要剔除的字段（见下）                          | `"password,token"`             |
| `maxParamLength`    | ✅  | 参数 JSON 截断长度                             | `4000`                         |
| `recordDuration`    | ✅  | 是否记录方法耗时                                 | `true`                         |
| `logLevel`          | ❌  | **尚未实现**：切面从不读取该值，写了不生效                  | —                              |
| `operatorType`      | ❌  | **尚未实现**：操作人类型实际由身份上下文推断，不由注解决定           | —                              |
| `syncTimeout`       | ❌  | **尚未实现**：同步发送没有超时控制                       | —                              |

### `excludeParams` 匹配规则

- 在**序列化请求参数时**剔除，被排除的字段**不会落库**（事后脱敏做不到这一点）
- 忽略大小写的**包含**匹配：排除词 `password` 同时命中 `password` 与 `newPassword`
- 同时按**形参名**剔除整个入参 —— 覆盖「敏感值以裸标量传入」的写法，如
  `(@RequestBody X param, @RequestHeader String token)` 里的 `token`
- **递归**处理嵌套对象与数组
- 默认值 `password,pwd,passwd,token,secret,credential,apikey,api_key,accesskey,privatekey,private_key`
  （含 `pwd`/`passwd` 是因为本代码库的密码字段就叫 `newPwd`/`oldPwd`/`confirmPwd`，只列 `password` 会全漏）
- ⚠️ **边界**：只按**键名**匹配，**看不进字符串值内部**。若某字段的值本身是一段 JSON
  （如消息渠道的 `channelConfig`，内含 `appSecret`），排除词对它无效 ——
  这类端点必须显式写 `saveRequestParams = false`
- `MultipartFile` 与 `MultipartFile[]` 入参会被自动跳过，无需手工排除

## SpEL 表达式

### 可用变量

- `#paramName` - 方法参数名（项目全局开启 `-parameters`，参数名可直接用）
- `#result` - 方法返回值（**仅在方法成功返回后可用**，`operation` 里不要依赖它）
- `#request` - HttpServletRequest
- `#method` - 当前方法
- `#target` - 目标对象
- `#class` - 目标类

> 解析失败不会抛异常，会把表达式原样记为描述，便于排查 —— 所以别写花哨表达式。

### 示例

```java
@Log(
    title = "订单管理",
    logType = "ORDER_PAY",
    businessType = BusinessType.UPDATE,
    operation = "'支付订单: ' + #param.orderNo + ', 金额: ' + #param.amount",
    targetId = "#param.orderNo",
    targetType = "ORDER",
    riskLevel = 70,
    condition = "#param.amount > 100"  // 只记录大额订单
)
```

## 与 Audit 模块集成

`@Log` 产生的日志最终落 ClickHouse 有**两条**链路，按服务形态选：

### 链路一：同 JVM 直写（carlos-audit 自己）

```xml
<dependency>
    <groupId>com.carlos.audit</groupId>
    <artifactId>carlos-audit-bus</artifactId>
</dependency>
```

`carlos-audit-bus` 里的 `AuditLogStorage` 是一个 `LogStorage` 实现，直接写 ClickHouse。
**只有 carlos-audit 本服务能用**，因为那是它的模块。

### 链路二：跨服务走 MQ（其他所有业务服务）

```xml
<dependency>
    <groupId>com.carlos.audit</groupId>
    <artifactId>carlos-audit-api</artifactId>
</dependency>
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-log</artifactId>
</dependency>
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mq</artifactId>
</dependency>
```

三者齐备时，`carlos-audit-api` 里的 `AuditMqLogStorage` 自动生效：把日志投递到 MQ 主题
`audit:log`，由 carlos-audit 既有的消费端落库（与 Feign 入口复用同一段 service 代码）。

**缺任何一个都不会报错，只会静默退化成「只打应用日志、ClickHouse 里没有」** ——
这是最容易踩的坑。排查时看容器里 `LogStorage` 的实现类名是不是 `AuditMqLogStorage`。
可用 `carlos.audit.mq-log-storage.enabled=false` 显式退回兜底存储器。

### 还差一步：操作人身份

审计行里的「操作人」来自 Spring Security 上下文，而该上下文只在
`carlos.resource-server.enabled=true` 时由 `UserContextAuthenticationFilter` 建立
（它校验网关签发的身份断言 `X-User-Context`）。**开关关着时审计行会是
`principalType=ANONYMOUS`、操作人为空** —— 日志还在，但没有价值。

因此业务服务需要：

```yaml
carlos:
  resource-server:
    enabled: true
```

并且本地直连服务端口调试时拿不到身份（断言由网关签名），**必须经网关调用**才有操作人。

## 版本变更

### 3.0.0

- 完全重构，基于 Disruptor 的高性能异步处理
- 支持 SpEL 表达式
- 存储接口化，支持多存储器
- 兼容旧 API（已标记为弃用）
