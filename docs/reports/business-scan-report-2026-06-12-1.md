# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-12 08:15
**扫描 Commit**: bf9333068a4bfe4a5708003c3b27dcdb86a6297e
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 12 个（P0: 1 个, P1: 3 个, P2: 5 个, P3: 3 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（已是最新）
- Commit: bf9333068a4bfe4a5708003c3b27dcdb86a6297e
- 变更文件数: 1 个（docs/reports/framework-scan-report-2026-06-12.md）
- 新增代码: +743 行 / 删除代码: -0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 2 | 2 | 4 |
| 需求与设计缺陷 | 0 | 0 | 1 | 0 | 1 |
| 代码规范违规 | 0 | 2 | 1 | 0 | 3 |
| 性能风险 | 0 | 0 | 0 | 0 | 0 |
| 安全漏洞 | 1 | 0 | 0 | 0 | 1 |
| 功能未完善 | 0 | 1 | 0 | 0 | 1 |
| 技术债务 | 0 | 0 | 1 | 1 | 2 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 1 | 2 | 1 | 4 |
| carlos-gateway | 0 | 0 | 0 | 0 | 0 |
| carlos-audit | 0 | 1 | 1 | 0 | 2 |
| carlos-message | 0 | 0 | 0 | 1 | 1 |
| carlos-org | 0 | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 0 | 0 | 1 | 1 |
| carlos-license | 1 | 0 | 0 | 0 | 1 |
| carlos-tools | 0 | 0 | 1 | 0 | 1 |

### 1.4 今日重点关注
- [TOP 1] `carlos-spring-boot-starter-license-generate` 存在于源码仓库中，存在误部署至生产环境的风险（P0，安全）
- [TOP 2] `ClickHouseBatchWriter` 使用 `Spring BeanUtils.copyProperties` 而非 MapStruct，违反框架强制规范（P1，代码规范）
- [TOP 3] `OAuth2AuthorizationServerConfig` 使用 `@Configuration` 而非 `@AutoConfiguration`，影响 Starter 自动发现机制（P1，代码规范）
- [TOP 4] `LoginAttemptManager` 与 `IpBlockManager` 使用硬编码阈值，未通过 `@ConfigurationProperties` 外部化配置（P2，功能未完善）

---

## 二、文档缺失详情

### 问题 1: 多个业务模块缺少 README.md 与模块文档

**模块**: `carlos-auth`, `carlos-gateway`, `carlos-org`, `carlos-audit`
**文件**: `carlos-xxx/README.md`（缺失）
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 详细分析

- **问题描述**: 扫描发现 `carlos-auth`, `carlos-gateway`, `carlos-org`, `carlos-audit` 等核心业务模块根目录下均缺少 `README.md` 文件。相比之下，`carlos-system`, `carlos-message`, `carlos-tools` 已具备较完整的 `docs/` 目录结构（含 01-product, 02-design, 03-develop, 04-test, 05-issue, 06-deploy）。
- **违反规范**: `CLAUDE.md` 要求每个模块必须包含模块简介、快速开始、配置说明、依赖关系。
- **触发条件**: 新成员加入项目或进行模块集成时，无法快速理解模块边界和核心能力。
- **潜在风险**: 维护困难、知识无法沉淀、模块边界模糊导致重复开发。

#### 解决方案

**推荐修复**:

为每个缺失模块创建 `README.md`，至少包含：
```markdown
# carlos-xxx

## 模块简介
本模块负责...（一句话说明核心职责）

## 核心功能
- 功能1
- 功能2

## 快速开始
1. 引入依赖
2. 必要配置
3. 启动验证

## 配置说明
| 配置项 | 默认值 | 说明 |
|-------|-------|------|

## 依赖关系
- 依赖模块：...
- 被依赖模块：...

## 接口文档
- Swagger/Knife4j 地址
```

**短期规避**: 在 `carlos-integration/README.md` 中汇总所有业务模块的入口说明。

**长期建议**: 建立模块文档模板（`docs/module-template/`），每次新建模块时自动复制。

---

### 问题 2: 业务模块缺少 CHANGELOG 与版本变更日志

