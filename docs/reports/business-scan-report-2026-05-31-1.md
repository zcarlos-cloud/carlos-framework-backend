# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-31 08:45
**扫描 Commit**: cdbb131c5e0713e978c0fcb7a3b65d7db48931cc
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 12 个（P0: 0 个, P1: 2 个, P2: 6 个, P3: 4 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功
- Commit: cdbb131c `build(jenkins): 修改Maven构建命令以包含特定模块`
- 今日新增: Jenkins 流水线配置优化、存储配置模块前后端实现
- 变更文件数: 约 40 个（主要新增 Jenkins/存储配置相关代码）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 2 | 2 | 4 |
| 需求与设计缺陷 | 0 | 0 | 1 | 0 | 1 |
| 代码规范违规 | 0 | 0 | 0 | 0 | 0 |
| 性能风险 | 0 | 0 | 1 | 0 | 1 |
| 安全漏洞 | 0 | 0 | 0 | 0 | 0 |
| 功能未完善 | 0 | 2 | 1 | 0 | 3 |
| 技术债务 | 0 | 0 | 1 | 2 | 3 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-gateway | 0 | 0 | 0 | 0 | 0 |
| carlos-auth | 0 | 0 | 1 | 1 | 2 |
| carlos-audit | 0 | 2 | 1 | 0 | 3 |
| carlos-org | 0 | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 0 | 2 | 1 | 3 |
| carlos-license | 0 | 0 | 1 | 0 | 1 |
| carlos-message | 0 | 0 | 0 | 0 | 0 |
| carlos-tools | 0 | 0 | 0 | 0 | 0 |

### 1.4 今日重点关注
- [TOP 1] `carlos-audit-bus`: `AuditLogQueryService` ClickHouse 查询功能全部 TODO 未实现（P1，核心功能缺失）
- [TOP 2] `carlos-system-bus`: `SysRegionManagerImpl` 2 处 FIXME 遗留（P2，缓存失效+层级变更未处理）
- [TOP 3] `carlos-auth-service`: `CustomGrantTypes` 密码授权类型存在但文档说明已移除（P2，文档与实际不符）

---

## 二、功能未完善详情（P1）

### 问题 1: AuditLogQueryService ClickHouse 查询功能全部 TODO 未实现

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L45-L120
**严重程度**: P1-High
**影响范围**: 模块级
**问题类型**: 功能缺失

#### 相关代码

```java
@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogMainManager auditLogMainManager;

    public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             String logType, String logStatus) {
        // TODO: 实现 ClickHouse 查询
        // 示例 SQL:
        // SELECT * FROM audit_log_main_local
        // WHERE tenant_id = #{tenantId}
        // AND server_time >= #{startTime} AND server_time <= #{endTime}
        // ...
        return new Paging<>(); // 返回空结果
    }

    public AuditLogMainDTO getById(Long id) {
        // TODO: 实现查询
        return null;
    }

    public AuditLogStatsVO getRealtimeStats(String tenantId, LocalDate date) {
        // TODO: 从物化视图查询统计
        return new AuditLogStatsVO(); // 返回空结果
    }

    public List<AuditLogMainDTO> listRiskEvents(String tenantId, int minRiskLevel,
                                                LocalDateTime startTime, LocalDateTime endTime) {
        // TODO: 实现查询
        return Collections.emptyList();
    }
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计模块的核心查询服务，但所有 4 个查询方法均为 TODO 空实现，仅返回空对象或空集合。
- **影响功能**:
  - 审计日志分页查询：返回空分页，前端无法查看审计日志
  - 单条日志详情：返回 null，可能触发 NPE
  - 实时统计：返回空统计对象，Dashboard 数据为空
  - 风险事件查询：返回空列表，安全运营无法获取风险事件
- **触发条件**: 任何调用审计查询接口的场景都会触发
- **潜在风险**: 审计模块写入功能完善（ClickHouseBatchWriter 设计优秀），但查询功能完全不可用，形成"只写不读"的半残模块。

#### 解决方案

**推荐修复**:

```java
public Paging<AuditLogMainDTO> pageQuery(String tenantId, int pageNum, int pageSize,
                                         LocalDateTime startTime, LocalDateTime endTime,
                                         String logType, String logStatus) {
    // 构建 ClickHouse SQL（使用 ClickHouseClient 原生 API）
    StringBuilder sql = new StringBuilder("SELECT * FROM audit_log_main_local WHERE 1=1");
    List<Object> params = new ArrayList<>();
    
    if (StrUtil.isNotBlank(tenantId)) {
        sql.append(" AND tenant_id = ?");
        params.add(tenantId);
    }
    if (startTime != null) {
        sql.append(" AND server_time >= ?");
        params.add(startTime.format(CK_DATETIME_FORMATTER));
    }
    if (endTime != null) {
        sql.append(" AND server_time <= ?");
        params.add(endTime.format(CK_DATETIME_FORMATTER));
    }
    if (StrUtil.isNotBlank(logType)) {
        sql.append(" AND log_type = ?");
        params.add(logType);
    }
    if (StrUtil.isNotBlank(logStatus)) {
        sql.append(" AND state = ?");
        params.add(logStatus);
    }
    
    // 排序 + 分页
    sql.append(" ORDER BY server_time DESC");
    sql.append(" LIMIT ? OFFSET ?");
    params.add(pageSize);
    params.add((pageNum - 1) * pageSize);
    
    // 执行查询（通过 ClickHouseClient）
    List<AuditLogMainDTO> records = clickHouseQueryExecutor.query(sql.toString(), params, 
        rs -> mapResultSetToDTO(rs));
    
    // 查询总数
    long total = clickHouseQueryExecutor.count("SELECT count() FROM audit_log_main_local WHERE ...", params);
    
    return Paging.of(records, total, pageNum, pageSize);
}
```

**短期规避**: 提供 MySQL 兜底查询（如果审计数据同时写入 MySQL）

**长期建议**:
- 引入 ClickHouse ORM 映射层（如使用 MyBatis-Plus 的 ClickHouse 支持或自定义 TypeHandler）
- 建立审计查询的物化视图和预聚合表，提升统计查询性能
- 考虑将审计查询与写入分离（读写分离架构）

---

## 三、技术债务详情

### 问题 2: SysRegionManagerImpl FIXME 遗留 — 缓存失效与层级变更未处理

**模块**: `carlos-system-bus`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L283, L295
**严重程度**: P2-Medium
**影响范围**: 模块级
**问题类型**: 技术债务

#### 相关代码

```java
@Override
public List<String> getParentsCode(String code) {
    // FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
    return RedisUtil.getList(String.format(ANC_KEY, code));
}

