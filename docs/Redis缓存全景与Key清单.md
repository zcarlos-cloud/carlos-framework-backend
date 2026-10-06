# Carlos Framework 缓存全景与 Redis Key 清单

> 生成日期：2026-10-05
> 取证方式：全仓库静态扫描（`grep` 常量 + 调用点）+ 线上 Redis db0 实扫（`SCAN *`，95 键）双向核对。
> 适用范围：`carlos-framework-backend` 全部模块（含 `carlos-spring-boot-starter-*` 框架层）。

---

## 0. 结论速览

- 本框架**不使用 Spring 的 `@Cacheable` 注解缓存**。2026-10-04 起连 `CacheManager` / `PrefixKeySerializer` / `spring-boot-starter-cache` 依赖都已整体删除，全仓库 `import org.springframework.cache` 零命中。
- 缓存由**三档载体**构成：Redis（主）、Redisson（分布式对象/锁/队列）、JVM 本地内存（Caffeine / Guava / `ConcurrentHashMap`）。
- 所有 Redis key 遵循 **`{模块}:{业务}:{标识}`**，模块根**白名单 8 个**：`auth` / `system` / `org` / `message` / `monitor` / `audit` / `gateway` / `framework`。**新 key 的第一段不在表内即视为违规**（规范见 `.claude/skills/carlos-framework-standard/SKILL.md`）。
- 线上 db0 顶层实际只出现 **6 个根**：`auth` / `system` / `org` / `monitor` / `framework`，另加 Redisson 自生成的 `{framework:ratelimit:…}` 带括号键（属 `framework` 根）。

---

## 1. 缓存分层与统一入口

| 档位 | 载体 | 入口类 | 说明 |
|---|---|---|---|
| L2 主存储（同步） | Redis | `com.carlos.redis.util.RedisUtil` | **业务代码唯一允许调用的 Redis 入口**，只有 Manager 层可用 |
| L2 主存储（响应式） | Redis | `com.carlos.redis.util.ReactiveRedisUtil` | 仅供 `carlos-gateway`（WebFlux 栈） |
| 分布式对象 / 锁 / 队列 / 限流 | Redis（Redisson 协议） | `RedissonClient` | ⚠️ **不走 Spring 的 key 序列化器**，key 直传 |
| L1 本地缓存 | Caffeine | `com.carlos.redis.caffeine.CaffeineUtil`（静态持有框架级 bean） | 单节点内存，无跨实例失效 |
| L0 进程内 | `ConcurrentHashMap` / Guava `Cache` | 各组件自持 | 枚举字典、去重窗口等 |
| 多级缓存 | Caffeine(L1) + Redisson `RLocalCachedMap`(L2) | `com.carlos.redis.multilevel.MultiLevelCacheUtil` | Redisson 内建 Pub/Sub 失效同步 |

**强制约束**（CLAUDE.md / SKILL.md）：Redis 只能在 **Manager 层**操作；Service 层禁止直接引 `RedisTemplate`。审计模块的 ClickHouse 是既定例外（非缓存）。

底层模板（`RedisCacheConfig`）：

- `@Primary RedisTemplate<String, Object>`：**key 用 `StringRedisSerializer`，value 用 `ConfigurableRedisSerializer`**（由 `carlos.cache.serializer` 决定：`jackson`(默认) / `fastjson` / `kryo` / `jdk`）。
- 同时导出 `hashOperations` / `valueOperations` / `listOperations` / `setOperations` / `zSetOperations` 五个 `opsForXxx()` bean 供注入。

---

## 2. 模块根白名单

| 根 | 归属模块 | 承载什么 |
|---|---|---|
| `auth` | carlos-auth + starter-security + gateway（共用） | OAuth2 授权、登录安全、验证码、限流、会话、权限集合、token 内省/黑名单 |
| `system` | carlos-system | 字典、区域、序列号计数、i18n |
| `org` | carlos-org | 部门树各视图、初始化锁 |
| `message` | carlos-message | 消息投递、限流、防刷指纹、重试、回执 |
| `monitor` | carlos-monitor | 实例、指标快照、慢日志游标、告警防抖、服务注册 |
| `audit` | carlos-audit | 仅 MQ 主题名（物理键落在 `framework:` 下） |
| `gateway` | carlos-gateway | 防重放 nonce、响应缓存 |
| `framework` | `carlos-spring-boot-starter-*` | 雪花 workerId、MQ Stream/延时、限流、锁、多级缓存、数据权限、翻译、AI 向量索引 |

