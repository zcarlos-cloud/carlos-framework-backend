# Carlos Framework 模块深度扫描报告 - Batch 2

> **扫描时间**: 2026-05-27  
> **扫描模块**: encrypt / minio / flowable / ai / apm  
> **扫描范围**: 架构设计、功能完整度、安全评估、技术债务  
> **扫描版本**: carlos-framework 3.0.0-SNAPSHOT

---

## 模块一：carlos-spring-boot-starter-encrypt

### 架构设计评估

| 检查项 | 状态 | 说明 |
|--------|------|------|
| AutoConfiguration 注册 | ✅ | `EncryptConfig` 在 `AutoConfiguration.imports` 正确注册 |
| 条件注解 | ✅ | `@ConditionalOnClass(SM4.class)` / `@ConditionalOnProperty` 使用正确 |
| Properties 绑定 | ✅ | `EncryptProperties` 使用 `@ConfigurationProperties(prefix = "carlos.encrypt")` |
| HealthIndicator | ⚠️ | 存在 `EncryptHealthIndicator` 但实现较简单 |

### 发现的问题

#### 🔴 P1 - 安全漏洞：SM4 密钥使用 MD5 派生（非安全密钥派生函数）

- **文件路径**: `src/main/java/com/carlos/encrypt/EncryptUtil.java`
- **代码片段**:
```java
// 加密 - 使用用户密钥派生真正的密钥
String realKey = DigestUtil.md5Hex16(key);
String iv = DigestUtil.md5Hex16(StrUtil.subWithLength(key, 0, 16));
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 所有使用 SM4 加密的数据，密钥派生安全性不足
- **详细分析**: MD5 已被证明存在碰撞攻击，且 MD5 不是密钥派生函数（KDF）。虽然 SM4 密钥长度为 128 位，MD5 输出也是 128 位（hex16），但这只是巧合的长度匹配，并非设计意图。正确的做法应使用 HKDF、PBKDF2 或至少 SHA-256 进行密钥派生。
- **解决方案**:
```java
// 引入 Hutool 的 PBKDF2 或自行实现 HKDF
// 修改 EncryptUtil 的 deriveKey 方法：
private static String deriveKey(String key, String salt) {
    // 使用 PBKDF2 替代 MD5
    return cn.hutool.crypto.digest.DigestUtil.pbkdf2(
        key.toCharArray(), 
        salt.getBytes(StandardCharsets.UTF_8), 
        10000,  // 迭代次数
        128     // 输出长度（SM4 密钥 128 位）
    );
}
```

#### 🟡 P2 - 技术债务：SM2 默认未启用，国密支持不完整

- **文件路径**: `src/main/java/com/carlos/encrypt/config/EncryptConfig.java`
- **代码片段**:
```java
@Bean
@ConditionalOnProperty(prefix = "carlos.encrypt", name = "sm2.enabled", havingValue = "true")
public SM2 sm2Encrypt() {
    return new SM2(privateKeyStr, publicKeyStr);
}
```
- **问题类型**: 功能缺失
- **严重程度**: P2
- **影响范围**: SM2 国密非对称加密默认不可用
- **详细分析**: SM2 是中国密码标准（GM/T 0003-2012），作为国密 starter，SM2 默认禁用且私钥通过配置文件注入无安全存储，降低了国密合规性。

#### 🟡 P2 - 技术债务：测试代码硬编码密钥

- **文件路径**: `src/test/java/com/carlos/encrypt/SM4UtilTest.java`
- **代码片段**:
```java
String key = "1234563456789076543457689";
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: 测试代码，不会直接影响生产
- **详细分析**: 测试代码中硬编码了密钥和明文身份证号 `511324199405134859`，虽然是测试环境，但不符合安全编码规范。应使用随机生成密钥和假数据。

#### 🟡 P2 - 功能缺失：缺少配置元数据

- **文件路径**: `src/main/resources/META-INF/additional-spring-configuration-metadata.json` — 文件不存在
- **问题类型**: 功能缺失
- **严重程度**: P2
- **影响范围**: IDE 自动补全、配置校验
- **详细分析**: 缺少 `additional-spring-configuration-metadata.json`，导致 IDE 无法提供 `carlos.encrypt.*` 配置项的自动补全和提示。

---

## 模块二：carlos-spring-boot-starter-minio

### 架构设计评估