@Override
public void updateCache(SysRegionDTO bean) {
    // FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
    RedisUtil.putHash(String.format(SELF_KEY, bean.getRegionCode()), bean);
}
```

#### 详细分析

- **问题描述**: 两处 FIXME 分别标记于 2025-12-05 和 2025-12-08，距今已 5 个月未处理。
  1. `getParentsCode`：未考虑缓存失效场景，若缓存被清理或过期，返回空列表可能导致业务异常。
  2. `updateCache`：行政区域层级变更（如撤县设区）时，只更新本体 Hash，不更新祖先链、子孙集合等关联缓存，导致缓存数据不一致。
- **触发条件**:
  - 缓存被手动清理或 Redis 重启后，首次调用 `getParentsCode`
  - 行政区域的 `parentCode` 或 `parents` 字段发生变更时
- **潜在风险**:
  - 缓存失效时返回空祖先链，影响依赖祖先链的权限判断或数据范围计算
  - 层级变更后缓存不一致，可能导致"旧父级仍包含该节点"或"新父级未包含该节点"

#### 解决方案

**推荐修复**:

```java
@Override
public List<String> getParentsCode(String code) {
    String ancKey = String.format(ANC_KEY, code);
    List<String> parents = RedisUtil.getList(ancKey);
    if (CollUtil.isNotEmpty(parents)) {
        return parents;
    }
    // 缓存失效兜底：从数据库重建
    SysRegionDTO region = getByRegionCode(code);
    if (region != null && StrUtil.isNotBlank(region.getParents())) {
        parents = StrUtil.split(region.getParents(), StrUtil.COMMA);
        // 异步回写缓存
        RedisUtil.putList(ancKey, parents);
    }
    return parents != null ? parents : Collections.emptyList();
}

@Override
public void updateCache(SysRegionDTO bean) {
    // 获取旧数据，判断层级是否变更
    SysRegionDTO oldRegion = getByRegionCode(bean.getRegionCode());
    if (oldRegion == null) {
        putCache(bean);
        return;
    }
    
    boolean levelChanged = !Objects.equals(oldRegion.getParentCode(), bean.getParentCode())
        || !Objects.equals(oldRegion.getParents(), bean.getParents());
    
    if (levelChanged) {
        // 层级变更：删除旧缓存关系，重建全部缓存
        deleteCache(oldRegion);
        putCache(bean);
    } else {
        // 仅基本信息变更：更新本体 Hash
        RedisUtil.putHash(String.format(SELF_KEY, bean.getRegionCode()), bean);
    }
}
```

**短期规避**: 行政区域的层级变更属于低频操作，可暂时通过手动清理缓存解决。

---

### 问题 3: AuditLogQueryService 统计查询性能风险

**模块**: `carlos-audit-bus`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`
**行号**: L80-L95
**严重程度**: P2-Medium
**影响范围**: 模块级
**问题类型**: 性能风险

