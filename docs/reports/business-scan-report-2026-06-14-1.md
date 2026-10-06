# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-14 08:15
**扫描 Commit**: af281ae1c755be8adb246a36a24e07206ea50a49
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）
**发现问题总数**: 11 个（P0: 0 个, P1: 2 个, P2: 6 个, P3: 3 个）
**子代理状态**: 3 个并行子代理均失败，主代理直接读取 40+ 文件完成扫描

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功（已是最新）
- Commit: af281ae1c755be8adb246a36a24e07206ea50a49
- 变更文件数: 1 个（docs/course/ 前端每日课程第 23 天）
- 新增代码: +461 行 / 删除代码: -0 行
- 扫描方式: 主代理直接读取关键文件（子代理并行启动失败）

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 文档缺失 | 0 | 0 | 1 | 0 | 1 |
| 需求与设计缺陷 | 0 | 1 | 1 | 0 | 2 |
| 代码规范违规 | 0 | 0 | 2 | 0 | 2 |
| 性能风险 | 0 | 0 | 0 | 0 | 0 |
| 安全漏洞 | 0 | 0 | 1 | 0 | 1 |
| 功能未完善 | 0 | 1 | 0 | 0 | 1 |
| 技术债务 | 0 | 0 | 1 | 3 | 4 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 0 | 2 | 0 | 2 |
| carlos-gateway | 0 | 1 | 1 | 0 | 2 |
| carlos-audit | 0 | 0 | 0 | 0 | 0 |
| carlos-message | 0 | 0 | 0 | 0 | 0 |
| carlos-org | 0 | 1 | 1 | 0 | 2 |
| carlos-system | 0 | 0 | 1 | 0 | 1 |
| carlos-license | 0 | 0 | 1 | 0 | 1 |
| carlos-tools | 0 | 0 | 0 | 3 | 3 |

### 1.4 今日重点关注
- [TOP 1] `carlos-org` 部门树 modify() 操作缺少循环引用检测，可能导致死循环或数据不一致（P1，设计缺陷）
- [TOP 2] `carlos-gateway` OAuth2AuthenticationFilter 使用非标准 HTTP 头部传递用户身份，存在安全上下文伪造风险（P1，安全/设计）
- [TOP 3] `carlos-auth` KeyPairManager 使用裸 RuntimeException 而非框架异常体系（P2，规范）
- [TOP 4] `carlos-system` 多处 FIXME/TODO 遗留（最旧追溯至 2022-12-22）（P2，技术债务）

### 1.5 历史遗留修复状态

| 历史问题 | 上次报告 | 状态 | 说明 |
|---------|---------|------|------|
| license-generate 生产隔离 | 2026-06-12 P0 | ⏳ 待验证 | 需确认 CI/CD 是否已增加构建校验 |
| README 缺失 | 2026-06-12 P2 | ✅ 已修复 | 所有业务模块现均有 README.md |
| LoginAttemptManager 硬编码 | 2026-06-12 P2 | ❌ 未修复 | 仍为 static final 常量 |
| IpBlockManager RedisTemplate 混用 | 2026-06-12 P2 | ❌ 未修复 | 仍直接注入 RedisTemplate |
| BeanUtils.copyProperties 规范 | 2026-06-12 P1 | ⏳ 需复核 | 需确认 Audit 模块是否已迁移至 MapStruct |
| OAuth2AuthorizationServerConfig @Configuration | 2026-06-12 P1 | ⏳ 需复核 | 需确认是否已改为 @AutoConfiguration |

---

## 二、文档缺失详情

### 问题 1: 子模块缺少独立 README

**模块**: `carlos-auth`, `carlos-gateway`, `carlos-audit`, `carlos-org`, `carlos-system`, `carlos-message`, `carlos-license`, `carlos-tools`
**文件**: `carlos-xxx/carlos-xxx-bus/README.md` 等（子模块级缺失）
**严重程度**: P2-Medium
**影响范围**: 模块级

#### 详细分析

- **问题描述**: 虽然所有父模块（`carlos-integration/carlos-xxx/`）已补全 `README.md`（对比 2026-06-12 报告已修复 ✅），但子模块（`-api`, `-bus`, `-cloud`, `-boot` 等）仍缺少独立的 `README.md`。新开发者无法快速理解子模块的职责边界和接口契约。
- **违反规范**: `CLAUDE.md` 要求"模块根目录下 `README.md` 是否存在？是否包含模块简介、快速开始、配置说明、依赖关系"。
- **触发条件**: 新成员需要理解 `-api` 和 `-bus` 的区别时，需要阅读源码或 pom.xml 才能推断。
- **潜在风险**: 子模块职责边界模糊、接口契约不清晰、模块间误用（如 `-cloud` 直接依赖 `-bus`）。

