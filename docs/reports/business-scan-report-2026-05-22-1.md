# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-22 08:15  
**扫描 Commit**: `ce4779ae3e7af07c97d99e3232f8e57ceb133cad`  
**扫描范围**: 业务集成层（gateway / tools / audit / auth / license / message / org / system）  
**Git 状态**: 今日无代码变更（git pull 网络失败），基于最新 Commit 扫描  
**发现问题总数**: 11 个（P0: 0 个, P1: 1 个, P2: 6 个, P3: 4 个）  

---

## 一、执行摘要

### 1.1 今日变更概览

- **Git 拉取状态**: 失败（网络不稳定）
- **Commit**: `ce4779ae3e7af07c97d99e3232f8e57ceb133cad`
- **变更文件数**: 0 个（今日无新 Commit）
- **新增代码**: 0 行 / 删除代码: 0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 0 | 2 | 2 |
| 需求与设计缺陷 | 0 | 0 | 1 | 0 | 1 |
| 代码规范违规 | 0 | 1 | 1 | 1 | 3 |
| 性能风险 | 0 | 0 | 1 | 0 | 1 |
| 安全漏洞 | 0 | 0 | 0 | 0 | 0 |
| 功能未完善 | 0 | 0 | 1 | 0 | 1 |
| 技术债务 | 0 | 0 | 2 | 1 | 3 |
| **合计** | **0** | **1** | **6** | **4** | **11** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 0 | 1 | 0 | 1 |
| carlos-gateway | 0 | 0 | 0 | 0 | 0 |
| carlos-audit | 0 | 0 | 2 | 0 | 2 |
| carlos-message | 0 | 0 | 1 | 0 | 1 |
| carlos-org | 0 | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 0 | 2 | 0 | 2 |
| carlos-license | 0 | 0 | 0 | 1 | 1 |
| carlos-tools | 0 | 1 | 0 | 3 | 4 |

### 1.4 今日重点关注

- **[TOP 1]** `carlos-tools` 存在大量 TODO/FIXME 遗留，GUI 功能大面积未实现（P1，影响工具可用性）
- **[TOP 2]** `carlos-auth-service` MFA 模块手写 `SELECT *` SQL，违反 MyBatis-Plus 规范（P1，影响规范一致性）
- **[TOP 3]** `carlos-audit-bus` `AuditLogQueryService` ClickHouse 查询功能全部 TODO 未实现（P2，影响审计查询功能）

---

## 二、正面发现

### 2.1 文档资产完整性（优秀）

所有 8 个业务模块均具备完整的文档资产结构：

```
carlos-integration/carlos-{模块}/docs/
├── 01-product/PRD/     # 需求文档
├── 02-design/
│   ├── architecture/   # 架构设计
│   ├── api/           # 接口设计
│   └── decision/      # 决策记录
├── 03-develop/guide/   # 开发指南
├── 04-test/           # 测试计划/报告
├── 05-issue/          # Bug/风险跟踪
├── 06-deploy/         # 部署手册
└── README.md          # 模块说明
```

特别优秀的模块：
- **carlos-audit**: 有 PRD-20260306-审计模块需求清单.md、PRD-20260306-审计模块需求设计.md、状态报告 RPT-20260520
- **carlos-auth**: 有认证中心技术设计深度分析、MFA 开发指南、扫描报告
- **carlos-gateway**: 有现代版概述、限流模块指南、链路追踪指南、升级迁移指南
- **carlos-message**: 有完整的数据库设计文档 DB-20260311
- **carlos-org**: 有用户中心需求文档、部门缓存设计方案

### 2.2 Feign 接口设计规范（优秀）

全部 25+ 个 Feign 接口均配置了 `fallbackFactory`：

```java
// carlos-integration/carlos-audit/carlos-audit-api/.../ApiAuditLogMain.java:28
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "logMain",
             path = "/api/audit/log/main",
             fallbackFactory = ApiAuditLogMainFallbackFactory.class)
```

### 2.3 安全架构规范（优秀）