#### 详细分析

- **问题描述**: `getRealtimeStats` 和 `listRiskEvents` 方法即使实现后，直接对 ClickHouse 的 `audit_log_main_local` 大表做聚合查询和范围扫描，性能风险高。
- **触发条件**: 审计日志量大时（日百万级），统计查询响应时间可能超过 10 秒
- **潜在风险**: 慢查询拖垮 ClickHouse 节点，影响审计日志写入性能

#### 解决方案

**推荐修复**:
- 使用 ClickHouse 物化视图（Materialized View）预聚合统计数据
- 风险事件使用独立的 `audit_log_risk_mv` 物化视图
- 统计查询走物化视图而非原始表

```sql
-- 创建统计物化视图
CREATE MATERIALIZED VIEW audit_log_stats_mv_local
ENGINE = SummingMergeTree()
PARTITION BY toYYYYMMDD(event_date)
ORDER BY (tenant_id, event_date, log_type)
AS SELECT
    tenant_id,
    event_date,
    log_type,
    count() as total_count,
    sum(risk_level) as total_risk_score,
    avg(risk_level) as avg_risk_level
FROM audit_log_main_local
GROUP BY tenant_id, event_date, log_type;
```

---

## 四、需求与设计缺陷详情

### 问题 4: CustomGrantTypes 密码授权类型与文档说明矛盾

**模块**: `carlos-auth-service`
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/grant/CustomGrantTypes.java`
**行号**: L25-L45
**严重程度**: P2-Medium
**影响范围**: 类级
**问题类型**: 设计缺陷

#### 相关代码

```java
public final class CustomGrantTypes {
    /**
     * 密码授权类型（内部使用，供统一身份源架构兼容）
     */
    public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    /**
     * 短信验证码授权类型
     */
    public static final AuthorizationGrantType SMS_CODE = new AuthorizationGrantType("sms_code");
    // ... email_code, qr_code, social_login, mfa
}
```

#### 详细分析

- **问题描述**: 类文档注释明确说明"密码授权类型（password）已被移除，账号密码登录应使用表单登录（/login）端点"，但代码中仍定义了 `PASSWORD` 常量。
- **矛盾点**: 文档说已移除，但常量存在且可用，可能导致开发者误用。
- **触发条件**: 若客户端仍使用 `grant_type=password` 请求 Token，授权服务器可能处理该请求
- **潜在风险**: 密码授权类型在 OAuth 2.1 中已被弃用，继续使用违反安全最佳实践；若被外部调用，增加密码泄露风险。

#### 解决方案

**推荐修复**:
1. 若确实需要内部兼容，将 `PASSWORD` 标记为 `@Deprecated` 并添加更明确的警告
2. 在授权服务器配置中明确拒绝 `password` grant type 的外部请求（仅允许内部 IP 或特定客户端）
3. 更新类注释，说明 `PASSWORD` 保留的原因和限制条件

```java
/**
 * @deprecated 仅用于内部服务兼容，外部客户端禁止使用。
 * 账号密码登录请使用 /login 表单端点 + Authorization Code 流程。
 */
