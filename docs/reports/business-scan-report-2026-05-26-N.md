# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-26 08:42  
**扫描 Commit**: `cac0340f`  
**扫描范围**: 业务集成层（gateway / tools / audit / auth / license / message / org / system）  
**发现问题总数**: 14 个（P0: 0 个, P1: 5 个, P2: 3 个, P3: 6 个）  
**扫描状态**: 今日无代码变更，基于最新 Commit `cac0340f` 全量扫描

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 失败（网络不可用），使用本地代码继续扫描
- Commit: `cac0340f`
- 变更文件数: 0 个（今日无新 Commit）
- 新增代码: +0 行 / 删除代码: -0 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 安全漏洞 | 0 | 4 | 0 | 0 | 4 |
| 代码规范违规 | 0 | 1 | 0 | 0 | 1 |
| 功能未完善 | 0 | 0 | 1 | 3 | 4 |
| 性能风险 | 0 | 0 | 1 | 0 | 1 |
| 技术债务 | 0 | 0 | 1 | 3 | 4 |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 2 | 0 | 0 | 2 |
| carlos-gateway | 0 | 0 | 0 | 0 | 0 |
| carlos-audit | 0 | 0 | 1 | 3 | 4 |
| carlos-message | 0 | 0 | 1 | 0 | 1 |
| carlos-org | 0 | 0 | 0 | 0 | 0 |
| carlos-system | 0 | 1 | 0 | 0 | 1 |
| carlos-license | 0 | 0 | 0 | 0 | 0 |
| carlos-tools | 0 | 0 | 0 | 0 | 0 |

### 1.4 今日重点关注
- **[TOP 1]** auth-cloud 硬编码 SM2 私钥 + SM4 密钥 + 明文密码（P1，安全红线）
- **[TOP 2]** 多处配置文件硬编码数据库密码（P1，system/auth/org/audit）
- **[TOP 3]** SysDictMapper.xml 手写 SQL 使用 `SELECT *`（P1，性能风险）

---

## 二、安全漏洞详情（P1）

### 问题 1: auth-cloud 配置文件硬编码 SM2 私钥和 SM4 密钥

**模块**: `carlos-auth`  
**文件**: `carlos-integration/carlos-auth/carlos-auth-cloud/src/main/resources/application.yml`  
**行号**: L115-L123, L193  
**严重程度**: P1-High  
**影响范围**: 模块级（auth-cloud 生产配置）

#### 相关代码

```yaml
# carlos-auth/carlos-auth-cloud/src/main/resources/application.yml L115-L123
encrypt:
  sm4:
    enabled: true
    encrypt-mode: cbc
    key: "asdfghjklmnbvcx"            # 密钥（16 字节，不足会自动 MD5 处理）
    iv: "mnbvcxzpoiuytre1"
  sm2:
    enabled: true
    public-key: "0403c7a7250306531238d49c2e0146e48217f2d65b28ed1d61565bd40472c7516bc78574de0a89f9067328558d656ecdc2504f49bb2bd843a9cfa9b404463ed453"
    private-key: "3134665e1e009bab370738fd5ff1dabc7f390b683885c321f1523f4f725e8aa1"

# L193
auth:
  default:
    password: 123456        # 登录密码
```

#### 详细分析

- **问题描述**: SM2 非对称加密私钥和 SM4 对称加密密钥以明文形式硬编码在 application.yml 中。SM2 私钥一旦泄露，所有加密通信可被解密。SM4 密钥硬编码意味着所有使用该密钥加密的数据可被任何能访问代码仓库的人解密。
- **违反规范**: AGENTS.md 安全红线 — "无硬编码凭据"
- **触发条件**: 任何人通过 Git 历史或代码审查可获取私钥；代码仓库泄露 = 加密体系失效
- **潜在风险**: 国密体系形同虚设；中间人可解密通信；密码 `123456` 是默认登录密码，存在暴力破解风险

#### 解决方案

