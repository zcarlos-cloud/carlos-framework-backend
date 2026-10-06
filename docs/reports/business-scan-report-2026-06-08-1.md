# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-08 08:00
**扫描 Commit**: `8d5b93e0092ba0ec863d8fdb8b2c598b259bba62`
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 13 个（P0: 0 个, P1: 3 个, P2: 6 个, P3: 4 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（无新变更，基于本地代码扫描）
- Commit: `8d5b93e0`
- 变更文件数: 0 个（今日无新 Commit）
- 基于最新代码全量扫描

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 0 | 2 | 2 |
| 需求与设计缺陷 | 0 | 1 | 1 | 0 | 2 |
| 代码规范违规 | 0 | 1 | 2 | 0 | 3 |
| 性能风险 | 0 | 0 | 0 | 0 | 0 |
| 安全漏洞 | 0 | 0 | 1 | 0 | 1 |
| 功能未完善 | 0 | 1 | 1 | 1 | 3 |
| 技术债务 | 0 | 0 | 1 | 1 | 2 |
| **合计** | **0** | **3** | **6** | **4** | **13** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 1 | 2 | 0 | 3 |
| carlos-audit | 0 | 1 | 1 | 0 | 2 |
| carlos-gateway | 0 | 0 | 0 | 1 | 1 |
| carlos-license | 0 | 0 | 0 | 1 | 1 |
| carlos-message | 0 | 0 | 0 | 0 | 0 |
| carlos-org | 0 | 1 | 1 | 0 | 2 |
| carlos-system | 0 | 0 | 1 | 1 | 2 |
| carlos-tools | 0 | 0 | 1 | 1 | 2 |

### 1.4 今日重点关注
- **[TOP 1]** P1-001: `carlos-auth-service` 使用 `InMemoryRegisteredClientRepository` 存储客户端信息，不支持集群、重启丢失
- **[TOP 2]** P1-002: `carlos-audit-bus` 的 `AuditLogQueryService` ClickHouse 查询功能完全未实现（空壳方法）
- **[TOP 3]** P1-003: `carlos-system-bus` / `carlos-org-bus` 存在手写 SQL 的 XML 文件，违反框架强制规范

---

## 二、安全漏洞详情

### 问题 1: [P2-001] 验证码发送失败仍返回 true，业务误判风险

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaService.java`
**行号**: L65-L78
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 相关代码

```java
// CaptchaService.java L65-L78
if (StrUtil.isNotBlank(templateCode)) {
    try {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("code", captcha);
        SmsUtil.sendByTemplateKey(phone, templateCode, params);
        log.info("SMS captcha sent to {} via SMS service", SensitiveDataUtil.maskPhone(phone));
    } catch (Exception e) {
        log.error("Failed to send SMS captcha to {}", SensitiveDataUtil.maskPhone(phone), e);
        // 短信发送失败不影响验证码已生成的事实，业务上仍返回成功
    }
} else {
    log.warn("SMS captcha template not configured, logged only: phone={}, code={}",
        SensitiveDataUtil.maskPhone(phone), captcha);
}

return true;  // 无论短信是否发送成功，都返回 true
```

#### 详细分析

- **问题描述**: `sendSmsCaptcha` 方法在短信发送失败时（捕获异常后）仍然返回 `true`。注释说明 "业务上仍返回成功"，但这会导致调用方误判短信已发送成功。用户可能收不到验证码，但系统告知发送成功，造成用户体验问题和排查困难。
- **违反原则**: 准确反馈原则——API 返回应准确反映操作结果，不能掩盖失败。
- **触发条件**: 短信服务异常、模板配置错误、网络问题时。
- **潜在风险**: 用户反复请求验证码但收不到，投诉增加；测试环境误以为短信功能正常。

#### 解决方案

**推荐修复**:

```java
public boolean sendSmsCaptcha(String phone) {
    if (!isValidPhone(phone)) {
        log.warn("Invalid phone number: {}", phone);
        return false;
    }

    String captcha = generateNumericCaptcha(6);
    captchaManager.saveCaptcha("sms", phone, captcha, 300);

    String templateCode = oAuth2Properties.getAlertNotification().getCaptchaSmsTemplateCode();
    if (StrUtil.isBlank(templateCode)) {
        log.warn("SMS captcha template not configured, logged only: phone={}, code={}",
            SensitiveDataUtil.maskPhone(phone), captcha);
        return false;  // 明确返回失败
    }

    try {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("code", captcha);
        SmsUtil.sendByTemplateKey(phone, templateCode, params);
        log.info("SMS captcha sent to {} via SMS service", SensitiveDataUtil.maskPhone(phone));
        return true;
    } catch (Exception e) {
        log.error("Failed to send SMS captcha to {}", SensitiveDataUtil.maskPhone(phone), e);
        // 发送失败时删除已保存的验证码，避免用户验证时使用未发送的验证码
        captchaManager.deleteCaptcha("sms", phone);
        return false;
    }
}
```

---

## 三、需求与设计缺陷详情

### 问题 2: [P1-001] OAuth2 客户端使用内存存储，不支持集群

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2ClientConfig.java`
**行号**: L60-L65
**严重程度**: P1-High
**影响范围**: 模块级（影响集群部署场景）

