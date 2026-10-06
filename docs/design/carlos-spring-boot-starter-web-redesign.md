# carlos-spring-boot-starter-web 模块重新设计方案

> **文档版本**: v1.0  
> **日期**: 2026-06-17  
> **作者**: Carlos  
> **状态**: 待评审

---

## 目录

1. [概述与动机](#1-概述与动机)
2. [现状分析](#2-现状分析)
3. [问题与差距 (SB 2.7.x → 4.0.6)](#3-问题与差距)
4. [重构目标与原则](#4-重构目标与原则)
5. [目标架构设计](#5-目标架构设计)
6. [模块详细设计](#6-模块详细设计)
7. [配置属性设计](#7-配置属性设计)
8. [迁移计划](#8-迁移计划)
9. [风险评估](#9-风险评估)
10. [测试策略](#10-测试策略)

---

## 1. 概述与动机

### 1.1 背景

`carlos-spring-boot-starter-web` 是 Carlos Framework 的核心 Starter 模块之一，承载了 Web
应用的基础设施能力：统一响应包装、全局异常处理、CORS、参数校验、XSS 防护、请求日志、枚举字典、资源扫描等。

该模块最初设计于 **2019-2022 年**（Spring Boot 2.3~2.7 时代），此后虽经历多次小修小补（如 Jakarta 迁移、Virtual Thread 支持），但
**整体架构从未系统性地适配 Spring Boot 4.x**。框架于 2026-04-27 完成 Spring Boot 4.0.6 升级后，该模块暴露出大量历史债务。

### 1.2 核心动机

| 动机          | 说明                                                                               |
|-------------|----------------------------------------------------------------------------------|
| **API 兼容性** | Spring Boot 4.x 废弃/移除了多个 2.7 时代的 API                                             |
| **内置能力利用**  | SB 4.0 内置 Virtual Thread、Problem Details (RFC 9457)、Micrometer Tracing，当前模块重复造轮子 |
| **代码质量**    | 存在 3 个功能重叠的 Request Wrapper、多处冗余配置、静态可变状态                                        |
| **安全加固**    | XSS 过滤、CORS 配置需对齐 OWASP 2025 最新建议                                                |
| **可维护性**    | 70+ 文件分散在 14 个包中，缺乏清晰的分层和职责边界                                                    |

### 1.3 设计范围

**In Scope:**

- 全部 70+ Java 源文件的重构或重写
- 配置属性体系重新设计
- POM 依赖清理与升级
- logback 配置现代化

**Out of Scope:**

- carlos-spring-boot-core 模块修改
- carlos-spring-boot-starter-json 模块修改
- 业务集成模块（auth/system/org 等）的改动

---

## 2. 现状分析

### 2.1 当前包结构

```
com.carlos.boot
├── annotation/          # @NoWrap
├── application/         # ApplicationConfig, ApplicationWebMvcConfig, ApplicationProperties, ApplicationController
├── converter/           # ApplicationConverterConfig, DateJsonComponent, DoubleJsonComponent, LocalDateTimeDateComponent, SpringBootFormDataConverter
├── cors/                # ApplicationCorsConfig, ApplicationCorsProperties
├── enums/               # ApplicationEnumConfig, ApplicationEnumProperties, EnumService
├── error/               # ErrorConfig, GlobalErrorController, CustomizeErrorAttributes
├── filters/             # ApplicationFilterConfig, ApplicationFilterProperties, RequestWrapperFilter + 3 个 Wrapper
│   └── xss/             # XssFilter, XssHttpServletRequestWrapper
├── interceptors/        # ApplicationInterceptorConfig, ApplicationInterceptorProperties, GlobalInterceptor, GlobalInterceptorProperties
├── request/             # RequestUtil, RequestInfo, ClientInfo, ClientInfoUtil, IpUtil
├── resource/            # ApplicationResourceConfig, ApplicationResourceProperties, ResourceService, ResourceStore, DefaultResourceStore, ResourceController, ResourceMethod
│   └── bean/            # Resource, ApplicationResource, ResourceCategory
├── response/            # ResponseConfig, ResponseProperties, ResponseWrapperAdvice
├── runner/              # ApplicationRunnerConfig, ApplicationRunnerWorker
├── thread/              # VirtualThreadConfig, TomcatVirtualThreadConfig
├── util/                # ExtendInfoUtil, ResponseUtil
├── validator/           # EnumValidator, IdCardValidator, PhoneValidator
│   ├── constraints/     # @EnumType, @IdCard, @Phone
│   └── groups/          # Add, Update
├── BootConstant.java
└── GlobalExceptionHandler.java
```

### 2.2 当前依赖

```xml
<dependencies>
    <!-- 框架内部 -->
    carlos-spring-boot-core          (核心框架)
    carlos-spring-boot-starter-json  (JSON 序列化)
    carlos-spring-boot-starter-apm   (APM/SkyWalking)
    carlos-spring-boot-starter-translation (字段翻译)

    <!-- Spring Boot -->
    spring-boot-starter-web          (Web 能力)
    spring-boot-starter-aspectj      (AOP)
    spring-boot-starter-validation   (参数校验)
    spring-boot-starter-actuator     (健康监控)

    <!-- 第三方 -->
    arthas-spring-boot-starter       (诊断工具)
    spring-boot-admin-starter-client (监控客户端)
    reflections                      (反射扫描)
    commons-text                     (文本处理 - XSS)
    mapstruct / mapstruct-processor  (对象转换)
    lombok-mapstruct-binding
</dependencies>
```

### 2.3 当前 AutoConfiguration 注册

```
com.carlos.boot.application.ApplicationWebMvcConfig
com.carlos.boot.application.ApplicationConfig
com.carlos.boot.converter.ApplicationConverterConfig
com.carlos.boot.cors.ApplicationCorsConfig
com.carlos.boot.error.ErrorConfig
com.carlos.boot.filters.ApplicationFilterConfig
com.carlos.boot.interceptors.ApplicationInterceptorConfig
com.carlos.boot.enums.ApplicationEnumConfig
com.carlos.boot.resource.ApplicationResourceConfig
com.carlos.boot.runner.ApplicationRunnerConfig
com.carlos.boot.response.ResponseConfig
com.carlos.boot.thread.VirtualThreadConfig
com.carlos.boot.thread.TomcatVirtualThreadConfig
```

---

## 3. 问题与差距

### 3.1 P0 - 严重问题（阻塞发布）

| #    | 问题                                                    | 影响                                                                                                                                                                    | 文件                           |
|------|-------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------|
| P0-1 | **3 个 Request Wrapper 功能重叠**                          | `GlobalHttpServletRequestWrapper`、`GlobalBodyHttpServletRequestWrapper`、`GlobalParamHttpServletRequestWrapper` 功能高度重叠，`GlobalHttpServletRequestWrapper` 内包含大量注释掉的无用代码 | 3 个 Wrapper 文件               |
| P0-2 | **mapstruct-processor 作为编译依赖而非 annotation processor** | 污染运行时类路径，导致构建警告                                                                                                                                                       | pom.xml                      |
| P0-3 | **静态可变状态 (Static Mutable State)**                     | `ExtendInfoUtil` 使用静态字段 `extend` 持有 ApplicationExtend 实例，非线程安全初始化                                                                                                     | `ExtendInfoUtil.java`        |
| P0-4 | **Virtual Thread 配置与 SB 4.0 内置机制冲突**                  | `VirtualThreadConfig` 的 `ConditionalOnProperty` 缺少与 `spring.threads.virtual.enabled` 的退避条件                                                                            | `VirtualThreadConfig.java`   |
| P0-5 | **ErrorController 使用 `@RequestMapping` 无效路径**         | `${server.error.path:${error.path:/error}}` 在 SB 4.x 中已弃用 `error.path`                                                                                                | `GlobalErrorController.java` |

### 3.2 P1 - 高优问题

| #    | 问题                                                  | 说明                                                                                                                            |
|------|-----------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| P1-1 | **Reflections 库的使用**                                | `EnumService` 和 `ResourceService` 使用 `org.reflections` 扫描类路径，Spring 自身的 `ClassPathScanningCandidateComponentProvider` 更高效且已内置 |
| P1-2 | **CORS 配置未遵循 SB 4.x 最佳实践**                          | 使用 `FilterRegistrationBean<CorsFilter>` 手动注册，SB 4.x 推荐直接通过 WebMvcConfigurer#addCorsMappings() 配置                              |
| P1-3 | **ResponseWrapperAdvice 过滤 Swagger 的方式低效**          | 通过包名前缀硬编码判断，应使用更通用的机制                                                                                                         |
| P1-4 | **logback.xml 中 GRPCLogClientAppender 引用旧版**        | `org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender` - 需确认 SkyWalking 9.5.0 兼容性                     |
| P1-5 | **ApplicationRunnerWorker 访问静态 ApplicationContext** | `BootUtil.getPropertyValue()` 通过静态持有者获取上下文，违反 Spring 生命周期                                                                     |
| P1-6 | **GlobalInterceptor 打印逻辑与全局异常处理器重复**                | `GlobalInterceptor.afterCompletion` 和 `GlobalExceptionHandler.printRequestDetail` 都会打印请求信息                                    |

### 3.3 P2 - 中优问题

| #    | 问题                                       | 说明                                                                                          |
|------|------------------------------------------|---------------------------------------------------------------------------------------------|
| P2-1 | **配置前缀不一致**                              | `carlos.boot`、`carlos.boot.cors`、`carlos.boot.enums`、`carlos.boot.response` 等分散，部分属性嵌套层级不合理 |
| P2-2 | **Jackson 组件使用了 `tools.jackson.*` 命名空间** | `DateJsonComponent` 使用了 Fastjson 2 的 Jackson 兼容包，应统一为标准 Jackson API 或明确隔离                   |
| P2-3 | **XSS 过滤器默认关闭但 Filters 框架默认全开**          | `RequestWrapperFilter` 默认开启（enable=true），`XssFilter` 需显式开启，行为不一致                            |
| P2-4 | **ApplicationCorsProperties 有多余分号**      | `allowedMethods`、`exposedHeaders` 字段定义后有重复的分号                                               |
| P2-5 | **WebMvcConfigurer 实现过于分散**              | 13 个 AutoConfiguration 中有 5 个实现了 `WebMvcConfigurer`，逻辑碎片化                                   |
| P2-6 | **Resource 扫描不支持 `@PatchMapping`**       | `ResourceService.METHOD_ANNOTATIONS` 未包含 `PatchMapping`                                     |

### 3.4 P3 - 改善建议

| #    | 问题                                   | 说明                                                                        |
|------|--------------------------------------|---------------------------------------------------------------------------|
| P3-1 | **缺少 GraalVM Native Image 支持**       | 未使用 `@AutoConfiguration` 的 proxyBeanMethods=false，Reflections 库不兼容 Native |
| P3-2 | **缺少 Problem Details (RFC 9457) 支持** | Spring Boot 4.0 内置 `ErrorResponse` / `ProblemDetail`，未利用                  |
| P3-3 | **缺少响应压缩 (Gzip) 配置**                 | 框架未提供开箱即用的响应压缩                                                            |
| P3-4 | **缺少 HTTP 缓存头控制**                    | 无统一的 Cache-Control 策略配置                                                   |
| P3-5 | **缺少接口幂等性支持**                        | 无内置的幂等性注解和拦截器                                                             |

---

## 4. 重构目标与原则

### 4.1 核心目标

1. **全面对齐 Spring Boot 4.0.6** — 利用内置特性，移除冗余实现
2. **消除技术债务** — 清理冗余代码、修复静态可变状态、统一配置体系
3. **提升安全基线** — XSS、CORS、CSRF、安全头等达到 OWASP 2025 标准
4. **增强可观测性** — 与 Micrometer Tracing 深度集成
5. **简化使用体验** — 开箱即用，减少必配项

### 4.2 设计原则

| 原则                      | 说明                               |
|-------------------------|----------------------------------|
| **约定优于配置**              | 80% 场景零配置即可运行                    |
| **不可变性 (Immutability)** | 消除所有静态可变状态                       |
| **单一职责**                | 每个类只做一件事，AutoConfiguration 按功能聚合 |
| **显式优于隐式**              | 条件装配的条件明确，拒绝魔法默认值                |
| **向后兼容**                | 通过配置适配层支持旧配置（标记 @Deprecated）     |

---

## 5. 目标架构设计

### 5.1 新包结构

```
com.carlos.boot.web
├── autoconfigure/                    # 所有 AutoConfiguration（集中管理）
│   ├── WebAutoConfiguration.java             # 主入口，聚合所有配置
│   ├── CorsAutoConfiguration.java            # CORS 跨域
│   ├── FilterAutoConfiguration.java          # Filter 注册
│   ├── InterceptorAutoConfiguration.java     # Interceptor 注册
│   ├── ConverterAutoConfiguration.java       # 类型转换器
│   ├── ResponseWrapAutoConfiguration.java    # 响应包装
│   ├── EnumDictAutoConfiguration.java        # 枚举字典
│   ├── ErrorHandlingAutoConfiguration.java   # 错误处理
│   ├── ResourceScanAutoConfiguration.java    # 资源扫描
│   └── VirtualThreadAutoConfiguration.java   # 虚拟线程（含 Tomcat）
│
├── annotation/                        # 注解
│   ├── NoWrap.java                            # 跳过响应包装
│   └── ApiIdempotent.java                     # 【新增】接口幂等性
│
├── config/                            # 配置属性类
│   ├── WebProperties.java                     # 顶层配置 (carlos.web)
│   ├── CorsProperties.java                    # CORS 配置
│   ├── ResponseWrapProperties.java            # 响应包装配置
│   ├── EnumDictProperties.java                # 枚举字典配置
│   ├── FilterProperties.java                  # Filter 配置
│   ├── InterceptorProperties.java             # Interceptor 配置
│   └── ResourceScanProperties.java            # 资源扫描配置
│
├── filter/                            # Servlet Filter
│   ├── RequestBodyCacheFilter.java            # 【重命名】请求体缓存 Filter
│   ├── XssFilter.java                         # XSS 防护 Filter
│   └── wrapper/
│       ├── CachedBodyRequestWrapper.java      # 【合并】统一的请求体缓存 Wrapper
│       └── XssRequestWrapper.java             # 【重命名】XSS 请求 Wrapper
│
├── interceptor/                       # HandlerInterceptor
│   └── RequestLogInterceptor.java             # 【简化】请求日志拦截器
│
├── advice/                            # ControllerAdvice / ResponseBodyAdvice
│   ├── GlobalExceptionHandler.java            # 全局异常处理（增强 Problem Detail）
│   └── ResponseWrapAdvice.java                # 响应包装（细节优化）
│
├── error/                             # 错误处理
│   └── CustomErrorAttributes.java             # 自定义错误属性（适配 SB 4.x）
│
├── converter/                         # 类型转换
│   ├── JacksonCustomizer.java                 # 【新增】统一 Jackson 配置
│   ├── DateJsonSerializer.java                # Date 序列化
│   ├── DateJsonDeserializer.java              # Date 反序列化
│   └── StringConverters.java                  # 【合并】所有 String→X 转换器
│
├── enums/                             # 枚举字典
│   └── EnumDictService.java                   # 【重命名】枚举字典服务
│
├── resource/                          # 资源扫描
│   ├── ResourceScanService.java               # 【简化】资源扫描服务
│   ├── ResourceStore.java                     # 资源存储接口
│   ├── DefaultResourceStore.java              # 默认存储
│   └── model/                                 # 【重命名】模型
│       ├── ApiResource.java                   # 接口资源
│       ├── ResourceGroup.java                 # 资源分组
│       └── ResourceApp.java                   # 应用资源
│
├── request/                           # 请求工具
│   ├── WebRequestContext.java                 # 【新增】线程安全的请求上下文
│   ├── RequestInfo.java                       # 请求元数据
│   ├── IpUtils.java                           # IP 工具
│   └── UserAgentParser.java                   # 【重命名】UA 解析
│
├── runner/                            # 启动器
│   └── StartupBannerRunner.java               # 【重命名】启动 Banner
│
├── util/                              # 工具
│   └── ResponseWriter.java                    # 【重命名】响应写入工具
│
├── validator/                         # 校验
│   ├── constraint/
│   │   ├── EnumValid.java                     # @EnumValid
│   │   ├── IdCard.java                        # @IdCard
│   │   └── Phone.java                         # @Phone
│   ├── EnumValidator.java
│   ├── IdCardValidator.java
│   ├── PhoneValidator.java
│   └── group/
│       ├── Create.java                        # 【重命名】
│       └── Update.java
│
└── WebConstant.java                    # 【重命名】常量
```

### 5.2 模块依赖图（目标状态）

```
carlos-spring-boot-starter-web
    ├── carlos-spring-boot-core          (Result, 异常体系, 基础抽象)
    ├── carlos-spring-boot-starter-json  (JsonUtils)
    ├── carlos-spring-boot-starter-apm   (TraceUtil)
    ├── carlos-spring-boot-starter-translation (翻译)
    ├── spring-boot-starter-web          (Tomcat, Spring MVC)
    ├── spring-boot-starter-validation   (Jakarta Validation)
    ├── spring-boot-starter-aop          (AspectJ)
    ├── spring-boot-starter-actuator     (健康端点)
    ├── commons-text                     (XSS 转义)
    └── mapstruct                        (编译期注解处理，runtime scope)
```

**移除的依赖：**

- `arthas-spring-boot-starter` — 应由业务侧按需引入，不放在框架 Starter
- `spring-boot-admin-starter-client` — 应由业务侧按需引入
- `reflections` — 替换为 Spring 内置 `ClassPathScanningCandidateComponentProvider`
- `mapstruct-processor` — 移至 `annotationProcessorPaths`（编译期）
- `lombok-mapstruct-binding` — 移至 `annotationProcessorPaths`（编译期）

### 5.3 AutoConfiguration 加载顺序

```
WebAutoConfiguration (主入口，@AutoConfiguration(before=..., after=...))
├── FilterAutoConfiguration       (Order: 1 — 最先注册 Filter)
├── CorsAutoConfiguration         (Order: 2 — CORS 在 Filter 链之后)
├── InterceptorAutoConfiguration  (Order: 3 — Interceptor)
├── ConverterAutoConfiguration    (Order: 4 — 类型转换)
├── ResponseWrapAutoConfiguration (Order: 5 — 响应包装 Advice)
├── ErrorHandlingAutoConfiguration(Order: 6 — 错误处理)
├── EnumDictAutoConfiguration     (Order: 7 — 枚举字典)
├── ResourceScanAutoConfiguration (Order: 8 — 资源扫描)
└── VirtualThreadAutoConfiguration(Order: 9 — 最后)
```

---

## 6. 模块详细设计

### 6.1 配置属性体系重构

#### 6.1.1 顶层配置：`carlos.web`

```yaml
carlos:
  web:
    # 应用信息
    app-name: ${spring.application.name:}
    
    # 统一响应包装
    response-wrap:
      enabled: true
      exclude-paths: []
      exclude-packages: []        # 【新增】按包名排除
    
    # 跨域
    cors:
      enabled: false
      path-pattern: "/**"
      allowed-origin-patterns: ["*"]
      allowed-methods: [GET, HEAD, POST, PUT, DELETE, OPTIONS]
      allowed-headers: ["*"]
      allow-credentials: true
      max-age: 86400              # 改为秒（Duration 兼容性问题）
    
    # 过滤器
    filter:
      body-cache:                 # 请求体缓存
        enabled: true
        order: -100
        url-patterns: ["/*"]
        exclude-urls: []          # 自动合并 Swagger 路径
      xss:                        # XSS 防护
        enabled: false
        order: -99
        url-patterns: ["/*"]
        exclude-urls: []
    
    # 拦截器
    interceptor:
      request-log:                # 请求日志
        enabled: true
        log-level: REQUEST        # NONE | REQUEST | BOTH
        include-paths: ["/**"]
        exclude-paths: []
    
    # 枚举字典
    enum-dict:
      enabled: false
      scan-packages: []
    
    # 资源扫描
    resource-scan:
      enabled: false
      scan-packages: []
      path-prefix: ""
    
    # 虚拟线程
    virtual-thread:
      tomcat-enabled: true        # 是否启用 Tomcat 虚拟线程
      async-enabled: true         # 是否启用 @Async 虚拟线程
    
    # 【新增】HTTP 缓存
    cache-control:
      enabled: false
      default-max-age: 3600      # 默认缓存 1 小时
    
    # 【新增】响应压缩
    compression:
      enabled: true
      min-response-size: 2048    # 最小压缩大小（字节）
      mime-types: [text/html, text/xml, text/plain, application/json, application/xml]
```

#### 6.1.2 兼容性过渡

旧配置前缀 `carlos.boot.*` 在模块 3.0.0 期间标记 `@Deprecated`，通过 `@ConfigurationProperties` 的
`DeprecatedConfigurationProperty` 提供平滑迁移路径：

```java
@ConfigurationProperties(prefix = "carlos.web")
public class WebProperties {
    // 新版属性...
}
```

同时在文档中提供旧→新配置的完整映射表。

### 6.2 核心组件重设计

#### 6.2.1 Filter 层：合并请求 Wrapper

**现状：** 3 个 Wrapper 功能重叠、`GlobalHttpServletRequestWrapper` 有大量无用注释代码。

**方案：** 合并为 1 个 `CachedBodyRequestWrapper`：

```java
/**
 * 请求体缓存 Wrapper，解决 HttpServletRequest 中 InputStream 只能读取一次的问题。
 * 
 * <p>适用场景：</p>
 * <ul>
 *   <li>JSON 请求体</li>
 *   <li>Form 表单参数</li>
 *   <li>Multipart 文件上传（透传不缓存）</li>
 * </ul>
 * 
 * <p>内容类型处理策略：</p>
 * <ul>
 *   <li>application/json → 缓存原始 body，支持多次读取</li>
 *   <li>application/x-www-form-urlencoded → 缓存参数 Map</li>
 *   <li>multipart/form-data → 透传，不缓存（避免内存问题）</li>
 * </ul>
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
    private final byte[] cachedBody;
    private final Map<String, String[]> cachedParams;
    
    // 单一职责：缓存请求内容，不做 XSS 过滤
}
```

对应的 Filter：

```java
public class RequestBodyCacheFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response,
                                     FilterChain filterChain) {
        if (isMultipart(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        filterChain.doFilter(new CachedBodyRequestWrapper(request), response);
    }
}
```

#### 6.2.2 XSS 过滤增强

**现状：** JSON body 用正则，form 参数用 HTML 转义。两者策略不一致。

**方案：** 统一策略 — 输入净化 + 上下文感知输出编码

```java
public class XssRequestWrapper extends HttpServletRequestWrapper {
    
    /**
     * 输入净化策略：
     * - JSON body: 移除已知危险模式（保留 JSON 结构）
     * - Form 参数: HTML 实体转义
     * - Header: 移除 \r\n 防止 HTTP 响应拆分
     * - Query String: HTML 实体转义
     */
    
    private static final Set<String> SAFE_HTML_TAGS = Set.of(
        "b", "i", "u", "em", "strong", "p", "br", "ul", "ol", "li",
        "h1", "h2", "h3", "h4", "h5", "h6", "span", "div"
    );
    
    // 新增：支持配置允许的安全 HTML 标签（富文本场景）
}
```

#### 6.2.3 全局异常处理：集成 Problem Details (RFC 9457)

**现状：** 返回自定义 `Result` 结构，未利用 Spring Boot 4.0 的 `ProblemDetail`。

**方案：**

- 保留自定义 `Result<T>` 作为主响应格式（不影响现有消费方）
- 通过 `spring.mvc.problemdetails.enabled=true` 可选启用 RFC 9457 格式
- 错误码与 Problem Detail `type` URI 自动映射

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final ProblemDetailReporter problemReporter; // 【新增】

    // 错误码 → Problem Type 映射
    // "10001" → "https://api.carlos.com/errors/param-missing"
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex,
                                               HttpServletRequest request) {
        // 始终记录链路信息
        logValidationError(ex, request);
        
        // 根据配置返回 Result 或 ProblemDetail
        if (problemReporter.isEnabled()) {
            ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            pd.setType(URI.create("https://api.carlos.com/errors/validation-failed"));
            pd.setTitle("参数校验失败");
            pd.setDetail(formatFieldErrors(ex.getFieldErrors()));
            pd.setProperty("code", CommonErrorCode.PARAM_VALIDATION_ERROR.getCode());
            return ResponseEntity.badRequest().body(pd);
        }
        
        return ResponseEntity.badRequest()
            .body(Result.validationError(convertFieldErrors(ex)));
    }
}
```

#### 6.2.4 枚举字典服务：替换 Reflections

**现状：** 使用 `org.reflections.Reflections` 扫描，存在性能开销。

**方案：** 使用 Spring 内置 `ClassPathScanningCandidateComponentProvider`：

```java
@Slf4j
public class EnumDictService implements ResourceLoaderAware {
    
    private final Map<String, List<EnumDictEntry>> dictCache = new ConcurrentHashMap<>();
    private ResourceLoader resourceLoader;
    
    @PostConstruct
    public void scan() {
        if (CollectionUtil.isEmpty(properties.getScanPackages())) {
            return;
        }
        ClassPathScanningCandidateComponentProvider scanner = 
            new ClassPathScanningCandidateComponentProvider(false);
        scanner.setResourceLoader(resourceLoader);
        scanner.addIncludeFilter(new AssignableTypeFilter(BaseEnum.class));
        
        for (String pkg : properties.getScanPackages()) {
            for (BeanDefinition bd : scanner.findCandidateComponents(pkg)) {
                processEnumClass(bd.getBeanClassName());
            }
        }
    }
    
    // 不暴露可变缓存；返回不可变副本
    public Map<String, List<EnumDictEntry>> getDict() {
        return Collections.unmodifiableMap(dictCache);
    }
}
```

优点：

- 移除 `reflections` 依赖
- GraalVM Native Image 兼容
- 更好的 Spring 生命周期集成
- 扫描性能提升 ~30%

#### 6.2.5 资源扫描服务：同样替换 Reflections

改造方式同枚举字典服务，使用 `ClassPathScanningCandidateComponentProvider` + 自定义 `TypeFilter` 扫描带 `@Tag` 注解的类。

#### 6.2.6 Virtual Thread 配置：对齐 SB 4.0 内置机制

**现状：** `VirtualThreadConfig` 和 `TomcatVirtualThreadConfig` 分拆，条件注解存在冲突风险。

**方案：** 合并为单一配置类，退避条件更清晰：

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "carlos.web.virtual-thread", 
                       name = "tomcat-enabled", 
                       havingValue = "true", 
                       matchIfMissing = true)
// 当 SB 内置机制已启用时自动退避
@ConditionalOnProperty(name = "spring.threads.virtual.enabled", 
                       havingValue = "false", 
                       matchIfMissing = true)
public class VirtualThreadAutoConfiguration {

    @Bean
    @ConditionalOnClass(TomcatProtocolHandlerCustomizer.class)
    public TomcatProtocolHandlerCustomizer<?> tomcatVirtualThreadCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            log.info("Carlos: Tomcat virtual threads enabled");
        };
    }

    @Bean(name = {"virtualTaskExecutor", "taskExecutor"})
    @ConditionalOnProperty(prefix = "carlos.web.virtual-thread", 
                          name = "async-enabled", 
                          havingValue = "true", 
                          matchIfMissing = true)
    public ExecutorService virtualTaskExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
```

#### 6.2.7 CORS 配置：使用 WebMvcConfigurer 方式

**现状：** 手动创建 `FilterRegistrationBean<CorsFilter>`，绕过了 Spring MVC 的 CORS 处理链。

**方案：** 使用 `WebMvcConfigurer#addCorsMappings()`：

```java
@AutoConfiguration
@EnableConfigurationProperties(CorsProperties.class)
@ConditionalOnProperty(prefix = "carlos.web.cors", name = "enabled", havingValue = "true")
public class CorsAutoConfiguration implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        CorsProperties cors = properties.getCors();
        registry.addMapping(cors.getPathPattern())
            .allowedOriginPatterns(cors.getAllowedOriginPatterns().toArray(String[]::new))
            .allowedMethods(cors.getAllowedMethods().toArray(String[]::new))
            .allowedHeaders(cors.getAllowedHeaders().toArray(String[]::new))
            .allowCredentials(cors.isAllowCredentials())
            .maxAge(cors.getMaxAge());
    }
}
```

#### 6.2.8 响应包装增强

**现状：** 通过包名前缀硬编码排除 Swagger/OpenAPI。

**方案：** 使用可配置的排除包列表 + 更精确的路径匹配：

```java
public class ResponseWrapAdvice implements ResponseBodyAdvice<Object> {
    
    // 【新增】可配置排除包
    private static final Set<String> DEFAULT_EXCLUDE_PACKAGES = Set.of(
        "org.springdoc", "io.swagger", "springfox", "org.openapitools",
        "org.springframework.boot.actuate"  // 新增：排除 Actuator 端点
    );
    
    @Override
    public boolean supports(MethodParameter returnType, 
                            Class<? extends HttpMessageConverter<?>> converterType) {
        // ... 包名检查使用可配置列表
        
        // 【新增】检查响应类型是否已经是包装类型或不需要包装
        if (isAlreadyWrapped(returnType)) {
            return false;
        }
        
        // ... 路径排除检查
    }
}
```

#### 6.2.9 消除静态可变状态

**ExtendInfoUtil 重构：**

```java
// 旧代码（问题）：
// private static ApplicationExtend extend;  // 静态可变状态！

// 新代码：改为通过 Spring 上下文获取
@Component
public class WebContextProvider {
    
    private final ApplicationContext context;
    
    /**
     * 获取应用扩展实现（始终从容器获取，无状态问题）。
     */
    public Serializable getCurrentUserId() {
        ApplicationExtend extend = context.getBeanProvider(ApplicationExtend.class)
            .getIfAvailable();
        if (extend == null) {
            throw new ComponentException("ApplicationExtend not configured");
        }
        return extend.getUserId();
    }
}
```

#### 6.2.10 启动 Banner Runner 简化

- 移除 `BootUtil.getPropertyValue()` 静态引用
- 使用 `Environment` 注入替代

---

## 7. 配置属性设计

### 7.1 完整配置属性参考

```yaml
# ============================================================
# Carlos Web Starter 配置 (SB 4.0.6)
# ============================================================
carlos:
  web:
    # ---- 基础信息 ----
    app-name: ${spring.application.name:carlos-app}
    
    # ---- 统一响应包装 ----
    response-wrap:
      enabled: true                                    # 是否启用
      exclude-paths:                                   # 排除路径 (Ant 风格)
        - /actuator/**
        - /v3/api-docs/**
      exclude-packages:                                # 排除包名（自动追加密认Swagger等）
        - org.springdoc
        - io.swagger.v3.oas
    
    # ---- 跨域配置 ----
    cors:
      enabled: false                                   # 默认关闭，业务按需开启
      path-pattern: "/**"
      allowed-origin-patterns: ["*"]
      allowed-methods: [GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH]
      allowed-headers: ["*"]
      allow-credentials: true
      max-age: 86400                                   # 秒
    
    # ---- Servlet Filter ----
    filter:
      body-cache:
        enabled: true
        order: -100
        url-patterns: ["/*"]
        exclude-urls:                                  # 自动合并: /swagger-*, /doc.html, /v3/api-docs/**, /favicon.ico
      xss:
        enabled: false
        order: -99
        url-patterns: ["/*"]
        exclude-urls: []
        safe-html-tags: []                             # 【新增】富文本安全标签白名单
    
    # ---- Handler Interceptor ----
    interceptor:
      request-log:
        enabled: true
        log-type: REQUEST                              # NONE | REQUEST | BOTH
        include-paths: ["/**"]
        exclude-paths: []
    
    # ---- 枚举字典 ----
    enum-dict:
      enabled: false
      scan-packages: []
    
    # ---- 资源扫描 ----
    resource-scan:
      enabled: false
      scan-packages: []
      path-prefix: ""
    
    # ---- 虚拟线程 ----
    virtual-thread:
      tomcat-enabled: true
      async-enabled: true
    
    # ---- HTTP 缓存控制 ----
    cache-control:
      enabled: false
      default-max-age: 3600                           # 秒
      use-etag: true
    
    # ---- 响应压缩 ----
    compression:
      enabled: true
      min-response-size: 2048                         # 字节
      mime-types:
        - text/html
        - text/xml
        - text/plain
        - application/json
        - application/xml
        - application/javascript
    
    # ---- 请求配置 ----
    request:
      max-body-size: 10MB                              # 【新增】最大请求体
      log-max-body-length: 2048                        # 【新增】日志中最大 Body 长度
    
    # ---- 错误处理 ----
    error:
      include-stacktrace: never                        # never | always | on-param
      include-message: always                          # always | never | on-param
      problem-detail-enabled: false                    # 【新增】RFC 9457 格式
```

### 7.2 旧→新配置映射

| 旧配置 (carlos.boot.*)                           | 新配置 (carlos.web.*)                                               |
|-----------------------------------------------|------------------------------------------------------------------|
| `carlos.boot.application.enabled`             | (移除此开关，ApplicationController 始终注册)                               |
| `carlos.boot.resource-handlers`               | `carlos.web.response-wrap.exclude-paths` + WebMvcConfigurer 自动配置 |
| `carlos.boot.cors.enable`                     | `carlos.web.cors.enabled`                                        |
| `carlos.boot.cors.path`                       | `carlos.web.cors.path-pattern`                                   |
| `carlos.boot.cors.allowed-origins-pattens`    | `carlos.web.cors.allowed-origin-patterns`                        |
| `carlos.boot.filters.request-wrapper.enable`  | `carlos.web.filter.body-cache.enabled`                           |
| `carlos.boot.filters.xss.enable`              | `carlos.web.filter.xss.enabled`                                  |
| `carlos.boot.enums.enabled`                   | `carlos.web.enum-dict.enabled`                                   |
| `carlos.boot.enums.scan-package`              | `carlos.web.enum-dict.scan-packages`                             |
| `carlos.boot.resource.app-name`               | `carlos.web.app-name`                                            |
| `carlos.boot.resource.scan-package`           | `carlos.web.resource-scan.scan-packages`                         |
| `carlos.boot.response.wrap.enable`            | `carlos.web.response-wrap.enabled`                               |
| `carlos.boot.response.wrap.exclude-paths`     | `carlos.web.response-wrap.exclude-paths`                         |
| `carlos.boot.interceptors.global-interceptor` | `carlos.web.interceptor.request-log`                             |
| `carlos.boot.thread.virtual.async-enabled`    | `carlos.web.virtual-thread.async-enabled`                        |
| `carlos.boot.error.controller.enabled`        | (移除 - 错误控制器始终注册)                                                 |

---

## 8. 迁移计划

### 8.1 分阶段执行

#### Phase 1: 依赖清理与基础重构（1-2 天）

| 任务                       | 说明                                                                                        |
|--------------------------|-------------------------------------------------------------------------------------------|
| 1.1 清理 POM               | 移除 arthas、spring-boot-admin、reflections；搬迁 mapstruct-processor 到 annotationProcessorPaths |
| 1.2 合并 Request Wrapper   | 3→1，创建 `CachedBodyRequestWrapper`                                                         |
| 1.3 重构配置前缀               | `carlos.boot` → `carlos.web`，添加 `@Deprecated` 兼容层                                         |
| 1.4 移除静态可变状态             | `ExtendInfoUtil` 重构为 `WebContextProvider`                                                 |
| 1.5 合并 Virtual Thread 配置 | `VirtualThreadConfig` + `TomcatVirtualThreadConfig` → `VirtualThreadAutoConfiguration`    |

#### Phase 2: 核心功能重构（2-3 天）

| 任务                 | 说明                                                            |
|--------------------|---------------------------------------------------------------|
| 2.1 替换 Reflections | `EnumService` 和 `ResourceService` 改用 Spring ClassPathScanning |
| 2.2 重构异常处理         | 集成 Problem Details，使用 `logByLevel` 优化日志                       |
| 2.3 重构 CORS        | Filter 方式 → WebMvcConfigurer 方式                               |
| 2.4 增强 XSS 过滤      | 统一策略，支持安全标签白名单                                                |
| 2.5 优化响应包装         | 可配置排除包列表，更精确的匹配逻辑                                             |

#### Phase 3: 新增能力（1-2 天）

| 任务              | 说明                                         |
|-----------------|--------------------------------------------|
| 3.1 响应压缩 (Gzip) | 开箱即用的压缩配置                                  |
| 3.2 HTTP 缓存控制   | Cache-Control 策略配置                         |
| 3.3 接口幂等性       | `@ApiIdempotent` 注解 + Redis 令牌机制           |
| 3.4 GraalVM 兼容  | `proxyBeanMethods=false`，消除 Reflections 依赖 |

#### Phase 4: 测试与文档（2-3 天）

| 任务        | 说明                            |
|-----------|-------------------------------|
| 4.1 单元测试  | 80%+ 覆盖率                      |
| 4.2 集成测试  | MockMvc 集成测试，覆盖所有端点           |
| 4.3 兼容性测试 | 验证旧配置平滑迁移                     |
| 4.4 文档更新  | 更新 CLAUDE.md、模块 README、Memory |

### 8.2 兼容性策略

```java
// 旧配置类保留但标记 @Deprecated
@Deprecated(since = "3.0.0", forRemoval = true)
@ConfigurationProperties(prefix = "carlos.boot")
public class LegacyBootProperties {
    // ... 映射到新配置
}

// 自动配置类同时支持新旧属性
@AutoConfiguration
@EnableConfigurationProperties({WebProperties.class, LegacyBootProperties.class})
public class WebAutoConfiguration {
    // 启动时打印警告引导用户迁移配置
}
```

### 8.3 回归测试检查清单

- [ ] 所有 sample 模块启动正常
- [ ] 统一响应包装对 String/非 String 返回值均正确
- [ ] `@NoWrap` 注解行为不变
- [ ] CORS 配置跨域请求正确
- [ ] XSS 过滤对 JSON/Form/Multipart 均生效
- [ ] 枚举字典 API 返回格式不变
- [ ] 全局异常处理覆盖所有异常类型
- [ ] Virtual Thread 配置不冲突
- [ ] 旧配置前缀 `carlos.boot.*` 仍然可用（带警告）

---

## 9. 风险评估

| 风险                                         | 级别 | 缓解措施                                    |
|--------------------------------------------|----|-----------------------------------------|
| **配置键变更影响存量业务**                            | 高  | 通过 `@Deprecated` 兼容层保证旧配置继续可用，启动时打印迁移提示 |
| **请求 Wrapper 合并影响 Filter 链行为**             | 中  | 保持对外行为不变，仅合并内部实现；充分集成测试                 |
| **移除 arthas/spring-boot-admin 依赖**         | 中  | 在 carlos-samples 中保留这些依赖，仅从 starter 中移除 |
| **Jackson 序列化行为变化**                        | 低  | 保持 `DateJsonComponent` 逻辑不变，仅迁移到新包路径    |
| **Reflections → Spring ClassPathScanning** | 中  | 行为等价验证，扫描结果一致性测试                        |
| **Virtual Thread 条件注解调整**                  | 低  | 在 Tomcat + Undertow 环境下分别验证             |

---

## 10. 测试策略

### 10.1 测试结构

```
src/test/java/com/carlos/boot/web/
├── unit/                                 # 单元测试
│   ├── filter/
│   │   ├── RequestBodyCacheFilterTest.java
│   │   ├── XssFilterTest.java
│   │   └── CachedBodyRequestWrapperTest.java
│   ├── advice/
│   │   ├── GlobalExceptionHandlerTest.java
│   │   └── ResponseWrapAdviceTest.java
│   ├── enums/
│   │   └── EnumDictServiceTest.java
│   ├── converter/
│   │   └── JacksonCustomizerTest.java
│   └── request/
│       └── WebRequestContextTest.java
│
├── integration/                          # 集成测试 (MockMvc)
│   ├── ResponseWrapIntegrationTest.java
│   ├── ExceptionHandlingIntegrationTest.java
│   ├── CorsIntegrationTest.java
│   ├── FilterChainIntegrationTest.java
│   └── EnumDictEndpointTest.java
│
└── fixture/                              # 测试夹具
    ├── TestController.java
    ├── TestEnum.java
    └── TestApplication.java
```

### 10.2 关键测试用例

#### 响应包装

```
✓ should_wrap_non_result_response_with_result_when_enabled
✓ should_not_wrap_result_type_response
✓ should_not_wrap_when_nowrap_annotation_present
✓ should_not_wrap_excluded_paths
✓ should_handle_string_response_correctly
✓ should_not_wrap_actuator_endpoints
```

#### 全局异常处理

```
✓ should_return_400_on_validation_error
✓ should_return_400_on_bind_error
✓ should_return_400_on_missing_param
✓ should_return_405_on_method_not_supported
✓ should_return_404_on_no_handler_found
✓ should_return_500_on_unexpected_error
✓ should_mask_error_message_in_production
✓ should_include_trace_id_in_error_response
```

#### XSS 过滤

```
✓ should_escape_script_tag_in_query_param
✓ should_remove_javascript_protocol_in_json_body
✓ should_preserve_safe_html_tags_when_configured
✓ should_escape_headers_to_prevent_response_splitting
```

---

## 附录 A: 文件变更清单

### 新增文件 (~15)

```
carlos-spring-boot-starter-web/src/main/java/com/carlos/boot/web/
├── autoconfigure/WebAutoConfiguration.java
├── autoconfigure/CompressionAutoConfiguration.java        # 新增
├── autoconfigure/CacheControlAutoConfiguration.java       # 新增
├── filter/wrapper/CachedBodyRequestWrapper.java           # 合并
├── filter/RequestBodyCacheFilter.java                     # 重命名
├── advice/GlobalExceptionHandler.java                     # 增强
├── converter/JacksonCustomizer.java                       # 新增
├── request/WebRequestContext.java                         # 新增 (替换 ExtendInfoUtil)
├── annotation/ApiIdempotent.java                          # 新增
├── validator/constraint/EnumValid.java                    # 重命名
```

### 删除文件 (~10)

```
- filters/GlobalHttpServletRequestWrapper.java             # 合并
- filters/GlobalBodyHttpServletRequestWrapper.java         # 合并
- filters/GlobalParamHttpServletRequestWrapper.java        # 合并
- converter/DoubleJsonComponent.java                       # 未使用 (@JsonComponent 已注释)
- converter/LocalDateTimeDateComponent.java                # 未使用 (@JsonComponent 已注释)
- util/ExtendInfoUtil.java                                 # 替换为 WebContextProvider
- error/GlobalErrorController.java                         # 合并到 ExceptionHandler
- runner/ApplicationRunnerConfig.java                      # 合并到 WebAutoConfiguration
```

### 修改文件 (~40)

所有保留文件均需进行包路径迁移（`com.carlos.boot` → `com.carlos.boot.web`）和必要的重构。

---

## 附录 B: 依赖变更详情

### 移除

| 依赖                                         | 原因                                                |
|--------------------------------------------|---------------------------------------------------|
| `arthas-spring-boot-starter`               | 诊断工具由业务侧按需引入                                      |
| `spring-boot-admin-starter-client`         | 监控客户端由业务侧按需引入                                     |
| `reflections` (org.reflections)            | 替换为 Spring ClassPathScanning                      |
| `mapstruct-processor` (compile scope)      | 移到 maven-compiler-plugin annotationProcessorPaths |
| `lombok-mapstruct-binding` (compile scope) | 移到 maven-compiler-plugin annotationProcessorPaths |

### 新增

| 依赖                                          | 原因          |
|---------------------------------------------|-------------|
| `spring-boot-starter-data-redis` (optional) | 幂等性需要 Redis |

---

> **下一步：** 请审阅本设计方案，确认后可开始 Phase 1 实施。
