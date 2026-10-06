# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-02 08:00  
**扫描 Commit**: `241982a001f5bd792a910226343b26aaf340da01`  
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）  
**Git 拉取状态**: 成功（已是最新）  
**今日变更**: 无新增业务模块代码变更

**发现问题总数**: 9 个（P0: 3 个, P1: 5 个, P2: 3 个, P3: 0 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- **Git 拉取状态**: 成功
- **Commit**: `241982a001f5bd792a910226343b26aaf340da01`
- **变更文件数**: 0 个（业务模块无新增变更）
- **新增代码**: 0 行
- **删除代码**: 0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 0 | 0 | 0 |
| 需求与设计缺陷 | 1 | 2 | 0 | 0 | 3 |
| 代码规范违规 | 0 | 0 | 0 | 0 | 0 |
| 性能风险 | 0 | 0 | 0 | 0 | 0 |
| 安全漏洞 | 1 | 2 | 0 | 0 | 3 |
| 功能未完善 | 1 | 1 | 1 | 0 | 3 |
| 技术债务 | 0 | 0 | 2 | 0 | 2 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | 合计 |
|-----|----|----|----|------|
| carlos-auth | 1 | 1 | 0 | 2 |
| carlos-gateway | 0 | 0 | 0 | 0 |
| carlos-audit | 1 | 1 | 1 | 3 |
| carlos-message | 0 | 1 | 0 | 1 |
| carlos-org | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 0 | 1 | 1 |
| carlos-license | 1 | 2 | 0 | 3 |
| carlos-tools | 0 | 0 | 2 | 2 |

### 1.4 今日重点关注
- **[TOP 1] P0-001: license-generate 模块存在于集成层** — 生产环境严禁包含，存在私钥泄露风险
- **[TOP 2] P0-002: DeviceFingerprint 使用 MD5** — 密码学上已破解，不应用于安全场景
- **[TOP 3] P0-003: AuditLogQueryService 核心功能未实现** — 所有查询方法都是空实现，审计系统无法查询

---

## 二、安全漏洞详情（最高优先级）

### 问题 1: P0-001 carlos-spring-boot-starter-license-generate 存在于集成层

**模块**: `carlos-license`  
**文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/pom.xml`  
**严重程度**: P0-Critical  
**影响范围**: 框架级 — 生产环境若误包含此模块，将导致私钥泄露和证书伪造

#### 详细分析

- **问题描述**: `carlos-spring-boot-starter-license-generate` 是 License 私钥生成模块，包含私钥管理和证书创建能力。该模块存在于 `carlos-integration` 业务集成层中，与 `carlos-license-core` 和 `carlos-spring-boot-starter-license-verify` 同处一个父模块下。
- **违反原则**: AGENTS.md 安全红线明确声明："生产环境严禁包含 carlos-spring-boot-starter-license-generate"
- **触发条件**: 若生产环境的依赖配置误包含此模块，或开发者不了解此限制而引入
- **潜在风险**:
  - 私钥泄露：生成模块包含私钥，一旦部署到生产环境，攻击者可通过 `/license/generate` 接口创建任意有效证书
  - 证书伪造：攻击者可绕过 License 验证，无限期使用软件
  - 授权失效：软件授权机制完全失效

#### 相关代码

```java
// carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/...
// LicenseCreatorController.java
@RestController
@RequestMapping("/license")
public class LicenseCreatorController {
    
    @PostMapping("/generate")
    public String generateLicense(@RequestBody LicenseCreatorParam param) {
        return licenseCreatorService.generateLicense(param);  // ← 可生成任意证书
    }
    
    @GetMapping("/getServerInfos")
    public LicenseCheckModel getServerInfos(OSType osType) {
        return licenseCreatorService.getServerInfos(osType);  // ← 可获取服务器硬件信息
    }
}
```

#### 解决方案

**推荐修复**:

```xml
<!-- 在 carlos-license/pom.xml 中，将 generate 模块标记为可选，并添加注释 -->
<modules>
    <module>carlos-license-core</module>
    <module>carlos-spring-boot-starter-license-verify</module>
    <!-- 
        ⚠️ 安全警告：以下模块仅用于开发环境生成证书，严禁包含在生产环境！
        生产环境构建时必须排除：mvn clean install -pl !carlos-spring-boot-starter-license-generate
    -->
    <module>carlos-spring-boot-starter-license-generate</module>