**推荐修复**:

```yaml
# 使用环境变量或外部密钥管理服务
encrypt:
  sm4:
    key: "${SM4_KEY:}"        # 从环境变量注入，无默认值
  sm2:
    private-key: "${SM2_PRIVATE_KEY:}"  # 从环境变量注入
```

或使用 Spring Cloud Config / Nacos 加密配置：

```yaml
encrypt:
  sm4:
    key: "${cipher}{...}"   # 使用 Spring Cloud Config 加密机制
```

**短期规避**: 立即轮换密钥，将 production.yml 加入 `.gitignore`，通过 CI/CD 环境变量注入

**长期建议**: 
1. 引入密钥管理服务（KMS/Vault）管理非对称密钥
2. SM2 私钥绝不出现在代码仓库，仅通过安全通道分发到运行环境
3. 实施密钥轮换策略（定期更换 SM4 对称密钥）

---

### 问题 2: 多处业务模块配置文件硬编码数据库密码

**模块**: `carlos-system` / `carlos-auth` / `carlos-org` / `carlos-audit`  
**文件**:  
- `carlos-system/carlos-system-boot/src/main/resources/application.yml:157` — `password: hadoop543216`  
- `carlos-auth/carlos-auth-boot/src/main/resources/application.yml:140` — `password: hadoop543216`  
- `carlos-org/carlos-org-boot/src/main/resources/application.yml:167` — `password: hadoop543216`  
- `carlos-audit/carlos-audit-boot/src/main/resources/application.yml:34` — `password: Hadoop@2022`  
- `carlos-audit/docs/06-deploy/config/application.yml:34` — `password: Hadoop@2022`  
- `carlos-system/docs/06-deploy/config/application.yml:157` — `password: hadoop543216`  
**严重程度**: P1-High  
**影响范围**: 多模块联动

#### 详细分析

- **问题描述**: 6 处业务模块的 application.yml 中硬编码了真实数据库密码。这些文件随代码仓库同步，任何能访问仓库的人都能获取生产环境数据库密码。
- **违反规范**: AGENTS.md 安全红线 — "无硬编码凭据"
- **触发条件**: Git 仓库访问即可获取密码
- **潜在风险**: 数据库密码泄露 = 数据层完全暴露；部署文档中的密码同样存在泄露风险

#### 解决方案

**推荐修复**:

```yaml
# 统一使用环境变量注入，禁止任何默认值
spring:
  datasource:
    password: "${DB_PASSWORD:}"  # 无默认值，启动时校验
```

**短期规避**: 
1. 立即轮换所有硬编码密码
2. 将含密码的 yml 文件加入 `.gitignore`
3. 清理 Git 历史（`git filter-repo` 或 BFG Repo-Cleaner）移除已提交的密码

**长期建议**: 
1. 所有 boot/cloud 模块的 application.yml 密码统一通过 `${}` 环境变量注入
2. 在 CI/CD 流水线中注入密码，禁止在仓库任何位置出现真实凭据
3. 部署文档模板中仅保留占位符（如 `password: ${DB_PASSWORD}`）

---

### 问题 3: Nacos 配置中心使用默认/弱密码

**模块**: `carlos-system` / `carlos-auth`  
**文件**:  
- `carlos-system/carlos-system-cloud/src/main/resources/bootstrap.yml:37` — `${NACOS_PASSWORD:carlos@123456}`  
- `carlos-system/carlos-system-cloud/src/main/resources/bootstrap.yml:57` — `${NACOS_PASSWORD:nacos}`  
- `carlos-auth/carlos-auth-cloud/src/main/resources/application.yml:57` — `${NACOS_PASSWORD:nacos}`  
**严重程度**: P1-High  
**影响范围**: 模块级

#### 详细分析