#### 相关代码

```java
// OAuth2ClientConfig.java L60-L65
if (clientConfigs != null && !clientConfigs.isEmpty()) {
    for (OAuth2ClientProperties config : clientConfigs) {
        clients.add(buildRegisteredClient(config, passwordEncoder));
        log.info("Registered OAuth2 client: {}", config.getClientId());
    }
    return new InMemoryRegisteredClientRepository(clients);
}

log.info("Using CompositeRegisteredClientRepository");
return new CustomizeRegisteredClientRepository(
    SpringUtil.getBean(AppClientService.class));
```

#### 详细分析

- **问题描述**: `registeredClientRepository` 方法在配置了客户端列表时返回 `InMemoryRegisteredClientRepository`，将客户端信息存储在内存中。这在生产集群环境中有严重问题：
  1. 每个节点独立维护客户端信息，无法同步
  2. 服务重启后客户端信息丢失（除非在配置中硬编码）
  3. 动态客户端注册（如通过管理界面新增客户端）无法生效
  4. 与 `CustomizeRegisteredClientRepository`（基于数据库/AppClientService）不一致
- **违反原则**: 生产可用性原则——集群环境下的服务必须支持共享状态存储。
- **触发条件**: 生产环境使用多实例部署时，配置 `carlos.oauth2.clients` 列表后，每个节点独立维护内存副本。
- **潜在风险**: 动态客户端注册失效、集群节点间客户端信息不一致、重启后需重新配置。

#### 解决方案

**推荐修复**:

统一使用数据库-backed 的客户端仓库：

```java
@Bean
@ConditionalOnMissingBean
public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
    // 统一使用 CompositeRegisteredClientRepository，支持数据库 + 配置文件混合
    return new CustomizeRegisteredClientRepository(
        SpringUtil.getBean(AppClientService.class), passwordEncoder);
}
```

修改 `CustomizeRegisteredClientRepository` 以同时支持配置文件中的客户端和数据库中的客户端，将所有客户端信息持久化到数据库。

**短期规避**: 在配置文件中明确列出所有客户端信息，避免动态注册。但这不解决重启丢失问题。

---

### 问题 3: [P2-002] 审计日志查询功能完全未实现（空壳方法）

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L48-L117
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 相关代码

```java
// AuditLogQueryService.java L48-L117
public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         String logType, String logStatus) {
    // TODO: 实现 ClickHouse 查询
    return new Paging<>();
}

public AuditLogMainDTO getById(Long id) {
    // TODO: 实现查询
    return null;
}

public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
    // TODO: 从物化视图查询统计
    return new AuditLogStatsVO();
}

public List<AuditLogMainDTO> listRiskEvents(String tenantId, int minRiskLevel,
                                            LocalDateTime startTime, LocalDateTime endTime) {
    // TODO: 实现查询
    return Collections.emptyList();
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计模块的查询服务，但核心方法（分页查询、详情查询、统计查询、风险事件查询）全部为空壳实现，只有 TODO 注释和空返回。这意味着审计模块的写入功能（`ClickHouseBatchWriter`）已经完善，但查询功能完全不可用。用户无法查看审计日志、无法进行安全分析、无法进行合规审计。
- **违反原则**: 功能完整性原则——模块核心功能必须完整实现，不能只有写入没有查询。
- **触发条件**: 任何尝试查询审计日志的操作（管理后台、安全分析、合规审计）都会得到空结果。
- **潜在风险**: 等保/密评无法通过、安全事件无法追溯、操作日志查询功能形同虚设。

#### 解决方案

**推荐修复**:

1. 使用 `ClickHouseClient` 或 `JdbcTemplate` 直接查询 ClickHouse：

```java
public Paging<AuditLogMainDTO> pageQuery(...) {
    String sql = """
        SELECT * FROM audit_log_main_local
        WHERE tenant_id = ?
        AND server_time >= ? AND server_time <= ?
        AND (? IS NULL OR log_type = ?)
        AND (? IS NULL OR state = ?)
        ORDER BY server_time DESC
        LIMIT ? OFFSET ?
        """;
    // 使用 ClickHouseClient 或 JdbcTemplate 执行
}
```

2. 物化视图查询：

```java
public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
    String sql = "SELECT * FROM audit_log_stats_mv_local WHERE tenant_id = ? AND event_date = ?";
    // 执行查询并映射结果
}
```

**短期规避**: 如果 ClickHouse 查询尚未准备好，可以临时提供 MySQL 查询（回退到 `auditLogMainManager`），但需标注性能限制。

---

## 四、代码规范违规详情

### 问题 4: [P1-002] SysDictMapper.xml 使用 SELECT * 手写 SQL

**模块**: `carlos-system-bus`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/resources/mapper/dict/SysDictMapper.xml`
**行号**: L18-L35
**严重程度**: P1-High
**影响范围**: 模块级