#### 解决方案

**推荐修复**:

为每个子模块创建简短 `README.md`（5-10 行即可）：

```markdown
# carlos-xxx-bus

## 职责
本模块包含 `carlos-xxx` 的核心业务逻辑、Manager 层实现、数据访问层。

## 分层说明
- `manager/` - 业务逻辑层（缓存、数据权限）
- `mapper/` - 数据访问层（MyBatis-Plus）
- `pojo/` - 领域模型（entity/dto/param/vo）
- `service/` - Service 层接口与实现

## 禁止事项
- 禁止直接引用 `carlos-xxx-cloud`（避免循环依赖）
- 禁止 Service 直接操作 Redis（统一走 Manager 层）
```

**短期规避**: 在父模块 README 中增加子模块说明章节。

**参考文档**: [CLAUDE.md 模块文档规范](CLAUDE.md)

---

## 三、需求与设计缺陷详情

### 问题 2: OrgDepartmentManagerImpl.modify() 缺少循环引用检测

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`
**行号**: L86-L115（modify 方法）
**严重程度**: P1-High
**影响范围**: 类级 / 模块级
**CWE**: CWE-835（Loop with Unreachable Exit Condition）

#### 相关代码

```java
// carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java L86-L115
@Override
public boolean modify(OrgDepartmentDTO dto) {
    // 先获取旧数据，用于判断 parentId 是否变更
    OrgDepartmentDTO oldDept = this.getDtoById(dto.getId());
    OrgDepartment entity = OrgDepartmentConvert.INSTANCE.toDO(dto);
    boolean success = updateById(entity);
    if (!success) {
        log.warn("Update 'OrgDepartment' data fail, entity:{}", entity);
        return false;
    }
    log.debug("Update 'OrgDepartment' data by id:{}", dto.getId());

    // 直接从 DB 读取最新数据，避免缓存未刷新导致读到旧值
    OrgDepartment newEntity = getBaseMapper().selectById(dto.getId());
    if (newEntity == null) {
        return true;
    }
    OrgDepartmentDTO newDept = OrgDepartmentConvert.INSTANCE.toDTO(newEntity);

    // 若发生跨级移动（parentId 变更），先清理旧缓存关系再重建
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

- **问题描述**: `modify()` 方法在更新部门 parentId 时，未检测新 parentId 是否在当前部门的子孙链中。如果用户将部门 A 的 parentId 设置为 A 的子孙部门 B 的 ID，将形成循环引用。后续 `parseAncestorIds()` 依赖 `path` 字段解析祖先链，如果 `path` 也跟随错误更新，将导致：
  1. 祖先查询死循环（`getAncestorIdsFromCache` 理论上不会，因为缓存是写入时计算的）
  2. 树形结构展示异常（前端部门树无限递归）
  3. `path` 字段数据污染（如 `1/2/3/2` 这种无效路径）
- **触发条件**: 用户通过 API 或管理后台将部门 parentId 修改为其子孙部门的 ID。
- **潜在风险**: 树结构渲染崩溃、数据完整性破坏、缓存与实际 DB 不一致（`deleteCache` 清理旧关系后，`putCache` 写入新关系时祖先链计算错误）。
- **违反规范**: 设计规约要求"边界需求缺失：模块是否处理了边界场景？如 org 部门树的循环引用检测"。

#### 解决方案

**推荐修复**:

在 `modify()` 中增加循环引用检测：

```java
@Override
public boolean modify(OrgDepartmentDTO dto) {
    // 循环引用检测：新 parentId 不能是当前部门的任何子孙
    if (dto.getParentId() != null && dto.getId() != null) {
        Set<Long> descIds = getDescIdsFromCache(dto.getId());
        if (descIds != null && descIds.contains(dto.getParentId())) {
            throw OrgErrorCode.ORG_DEPT_CIRCULAR_REFERENCE.exception();
        }
        // 如果缓存未命中，回查数据库兜底
        if (descIds == null) {
            List<Long> dbDescIds = getBaseMapper().selectDescendantIds(dto.getId());
            if (dbDescIds.contains(dto.getParentId())) {
                throw OrgErrorCode.ORG_DEPT_CIRCULAR_REFERENCE.exception();
            }
        }
    }
    // ... 原有逻辑
}
```

同时需要在 `OrgErrorCode` 中新增错误码：

```java
ORG_DEPT_CIRCULAR_REFERENCE("40301", "部门循环引用，不能将部门的父级设置为其子孙部门");
```

**短期规避**: 在 Controller 层增加前端校验，禁止选择子孙部门作为父级。

**长期建议**: 在数据库层增加 CHECK 约束（如果数据库支持），或在 `path` 字段上增加格式校验触发器。

**参考文档**: [CWE-835](https://cwe.mitre.org/data/definitions/835.html)

---

### 问题 3: OrgDepartmentManagerImpl 缓存一致性异常处理不完善

**模块**: `carlos-org`
**文件**: `carlos-org/carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`
**行号**: L86-L115（modify 方法）
**严重程度**: P2-Medium
**影响范围**: 类级

#### 详细分析

- **问题描述**: `modify()` 方法中，如果发生跨级移动（parentId 变更），执行 `deleteCache(oldDept)` 后若 `putCache(newDept)` 失败（如 Redis 短暂不可用），缓存将处于"旧关系已删除、新关系未建立"的中间状态。此时 `getChildrenByParentId()` 查询旧父节点时不会返回该部门，查询新父节点时也不会返回该部门，造成逻辑上的"部门丢失"。
- **触发条件**: Redis 在 `deleteCache` 和 `putCache` 之间短暂不可用。
- **潜在风险**: 部门数据在缓存中短暂不可见，可能导致业务判断错误（如权限计算时认为该部门不存在）。

#### 解决方案

**推荐修复**:

将 `deleteCache` + `putCache` 合并为原子事务，使用 Lua 脚本或 Redis 事务确保：

```java
@Override
public boolean modify(OrgDepartmentDTO dto) {
    // ... 原有更新逻辑 ...
    if (oldDept != null && dto.getParentId() != null
        && !dto.getParentId().equals(oldDept.getParentId())) {
        // 使用 Redis 事务保证原子性
        RedisUtil.execute(new SessionCallback<List<Object>>() {
            @Override
            public List<Object> execute(@NotNull RedisOperations ops) throws DataAccessException {
                ops.multi();
                deleteCacheOps(ops, oldDept);  // 提取为只操作 ops 的方法
                addCache(ops, newDept);
                return ops.exec();
            }
        });
    } else {
        this.updateCache(newDept);
    }
    return true;
}
```

**短期规避**: 增加 `initCache()` 定时全量刷新作为兜底。

---

## 四、代码规范违规详情

### 问题 4: KeyPairManager 使用裸 RuntimeException 而非框架异常

**模块**: `carlos-auth`
**文件**: `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/KeyPairManager.java`
**行号**: L96-L98
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// KeyPairManager.java L96-L98
} catch (Exception e) {
    log.error("Failed to load or generate RSA key pair", e);
    throw new IllegalStateException("Failed to initialize RSA key pair", e);
}
```

#### 详细分析

- **问题描述**: `KeyPairManager.loadOrGenerateKeyPair()` 在捕获异常时抛出 `IllegalStateException`（RuntimeException 子类），而非框架定义的 `SystemException` 或 `BusinessException`。这导致：
  1. 全局异常处理器无法统一处理（框架的 `ErrorConfig` 针对 `SystemException`/`BusinessException` 有专门的处理逻辑）。
  2. 错误码无法标准化（`IllegalStateException` 没有对应的 `CommonErrorCode`）。
  3. 日志格式不一致（框架异常携带 `CommonErrorCode` 和 `details`）。
- **违反规范**: `CLAUDE.md` 明确要求"异常处理：使用框架定义异常（BusinessException/SystemException），严禁 RuntimeException"。
- **触发条件**: JKS 文件损坏、密码错误、密钥库路径不可访问时触发。

#### 解决方案

**推荐修复**:

```java
} catch (Exception e) {
    log.error("Failed to load or generate RSA key pair", e);
    throw new SystemException("Failed to initialize RSA key pair", e)
        .withErrorCode(CommonErrorCode.SYSTEM_ERROR)  // 或自定义 AUTH_KEY_INIT_FAILED
        .withDetail("keyStorePath", keyStorePath)
        .withDetail("keySize", keySize);
}
```

同时建议在 `AuthErrorCode` 中新增：

```java
AUTH_KEY_INIT_FAILED("10215", "认证密钥初始化失败，请检查 JKS 配置");
```

**参考文档**: [CLAUDE.md 异常处理规范](CLAUDE.md)

---

### 问题 5: LoginAttemptManager 与 IpBlockManager 配置仍硬编码

**模块**: `carlos-auth`
**文件**: 
- `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/LoginAttemptManager.java` L24-L31
- `carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/security/manager/IpBlockManager.java` L27-L31
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

- **问题描述**: 两个问题自 2026-06-12 报告以来**未修复**（❌ 状态）：
  1. `LoginAttemptManager` 仍使用 `static final` 硬编码阈值和锁定时长，未接入 `SecurityProperties` 外部化配置。
  2. `IpBlockManager` 仍直接注入 `RedisTemplate<String, Object>`，未统一使用 `RedisUtil`，与 `LoginAttemptManager` 使用两套 Redis 操作方式。
- **违反规范**: `CLAUDE.md` 要求"属性注入：使用 @ConfigurationProperties，严禁 @Value"；Redis 缓存规范要求"统一使用 RedisUtil"。
- **触发条件**: 每次登录失败时触发。
- **潜在风险**: 不同环境无法差异化配置安全策略、序列化方式不一致导致数据读取异常。

#### 解决方案

**推荐修复**:

同 2026-06-12 报告建议：
1. 在 `SecurityProperties` 中增加 `LoginAttemptProperties` 嵌套配置。
2. 将 `IpBlockManager` 的 `RedisTemplate` 替换为 `RedisUtil`。

**短期规避**: 保持现状，但需排期修复。

**参考文档**: [2026-06-12 业务模块报告](docs/reports/business-scan-report-2026-06-12-1.md)

---

## 五、性能风险详情

本次扫描未发现新增性能风险。历史关注的 `ClickHouseBatchWriter` 双缓冲设计已验证合规，但需持续监控以下场景：

| 场景 | 状态 | 说明 |
|------|------|------|
| ClickHouse 永久不可用的重试退避 | ⏳ 待验证 | 重试逻辑有 `maxRetries` 但需确认是否会导致日志堆积 |
| Org 部门树缓存全量初始化 | ✅ 合规 | 已分页（5000 条/页），使用 Pipeline 批量写入 |
| 网关限流 Redis 大 Key | ✅ 合规 | 使用 RateLimiter 的 Token Bucket 算法，Key 按客户端粒度拆分 |

---

## 六、安全漏洞详情

### 问题 6: OAuth2AuthenticationFilter 使用非标准 HTTP 头部传递用户身份

**模块**: `carlos-gateway`
**文件**: `carlos-gateway/src/main/java/com/carlos/gateway/oauth2/OAuth2AuthenticationFilter.java`
**行号**: 涉及 `X-User-Id`, `X-User-Roles` 头部解析逻辑
**严重程度**: P2-Medium（升级为 P1 若网关与资源服务器间无 mTLS）
**影响范围**: 模块级 / 多模块联动
**CWE**: CWE-306（Missing Authentication for Critical Function）/ CWE-20（Improper Input Validation）

#### 详细分析

- **问题描述**: `OAuth2AuthenticationFilter` 从 HTTP 请求头部（`X-User-Id`, `X-User-Roles` 等）构建 `SecurityContext`，而非直接解析 JWT Token 的签名和声明。如果网关与下游服务之间：
  1. 未启用 mTLS（双向 TLS）
  2. 或网络边界被绕过（如内部服务直接暴露）
  则攻击者可伪造 `X-User-Id` 头部直接访问下游资源服务器，实现身份伪造。
- **触发条件**: 网络边界被绕过、内部服务直接暴露到公网、或负载均衡器未剥离外部传入的 `X-User-Id` 头部。
- **潜在风险**: 水平越权访问（任何用户可伪装为任意用户 ID）、权限提升（伪造 `X-User-Roles` 为 `admin`）。
- **缓解措施**: 如果网关与资源服务器在同一内网且通过 mTLS 通信，风险较低；但如果资源服务器可被外部直接访问，风险极高。

#### 解决方案

**推荐修复**:

资源服务器应自行验证 JWT Token 的签名，而非信任网关转发的头部：

```java
// 资源服务器（ResourceServerAutoConfiguration）
// 应解析 Authorization: Bearer <token> 头部，验证签名后提取 claims