- **问题描述**: Nacos 配置中心使用弱密码 `nacos` 或 `carlos@123456` 作为默认值。虽然使用了 `${}` 占位符语法，但默认值的存在意味着如果环境变量未设置，将回退到弱密码。
- **违反规范**: AGENTS.md 安全红线 — "无硬编码凭据"（默认值也属于硬编码）
- **触发条件**: 环境变量 `NACOS_PASSWORD` 未设置时自动回退到默认弱密码
- **潜在风险**: 配置中心密码泄露 = 所有配置（包括数据库密码、API 密钥）可被读取和篡改

#### 解决方案

**推荐修复**:

```yaml
# 禁止默认值，启动失败优于使用弱密码
spring:
  cloud:
    nacos:
      config:
        password: "${NACOS_PASSWORD}"  # 无默认值
      discovery:
        password: "${NACOS_PASSWORD}"  # 无默认值
```

**短期规避**: 立即修改 Nacos 密码为强密码，确保所有环境都通过环境变量注入

---

## 三、代码规范违规详情（P1）

### 问题 4: SysDictMapper.xml 手写 SQL 使用 SELECT *

**模块**: `carlos-system`  
**文件**: `carlos-integration/carlos-system/carlos-system-bus/src/main/resources/mapper/dict/SysDictMapper.xml`  
**行号**: L15  
**严重程度**: P1-High  
**影响范围**: 类级

#### 相关代码

```xml
<!-- carlos-system/carlos-system-bus/src/main/resources/mapper/dict/SysDictMapper.xml L15-L33 -->
<select id="selectOwnPage" resultMap="baseMap">
    SELECT *
    FROM sys_dict
    <where>
      <if test="param.dictName != null and param.dictName != ''">
        AND dict_name LIKE CONCAT('%', #{param.dictName}, '%')
      </if>
      ...
    </where>
    ORDER BY id DESC
</select>
```

#### 详细分析

- **问题描述**: 手写 SQL 使用了 `SELECT *`，违反了框架"严禁直接写 SQL"的强制规范。虽然该查询使用了 `#{param.xxx}` 参数绑定（无 SQL 注入风险），但 `SELECT *` 存在性能问题：返回不必要的列、增加网络传输量、影响查询缓存效率。resultMap 中只映射了 7 个字段，但 `SELECT *` 会返回表中所有列。
- **违反规范**: AGENTS.md / SKILL.md — "数据查询: MyBatis-Plus + mybatis-plus-join 在 Manager 层，严禁直接写 SQL"
- **触发条件**: 每次调用 `selectOwnPage` 时
- **潜在风险**: 性能下降（不必要列传输）、维护困难（表结构变更后 resultMap 与实际返回列不匹配）、违反团队规范

#### 解决方案

**推荐修复**:

```xml
<select id="selectOwnPage" resultMap="baseMap">
    SELECT id, dict_name, dict_code, description, type, create_time, update_time
    FROM sys_dict
    <where>
      <if test="param.dictName != null and param.dictName != ''">
        AND dict_name LIKE CONCAT('%', #{param.dictName}, '%')
      </if>
      ...
    </where>
    ORDER BY id DESC
</select>
```

**长期建议**: 评估是否可用 MyBatis-Plus QueryWrapper 替代该手写 XML，彻底消除手写 SQL

---

## 四、性能风险详情（P2）

### 问题 5: message 同步发送无渠道级限流控制

**模块**: `carlos-message`  
**文件**: `carlos-integration/carlos-message/carlos-message-bus/src/main/java/com/carlos/message/service/MessageSendService.java`  
**行号**: L81-L150（send 方法）  
**严重程度**: P2-Medium  
**影响范围**: 类级

#### 相关代码

```java
// MessageSendService.java L130-L150
for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
    for (String channelCode : channelCodes) {
        // ... 保存接收人记录
        
        // 调用渠道发送
        ChannelAdapter adapter = ChannelFactory.getChannel(channelCode);
        // 检查支持性...
        
        // 直接发送，无速率限制
        SendResult sendResult = adapter.send(context);
        // ... 处理结果
    }
}
```

#### 详细分析