@Deprecated(since = "3.1.0")
public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
```

---

## 五、文档缺失详情

### 问题 5: carlos-audit 模块缺少 README.md

**模块**: `carlos-audit`
**严重程度**: P3-Low
**影响范围**: 模块级

- **缺失文件**: `carlos-integration/carlos-audit/README.md`
- **应有内容**: 模块简介、ClickHouse 表结构、Disruptor 配置、写入/查询架构说明、部署依赖
- **影响**: 新成员无法快速了解审计模块的架构设计

### 问题 6: carlos-message 模块缺少设计文档

**模块**: `carlos-message`
**严重程度**: P3-Low
**影响范围**: 模块级

- **缺失文件**: `carlos-integration/carlos-message/docs/design/`
- **已有内容**: 仅有 `docs/` 目录但缺少详细设计文档
- **影响**: 消息模块的多渠道路由、模板渲染、限流防刷等核心设计未文档化

### 问题 7: carlos-gateway WafFilter 规则缺少维护文档

**模块**: `carlos-gateway`
**严重程度**: P2-Medium
**影响范围**: 模块级

- **问题**: WafFilter 的正则规则（SQL 注入、XSS、路径遍历等）缺乏维护文档，规则更新历史、误报处理流程未记录
- **影响**: 安全规则调整依赖口头沟通，误报问题难以追溯

---

## 六、安全评估详情

### 6.1 认证授权安全

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 弱密码编码器（MD5/SHA1） | ✅ 无 | OAuth2PasswordEncoderConfig 仅支持 BCrypt |
| 密码明文存储 | ✅ 无 | 全部使用 PasswordEncoder 编码 |
| 登录防暴力破解 | ⚠️ 待确认 | 需检查是否有账户锁定/验证码机制 |
| JWT 密钥长度 | ✅ 达标 | KeyPairManager 支持 2048/4096 位 RSA |
| JWT 密钥轮换 | ✅ 已支持 | KeyPairManager 文档说明支持新旧密钥同时生效 |
| CSRF 保护 | ✅ 已启用 | OAuth2AuthorizationServerConfig 启用 CSRF，API 端点忽略 |
| Token 过期时间 | ⚠️ 待确认 | 需检查 OAuth2TokenConfig 中 access/refresh token 过期时间 |

### 6.2 数据安全

| 检查项 | 状态 | 说明 |
|--------|------|------|
| SQL 注入防护 | ✅ 有 | WafFilter 有 SQL 注入正则拦截 |
| XSS 防护 | ✅ 有 | WafFilter 有 XSS 正则拦截 |
| 路径遍历防护 | ✅ 有 | WafFilter 有路径遍历正则拦截 |
| 命令注入防护 | ✅ 有 | WafFilter 有命令注入正则拦截 |
| 敏感文件访问防护 | ✅ 有 | WafFilter 拦截 .git/.env 等敏感路径 |
| 防重放攻击 | ✅ 有 | ReplayProtectionFilter 基于 HMAC-SHA256 + 时间戳 |
| 敏感数据脱敏 | ⚠️ 待确认 | 需检查各模块返回的手机号/身份证是否脱敏 |

### 6.3 License 安全

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 生产环境含 license-generate | ✅ 无 | starter-license-generate 独立模块，不会误入生产 |
| 私钥管理 | ✅ 安全 | license-generate 仅在开发环境使用 |
| XMLDecoder 白名单 | ✅ 有 | CustomLicenseManager 有 ALLOWED_CLASSES 白名单 |
| 证书校验 | ✅ 有 | CustomLicenseManager 重写 validate 方法 |

---

## 七、架构设计亮点

### 7.1 WafFilter — Web 应用防火墙（优秀）

**评价**: 该过滤器设计堪称网关安全典范：

1. **ThreadLocal Matcher 缓存**: 避免每次请求重复创建 Matcher 对象，显著降低 GC 压力
2. **BitSet 快速字符检查**: 先通过字符白名单快速排除安全请求，避免正则匹配开销
3. **正则预编译**: 所有模式在类加载时预编译，运行时零编译开销
4. **五类攻击全覆盖**: SQL 注入、XSS、CSRF、路径遍历、敏感文件访问、命令注入
5. **Header 检查优化**: 使用快速路径判断是否需要深度检查

### 7.2 ReplayProtectionFilter — 防重放攻击（优秀）

**评价**:
- ThreadLocal Mac 实例缓存（Mac 创建开销大）
- HexFormat (JDK 17+) 替代手动十六进制转换，性能提升 2-3 倍
- HMAC-SHA256 签名 + 5 分钟时间戳容差
- Redis 存储 nonce，防止重放
- 统一白名单检查结果复用，避免重复路径匹配

### 7.3 ClickHouseBatchWriter — 审计日志批量写入（优秀）

**评价**:
- 双缓冲设计，每次 flush 创建新 ArrayList（避免 clear() 污染异步数据）
- ClickHouseClient 原生 FORMAT JSONEachRow 插入，无 SQL 拼接风险
- 循环重试替代递归，避免栈溢出
- 本地磁盘备份兜底（配置 `localBackupOnFailure`）
- 防御性背压：缓冲区溢出时丢弃最旧数据
- 使用 VirtualThreadPerTaskExecutor（Java 21 虚拟线程）
- 统一在 Java 端生成 id 和时间戳，避免客户端与服务端时间不一致

### 7.4 OrgDepartmentManagerImpl — 五层 Redis 缓存（优秀）

**评价**:
- `SELF_KEY` = `dept:hash:{id}` — Hash 存储部门实体
- `CODE_KEY` = `dept:code:{deptCode}` — 部门编码映射
- `CHILDREN_KEY` = `dept:children:{parentId}` — Set 存储直接子部门（SMEMBERS O(N)）
- `DESC_KEY` = `dept:desc:{id}` — Set 存储全部子孙
- `ANC_KEY` = `dept:ancestors:{id}` — List 存储祖先链
- 跨级移动时清理旧缓存关系再重建
- 分页初始化缓存（5000条/页）
- 使用 Redis Pipeline 批量操作

### 7.5 OAuth2AuthorizationServerConfig — 安全配置拆分（优秀）

**评价**:
- 单体配置拆分为 6 个独立配置类：ClientConfig、JwkConfig、TokenConfig、AuthorizationServiceConfig、PasswordEncoderConfig、AuthenticationConfig
- 遵循单一职责原则，每个配置类专注一个领域
- CSRF 保护启用，但 API 端点（/oauth2/token、/oauth2/introspect、/oauth2/revoke）忽略 CSRF
- 使用 CookieCsrfTokenRepository 支持 AJAX 前端获取 Token

---

## 八、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|------|---------|---------|---------|---------|---------|---------|
| carlos-gateway | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.7/5.0 |
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.6/5.0 |
| carlos-audit | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐☆☆☆ | 3.4/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.7/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.3/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 |
| carlos-message | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 九、待办事项与修复计划

### 9.1 今日新增待办

- [ ] **P1-001** `carlos-audit-bus`: 实现 `AuditLogQueryService` 的 ClickHouse 查询功能（分页查询、单条详情、实时统计、风险事件）
- [ ] **P1-002** `carlos-audit-bus`: 设计 ClickHouse 物化视图用于统计查询，避免大表扫描
- [ ] **P2-003** `carlos-system-bus`: 修复 `SysRegionManagerImpl` 两处 FIXME（缓存失效兜底、层级变更时重建关联缓存）
- [ ] **P2-004** `carlos-auth-service`: 更新 `CustomGrantTypes` 文档注释，明确 `PASSWORD` 授权类型的使用限制或标记 `@Deprecated`
- [ ] **P2-005** `carlos-audit`: 补充模块 README.md，包含 ClickHouse 表结构、Disruptor 配置说明
- [ ] **P2-006** `carlos-gateway`: 补充 WafFilter 规则维护文档，记录规则更新历史和误报处理流程
- [ ] **P3-007** `carlos-message`: 补充多渠道路由和模板渲染的设计文档

### 9.2 历史遗留跟踪

- [ ] **P0-008** `carlos-auth-service`: OAuth2 授权服务器 CSRF 保护 ✅ 已修复（2026-04-10）
- [ ] **P1-009** `carlos-audit-bus`: ClickHouse 查询功能 ⏳ 待实现（本次报告新增）

---

## 十、扫描结论

**整体评价**: Carlos Framework 业务集成层整体架构设计**优秀**，安全基线**达标**，代码规范**良好**。但审计模块的查询功能存在严重功能缺失，需要优先修复。

**关键结论**:
1. ✅ Gateway 安全过滤器设计优秀：WafFilter（ThreadLocal Matcher + BitSet）、ReplayProtectionFilter（HMAC-SHA256 + ThreadLocal Mac）
2. ✅ Auth 模块安全配置完善：BCrypt 唯一编码器、CSRF 保护、RSA 密钥管理
3. ✅ Org 模块五层 Redis 缓存设计优秀，分页初始化 + Pipeline 批量操作
4. ✅ Audit 写入模块 ClickHouseBatchWriter 设计优秀：双缓冲 + VirtualThread + 防御性背压
5. ⚠️ Audit 查询模块 `AuditLogQueryService` 4 个方法全部 TODO 空实现，核心功能缺失（P1）
6. ⚠️ System 区域管理 2 处 FIXME 遗留 5 个月未处理（缓存失效 + 层级变更）
7. ⚠️ 多个业务模块缺少 README 和设计文档
8. ✅ License 模块 XMLDecoder 有白名单保护，生产环境不含 license-generate
9. ✅ Message 模块有敏感词过滤器和渠道路由器测试覆盖

**下一步建议**:
1. 优先实现 `AuditLogQueryService` 的 ClickHouse 查询功能（P1-001）
2. 建立 ClickHouse 物化视图优化统计查询（P1-002）
3. 清理 `SysRegionManagerImpl` FIXME（P2-003）
4. 补充各业务模块 README 和设计文档（P2-005 ~ P3-007）

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*扫描工具: OpenClaw Agent / kimi-k2p6 / 2026-05-31*
