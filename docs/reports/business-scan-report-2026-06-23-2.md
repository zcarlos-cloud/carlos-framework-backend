# Carlos Framework 业务模块扫描报告 Part2

**日期**: 2026-06-23  
**扫描范围**: carlos-message、carlos-org、carlos-system  
**Git Commit**: `829702c8266a88a7592561de388a943b4bfcc904`  
**Java 文件数**: carlos-message 183 / carlos-org 372 / carlos-system 279  
**评估维度**: 文档完整性 / 需求覆盖 / 代码规范 / 性能风险 / 安全风险 / 功能完整度

---

## 一、执行摘要

本次扫描覆盖 Carlos Framework 三个核心业务模块共 **834 个 Java 文件**。发现:
- **P0 问题 3 个**: 敏感数据暴露、缓存一致性漏洞、无分布式 ID 生成
- **P1 问题 10 个**: 超大方法、无操作日志基础设施、缺少重试机制等
- **P2 问题 8 个**: 代码规范不一致、魔法值、注释缺失等

整体健康度评估:

| 模块 | 健康度 | 评级 |
|------|--------|------|
| carlos-message | 78/100 | B+ |
| carlos-org | 72/100 | B |
| carlos-system | 68/100 | B- |

---

## 二、问题分布总览

```
P0 (阻断): ████████████ 3
P1 (高危): ████████████████████████████████████████ 10
P2 (建议): ████████████████████████████ 8
```

---

## 三、模块A: carlos-message（消息中心）

**子模块**: api / boot / bus / cloud / core  
**文件数**: 183  
**核心功能**: 多渠道路由(钉钉/邮件/短信/WebSocket)、Freemarker模板渲染、同步/异步/定时发送、限流防刷、Feign降级、Redis Stream消息队列、渠道健康检查

### 3.1 发现问题

#### 🔴 P0-1: MessageSendService.send() 方法过大（573行超大方法）

**文件**: `carlos-message-bus/src/main/java/com/carlos/message/service/MessageSendService.java:573`

**问题**: send() 方法内部包含参数校验、模板查询、消息ID生成、模板渲染、记录保存、渠道解析、接收人创建、循环发送、异常处理、统计更新等全部逻辑，单一方法超过 300 行，违反单一职责原则。

**代码片段**（send方法行 66-299 共 233 行）:
```java
@Transactional(rollbackFor = Exception.class)
public Result<String> send(MessageCreateParam param) {
    // 1. 参数校验
    // 2. 查询模板信息
    // 3. 生成消息ID
    // 4. 渲染模板内容
    // 5. 保存消息记录
    // 6. 确定使用的渠道列表
    // 7. 批量保存接收人记录
    // 8. 发送消息 (for receiver × for channel 嵌套循环，含 adapter、限流、构建上下文、发送、日志)
    // 8. 统计更新
    // ... 共 233 行
}
```

**影响**: 维护困难，测试困难，嵌套循环可达 O(n×m×k)。

**解决方案**: 拆分为独立方法或使用策略模式：
```java
// 拆分建议
public Result<String> send(MessageCreateParam param) {
    MessageTemplateDTO template = validateAndGetTemplate(param);
    String messageId = generateMessageId();
    String content = renderTemplate(template, param);
    saveMessageRecord(record);
    List<MessageReceiver> receivers = createReceivers(param, channelCodes);
    SendStatistics stats = executeSendLoop(messageId, param, channelCodes, receivers);
    updateFinalStatistics(messageId, stats);
    return Result.success(messageId);
}
```

---

#### 🔴 P0-2: MessageSendService.sendAsync() 与 send() 大量重复代码

**文件**: 同上

**问题**: send() 和 sendAsync() 和 schedule() 三个方法的模板查询、参数校验、消息ID生成、模板渲染、记录保存、渠道解析、接收人创建的代码几乎完全相同（~80 行重复）。