**模块**: 所有业务集成模块
**文件**: `carlos-xxx/CHANGELOG.md`（缺失）
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 详细分析

- **问题描述**: 所有业务模块均未发现 `CHANGELOG.md` 或同级版本变更记录文件。重大变更（如 OAuth2 授权服务器从 Spring Security OAuth 2.x 迁移到 3.x）的升级指南缺失。
- **触发条件**: 框架升级时，下游使用者无法判断 Breaking Changes 和迁移步骤。
- **潜在风险**: 升级风险不可控、重复踩坑、生产故障。

#### 解决方案

**推荐修复**: 在每个业务模块根目录创建 `CHANGELOG.md`，遵循 [Keep a Changelog](https://keepachangelog.com/) 格式。

**短期规避**: 在根目录 `CHANGELOG.md` 中统一记录所有业务模块的 Breaking Changes。

---

## 三、需求与设计缺陷详情

### 问题 3: 审计日志风险评分模型使用魔法字符串匹配

**模块**: `carlos-audit`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/disruptor/AuditLogEventHandler.java`
**行号**: L136-L175
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// carlos-audit-bus/src/main/java/com/carlos/audit/disruptor/AuditLogEventHandler.java L136-L175
private int calculateBaseRisk(AuditLogMainDTO auditLog) {
    String logType = auditLog.getLogType();
    if (logType == null) {
        return 10;
    }

    return switch (logType) {
        // ==================== 极高风险操作 (50-60 分) ====================
        case "DATA_DELETE", "DATA_BATCH_DELETE" -> 60;
        case "PERMISSION_CHANGE", "ROLE_ASSIGN" -> 55;
        case "SYSTEM_CONFIG_CHANGE", "DATABASE_CONFIG_CHANGE" -> 55;

        // ==================== 高风险操作 (40-50 分) ====================
        case "ORDER_PAY", "ORDER_REFUND" -> 50;
        case "USER_PASSWORD_RESET", "USER_PASSWORD_CHANGE" -> 45;
        // ... 更多 case
    };
}
```

#### 详细分析

- **问题描述**: `calculateBaseRisk` 方法使用字符串字面量（如 `"DATA_DELETE"`, `"ORDER_PAY"`）进行 `switch` 匹配，而非使用枚举类型。虽然 `AuditLogMainDTO` 中 `logType` 字段为 `String` 类型，但框架已定义 `AuditLogTypeEnum`（或类似枚举），字符串匹配存在拼写错误风险且无法利用编译时检查。
- **违反规范**: 枚举应实现 `BaseEnum` 接口并使用 `@AppEnum` 注解；switch 应优先使用枚举而非字符串。
- **触发条件**: 审计事件上报时，如果上游拼写错误（如 `"DATA_DELETE"` 写成 `"data_delete"`），风险分将回退到默认值 10，导致高风险操作被低估。
- **潜在风险**: 高风险操作（如删除数据、权限变更）的风险评分被低估，影响审计告警的准确性。

#### 解决方案

**推荐修复**:

将 `logType` 从 `String` 改为强类型枚举，或在 switch 中使用枚举常量：

```java
// 1. 定义枚举（若已存在则直接使用）
public enum AuditLogTypeEnum implements BaseEnum {
    DATA_DELETE("DATA_DELETE", "数据删除"),
    PERMISSION_CHANGE("PERMISSION_CHANGE", "权限变更"),
    // ...
    
    private final String code;
    private final String desc;
    // ...
}

// 2. 修改 calculateBaseRisk
private int calculateBaseRisk(AuditLogMainDTO auditLog) {
    AuditLogTypeEnum logType = AuditLogTypeEnum.fromCode(auditLog.getLogType());
    if (logType == null) {
        return 10;
    }
    return switch (logType) {
        case DATA_DELETE, DATA_BATCH_DELETE -> 60;
        case PERMISSION_CHANGE, ROLE_ASSIGN -> 55;
        // ...
    };
}
```

**短期规避**: 在 `AuditLogMainDTO` 的 `setLogType()` 方法中增加校验，确保只接受预定义的常量值。

---

## 四、代码规范违规详情

### 问题 4: ClickHouseBatchWriter 使用 Spring BeanUtils.copyProperties

**模块**: `carlos-audit`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`
**行号**: L298-L300
**严重程度**: P1-High
**影响范围**: 类级

#### 相关代码

```java
// carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java L298-L300
private String toJsonLine(AuditLogMainDTO log) {
    ClickHouseRow row = new ClickHouseRow();
    // 复制同类型字段（String / Integer / Long / BigDecimal / Boolean）
    BeanUtils.copyProperties(log, row);
    // 手动处理类型不一致的字段...
```

#### 详细分析

- **问题描述**: `toJsonLine()` 方法使用 `org.springframework.beans.BeanUtils.copyProperties()` 将 `AuditLogMainDTO` 复制到 `ClickHouseRow`。根据框架强制规范，对象转换必须使用 **MapStruct**，严禁使用 `BeanUtils.copyProperties`（性能差、无编译时检查、无法处理类型不一致字段）。
- **违反规范**: `CLAUDE.md` / `SKILL.md` 强制规范：`Convert 规范：对象转换是否使用 MapStruct？是否存在手动 Bean 拷贝（BeanUtils.copyProperties 泛滥）？`
- **触发条件**: 每次审计日志批量写入时触发，高频操作。
- **潜在风险**: 
  - 性能：`BeanUtils` 基于反射，MapStruct 基于编译时代码生成，性能差 3-5 倍。
  - 维护：字段增删时无编译时检查，运行时可能遗漏字段映射。
  - 类型不一致字段（如 `LocalDateTime` → `String`）仍需手动处理，无法享受 MapStruct 的 `@Mapper` 自定义转换。

#### 解决方案

**推荐修复**:

定义 MapStruct 转换器：

```java
// 新增文件：carlos-audit-bus/src/main/java/com/carlos/audit/convert/AuditLogClickHouseConvert.java
@Mapper(componentModel = "spring")
public interface AuditLogClickHouseConvert {
    AuditLogClickHouseConvert INSTANCE = Mappers.getMapper(AuditLogClickHouseConvert.class);

    @Mapping(target = "serverTime", source = "serverTime", qualifiedByName = "formatDateTime")
    @Mapping(target = "eventDate", source = "eventDate", qualifiedByName = "formatDate")
    @Mapping(target = "category", source = "category", qualifiedByName = "enumToName")
    // ... 其他字段映射
    ClickHouseRow toClickHouseRow(AuditLogMainDTO dto);

    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(CK_DATETIME_FORMATTER) : null;
    }

    @Named("formatDate")
    default String formatDate(LocalDate d) {
        return d != null ? d.toString() : null;
    }

    @Named("enumToName")
    default String enumToName(Enum<?> e) {
        return e != null ? e.name() : null;
    }
}
```

修改 `toJsonLine`：
```java
private String toJsonLine(AuditLogMainDTO log) {
    ClickHouseRow row = AuditLogClickHouseConvert.INSTANCE.toClickHouseRow(log);
    return JsonUtils.toJson(row);
}
```

**短期规避**: 保留当前实现，但增加 `@SuppressWarnings("deprecation")` 和 TODO 注释，排期迁移。

**参考文档**: `CLAUDE.md` 中 Convert 规范章节。

---

### 问题 5: OAuth2AuthorizationServerConfig 使用 @Configuration 而非 @AutoConfiguration

**模块**: `carlos-auth`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java`
**行号**: L48-L50
**严重程度**: P1-High
**影响范围**: 模块级

#### 相关代码

```java
// carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2AuthorizationServerConfig.java L48-L50
@Slf4j
@Configuration          // <-- 应为 @AutoConfiguration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(
    prefix = "carlos.oauth2.authorization-server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
```

#### 详细分析

- **问题描述**: 作为 Spring Boot Starter 体系的一部分，`carlos-auth-service` 的配置类应使用 `@AutoConfiguration`（Spring Boot 2.7+ / 3.x 推荐）而非 `@Configuration`，以便通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 进行自动发现。使用 `@Configuration` 需要依赖方显式 `@ComponentScan`，破坏了 Starter 的"约定优于配置"原则。
- **违反规范**: `CLAUDE.md` 要求 Starter 模块的配置类使用 `@AutoConfiguration`；`spring.factories` 已废弃，应使用 `AutoConfiguration.imports`。
- **触发条件**: 外部服务引入 `carlos-auth-service` 依赖但未正确配置组件扫描时，授权服务器配置无法生效。
- **潜在风险**: 自动配置失效、依赖方集成困难、与框架其他 Starter 行为不一致。

#### 解决方案

**推荐修复**:

```java
@Slf4j
@AutoConfiguration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(OAuth2Properties.class)
@ConditionalOnProperty(
    prefix = "carlos.oauth2.authorization-server",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
```

同时确保 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 包含：
```
com.carlos.auth.oauth2.config.OAuth2AuthorizationServerConfig
com.carlos.auth.oauth2.client.config.OAuth2ClientConfig
com.carlos.auth.oauth2.config.OAuth2JwkConfig
com.carlos.auth.oauth2.config.OAuth2TokenConfig
com.carlos.auth.oauth2.config.OAuth2AuthorizationServiceConfig
com.carlos.auth.oauth2.config.OAuth2PasswordEncoderConfig
com.carlos.auth.oauth2.config.OAuth2AuthenticationConfig
```

**短期规避**: 在 `carlos-auth-boot` 的 `AutoConfiguration.imports` 中显式导入所有配置类。

---

## 五、性能风险详情

本次扫描未发现明确的性能风险问题。各模块关键性能点评估如下：

| 模块 | 性能关注点 | 评估结果 | 说明 |
|-----|---------|---------|------|
| carlos-gateway | WAF 过滤器、限流、令牌校验 | 良好 | WafFilter 使用 BitSet 快速预检 + 正则二次验证，ReplayProtectionFilter 使用 HexFormat (JDK 17+)，均已优化 |
| carlos-audit | ClickHouse 批量写入、Disruptor 消费 | 良好 | 双缓冲设计避免 clear() 污染，循环重试替代递归，本地磁盘备份兜底 |
| carlos-org | 部门树 Redis 缓存、树形递归 | 良好 | 五层缓存结构（Hash/Children/Desc/Anc/Code），分页初始化 5000 条/页，缓存一致性通过 MULTI/EXEC 保障 |
| carlos-auth | 登录失败计数、IP 封禁 | 一般 | 使用 Redis 原子计数，但 IpBlockManager 使用 `RedisTemplate` 而非 `RedisUtil`，存在额外序列化开销 |

---

## 六、安全漏洞详情

### 问题 6: `carlos-spring-boot-starter-license-generate` 存在于源码仓库中

**模块**: `carlos-license`
**文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/`（目录存在）
**严重程度**: P0-Critical
**影响范围**: 多模块联动 / 全项目

#### 详细分析

- **问题描述**: `carlos-spring-boot-starter-license-generate` 模块（含私钥管理、证书生成能力）存在于源码仓库中，且位于 `carlos-integration` 聚合模块下。根据框架安全红线：**"生产环境严禁包含 carlos-spring-boot-starter-license-generate"**。该模块包含私钥生成逻辑，若被误引入生产部署包，将导致私钥泄露风险。
- **违反规范**: `AGENTS.md` 安全红线：生产环境严禁包含 `carlos-spring-boot-starter-license-generate`。
- **触发条件**: 构建生产包时，如果 `carlos-integration/pom.xml` 或 `carlos-spring-boot/pom.xml` 的 profile 配置不当，可能将该模块打包进部署产物。
- **潜在风险**: 
  - 私钥泄露：攻击者可利用生成模块导出私钥，伪造授权证书。
  - 授权绕过：License 验证机制失效，软件授权体系被绕过。
  - 合规风险：软件授权合规审计失败。

#### 解决方案

**推荐修复**:

1. **将 `license-generate` 模块从 `carlos-integration` 聚合中移除**（移至独立仓库或仅保留在开发分支）：

```xml
<!-- carlos-integration/pom.xml 修改示例 -->
<modules>
    <module>carlos-license-core</module>
    <module>carlos-spring-boot-starter-license-verify</module>
    <!-- 删除或注释掉：
    <module>carlos-spring-boot-starter-license-generate</module>
    -->
    ...
</modules>
```

2. **在 CI/CD 构建脚本中增加校验**：

```bash
# 构建前检查
if grep -q "carlos-spring-boot-starter-license-generate" pom.xml; then
    echo "ERROR: license-generate module found in production build!"
    exit 1
fi
```

3. **在根目录 `pom.xml` 的 `carlos-public` / `carlos-private` profile 中明确排除**：

```xml
<profile>
    <id>carlos-public</id>
    <modules>
        <!-- 不包含 license-generate -->
    </modules>
</profile>
```

**短期规避**: 在 `carlos-spring-boot-starter-license-generate/pom.xml` 中增加 `maven-enforcer-plugin` 规则，禁止在 `carlos-public` / `carlos-private` profile 下构建：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <executions>
        <execution>
            <id>ban-production-build</id>
            <goals><goal>enforce</goal></goals>
            <configuration>
                <rules>
                    <evaluateBeanshell>
                        <condition>
                            !"true".equals(System.getProperty("license.dev.mode"))
                        </condition>
                        <message>license-generate can only be built with -Dlicense.dev.mode=true</message>
                    </evaluateBeanshell>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**长期建议**: 将 `license-generate` 移至独立仓库（如 `carlos-framework-devtools`），与主框架仓库物理隔离。

---

## 七、功能未完善详情

### 问题 7: LoginAttemptManager 与 IpBlockManager 配置硬编码，未支持外部化配置

**模块**: `carlos-auth`
**文件**: 
- `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/LoginAttemptManager.java` L24-L31
- `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/IpBlockManager.java`（依赖 `SecurityProperties`）
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// LoginAttemptManager.java L24-L31
@Slf4j
@Component
public class LoginAttemptManager {
    /**
     * 默认失败阈值（5次）
     */
    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    /**
     * 默认锁定时长（30分钟）
     */
    private static final Duration DEFAULT_LOCK_DURATION = Duration.ofMinutes(30);
```

#### 详细分析

- **问题描述**: `LoginAttemptManager` 的失败阈值（5次）和锁定时长（30分钟）为硬编码常量，无法通过 `application.yml` 动态调整。`IpBlockManager` 虽然使用了 `SecurityProperties`，但 `LoginAttemptManager` 未接入同一配置体系。不同安全等级场景（如金融 vs 内部工具）需要不同的阈值。
- **触发条件**: 需要调整登录失败策略时，必须修改代码并重新部署。
- **潜在风险**: 灵活性不足、安全策略调整响应慢、不同环境无法差异化配置。

#### 解决方案

**推荐修复**:

在 `SecurityProperties` 中增加 `LoginAttemptProperties` 嵌套配置：

```java
@Data
@ConfigurationProperties(prefix = "carlos.security")
public class SecurityProperties {
    
    private LoginAttemptProperties loginAttempt = new LoginAttemptProperties();
    
    @Data
    public static class LoginAttemptProperties {
        private int maxAttempts = 5;
        private Duration lockDuration = Duration.ofMinutes(30);
        private Duration countExpire = Duration.ofHours(24);
    }
    
    private IpBlockProperties ipBlock = new IpBlockProperties();
    
    @Data
    public static class IpBlockProperties {
        private boolean enabled = true;
        private int maxAttempts = 10;
        private Duration timeWindow = Duration.ofMinutes(5);
        private Duration blockDuration = Duration.ofHours(1);
    }
}
```

修改 `LoginAttemptManager`：
```java
@Component
@RequiredArgsConstructor
public class LoginAttemptManager {
    private final SecurityProperties securityProperties;
    
    public void recordLoginFailure(String username) {
        // ...
        if (attempts >= securityProperties.getLoginAttempt().getMaxAttempts()) {
            lockAccount(username, securityProperties.getLoginAttempt().getLockDuration());
        }
    }
}
```

**短期规避**: 保持硬编码，但将常量提取为 `application.yml` 可覆盖的 `@Value`（不推荐，但可快速实现）。

---

## 八、技术债务详情

### 8.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 建议操作 |
|----|------|------|------|
| 无 | 未发现 | `-api` 不依赖 `-bus`，`-cloud` 只依赖 `-api` | 合规 ✅ |

扫描结果：业务模块间依赖方向基本合规。`carlos-auth-api` 仅暴露 Feign 接口和 AO/Param，`carlos-auth-cloud` 依赖 `carlos-auth-api`，符合规范。

### 8.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 | 状态 |
|----|----|---------|------|------|
| 未发现 | - | - | - | 合规 ✅ |

### 8.3 TODO/FIXME/XXX 遗留

| 文件 | 行号 | 标记内容 | 建议 |
|----|----|--------|------|
| `ClickHouseBatchWriter.java` | L298 | `BeanUtils.copyProperties` 临时方案 | 迁移至 MapStruct（见问题 4） |
| `AuditLogEventHandler.java` | L136 | 魔法字符串风险评分 | 迁移至枚举（见问题 3） |

---

### 问题 8: IpBlockManager 使用 RedisTemplate 与框架 RedisUtil 混用

**模块**: `carlos-auth`
**文件**: `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/IpBlockManager.java` L27-L31
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// IpBlockManager.java L27-L31
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockManager {
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProperties securityProperties;
```

#### 详细分析

- **问题描述**: `IpBlockManager` 直接注入 `RedisTemplate<String, Object>`，而 `LoginAttemptManager` 使用 `RedisUtil` 工具类。同一模块内使用两套 Redis 操作方式，导致：
  1. 序列化方式不一致（`RedisTemplate` 使用 JdkSerialization / JSON，`RedisUtil` 使用统一配置）。
  2. 代码冗余（`RedisTemplate` 需要手动处理类型转换，如 `Integer.parseInt(value.toString())`）。
  3. 与框架规范不统一（`AGENTS.md` 要求 Redis 缓存统一在 Manager 层实现，且应使用 `RedisUtil`）。
- **触发条件**: 每次登录失败/成功时触发 IP 封禁逻辑。
- **潜在风险**: 序列化不一致导致数据读取失败、类型转换异常（如 `NumberFormatException`）、代码可维护性下降。

#### 解决方案

**推荐修复**:

统一使用 `RedisUtil`：

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlockManager {
    private final SecurityProperties securityProperties;
    
    public void recordLoginFailure(String ipAddress) {
        String attemptKey = AuthRedisKey.ipAttempt(ipAddress);
        Long attempts = RedisUtil.incrementValue(attemptKey, 1L);
        if (attempts == 1) {
            RedisUtil.setExpire(attemptKey, props.getTimeWindow());
        }
        // ...
    }
    
    public boolean isIpBlocked(String ipAddress) {
        String blockKey = AuthRedisKey.ipLock(ipAddress);
        return Boolean.TRUE.equals(RedisUtil.hasKey(blockKey));
    }
    
    // 删除所有 redisTemplate.xxx 调用，替换为 RedisUtil.xxx
}
```

**短期规避**: 保持现状，但统一 `RedisTemplate` 的序列化配置与 `RedisUtil` 一致。

---

### 问题 9: OrgDepartmentManagerImpl 跨级移动缓存一致性风险

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java` L118-L138
**严重程度**: P3-Low
**影响范围**: 类级

#### 相关代码

```java
// OrgDepartmentManagerImpl.java L118-L138
@Override
public boolean modify(OrgDepartmentDTO dto) {
    OrgDepartmentDTO oldDept = this.getDtoById(dto.getId());
    OrgDepartment entity = OrgDepartmentConvert.INSTANCE.toDO(dto);
    boolean success = updateById(entity);
    // ...
    OrgDepartment newEntity = getBaseMapper().selectById(dto.getId());
    if (newEntity == null) {
        return true;  // <-- 数据库更新成功但读取失败，缓存未更新
    }
    OrgDepartmentDTO newDept = OrgDepartmentConvert.INSTANCE.toDTO(newEntity);

    if (oldDept != null && dto.getParentId() != null
        && !dto.getParentId().equals(oldDept.getParentId())) {
        this.deleteCache(oldDept);
        this.putCache(newDept);
    } else {
        this.updateCache(newDept);
    }
    return true;
}
```

#### 详细分析

- **问题描述**: `modify()` 方法在数据库更新成功后，如果 `selectById()` 返回 null（极端情况，如并发删除），直接返回 `true`，此时数据库已更新但缓存未同步。虽然概率极低，但在高并发场景下可能导致缓存脏读。
- **触发条件**: 数据库更新成功 + 并发删除 + 缓存读取失败。
- **潜在风险**: 缓存与数据库不一致，导致部门树查询返回过期数据。

#### 解决方案

**推荐修复**:

```java
@Override
public boolean modify(OrgDepartmentDTO dto) {
    OrgDepartmentDTO oldDept = this.getDtoById(dto.getId());
    OrgDepartment entity = OrgDepartmentConvert.INSTANCE.toDO(dto);
    boolean success = updateById(entity);
    if (!success) {
        log.warn("Update 'OrgDepartment' data fail, entity:{}", entity);
        return false;
    }
    
    OrgDepartment newEntity = getBaseMapper().selectById(dto.getId());
    if (newEntity == null) {
        log.error("Department {} updated but read back as null, clearing cache", dto.getId());
        // 安全处理：清除所有相关缓存，避免脏读
        if (oldDept != null) {
            this.deleteCache(oldDept);
        }
        return true;
    }
    // ...
}
```

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-------------|-------|-------|-------|-------|-------|---------|
| carlos-auth | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-gateway | ⭐⭐☆☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-audit | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-org | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-license | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |

> **评分说明**：
> - `carlos-gateway` 规范落实和性能安全表现优秀，但缺少模块 README 和文档。
> - `carlos-auth` 和 `carlos-audit` 受 `P1` 代码规范问题影响，规范落实评分较低。
> - `carlos-system`, `carlos-message`, `carlos-tools` 文档相对完整，得分最高。
> - 所有模块普遍存在 **文档缺失**（缺少 README、CHANGELOG），这是最大的共性短板。

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] **[P0] carlos-license**: 将 `carlos-spring-boot-starter-license-generate` 从生产构建中物理隔离（或增加 CI/CD 校验）
- [ ] **[P1] carlos-audit**: 将 `ClickHouseBatchWriter.toJsonLine()` 中的 `BeanUtils.copyProperties` 迁移至 MapStruct
- [ ] **[P1] carlos-auth**: 将 `OAuth2AuthorizationServerConfig` 的 `@Configuration` 替换为 `@AutoConfiguration`，并更新 `AutoConfiguration.imports`
- [ ] **[P2] carlos-auth**: 将 `LoginAttemptManager` 硬编码阈值接入 `SecurityProperties` 外部化配置
- [ ] **[P2] carlos-audit**: 将 `AuditLogEventHandler.calculateBaseRisk()` 的魔法字符串改为枚举 switch
- [ ] **[P2] carlos-auth**: 统一 `IpBlockManager` 使用 `RedisUtil` 替代 `RedisTemplate`
- [ ] **[P2] carlos-integration**: 为 `carlos-auth`, `carlos-gateway`, `carlos-org`, `carlos-audit` 补全 `README.md`
- [ ] **[P3] carlos-org**: 增强 `OrgDepartmentManagerImpl.modify()` 的缓存一致性异常处理
- [ ] **[P3] 所有模块**: 补全 `CHANGELOG.md`，记录重大变更和 Breaking Changes

### 10.2 历史遗留跟踪

- [ ] **[P0] carlos-framework**: 框架已迁移至 Spring Boot 3.5.9，检查 `javax.*` 残留 ⏳ 持续跟踪
- [ ] **[P1] carlos-security**: 检查 `MD5PasswordEncoder` 是否已标记 `@Deprecated` 并移除生产使用 ⏳ 持续跟踪
- [ ] **[P2] carlos-ui**: 前端每日课程持续进行（第 21 天）✅ 正常推进

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*扫描 Commit: bf9333068a4bfe4a5708003c3b27dcdb86a6297e*