</modules>
```

**短期规避**:
- 立即在 CI/CD 构建脚本中添加 `-pl !carlos-spring-boot-starter-license-generate` 排除
- 在文档中明确标注此模块的安全风险

**长期建议**:
- 将 `license-generate` 模块从 `carlos-integration` 中彻底移出，作为独立仓库管理
- 或将其标记为 `<scope>provided</scope>` 并添加 Maven Enforcer 插件禁止生产环境使用

---

### 问题 2: P0-002 DeviceFingerprint 使用 MD5 哈希算法

**模块**: `carlos-auth`  
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/util/DeviceFingerprint.java`  
**行号**: L33, L55  
**严重程度**: P0-Critical  
**影响范围**: 模块级 — 设备指纹用于安全认证场景（MFA 可信设备），MD5 已不具备抗碰撞能力

#### 相关代码

```java
@Component
public class DeviceFingerprint {
    
    public String generate(HttpServletRequest request) {
        String fingerprintData = "%s|%s|%s|%s".formatted(
            userAgent != null ? userAgent : "",
            accept != null ? accept : "",
            acceptLanguage != null ? acceptLanguage : "",
            acceptEncoding != null ? acceptEncoding : ""
        );
        // 生成MD5指纹 ← 使用已破解的哈希算法
        return SecureUtil.md5(fingerprintData);  // L33
    }
    
    public String generateEnhanced(HttpServletRequest request, ...) {
        // ...
        return SecureUtil.md5(enhancedData);  // L55
    }
}
```

#### 详细分析

- **问题描述**: `DeviceFingerprint` 使用 MD5 算法生成设备指纹。MD5 已被密码学社区证明存在严重碰撞漏洞，不再适合任何安全相关场景。设备指纹用于多因素认证（MFA）中的可信设备识别，如果攻击者能构造碰撞，可能绕过可信设备验证。
- **违反原则**: CWE-327: Use of a Broken or Risky Cryptographic Algorithm
- **触发条件**: 任何使用设备指纹进行可信设备验证的场景
- **潜在风险**:
  - 碰撞攻击：攻击者可以构造具有相同 MD5 哈希的不同设备指纹，绕过可信设备检查
  - MFA 绕过：如果攻击者能够伪造可信设备指纹，可能跳过 MFA 验证步骤

#### 解决方案

**推荐修复**:

```java
@Component
public class DeviceFingerprint {
    
    // 使用 SHA-256 替代 MD5
    public String generate(HttpServletRequest request) {
        String fingerprintData = "%s|%s|%s|%s".formatted(
            userAgent != null ? userAgent : "",
            accept != null ? accept : "",
            acceptLanguage != null ? acceptLanguage : "",
            acceptEncoding != null ? acceptEncoding : ""
        );
        return SecureUtil.sha256(fingerprintData);  // ✅ 使用 SHA-256
    }
    
    public String generateEnhanced(HttpServletRequest request, ...) {
        // ...
        return SecureUtil.sha256(enhancedData);  // ✅ 使用 SHA-256
    }
}
```

