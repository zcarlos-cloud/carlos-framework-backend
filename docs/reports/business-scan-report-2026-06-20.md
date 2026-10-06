# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-20 08:00 CST  
**扫描 Commit**: `b6ce60bd`  
**扫描范围**: 业务集成层（gateway / tools / audit / auth / license / message / org / system）  
**扫描文件数**: 1,370 个 Java 源文件（排除 test/ 目录）  
**发现问题总数**: 24 个（P0: 3, P1: 5, P2: 8, P3: 8）  
**今日新增发现**: 1 个（P0）

---

## 一、执行摘要

### 1.1 今日变更概览

- **Git 拉取状态**: 已是最新
- **Commit**: `b6ce60bd`
- **变更文件数**: 0 个（carlos-integration/ 目录今日无代码变更）
- **新增代码**: 0 行 / 删除代码: 0 行
- **扫描模式**: 复扫模式（基于最新 commit 全量扫描，重点跟踪历史遗留问题修复状态）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 设计缺陷 | 1 | 1 | 1 | 0 | 3 |
| 安全漏洞 | 1 | 1 | 0 | 0 | 2 |
| 代码规范违规 | 0 | 2 | 3 | 2 | 7 |
| 性能风险 | 1 | 0 | 2 | 1 | 4 |
| 功能缺失 | 0 | 0 | 0 | 1 | 1 |
| 技术债务 | 0 | 1 | 2 | 4 | 7 |
| **合计** | **3** | **5** | **8** | **8** | **24** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 | 状态 |
|-----|----|----|----|----|------|------|
| carlos-message | 1 | 0 | 0 | 0 | 1 | 🔴 |
| carlos-system | 1 | 0 | 2 | 1 | 4 | 🟠 |
| carlos-audit | 1 | 1 | 1 | 0 | 3 | 🟠 |
| carlos-org | 0 | 1 | 0 | 1 | 2 | 🟡 |
| carlos-auth | 0 | 1 | 1 | 1 | 3 | 🟡 |
| carlos-gateway | 0 | 0 | 2 | 2 | 4 | 🟡 |
| carlos-license | 0 | 1 | 1 | 0 | 2 | 🟡 |
| carlos-tools | 0 | 1 | 1 | 3 | 5 | 🟡 |

### 1.4 今日重点关注

- **[TOP 1]** P0-006: `carlos-audit` ClickHouseHealthIndicator 使用 Spring Boot 2.x 已移除 API（**今日新增发现**，编译失败风险）
- **[TOP 2]** P0-001: `carlos-message` SensitiveWordFilter 全局 `synchronized` 锁（未修复，连续 2 天）
- **[TOP 3]** P0-002: `carlos-system` Service 层返回 VO 违反架构规范（未修复，连续 2 天）

---

## 二、历史遗留问题跟踪

> 以下问题全部来自昨日（2026-06-19）扫描报告，今日复扫验证修复状态。

### P0-001: carlos-message SensitiveWordFilter 全局 synchronized 锁 ⏳ 未修复

- **模块**: carlos-message
- **文件**: `carlos-integration/carlos-message/carlos-message-bus/src/main/java/com/carlos/message/filter/SensitiveWordFilter.java:47`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: `addWord()` 方法使用 `synchronized` 修饰，对 DFA 树的整个根节点加锁。高并发场景下所有写入线程串行执行。
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
- **风险**: 消息发送高峰期，敏感词库更新会阻塞所有消息过滤请求。
- **修复建议**: 使用 `ReentrantReadWriteLock` 或 `CopyOnWrite` 策略。
- **建议时效**: 24 小时内修复

---

### P0-002: carlos-system Service 层返回 VO 违反架构规范 ⏳ 未修复

