# Carlos Framework 业务集成层代码质量扫描报告

**报告日期**: 2026-06-19  
**扫描范围**: carlos-integration 下 8 个业务模块  
**扫描文件数**: 1,370 个 Java 源文件（排除 test/ 目录）  
**规范基线**: 框架强制规范（SKILL.md）+ 通用 Java/Spring Boot 最佳实践  
**扫描工具**: 静态代码分析（grep 模式匹配 + 人工精读）  

---

## 1. 执行摘要

本次扫描覆盖 8 个业务集成模块，共发现 **可归类问题 23 项**，其中：

| 优先级 | 数量 | 说明 |
|--------|------|------|
| **P0 - 阻塞** | 2 | 架构规范违反、生产安全隐患 |
| **P1 - 高** | 5 | 代码冗余、异常处理缺陷、安全降级 |
| **P2 - 中** | 8 | 性能隐患、实现不一致、异常吞咽 |
| **P3 - 低** | 8 | 代码风格、可维护性改进 |

**总体评估**: 代码基础质量良好，框架核心规范（MyBatis-Plus、Redis缓存分层、异常体系）执行到位，但存在**枚举类 Lombok 冗余**、**Service 层返回 VO** 等架构规范违反，以及**敏感词过滤全局锁**、**MD5 设备指纹**等安全/性能隐患。

---

## 2. 扫描范围

| 模块 | 子模块 | 文件数 | 扫描重点 |
|------|--------|--------|----------|
| carlos-gateway | - | 98 | 过滤器链、Token 验证、异常处理、WAF |
| carlos-auth | api, boot, cloud, service | 225 | OAuth2 配置、密码编码、异常体系、MFA |
| carlos-audit | api, boot, bus, cloud | 117 | ClickHouse 写入、Disruptor、枚举规范 |
| carlos-license | core, verify, generate | 22 | 许可证管理、异常声明 |
| carlos-message | api, boot, bus, cloud, core | 180 | 敏感词过滤、消息通道 |
| carlos-org | api, boot, bus, cloud | 367 | 组织架构缓存、枚举规范 |
| carlos-system | api, boot, bus, cloud | 279 | 字典管理、文件服务、配置管理 |
| carlos-tools | - | 82 | 代码生成器、异常处理 |

---

## 3. 问题详单

### 3.1 P0 - 阻塞级（必须立即修复）

#### P0-001: carlos-message SensitiveWordFilter 全局 synchronized 锁 [性能/并发]

- **文件**: `carlos-integration/carlos-message/carlos-message-bus/src/main/java/com/carlos/message/filter/SensitiveWordFilter.java`
- **行号**: 47
- **问题**: `addWord()` 方法使用 `synchronized` 修饰，对 DFA 树的整个根节点加锁。在高并发消息过滤场景下，所有写入线程串行执行，严重限制吞吐量。
- **代码片段**:
```java
public synchronized void addWord(String word) {
    if (word == null || word.isEmpty()) {
        return;
    }
    Map<Character, Node> current = root;
    for (char c : word.toCharArray()) {
        Node node = current.computeIfAbsent(c, k -> new Node());
        current = node.children;
    }
    current.computeIfAbsent(Node.END_FLAG, k -> new Node()).isEnd = true;
}
```
- **风险**: 消息发送高峰期，敏感词库更新（如动态添加新敏感词）会阻塞所有消息过滤请求。
- **修复建议**: 使用 `ReentrantReadWriteLock`（读多写少场景），或采用 `CopyOnWrite` 策略（词库更新频率远低于读取频率），或拆分为无锁的 `ConcurrentHashMap` 节点结构。

---

#### P0-002: carlos-system Service 层返回 VO 违反架构规范 [架构]

