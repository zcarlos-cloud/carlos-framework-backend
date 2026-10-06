# carlos-spring-boot-starter-web

## 模块简介

`carlos-spring-boot-starter-web` 是 Carlos 框架的 Spring Boot 集成模块，提供了企业级的 Web
应用开发所需的自动配置、请求处理、异常处理、类型转换、CORS、XSS 防护、枚举服务、资源扫描等功能。该模块通过 Spring Boot
自动配置机制，简化了应用开发配置。

## 主要功能

### 1. 全局异常处理

**GlobalExceptionHandler** 统一处理各种异常：

```java
@RestController
public class UserController {

    @PostMapping("/user")
    public Result<User> createUser(@Valid @RequestBody UserDTO dto) {
        // 参数验证失败会被自动捕获并返回友好的错误信息
        return Result.success(userService.create(dto));
    }

    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        // 业务异常会被自动捕获并返回标准格式
        throw new ServiceException("用户不存在");
    }
}
```

**支持的异常类型**：

- `MethodArgumentNotValidException`: 参数验证错误
- `BindException`: 绑定错误
- `HttpMessageNotReadableException`: HTTP 解析错误
- `HttpMediaTypeException`: 媒体类型错误
- `GlobalException`: 自定义业务异常
- `HttpRequestMethodNotSupportedException`: HTTP 方法错误
- `Exception`: 通用异常兜底

**GlobalErrorController** 处理未映射的错误路径，返回 JSON 格式响应。

### 2. 统一响应包装

**ResponseInfoAdvice** 自动包装所有控制器响应：

```java
@RestController
public class UserController {

    // 返回值会自动包装为 Result<User>
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }

    // 已经是 Result 类型的不会重复包装
    @GetMapping("/user/list")
    public Result<List<User>> listUsers() {
        return Result.success(userService.list());
    }
}
```

**特性**：

- 自动包装普通对象为 `Result` 类型
- 过滤 Swagger/SpringDoc 请求
- 支持请求/响应日志记录
- 区分 Web API 和 RPC 调用

### 3. 请求信息管理

**RequestUtil** 提供线程安全的请求信息访问：

```java
// 获取当前请求信息
RequestInfo requestInfo = RequestUtil.getRequestInfo();

// 获取请求 ID
String requestId = requestInfo.getRequestId();

// 获取客户端 IP
String ip = requestInfo.getIp();

// 获取请求 URL
String url = requestInfo.getUrl();

// 获取 HTTP 方法
String method = requestInfo.getMethod();

// 获取用户上下文
UserContext userContext = requestInfo.getUserContext();
Long userId = userContext.getUserId();
String userName = userContext.getUserName();

// 判断是否为 Feign RPC 请求
boolean isRpc = requestInfo.isRpc();
```

**RequestInfo 包含的信息**：

- 请求 ID、URL、IP、方法、Content-Type
- 用户上下文（用户 ID、角色、部门、租户 ID）
- 应用信息（应用 ID、应用密钥、应用名称）
- 请求/响应时间

### 4. 请求过滤与包装

#### RequestWrapperFilter - 请求体缓存

支持多次读取请求体：

```java
// 在 Filter 或 Interceptor 中可以多次读取请求体
String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
// 后续仍可继续读取
String body2 = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8);
```

**支持的请求类型**：

- `application/json`
- `application/x-www-form-urlencoded`
- `multipart/form-data`

#### XssFilter - XSS 防护

自动转义 HTML 特殊字符，防止 XSS 攻击：

```yaml
carlos:
  boot:
    filters:
      xss:
        enable: true
        url-patterns: ["/*"]
        excludes: ["/api/content/*"]  # 排除富文本编辑器接口
```

**转义规则**：

- `<` → `&lt;`
- `>` → `&gt;`
- `"` → `&quot;`
- `'` → `&#x27;`
- `/` → `&#x2F;`

### 5. HTTP 拦截器

**GlobalInterceptor** 记录请求/响应日志：

```yaml
carlos:
  boot:
    interceptors:
      global-interceptor:
        format: true  # 格式化输出
        print-type: BOTH_ORDER  # 日志类型
        exclude-paths:
          - /swagger-ui/**
          - /doc.html
          - /actuator/**
        include-paths:
          - /api/**
```

**日志类型** (`printType`)：

- `REQUEST`: 仅记录请求
- `RESPONSE`: 仅记录响应
- `BOTH_ORDER`: 分别记录请求和响应
- `BOTH_TOGETHER`: 合并记录请求和响应
- `NONE`: 不记录

**日志示例**：

```
[REQUEST] [GET] /api/user/123
  IP: 192.168.1.100
  Headers: {Authorization=Bearer xxx, Content-Type=application/json}

[RESPONSE] [GET] /api/user/123
  Status: 200
  Duration: 45ms
  Body: {"code":2000,"success":true,"data":{...}}
```

### 6. 类型转换与序列化

#### 表单数据转换

自动转换表单参数为 Java 类型：