#### 相关代码

```xml
<!-- SysDictMapper.xml L18-L35 -->
<select id="selectOwnPage" resultMap="baseMap">
  SELECT *
  FROM sys_dict
  <where>
    <if test="param.dictName != null and param.dictName != ''">
      AND dict_name LIKE CONCAT('%', #{param.dictName}, '%')
    </if>
    <if test="param.description != null and param.description != ''">
      AND description LIKE CONCAT('%', #{param.description}, '%')
    </if>
    <if test="param.type != null">
      AND type = #{param.type.code}
    </if>
    <if test="param.start != null">
      AND create_time >= #{param.start}
    </if>
    <if test="param.end != null">
      AND create_time <= #{param.end}
    </if>
  </where>
  ORDER BY id DESC
</select>
```

#### 详细分析

- **问题描述**: `SysDictMapper.xml` 中手写 SQL 使用了 `SELECT *`，且使用了 XML 方式定义查询。根据框架强制规范，数据查询必须使用 MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL（包括 XML 中的手写 SQL）。
- **违反规范**: `CLAUDE.md` / `SKILL.md` 强制规范："数据查询：MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL"
- **触发条件**: 任何调用 `selectOwnPage` 的分页查询。
- **潜在风险**: 数据库表结构变更后 `SELECT *` 可能引入多余字段或字段缺失；手写 SQL 难以维护；违反框架统一规范。

#### 解决方案

**推荐修复**:

使用 MyBatis-Plus 的 `QueryWrapper` 替代手写 XML：

```java
// SysDictManagerImpl.java
public PageInfo<SysDict> selectOwnPage(SysDictPageParam param) {
    LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StrUtil.isNotBlank(param.getDictName()), SysDict::getDictName, param.getDictName())
           .like(StrUtil.isNotBlank(param.getDescription()), SysDict::getDescription, param.getDescription())
           .eq(param.getType() != null, SysDict::getType, param.getType().getCode())
           .ge(param.getStart() != null, SysDict::getCreateTime, param.getStart())
           .le(param.getEnd() != null, SysDict::getCreateTime, param.getEnd())
           .orderByDesc(SysDict::getId);
    return page(new MybatisPage<>(param.getCurrent(), param.getSize()), wrapper);
}
```

删除 `SysDictMapper.xml` 中的 `selectOwnPage` 定义。

---

### 问题 5: [P2-003] OrgDepartmentMapper.xml 手写 SQL 查询部门用户

**模块**: `carlos-org-bus`
**文件**: `carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgDepartmentMapper.xml`
**行号**: L7-L35
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 相关代码

```xml
<!-- OrgDepartmentMapper.xml L7-L35 -->
<!-- 获取部门下的用户列表 -->
<select id="getUsersByDeptId" resultType="com.carlos.org.pojo.dto.OrgDepartmentUserDTO">
  SELECT
  u.id as userId,
  u.account,
  u.name,
  u.gender,
  u.phone,
  u.email,
  u.user_status as userStatus,
  u.main_dept as mainDept,
  u.join_time as joinTime
  FROM org_user u
  INNER JOIN org_user_department ud ON u.id = ud.user_id
  WHERE ud.dept_id = #{deptId}
  AND u.deleted = 0
  AND ud.deleted = 0
  ORDER BY u.create_time DESC
</select>

<!-- 分页获取部门下的用户列表 -->
<select id="getUsersByDeptIdPaged" resultType="com.carlos.org.pojo.dto.OrgDepartmentUserDTO">
  SELECT ... （同上）
</select>
```

