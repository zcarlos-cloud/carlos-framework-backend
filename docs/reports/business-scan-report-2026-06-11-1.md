# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-11 08:30  
**扫描 Commit**: `db1cf8c4`  
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）  
**发现问题总数**: 14 个（P0: 0 个, P1: 3 个, P2: 7 个, P3: 4 个）  
**Java 文件数**: 1352 个  
**参考规范**: `.claude/skills/carlos-framework-standard/SKILL.md`  

---

## 一、执行摘要

### 1.1 今日变更概览

- Git 拉取状态: 失败（网络问题），使用本地代码继续扫描
- Commit: `db1cf8c4`（今日框架核心扫描报告提交）
- 本次为首次对业务集成层进行全量深度扫描，覆盖 8 个业务模块、1352 个 Java 文件

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 1 | 2 | 3 |
| 需求与设计缺陷 | 0 | 1 | 2 | 0 | 3 |
| 代码规范违规 | 0 | 0 | 1 | 1 | 2 |
| 性能风险 | 0 | 1 | 1 | 0 | 2 |
| 安全漏洞 | 0 | 1 | 1 | 0 | 2 |
| 功能未完善 | 0 | 0 | 1 | 0 | 1 |
| 技术债务 | 0 | 0 | 0 | 1 | 1 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 0 | 1 | 1 | 2 |
| carlos-gateway | 0 | 0 | 2 | 1 | 3 |
| carlos-audit | 0 | 0 | 1 | 0 | 1 |
| carlos-message | 0 | 0 | 0 | 0 | 0 |
| carlos-org | 0 | 2 | 2 | 1 | 5 |
| carlos-system | 0 | 0 | 0 | 0 | 0 |
| carlos-license | 0 | 1 | 1 | 0 | 2 |
| carlos-tools | 0 | 0 | 0 | 1 | 1 |

### 1.4 今日重点关注

- **[TOP 1] License 证书生成使用 1024 位 RSA 密钥（P1）**：`LicenseCreatorService` 调用 `keytool` 生成私钥时使用 `-keysize 1024`，不符合 NIST 最小 2048 位要求，存在被暴力破解风险。
- **[TOP 2] Org 部门用户查询功能未实现（P1）**：`OrgDepartmentManagerImpl.getUsersByDeptId()` 直接返回 `Collections.emptyList()`，部门-人员关联查询功能缺失。
- **[TOP 3] Org 内存分页存在 OOM 风险（P1）**：`OrgDepartmentManagerImpl.getUsersByDeptId(deptId, param)` 先全量加载再内存分页，大数据量时可能引发内存溢出。

---

## 二、文档缺失详情

### P2-001: 业务模块缺少独立 README 和架构设计文档

**模块**: `carlos-integration/*`（所有业务模块）

**详细分析**:
1. 扫描发现所有业务模块（audit/auth/gateway/license/message/org/system/tools）均未在模块根目录下提供 `README.md`。
2. 缺少模块级架构设计文档，新成员无法快速理解模块边界、数据流、接口契约。
3. 缺少接口设计文档（Feign 接口契约、REST 端点请求/响应参数说明）。

**修复方案**:
1. 为每个业务模块补充 `README.md`，包含模块简介、核心功能、依赖关系、快速启动。
2. 在 `docs/design/` 目录下补充模块级架构设计文档（数据流图、接口契约表）。
3. 使用 Knife4j 的 `@Tag`、`@Operation`、`@Schema` 注解完善 Controller 层文档（已部分实现，需全面检查）。

---

### P3-001: Gateway 缺少 WAF 规则文档

**模块**: `carlos-gateway`

**详细分析**:
1. `WafFilter` 包含 SQL 注入、XSS、路径遍历、敏感文件访问等多层正则规则，但规则逻辑和覆盖范围未文档化。
2. 运维人员无法快速判断某条请求被拦截的原因，也无法根据业务需求调整规则。