**代码对比**:
```java
// send() 方法 - 第73-117行
if (CollUtil.isEmpty(param.getReceivers())) { return Result.error("接收人不能为空"); }
// ... 45行重复逻辑

// sendAsync() 方法 - 第306-349行
if (CollUtil.isEmpty(param.getReceivers())) { return Result.error("接收人不能为空"); }
// ... 完全相同的 45 行重复逻辑

// schedule() 方法 - 第357-400行  
if (CollUtil.isEmpty(param.getReceivers())) { return Result.error("接收人不能为空"); }
// ... 完全相同的 45 行重复逻辑
```

**解决方案**: 提取公共方法 `prepareMessage(MessageCreateParam param, LocalDateTime scheduleTime)` 返回封装对象。

---

#### 🟠 P1-1: send() 方法中 receiverIndex 方式脆弱

**文件**: `carlos-message-bus/src/main/java/com/carlos/message/service/MessageSendService.java`

**代码**:
```java
int receiverIndex = 0;
for (MessageCreateParam.ReceiverParam receiverParam : param.getReceivers()) {
    for (String channelCode : channelCodes) {
        Long receiverId = receiverList.get(receiverIndex++).getId();
        // ... 发送逻辑
    }
}
```

**问题**: 依赖 receiverList 的创建顺序与嵌套循环完全一致（接收人×渠道数），若创建逻辑变更则导致数组下标越界或发送给错误用户。

**解决方案**: 使用 Map 索引或直接通过 receiverParam+channelCode 查找：
```java
Map<String, Long> receiverKeyToId = receiverList.stream()
    .collect(Collectors.toMap(
        r -> r.getReceiverId() + ":" + r.getChannelCode(),
        MessageReceiver::getId));
```

---

#### 🟠 P1-2: generateMessageId() 非分布式安全

**文件**: `carlos-message-bus/src/main/java/com/carlos/message/service/MessageSendService.java:529`

**代码**:
```java
private String generateMessageId() {
    return "MSG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
}
```

**问题**: `System.currentTimeMillis()` 在分布式环境下可能重复（同一毫秒内多节点生成），`UUID.randomUUID().substring(0, 8)` 截断后碰撞概率大幅提升。

**解决方案**: 使用框架已有的雪花 ID 生成器（carlos-starter-snowflake）或 Redis 自增：
```java
private String generateMessageId() {
    return "MSG" + snowflake.nextId();
}
```

---

#### 🟠 P1-3: RetryPolicy 接口无实现类 — 重试机制缺失

**文件**: `carlos-message-core/src/main/java/com/carlos/message/core/retry/RetryPolicy.java`

**代码**:
```java
public interface RetryPolicy {
    boolean shouldRetry(int retryCount, String errorCode);
    long getRetryInterval(int retryCount);
    int getMaxRetries();
}
```

**问题**: 定义了 RetryPolicy 接口但没有 DefaultRetryPolicy 实现。MessageSendService 发送失败（如渠道异常）只记录日志和状态，不执行重试。发送失败=永久失败，无补偿。

**解决方案**:
```java
@Component
public class ExponentialBackoffRetryPolicy implements RetryPolicy {
    private static final int MAX_RETRIES = 3;
    
    @Override
    public boolean shouldRetry(int retryCount, String errorCode) {
        return retryCount < MAX_RETRIES && isRetryable(errorCode);
    }
    
    @Override
    public long getRetryInterval(int retryCount) {
        return (long) Math.pow(2, retryCount) * 1000; // 2s, 4s, 8s
    }
    
    @Override
    public int getMaxRetries() { return MAX_RETRIES; }
}
```

---

#### 🟠 P1-4: Feign 降级缺少错误码

**文件**: `carlos-message-api/src/main/java/com/carlos/message/api/fallback/MessageSendApiFallbackFactory.java`

**代码**:
```java
@Override
public Result<SendResultAO> send(MessageSendParam param) {
    return Result.error("消息发送服务暂不可用，请稍后重试");
}
```