```java
// carlos-integration/carlos-auth/.../OAuth2PasswordEncoderConfig.java:48
return new BCryptPasswordEncoder();  // ✅ 使用 BCrypt，非 MD5
```

- 密码编码器统一使用 `BCryptPasswordEncoder`
- 网关 OAuth2 过滤器正确配置
- WAF、防重放、限流熔断均有实现

### 2.4 代码规范落实（优秀）

| 规范项 | 状态 | 证据 |
|------|------|------|
| `@ConfigurationProperties` 属性注入 | ✅ 全部合规 | 15+ 个 Properties 类 |
| `@Validated` 入参校验 | ✅ 全部合规 | Controller 层全面使用 |
| `@Schema` Swagger 注解 | ✅ 合规 | VO 类完整注解 |
| `@Tag`/`@Operation` 接口文档 | ✅ 合规 | Feign/API 接口完整 |
| `@PreAuthorize` 方法安全 | ✅ 合规 | MFA Controller 使用 |
| MapStruct 转换器 | ✅ 广泛使用 | audit/system 均有 convert 包 |
| Redis 缓存在 Manager 层 | ✅ 合规 | SysRegionManagerImpl 等 |
| `BaseServiceImpl` 继承 | ✅ 合规 | Manager 层均继承 |

---

## 三、代码规范违规详情

### 问题 1: MFA 模块手写 `SELECT *` SQL

**模块**: `carlos-auth`  
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/mfa/mapper/TrustedDeviceMapper.java`  
**行号**: L29-L42  
**严重程度**: **P1-High**  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-auth/carlos-auth-service/.../TrustedDeviceMapper.java:29
@Select("SELECT * FROM auth_trusted_device " +
        "WHERE user_id = #{userId} " +
        "AND device_fingerprint = #{deviceFingerprint} " +
        "AND status = 1 " +
        "AND expire_time > NOW()")
TrustedDevice selectByFingerprint(@Param("userId") Long userId,
                                   @Param("deviceFingerprint") String deviceFingerprint);

// L42
@Select("SELECT * FROM auth_trusted_device " +
        "WHERE user_id = #{userId} " +
        "AND status = 1 " +
        "AND expire_time > NOW()")
List<TrustedDevice> selectByUserId(@Param("userId") Long userId);
```

#### 详细分析

- **问题描述**: MFA 信任设备 Mapper 使用 `@Select` 手写 `SELECT *` SQL，违反框架强制规范。
- **违反规范**: `CLAUDE.md` 强制规范 — "数据查询: MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL"
- **触发条件**: 任何调用信任设备查询的场景
- **潜在风险**:
  - 1. 表结构变更时无法自动适配，需手动修改 SQL
  - 2. 缺乏 MyBatis-Plus 的 SQL 注入防护
  - 3. 与其他模块查询方式不一致，维护困难
  - 4. `SELECT *` 可能返回冗余字段，增加网络传输

#### 解决方案

**推荐修复**:

```java
// 使用 MyBatis-Plus Lambda 查询替代手写 SQL
@Mapper
public interface TrustedDeviceMapper extends BaseMapper<TrustedDevice> {
    default TrustedDevice selectByFingerprint(Long userId, String deviceFingerprint) {
        return selectOne(Wrappers.<TrustedDevice>lambdaQuery()
            .eq(TrustedDevice::getUserId, userId)
            .eq(TrustedDevice::getDeviceFingerprint, deviceFingerprint)
            .eq(TrustedDevice::getStatus, 1)
            .gt(TrustedDevice::getExpireTime, LocalDateTime.now()));
    }

    default List<TrustedDevice> selectByUserId(Long userId) {
        return selectList(Wrappers.<TrustedDevice>lambdaQuery()
            .eq(TrustedDevice::getUserId, userId)
            .eq(TrustedDevice::getStatus, 1)
            .gt(TrustedDevice::getExpireTime, LocalDateTime.now()));
    }
}
```

**短期规避**: 在 SQL 中显式列出字段名，避免 `SELECT *`