- **模块**: carlos-system
- **文件**:
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/storage/service/FileStorageConfigService.java:98`
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/service/FileStorageService.java`（需进一步确认）
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/configration/service/SysConfigService.java:157`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: Service 层方法直接返回 VO（View Object），违反框架强制规范"**Service 返回值：返回 DTO 或 Entity，严禁返回 VO**"。
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
- **风险**: 破坏分层架构，Service 层与展示层耦合，难以复用和单元测试。
- **修复建议**: Service 返回 DTO，VO 转换上移至 Controller 层或专门的 Convert 组件。
- **建议时效**: 24 小时内修复

---

### P0-003: carlos-system SysDictManagerImpl 未继承 BaseServiceImpl ⏳ 未修复

- **模块**: carlos-system
- **文件**:
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/manager/impl/SysDictManagerImpl.java:41`
  - `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/manager/impl/SysDictItemManagerImpl.java:44`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: Manager 直接继承 MyBatis-Plus 的 `ServiceImpl` 而非框架定义的 `BaseServiceImpl`，破坏了框架统一的分页、DTO 转换、Join 查询能力。
- **代码片段**:
```java
public class SysDictManagerImpl extends ServiceImpl<SysDictMapper, SysDict> implements SysDictManager, ICacheManager<SysDictDTO> {
```
- **风险**: 无法复用框架 BaseService 提供的分页包装、统一异常处理、Join 查询等能力。
- **修复建议**: 改为 `extends BaseServiceImpl<SysDictMapper, SysDict>`。
- **建议时效**: 本周内修复

---

### P1-001: carlos-audit 枚举类 @Getter 与手写 getter 重复 ⏳ 未修复

- **模块**: carlos-audit
- **文件**: 11 个枚举类文件
  - `AuditLogStatusEnum.java`
  - `AuditLogChangeTypeEnum.java`
  - `AuditLogCategoryEnum.java`
  - `AuditLogBizChannelEnum.java`
  - `AuditLogPrincipalTypeEnum.java`
  - `AuditLogAttachmentTypeEnum.java`
  - `AuditLogAuthTypeEnum.java`
  - `AuditLogAuthProviderEnum.java`
  - `AuditLogPayloadStorageTypeEnum.java`
  - `AuditLogStorageTypeEnum.java`
  - `AuditLogArchiveStateEnum.java`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 枚举类同时标注 `@Getter`（Lombok 自动生成 getter）和手写 `getCode()`/`getDesc()` 方法，造成完全冗余的代码。
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
- **修复建议**: 移除手写 `getCode()` 和 `getDesc()`，保留 `@Getter`。
- **建议时效**: 本周内修复

---

### P1-002: carlos-org 枚举类 @Getter 与手写 getter 重复 ⏳ 未修复

- **模块**: carlos-org
- **文件**: 8 个枚举类文件
  - `OrgDepartmentStateEnum.java`
  - `OrgRoleStateEnum.java`
  - `OrgUserGenderEnum.java`
  - `OrgUserStateEnum.java`
  - `OrgPermissionTypeEnum.java`
  - `OrgDataScopeEnum.java`
  - `OrgPermissionStateEnum.java`
  - `OrgRoleTypeEnum.java`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 同 P1-001 模式。
- **修复建议**: 移除冗余手写 getter。
- **建议时效**: 本周内修复

---

### P1-003: carlos-auth 设备指纹使用 MD5 哈希 ⏳ 未修复