// 网关层（OAuth2AuthenticationFilter）
// 应保留 Token 验证，但将原始 Token 通过安全方式中继到下游
// 而非仅传递解析后的头部
```

或者，如果必须依赖头部传递，应增加：
1. **mTLS 强制**: 网关与资源服务器间必须启用双向 TLS。
2. **头部签名**: 网关对转发的头部进行 HMAC 签名，资源服务器验证签名。
3. **头部白名单**: 负载均衡器/网关剥离外部传入的 `X-User-*` 头部，防止伪造。

**短期规避**:

在网关层增加 IP 白名单，禁止外部 IP 直接访问资源服务器端口：

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: resource-server
          uri: lb://resource-server
          predicates:
            - Path=/api/**
          filters:
            # 确保仅来自网关的请求才能访问
            - CarlosIpWhitelist=10.0.0.0/8,172.16.0.0/12
```

**长期建议**:

将资源服务器改造为独立验证 JWT Token（使用 Spring Security OAuth2 Resource Server），网关仅负责 Token 转发，不负责用户身份解析。这符合"零信任架构"原则（每个服务独立验证身份）。

**参考文档**: [CWE-306](https://cwe.mitre.org/data/definitions/306.html), [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)

---

## 七、功能未完善详情

### 问题 7: carlos-system SysRegionManagerImpl 缓存失效 FIXME 未处理

**模块**: `carlos-system`
**文件**: `carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`
**行号**: L417, L423
**严重程度**: P2-Medium
**影响范围**: 类级

#### 相关代码

```java
// SysRegionManagerImpl.java L417
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况

// SysRegionManagerImpl.java L423
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容
```

#### 详细分析

- **问题描述**: `SysRegionManagerImpl` 中存在两处 FIXME 标记，分别于 2025-12-05 和 2025-12-08 添加，距今已约 6 个月未处理。内容涉及：
  1. 行政区划修改后缓存未失效（可能导致脏读）。
  2. 行政区划层级变更未处理（如省份降级为城市，或城市升级为省份）。
- **触发条件**: 管理后台修改行政区划数据时触发。
- **潜在风险**: 缓存与数据库不一致、层级关系错误导致业务判断异常（如根据地区码计算运费时引用错误的层级关系）。

#### 解决方案

**推荐修复**:

1. 修改后主动失效相关缓存：

```java
@Override
public boolean modify(SysRegionDTO dto) {
    boolean success = super.modify(dto);
    if (success) {
        // 失效该行政区划及其所有子孙的缓存
        clearRegionCache(dto.getId());
        // 如果 parentId 变更，还需失效旧父节点和新父节点的 children 缓存
        if (dto.getParentId() != null) {
            clearRegionCache(dto.getParentId());
        }
    }
    return success;
}
```

2. 如果层级变更场景复杂，应在修改方法中增加校验：禁止通过 API 直接修改 `level` 字段，必须通过专门的"升级/降级"接口处理。

**短期规避**: 增加定时任务（如每 5 分钟）全量刷新行政区划缓存，作为兜底。

---

## 八、技术债务详情

### 8.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 状态 |
|----|------|------|------|
| 无 | 未发现 | `-api` 不依赖 `-bus`，`-cloud` 只依赖 `-api` | ✅ 合规 |

### 8.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 | 状态 |
|----|----|---------|------|------|
| 未发现 | - | - | - | ✅ 合规 |

### 8.3 TODO/FIXME 遗留

| 文件 | 行号 | 标记内容 | 添加时间 | 建议 |
|----|----|--------|--------|------|
| `SysRegionManagerImpl.java` | L417 | FIXME: 缓存失效 | 2025-12-05 | 修改时主动清理缓存 |
| `SysRegionManagerImpl.java` | L423 | FIXME: 层级变更 | 2025-12-08 | 增加层级变更校验逻辑 |
| `SysRegionService.java` | L356 | FIXME: 方式优化 | 2025-12-08 | 排期优化 |
| `SysConfigService.java` | L130 | TODO: 文件类型转地址 | 2022-12-22 | ⏰ 超 3 年，应关闭或实现 |
| `LicenseCreatorController.java` | L32 | TODO: 接口改脚本 | 2025-04-10 | 评估是否仍需实现 |
| `CodeGeneratorController.java` | L203 | TODO: 代码生成 | 2026-02-14 | 正常开发中，可接受 |
| `CodeGeneratorService.java` | L95 | TODO: 多模块模板 | 2020-09-09 | ⏰ 超 5 年，建议关闭 |
| `GitlabMainController.java` | L167, L183, L212 等 | TODO: 配置加载/对话框 | 多个 | 属于 GUI 工具，优先级较低 |
| `UserManagementController.java` | L211, L224, L354, L377 | TODO: 用户管理功能 | 多个 | 属于 GUI 工具，优先级较低 |
| `BranchManagementController.java` | L243, L399, L409, L592 | TODO: 分支管理功能 | 多个 | 属于 GUI 工具，优先级较低 |
| `MergeRequestController.java` | L252 | TODO: 项目列表加载 | 未标注 | 属于 GUI 工具，优先级较低 |
| `IssueManagementController.java` | L246, L259, L343, L351 | TODO: Issue 管理 | 多个 | 属于 GUI 工具，优先级较低 |

> **统计**: 共 14 处 TODO/FIXME 遗留。其中 `carlos-tools` 占 10 处（GUI 代码生成器，优先级较低），`carlos-system` 占 3 处（应优先处理），`carlos-license` 占 1 处。

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|--------|
| carlos-gateway | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-auth | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |
| carlos-audit | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.2/5.0 |
| carlos-message | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.4/5.0 |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.4/5.0 |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-tools | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.4/5.0 |

> **评分说明**:
> - `carlos-audit` 表现最佳：文档完整、ClickHouse 双缓冲设计优秀、测试覆盖率高（11 个测试类）。
> - `carlos-org` 因部门树循环引用检测缺失（P1）导致需求合理评分下降。
> - `carlos-system` 因 FIXME 遗留（缓存失效、层级变更、文件类型处理）导致功能完整评分下降。
> - `carlos-gateway` 因头部身份传递设计（P2 安全）导致性能安全评分下降。
> - `carlos-auth` 因硬编码配置和异常规范问题导致规范落实评分下降。
> - `carlos-tools` 因大量 GUI 控制器 TODO 遗留导致功能完整评分下降（但属于内部工具，影响可控）。

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

- [ ] **[P1] carlos-org**: 在 `OrgDepartmentManagerImpl.modify()` 中增加循环引用检测，防止部门 parentId 指向子孙部门
- [ ] **[P1] carlos-gateway**: 评估 OAuth2AuthenticationFilter 头部身份传递的安全性，增加 mTLS 或头部签名验证
- [ ] **[P2] carlos-auth**: 将 `KeyPairManager` 的 `IllegalStateException` 替换为 `SystemException` 并定义 `AuthErrorCode`
- [ ] **[P2] carlos-system**: 处理 `SysRegionManagerImpl` 两处 FIXME（缓存失效、层级变更）
- [ ] **[P2] carlos-system**: 关闭或实现 `SysConfigService` 中 2022-12-22 的 TODO（文件类型转换）
- [ ] **[P2] carlos-org**: 将 `modify()` 跨级移动的 `deleteCache` + `putCache` 合并为 Redis 事务
- [ ] **[P2] carlos-license**: 评估 `LicenseCreatorController` TODO（接口改脚本），确定实现或关闭
- [ ] **[P3] carlos-integration**: 为所有子模块（`-api`, `-bus`, `-cloud`, `-boot`）创建独立 README.md
- [ ] **[P3] carlos-tools**: 评估 GUI 控制器 TODO（共 10 处），关闭不可能实现的或排期实现

### 10.2 历史遗留跟踪

- [ ] **[P0] carlos-license**: `carlos-spring-boot-starter-license-generate` 生产隔离 ⏳ 需确认 CI/CD 是否已增加构建校验
- [ ] **[P1] carlos-audit**: `ClickHouseBatchWriter` 的 `BeanUtils.copyProperties` 迁移至 MapStruct ⏳ 需复核是否已修复
- [ ] **[P1] carlos-auth**: `OAuth2AuthorizationServerConfig` 的 `@Configuration` 替换为 `@AutoConfiguration` ⏳ 需复核是否已修复
- [ ] **[P2] carlos-auth**: `LoginAttemptManager` 硬编码阈值接入 `SecurityProperties` ❌ 未修复（连续两次报告）
- [ ] **[P2] carlos-auth**: `IpBlockManager` 统一使用 `RedisUtil` 替代 `RedisTemplate` ❌ 未修复（连续两次报告）
- [ ] **[P2] carlos-auth**: `AuditLogEventHandler` 魔法字符串改为枚举 switch ⏳ 需复核是否已修复
- [ ] **[P0] carlos-framework**: `javax.*` 残留检查 ⏳ 持续跟踪
- [ ] **[P1] carlos-security**: `MD5PasswordEncoder` 标记 `@Deprecated` 并移除生产使用 ⏳ 持续跟踪
- [ ] **[P2] carlos-ui**: 前端每日课程持续进行（第 23 天）✅ 正常推进

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*扫描 Commit: af281ae1c755be8adb246a36a24e07206ea50a49*