```java
@GetMapping("/search")
public Result<List<User>> search(
    @RequestParam String keyword,
    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
    @RequestParam LocalDateTime createTime
) {
    // 字符串自动转换为 Date、LocalDateTime 等类型
    return Result.success(userService.search(keyword, startDate, createTime));
}
```

**支持的转换**：

- `String` → `Date`
- `String` → `LocalDateTime`
- `String` → `LocalDate`
- `String` → `LocalTime`

#### JSON 序列化

**日期序列化**：

```java
public class User {
    // 使用默认格式 yyyy-MM-dd HH:mm:ss
    private Date createTime;

    // 自定义格式
    @JsonFormat(pattern = "yyyy/MM/dd")
    private Date birthDate;

    // LocalDateTime 自动序列化为 yyyy-MM-dd HH:mm:ss
    private LocalDateTime updateTime;
}
```

**Double 序列化**：

自动移除尾部零：

```json
{
  "price": 100.5,    // 而不是 100.50
  "amount": 200      // 而不是 200.00
}
```

### 7. CORS 配置

跨域资源共享配置：

```yaml
carlos:
  boot:
    cors:
      enable: true
      path: /**
      allowed-origins-pattens:
        - http://localhost:3000
        - https://*.example.com
      allowed-methods:
        - GET
        - POST
        - PUT
        - DELETE
        - OPTIONS
      allowed-headers:
        - Authorization
        - Content-Type
      allow-credentials: true
      exposed-headers:
        - X-Total-Count
      max-age: 86400  # 1 天
```

**特性**：

- 支持多个源
- 支持通配符模式
- 可配置允许的方法和头
- 支持凭证传递
- 可配置缓存时间

### 8. 枚举服务

自动扫描和缓存枚举定义：

```yaml
carlos:
  boot:
    enums:
      enabled: true
      scan-package:
        - com.carlos
        - com.example
```

**定义枚举**：

```java
@AppEnum
public enum UserStatus implements BaseEnum {
    ACTIVE(1, "启用"),
    INACTIVE(0, "禁用");

    private final Integer code;
    private final String desc;

    UserStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
```

**获取枚举列表**：

```bash
# 获取所有枚举
GET /application/enums

# 响应
{
  "code": 2000,
  "success": true,
  "data": {
    "UserStatus": [
      {"code": 1, "desc": "启用", "name": "ACTIVE"},
      {"code": 0, "desc": "禁用", "name": "INACTIVE"}
    ]
  }
}
```

### 9. 资源扫描

自动扫描和注册 API 资源：

```yaml
carlos:
  boot:
    resource:
      app-name: "用户管理系统"
      prefix: "user-service"
      scan-package:
        - com.carlos.controller
```

**定义资源**：

```java
@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping
    public Result<User> create(@RequestBody UserDTO dto) {
        return Result.success(userService.create(dto));
    }

    @Operation(summary = "查询用户", description = "根据 ID 查询用户")
    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
}
```

**获取资源列表**：

```bash
# 获取所有资源
GET /application/resources

# 响应
{
  "code": 2000,
  "success": true,
  "data": [
    {
      "category": "用户管理",
      "resources": [
        {
          "name": "创建用户",
          "path": "/user",
          "method": "POST",
          "description": "创建新用户"
        },
        {
          "name": "查询用户",
          "path": "/user/{id}",
          "method": "GET",
          "description": "根据 ID 查询用户"
        }
      ]
    }
  ]
}
```

### 10. 自定义验证注解

#### @Phone - 手机号验证

```java
public class UserDTO {
    @Phone(message = "手机号格式不正确")
    private String phone;
}
```

验证规则：`1[3-9]xxxxxxxxx`（中国大陆手机号）

#### @IdCard - 身份证号验证

```java
public class UserDTO {
    @IdCard(message = "身份证号格式不正确")
    private String idCard;
}
```

验证规则：18 位或 15 位身份证号

#### @EnumValid - 枚举值验证

```java
public class UserDTO {
    @EnumValid(type = UserStatusEnum.class, message = "用户状态不正确")
    private Integer status;
}
```

验证枚举字段值是否在枚举定义范围内（枚举需实现 `BaseEnum`）。

#### 验证分组

分组接口位于 `com.carlos.boot.web.validator.group`：`Create` / `Update`（均继承 `Default`）。

```java
public class UserDTO {
    @NotNull(groups = Update.class)
    private Long id;

    @NotBlank(groups = {Create.class, Update.class})
    private String name;
}

// 使用
@PostMapping
public Result<User> create(@Validated(Create.class) @RequestBody UserDTO dto) {
    // id 不需要验证
}

@PutMapping
public Result<User> update(@Validated(Update.class) @RequestBody UserDTO dto) {
    // id 和 name 都需要验证
}
```

### 11. 客户端信息提取

#### IpUtil - IP 地址提取

```java
// 获取客户端真实 IP（支持代理）
String ip = IpUtil.getIpAddr(request);

// 支持的代理头
// X-Forwarded-For
// X-Real-IP
// Proxy-Client-IP
// WL-Proxy-Client-IP
// HTTP_CLIENT_IP
// HTTP_X_FORWARDED_FOR
```

#### ClientInfoUtil - User-Agent 解析