**参考文档**:
- [CWE-327: Use of a Broken or Risky Cryptographic Algorithm](https://cwe.mitre.org/data/definitions/327.html)
- [MD5 碰撞攻击](https://www.win.tue.nl/hashclash/)

---

### 问题 3: P0-003 AuditLogQueryService 核心查询功能全部未实现

**模块**: `carlos-audit`  
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`  
**行号**: L45-L100  
**严重程度**: P0-Critical  
**影响范围**: 模块级 — 审计系统无法执行查询，形同虚设

#### 相关代码

```java
@Service
public class AuditLogQueryService {
    
    public Paging<AuditLogMainDTO> pageQuery(...) {
        // TODO: 实现 ClickHouse 查询
        return new Paging<>();  // ← 返回空分页
    }
    
    public AuditLogMainDTO getById(Long id) {
        // TODO: 实现查询
        return null;  // ← 返回 null
    }
    
    public AuditLogStatsVO getRealtimeStats(...) {
        // TODO: 从物化视图查询统计
        return new AuditLogStatsVO();  // ← 返回空对象
    }
    
    public List<AuditLogMainDTO> listRiskEvents(...) {
        // TODO: 实现查询
        return Collections.emptyList();  // ← 返回空列表
    }
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计系统的核心查询服务，但所有方法都是空实现（仅返回空对象/空列表）。这意味着审计日志虽然可以被收集和写入 ClickHouse，但无法被查询、统计和分析，审计系统形同虚设。
- **违反原则**: 功能完整性缺失，核心需求未完成
- **触发条件**: 任何调用审计查询接口的场景
- **潜在风险**:
  - 安全事件无法追溯：发生安全问题后无法查询审计日志进行溯源
  - 合规风险：无法满足等保/ISO 27001 等合规要求的审计日志可查询性
  - 系统监控缺失：无法获取实时审计统计和风险事件

#### 解决方案

**推荐修复**: 实现 ClickHouse 查询逻辑

```java
public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         String logType, String logStatus) {
    // 实现 ClickHouse 分页查询
    return auditLogMainManager.pageQueryFromClickHouse(tenantId, pageNum, pageSize, 
        startTime, endTime, logType, logStatus);
}
```

**短期规避**: 在审计查询接口上添加 `@Deprecated` 或返回明确的错误信息，告知使用者查询功能尚未实现。

---

## 三、需求与设计缺陷详情

### 问题 4: P1-001 GitlabMainController 硬编码 Token 占位符

**模块**: `carlos-tools`  
**文件**: `carlos-integration/carlos-tools/src/main/java/com/carlos/fx/gitlab/controller/GitlabMainController.java`  
**行号**: L202  
**严重程度**: P1-High  
**影响范围**: 类级

#### 相关代码

```java
private void connectToServer(String serverUrl) {
    // 临时使用硬编码的 Token
    String token = "your-gitlab-token";  // ← 硬编码占位符
    gitLabApi = new GitLabApi(serverUrl, token);
    // ...
}
```

#### 详细分析

- **问题描述**: `GitlabMainController` 是 GUI 工具模块中的 GitLab 管理工具，在 `connectToServer()` 方法中使用了硬编码的 Token 占位符 `"your-gitlab-token"`。虽然这是 TODO 标记的临时实现，但代码已经存在于代码库中，开发者可能在测试时直接使用此占位符，存在误提交真实 Token 的风险。
- **解决方案**: 使用配置中心或本地配置文件存储 Token，启动时检测 Token 是否配置，未配置则弹出输入对话框。

---

### 问题 5: P1-002 AuditProperties ClickHouse 默认密码为空

**模块**: `carlos-audit`  
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/config/AuditProperties.java`  
**行号**: L85  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 相关代码

```java
@Data
public static class ClickHouse {
    private String username = "default";
    private String password = "";  // ← 默认空密码
}
```

#### 详细分析

- **问题描述**: `AuditProperties.ClickHouse` 中 `password` 默认值为空字符串。ClickHouse 默认情况下通常不启用认证，但如果配置了密码认证，空密码可能导致连接失败。更严重的是，如果开发者未配置密码而部署到生产环境，且 ClickHouse 恰好配置了弱认证，可能导致未授权访问。
- **解决方案**: 将默认值设为 null 或移除默认值，强制要求配置文件中显式设置密码。添加 `@NotBlank` 校验。

---

### 问题 6: P1-003 MessageAntiSpamService 使用 MD5 生成消息指纹

**模块**: `carlos-message`  
**文件**: `carlos-integration/carlos-message/carlos-message-bus/src/main/java/com/carlos/message/service/MessageAntiSpamService.java`  
**行号**: L96  
**严重程度**: P1-High  
**影响范围**: 方法级

#### 相关代码

```java
public String generateFingerprint(String content, String receiverId, String channelCode) {
    String source = (content != null ? content : "") + "|" + 
                    (receiverId != null ? receiverId : "") + "|" + 
                    (channelCode != null ? channelCode : "");
    return DigestUtil.md5Hex(source);  // ← MD5 用于指纹生成
}
```

#### 详细分析

- **问题描述**: 消息防刷服务使用 MD5 生成消息指纹用于去重。虽然 MD5 的碰撞性在去重场景影响较小（攻击者难以构造具有相同 MD5 的恶意消息），但 MD5 的计算效率已不再是优势，SHA-256 在大多数 JVM 上性能接近。更重要的是，MD5 作为已废弃的算法，不应在新代码中使用。
- **解决方案**: 使用 `DigestUtil.sha256Hex()` 替代 `md5Hex()`。

---

### 问题 7: P1-004 DefaultExtendUserDetailsService 角色 ID 映射使用 hashCode 可能碰撞

**模块**: `carlos-auth`  
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/service/DefaultExtendUserDetailsService.java`  
**行号**: L110-L120  
**严重程度**: P1-High  
**影响范围**: 方法级

#### 相关代码

```java
for (String roleCode : userInfo.getRoleCodes()) {
    if ("ADMIN".equals(roleCode)) {
        roleIds.add(1L);
    } else if ("USER".equals(roleCode)) {
        roleIds.add(2L);
    } else {
        roleIds.add((long) roleCode.hashCode());  // ← hashCode 可能碰撞
    }
}
```

#### 详细分析

- **问题描述**: 默认用户详情服务中，对于非 ADMIN/USER 的角色，使用 `roleCode.hashCode()` 作为 roleId。`String.hashCode()` 在 Java 中可能产生碰撞（不同字符串产生相同哈希值），这会导致两个不同角色的用户被分配相同的 roleId，进而可能导致权限混乱。
- **解决方案**: 使用确定性映射（如角色名到数据库角色表查询），或使用 UUID/雪花算法生成唯一 ID。

---

### 问题 8: P1-005 LicenseCreatorController 接口缺乏认证保护

**模块**: `carlos-license`  
**文件**: `carlos-integration/carlos-license/carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorController.java`  
**行号**: L28-L55  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 相关代码

```java
@RestController
@RequestMapping("/license")
public class LicenseCreatorController {
    
    @GetMapping("/getServerInfos")
    public LicenseCheckModel getServerInfos(OSType osType) {
        return licenseCreatorService.getServerInfos(osType);  // ← 无认证
    }
    
    @PostMapping("/generate")
    public String generateLicense(@RequestBody LicenseCreatorParam param) {
        return licenseCreatorService.generateLicense(param);  // ← 无认证
    }
    
    @GetMapping("/download")
    public void downloadLicense(String id, HttpServletResponse response) throws Throwable {
        licenseCreatorService.downloadLicense(id, response);  // ← 无认证
    }
}
```

#### 详细分析

- **问题描述**: `LicenseCreatorController` 的所有接口（获取服务器信息、生成证书、下载证书）都没有任何认证/授权注解。即使此模块仅在开发环境使用，也缺乏基本的安全保护。如果误部署到生产环境，任何人都可以调用这些接口。
- **解决方案**: 添加 `@PreAuthorize("hasRole('ADMIN')")` 或至少添加 `@ConditionalOnProperty` 限制仅开发环境可用。

---

## 四、技术债务详情

### 问题 9: P2-001 carlos-tools 模块大量 TODO 遗留

**模块**: `carlos-tools`  
**文件**: 多个文件  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// GitlabMainController.java
// TODO: 从配置文件加载服务器列表
// TODO: 显示服务器配置对话框
// TODO: 从配置文件获取对应的 Token
String token = "your-gitlab-token";

// CodeGeneratorController.java
// TODO: Carlos 2026-02-14

// CodeGeneratorService.java
// TODO: Carlos 2020/9/9 如果是多模块模板...
```

#### 详细分析

- **问题描述**: `carlos-tools` 模块存在大量 TODO 注释，部分最早可追溯至 2020 年。这些 TODO 标记的代码长期未实现，导致工具模块功能不完整。
- **解决方案**: 建立 TODO 清理机制，超过一定时间的 TODO 必须转化为 Issue 或删除。

---

### 问题 10: P2-002 SysDictManagerImpl 未实现缓存管理接口方法

**模块**: `carlos-system`  
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/dict/manager/impl/SysDictManagerImpl.java`  
**严重程度**: P2-Medium  
**影响范围**: 类级

#### 详细分析

- **问题描述**: `SysDictManagerImpl` 实现了 `ICacheManager<SysDictDTO>` 接口，但没有实现任何缓存操作方法（`putCache`, `deleteCache`, `updateCache`, `initCache` 等）。系统字典是高频读取的数据，缺乏缓存会导致每次查询都访问数据库。
- **解决方案**: 参考 `OrgDepartmentManagerImpl` 实现 Redis 缓存策略，或移除 `ICacheManager` 接口实现并添加注释说明缓存策略未实现的原因。

---

### 问题 11: P2-003 SysRegionManagerImpl 缓存失效 FIXME 遗留

**模块**: `carlos-system`  
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`  
**严重程度**: P2-Medium  
**影响范围**: 类级

#### 相关代码

```java
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
```

#### 详细分析

- **问题描述**: 行政区划管理器的缓存失效逻辑存在 FIXME 标记，说明缓存更新策略尚未完善。如果区域数据发生变更（如行政区划调整），缓存可能无法及时失效，导致数据不一致。
- **解决方案**: 完善缓存失效逻辑，在数据变更时清理相关缓存。

---

## 五、设计优秀模块评分

### 5.1 业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|------|---------|---------|---------|--------|--------|---------|
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | 4.0/5.0 |
| carlos-gateway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-audit | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐☆☆☆ | 3.2/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-tools | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | ⭐⭐☆☆☆ | 2.2/5.0 |

### 5.2 设计亮点

#### OAuth2AuthorizationServerConfig（⭐⭐⭐⭐⭐）

- **CSRF 保护完善**: P0-008 修复，启用 CSRF 但正确忽略 API 端点（`/oauth2/token`, `/oauth2/introspect` 等）
- **分层配置清晰**: 授权服务器端点过滤器链与默认安全过滤器链分离
- **条件装配精确**: 基于 `carlos.oauth2.authorization-server.enabled` 条件启用
- **Javadoc 完善**: 每个配置项都有详细的安全说明和注释

#### ClickHouseBatchWriter（⭐⭐⭐⭐⭐）

- **双缓冲设计**: 每次 flush 创建新 ArrayList，避免 `clear()` 污染异步数据
- **背压保护**: 缓冲区溢出时丢弃最旧数据，防止 OOM
- **优雅关闭**: `@PreDestroy` 执行最终刷新，`awaitTermination` 等待完成
- **本地备份兜底**: 配置 `localBackupOnFailure` 启用磁盘备份
- **循环重试**: 指数退避重试策略，避免递归栈溢出

#### AuditLogEventHandler（⭐⭐⭐⭐⭐）

- **多维度风险评分**: 基础(60) + 状态(20) + 主体(10) + 渠道(10) = 100 分模型
- **操作类型风险映射**: `DATA_DELETE=60`, `USER_LOGIN=20` 等合理分级
- **数据清洗 enrich**: 自动填充缺失字段（serverTime, eventDate, schemaVersion）
- **异常传递**: 抛出异常让 Disruptor 异常处理器处理，不吞异常

#### OrgDepartmentManagerImpl（⭐⭐⭐⭐⭐）

- **五层 Redis 缓存**: SELF(Hash) / CODE(映射) / CHILDREN(Set) / DESC(Set) / ANC(List)
- **缓存一致性**: 跨级移动时清理旧缓存关系再重建
- **分页初始化**: 5000 条/页批量加载，避免大 Key 问题
- **管道操作**: 使用 Redis Pipeline 批量写入，减少网络往返

#### CustomLicenseManager（⭐⭐⭐⭐⭐）

- **XMLDecoder 安全加固**: 白名单校验类名和方法名，防止反序列化攻击
- **XXE 防护**: 禁用 DTD，关闭 XInclude 和实体扩展
- **锁保护**: `ReentrantLock` 保护 License 创建/安装/校验操作
- **硬件绑定**: 校验 IP、Mac、主板序列号、CPU 序列号

#### UserContextRelayFilter（⭐⭐⭐⭐⭐）

- **用户上下文传递**: 通过 HTTP Header 将认证信息传递给下游服务
- **多租户支持**: 传递 tenantId 和 departmentId
- **链路追踪**: 自动注入 X-Request-ID
- **Ordered 优先级**: 在 OAuth2AuthenticationFilter 之后执行（order = -90）

---

## 六、待办事项与修复计划

### 6.1 今日新增待办

- [ ] **P0-001**: `carlos-license` — 将 `license-generate` 模块从生产构建中排除，或移出 `carlos-integration`
- [ ] **P0-002**: `carlos-auth` — 将 `DeviceFingerprint` 的 MD5 替换为 SHA-256
- [ ] **P0-003**: `carlos-audit` — 实现 `AuditLogQueryService` 的所有 ClickHouse 查询方法
- [ ] **P1-001**: `carlos-tools` — 移除 `GitlabMainController` 中的硬编码 Token，改为配置读取
- [ ] **P1-002**: `carlos-audit` — 修改 `AuditProperties.ClickHouse.password` 默认值为 null，强制显式配置
- [ ] **P1-003**: `carlos-message` — 将 `MessageAntiSpamService` 的 MD5 替换为 SHA-256
- [ ] **P1-004**: `carlos-auth` — 修复 `DefaultExtendUserDetailsService` 的 roleId 映射逻辑，避免 hashCode 碰撞
- [ ] **P1-005**: `carlos-license` — 为 `LicenseCreatorController` 添加认证/授权保护
- [ ] **P2-001**: `carlos-tools` — 清理遗留 TODO，建立 TODO 跟踪机制
- [ ] **P2-002**: `carlos-system` — 为 `SysDictManagerImpl` 实现缓存管理接口方法
- [ ] **P2-003**: `carlos-system` — 完善 `SysRegionManagerImpl` 缓存失效逻辑

### 6.2 历史遗留跟踪

- [ ] **P0-006**: `starter-encrypt` — ECB 模式已禁用 ✅ 已修复
- [ ] **P0-004**: `starter-security` — `UserContextAuthenticationFilter` 可信来源 IP 验证 ✅ 已修复

---

## 七、架构师评语

### 整体评价

Carlos Framework 业务集成层的整体质量**良好**，各核心业务模块（gateway、org、auth、message）的设计和实现都达到了较高水平。特别是在以下方面表现优秀：

1. **审计系统**: `ClickHouseBatchWriter` 的双缓冲 + 背压设计、`AuditLogEventHandler` 的多维度风险评分模型属于业界优秀水平
2. **网关层**: `OAuth2GatewayAutoConfiguration` 的 Token 验证器切换设计（JWT/Opaque）、`UserContextRelayFilter` 的用户上下文传递机制设计完善
3. **授权服务器**: `OAuth2AuthorizationServerConfig` 的 CSRF 保护配置精确，分层配置清晰
4. **组织架构**: `OrgDepartmentManagerImpl` 的五层 Redis 缓存设计精细，缓存一致性处理完善
5. **License 安全**: `CustomLicenseManager` 的 XMLDecoder 白名单 + XXE 防护是优秀的安全实践

### 主要风险

1. **license-generate 模块（P0）**: 生产环境若误包含此模块，将导致私钥泄露和证书伪造，必须立即建立构建排除机制
2. **MD5 使用（P0/P1）**: `DeviceFingerprint` 和 `MessageAntiSpamService` 使用 MD5，必须替换为 SHA-256
3. **审计查询缺失（P0）**: `AuditLogQueryService` 核心功能未实现，审计系统无法发挥价值

### 建议优先级

| 优先级 | 事项 | 预计工时 |
|--------|------|---------|
| 🔴 立即 | 排除 license-generate 生产风险（P0-001） | 1h |
| 🔴 立即 | 替换 DeviceFingerprint MD5（P0-002） | 1h |
| 🔴 立即 | 实现 AuditLogQueryService（P0-003） | 8h |
| 🟡 本周 | 替换 MessageAntiSpamService MD5（P1-003） | 1h |
| 🟡 本周 | 修复 GitlabMainController 硬编码 Token（P1-001） | 2h |
| 🟡 本周 | 修复 roleId hashCode 碰撞（P1-004） | 2h |
| 🟡 本周 | 为 LicenseCreatorController 添加认证（P1-005） | 2h |
| 🟢 本月 | 完善 SysDictManagerImpl 缓存（P2-002） | 4h |
| 🟢 本月 | 清理 carlos-tools TODO（P2-001） | 4h |

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