> 跨模块共用的 key 常量统一放在 `carlos-spring-boot-core` 的 `com.carlos.core.constant.RedisKeys`
> （因为 carlos-gateway 依赖 core 但**不依赖** carlos-auth）。模块私有 key 留在各自常量类里。

---

## 3. 业务模块 Key 清单

### 3.1 `auth` — carlos-auth

常量总闸：`carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/constant/AuthRedisKey.java`

#### OAuth2 授权（`RedisOAuth2AuthorizationService` / `RedisOAuth2AuthorizationConsentService` / `BackChannelLogoutController`）

| Key 格式 | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `auth:oauth2:id:{authorizationId}` | String(JSON) | 授权主体主索引 | `AuthRedisKey:48`、`RedisOAuth2AuthorizationService.java:111,288` |
| `auth:oauth2:state:{state}` | String(JSON) | 授权请求暂存 | `RedisOAuth2AuthorizationService.java:111` |
| `auth:oauth2:code:{code}` | String | 授权码索引 | `RedisOAuth2AuthorizationService.java:120` |
| `auth:oauth2:access_token:{token}` | String | access token 索引 | `RedisOAuth2AuthorizationService.java:130` |
| `auth:oauth2:refresh_token:{token}` | String | refresh token 索引 | `RedisOAuth2AuthorizationService.java:142` |
| `auth:oauth2:session_meta:{authorizationId}` | String(JSON `{ip,userAgent}`) | 在线用户会话元数据（S4） | `AuthRedisKey:48`、`RedisOAuth2AuthorizationService.java:334` |
| `auth:oauth2:user_sessions:{username}` | Set | 用户 → 会话索引（在线用户枚举） | `AuthRedisKey:55,215`、`OnlineUserManager.java:75` |
| `auth:oauth2:user_sessions:*` | 扫描 pattern | 在线用户模糊/全量扫描 | `AuthRedisKey:208,229`、`OnlineUserManager.java:49` |
| `auth:oauth2:consent:{clientId}:{principalName}` | String | 授权同意记录 | `AuthRedisKey:42`、`RedisOAuth2AuthorizationConsentService.java:65` |
| `auth:oauth2:client:{id}:{appKey}` | String | OAuth2 客户端缓存，**TTL 1 天** | `AuthRedisKey:41`、`AppClientManagerImpl.java:45,212-217` |

#### 登录安全（`LoginAttemptManager` / `IpBlockManager`）

| Key 格式 | 用途 | 代码位置 |
|---|---|---|
| `auth:attempt:user:{username}` | 用户名失败计数（`INCR`，TTL 24h） | `AuthRedisKey:26`、`LoginAttemptManager.java:56,88,219` |
| `auth:lock:user:{username}` | 用户名锁定（值 `"LOCKED"`） | `AuthRedisKey:25`、`LoginAttemptManager.java:57,139` |
| `auth:attempt:ip:{ip}` | IP 失败计数 | `AuthRedisKey:28`、`IpBlockManager.java:69,115,216` |
| `auth:lock:ip:{ip}` | IP 封禁 | `AuthRedisKey:27`、`IpBlockManager.java:70,116,156` |

#### 验证码 / 密码 / MFA / 扫码 / Passkey / 社交