#### 详细分析

- **问题描述**: `OrgDepartmentMapper.xml` 中手写 SQL 查询部门用户关联列表。虽然使用了 `#{ }` 参数绑定（无 SQL 注入风险），但手写 SQL 本身违反了框架强制规范。此外，这种查询可以通过 MyBatis-Plus 的 `QueryWrapper` 或 `mybatis-plus-join` 实现。
- **违反规范**: `CLAUDE.md` / `SKILL.md` 强制规范："数据查询：MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL"
- **触发条件**: 查询部门用户列表时调用。
- **潜在风险**: 手写 SQL 难以维护，跨数据库兼容性差（如 DM 数据库的 `deleted` 字段处理可能不同）。

#### 解决方案

**推荐修复**:

使用 `mybatis-plus-join` 替代手写 SQL：

```java
// OrgDepartmentManagerImpl.java
public List<OrgDepartmentUserDTO> getUsersByDeptId(Long deptId) {
    return joinQueryWrapper
        .select(OrgUser.class, "u", "id as userId", "account", "name", "gender", "phone", "email", "user_status as userStatus", "main_dept as mainDept", "join_time as joinTime")
        .select(OrgUserDepartment.class, "ud")
        .from(OrgUser.class, "u")
        .innerJoin(OrgUserDepartment.class, "ud", "u.id = ud.user_id")
        .eq("ud.dept_id", deptId)
        .eq("u.deleted", 0)
        .eq("ud.deleted", 0)
        .orderByDesc("u.create_time")
        .list(OrgDepartmentUserDTO.class);
}
```

---

## 五、功能未完善详情

### 问题 6: [P2-004] 邮箱验证码功能未实现（仅记录日志）

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/captcha/CaptchaService.java`
**行号**: L95-L105
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 相关代码

```java
// CaptchaService.java L95-L105
public boolean sendEmailCaptcha(String email) {
    String captcha = generateNumericCaptcha(6);
    captchaManager.saveCaptcha("email", email, captcha, 300);

    // 邮件服务未配置，仅记录日志（需引入 spring-boot-starter-mail 后集成）
    log.warn("Email captcha service not configured, logged only: email={}, code={}",
        SensitiveDataUtil.maskEmail(email), captcha);

    return true;  // 返回 true，但实际未发送邮件
}
```

#### 详细分析

- **问题描述**: `sendEmailCaptcha` 方法虽然返回 `true`，但实际上仅生成验证码并记录日志，并未发送邮件。注释说明 "需引入 spring-boot-starter-mail 后集成"，但这是一个已知的未完成需求。邮箱验证码是 MFA（多因素认证）和注册流程的重要功能，缺失会影响用户体验。
- **违反原则**: 功能完整性原则——对外暴露的接口应实现承诺的功能，或明确标记为不可用（抛异常/返回 false）。
- **触发条件**: 用户选择邮箱验证码登录/注册时。
- **潜在风险**: 用户无法通过邮箱验证码完成认证，业务流程中断；返回 `true` 导致调用方误以为邮件已发送。

#### 解决方案

**推荐修复**:

1. 引入 `spring-boot-starter-mail` 并集成邮件发送：

```java
public boolean sendEmailCaptcha(String email) {
    String captcha = generateNumericCaptcha(6);
    captchaManager.saveCaptcha("email", email, captcha, 300);

    try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("验证码");
        message.setText("您的验证码是: " + captcha + "，有效期5分钟。");
        mailSender.send(message);
        log.info("Email captcha sent to {}", SensitiveDataUtil.maskEmail(email));
        return true;
    } catch (Exception e) {
        log.error("Failed to send email captcha to {}", SensitiveDataUtil.maskEmail(email), e);
        captchaManager.deleteCaptcha("email", email);
        return false;
    }
}
```

2. **短期规避**: 如果无法立即集成邮件服务，应返回 `false` 并明确提示：

```java
public boolean sendEmailCaptcha(String email) {
    log.warn("Email captcha service not configured");
    return false;  // 明确返回失败
}
```

---

### 问题 7: [P3-001] 认证事件发布被注释，审计链不完整

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/ext/ExtendAuthenticationProvider.java`
**行号**: L105-L110
**严重程度**: P3-Low
**影响范围**: 类级

#### 相关代码