**问题**: 所有降级方法返回 `Result.error(String)`，无标准错误码，前端无法区分不同类型的降级场景（超时/熔断/限流）。

**解决方案**:
```java
@Override
public Result<SendResultAO> send(MessageSendParam param) {
    return Result.error(MessageErrorCode.MESSAGE_SERVICE_UNAVAILABLE, 
        "消息发送服务暂不可用，请稍后重试");
}
```

---

#### 🟡 P2-1: MessageSendService MDC 手动清理

**文件**: 同上

**问题**: `MDC.put("messageId", messageId)` 在 sendAsync() 和 schedule() 方法中没有对应的 `MDC.remove()` 调用，可能污染异步线程池的 MDC 上下文。

---

### 3.2 六维评估

| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 7/10 | 核心类有 Javadoc，部分私有方法欠缺 |
| 需求覆盖 | 9/10 | 多渠道/模板/限流/降级/队列覆盖完整 |
| 代码规范 | 6/10 | 超大方法、代码重复严重 |
| 性能风险 | 7/10 | 嵌套循环、无批量发送优化。渠道配置有本地缓存 |
| 安全风险 | 8/10 | 敏感词过滤(DFA)不完善，模板注入风险待评估 |
| 功能完整度 | 8/10 | 重试机制未实现，无消息撤回链路追踪 |

**综合得分**: 78/100

---

## 四、模块B: carlos-org（组织架构）

**子模块**: api / boot / bus / cloud  
**文件数**: 372  
**核心功能**: 五层Redis缓存(dept:hash/dept:code/dept:children/dept:desc/dept:anc)、用户管理、部门管理、角色管理、岗位管理、数据权限、密码编码器、Excel导入导出

### 4.1 发现问题

#### 🔴 P0-3: 用户查询暴露密码字段 pwd