| Key 格式 | 用途 | 代码位置 |
|---|---|---|
| `auth:captcha:{type}:{target}` | 验证码值（`type` = `sms`/`email`…，TTL 300s） | `AuthRedisKey:20`、`CaptchaManager.java:107` |
| `auth:captcha:limit:{type}:{target}` | 验证码发送次数限制（TTL 1 天） | `AuthRedisKey:22`、`CaptchaManager.java:111` |
| `auth:password:key:{keyId}` | 登录密码 SM2 私钥暂存，**TTL 300s** | `AuthRedisKey:45`、`PublicKeyController.java:70,101` |
| `auth:mfa:token:{mfaToken}` | MFA 挑战 token | `MfaService.java:44,170,194` |
| `auth:qrcode:{qrcodeId}` | 扫码登录会话 | `QrCodeService.java:43,194,212` |
| `auth:passkey:challenge:{challengeB64}` | WebAuthn 认证挑战 | `AuthRedisKey:66`、`PasskeyService.java:213,231` |
| `auth:passkey:registration:{challengeB64}` | WebAuthn 注册挑战 | `AuthRedisKey:67`、`PasskeyService.java:104,119` |
| `auth:social:state:{state}` | 第三方登录 state（CSRF 防伪） | `AuthRedisKey:70`、`SocialCallbackController.java:103,137` |
| `auth:common:locations:{userId}` | 常用登录地点（List，**TTL 90 天**） | `AuthRedisKey:35`、`IpLocationUtil.java:158-166` |

#### 应用门户缓存（`App*ManagerImpl`）

| Key 格式 | 用途 | TTL | 代码位置 |
|---|---|---|---|
| `auth:app:category:all` | 应用分类全量 | 30 min | `AppCategoryManagerImpl.java:30` |
| `auth:app:portal:apps:visible` | 门户可见应用 | 5 min | `AppPortalManagerImpl.java:29` |
| `auth:app:role-access:{appId}` | 应用角色访问关系 | 10 min | `AppRoleAccessManagerImpl.java:30,58,100` |

#### 权限与令牌（跨模块）

| Key / Channel | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `auth:permissions:{roleCode}` | Set | 角色→权限集合。**写方 starter-security 的 `RedisPermissionProvider` 按 roleCode；读方 gateway 的 `DefaultPermissionProvider` 按 userId**（语义冲突，见 §8） | `RedisKeys.AUTH_PERMISSIONS`、`RedisPermissionProvider.java:100,176,202,232`、`DefaultPermissionProvider.java:31` |
| `auth:permission:change` | **Pub/Sub 频道**（非 key） | 权限变更广播，跨节点清本地缓存 | `RedisKeys.AUTH_PERMISSION_CHANGE_CHANNEL`、`PermissionCacheSyncManager.java:65,113,125`、`ResourceServerAutoConfiguration.java:146` |
| `auth:token:introspection:{sha256(token)}` | String | 网关不透明 token 内省结果。**写 carlos-auth（踢下线时删）/ 读 carlos-gateway** | `RedisKeys.AUTH_TOKEN_INTROSPECTION`、`AuthRedisKey:57`、`OpaqueTokenValidator.java:62` |
| `auth:token:blacklist:{jti}` | String | JWT 吊销黑名单。**⚠️ 只有读方（`JwtTokenValidator.java:96`），无写入方** | `JwtTokenValidator.java:96` |

> 线上 db0 实测存在 `auth:permissions:SUPER_ADMIN`、`auth:permissions:ORG_VIEWER`（均为 roleCode 形态）。

#### 登录/SSO 限流（经 `RateLimitUtil` → **实际键带 `framework:ratelimit:` 前缀**）

| 业务 key（`AuthRedisKey`） | 完整 Redis 键 | 限流参数 | 代码位置 |
|---|---|---|---|
| `auth:rate:ip:{ip}` | `framework:ratelimit:auth:rate:ip:{ip}` | 30/分钟 | `UserAuthController.java:104,213` |
| `auth:rate:login:{username}` | `framework:ratelimit:auth:rate:login:{username}` | 5/分钟 | `UserAuthController.java:168` |
| `auth:rate:captcha:sms:{phone}` | `framework:ratelimit:…` | 3/分钟 | `CaptchaController.java:55,101` |
| `auth:rate:captcha:email:{email}` | `framework:ratelimit:…` | 3/分钟 | `CaptchaController.java:55,101` |
| `auth:sso:exchange:{ip}` | `framework:ratelimit:auth:sso:exchange:{ip}` | 20/分钟 | `SsoController.java:70` |
| `auth:sso:logout:{token}` | `framework:ratelimit:auth:sso:logout:{token}` | 10/小时 | `SsoController.java:119` |

### 3.2 `system` — carlos-system