- **问题描述**: `MessageChannel` 实体已定义 `rateLimitQps` 和 `rateLimitBurst` 字段（数据库和 DTO 都有），但 `MessageSendService.send()` 方法在循环内逐条调用渠道发送时，**完全没有使用这些限流参数**。如果一次性向大量接收人发送消息（如 1000 人），且通过短信渠道（如阿里云短信有 QPS 限制），可能触发渠道方的频率限制导致发送失败，甚至被封号。
- **触发条件**: 大批量消息发送 + 高频率渠道（如短信、微信推送）
- **潜在风险**: 渠道方封号、消息大量失败、API 调用费用激增

#### 解决方案

**推荐修复**:

```java
// 在 ChannelAdapter 或发送循环中加入限流
private final Map<String, RateLimiter> channelRateLimiters = new ConcurrentHashMap<>();

private RateLimiter getRateLimiter(String channelCode, MessageChannelDTO channel) {
    return channelRateLimiters.computeIfAbsent(channelCode, k -> {
        double qps = channel.getRateLimitQps() != null ? channel.getRateLimitQps() : 10;
        return RateLimiter.create(qps); // Guava RateLimiter
    });
}

// 发送前限流
RateLimiter limiter = getRateLimiter(channelCode, channelConfig);
limiter.acquire(); // 阻塞等待获取许可
SendResult sendResult = adapter.send(context);
```

或使用 Bucket4j 令牌桶算法实现更精细的限流（支持 burst）。

**短期规避**: 在渠道适配器层（如 `SmsChannelAdapter`）自行实现限流

---

## 五、功能未完善详情（P2/P3）

### 问题 6: audit ClickHouse 查询功能 4 处 TODO 未实现（P2）

**模块**: `carlos-audit`  
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/service/AuditLogQueryService.java`  
**行号**: L48, L69, L83, L117  
**严重程度**: P2-Medium  
**影响范围**: 模块级

#### 相关代码

```java
// AuditLogQueryService.java
public PageInfo<AuditLogMainDTO> queryPage(AuditLogQueryParam param) {
    // TODO: 实现 ClickHouse 查询
    return new PageInfo<>();
}

public List<AuditLogMainDTO> queryList(AuditLogQueryParam param) {
    // TODO: 实现查询
    return Collections.emptyList();
}

public Map<String, Object> queryStatistics(AuditLogQueryParam param) {
    // TODO: 从物化视图查询统计
    return Collections.emptyMap();
}

public AuditLogMainDTO queryDetail(Long id) {
    // TODO: 实现查询
    return null;
}
```

#### 详细分析

- **问题描述**: 审计日志查询服务所有核心方法均为空实现（返回空集合或 null），意味着审计日志写入后无法通过 API 查询。这是核心功能缺失。
- **触发条件**: 调用审计日志查询 API 时
- **潜在风险**: 审计日志系统无法提供查询能力，等于"只进不出"

#### 解决方案

**推荐修复**: 实现 ClickHouse 查询逻辑（使用 ClickHouseClient 原生查询或 JDBC）

---

### 问题 7: system 地区管理 FIXME 遗留（P3 x3）

**模块**: `carlos-system`  
**文件**: `carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/manager/impl/SysRegionManagerImpl.java`  
**行号**: L417, L423  
**文件**: `carlos-system/carlos-system-bus/src/main/java/com/carlos/system/region/service/SysRegionService.java`  
**行号**: L356  
**严重程度**: P3-Low  
**影响范围**: 类级

#### 相关代码

```java
// SysRegionManagerImpl.java L417
// FIXME: Carlos 2025-12-05 是否需要考虑缓存失效的情况

// SysRegionManagerImpl.java L423
// FIXME: Carlos 2025-12-08 当前只考虑更改基本信息，不更改层级等内容