- **文件**: 
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/storage/service/FileStorageConfigService.java:98`
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/service/FileStorageService.java:428`
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/configration/service/SysConfigService.java:157`
- **问题**: Service 层方法直接返回 VO（View Object），违反框架强制规范"**Service 返回值：返回 DTO 或 Entity，严禁返回 VO**"。VO 应在 Controller/ApiImpl 层通过 MapStruct 转换产生。
- **代码片段**（FileStorageConfigService）:
```java
public FileStorageConfigVO getById(Long id) {
    FileStorageConfigDTO dto = configManager.getDtoById(id);
    if (dto == null) {
        return null;
    }
    // 解密敏感字段
    decryptSensitiveFields(dto);
    return toVOWithDesensitization(dto);
}
```
- **风险**: 破坏分层架构，导致 Service 层与展示层耦合，难以复用和单元测试。
- **修复建议**: Service 返回 DTO，VO 转换上移至 Controller 层或专门的 Convert 组件。

---

### 3.2 P1 - 高优先级

#### P1-001: carlos-audit 枚举类 @Getter 与手写 getter 重复 [代码冗余]

- **文件**: `carlos-integration/carlos-audit/carlos-audit-api/src/main/java/com/carlos/audit/api/pojo/enums/*.java` 等 **11 个文件**
- **问题**: 枚举类同时标注 `@Getter`（Lombok 自动生成 getter）和手写 `getCode()`/`getDesc()` 方法，造成**完全冗余的代码**。
- **涉及文件**:
  - `AuditLogStatusEnum.java` (行 18 `@Getter`, 行 53-58 手写 getter)
  - `AuditLogChangeTypeEnum.java`
  - `AuditLogCategoryEnum.java`
  - `AuditLogBizChannelEnum.java`
  - `AuditLogPrincipalTypeEnum.java`
  - `AuditLogAttachmentTypeEnum.java`
  - `AuditLogAuthTypeEnum.java`
  - `AuditLogAuthProviderEnum.java`
  - `AuditLogPayloadStorageTypeEnum.java` (bus 模块)
  - `AuditLogStorageTypeEnum.java` (bus 模块)
  - `AuditLogArchiveStateEnum.java` (bus 模块)
- **代码片段**:
```java
@Getter
@AllArgsConstructor
public enum AuditLogStatusEnum implements BaseEnum {
    SUCCESS(1, "成功"),
    FAIL(2, "失败");

    @EnumValue
    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {   // ← 与 @Getter 重复
        return code;
    }

    @Override
    public String getDesc() {    // ← 与 @Getter 重复
        return desc;
    }
}
```
- **修复建议**: 移除手写 `getCode()` 和 `getDesc()`，保留 `@Getter` 和 `@AllArgsConstructor`。若需要 `BaseEnum` 接口方法，确认 `@Getter` 是否已覆盖。

---

#### P1-002: carlos-org 枚举类同样存在 @Getter 与手写 getter 重复 [代码冗余]

- **文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/java/com/carlos/org/pojo/enums/*.java` 等 **8 个文件**
- **涉及文件**:
  - `OrgDepartmentStateEnum.java`
  - `OrgRoleStateEnum.java`
  - `OrgUserGenderEnum.java`
  - `OrgUserStateEnum.java`
  - `OrgPermissionTypeEnum.java`
  - `OrgDataScopeEnum.java`
  - `OrgPermissionStateEnum.java`
  - `OrgRoleTypeEnum.java`
- **修复建议**: 同 P1-001，移除冗余手写 getter。

---

#### P1-003: carlos-auth 设备指纹使用 MD5 哈希 [安全]

- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/util/DeviceFingerprint.java`
- **行号**: 46, 73
- **问题**: 设备指纹生成使用 `SecureUtil.md5()`，MD5 算法已被证明存在碰撞攻击风险，不符合现代安全标准。
- **代码片段**:
```java
// 生成MD5指纹
return SecureUtil.md5(fingerprintData);
// ...
return SecureUtil.md5(enhancedData);
```
- **风险**: 虽然设备指纹碰撞不会直接导致安全漏洞，但降低了指纹的唯一性，可能导致多设备被识别为同一设备（影响 MFA 信任设备判断）。
- **修复建议**: 替换为 `SecureUtil.sha256()` 或 `MessageDigest.getInstance("SHA-256")`。

---

#### P1-004: carlos-tools CodeGeneratorService 方法签名抛出 Exception [异常规范]

- **文件**: `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/codege/service/CodeGeneratorService.java`
- **问题**: `createObject()` 方法签名声明 `throws Exception`，违反框架规范"使用框架定义异常（BusinessException/SystemException），严禁直接抛出 Exception"。
- **代码片段**:
```java
public void createObject(DatabaseInfo databaseInfo, CodeGenerateInfo codeGenerateInfo) throws Exception {
```
- **风险**: 调用方被迫使用 `try-catch (Exception e)`，无法区分业务异常和系统异常，破坏异常体系。
- **修复建议**: 将内部异常转换为 `BusinessException` 或 `SystemException` 抛出，移除方法签名上的 `throws Exception`。

---

#### P1-005: carlos-license 大量使用 throws Exception [异常规范]

- **文件**:
  - `carlos-integration/carlos-license/carlos-license-core/src/main/java/com/carlos/license/CustomLicenseManager.java` (行 85, 110, 139, 286)
  - `carlos-integration/carlos-license/carlos-license-core/src/main/java/com/carlos/license/service/AbstractSystemInfoDao.java` (行 58, 70, 82, 94, 106)
  - `carlos-integration/carlos-license/carlos-license-core/src/main/java/com/carlos/license/service/WindowsSystemInfoDao.java` (行 24, 48, 73, 103)
  - `carlos-integration/carlos-license/carlos-license-core/src/main/java/com/carlos/license/service/LinuxSystemInfoDao.java` (行 27, 51, 76, 107)
- **问题**: 许可证核心组件和系统信息采集 DAO 层大量方法声明 `throws Exception`，属于 TrueLicense 框架原有代码风格，未按 Carlos Framework 规范改造。
- **修复建议**: 对 `CustomLicenseManager` 进行异常转换封装；`AbstractSystemInfoDao` 子类将具体异常（IOException, InterruptedException 等）捕获后转换为 `LicenseException` 或 `SystemException`。

---

### 3.3 P2 - 中优先级

#### P2-001: carlos-audit ClickHouseBatchWriter 使用 Thread.sleep 实现背压 [性能]

- **文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
- **行号**: 233, 305
- **问题**: 使用 `Thread.sleep()` 实现背压等待和重试退避，占用线程调度资源，精度差（毫秒级）。
- **代码片段**:
```java
// 背压等待
while (bufferSize.get() >= maxBufferSize && System.currentTimeMillis() < deadline) {
    try {
        Thread.sleep(timeoutMs);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
    }
}

// 重试退避
long backoffMs = Math.min(1000L * (1L << (retry - 1)), MAX_BACKOFF_MS);
Thread.sleep(backoffMs);
```
- **修复建议**: 背压场景使用 `LockSupport.parkNanos()` 或 `Semaphore`；重试退避建议使用 `ScheduledExecutorService` 或 Resilience4j 重试策略。

---

#### P2-002: carlos-system SysDictManagerImpl/SysDictItemManagerImpl 继承 ServiceImpl 而非 BaseServiceImpl [架构一致]

- **文件**:
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/manager/impl/SysDictManagerImpl.java:41`
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/manager/impl/SysDictItemManagerImpl.java:44`
- **问题**: 两个 Manager 直接继承 MyBatis-Plus 的 `ServiceImpl` 而非框架定义的 `BaseServiceImpl`，破坏了框架统一的分页、DTO 转换、Join 查询能力。
- **代码片段**:
```java
// SysDictManagerImpl
public class SysDictManagerImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictManager, ICacheManager<SysDictDTO> {

// SysDictItemManagerImpl
public class SysDictItemManagerImpl extends ServiceImpl<SysDictItemMapper, SysDictItem> implements SysDictItemManager {
```
- **风险**: 无法复用框架 BaseService 提供的分页包装、统一异常处理、Join 查询等能力。
- **修复建议**: 改为 `extends BaseServiceImpl<SysDictMapper, SysDict>`，并确认 `SysDictManager` 接口是否继承 `BaseService<SysDict>`。

---

#### P2-003: carlos-gateway 多处 catch(Exception e) 吞没异常 [可观测性]

- **文件及行号**:
  - `GatewatApplicationExtendImpl.java:45`
  - `GatewayExceptionHandler.java:235, 329`
  - `WafFilter.java:346, 441`
  - `RequestTracingFilter.java:166`
  - `RequestTransformFilter.java:62`
  - `CarlosRateLimiterAutoConfiguration.java:176`
- **问题**: 多处使用 `catch (Exception e)` 且仅记录日志或直接忽略，可能吞没关键异常，导致故障难以定位。
- **修复建议**: 区分可恢复异常和致命异常；对致命异常使用 `SystemException` 包装后抛出；确保所有异常路径都有足够的上下文日志。

---

#### P2-004: carlos-gateway catch(Exception ignored) 模式 [可观测性]

- **文件**: `GatewayExceptionHandler.java:235, 329`
- **问题**: 使用 `ignored` 作为异常变量名，暗示异常被有意忽略，但实际可能丢失重要错误信息。
- **代码片段**:
```java
} catch (Exception ignored) {
    // 可能丢失异常信息
}
```
- **修复建议**: 即使是可忽略的异常，也应记录 DEBUG 级别日志，便于问题排查。

---

#### P2-005: carlos-system FileService 静态线程池无优雅关闭 [资源管理]

- **文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/service/FileService.java`
- **行号**: ~45
- **问题**: 使用静态 `ThreadPoolExecutor` 处理文件上传，但未注册 JVM ShutdownHook 或 `@PreDestroy` 方法进行优雅关闭。
- **代码片段**:
```java
private static final ThreadPoolExecutor UPLOAD_POOL = ExecutorUtil.get(2, 8, "upload", 100, null);
```
- **风险**: 应用关闭时，正在执行的上传任务可能被强制中断，导致文件损坏或记录不一致。
- **修复建议**: 添加 `@PreDestroy` 方法调用 `UPLOAD_POOL.shutdown()` 和 `awaitTermination()`。

---

#### P2-006: carlos-auth 枚举类 @Getter 与手写 getter 重复 [代码冗余]

- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/app/pojo/enums/ClientStatusEnum.java`
- **问题**: 同 P1-001 模式，仅影响 1 个枚举类。

---

#### P2-007: carlos-system 枚举类 @Getter 与手写 getter 重复 [代码冗余]

- **文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/pojo/enums/DictTypeEnum.java`
- **问题**: 同 P1-001 模式，仅影响 1 个枚举类。

---

#### P2-008: carlos-gateway JwtTokenValidator catch(Exception e) 过于宽泛 [安全]

- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/JwtTokenValidator.java`
- **行号**: 68, 102, 163
- **问题**: Token 验证过程中捕获所有 Exception，可能将配置错误、密钥问题等系统异常错误地报告为"Token 无效"。
- **修复建议**: 区分 `JWTDecodeException`（Token 格式错误）、`SignatureVerificationException`（签名无效）和 `IllegalStateException`（配置错误），分别处理。

---

### 3.4 P3 - 低优先级

#### P3-001: carlos-auth Javadoc 中 @Autowired 示例注释 [文档]

- **文件**:
  - `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/service/DefaultExtendUserDetailsService.java:33`
  - `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/service/ExtendUserDetailsService.java:34`
- **问题**: Javadoc 代码示例中使用了 `@Autowired` 字段注入，虽然只是文档，但可能误导开发者。框架规范要求使用构造器注入（`@RequiredArgsConstructor`）。
- **修复建议**: 更新 Javadoc 示例，使用构造器注入风格。

---

#### P3-002: carlos-org 异常类继承 GlobalException 而非 BusinessException [异常体系]

- **文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/java/com/carlos/org/exception/OrgModuleException.java`
- **问题**: 继承 `GlobalException` 而非框架推荐的 `BusinessException` 或 `SystemException`，虽然功能等价，但不够语义化。
- **修复建议**: 改为继承 `BusinessException`，并使用 `OrgErrorCode` 枚举定义错误码。

---

#### P3-003: carlos-gateway 类名拼写错误 [代码风格]

- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/config/GatewatApplicationExtendImpl.java`
- **问题**: 类名 `GatewatApplicationExtendImpl` 疑似拼写错误（应为 `GatewayApplicationExtendImpl`）。
- **修复建议**: 重命名类，并检查所有引用。

---

#### P3-004: carlos-audit ClickHouseBatchWriter 重试使用递归 [已修复]

- **文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
- **状态**: ✅ 已按规范使用循环替代递归，避免栈溢出。

---

#### P3-005 ~ P3-008: 其他代码风格问题

- `carlos-system` 部分 Manager 方法命名不统一（部分用 `getDtoById`，部分用 `getById`）
- `carlos-auth` `PasswordEncoderType` 枚举仅定义了 `BCRYPT`，但 `OAuth2PasswordEncoderConfig` 中 switch 的 default 分支冗余
- `carlos-gateway` `GatewayErrorAttributes` 中 `mapErrorCodeToHttpStatus` 的 errorCode 使用字符串字面量，建议使用常量

---

## 4. 模块质量评估

### 4.1 carlos-gateway (98 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐ | 配置类、过滤器均有良好 Javadoc |
| 需求覆盖度 | ⭐⭐⭐⭐⭐ | JWT/OpaqueToken 双验证、WAF、熔断、灰度、限流齐全 |
| 代码规范 | ⭐⭐⭐⭐ | 无 @Value/@Autowired，无 RuntimeException，但多处 catch(Exception) 过宽 |
| 性能风险 | ⭐⭐⭐⭐ | WebClient 非阻塞，虚拟线程 @Async，但 broad catch 可能影响快速失败 |
| 安全评估 | ⭐⭐⭐⭐ | WAF、重放攻击防护到位，Token 验证链完整 |

### 4.2 carlos-auth (225 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐⭐ | OAuth2 配置拆分清晰，每个配置类职责单一 |
| 需求覆盖度 | ⭐⭐⭐⭐⭐ | 授权码/密码/短信/扫码/MFA/社交登录全支持 |
| 代码规范 | ⭐⭐⭐⭐ | 使用构造器注入，BusinessException 规范，但枚举类有冗余 getter |
| 性能风险 | ⭐⭐⭐⭐ | Redis 缓存权限，但 MD5 设备指纹应升级 |
| 安全评估 | ⭐⭐⭐⭐⭐ | BCrypt 默认，RSA 密钥管理，CSRF 保护 |

### 4.3 carlos-audit (117 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐ | ClickHouse 写入、Disruptor 事件处理有详细注释 |
| 需求覆盖度 | ⭐⭐⭐⭐⭐ | 批量写入、多维度风险评分、本地磁盘备份兜底 |
| 代码规范 | ⭐⭐⭐ | 11 个枚举类 @Getter + 手写 getter 重复，Thread.sleep 背压 |
| 性能风险 | ⭐⭐⭐ | Thread.sleep 背压、双缓冲设计合理但 sleep 精度不足 |
| 安全评估 | ⭐⭐⭐⭐ | 审计日志本身无敏感信息泄露 |

### 4.4 carlos-license (22 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐ | 基于 TrueLicense，文档较少 |
| 需求覆盖度 | ⭐⭐⭐⭐ | 生成/验证/系统信息绑定齐全 |
| 代码规范 | ⭐⭐⭐ | 大量 `throws Exception`，未按框架异常体系改造 |
| 性能风险 | ⭐⭐⭐⭐⭐ | 无性能瓶颈 |
| 安全评估 | ⭐⭐⭐⭐ | 生产环境已排除 generate 模块 |

### 4.5 carlos-message (180 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐ | 敏感词过滤、消息通道文档齐全 |
| 需求覆盖度 | ⭐⭐⭐⭐ | DFA 敏感词、多渠道消息 |
| 代码规范 | ⭐⭐⭐⭐ | 无严重违规 |
| 性能风险 | ⭐⭐⭐ | **敏感词 addWord synchronized 全局锁是最大瓶颈** |
| 安全评估 | ⭐⭐⭐⭐ | 敏感词过滤到位 |

### 4.6 carlos-org (367 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐ | 五层 Redis 缓存结构注释清晰 |
| 需求覆盖度 | ⭐⭐⭐⭐⭐ | 部门/用户/岗位/角色/权限完整 |
| 代码规范 | ⭐⭐⭐ | 8 个枚举类冗余 getter，OrgModuleException 继承层级可优化 |
| 性能风险 | ⭐⭐⭐⭐ | Redis 缓存树设计优秀，分页初始化 5000 条/页 |
| 安全评估 | ⭐⭐⭐⭐ | 数据权限控制到位 |

### 4.7 carlos-system (279 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐⭐ | 字典、配置、文件服务文档齐全 |
| 需求覆盖度 | ⭐⭐⭐⭐⭐ | i18n、区域、上传存储、系统配置完整 |
| 代码规范 | ⭐⭐⭐ | **Service 返回 VO (P0)**，SysDictManagerImpl 未继承 BaseServiceImpl |
| 性能风险 | ⭐⭐⭐⭐ | 文件上传线程池配置合理 |
| 安全评估 | ⭐⭐⭐⭐ | 文件类型/MIME 双重校验 |

### 4.8 carlos-tools (82 files)

| 维度 | 评分 | 说明 |
|------|------|------|
| 文档完整性 | ⭐⭐⭐ | GUI 代码生成器文档一般 |
| 需求覆盖度 | ⭐⭐⭐⭐ | 支持多数据库、多模板 |
| 代码规范 | ⭐⭐⭐⭐ | `throws Exception` 是主要问题 |
| 性能风险 | ⭐⭐⭐⭐⭐ | 无运行时性能问题 |
| 安全评估 | ⭐⭐⭐⭐⭐ | 仅开发工具，无生产安全风险 |

---

## 5. 修复建议优先级路线图

### 第一阶段（本周）- P0 阻塞
1. [ ] 修复 carlos-message `SensitiveWordFilter.addWord()` synchronized 全局锁 → 使用 `ReentrantReadWriteLock` 或 `CopyOnWrite`
2. [ ] 修复 carlos-system Service 返回 VO → 改为返回 DTO，转换上移

### 第二阶段（两周内）- P1 高优
3. [ ] 清理 carlos-audit 11 个枚举类冗余手写 getter
4. [ ] 清理 carlos-org 8 个枚举类冗余手写 getter
5. [ ] 替换 carlos-auth 设备指纹 MD5 → SHA-256
6. [ ] 改造 carlos-tools `CodeGeneratorService` 移除 `throws Exception`
7. [ ] 改造 carlos-license 异常体系，移除 `throws Exception`

### 第三阶段（一个月内）- P2 中优
8. [ ] carlos-audit ClickHouseBatchWriter Thread.sleep → `LockSupport.parkNanos` 或 Semaphore
9. [ ] carlos-system SysDictManagerImpl/SysDictItemManagerImpl 改为继承 BaseServiceImpl
10. [ ] carlos-gateway 细化异常捕获粒度，避免 catch(Exception e) 吞没
11. [ ] carlos-system FileService 线程池添加优雅关闭

### 第四阶段（持续改进）- P3 低优
12. [ ] 更新 carlos-auth Javadoc 中的 @Autowired 示例为构造器注入
13. [ ] 重命名 carlos-gateway `GatewatApplicationExtendImpl` 拼写错误
14. [ ] 统一 Manager 层方法命名规范

---

## 6. 扫描方法说明

本次扫描采用以下策略：

1. **批量模式扫描**: 使用 `grep` 对全量 1,370 个 Java 文件进行模式匹配，检测：
   - `@Value` / `@Autowired` 字段注入
   - `RuntimeException` 直接抛出
   - `System.out.print` 调试输出
   - `new Exception` / `throws Exception` 宽泛异常
   - `Thread.sleep` 线程阻塞
   - `synchronized` 关键字
   - `${...}` SQL 注入风险
   - `public.*get[A-Z]` / `public.*set[A-Z]` 手写 getter/setter

2. **重点文件精读**: 对扫描出的可疑文件进行完整内容读取，确认问题真实性、严重程度和上下文。

3. **规范对比**: 以框架 AGENTS.md 中记录的强制规范为基准，逐项核对。

4. **排除范围**: 所有 `test/` 目录文件不参与统计；`carlos-spring-boot-starter-license-generate` 按安全红线已标记为生产环境排除。

---

## 7. 附录

### A. 扫描原始命令

```bash
# 枚举类手写 getter 扫描
grep -rn "public.*get.*(" carlos-integration/ --include="*Enum*.java" | grep -v "test/"

# @Value / @Autowired 扫描
grep -rn "@Value\|@Autowired" carlos-integration/ --include="*.java" | grep -v "test/"

# synchronized 扫描
grep -rn "synchronized" carlos-integration/ --include="*.java" | grep -v "test/"

# Thread.sleep 扫描
grep -rn "Thread.sleep" carlos-integration/ --include="*.java" | grep -v "test/"

# throws Exception 扫描
grep -rn "throws Exception" carlos-integration/ --include="*.java" | grep -v "test/"

# Service 返回 VO 扫描
grep -rn "public .*VO .*(" carlos-integration/ --include="*Service.java" | grep -v "test/"
```

### B. 文件哈希（用于报告完整性校验）

- 报告生成时间: 2026-06-19
- 代码库 Commit: `ca253d3a` (chore: daily optimization report 2026-04-30-0918)
- 扫描执行环境: Linux 6.17.0-29-generic, JDK 21, Maven 3.8+

---

*报告生成完毕。如有疑问请联系 Carlos Framework 开发团队。*