| Key 格式 | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `system:dict:dict_item:{dictCode}` | Set | 字典编码 → 选项 ID 集合 | `SysDictCacheManager.java:64` |
| `system:dict:item:{itemId}` | Hash | 单个字典选项明细 | `SysDictCacheManager.java:70` |
| `system:region:hash:{code}` | Hash | 区域自身字段 | `SysRegionManagerImpl.java:55` |
| `system:region:code:{...}` | String | 编码 → ID | `SysRegionManagerImpl.java` |
| `system:region:children:{id}` | Set | 直接子区域 | `SysRegionManagerImpl.java:63` |
| `system:region:desc:{id}` | Set | 全部后代 | `SysRegionManagerImpl.java:68` |
| `system:region:ancestors:{id}` | List | 祖先链 | `SysRegionManagerImpl.java:74` |
| `system:sequence:{ruleCode}[:{period}]` | String(计数) | 序列号原子计数（真相源；DB `current_value` 仅兜底） | `SequenceCodeFormatter.java:33,97` |
| `system:sequence:{ruleCode}:*` | 扫描 pattern | 删规则时清计数键 | `SequenceCodeFormatter.java:112` |
| `system:i18n:messages:{lang}` | String | 语言包全量 | `SysI18nController.java:179` |
| `system:i18n:messages:{lang}:{moduleCodes}` | String | 按模块裁剪的语言包 | `SysI18nController.java:181` |

> `SysDictCacheManager` 使用的是 Redisson `RLocalCachedMap`（key = `system:dict:*` 命名空间下的成员），
> 因此字典命中可**完全不走 Redis**（本地内存直答），写操作由 Redisson 的 `SyncStrategy.INVALIDATE` 跨实例失效。
> 清空走 `RedisUtil.deleteSpace("system:dict:")`（`SysDictCacheManager.java:129`）。

### 3.3 `org` — carlos-org

| Key 格式 | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `org:dept:hash:{id}` | Hash | 部门自身字段 | `OrgDepartmentManagerImpl.java:69` |
| `org:dept:code:{code}` | String | 编码 → 部门 | `OrgDepartmentManagerImpl.java:72` |
| `org:dept:children:{id}` | Set | 直接子部门 | `OrgDepartmentManagerImpl.java:75` |
| `org:dept:desc:{id}` | Set | 全部后代 | `OrgDepartmentManagerImpl.java:78` |
| `org:dept:ancestors:{id}` | List | 祖先链（根部门为 `0`） | `OrgDepartmentManagerImpl.java:81` |
| `org:lock:init:superadmin` | Redisson `RLock` | 超管初始化互斥锁 | `OrgInitService.java:46` |

> 线上 db0 实测存在 `org:dept:code:HQ`、`org:dept:hash:{id}`、`org:dept:children:0`、
> `org:dept:ancestors:{id}`、`org:dept:desc:HQ` —— 与代码完全对齐。

### 3.4 `message` — carlos-message

| Key 格式 | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `message:{channelCode}` | **MQ topic 名**（最终落 `framework:mq:stream:message:{channelCode}`） | 一渠道一主题 | `RedisStreamMessageQueue.java:62,145` |
| `message:ratelimit:{channelCode}` | String(滑动窗口) | 渠道级滑动窗口限流 | `RedisSlidingWindowRateLimiter.java:40,80` |
| `message:rate:{receiverId}:{channelCode\|all}` | String | 接收者防轰炸限流 | `MessageAntiSpamService.java:34,59` |
| `message:fingerprint:{fingerprint}` | String | 内容指纹去重 | `MessageAntiSpamService.java:35,81` |
| `message:retry:zset` | ZSet | 重试调度（score = 下次重试时间） | `MessageRetryTask.java:51` |
| `message:record:{messageId}` | String | 发送回执 | `MessageRecordManagerImpl.java:165,212` |

### 3.5 `monitor` — carlos-monitor

常量总闸：`carlos-integration/carlos-monitor/carlos-monitor-bus/src/main/java/com/carlos/monitor/constant/CacheKeyConstant.java`