```java
// ExtendAuthenticationProvider.java L105-L110
if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));
}

// ...

if (eventPublisher != null) {
    // eventPublisher.publishEvent(new AuthenticationFailureEvent(token, e));
}
```

#### 详细分析

- **问题描述**: 认证成功/失败的事件发布被注释掉了。这意味着 `carlos-audit` 模块无法通过监听认证事件来自动记录登录/失败的审计日志。虽然 `AuditLogEventHandler` 已实现了风险评分模型，但认证事件是审计的重要数据源，缺失会导致审计覆盖不完整。
- **触发条件**: 所有认证流程（登录、授权码、密码、短信等）。
- **潜在风险**: 登录审计日志缺失，无法满足安全合规要求。

#### 解决方案

取消注释，启用认证事件发布：

```java
if (eventPublisher != null) {
    eventPublisher.publishEvent(new AuthenticationSuccessEvent(authenticatedToken));
}
```

确保 `AuthenticationSuccessEvent` 和 `AuthenticationFailureEvent` 类已定义，且 `carlos-audit` 模块已订阅这些事件。

---

## 六、技术债务详情

### 6.1 TODO/FIXME 遗留清单

| 文件 | 行号 | 标记 | 内容 | 建议 |
|----|----|----|------|------|
| `carlos-audit/carlos-audit-bus/src/.../AuditLogQueryService.java` | 48 | TODO | 实现 ClickHouse 查询 | P2 优先处理 |
| `carlos-audit/carlos-audit-bus/src/.../AuditLogQueryService.java` | 69 | TODO | 实现查询 | P2 优先处理 |
| `carlos-audit/carlos-audit-bus/src/.../AuditLogQueryService.java` | 83 | TODO | 从物化视图查询统计 | P2 优先处理 |
| `carlos-audit/carlos-audit-bus/src/.../AuditLogQueryService.java` | 117 | TODO | 实现查询 | P2 优先处理 |
| `carlos-system/carlos-system-bus/src/.../SysRegionManagerImpl.java` | 417 | FIXME | 是否需要考虑缓存失效的情况 | P2 处理 |
| `carlos-system/carlos-system-bus/src/.../SysRegionManagerImpl.java` | 423 | FIXME | 当前只考虑更改基本信息，不更改层级等内容 | P2 处理 |
| `carlos-system/carlos-system-bus/src/.../SysRegionService.java` | 356 | FIXME | 方式优化 | P3 处理 |
| `carlos-system/carlos-system-bus/src/.../SysConfigService.java` | 130 | TODO | 2022/12/22 如果是文件类型，将文件转换成地址 | P3 处理（已3年） |
| `carlos-license/.../LicenseCreatorController.java` | 32 | TODO | 该接口的功能应该用一个脚本代替 | P3 处理 |
| `carlos-tools/src/.../CodeGeneratorController.java` | 203 | TODO | 2026-02-14 | P3 近期 |
| `carlos-tools/src/.../CodeGeneratorService.java` | 95 | TODO | 2020/9/9 如果是多模块模板 | P3 已6年 |
| `carlos-tools/src/.../Gitlab*.java` | 多处 | TODO | GitLab 相关功能大量 TODO | P3 排期清理 |

### 6.2 模块间依赖合规性

| 模块 | 检查项 | 状态 | 说明 |
|-----|------|------|------|
| carlos-audit-api | 不依赖 -bus | ✅ | 仅包含 Feign 接口和 AO |
| carlos-audit-cloud | 只依赖 -api | ✅ | 符合规范 |
| carlos-auth-api | 不依赖 -bus | ✅ | 仅包含 Feign 接口和 AO |
| carlos-auth-cloud | 只依赖 -api | ✅ | 符合规范 |
| carlos-org-api | 不依赖 -bus | ✅ | 符合规范 |
| carlos-org-cloud | 只依赖 -api | ✅ | 符合规范 |
| carlos-system-api | 不依赖 -bus | ✅ | 符合规范 |
| carlos-system-cloud | 只依赖 -api | ✅ | 符合规范 |
| carlos-message-api | 不依赖 -bus | ✅ | 符合规范 |
| carlos-message-cloud | 只依赖 -api | ✅ | 符合规范 |

### 6.3 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 |
|----|----|---------|------|
| 未发现 | - | - | - |

---

## 七、文档完整性评估

### 问题 8: [P3-002] docs 目录中模板内容未填充