```java
ClientInfo clientInfo = ClientInfoUtil.get(request);

// 浏览器信息
String browser = clientInfo.getBrowser();      // Chrome
String browserVersion = clientInfo.getVersion(); // 120.0.0.0
String engine = clientInfo.getEngine();         // Blink

// 操作系统信息
String os = clientInfo.getOs();                 // Windows 10
String platform = clientInfo.getPlatform();     // Windows

// 移动设备信息
boolean isMobile = clientInfo.isMobile();
String mobile = clientInfo.getMobile();         // iPhone
```

### 12. 应用启动

**ApplicationRunnerWorker** 在应用启动时打印配置信息：

```
----------------------------------------------------------
Application: user-service
Profile: dev
Context Path: /api
Admin URL: http://localhost:8080/actuator
----------------------------------------------------------
```

### 13. 静态资源配置

```yaml
carlos:
  boot:
    resource-handlers:
      - path-patterns: /static/**
        locations: classpath:/static/
      - path-patterns: /uploads/**
        locations: file:/data/uploads/
```

## 配置说明

### 完整配置示例

```yaml
carlos:
  boot:
    # CORS 配置
    cors:
      enable: true
      path: /**
      allowed-origins-pattens:
        - http://localhost:3000
        - https://*.example.com
      allowed-methods: [GET, POST, PUT, DELETE, OPTIONS]
      allowed-headers: [Authorization, Content-Type]
      allow-credentials: true
      exposed-headers: [X-Total-Count]
      max-age: 86400

    # 过滤器配置
    filters:
      # 请求包装器
      request-wrapper:
        enable: true
        name: requestWrapperFilter
        url-patterns: [/*]
        order: 1

      # XSS 防护
      xss:
        enable: true
        name: xssFilter
        url-patterns: [/*]
        excludes: [/api/content/*]
        order: 2

    # 拦截器配置
    interceptors:
      global-interceptor:
        format: true
        print-type: BOTH_ORDER
        exclude-paths:
          - /swagger-ui/**
          - /doc.html
          - /actuator/**
        include-paths:
          - /api/**

    # 枚举服务
    enums:
      enabled: true
      scan-package:
        - com.carlos
        - com.example

    # 资源扫描
    resource:
      app-name: "用户管理系统"
      prefix: "user-service"
      scan-package:
        - com.carlos.controller

    # 静态资源
    resource-handlers:
      - path-patterns: /static/**
        locations: classpath:/static/
      - path-patterns: /uploads/**
        locations: file:/data/uploads/

# Spring Boot Admin 配置
spring:
  boot:
    admin:
      client:
        url: http://localhost:8080
        instance:
          prefer-ip: true
```

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-web</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- **Spring Boot Starter Web**: Web 框架（使用 Undertow 替代 Tomcat）
- **Spring Boot Starter Validation**: 参数验证
- **Spring Boot Admin Client**: 应用监控
- **Spring Boot Actuator**: 健康检查和指标
- **Reflections**: 类路径扫描
- **Apache Commons Text**: XSS 转义
- **MapStruct**: Bean 映射
- **carlos-spring-boot-core**: 核心基础模块
- **carlos-spring-boot-starter-json**: JSON 工具
- **carlos-apm**: APM 追踪工具

## 设计模式

- **ThreadLocal 模式**: 请求上下文管理
- **过滤器链模式**: 多个过滤器处理请求
- **拦截器模式**: HTTP 拦截器处理横切关注点
- **通知模式**: `ResponseBodyAdvice` 统一响应处理
- **策略模式**: 可插拔的 `ResourceStore` 实现
- **反射模式**: 动态枚举和资源发现
- **装饰器模式**: 请求包装类缓存

## 使用场景

1. **Web API 开发**: 统一响应格式、异常处理、参数验证
2. **安全防护**: XSS 防护、CORS 配置
3. **日志记录**: 请求/响应日志、用户行为追踪
4. **前后端分离**: CORS 支持、统一响应格式
5. **微服务**: 请求追踪、服务监控
6. **API 文档**: 资源扫描、枚举服务
7. **多租户**: 租户信息提取、数据隔离

## 注意事项

1. **XSS 防护**: 富文本编辑器接口需要排除 XSS 过滤
2. **请求包装**: 会缓存请求体，大文件上传时注意内存占用
3. **响应包装**: 已经是 `Result` 类型的不会重复包装
4. **CORS 配置**: 生产环境应限制允许的源
5. **日志记录**: 敏感信息（密码、Token）应脱敏
6. **ThreadLocal**: 请求结束后会自动清理，避免内存泄漏
7. **枚举扫描**: 启动时扫描，大量枚举会影响启动速度
8. **资源扫描**: 需要使用 `@Tag` 和 `@Operation` 注解

## 版本要求

- JDK 17+
- Spring Boot 3.5.8+
- Maven 3.8+

## 相关模块

- `carlos-spring-boot-core`: 核心基础模块
- `carlos-spring-boot-starter-json`: JSON 工具
- `carlos-apm`: APM 追踪
- `carlos-gateway`: API 网关
- `carlos-auth`: OAuth2 认证授权