| Key 格式 | 数据形态 | TTL | 用途 | 代码位置 |
|---|---|---|---|---|
| `monitor:instance:{id}` | String(JSON) | 300s | 实例详情 | `CacheKeyConstant.java:36`、`MonitorInstanceManagerImpl.java:59,159` |
| `monitor:instance:page:{version}:{paramHash}` | String(JSON) | 60s | 实例分页 | `CacheKeyConstant.java:47`、`MonitorInstanceManagerImpl.java:64,183` |
| `monitor:instance:page:ver` | String(递增) | — | 分页缓存版本号；写操作递增即整体失效 | `CacheKeyConstant.java:24` |
| `monitor:metric:current:{instanceId}` | Hash | 5 min | 当前指标快照（采集成功回写） | `CacheKeyConstant.java:76` |
| `monitor:collect:slowlog-cursor:{instanceId}:{node}` | String | — | Redis 慢日志采集游标（单机 node 为空串） | `CacheKeyConstant.java:87` |
| `monitor:alarm:prev:{ruleId}:{instanceId}:{metricName}` | String | 3600s | 告警上一周期值（算环比用，首周期无值时记 0） | `AlarmEngine.java:129-131` |
| `monitor:alarm:violation:{ruleId}:{instanceId}` | String(计数) | 3×采集间隔 | 连续违规周期计数（防抖，达到 `durationCount` 才触发） | `AlarmEngine.java:76` |
| `monitor:confirm:{token}` | String | 60s | 高危写操作一次性确认令牌 | `RedisWriteGuard.java:36,64,81` |
| `monitor:logger:ttl:{service}:{address}:{loggerName}` | String | — | 日志级别临时调整的到期时间 | `LoggerLevelManager.java:33,66` |
| `monitor:service:seen:{serviceName}` | String(时间戳) | 3600s | 服务注册心跳 | `ServiceRegistrySyncTask.java:44,90,127` |

> `MonitorCacheUtil`（`com.carlos.monitor.util.MonitorCacheUtil`）是监控模块**专用读取工具**：
> 统一按 `Number`/`String` 宽容解析，规避框架 `RedisTemplate` 把 Long 序列化成字符串导致的
> `ClassCastException`。监控模块所有取值都走它，不直接调 `RedisUtil.getValue`。

### 3.6 `audit` — carlos-audit

| 名称 | 值 | 说明 | 代码位置 |
|---|---|---|---|
| MQ 主题 | `audit:log` | 业务语义主题名，**本身不是 Redis 键** | `AuditMqTopics.java:20` |
| 消费组 | `audit:log:writer` | Stream 消费组名 | `AuditMqTopics.java:23` |
| **物理 Stream key** | `framework:mq:stream:audit:log` | 由 MQ 框架加前缀构成（线上实测存在） | `MqTopics.streamKey()` |

> 审计**没有自己的 Redis 缓存**。跨服务 `@Log` 落 ClickHouse 是靠 MQ 桥接
> （`AuditMqLogStorage` → `audit:log` 主题），本机去重用的是一段 Guava `CacheBuilder` 内存窗口
> （`AuditLogMqConsumer.java:84`，非 Redis）。

### 3.7 `gateway` — carlos-gateway

| Key 格式 | 数据形态 | 用途 | 代码位置 |
|---|---|---|---|
| `gateway:nonce:{nonce}` | String | 请求防重放（同一 nonce 只能出现一次） | `GatewayHeaderConstants.java:50`、`ReplayProtectionFilter.java:200` |
| `gateway:cache:response:{sha256(hash)}` | String(JSON) | 响应缓存（L1 Caffeine + L2 Redis 双写） | `GatewayHeaderConstants.java:57`、`ResponseCacheFilter.java:173,130,231` |
| `request_rate_limiter.{routeId}.tokens` / `.timestamp` | Hash | **Spring Cloud Gateway `RedisRateLimiter` 内建**，网关路由级令牌桶 | `CarlosRedisRateLimiter.java`（继承 SCG，**键名不可改**） |

> 网关的其余缓存全是**进程内 Caffeine**（见 §5），不进 Redis。

---

## 4. 框架层（`carlos-spring-boot-starter-*`）

### 4.1 雪花 ID workerId 注册