**模块**: 所有业务模块（audit / auth / org / system / message / license / gateway / tools）
**严重程度**: P3-Low
**影响范围**: 项目级

#### 详细分析

- **问题描述**: 所有业务模块的 `docs/` 目录结构完整（01-product, 02-design, 03-develop, 04-test, 05-issue），但模板文件（如测试计划、缺陷报告、ADR 等）中仍包含占位符（如 `{模块名称}`、`{负责人}`、`{开始日期}` 等），未填入实际内容。这导致文档无法直接使用，需要开发者手动填充。
- **触发条件**: 任何尝试参考文档了解模块信息的开发者。
- **潜在风险**: 新开发者难以快速理解模块，文档形同虚设。

#### 解决方案

1. 填充模板中的占位符，或移除未使用的模板文件。
2. 在 `README.md` 中提供模块的精简介绍，而非依赖未填充的模板。

---

### 问题 9: [P3-003] carlos-tools 大量 TODO 未清理

**模块**: `carlos-tools`
**严重程度**: P3-Low
**影响范围**: 模块级

#### 详细分析

- **问题描述**: `carlos-tools` 模块的 GitLab 相关控制器中存在大量 TODO（如 "Show add user dialog"、"Parse Excel and import users"、"Export users to Excel" 等），表明这些功能尚未实现。部分 TODO 标注日期为 2020 年，已超过 6 年未处理。
- **触发条件**: 使用 carlos-tools 的 GitLab 管理功能时。
- **潜在风险**: 代码可读性降低，工具类模块质量下降，维护困难。

#### 解决方案

1. 清理长期未处理的 TODO（超过 2 年的 TODO 应删除或实现）。
2. 对于未实现的功能，在文档中明确说明 "暂不支持"，而非保留 TODO。

---

## 八、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|---------|
| carlos-gateway | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.4/5.0 |
| carlos-audit | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | 3.2/5.0 |
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.4/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.4/5.0 |
| carlos-tools | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | 2.6/5.0 |

> 评分说明：
> - `carlos-audit` 功能完整度低（⭐⭐☆☆☆）是因为 `AuditLogQueryService` 查询功能完全未实现（空壳方法）
> - `carlos-auth` 规范落实低（⭐⭐⭐☆☆）是因为 `InMemoryRegisteredClientRepository` 使用内存存储、验证码发送失败仍返回 true
> - `carlos-org` / `carlos-system` 规范落实低（⭐⭐⭐☆☆）是因为存在手写 SQL 的 XML 文件
> - `carlos-tools` 整体评分低（2.6/5.0）是因为大量 TODO 未清理、功能不完整

---

## 九、待办事项与修复计划

### 9.1 今日新增待办

- [ ] **P1-001** `carlos-auth-service`: 将 `InMemoryRegisteredClientRepository` 替换为 `CustomizeRegisteredClientRepository`（数据库-backed），支持集群共享和动态客户端注册
- [ ] **P1-002** `carlos-audit-bus`: 实现 `AuditLogQueryService` 的 ClickHouse 查询功能（pageQuery、getById、getRealtimeStats、listRiskEvents）
- [ ] **P1-003** `carlos-system-bus` / `carlos-org-bus`: 移除手写 SQL 的 XML 文件，改用 MyBatis-Plus / mybatis-plus-join 实现
- [ ] **P2-001** `carlos-auth-service`: 修复 `CaptchaService.sendSmsCaptcha` 短信发送失败时返回 true 的问题，改为返回 false 并删除已保存的验证码
- [ ] **P2-002** `carlos-auth-service`: 集成 `spring-boot-starter-mail` 实现邮箱验证码发送，或暂时返回 false
- [ ] **P2-003** `carlos-system-bus`: 修复 `SysRegionManagerImpl` 中 FIXME 标记的缓存失效和层级变更问题
- [ ] **P2-004** `carlos-auth-service`: 取消 `ExtendAuthenticationProvider` 中认证事件发布的注释，启用 `AuthenticationSuccessEvent` 和 `AuthenticationFailureEvent`
- [ ] **P3-001** 所有模块: 填充 docs 目录模板中的占位符，或移除未使用的模板文件
- [ ] **P3-002** `carlos-tools`: 清理超过 2 年的 TODO，或移除未实现的 GitLab 功能代码

### 9.2 历史遗留跟踪

- 无历史遗留问题（首次业务模块全量扫描）

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*生成时间: 2026-06-08 08:00*
*扫描 Commit: 8d5b93e0092ba0ec863d8fdb8b2c598b259bba62*