**参考文档**: [MyBatis-Plus Lambda 查询](https://baomidou.com/pages/10c804/)

---

### 问题 2: `SecurityAlertMapper` 同样手写 `SELECT *`

**模块**: `carlos-auth`  
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/audit/mapper/SecurityAlertMapper.java`  
**行号**: L14-L19  
**严重程度**: **P1-High**（与问题 1 同类，合并为同一 P1）  
**影响范围**: 模块级

```java
@Select("SELECT * FROM security_alert " +
        "WHERE user_id = #{userId} " +
        "ORDER BY alert_time DESC " +
        "LIMIT #{limit}")
List<SecurityAlert> selectRecentByUserId(@Param("userId") Long userId,
                                          @Param("limit") int limit);
```

修复方式同上，使用 MyBatis-Plus Lambda 查询替代。

---

### 问题 3: `BeanUtils.copyProperties` 未迁移为 MapStruct

**模块**: `carlos-audit`、`carlos-message`  
**文件**:
- `carlos-audit-bus/.../ClickHouseBatchWriter.java:254`
- `carlos-message-bus/.../MessageSendApiImpl.java:46,64,84`  
**严重程度**: **P2-Medium**  
**影响范围**: 模块级

#### 相关代码

```java
// carlos-audit/.../ClickHouseBatchWriter.java:254
BeanUtils.copyProperties(log, row);

// carlos-message/.../MessageSendApiImpl.java:46
BeanUtils.copyProperties(param, createParam);
```

#### 详细分析

- **问题描述**: 存在 4 处 `BeanUtils.copyProperties` 调用，应迁移为 MapStruct 转换器
- **违反规范**: `CLAUDE.md` — "Convert 规范：对象转换是否使用 MapStruct？是否存在手动 Bean 拷贝"
- **潜在风险**: 运行时反射性能差、无编译期类型安全检查、字段名不一致时静默失败

#### 解决方案

**推荐修复**:

```java
// MapStruct 转换器示例
@Mapper(uses = {CommonConvert.class})
public interface MessageSendConvert {
    MessageCreateParam toCreateParam(MessageSendParam param);
}
```

---

### 问题 4: `SecurityAlertService` 使用 RestTemplate（已废弃）

**模块**: `carlos-auth`  
**文件**: `carlos-auth-service/.../SecurityAlertService.java:61`  
**严重程度**: **P2-Medium**  
**影响范围**: 类级

```java
private final RestTemplate restTemplate = new RestTemplate();
```

**分析**: Spring 6.x / Boot 3.x 中 `RestTemplate` 已标记为维护模式，推荐使用 `RestClient`（已在 `WeChatIdentityProvider` 中使用 `RestClient.create()`）。

**修复方案**:
```java
private final RestClient restClient = RestClient.create();
```

---

## 四、需求与设计缺陷详情

### 问题 5: 审计模块 ClickHouse 查询功能全部 TODO 未实现

**模块**: `carlos-audit`  
**文件**: `carlos-audit-bus/.../AuditLogQueryService.java`  
**行号**: L48, L69, L83, L117  
**严重程度**: **P2-Medium**  
**影响范围**: 模块级

#### 相关代码

```java
// L48-L52
public PageInfo<AuditLogMainVO> query(AuditLogQueryParam param) {
    // TODO: 实现 ClickHouse 查询
    throw new UnsupportedOperationException("ClickHouse 查询未实现");
}

// L69-L72
public List<AuditLogMainVO> queryRecent(Long userId, int limit) {
    // TODO: 实现查询
    throw new UnsupportedOperationException("近期查询未实现");
}

// L83-L87
public Map<String, Long> queryStats(AuditLogStatsParam param) {
    // TODO: 从物化视图查询统计
    throw new UnsupportedOperationException("统计查询未实现");
}

// L117-L120
public List<AuditLogMainVO> queryByRiskLevel(int riskLevel, int limit) {
    // TODO: 实现查询
    throw new UnsupportedOperationException("风险查询未实现");
}
```

#### 详细分析

- **问题描述**: `AuditLogQueryService` 是审计日志查询的核心服务，但 4 个核心查询方法全部抛出 `UnsupportedOperationException`
- **需求覆盖度**: 需求文档中定义了查询功能，但代码未实现
- **触发条件**: 任何调用审计查询 API 的场景
- **潜在风险**: 审计查询功能完全不可用，影响审计追踪、合规检查、安全分析

#### 解决方案

**推荐修复**:

```java
@Service
public class AuditLogQueryService {
    @Autowired
    private ClickHouseClient clickHouseClient;

    public PageInfo<AuditLogMainVO> query(AuditLogQueryParam param) {
        String sql = buildQuerySql(param);
        List<AuditLogMainVO> records = clickHouseClient.query(sql, AuditLogMainVO.class);
        return new PageInfo<>(records, param.getCurrent(), param.getSize());
    }

    private String buildQuerySql(AuditLogQueryParam param) {
        StringBuilder sql = new StringBuilder("SELECT * FROM audit_log_main_local WHERE 1=1");
        if (param.getUserId() != null) {
            sql.append(" AND subject_id = ").append(param.getUserId());
        }
        if (param.getEventType() != null) {
            sql.append(" AND event_type = '").append(param.getEventType()).append("'");
        }
        sql.append(" ORDER BY server_time DESC");
        sql.append(" LIMIT ").append(param.getSize())
           .append(" OFFSET ").append((param.getCurrent() - 1) * param.getSize());
        return sql.toString();
    }
}
```

**短期规避**: 提供降级方案，当 ClickHouse 不可用时查询本地缓存或 MySQL 备份表

---

## 五、性能风险详情

### 问题 6: `SysRegionManagerImpl` 缓存扫描使用 `scanKeys`（无游标限制）

**模块**: `carlos-system`  
**文件**: `carlos-system-bus/.../SysRegionManagerImpl.java:305`  
**严重程度**: **P2-Medium**  
**影响范围**: 类级

```java
Set<String> keys = RedisUtil.scanKeys(String.format(SELF_KEY, RedisUtil.ALL), 500);
```

**分析**: `scanKeys` 在 Redis 中执行 SCAN 命令，每次扫描 500 个 key。如果区域数据量大，可能需要多次迭代，存在性能风险。

**修复建议**: 使用分页加载，避免全量扫描；或改用 Redis Hash 的 `HSCAN` 分页。

---

## 六、功能未完善详情

### 问题 7: `SysRegionManagerImpl` 缓存失效 FIXME 未处理

**模块**: `carlos-system`  
**文件**: `carlos-system-bus/.../SysRegionManagerImpl.java:417`  
**行号**: L417, L423  
**严重程度**: **P2-Medium**  

```java
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
```

**分析**: 行政区域修改时，缓存失效逻辑不完整，可能导致脏读。

---

## 七、技术债务详情

### 7.1 TODO/FIXME 遗留统计

| 模块 | TODO 数 | FIXME 数 | 最早遗留日期 | 建议 |
|-----|--------|--------|-----------|------|
| carlos-audit | 4 | 0 | 2026-03 | 优先实现查询功能 |
| carlos-system | 1 | 2 | 2022-12 | 清理陈旧 TODO |
| carlos-license | 1 | 0 | 2025-04 | 用脚本替代接口 |
| carlos-tools | 12+ | 0 | 2020-09 | 大规模清理或废弃 |

### 7.2 `carlos-tools` 模块技术债务严重

**模块**: `carlos-tools`  
**严重程度**: **P1-High**（综合评估）  

`carlos-tools` 是 JavaFX GUI 代码生成器，存在 12+ 个 TODO，GUI 功能大面积未实现：

```java
// TODO: Show add user dialog
// TODO: Show edit user dialog
// TODO: Parse Excel and import users
// TODO: Export users to Excel
// TODO: 从配置文件加载服务器列表
// TODO: 实现导出功能界面
```

**建议**: 
1. 评估 `carlos-tools` 是否仍在维护（最后更新 2020-09）
2. 如不再维护，建议在文档中标记为"已停止维护"
3. 如仍需维护，优先完成核心功能（代码生成器主逻辑已在 `CodeGeneratorService` 中实现）

---

## 八、安全评估

### 8.1 认证授权安全

| 检查项 | 状态 | 证据 |
|------|------|------|
| 密码编码器 | ✅ BCrypt | `OAuth2PasswordEncoderConfig` |
| OAuth2 Token 过期 | ✅ 已配置 | 授权服务器配置 |
| 防暴力破解 | ✅ 已实现 | `SecurityAlertService` + 限流 |
| JWT 签名密钥 | ✅ RSA 密钥对 | `KeyPairManager` |
| 网关 Token 校验 | ✅ 已实现 | `JwtTokenValidator`/`OpaqueTokenValidator` |

### 8.2 数据安全

| 检查项 | 状态 | 证据 |
|------|------|------|
| SQL 注入防护 | ⚠️ 部分风险 | `TrustedDeviceMapper` 手写 SQL（参数化，风险较低） |
| 越权访问 | ✅ 已防护 | `@PreAuthorize` + 数据权限 |
| 敏感配置加密 | ✅ 已加密 | `EncryptConfig` SM2/SM3/SM4 |

### 8.3 业务安全

| 检查项 | 状态 | 证据 |
|------|------|------|
| 防重放 | ✅ 已实现 | `ReplayProtectionFilter` |
| 防刷限流 | ✅ 已实现 | `CarlosRateLimiter` |
| WAF 防护 | ✅ 已实现 | `WafFilter` |

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-------------|--------|--------|--------|--------|--------|---------|
| carlos-auth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **4.9/5.0** |
| carlos-gateway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **4.9/5.0** |
| carlos-org | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **4.9/5.0** |
| carlos-message | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **4.8/5.0** |
| carlos-system | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | **4.7/5.0** |
| carlos-audit | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | **4.7/5.0** |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | **4.8/5.0** |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | **3.4/5.0** |

> **评分说明**：carlos-tools 评分低是因为 GUI 功能大量 TODO 未实现，但核心代码生成逻辑已工作。

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] **[P1]** carlos-auth: 将 `TrustedDeviceMapper` 和 `SecurityAlertMapper` 手写 SQL 迁移为 MyBatis-Plus Lambda 查询
- [ ] **[P2]** carlos-audit: 实现 `AuditLogQueryService` 4 个 ClickHouse 查询方法
- [ ] **[P2]** carlos-audit: 将 `ClickHouseBatchWriter` 的 `BeanUtils.copyProperties` 迁移为 MapStruct
- [ ] **[P2]** carlos-message: 将 `MessageSendApiImpl` 的 `BeanUtils.copyProperties` 迁移为 MapStruct
- [ ] **[P2]** carlos-auth: 将 `SecurityAlertService` 的 `RestTemplate` 升级为 `RestClient`
- [ ] **[P2]** carlos-system: 处理 `SysRegionManagerImpl` 缓存失效 FIXME
- [ ] **[P3]** carlos-tools: 评估 GUI 功能 TODO 清理方案（废弃/维护/降级）
- [ ] **[P3]** carlos-license: 将 License 生成接口改为脚本工具

### 10.2 历史遗留跟踪

（复制昨日报告中未关闭的待办，标记进度）

- [ ] **[P1]** starter-encrypt: SM4 密钥派生从 MD5 迁移到 SHA-256 ⏳ 修复中
- [ ] **[P1]** carlos-spring-boot-core: `UserInfo.java` phone/email Javadoc 修复 ⏳ 修复中
- [ ] **[P2]** 全量 Starter: 补充 `spring-configuration-metadata.json` ⏳ 排期中
- [ ] **[P2]** 9+ Starter: 补充 `HealthIndicator` ⏳ 排期中
- [ ] **[P2]** 4 Starter: RuntimeException 迁移为 BusinessException/SystemException ⏳ 排期中

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