| Key 格式 | 数据形态 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:snowflake:{workerId}_{dataCenterId}` | String(SnowflakeInfo) | 实例抢注 workerId；`setIfAbsent` 互斥，**定时续期**，宕机后靠 TTL 释放 | `SnowflakeConstant.java:25`、`SnowflakeRedisCacheManager.java:53` |

- 默认 `namespace = framework:snowflake`；**各服务 yml 里的 `carlos.snowflake.namespace` 覆盖已在 2026-10-03 全部删除**，
  现存 `carlos-audit` / `carlos-monitor` 的 `snowflake:` 块只保留 `tag:`。
- 线上实测键：`framework:snowflake:8_0`。

### 4.2 MQ / Stream / 延时队列

| Key 格式 | 数据形态 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:mq:stream:{topic}` | Redis Stream | 主题物理键 | `RedisStreamProperties.java:27`、`MqTopics.java` |
| `framework:mq:stream:{topic}:dlq` | Redis Stream | 死信队列 | `MqTopics.dlqKey()` |
| `framework:mq:delay:{bucket}` | Redisson `RBlockingQueue`/`RDelayedQueue` | 延时消息桶（`bucket` = `high`/`normal`/`low`） | `RedisDelayedQueueProperties.java:25`、`RedissonDelayedQueue.java` |

### 4.3 分布式限流

| Key 格式 | 数据形态 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:ratelimit:{bizKey}` | Redisson `RRateLimiter` | 令牌桶。`bizKey` 由调用方给出（如 `auth:rate:ip:{ip}`） | `RateLimitProperties.java:28`、`RateLimitUtil.java:374` |
| `{framework:ratelimit:{bizKey}}` | Hash | Redisson `RRateLimiter` 内部键（**带大括号，Redis Cluster 哈希槽用**） | Redisson 生成 |
| `{framework:ratelimit:{bizKey}}:permits` | String | 同上，已发放许可数 | Redisson 生成 |
| `{framework:ratelimit:{bizKey}}:value` | String | 同上，当前令牌值 | Redisson 生成 |

> 线上实测三件套齐全，例如 `framework:ratelimit:auth:rate:login:admin` +
> `{framework:ratelimit:auth:rate:login:admin}:permits` + `{framework:ratelimit:auth:rate:login:admin}:value`。
> **清理限流键时必须连 `{...}:permits` / `{...}:value` 一起删**，否则残留令牌桶会沿用旧配额。

### 4.4 分布式锁

| Key 格式 | 数据形态 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:lock:{name}:{...}` | Redisson `RLock` | `@RedisLock` 注解式锁（当前**零业务使用**） | `RedisLockProperties.java:16`、`RedisLockAspect.java:161-171` |
| 任意（由调用方给全名） | Redisson `RLock` | 编程式 `RedissonLockUtil.getLock(name)`，**不自动加前缀** | `RedissonLockUtil.java:38-40` |

> 注意两者行为不同：注解式 `@RedisLock` 会拼 `framework:lock:` 前缀，编程式 `getLock(name)` 直接使用你传的名字。
> 业务里唯一的锁是 `org:lock:init:superadmin`，走的是**编程式**路径。

### 4.5 多级缓存 / 缓存空间