- **模块**: carlos-auth
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/util/DeviceFingerprint.java:46,73`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 设备指纹生成使用 `SecureUtil.md5()`，MD5 算法已被证明存在碰撞攻击风险。
- **代码片段**:
```java
// 生成MD5指纹
return SecureUtil.md5(fingerprintData);
// ...
return SecureUtil.md5(enhancedData);
```
- **风险**: 降低指纹唯一性，可能导致多设备被识别为同一设备（影响 MFA 信任设备判断）。
- **修复建议**: 替换为 `SecureUtil.sha256()` 或 `MessageDigest.getInstance("SHA-256")`。
- **建议时效**: 本周内修复

---

### P1-004: carlos-tools CodeGeneratorService 方法签名抛出 Exception ⏳ 未修复

- **模块**: carlos-tools
- **文件**: `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/codege/service/CodeGeneratorService.java:50`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: `createObject()` 方法签名声明 `throws Exception`，违反框架异常规范。
- **代码片段**:
```java
public void createObject(DatabaseInfo databaseInfo, CodeGenerateInfo codeGenerateInfo) throws Exception {
```
- **风险**: 调用方被迫使用 `try-catch (Exception e)`，无法区分业务异常和系统异常。
- **修复建议**: 将内部异常转换为 `BusinessException` 或 `SystemException` 抛出。
- **建议时效**: 本周内修复

---

### P1-005: carlos-license 大量使用 throws Exception ⏳ 未修复

- **模块**: carlos-license
- **文件**:
  - `CustomLicenseManager.java` (4 处)
  - `AbstractSystemInfoDao.java` (5 处)
  - `WindowsSystemInfoDao.java` (4 处)
  - `LinuxSystemInfoDao.java` (4 处)
  - **合计 17 处**
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 许可证核心组件大量方法声明 `throws Exception`，属于 TrueLicense 框架原有代码风格，未按 Carlos Framework 规范改造。
- **修复建议**: 对 `CustomLicenseManager` 进行异常转换封装；DAO 子类将具体异常捕获后转换为 `LicenseException` 或 `SystemException`。
- **建议时效**: 两周内修复

---

### P2-001: carlos-audit ClickHouseBatchWriter 使用 Thread.sleep 实现背压 ⏳ 未修复

- **模块**: carlos-audit
- **文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java:233,305`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 使用 `Thread.sleep()` 实现背压等待和重试退避，占用线程调度资源，精度差。
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
- **修复建议**: 背压场景使用 `LockSupport.parkNanos()` 或 `Semaphore`；重试退避建议使用 `ScheduledExecutorService`。
- **建议时效**: 本月内修复

---

### P2-002: carlos-gateway 多处 catch(Exception e) 吞没异常 ⏳ 未修复

- **模块**: carlos-gateway
- **文件及行号**:
  - `GatewatApplicationExtendImpl.java:45`
  - `GatewayExceptionHandler.java:235, 329`
  - `WafFilter.java:346, 441`
  - `RequestTracingFilter.java:166`
  - `RequestTransformFilter.java:62`
  - `CarlosRateLimiterAutoConfiguration.java:176`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 多处使用 `catch (Exception e)` 且仅记录日志或直接忽略，可能吞没关键异常，导致故障难以定位。
- **修复建议**: 区分可恢复异常和致命异常；对致命异常使用 `SystemException` 包装后抛出。
- **建议时效**: 本月内修复

---

### P2-003: carlos-gateway JwtTokenValidator catch(Exception) 过于宽泛 ⏳ 未修复

- **模块**: carlos-gateway
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/oauth2/validator/JwtTokenValidator.java:68,102,163`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: Token 验证过程中捕获所有 Exception，可能将配置错误、密钥问题等系统异常错误地报告为"Token 无效"。
- **修复建议**: 区分 `JWTDecodeException`、`SignatureVerificationException` 和 `IllegalStateException`，分别处理。
- **建议时效**: 本月内修复

---

### P2-004: carlos-system FileService 静态线程池无优雅关闭 ⏳ 未修复

- **模块**: carlos-system
- **文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/upload/service/FileService.java:~65`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 使用静态 `ThreadPoolExecutor` 处理文件上传，但未注册 JVM ShutdownHook 或 `@PreDestroy` 方法进行优雅关闭。
- **代码片段**:
```java
private static final ThreadPoolExecutor UPLOAD_POOL = ExecutorUtil.get(2, 8, "upload", 100, null);
```
- **风险**: 应用关闭时，正在执行的上传任务可能被强制中断，导致文件损坏或记录不一致。
- **修复建议**: 添加 `@PreDestroy` 方法调用 `UPLOAD_POOL.shutdown()` 和 `awaitTermination()`。
- **建议时效**: 本月内修复

---

### P2-005: carlos-auth 枚举类 @Getter 与手写 getter 重复 ⏳ 未修复

- **模块**: carlos-auth
- **文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/app/pojo/enums/ClientStatusEnum.java`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 同 P1-001 模式，仅影响 1 个枚举类。
- **建议时效**: 本月内修复

---

### P2-006: carlos-system 枚举类 @Getter 与手写 getter 重复 ⏳ 未修复

- **模块**: carlos-system
- **文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/pojo/enums/DictTypeEnum.java`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 同 P1-001 模式，仅影响 1 个枚举类。
- **建议时效**: 本月内修复

---

### P3-001: carlos-auth Javadoc 中 @Autowired 示例注释 ⏳ 未修复

- **模块**: carlos-auth
- **文件**:
  - `DefaultExtendUserDetailsService.java:33`
  - `ExtendUserDetailsService.java:34`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: Javadoc 代码示例中使用了 `@Autowired` 字段注入，可能误导开发者。框架规范要求使用构造器注入。
- **修复建议**: 更新 Javadoc 示例，使用构造器注入风格。

---

### P3-002: carlos-org 异常类继承 GlobalException 而非 BusinessException ⏳ 未修复

- **模块**: carlos-org
- **文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/java/com/carlos/org/exception/OrgModuleException.java:15`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 继承 `GlobalException` 而非框架推荐的 `BusinessException` 或 `SystemException`。
- **修复建议**: 改为继承 `BusinessException`，并使用 `OrgErrorCode` 枚举定义错误码。

---

### P3-003: carlos-gateway 类名拼写错误 ⏳ 未修复

- **模块**: carlos-gateway
- **文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/config/GatewatApplicationExtendImpl.java:24`
- **状态**: ⏳ **未修复**（连续 2 天）
- **问题**: 类名 `GatewatApplicationExtendImpl` 疑似拼写错误（应为 `GatewayApplicationExtendImpl`）。
- **修复建议**: 重命名类，并检查所有引用。

---

### P3-004: carlos-system 部分 Manager 方法命名不统一 ⏳ 未修复

- **模块**: carlos-system
- **问题**: 部分 Manager 方法用 `getDtoById`，部分用 `getById`，命名不统一。
- **状态**: ⏳ **未修复**（连续 2 天）

---

### P3-005: carlos-auth PasswordEncoderType 枚举 default 分支冗余 ⏳ 未修复

- **模块**: carlos-auth
- **问题**: `PasswordEncoderType` 枚举仅定义了 `BCRYPT`，但 `OAuth2PasswordEncoderConfig` 中 switch 的 default 分支冗余。
- **状态**: ⏳ **未修复**（连续 2 天）

---

### P3-006: carlos-gateway 字符串字面量错误码 ⏳ 未修复

- **模块**: carlos-gateway
- **问题**: `GatewayErrorAttributes` 中 `mapErrorCodeToHttpStatus` 的 errorCode 使用字符串字面量，建议使用常量。
- **状态**: ⏳ **未修复**（连续 2 天）

---

## 三、今日新增发现

### P0-006: carlos-audit ClickHouseHealthIndicator 使用 Spring Boot 2.x 已移除 API 🆕

- **模块**: carlos-audit
- **文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseHealthIndicator.java:5-6`
- **严重程度**: P0-Critical
- **影响范围**: 模块级（audit 模块编译失败风险）
- **问题描述**: `ClickHouseHealthIndicator` 使用了 Spring Boot 2.x 中已移除的 `org.springframework.boot.health.contributor` 包下的 `Health` 和 `HealthIndicator` 接口。在 Spring Boot 3.x/4.x 中，正确的包名为 `org.springframework.boot.actuate.health`。
- **代码片段**:
```java
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
```
- **触发条件**: 当项目使用 Spring Boot 3.x/4.x 编译时，这些导入将导致编译失败。
- **潜在风险**: audit 模块无法通过编译，影响框架整体构建。
- **解决方案**:
  - **推荐修复**: 将导入替换为 `org.springframework.boot.actuate.health.Health` 和 `org.springframework.boot.actuate.health.HealthIndicator`。
  - **参考**: 与框架核心代码扫描中发现的 P0-001 为同类问题（`org.springframework.boot.health.contributor` 已全面移除）。
- **修复命令**:
```bash
cd /home/carlos/projects/carlos-framework
sed -i 's/org.springframework.boot.health.contributor/org.springframework.boot.actuate.health/g' \
  carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseHealthIndicator.java
```

---

## 四、今日快速扫描结果

### 4.1 强制规范合规性扫描

| 检查项 | 扫描结果 | 说明 |
|-------|---------|------|
| `@Value` 字段注入 | ✅ 未发现 | 业务模块无 `@Value` 违规使用 |
| `@Autowired` 字段注入 | ✅ 未发现 | 仅 Javadoc 示例中存在（P3-001） |
| `RuntimeException` 直接抛出 | ✅ 未发现 | 业务模块使用框架异常体系 |
| `System.out.print` 调试输出 | ✅ 未发现 | 无调试输出残留 |
| `throws Exception` 宽泛异常 | ⚠️ 存在 | carlos-tools (P1-004)、carlos-license (P1-005，17处) |
| `synchronized` 关键字 | ⚠️ 存在 | carlos-message SensitiveWordFilter (P0-001) |
| `Thread.sleep` 线程阻塞 | ⚠️ 存在 | carlos-audit ClickHouseBatchWriter (P2-001) |
| `${...}` SQL 注入风险 | ✅ 未发现 | Mapper XML 中无 `${}` SQL 拼接（pom.xml 中 `${revision}` 为 Maven 属性） |
| 手写 getter/setter | ⚠️ 存在 | 枚举类中 @Getter 与手写 getter 重复（P1-001/P1-002/P2-005/P2-006） |
| Service 返回 VO | ⚠️ 存在 | carlos-system 3 处 (P0-002) |

### 4.2 文档完整性扫描

| 模块 | README | PRD | 设计文档 | 部署文档 | 数据库设计 | 状态 |
|------|--------|-----|---------|---------|-----------|------|
| carlos-gateway | ✅ | ✅ | ✅ | ✅ | — | 完整 |
| carlos-auth | ✅ | ✅ | ✅ | ✅ | ✅ | 完整 |
| carlos-audit | ✅ | ✅ | ✅ | ✅ | ✅ | 完整 |
| carlos-license | ✅ | ✅ | ✅ | ✅ | — | 完整 |
| carlos-message | ✅ | ✅ | ✅ | ✅ | ✅ | 完整 |
| carlos-org | ✅ | ✅ | ✅ | ✅ | ✅ | 完整 |
| carlos-system | ✅ | ✅ | ✅ | ✅ | ✅ | 完整 |
| carlos-tools | ✅ | ✅ | ✅ | ✅ | — | 完整 |

> 所有 8 个业务模块均具备完整的文档结构（`docs/` 目录下包含 PRD、设计、开发指南、测试、部署等子目录），文档资产覆盖全生命周期。

---

## 五、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 | 趋势 |
|------|---------|---------|---------|---------|---------|---------|------|
| carlos-gateway | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.2/5.0 | → 持平 |
| carlos-auth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.6/5.0 | → 持平 |
| carlos-audit | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.0/5.0 | → 持平 |
| carlos-license | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 3.8/5.0 | → 持平 |
| carlos-message | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | 3.8/5.0 | → 持平 |
| carlos-org | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.2/5.0 | → 持平 |
| carlos-system | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.2/5.0 | → 持平 |
| carlos-tools | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | 4.0/5.0 | → 持平 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改  
> 今日无代码变更，所有模块评分与昨日持平。

---

## 六、待办事项与修复计划

### 6.1 高优先级（立即处理）

- [ ] **TODO-1**: 修复 carlos-message `SensitiveWordFilter.addWord()` synchronized 全局锁 → 使用 `ReentrantReadWriteLock` 或 `CopyOnWrite`
  - 优先级: P0-001 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 2 小时

- [ ] **TODO-2**: 修复 carlos-system Service 返回 VO → 改为返回 DTO，转换上移
  - 优先级: P0-002 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 3 小时

- [ ] **TODO-3**: 修复 carlos-audit `ClickHouseHealthIndicator` 导入包名错误
  - 优先级: P0-006 🆕 | 状态: ⏳ 未修复
  - 预计工时: 0.5 小时

- [ ] **TODO-4**: 修复 carlos-system `SysDictManagerImpl` / `SysDictItemManagerImpl` 继承 `BaseServiceImpl`
  - 优先级: P0-003 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 1 小时

### 6.2 中优先级（本周处理）

- [ ] **TODO-5**: 清理 carlos-audit 11 个枚举类冗余手写 getter
  - 优先级: P1-001 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 1 小时

- [ ] **TODO-6**: 清理 carlos-org 8 个枚举类冗余手写 getter
  - 优先级: P1-002 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 1 小时

- [ ] **TODO-7**: 替换 carlos-auth 设备指纹 MD5 → SHA-256
  - 优先级: P1-003 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 0.5 小时

- [ ] **TODO-8**: 改造 carlos-tools `CodeGeneratorService` 移除 `throws Exception`
  - 优先级: P1-004 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 1 小时

- [ ] **TODO-9**: 改造 carlos-license 异常体系，移除 `throws Exception`
  - 优先级: P1-005 | 状态: ⏳ 未修复 | 连续: 2 天
  - 预计工时: 3 小时

### 6.3 低优先级（迭代处理）

- [ ] **TODO-10**: carlos-audit ClickHouseBatchWriter Thread.sleep → `LockSupport.parkNanos`
  - 优先级: P2-001 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-11**: carlos-gateway 细化异常捕获粒度
  - 优先级: P2-002 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-12**: carlos-gateway JwtTokenValidator 异常分类处理
  - 优先级: P2-003 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-13**: carlos-system FileService 线程池添加优雅关闭
  - 优先级: P2-004 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-14**: 清理 carlos-auth、carlos-system 枚举类冗余 getter
  - 优先级: P2-005/006 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-15**: 更新 carlos-auth Javadoc 示例为构造器注入
  - 优先级: P3-001 | 状态: ⏳ 未修复 | 连续: 2 天
- [ ] **TODO-16**: 重命名 carlos-gateway `GatewatApplicationExtendImpl`
  - 优先级: P3-003 | 状态: ⏳ 未修复 | 连续: 2 天

---

## 七、修复统计

| 统计项 | 数值 |
|-------|------|
| 扫描模块总数 | 8 个业务模块 |
| 扫描文件总数 | 1,370 个 Java 文件 |
| 发现问题总数 | 24（P0: 3, P1: 5, P2: 8, P3: 8） |
| 今日新增问题 | 1（P0-006 audit HealthIndicator 包名错误） |
| 昨日遗留问题 | 23 |
| 已修复问题 | 0 |
| 连续未修复 ≥2 天 | 23 个 |
| 编译阻断问题 | 1（P0-006） |
| 性能阻断问题 | 1（P0-001） |

---

## 八、建议与总结

### 8.1 整体评估

业务集成层代码质量基础良好，框架核心规范（MyBatis-Plus、Redis 缓存分层、异常体系）执行到位，所有模块文档资产完整。但存在以下需要关注的风险点：

1. **编译阻断风险**: P0-006（audit HealthIndicator 包名错误）为今日新增，与框架核心层的 HealthIndicator 问题为同一根因（Spring Boot 2.x API 残留）。
2. **性能阻断风险**: P0-001（message 敏感词全局锁）为最严重的运行时性能问题。
3. **架构规范风险**: P0-002（Service 返回 VO）破坏分层架构，影响代码可维护性。

### 8.2 建议优先修复顺序

1. **第一阶段（本周）**: P0-001（synchronized 全局锁）、P0-002（Service 返回 VO）、P0-006（HealthIndicator 包名）
2. **第二阶段（两周内）**: P1-001~P1-005（枚举冗余 getter、MD5 指纹、throws Exception）
3. **第三阶段（持续）**: P2/P3 级别问题迭代修复

### 8.3 自动化改进建议

建议引入以下 CI 检查，防止同类问题反复出现：

1. **编译检查**: 在 CI 中增加 `mvn compile` 步骤，确保 HealthIndicator 等 API 错误能及时发现。
2. **静态分析**: 引入 Checkstyle/SpotBugs 规则，检测 `synchronized` 在公共方法上的使用、`throws Exception` 等。
3. **规范校验**: 增加 Service 层返回类型检查（禁止返回 VO）。

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*  
*报告生成时间: 2026-06-20 08:00 CST*  
*下次扫描: 建议 2026-06-21*