// SysRegionService.java L356
// FIXME: Carlos 2025-12-08 方式优化
```

#### 详细分析

- **问题描述**: 地区管理模块有 3 处 FIXME 遗留，涉及缓存失效策略和层级变更处理，时间标记为 2025 年 12 月，距今已约 5 个月未处理。
- **触发条件**: 地区数据更新时
- **潜在风险**: 缓存与数据库不一致；地区层级变更可能导致数据错乱

#### 解决方案

**推荐修复**: 
1. 补充缓存失效逻辑（更新后清除 Redis 缓存）
2. 实现地区层级变更的完整处理（递归更新子地区）
3. 评估当前 FIXME 标记的代码是否已在后续提交中修复

---

### 问题 8: tools GitLab GUI 大量 TODO 遗留（P3）

**模块**: `carlos-tools`  
**文件**: `carlos-tools/src/main/java/com/carlos/fx/gitlab/controller/*.java`  
**严重程度**: P3-Low  
**影响范围**: 模块级

#### 详细分析

- **问题描述**: `carlos-tools` 是桌面 GUI 工具集（JavaFX），包含 GitLab 管理、代码生成器等功能，存在约 15 处 TODO 标记（从配置文件加载、对话框实现、导出功能等）。这是辅助开发工具，不影响生产系统。
- **潜在风险**: 低 — 仅影响开发者体验

---

## 六、技术债务详情

### 6.1 BeanUtils.copyProperties 使用（P3 x4）

| 文件 | 行号 | 上下文 | 建议 |
|----|----|------|------|
| `audit/clickhouse/ClickHouseBatchWriter.java` | L254 | BeanUtils.copyProperties(log, row) | 使用 MapStruct 或手动字段映射 |
| `message/apiimpl/MessageSendApiImpl.java` | L46 | BeanUtils.copyProperties(param, createParam) | 使用 MapStruct 转换 |
| `message/apiimpl/MessageSendApiImpl.java` | L64 | BeanUtils.copyProperties(param, createParam) | 使用 MapStruct 转换 |
| `message/apiimpl/MessageSendApiImpl.java` | L84 | BeanUtils.copyProperties(param, createParam) | 使用 MapStruct 转换 |

**分析**: 仅 4 处使用，相比大型项目来说控制得非常好。ClickHouseBatchWriter 中用于审计日志字段复制，MessageSendApiImpl 中用于参数转换。建议逐步替换为 MapStruct。

### 6.2 org 模块 fallback 位置不规范（P3）

**问题**: `carlos-org-bus` 模块下存在 fallback 类（如 `ApiOrgPositionHistoryFallbackFactory`），按规范应在 `carlos-org-api` 模块中。

**建议**: 将 `carlos-org-bus/src/main/java/com/carlos/org/position/api/fallback/` 下的 fallback 类迁移到 `carlos-org-api` 模块。

### 6.3 auth-cloud 部署配置暴露敏感信息（P3）

**文件**: `carlos-auth/carlos-auth-cloud/src/main/resources/application.yml`  
**问题**: 该文件包含 SM2 公钥/私钥、SM4 密钥、默认密码等敏感配置，且作为 cloud 模块的配置文件，可能被错误地打包到生产环境。  
**建议**: cloud 模块的 application.yml 应仅包含非敏感的服务注册/发现配置，所有加密密钥通过 Nacos/Config Server 注入。

---

## 七、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|---------|---------|---------|---------|---------|---------|
| carlos-auth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | 4.4/5.0 |
| carlos-gateway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-audit | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | 4.6/5.0 |
| carlos-message | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| carlos-org | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-system | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.6/5.0 |
| carlos-license | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-tools | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | 4.4/5.0 |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

**评分说明**:
- **carlos-auth** 安全扣 2 星（硬编码密钥/密码）
- **carlos-audit** 功能扣 1 星（ClickHouse 查询未实现）
- **carlos-system** 规范扣 1 星（手写 SELECT *）
- **carlos-message** 性能扣 1 星（无渠道限流）
- **carlos-tools** 需求/功能扣 1-2 星（大量 TODO）

---

## 八、待办事项与修复计划

### 8.1 今日新增待办

- [ ] **[P1] carlos-auth**: 从配置文件中移除硬编码 SM2 私钥、SM4 密钥、明文密码，改用环境变量注入
- [ ] **[P1] carlos-system/auth/org/audit**: 移除所有 boot 模块 application.yml 中的硬编码数据库密码
- [ ] **[P1] carlos-system/auth**: 移除 Nacos 配置中的默认弱密码（nacos / carlos@123456）
- [ ] **[P1] carlos-system**: 修复 SysDictMapper.xml 中 SELECT * 为显式列名
- [ ] **[P2] carlos-audit**: 实现 AuditLogQueryService 中的 4 处 ClickHouse 查询 TODO
- [ ] **[P2] carlos-message**: 在 MessageSendService 中实现基于渠道 rateLimitQps/Burst 的限流控制
- [ ] **[P3] carlos-system**: 清理 SysRegionManagerImpl 和 SysRegionService 中的 3 处 FIXME
- [ ] **[P3] carlos-org**: 将 bus 模块中的 fallback 类迁移到 api 模块
- [ ] **[P3] carlos-audit/tools**: 清理历史 TODO（排期优化）

### 8.2 历史遗留跟踪

- [ ] **[P1] carlos-auth**: 硬编码凭据问题 ⏳ 新增（今日发现）
- [ ] **[P1] carlos-system**: SELECT * 问题 ⏳ 新增（今日发现）
- [ ] **[P2] carlos-audit**: ClickHouse 查询未实现 ⏳ 历史遗留（2025 年 TODO 标记）

---

## 九、正面发现（值得保持）

### 9.1 安全架构

| 项目 | 状态 | 说明 |
|-----|------|------|
| 密码编码器 | ✅ BCrypt | `OAuth2PasswordEncoderConfig` 使用 BCrypt，无 MD5/SHA1 |
| 暴力破解防护 | ✅ 完整 | `LoginAttemptManager` 实现 Redis 计数 + 账号锁定 + 数据库同步 |
| WAF 过滤器 | ✅ 6 重防护 | SQL 注入 / XSS / 命令注入 / 路径遍历 / 敏感文件 / CSRF |
| Gateway OAuth2 | ✅ 设计优秀 | Token 校验 + 白名单 + 用户上下文注入 + 过滤器顺序合理 |
| 防重放攻击 | ✅ 存在 | `ReplayProtectionFilter` 已注册 |

### 9.2 代码规范

| 项目 | 状态 | 统计 |
|-----|------|------|
| @Value 使用 | ✅ 无违规 | 业务模块中未发现 @Value |
| 模块依赖方向 | ✅ 合规 | api 不依赖 bus，cloud 依赖 api |
| BeanUtils 泛滥 | ✅ 控制极好 | 仅 4 处使用 |
| 枚举规范 | ✅ 执行良好 | 60 处实现 BaseEnum |
| @Valid 校验 | ✅ 覆盖广泛 | 102 处使用 |
| license-generate 生产误用 | ✅ 安全 | 无其他模块依赖 |

### 9.3 架构设计

| 项目 | 状态 | 说明 |
|-----|------|------|
| 文档结构 | ✅ 完整 | 全部 8 个模块有 README.md + docs/ 目录 |
| Feign Fallback | ✅ 基本覆盖 | 各业务模块都有 FallbackFactory |
| ClickHouse 批量写入 | ✅ 优秀 | 双缓冲 + 虚拟线程 + 本地磁盘备份 |
| 消息异步发送 | ✅ 设计合理 | `@Async("messageTaskExecutor")` 避免自调用代理失效 |
| 审计 Disruptor | ✅ 设计合理 | `AuditLogEventHandler` 处理 + 风险评分模型 |
| 组织缓存初始化 | ✅ 设计合理 | `CacheInitRunnerWorker` 启动预热 + Ordered 控制顺序 |

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