| Key / 名称 | 数据形态 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:cache:multilevel` | Redisson `RLocalCachedMap` | 多级缓存 L2 空间（L1 为 Caffeine） | `MultiLevelCacheUtil.java:24` |
| `framework:cache:datascope` | Redisson `RLocalCachedMap` | 数据权限 L2 空间 | `RedissonLocalDataScopeCache.java:22` |
| `framework:cache:translation` | Redisson `RLocalCachedMap` | 字段翻译 L2 空间 | `RedissonTranslationCacheManager.java:31` |

### 4.6 数据权限（starter-datascope）

| Key 格式 | 用途 | 代码位置 |
|---|---|---|
| `framework:datascope:{key}` | 通用键前缀 | `RedisDataScopeCache.java:22,31,42,55,64,84` |
| `framework:datascope:rules:config:{mapperMethod}` | 方法级规则配置缓存 | `DefaultRuleEngine.java:81` |
| `framework:datascope:user:subordinates:{userId}` | 下属用户 ID 集合 | `DefaultRuleEngine.java:269` |
| `framework:datascope:dept:children:{deptId}` | 子部门 ID 集合 | `DefaultRuleEngine.java:317` |
| `framework:datascope:region:children:{regionCode}` | 子区域编码集合 | `DefaultRuleEngine.java:413` |

> 后四条是 `DefaultRuleEngine` 传给 `DataScopeCache` 的**逻辑键**，最终会再拼上
> `framework:datascope:` 前缀（`RedisDataScopeCache` 内部 `KEY_PREFIX + key`）。

### 4.7 字段翻译（starter-translation）

`carlos-spring-boot/carlos-spring-boot-starter-translation/src/main/java/com/carlos/boot/translation/cache/CacheKeys.java`

| Key 格式 | 用途 |
|---|---|
| `framework:translation:user:{id}` | 用户翻译 |
| `framework:translation:dict:{type}:{code}` | 字典翻译 |
| `framework:translation:dept:{id}` | 部门翻译 |
| `framework:translation:region:{code}` | 区域翻译 |

### 4.8 其他框架层

| Key / 名称 | 类型 | 说明 | 代码位置 |
|---|---|---|---|
| `framework:ai:embeddings` | RediSearch 索引名 | LangChain4j 向量检索索引 | `AiEmbeddingRedisProperties.java:29` |
| `dingtalk:access_token:{appkey}` | String | 钉钉 access_token 缓存 | `DingtalkConstant.java:13`、`DingtalkAccessTokenManager.java:30` |
| — | Caffeine bean `caffeineCache` | 框架级 L1 缓存 bean（受 `carlos.redis.caffeine.enabled` 控制） | `CaffeineConfig.java:33` |

> ⚠️ `dingtalk:access_token:` 的**第一段不在模块根白名单内**（应为 `framework:` 或新立 `integration` 根），
> 属 2026-10-03 前缀收敛的漏网之鱼 —— 见 §8。

---

## 5. 本地/进程内缓存清单（无 Redis key）

这些是**单节点内存**，多实例部署时各自独立，不共享、不同步（除非另外走 Redisson `RLocalCachedMap`）。

### 5.1 框架层

| 类 | 类型 | 说明 |
|---|---|---|
| `CaffeineConfig` → `caffeineCache` | Caffeine | 框架统一 L1 bean，带 Micrometer 指标（`carlos.caffeine.*`） |
| `CaffeineUtil` | Caffeine 静态持有 | 静态访问框架 L1 bean |
| `MultiLevelCacheUtil` | Caffeine(L1) + Redisson(L2) | 多级缓存，`evictByPrefix` 支持按前缀清 |
| `CachedPermissionProvider` | Caffeine `LoadingCache` | 权限 L1（key = 角色编码），TTL + 最大容量可配 |
| `TranslationCacheManager` | Caffeine | 翻译 L1 |
| `CaffeineDataScopeCache` | Caffeine | 数据权限降级方案（无 Redis 时） |
| `EnumUtil` `ENUM_CACHE` / `CODE_MAPPING_CACHE` | `ConcurrentHashMap` | `@AppEnum` 枚举字典与 code→枚举映射 |

### 5.2 carlos-gateway（全部 Caffeine）

| 类 | 缓存内容 |
|---|---|
| `ResponseCacheFilter` | 响应缓存 L1 |
| `OAuth2AuthorizationFilter` | 用户权限集合（`AsyncLoadingCache`）+ 路径匹配结果 |
| `UnifiedWhitelistFilter` | 白名单匹配结果 |
| `Resilience4jCircuitBreakerFilter` | 熔断器实例表 |
| `GrayReleaseFilter` | 灰度策略 |
| `SelectStripPrefixGatewayFilterFactory` | 前缀匹配结果 |
| `MetricsFilter` | Micrometer Counter/Timer 句柄 |

### 5.3 其他业务模块

| 模块 | 类 | 缓存内容 |
|---|---|---|
| carlos-monitor | `RuleMatcher` | 告警规则（动态刷新） |
| carlos-monitor | `MonitorDataMaskRuleManagerImpl` | 脱敏规则 |
| carlos-monitor | `MonitorInstanceManagerImpl` | 健康状态 / 健康写入时间 |
| carlos-monitor | `InstanceConnectionRegistry` | 托管连接（`LoadingCache`） |
| carlos-monitor | `DbMonitorServiceImpl` / `RedisMetricsManagerImpl` | 上一轮指标快照（算增量用） |
| carlos-system | `SysConfigService` | `HOME_CONFIG` / `ALL_CONFIG` 两个静态 Caffeine |
| carlos-ai | `AiChatServiceImpl` | 会话记忆 `ChatMemory` |
| carlos-audit | `AuditLogMqConsumer` | MQ 去重窗口（Guava `CacheBuilder`） |

---

## 6. 不受本规范管辖的 Key

| Key | 归属 | 为什么不能改 |
|---|---|---|
| `request_rate_limiter.{routeId}.tokens` / `.timestamp` | Spring Cloud Gateway 内建 | 名字硬编码在 SCG `RedisRateLimiter` 里 |
| `{...}:permits` / `{...}:value` | Redisson 内部 | `RRateLimiter` 的伴随键，成对存在 |
| `signature_verify_*` / `TaskDataDealStream` / `dataitem:ai:handler:semaphore` / `report:validity:expire:*` | **共享同一 Redis 实例的另一个应用**（在 db1/db5） | 本仓库无任何代码产出，**绝不可动** |

---

## 7. Redis 实例拓扑（供排障）

- 实例：`100.108.187.89:16379`（**不是默认 6379**），standalone 模式，Redis 8.4.0，`databases: 10`。
- **所有本地服务共用 db0**（Nacos `common.yml` 的 `REDIS_DB: '0'`）。
- db1 / db5：他人应用，勿动。db3：本项目 MQ 手工诊断残留（`redis:mq:diag:*`）。

---

## 8. 已知缺口 / 待收敛

| 项 | 问题 | 建议 |
|---|---|---|
| `auth:permissions:` | **同一前缀两种语义**：starter-security 按 roleCode 写、gateway 按 userId 读；同一 Redis 里两种键混在一起 | 拆成 `auth:permissions:role:{roleCode}` 与 `auth:permissions:user:{userId}` |
| `auth:token:blacklist:{jti}` | **只有读方没有写方**，JWT 吊销功能未实现 ⇒ 该分支永远命中不了 | 补写入方或先删掉这段读逻辑 |
| `dingtalk:access_token:{appkey}` | 首段 `dingtalk` 不在模块根白名单内 | 改为 `framework:integration:dingtalk:access_token:{appkey}` |
| MQ 端到端 | Stream 链路（含 `framework:mq:stream:*`）**从未做过端到端真机验证** | 部署时补一轮全链路验证 |

---

## 9. 排查与新增速查

```bash
# 线上按模块列出顶层根（确认没有非法首段）
redis-cli -h 100.108.187.89 -p 16379 -n 0 --scan --pattern '*' \
  | awk -F: '{print $1}' | sort | uniq -c | sort -rn