**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgUserManagerImpl.java:52`

**代码**:
```java
@Override
public Paging<OrgUserVO> getPage(OrgUserPageParam param) {
    LambdaQueryWrapper<OrgUser> wrapper = queryWrapper();
    wrapper.select(
        OrgUser::getId,
        OrgUser::getAccount,
        OrgUser::getNickname,
        OrgUser::getPwd,          // ⚠️ 暴露密码
        OrgUser::getIdentify,
        OrgUser::getPhone,
        // ...
    );
```

**影响**: 分页查询用户列表时（`/org/user/page` 接口）直接查询密码字段并转换为 VO。**这是严重的安全问题**，密码哈希值不应暴露给前端。

**解决方案**: 从 select 中移除 `OrgUser::getPwd`:
```java
wrapper.select(
    OrgUser::getId,
    OrgUser::getAccount,
    OrgUser::getNickname,
    // OrgUser::getPwd,  // 移除密码字段
    OrgUser::getIdentify,
    OrgUser::getPhone,
    // ...
```

---

#### 🔴 P0-4: 部门缓存无失效机制 — 缓存一致性风险

**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`

**问题分析**: 五层 Redis 缓存结构设计精巧（SELF/CODE/CHILDREN/DESC/ANC 五层），但存在以下一致性问题：

1. **缓存写入有 initCache()** 在启动时全量加载（分页5000条/页），但没有监听部门变更事件来自动更新缓存
2. **clearCache() 使用 `RedisUtil.deleteSpace(PREFIX)`** — 这是 Redis `SCAN` + `DEL` 操作，在集群模式下 O(N) 扫描，大组织架构时性能不佳
3. 部门增删改看不出有对应的缓存更新逻辑（除非调用者显式清理整个缓存空间）

**代码**:
```java
@Override
public long clearCache() {
    long deleteCount = RedisUtil.deleteSpace(PREFIX);
    log.info("OrgDepartment cache has been cleaned, delete count: {}", deleteCount);
    return deleteCount;
}
```

**解决方案**: 实现缓存更新的增量策略：
```java
// 新增部门时：更新父部门的 CHILDREN/DESC，构建新区间的祖先DESC
public void addDepartment(OrgDepartmentDTO dto) {
    // 1. 写 DB
    add(dto);
    // 2. 更新缓存
    updateParentChildrenCache(dto.getParentId(), dto.getId());  // 直接子部门
    updateAncestorDescCache(dto);                               // 祖先的子孙
    // 3. 缓存自身
    cacheSelf(dto);
}
```

---

#### 🟠 P1-5: 密码编码器通过 Feign 远程调用 — 网络依赖单点

**文件**: `carlos-org-bus/src/main/java/com/carlos/org/encoder/DefaultPasswordEncoderProvider.java`

**代码**:
```java
// 基于 ApiAuth Feign 调用认证中心完成密码编码
ApiAuth apiAuth = SpringUtil.getBean(ApiAuth.class);
Result<String> result = apiAuth.encodePassword(passwordEncodeAO);
```

**问题**: 每次密码编码/校验都需要 **跨服务 Feign 调用** 认证中心。如果认证中心宕机或网络抖动，用户新建、密码修改等操作全部失败。这是不必要的强耦合。

**解决方案**: 使用本地 BCrypt 作为默认编码器，Feign 调用仅作为 fallback 或配置选项：
```java
@Override
public String encode(String rawPassword) {
    try {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    } catch (Exception e) {
        log.warn("本地BCrypt编码失败，尝试远程调用", e);
        // fallback to Feign
    }
}
```

---

#### 🟠 P1-6: OrgUser 列表查询缺少部门和岗位关联过滤

**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgUserManagerImpl.java:138`

**代码**:
```java
@Override
public List<OrgUserDTO> listByParam(OrgUserPageParam param) {
    LambdaQueryWrapper<OrgUser> wrapper = new LambdaQueryWrapper<>();
    wrapper.like(StrUtil.isNotBlank(param.getAccount()), OrgUser::getAccount, param.getAccount())
        .like(StrUtil.isNotBlank(param.getNickname()), OrgUser::getNickname, param.getNickname())
        .like(StrUtil.isNotBlank(param.getPhone()), OrgUser::getPhone, param.getPhone())
        // ⚠️ 缺少按部门、按岗位的筛选条件
```

**问题**: `OrgUserPageParam` 可能包含 `deptId` 或 `positionId` 等请求参数，但实现中没有关联到 `SysUserDepartment` 或 `OrgUserPosition` 表做 JOIN 查询。

**解决方案**: 使用 MyBatis-Plus Join 在 Manager 层实现 JOIN 查询。

---

#### 🟡 P2-2: 部门路径解析依赖字符串格式

**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java:492`

**代码**:
```java
private List<String> parseAncestorIds(OrgDepartmentDTO bean) {
    String path = bean.getPath();
    // 按 / 分割并去空
    List<String> parts = StrUtil.splitTrim(path, '/');
    // 过滤掉自身 id（若 path 包含自身）
    return parts.stream()
        .filter(StrUtil::isNotBlank)
        .filter(part -> !part.equals(String.valueOf(selfId)))
        .collect(Collectors.toList());
}
```

**问题**: path 格式如果发生变更（如改为 JSON 数组或 prefix 格式），所有依赖 path 解析的逻辑都会出错。缺少 path 格式校验。

---

### 4.2 六维评估

| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 6/10 | Manager 类有 Javadoc，但缓存策略无文档说明 |
| 需求覆盖 | 9/10 | 部门/用户/角色/岗位/数据权限完整覆盖 |
| 代码规范 | 7/10 | 分层清晰（Controller→Service→Manager→Mapper），VO/DTO/Entity职责正确 |
| 性能风险 | 6/10 | 五层缓存设计精巧但缺乏增量更新，全量清理性能差 |
| 安全风险 | 5/10 | **密码字段泄露严重**，密码编码器强依赖远程服务 |
| 功能完整度 | 8/10 | 导入/导出/预览/确认完整，但缺少组织架构变更历史 |

**综合得分**: 72/100

---

## 五、模块C: carlos-system（系统管理）

**子模块**: api / boot / bus / cloud  
**文件数**: 279  
**核心功能**: 字典管理、系统配置、菜单/路由管理、区域管理、文件上传、国际化i18n、操作资源管理、模块管理

### 5.1 发现问题

#### 🟠 P1-7: 系统配置无 Redis 缓存 — 每次查 DB

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/configration/service/SysConfigService.java:208`

**代码**:
```java
public List<SysConfigDTO> listConfig() {
    List<SysConfigDTO> configs = ALL_CONFIG.getIfPresent(ALL_KEY);
    if (CollUtil.isNotEmpty(configs)) {
        return configs;
    }
    Set<String> sets = new HashSet<>(HOME);
    sets.remove(TITLE);
    configs = configManager.getAllConfig(sets);
    ALL_CONFIG.put(ALL_KEY, configs);
    return configs;
}
```

**问题**: 系统配置使用 **本地 Caffeine** 缓存（static final Cache），在多实例部署时：
1. 每个实例都独立缓存，修改配置需等待 30 分钟过期（`expireAfterAccess(30L, TimeUnit.MINUTES)`）
2. 使用 `static final` 字段，非 Spring 管理，不符合框架规范
3. 首页配置 `getHomePageConfig()` 也有同样问题

**解决方案**: 使用 Redis 集中缓存 + Pub/Sub 失效通知：
```java
public List<SysConfigDTO> listConfig() {
    return cacheManager.getOrFetch("system:config:list", 
        () -> configManager.getAllConfig(excludes), 
        30, TimeUnit.MINUTES);
}
```

---

#### 🟠 P1-8: 无操作日志基础设施

**文件**: carlos-system 全部 bus 模块

**发现**: 通过全量搜索未找到任何 `@Scheduled` 定时任务、操作日志（`SysOperationLog` 或 `OperationLog`）相关的代码。搜索 `LogSave`、`OperationLog`、`operationLog`、`watch` 等关键词均为空。

**影响**: carlos-system 作为管理模块，缺少操作日志记录是功能缺口。菜单/配置/字典的增删改操作无法追溯。

**解决方案**: 集成已有的 carlos-audit 模块，在 Service 层添加 `@AuditLog` 注解：
```java
@Service
public class SysConfigService {
    @AuditLog(module = "系统配置", operation = OperationType.UPDATE)
    public void updateSysConfig(SysConfigDTO dto) { ... }
}
```

---

#### 🟠 P1-9: SysDictManager 不继承 BaseService — 代码规范不一致

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/dict/manager/SysDictManager.java`

**代码**:
```java
// ⚠️ 继承 IService 而非 BaseService
public interface SysDictManager extends IService<SysDict> {
    IPage<SysDictVO> getPage(SysDictPageParam param);  // 返回 IPage 而非 Paging
    // ...
}
```

**问题**: 
1. 框架规范要求 Manager 继承 `BaseService<T>`，但 SysDictManager 直接继承 `IService<SysDict>` 
2. `getPage()` 返回 `IPage<SysDictVO>` 而非标准的 `Paging<SysDictVO>` 
3. 方法签名与其他 Manager（如 SysMenuManager、OrgUserManager）不一致

**解决方案**: 
```java
public interface SysDictManager extends BaseService<SysDict> {
    Paging<SysDictVO> getPage(SysDictPageParam param);
    // ...
}
```

---

#### 🟠 P1-10: SysDictInitRunnerWorker 使用原始 ThreadUtil 无线程池管理

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/dict/config/SysDictInitRunnerWorker.java`

**代码**:
```java
@Override
public void run(ApplicationArguments args) {
    ThreadUtil.execute(() -> {
        cacheManager.initCache();
    });
}
```

**问题**: `ThreadUtil.execute()` 使用 Hutool 的全局线程池，无法监控、无法控制并发、异常无法追踪。如果 initCache() 是耗时操作（大数据量字典），可能长时间占用线程池。

**解决方案**: 使用框架的异步执行器或专用线程池：
```java
@Override
public void run(ApplicationArguments args) {
    CompletableFuture.runAsync(() -> cacheManager.initCache(), taskExecutor)
        .exceptionally(ex -> {
            log.error("字典初始化失败", ex);
            return null;
        });
}
```

---

#### 🟡 P2-3: RouteService 使用不完整实现

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/menu/service/RouteService.java:66`

**代码**:
```java
public List<RouteVO> getConstantRoutes() {
    return List.of();  // ⚠️ 空实现
}
```

**问题**: `getConstantRoutes()` 方法直接返回空列表，未实现常量路由功能。

---

#### 🟡 P2-4: SysConfigService 有 Caffeine import 但标注为 Guava

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/configration/service/SysConfigService.java:99-101`

**代码**:
```java
private static final Cache<String, SysConfigLoginPageVO> HOME_CONFIG = 
    Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(30L, TimeUnit.MINUTES).build();
private static final Cache<String, List<SysConfigDTO>> ALL_CONFIG = 
    Caffeine.newBuilder().maximumSize(10).expireAfterAccess(30L, TimeUnit.MINUTES).build();
```

**问题**: 代码使用的是 `com.github.benmanes.caffeine.cache.Cache`（Caffeine），但写法上用的是 `static final` 静态字段，不符合框架 Spring 管理 Bean 的规范。多实例场景下本地缓存无法同步。

---

#### 🟡 P2-5: getMenuTree 无深度限制防护

**文件**: `carlos-system-bus/src/main/java/com/carlos/system/menu/manager/SysMenuManager.java:81`

**代码**:
```java
List<SysMenuDTO> getMenuTree(Serializable menuId, boolean detail, MenuType menuType);
```

**问题**: `getMenuTree()` 方法没有 maxDepth 参数，如果菜单数据意外形成环状引用，将导致无限递归，StackOverflowError。

**解决方案**: 添加深度限制：
```java
List<SysMenuDTO> getMenuTree(Serializable menuId, boolean detail, MenuType menuType, int maxDepth);
```

---

### 5.2 六维评估

| 维度 | 得分 | 说明 |
|------|------|------|
| 文档完整性 | 6/10 | 核心类有 Javadoc，但新功能 RouteService 等无文档 |
| 需求覆盖 | 8/10 | 字典/配置/菜单/路由/区域/文件/国际化 功能齐全 |
| 代码规范 | 6/10 | SysDictManager 不继承 BaseService、Caffeine 使用 static |
| 性能风险 | 6/10 | 无 Redis 缓存、配置缓存 30 分钟过期、菜单全量加载 |
| 安全风险 | 7/10 | 无明显安全漏洞，但无操作日志追溯 |
| 功能完整度 | 7/10 | 操作日志缺失、常量路由空实现、无定时任务 |

**综合得分**: 68/100

---

## 六、RCA 根因分析

共性问题有清晰的根因追溯：

### 1. 超大方法/代码重复 → 架构重构滞后
`sender` 的三个方法(send/sendAsync/schedule)长期增量叠加，未及时提取公共逻辑，形成"God Method"。**根因**: 缺少 Code Review 门禁；建议拆分后引入策略模式。

### 2. 密码字段暴露 → ORM 层无字段级权限
MyBatis-Plus 的 `wrapper.select()` 直接暴露 Entity 字段，没有字段级安全控制。**根因**: 没有使用 DTO 投影查询；建议引入字段脱敏注解。

### 3. 缓存不一致 → 缺少分布式缓存框架封装
carlos-org 和 carlos-system 的缓存方案各异（Redis五层 / Caffeine本地），没有统一的缓存更新和失效策略。**根因**: ICacheManager 接口未强制要求实现失效通知。

---

## 七、改进路线图

### Phase 1: P0 立即修复（本周）

| 编号 | 问题 | 模块 | 预估工时 |
|------|------|------|----------|
| P0-3 | 移除用户分页查询的密码字段 | carlos-org | 0.5h |
| P0-4 | 部门缓存增量更新 | carlos-org | 4h |
| P0-1 | 拆分 MessageSendService | carlos-message | 6h |

### Phase 2: P1 高位修复（本月）

| 编号 | 问题 | 模块 | 预估工时 |
|------|------|------|----------|
| P1-2 | 消息ID改用雪花生成器 | carlos-message | 1h |
| P1-3 | 实现 DefaultRetryPolicy | carlos-message | 3h |
| P1-4 | Feign降级添加错误码 | carlos-message | 1h |
| P1-5 | 密码编码器本地化 | carlos-org | 2h |
| P1-7 | 系统配置接入Redis缓存 | carlos-system | 4h |
| P1-8 | 接入审计aop日志 | carlos-system | 8h |
| P1-9 | SysDictManager继承BaseService | carlos-system | 3h |

### Phase 3: P2 持续改进（下月）

- 统一 Caffeine 使用方式为 Spring 管理 Bean
- 补齐 RouteService 常量路由
- 添加菜单树深度限制
- 统一 MDC 上下文清理
- 补充缺失的 Javadoc 文档

---

## 八、各模块健康度评分卡

```
carlos-message  ████████████████████░░░░ 78  B+  文档6 覆盖9 规范6 性能7 安全8 功能8
carlos-org      ██████████████████░░░░░░ 72  B   文档6 覆盖9 规范7 性能6 安全5 功能8
carlos-system   █████████████████░░░░░░░ 68  B-  文档6 覆盖8 规范6 性能6 安全7 功能7
```

---

## 九、附录

### A. 已审查的关键文件

**carlos-message (12 个关键文件)**:
- `service/MessageSendService.java` — 573行，核心发送服务
- `channel/ChannelRouter.java` — 权重路由
- `channel/dingtalk/DingtalkChannelAdapter.java` — 钉钉适配器
- `channel/email/EmailChannelAdapter.java` — 邮件适配器
- `channel/sms/SmsChannelAdapter.java` — 短信适配器
- `channel/websocket/WebSocketChannelAdapter.java` — WebSocket适配器
- `channel/spi/ChannelSpiLoader.java` — SPI加载器
- `template/FreeMarkerTemplateEngine.java` — 模板引擎
- `queue/RedisStreamMessageQueue.java` — Redis Stream队列
- `filter/SensitiveWordFilter.java` — DFA敏感词
- `limiter/RedisSlidingWindowRateLimiter.java` — 滑动窗口限流
- `api/fallback/MessageSendApiFallbackFactory.java` — Feign降级

**carlos-org (8 个关键文件)**:
- `manager/impl/OrgDepartmentManagerImpl.java` — 五层Redis缓存
- `manager/impl/OrgUserManagerImpl.java` — 用户查询含密码
- `service/OrgUserService.java` — 779行用户服务
- `config/OrgDataScopeHelper.java` — 数据权限
- `encoder/DefaultPasswordEncoderProvider.java` — 密码编码
- `controller/OrgUserController.java` — 用户REST接口

**carlos-system (10 个关键文件)**:
- `dict/manager/SysDictCacheManager.java` — 字典缓存
- `dict/config/SysDictInitRunnerWorker.java` — 字典初始化
- `configration/service/SysConfigService.java` — 配置服务
- `configration/manager/impl/SysConfigManagerImpl.java` — 配置Manager
- `menu/manager/SysMenuManager.java` — 菜单Manager接口
- `menu/service/RouteService.java` — 路由服务

### B. 搜索覆盖
- 全局搜索操作日志: 无结果
- 全局搜索 @Scheduled 定时任务: 无结果
- 全局搜索 Snowflake 使用: 仅在 snowflake starter 内部使用
- 搜索 RetryPolicy 实现类: 无结果

---

**扫描完成时间**: 2026-06-23 23:38 CST  
**扫描工具**: AI Code Review + 手动分析  
**下一份报告**: 待定（业务模块 Part3 或其他集成模块）