| 检查项 | 状态 | 说明 |
|--------|------|------|
| AutoConfiguration 注册 | ✅ | `MinioConfig` + `MinioWebAutoConfig` 均注册 |
| 条件注解 | ✅ | `@ConditionalOnClass(MinioClient.class)` 正确 |
| Properties 绑定 | ✅ | `MinioProperties` 使用 `@ConfigurationProperties(prefix = "carlos.minio")` |
| HealthIndicator | ✅ | `MinioHealthIndicator` 存在 |
| Web 控制器 | ✅ | `MinioController` 提供 `/minio/**` 端点 |

### 发现的问题

#### 🔴 P0 - 安全漏洞：ClientInfo 明文暴露 Minio 凭证

- **文件路径**: `src/main/java/com/carlos/minio/bean/ClientInfo.java`
- **代码片段**:
```java
@Data
@Builder
@Schema(description = "Minio客户端信息")
public class ClientInfo {
    @Schema(description = "连接地址")
    private String endpoint;
    @Schema(description = "账号")
    private String accessKey;
    @Schema(description = "密钥")
    private String secretKey;
    @Schema(description = "默认桶名")
    private String defaultBucket;
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P0
- **影响范围**: 任何返回 `ClientInfo` 的 API 都会将 Minio `secretKey` 明文返回给客户端
- **详细分析**: `ClientInfo` 是一个 POJO，通过 `@Schema` 注解被 OpenAPI/Knife4j 收录。如果任何 Controller 返回此对象，secretKey 将直接暴露。虽然当前代码中没有直接返回此对象的 Controller 方法，但该类作为公共 API 存在被误用的风险。此外，`MinioProperties` 也包含明文 secretKey，但这是配置类，属于正常情况（需通过加密配置或环境变量管理）。
- **解决方案**:
```java
// 在 ClientInfo 中屏蔽 secretKey 的序列化
@JsonIgnore
@Schema(hidden = true)  // Knife4j/OpenAPI 中隐藏
public String getSecretKey() {
    return "******"; // 或返回 null
}
```

#### 🔴 P1 - 安全漏洞：MinioController 文件操作缺少权限校验

- **文件路径**: `src/main/java/com/carlos/minio/web/MinioController.java`
- **代码片段**:
```java
@GetMapping("/{bucket}/{object}")
public MinioObject getObject(@PathVariable String bucket, @PathVariable String object) {
    return MinioObject.builder()
        .bucket(bucket)
        .object(object)
        .attachmentName(object)
        .build();
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 任何拥有 `/minio` 路径访问权限的用户都可读取任意 bucket/object
- **详细分析**: `MinioController` 没有使用 `@PreAuthorize` 或其他权限校验注解。攻击者可通过直接构造 URL 访问任意 bucket 和 object。虽然框架层面有 `starter-security` 资源服务器保护，但 Controller 本身缺少方法级权限控制。
- **解决方案**:
```java
@GetMapping("/{bucket}/{object}")
@PreAuthorize("hasAuthority('minio:read')")
public MinioObject getObject(@PathVariable String bucket, @PathVariable String object) {
    // 增加 bucket 白名单校验
    if (!minioProperties.getAllowedBuckets().contains(bucket)) {
        throw new BusinessException("无权访问该存储桶");
    }
    // ...
}
```

#### 🟡 P2 - 安全漏洞：文件上传缺少类型/大小限制

- **文件路径**: `src/main/java/com/carlos/minio/utils/MinioUtil.java`
- **问题类型**: 安全漏洞
- **严重程度**: P2
- **影响范围**: 使用 MinioUtil 上传文件的所有功能
- **详细分析**: `MinioUtil.upload()` 方法没有对文件类型、文件大小进行限制，也没有病毒扫描机制。可能导致恶意文件上传（如可执行文件、脚本）。
- **解决方案**: 在上传方法中增加白名单校验：
```java
public static ObjectWriteResponse upload(String bucketName, MultipartFile file) {
    // 校验文件大小
    if (file.getSize() > minioProperties.getMaxFileSize()) {
        throw new BusinessException("文件大小超过限制");
    }
    // 校验文件类型
    String ext = FilenameUtil.extName(file.getOriginalFilename());
    if (!minioProperties.getAllowedExtensions().contains(ext.toLowerCase())) {
        throw new BusinessException("不支持的文件类型");
    }
    // ...
}
```

#### 🟡 P2 - 技术债务：MinioObjectHandlerMethodReturnValueHandler 异常处理缺失

- **文件路径**: `src/main/java/com/carlos/minio/web/MinioObjectHandlerMethodReturnValueHandler.java`
- **代码片段**:
```java
final InputStream inputStream = ObjectOptUtil.getObject(bucket, object);
mavContainer.setView(new MinioObjectView(inputStream, minioObject.getAttachmentName()));
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: 文件流返回的异常处理
- **详细分析**: 如果 `ObjectOptUtil.getObject()` 抛出异常（如 object 不存在），`MinioObjectView` 渲染时会出现未处理的异常，可能暴露内部错误信息。

---

## 模块三：carlos-spring-boot-starter-flowable

### 架构设计评估

| 检查项 | 状态 | 说明 |
|--------|------|------|
| AutoConfiguration 注册 | ✅ | `FlowableAutoConfiguration` 已注册 |
| 条件注解 | ✅ | `@ConditionalOnProperty(name = "carlos.flowable.enabled")` 正确，默认 false |
| Properties 绑定 | ✅ | `FlowableProperties` 配置完整 |
| HealthIndicator | ✅ | `FlowableHealthIndicator` 存在 |

### 发现的问题

#### 🔴 P1 - 安全漏洞：流程变量中敏感数据无脱敏处理

- **文件路径**: `src/main/java/com/carlos/flowable/service/FlowableRuntimeService.java`
- **代码片段**:
```java
public void setVariables(String processInstanceId, Map<String, Object> variables) {
    runtimeService.setVariables(processInstanceId, variables);
}

public Map<String, Object> getVariables(String processInstanceId) {
    return runtimeService.getVariables(processInstanceId);
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 所有使用 Flowable 流程变量的业务场景
- **详细分析**: 流程变量（如身份证号、银行卡号、手机号）直接以明文形式存储在 Flowable 的数据库表中（如 `ACT_RU_VARIABLE`、`ACT_HI_VARINST`）。Flowable 的历史表数据可能长期保留，导致敏感信息泄露风险。框架层没有对流程变量进行自动脱敏或加密存储的机制。
- **解决方案**:
```java
// 增加敏感变量注解和自动脱敏机制
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveVariable {
    SensitiveType type(); // ID_CARD, PHONE, BANK_CARD, etc.
}

// 在 setVariables 时自动脱敏
public void setVariables(String processInstanceId, Map<String, Object> variables) {
    Map<String, Object> sanitized = SensitiveDataUtil.sanitize(variables);
    runtimeService.setVariables(processInstanceId, sanitized);
}
```

#### 🔴 P1 - 安全漏洞：Authentication.setAuthenticatedUserId() 线程安全问题

- **文件路径**: `src/main/java/com/carlos/flowable/service/FlowableRuntimeService.java`
- **代码片段**:
```java
public ProcessInstanceDTO startProcessInstance(StartProcessParam param) {
    if (param.getStartUserId() != null) {
        Authentication.setAuthenticatedUserId(param.getStartUserId());
    }
    // ... 启动流程
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 并发启动流程实例时可能导致用户身份混淆
- **详细分析**: `Authentication.setAuthenticatedUserId()` 是 ThreadLocal 变量，但在异步或虚拟线程环境下（Spring Boot 3.5.9 支持虚拟线程），ThreadLocal 的线程安全性需要额外关注。此外，方法中没有清理 `Authentication`，虽然 Flowable 内部可能有清理机制，但显式清理更安全。
- **解决方案**:
```java
public ProcessInstanceDTO startProcessInstance(StartProcessParam param) {
    try {
        if (param.getStartUserId() != null) {
            Authentication.setAuthenticatedUserId(param.getStartUserId());
        }
        // ... 启动流程
    } finally {
        Authentication.setAuthenticatedUserId(null); // 显式清理
    }
}
```

#### 🟡 P2 - 功能缺失：缺少 ErrorCode 枚举

- **文件路径**: `src/main/java/com/carlos/flowable/exception/FlowableException.java`
- **代码片段**:
```java
public class FlowableException extends ComponentException {
    public FlowableException(String message) { super(message); }
    // ... 没有对应的 ErrorCode 枚举
}
```
- **问题类型**: 功能缺失
- **严重程度**: P2
- **影响范围**: 错误码体系不完整
- **详细分析**: Flowable 模块没有定义自己的 `ErrorCode` 枚举（如 `FlowableErrorCode`），导致抛出异常时只能使用通用错误码或字符串消息，不符合框架统一的错误码规范。

#### 🟡 P2 - 技术债务：硬编码分页默认值

- **文件路径**: `src/main/java/com/carlos/flowable/dto/TaskQueryParam.java`
- **代码片段**:
```java
private Integer pageNum = 1;
private Integer pageSize = 10;
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: 所有使用 TaskQueryParam 的分页查询
- **详细分析**: `pageSize = 10` 硬编码在 DTO 中，应通过配置文件或框架级分页默认值管理。

---

## 模块四：carlos-spring-boot-starter-ai

### 架构设计评估

| 检查项 | 状态 | 说明 |
|--------|------|------|
| AutoConfiguration 注册 | ✅ | `CarlosAiAutoConfiguration` 已注册 |
| 条件注解 | ✅ | `@ConditionalOnProperty(prefix = "carlos.ai", name = "enabled")` 正确 |
| Properties 绑定 | ✅ | `CarlosAiProperties` 配置完整 |
| HealthIndicator | ❌ | 缺失 AI 服务健康检查 |
| Service 分层 | ✅ | AiChatService / AiDocumentService / AiEmbeddingService 接口清晰 |

### 发现的问题

#### 🔴 P0 - 安全漏洞：AI API Key 明文存储风险

- **文件路径**: `src/main/java/com/carlos/ai/config/CarlosAiProperties.java`
- **代码片段**:
```java
@Data
@ConfigurationProperties(prefix = "carlos.ai")
public class CarlosAiProperties {
    private boolean enabled = true;
    private String apiKey;  // 明文存储在配置文件中
    private String baseUrl;
    // ...
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P0
- **影响范围**: AI 模块的所有外部 API 调用
- **详细分析**: `apiKey` 通过 `@ConfigurationProperties` 从 `application.yml` 绑定，如果配置文件提交到版本控制或日志中打印配置属性，API Key 将直接泄露。虽然框架层面使用 `${}` 占位符可以引用环境变量，但属性类本身没有加密存储或标记为敏感属性的机制。对比框架的 `EncryptProperties`，AI 模块没有利用已有的加密组件来保护 API Key。
- **解决方案**:
```java
// 方案一：使用框架已有的 EncryptProperties 对 apiKey 加密存储
@Data
@ConfigurationProperties(prefix = "carlos.ai")
public class CarlosAiProperties {
    private String apiKey;  // 存储为密文
    
    @JsonIgnore
    public String getDecryptedApiKey() {
        return EncryptUtil.decryptSm4(apiKey, systemKey);
    }
}

// 方案二：至少标记为敏感，防止日志打印
@Sensitive // 自定义注解，日志脱敏使用
private String apiKey;
```

#### 🔴 P1 - 安全漏洞：AI 对话缺少审计日志

- **文件路径**: `src/main/java/com/carlos/ai/service/impl/AiChatServiceImpl.java`
- **代码片段**:
```java
@Override
public String chat(String message) {
    return chatModel.generate(message);
}

@Override
public String chat(String sessionId, String message) {
    ChatMemory memory = getOrCreateMemory(sessionId);
    String response = chatModel.generate(message);
    memory.add(message);
    memory.add(response);
    return response;
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 所有 AI 对话功能
- **详细分析**: `AiChatServiceImpl` 的 `chat()` 方法没有记录用户输入和 AI 输出。在涉及敏感数据（如身份证号、业务数据）通过 AI 处理时，无法追溯数据流向。这与框架其他模块（如 audit）的审计要求不一致。此外，流式输出 `chatStream()` 也没有审计记录。
- **解决方案**:
```java
@Override
public String chat(String message) {
    // 记录审计日志
    AuditLogUtil.log("AI_CHAT", Map.of("input", maskSensitive(message)));
    String response = chatModel.generate(message);
    AuditLogUtil.log("AI_CHAT_RESPONSE", Map.of("outputLength", response.length()));
    return response;
}

private String maskSensitive(String input) {
    // 对输入进行敏感信息脱敏后记录
    return SensitiveDataUtil.mask(input);
}
```

#### 🟡 P2 - 功能缺失：缺少 HealthIndicator

- **文件路径**: `src/main/java/com/carlos/ai/` — `AiHealthIndicator.java` 不存在
- **问题类型**: 功能缺失
- **严重程度**: P2
- **影响范围**: 运维无法检测 AI 服务连通性
- **详细分析**: 模块缺少 `HealthIndicator`，无法通过 `/actuator/health` 监控 AI 模型（如 OpenAI、Ollama）的可用性。
- **解决方案**:
```java
@Component
public class AiHealthIndicator implements HealthIndicator {
    @Autowired
    private ChatModel chatModel;
    
    @Override
    public Health health() {
        try {
            chatModel.generate("ping");
            return Health.up().build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
```

#### 🟡 P2 - 技术债务：文档解析缺少敏感信息检测

- **文件路径**: `src/main/java/com/carlos/ai/service/impl/AiDocumentServiceImpl.java`
- **代码片段**:
```java
public List<Document> parsePdf(InputStream inputStream) {
    DocumentParser parser = new ApachePdfBoxDocumentParser();
    return parser.parse(inputStream);
}
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: 所有 PDF 文档解析后送入 AI 的场景
- **详细分析**: PDF 文档可能包含敏感信息（身份证号、银行卡号、商业机密），解析后直接作为 embedding 或 chat context 输入，没有进行内容扫描和标记。

---

## 模块五：carlos-spring-boot-starter-apm

### 架构设计评估

| 检查项 | 状态 | 说明 |
|--------|------|------|
| AutoConfiguration 注册 | ✅ | `ApmConfig` 已注册 |
| 条件注解 | ✅ | `@ConditionalOnProperty` 使用正确，多级控制 |
| Properties 绑定 | ✅ | `ApmProperties` 配置完整，支持 Sleuth/SkyWalking/MDC |
| HealthIndicator | ❌ | 缺失 APM 健康检查 |
| 双追踪支持 | ✅ | 同时支持 Micrometer Tracing (Brave) + SkyWalking |
| Feign 链路传递 | ✅ | `TracingFeignInterceptor` 支持 B3 + sw-trace-id 传递 |

### 发现的问题

#### 🔴 P1 - 安全漏洞：MdcFilter 无条件将 TraceID 返回给客户端

- **文件路径**: `src/main/java/com/carlos/apm/mdc/filter/MdcFilter.java`
- **代码片段**:
```java
if (apmProperties.getMdc().isAddToResponse()) {
    httpResponse.setHeader(HttpHeadersConstant.X_REQUEST_ID, requestId);
    String traceId = MdcUtil.getTraceId();
    if (traceId != null) {
        httpResponse.setHeader(apmProperties.getMdc().getResponseHeaderName(), traceId);
    }
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 所有 HTTP 响应
- **详细分析**: `addToResponse` 默认 `true`，将所有请求的 Trace ID 和 Request ID 返回给客户端。攻击者可通过 Trace ID 推断服务内部拓扑结构、服务调用链，甚至通过 Trace ID 的连续性推断请求量。虽然 Trace ID 本身通常不包含敏感数据，但信息泄露原则要求最小化对外暴露内部信息。建议默认关闭，或仅在调试环境启用。
- **解决方案**:
```java
// 修改默认值
@Data
public static class Mdc {
    private boolean addToResponse = false; // 默认关闭，仅在调试开启
}
```

#### 🔴 P1 - 安全漏洞：TraceTag SpEL 表达式存在注入风险

- **文件路径**: `src/main/java/com/carlos/apm/aop/TraceTagAspect.java`
- **代码片段**:
```java
private boolean evaluateCondition(String condition, StandardEvaluationContext context) {
    return Boolean.TRUE.equals(parser.parseExpression(condition).getValue(context, Boolean.class));
}

private String evaluateExpression(String expression, StandardEvaluationContext context) {
    Object value = parser.parseExpression(expression).getValue(context);
    return value != null ? value.toString() : null;
}
```
- **问题类型**: 安全漏洞
- **严重程度**: P1
- **影响范围**: 使用 `@TraceTag` 注解且 value/condition 来自外部输入的方法
- **详细分析**: `TraceTagAspect` 使用 `SpelExpressionParser` 解析 `tag.value()` 和 `tag.condition()`。虽然 `@TraceTag` 通常用于开发者编写的代码中，但如果表达式通过配置文件动态加载（如 `ApmProperties.SkyWalking.tags`），且配置文件可被外部修改，则存在 SpEL 注入风险（如 `T(java.lang.Runtime).getRuntime().exec('cmd')`）。
- **解决方案**:
```java
// 使用 SimpleEvaluationContext 替代 StandardEvaluationContext
// 或限制 SpEL 解析器的功能
private StandardEvaluationContext prepareContext(...) {
    // 使用 SimpleEvaluationContext 限制访问范围
    SimpleEvaluationContext context = SimpleEvaluationContext.forReadOnlyDataBinding()
        .withInstanceMethods()
        .build();
    // 只暴露必要的变量
    context.setVariable("result", result);
    // ...
    return context;
}
```

#### 🟡 P2 - 技术债务：SleuthConfig 手动创建 Tracing Bean 可能冲突

- **文件路径**: `src/main/java/com/carlos/apm/sleuth/config/SleuthConfig.java`
- **代码片段**:
```java
@Bean
@ConditionalOnMissingBean
public Tracing braveTracing(Sampler sampler) {
    return Tracing.newBuilder()
        .sampler(sampler)
        .build();
}
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: 与 Spring Boot 自动配置的 Tracing Bean 可能冲突
- **详细分析**: Spring Boot 3.x 已经通过 `micrometer-tracing-bridge-brave` 自动配置了 `Tracing` Bean，手动创建可能导致 Bean 冲突或覆盖，影响追踪行为。

#### 🟡 P2 - 功能缺失：缺少 HealthIndicator

- **文件路径**: `src/main/java/com/carlos/apm/` — `ApmHealthIndicator.java` 不存在
- **问题类型**: 功能缺失
- **严重程度**: P2
- **影响范围**: 运维监控
- **详细分析**: 缺少健康检查指标，无法通过 Actuator 检查追踪系统（如 SkyWalking Agent、Brave Tracer）是否正常工作。

#### 🟡 P2 - 技术债务：MdcUtil 使用 SpringUtil 获取 Bean 存在耦合

- **文件路径**: `src/main/java/com/carlos/apm/mdc/MdcUtil.java`
- **代码片段**:
```java
public static void setTraceId() {
    try {
        Tracer tracer = SpringUtil.getBean(Tracer.class);
        // ...
    }
}
```
- **问题类型**: 技术债务
- **严重程度**: P2
- **影响范围**: MDC 工具类
- **详细分析**: `MdcUtil` 作为工具类，通过 `SpringUtil.getBean()` 获取 `Tracer`，这增加了对 Spring 上下文的隐性依赖。如果 Spring 上下文未初始化完成或 Bean 不存在，每次调用都会抛出异常并被 catch，性能开销 unnecessary。

---

## 汇总统计

| 模块 | P0 | P1 | P2 | 总计 |
|------|----|----|----|------|
| encrypt | 0 | 2 | 2 | 4 |
| minio | 1 | 1 | 2 | 4 |
| flowable | 0 | 2 | 2 | 4 |
| ai | 1 | 1 | 2 | 4 |
| apm | 0 | 2 | 3 | 5 |
| **合计** | **2** | **8** | **11** | **21** |

### 按问题类型统计

| 问题类型 | 数量 |
|----------|------|
| 安全漏洞 | 8 |
| 技术债务 | 9 |
| 功能缺失 | 4 |

---

## 优先级修复建议

### 立即修复（P0）
1. **minio - ClientInfo 明文暴露密钥**: 在 `getSecretKey()` 上加 `@JsonIgnore` 和 `@Schema(hidden = true)`
2. **ai - API Key 明文存储**: 使用框架加密组件或环境变量 + `@Sensitive` 标记

### 短期修复（P1）
1. **encrypt - SM4 密钥派生**: 将 MD5 替换为 PBKDF2 或 HKDF
2. **minio - Controller 权限缺失**: 添加 `@PreAuthorize` 和 bucket 白名单
3. **flowable - 流程变量脱敏**: 增加敏感数据自动脱敏机制
4. **flowable - Authentication 线程安全**: 使用 try-finally 显式清理
5. **ai - 对话审计日志**: 集成 audit 模块记录 AI 交互
6. **apm - TraceID 返回客户端**: 默认关闭 `addToResponse`
7. **apm - SpEL 注入风险**: 使用 `SimpleEvaluationContext` 限制解析能力

### 中期优化（P2）
1. 各模块补充 `HealthIndicator`
2. 补充 `additional-spring-configuration-metadata.json`
3. 增加文件上传类型/大小限制
4. 优化 `SleuthConfig` Bean 冲突问题

---

> **报告生成完毕** | 共扫描 5 个模块，21 项问题，其中 P0 2 项，P1 8 项，P2 11 项。