```

**新增一个 Redis key 的正确姿势**

1. key 常量写进**本模块的常量类**（如 `AuthRedisKey`、`CacheKeyConstant`）；跨模块共用的才放
   `com.carlos.core.constant.RedisKeys`。
2. 第一段必须是 8 个模块根之一；禁止在业务代码里硬编码 key 字符串。
3. 只允许在 **Manager 层**通过 `RedisUtil` 读写。
4. 清理时用 `RedisUtil.deleteSpace(常量)`（传常量而非字面量，改前缀时自动跟随），
   或在 `*CacheManager` 里维护 `xxxPattern()`。
5. 若用 `RRateLimiter`，记得 `{...}:permits` / `{...}:value` 会一并产生。

**常用排查入口**

| 场景 | 看哪里 |
|---|---|
| 某个 key 是谁写的 | 全局搜常量值（本文档 §3/§4 已给出行号） |
| 改了前缀却扫不到键 | 检查 `deleteSpace()` / `redisKeyPattern` / 测试断言里的**硬编码字符串** |
| 读数报 `ClassCastException` | 框架 value 序列化器把 Long 序列化成了 String；监控模块用 `MonitorCacheUtil` |
| 缓存清了但没生效 | L1 是 Caffeine / `RLocalCachedMap` 的可能性：Caffeine 需重启或走 `evictByPrefix` |