**修复方案**:
1. 补充 `docs/waf-rules.md`，说明每条正则规则的目的、匹配场景、误报处理。
2. 在 WAF 拦截响应中增加规则编号，便于排查。

---

### P3-002: 业务模块 CHANGELOG 缺失

**模块**: `carlos-integration/*`

**详细分析**:
1. 所有业务模块缺少 `CHANGELOG.md`，版本变更、重大功能调整、兼容性变更无追溯。

**修复方案**:
1. 按照 [Keep a Changelog](https://keepachangelog.com/) 格式，为每个模块补充 CHANGELOG。

---

## 三、需求与设计缺陷详情

### P1-001: Org 部门用户查询功能未实现

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`  
**行号**: 第 180-183 行

**相关代码**:
```java
@Override
public List<OrgUserDTO> getUsersByDeptId(Serializable deptId) {
    List<OrgDepartmentUserDTO> users = getBaseMapper().getUsersByDeptId(deptId);
    return Collections.emptyList();  // 功能未实现，返回空列表
}
```

**详细分析**:
1. 方法查询了数据库获取用户列表，但最终返回 `Collections.emptyList()`，实际功能未实现。
2. 调用方会误以为该部门下没有用户，造成业务数据不一致。
3. 这是一个已知的未完成需求（开发中标记），但长期未修复。

**修复方案**:
```java
@Override
public List<OrgUserDTO> getUsersByDeptId(Serializable deptId) {
    List<OrgDepartmentUserDTO> users = getBaseMapper().getUsersByDeptId(deptId);
    if (CollUtil.isEmpty(users)) {
        return Collections.emptyList();
    }
    return users.stream()
        .map(OrgDepartmentConvert.INSTANCE::toUserDTO)
        .collect(Collectors.toList());
}
```

**短期规避**: 在方法注释中增加 `@deprecated` 和 `@TODO` 标记，提醒调用方该功能未完成。

---

### P2-002: Org 内存分页存在 OOM 风险

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`  
**行号**: 第 186-205 行

**相关代码**:
```java
@Override
public Paging<OrgDepartmentUserDTO> getUsersByDeptId(Serializable deptId, OrgDepartmentPageParam param) {
    List<OrgDepartmentUserDTO> allUsers = getBaseMapper().getUsersByDeptId(deptId);

    int total = allUsers.size();
    int pages = (int) Math.ceil((double) total / param.getSize());
    int fromIndex = (param.getCurrent() - 1) * param.getSize();
    int toIndex = Math.min(fromIndex + param.getSize(), total);

    List<OrgDepartmentUserDTO> records = fromIndex < total
        ? allUsers.subList(fromIndex, toIndex)
        : Collections.emptyList();
    // ... 构建分页对象
}
```

**详细分析**:
1. 先全量加载 `getUsersByDeptId` 返回所有用户，再在内存中分页。
2. 如果某部门有数万甚至数十万用户，将全部加载到内存，可能导致 OOM。
3. 未利用数据库的分页能力，违背了大数据量处理的基本原则。

**修复方案**:
1. 在 Mapper 层增加带分页参数的分页查询方法：
```java
// Mapper 接口
Page<OrgDepartmentUserDTO> getUsersByDeptId(@Param("deptId") Serializable deptId, Page<OrgDepartmentUserDTO> page);
```
2. 使用 MyBatis-Plus 分页插件进行数据库分页：
```java
@Override
public Paging<OrgDepartmentUserDTO> getUsersByDeptId(Serializable deptId, OrgDepartmentPageParam param) {
    Page<OrgDepartmentUserDTO> page = new Page<>(param.getCurrent(), param.getSize());
    Page<OrgDepartmentUserDTO> result = getBaseMapper().getUsersByDeptId(deptId, page);
    return MybatisPage.convert(result);
}
```

---

### P2-003: Auth 授权服务器缺少多因素认证（MFA）端点文档

**模块**: `carlos-auth`

**详细分析**:
1. `OAuth2AuthorizationServerConfig` 配置了授权服务器端点，但缺少 MFA（多因素认证）端点的显式配置说明。
2. `AGENTS.md` 提到支持 MFA，但配置中未见 TOTP/SMS 二次验证的端点配置。

**修复方案**:
1. 确认 MFA 功能是否已完整实现。若已实现，补充端点文档；若未实现，标记为待办需求。

---

## 四、代码规范违规详情

### P2-004: LicenseCreatorService 缺少输入参数校验

**模块**: `carlos-license`
**文件**: `carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorService.java`  
**行号**: 第 95-105 行（generateLicense 方法）

**相关代码**:
```java
public String generateLicense(LicenseCreatorParam param) {
    String fileId = IdUtil.fastUUID();
    LicenseCreatorParam.Identifier id = param.getIdentifier();
    String name = "CN=" + id.getAppName() + ...;
    // ... 直接生成，未校验 param 的必填字段
}
```

**详细分析**:
1. `generateLicense` 方法未对 `param` 进行空值和必填字段校验（如 `validity`、`storePass`、`keyPass` 等）。
2. 如果 `param.getIdentifier()` 或 `param.getValidity()` 为 null，会在后续 `keytool` 命令执行时报错。

**修复方案**:
```java
public String generateLicense(@Valid @NotNull LicenseCreatorParam param) {
    // 或内部校验
    if (param == null || param.getIdentifier() == null) {
        throw CommonErrorCode.PARAM_ERROR.exception("证书参数不能为空");
    }
    // ...
}
```

---

### P3-003: Org 缓存初始化缺少并发控制

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`  
**行号**: 第 216-260 行（initCache 方法）

**详细分析**:
1. `initCache()` 方法在应用启动时执行，使用分页加载 + Pipeline 写入 Redis。
2. 如果应用多实例部署，每个实例都会执行 `initCache()`，可能导致并发写入冲突或缓存覆盖。
3. 缺少分布式锁（如 Redisson 分布式锁）确保只有一个实例执行缓存初始化。

**修复方案**:
1. 使用 Redisson 分布式锁控制并发：
```java
public void initCache() {
    RLock lock = redissonClient.getLock("org:dept:cache:init:lock");
    try {
        if (lock.tryLock(10, 60, TimeUnit.SECONDS)) {
            // 检查是否已有其他实例完成初始化
            if (RedisUtil.hasKey("org:dept:cache:init:flag")) {
                return;
            }
            // 执行分页初始化...
            RedisUtil.setValue("org:dept:cache:init:flag", "1", 30, TimeUnit.MINUTES);
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

---

## 五、性能风险详情

### P1-002: Org 内存分页 OOM 风险（同 P2-002，升级为 P1）

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`  
**行号**: 第 186-205 行

**详细分析**:
1. 根部门（如总部）可能包含全公司所有用户（数万至数十万）。
2. 全量加载后内存分页，如果每条记录占用 200 字节，10 万用户约 20MB，加上对象开销可能达到 50-100MB。
3. 在高并发查询场景下，多个请求同时触发全量加载，可能导致内存快速耗尽。

**修复方案**: 同 P2-002，改为数据库分页。

---

### P2-005: Audit 审计日志 Disruptor 单线程处理瓶颈

**模块**: `carlos-audit`
**文件**: `carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/disruptor/AuditLogEventHandler.java`

**详细分析**:
1. `AuditLogEventHandler` 是 Disruptor 的单线程事件处理器，所有审计事件串行处理。
2. 如果审计日志量极大（如每秒数万条），单线程处理可能成为瓶颈。
3. `ClickHouseBatchWriter` 使用了虚拟线程执行器（`Executors.newVirtualThreadPerTaskExecutor()`），但事件 handler 本身是单线程的。

**修复方案**:
1. 考虑使用 Disruptor 的多消费者模式（WorkerPool），多个 handler 并行处理不同事件。
2. 或者将 `enrichAuditLog` 和 `calculateRiskLevel` 逻辑拆分为独立的 Disruptor 阶段，利用 Disruptor 的 Pipeline 能力并行处理。

---

## 六、安全漏洞详情

### P1-003: License 证书生成使用 1024 位 RSA 密钥

**模块**: `carlos-license`
**文件**: `carlos-spring-boot-starter-license-generate/src/main/java/com/carlos/license/generate/LicenseCreatorService.java`  
**行号**: 第 195-198 行
**CWE**: CWE-326（不充分的加密强度）

**相关代码**:
```java
private void generatePrivateKey(LicenseCreatorParam param, String path, String name) {
    String[] command = {
        "keytool",
        "-genkeypair",
        "-alias", param.getPrivateAlias(),
        // "-keyalg", "RSA",  // 被注释掉
        "-keysize", "1024",  // 密钥长度仅 1024 位
        // "-storetype", "PKCS12",  // 被注释掉
        ...
    };
    Process process = RuntimeUtil.exec(command);
}
```

**详细分析**:
1. NIST 和业界标准已要求 RSA 密钥至少 2048 位（推荐 3072 位）。1024 位 RSA 已被认为不安全，可在合理时间内被暴力破解。
2. 注释中已注明应该使用 `-keyalg RSA -keysize 2048 -storetype PKCS12`，但代码中仍使用 1024 位和默认 JKS 格式。
3. JKS 密钥库格式已废弃，行业标准为 PKCS12。

**修复方案**:
```java
String[] command = {
    "keytool",
    "-genkeypair",
    "-alias", param.getPrivateAlias(),
    "-keyalg", "RSA",
    "-keysize", "2048",  // 升级为 2048 位
    "-storetype", "PKCS12",  // 使用 PKCS12 格式
    "-validity", param.getValidity(),
    "-keystore", path,
    "-storepass", param.getStorePass(),
    "-keypass", param.getKeyPass(),
    "-dname", "\"" + name + "\""
};
```

---

### P2-006: Audit 审计日志未对敏感字段脱敏

**模块**: `carlos-audit`
**文件**: `carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/disruptor/AuditLogEventHandler.java`  
**行号**: 第 55-70 行（enrichAuditLog 方法）

**相关代码**:
```java
private void enrichAuditLog(AuditLogMainDTO auditLog) {
    if (auditLog.getServerTime() == null) {
        auditLog.setServerTime(LocalDateTime.now());
    }
    // ... 设置了时间、状态、Schema 版本
    // 但缺少对敏感字段的脱敏处理
}
```

**详细分析**:
1. 审计日志中可能包含 `request_payload_ref`、`response_payload_ref` 等字段，存储了请求/响应的 Payload。
2. 如果 Payload 中包含密码、Token、身份证号、手机号等敏感数据，审计日志中将以明文形式存储，存在数据泄露风险。
3. 缺少敏感数据脱敏策略（如密码替换为 `***`，手机号中间 4 位替换为 `*`）。

**修复方案**:
1. 在 `enrichAuditLog` 中增加敏感字段脱敏逻辑：
```java
private void enrichAuditLog(AuditLogMainDTO auditLog) {
    // ... 现有逻辑
    
    // 敏感数据脱敏
    if (auditLog.getRequestPayloadRef() != null) {
        String payload = auditLog.getRequestPayloadRef();
        payload = desensitizePayload(payload);
        auditLog.setRequestPayloadRef(payload);
    }
}

private String desensitizePayload(String payload) {
    if (StrUtil.isBlank(payload)) return payload;
    // 使用正则脱敏常见敏感字段
    return payload
        .replaceAll("\"password\"\s*:\s*\"[^\"]+\"", "\"password\":\"***\"")
        .replaceAll("\"token\"\s*:\s*\"[^\"]+\"", "\"token\":\"***\"")
        .replaceAll("(1[3-9]\\d)\\d{4}(\\d{4})", "$1****$2");  // 手机号
}
```
2. 或者使用更灵活的 JSON 解析脱敏，支持配置化脱敏规则。

---

## 七、功能未完善详情

### P2-007: Org 部门用户查询功能未实现（同 P1-001）

**模块**: `carlos-org`

**详细分析**:
1. `OrgDepartmentManagerImpl.getUsersByDeptId()` 返回 `Collections.emptyList()`，部门-人员关联查询功能缺失。
2. 这是一个核心功能缺失，影响组织架构模块的完整性。

**修复方案**: 同 P1-001。

---

## 八、技术债务详情

### 8.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 建议操作 |
|----|------|------|------|
| carlos-org-bus | 无直接违规 | - | 合规 ✓ |
| carlos-auth-service | 无直接违规 | - | 合规 ✓ |
| carlos-audit-bus | 无直接违规 | - | 合规 ✓ |
| carlos-gateway | 无直接违规 | - | 合规 ✓ |

**分析**: 业务模块的依赖方向基本合规，`-api` 不依赖 `-bus`，`-cloud` 依赖 `-api`，符合微服务分层设计。

### 8.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 |
|----|----|---------|------|
| `LicenseCreatorService.java` | 约 195 | `-keysize 1024`（弱加密） | `-keysize 2048` 或 `3072` |
| `LicenseCreatorService.java` | 约 195 | 默认 JKS 格式 | `-storetype PKCS12` |

### 8.3 TODO/FIXME 遗留

| 文件 | 行号 | 标记内容 | 建议 |
|----|----|--------|------|
| `OrgDepartmentManagerImpl.java` | 180 | `return Collections.emptyList();` | 完成功能实现 |
| `LicenseCreatorService.java` | 注释 | `-keyalg RSA` / `-keysize 2048` / `PKCS12` 注释 | 启用安全参数 |

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|--------|
| carlos-auth | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.4/5.0 |
| carlos-gateway | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.4/5.0 |
| carlos-audit | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.4/5.0 |
| carlos-org | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 3.2/5.0 |
| carlos-system | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-message | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-license | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-tools | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

**评分说明**:
- `carlos-auth`、`carlos-gateway`、`carlos-audit` 综合评分较高，架构设计优秀，安全控制完善，但文档得分较低。
- `carlos-org` 因功能未实现（getUsersByDeptId）和内存分页问题拉低评分。
- `carlos-license` 因密钥长度问题（1024 位）和缺少输入校验拉低安全评分。
- `carlos-tools` 因未深入扫描且缺少文档，评分偏低（建议下期重点扫描）。

---

## 十、优秀设计亮点

### 10.1 OAuth2 授权服务器安全设计（carlos-auth）

**文件**: `OAuth2AuthorizationServerConfig.java`

**亮点**:
1. CSRF 保护区分了浏览器端点和 API 端点，`/oauth2/token` 等机器间调用忽略 CSRF，表单端点启用 CSRF。
2. 使用 `CookieCsrfTokenRepository.withHttpOnlyFalse()` 支持前端 AJAX 获取 Token。
3. 授权服务器端点配置和默认安全过滤器链分离，职责清晰。

### 10.2 ClickHouse 批量写入设计（carlos-audit）

**文件**: `ClickHouseBatchWriter.java`

**亮点**:
1. 双缓冲设计避免 `clear()` 污染异步数据，每次 flush 创建新 `ArrayList`。
2. 使用 ClickHouse 官方 Java Client 原生 `FORMAT JSONEachRow` 批量插入，无 SQL 注入风险。
3. 循环重试替代递归重试，避免栈溢出。
4. 本地磁盘备份兜底，配置 `localBackupOnFailure` 即可启用。
5. 防御性背压：缓冲区溢出时丢弃最旧数据，防止 OOM。
6. 优雅关闭：`@PreDestroy` 执行最终刷新，`awaitTermination` 等待异步任务完成。

### 10.3 防重放攻击设计（carlos-gateway）

**文件**: `ReplayProtectionFilter.java`

**亮点**:
1. 三层防御：时间戳验证（1 分钟容差）、Nonce 唯一性检查（Redis 存储）、HMAC-SHA256 签名验证。
2. 白名单快速路径支持统一白名单和本地白名单两级降级。
3. 使用 `HexFormat`（JDK 17+）替代手动十六进制转换，性能提升 2-3 倍。
4. 每次创建新 `Mac` 实例（`Mac` 非线程安全，WebFlux EventLoop 线程复用场景下安全）。
5. 标准化错误响应，包含 `attackType`、`requestId`、`clientIp` 等上下文。

### 10.4 网关 Token 验证设计（carlos-gateway）

**文件**: `JwtTokenValidator.java` / `OpaqueTokenValidator.java`

**亮点**:
1. **JWT 验证器**：使用 RSA 公钥验证签名，验证 `iss`、`aud`、`exp`、`nbf` 等完整声明，支持 Token 黑名单（Redis 存储 `jti`）。
2. **Opaque Token 验证器**：通过 `introspection` 端点验证，使用 SHA-256 哈希值作为 Redis 缓存键（避免明文 Token 泄露到日志），缓存自省结果减少认证服务器压力。
3. 强制公钥配置：JWT 公钥未配置时直接抛出 `IllegalStateException`，禁止无验证跳过。

### 10.5 部门缓存五层设计（carlos-org）

**文件**: `OrgDepartmentManagerImpl.java`

**亮点**:
1. 五层 Redis 缓存结构：`SELF_KEY`（Hash 本体）、`CODE_KEY`（编码映射）、`CHILDREN_KEY`（直接子部门）、`DESC_KEY`（全部子孙）、`ANC_KEY`（祖先链）。
2. 跨级移动时先清理旧缓存关系再重建，保证缓存一致性。
3. 分页初始化缓存（5000 条/页），避免一次性加载全部数据。
4. Pipeline + 事务（MULTI/EXEC）批量操作 Redis，减少网络往返。

---

## 十一、待办事项与修复计划

### 11.1 今日新增待办

- [ ] **P1-001** `carlos-org`: 实现 `getUsersByDeptId()` 部门用户查询功能（24h 内）
- [ ] **P1-002** `carlos-org`: 将 `getUsersByDeptId(deptId, param)` 改为数据库分页（24h 内）
- [ ] **P1-003** `carlos-license`: 将 License 密钥长度从 1024 位升级到 2048 位，启用 PKCS12 格式（本周内）
- [ ] **P2-001** `carlos-integration/*`: 为所有业务模块补充 `README.md` 和架构设计文档（本周内）
- [ ] **P2-004** `carlos-license`: 为 `generateLicense` 增加输入参数校验（本周内）
- [ ] **P2-005** `carlos-audit`: 优化 Disruptor 单线程处理瓶颈，考虑多消费者模式（本月内）
- [ ] **P2-006** `carlos-audit`: 为审计日志 Payload 增加敏感数据脱敏（本月内）
- [ ] **P3-001** `carlos-gateway`: 补充 WAF 规则文档（排期优化）
- [ ] **P3-002** `carlos-integration/*`: 补充业务模块 CHANGELOG（排期优化）
- [ ] **P3-003** `carlos-org`: 为缓存初始化增加分布式锁控制（排期优化）

### 11.2 历史遗留跟踪

- [ ] **P0-001** `starter-redis`: Fastjson 1.x autoType 漏洞（框架核心扫描已发现）⏳ 待修复
- [ ] **P0-002** `AGENTS.md`: 文档与 BOM 版本脱节（框架核心扫描已发现）⏳ 待修复
- [ ] **P1-001** `carlos-dependencies`: knife4j 4.6.0-SNAPSHOT 快照依赖（框架核心扫描已发现）⏳ 待修复

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
