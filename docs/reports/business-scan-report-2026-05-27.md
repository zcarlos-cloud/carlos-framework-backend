# Carlos Framework 业务模块质量报告

> **扫描日期**: 2026-05-27  
> **扫描人**: Carlos Framework 专属开发助手  
> **HEAD**: `e990618f` (feat(i18n): 添加国际化消息管理功能)  
> **扫描范围**: 8 个业务集成模块（gateway / auth / audit / org / message / license / system / tools）  
> **总代码文件**: 1338 个 Java 文件

---

## 一、执行摘要

| 指标 | 数值 |
|------|------|
| 扫描模块 | 8 个 |
| 安全漏洞（P0） | **7 项** |
| 设计缺陷（P1） | **17 项** |
| 性能风险（P2） | **12 项** |
| 技术债务（P3） | **6 项** |
| 代码亮点 | **14 项** |

**本周修复重点**：AUTH-001（SecurityUser 硬编码）、GWY-001（WafFilter 响应截断）、ORG-001（部门删除缓存不一致）、MSG-001（事务占用连接）

---

## 二、P0 级发现（立即修复）

### 🔴 AUTH-001: `SecurityUser` 安全状态检查全部硬编码返回 `true`

**模块**: carlos-auth  
**文件**: `carlos-auth-service/src/main/java/com/carlos/auth/security/SecurityUser.java`

```java
@Override
public boolean isAccountNonLocked() { return true; }      // ← 锁定状态永远返回未锁定
@Override
public boolean isAccountNonExpired() { return true; }      // ← 账号过期永远返回未过期
@Override
public boolean isCredentialsNonExpired() { return true; }  // ← 密码过期永远返回未过期
```

**风险**: `LoginAttemptManager` 已将账号在 Redis 和 DB 中标记为 `LOCKED`，但 Spring Security 认证流程**永远不会识别账号锁定状态**。被锁定用户通过其他 grant type 仍可认证成功。

**修复**: 添加 `accountLocked`/`accountExpired`/`credentialsExpired` 字段，从 `LoginUserInfo` 同步状态。

---

### 🔴 GWY-001: `WafFilter.blockRequest()` 响应体未完整写入

**模块**: carlos-gateway  
**文件**: `carlos-gateway/src/main/java/com/carlos/gateway/security/WafFilter.java`

**问题**: `blockRequest()` 中 `response.writeWith(Mono.fromSupplier(() -> ...))` 返回的 Mono 未被订阅确认完成。在 Reactor 异步流中，`writeWith` 返回的 `Mono<Void>` 需要被链式处理，否则响应可能部分发送后连接被关闭。

**修复**: 确保 `blockRequest` 返回的 Mono 被正确链式传递：`return response.writeWith(...).then()`

---

### 🔴 GWY-002: `OAuth2AuthenticationFilter` 认证失败异常静默吞掉

**模块**: carlos-gateway  
**文件**: `carlos-gateway/src/main/java/com/carlos/gateway/oauth2/OAuth2AuthenticationFilter.java`

```java
.onErrorResume(e -> {
    log.error("Authentication failed for path: {}", path, e);
    return writeUnauthorizedResponse(exchange, ...);
})
```

**风险**: `writeUnauthorizedResponse` 本身也可能失败（如响应已提交），此时 `onErrorResume` 会再次捕获并尝试返回 `Mono.empty()`，客户端收到空响应或连接异常，无法获取错误信息。

**修复**: 嵌套 `onErrorResume` 兜底返回 500 错误码和纯文本响应。

---

### 🔴 AUD-001: `ClickHouseBatchWriter` flush 时双缓冲切换无原子性保护

**模块**: carlos-audit  
**文件**: `carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`

```java
// 触发 flush 时切换 activeBuffer
List<AuditLogMainDTO> toFlush = activeBuffer;
activeBuffer = (activeBuffer == buffer1) ? buffer2 : buffer1;
```

**风险**: 切换 `activeBuffer` 与 `toFlush` 引用提取之间没有同步保护。如果此时恰好有 `add()` 线程写入 `toFlush`，会导致 `toFlush` 在异步 flush 过程中被并发修改，数据污染或丢失。

**修复**: 使用 `ReentrantLock` 或 `synchronized` 保护 `activeBuffer` 切换和 `add()` 操作。

---

### 🔴 AUD-002: `ClickHouseBatchWriter` 应用关闭时数据丢失风险

**模块**: carlos-audit  
**文件**: `carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseBatchWriter.java`

**问题**: `@PreDestroy` 中 `awaitTermination` 超时后未强制 `shutdownNow()`，正在 flush 的数据可能丢失。同时未在关闭前执行最后一次 buffer flush。

**修复**: 关闭前强制 flush activeBuffer，超时后 `shutdownNow()` 并记录未 flush 的数据到磁盘备份。

---

### 🔴 ORG-001: `OrgDepartmentManagerImpl.delete()` 未级联清理子孙缓存

**模块**: carlos-org  
**文件**: `carlos-org-bus/src/main/java/com/carlos/org/manager/impl/OrgDepartmentManagerImpl.java`

```java
public boolean delete(Serializable id) {
    // ... 删除数据库记录
    this.deleteCache(dept);  // ← 仅清理自身缓存，未清理子孙的 DESC_KEY/ANC_KEY/CHILDREN_KEY
}
```

**风险**: 删除父部门后，子孙部门的 `dept:desc:{id}` / `dept:ancestors:{id}` / `dept:children:{parentId}` 缓存仍然存在，导致 `getChildrenByParentId()` / `listAll()` 等接口返回已删除的部门数据。

**修复**: delete 时递归清理所有子孙部门的五层缓存。

---

### 🔴 MSG-001: `MessageSendService.send()` 事务包裹外部渠道调用

**模块**: carlos-message  
**文件**: `carlos-message-bus/src/main/java/com/carlos/message/service/MessageSendService.java`

```java
@Transactional(rollbackFor = Exception.class)
public Result<String> send(MessageCreateParam param) {
    // ... 保存消息记录到 DB
    for (...) {
        SendResult sendResult = adapter.send(context);  // ← 外部渠道调用（短信/邮件/钉钉）
        // 可能耗时数秒，占用数据库连接不释放
    }
}
```

**风险**: `@Transactional` 包裹外部 HTTP/RPC 调用，长时间占用数据库连接。如果并发量大，连接池耗尽导致系统不可用。

**修复**: 拆分为两阶段：① 事务内仅保存消息记录和 receiver 状态 ② 事务外循环调用渠道发送。

---

## 三、P1 级发现（本周修复）

### 🟠 AUTH-002: `CaptchaController` 短信验证码登录未完成
**文件**: `carlos-auth-service/.../CaptchaController.java` L156-157
```java
// TODO: 查询用户，如果不存在则自动注册
// TODO: 生成并返回JWT令牌
```

### 🟠 AUTH-003: `SocialCallbackController` 社交登录 JWT 生成未完成
**文件**: `carlos-auth-service/.../SocialCallbackController.java` L142

### 🟠 AUTH-004: `LoginAttemptManager` 硬编码状态字符串
```java
userProvider.updateUserStatus(userInfo.getUserId(), "LOCKED");   // ← 应使用枚举
userProvider.updateUserStatus(userInfo.getUserId(), "ENABLE");
```

### 🟠 AUTH-005: `DefaultUserProvider` 缺少 `@ConditionalOnMissingBean`
仅 `@Profile("!prod")` 保护，意外激活 dev profile 时默认用户可用。

### 🟠 GWY-003: `ReplayProtectionFilter` ThreadLocal Mac 非线程安全
Mac 实例不是线程安全的，不应放入 ThreadLocal 供多线程复用。应使用 `ThreadLocal<Mac>` 但每个线程独立初始化。

### 🟠 GWY-004: `UserContextRelayFilter` Header 传递无签名保护
用户权限信息通过 HTTP Header 明文传递到下游，可被中间人篡改。应添加 HMAC 签名或 JWT 封装。

### 🟠 GWY-005: `WafFilter` 请求体检查异常路径下 dataBuffer 未释放
`checkRequestBody()` 中如果 `DataBufferUtils.join()` 失败，`onErrorResume` 返回 `chain.filter(exchange)` 但未释放 buffer，导致内存泄漏。

### 🟠 ORG-002: `OrgDepartmentManagerImpl.getUsersByDeptId()` 返回空列表
```java
public List<OrgUserDTO> getUsersByDeptId(Serializable deptId) {
    List<OrgDepartmentUserDTO> users = getBaseMapper().getUsersByDeptId(deptId);
    return Collections.emptyList();  // ← 查询了但返回空，实现未完成
}
```

### 🟠 ORG-003: `OrgDepartmentManagerImpl` 缺少分布式锁保护并发树修改
多个实例并发修改部门树（跨级移动/删除）时，Redis 缓存可能出现竞态条件。应使用 Redisson 分布式锁。

### 🟠 MSG-002: `MessageSendService.sendAsync()` 事务与异步执行时序问题
`@Transactional` 方法内保存记录后立即提交异步任务，异步任务可能在事务提交前执行，读取不到刚保存的 receiver 记录。

### 🟠 MSG-003: `MessageSendService` 消息 ID 高并发冲突
```java
private String generateMessageId() {
    return "MSG" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
}
```
同一毫秒下多个请求可能生成相同前缀，虽然 UUID 后缀不同但不够安全。应直接使用 UUID 或雪花 ID。

### 🟠 MSG-004: `MessageSendService.send()` 渠道发送失败无重试机制
首次失败后直接标记 FAILED，未尝试其他渠道或重试。

### 🟠 LIC-001: `LicenseVerify` 使用已停止维护的 TrueLicense 库
de.schlichtherle.license 库多年未更新，存在潜在安全漏洞。建议迁移到 modern 许可证方案。

### 🟠 LIC-002: `LicenseCreatorController` 缺少权限校验
证书生成端点缺少 `@PreAuthorize` 等权限注解，可能被未授权访问生成证书。

### 🟠 SYS-001: `SysDictManagerImpl` 未实现缓存，字典查询每次走数据库
实现了 `ICacheManager<SysDictDTO>` 接口但未实现 `putCache/getCache` 等方法，字典数据未利用 Redis 缓存。

### 🟠 SYS-002: `MenuController` 部分接口返回 `void` 而非 `Result`
```java
@PostMapping
public void add(@RequestBody @Validated MenuCreateParam param) {
```
不符合框架统一响应规范，前端无法获取操作结果。

### 🟠 SYS-003: `FileController` 缺少文件类型/大小校验
文件直接透传给 `fileService`，Controller 层未做前置校验。

---

## 四、P2 级发现（纳入下个迭代）

| 编号 | 模块 | 问题 | 文件 |
|------|------|------|------|
| P2-01 | auth | JwtProperties 默认密钥长度 2048，建议 4096 | `JwtProperties.java` |
| P2-02 | gateway | WafFilter CSRF 仅验证 Origin/Referer 前缀，可绕过 | `WafFilter.java` |
| P2-03 | gateway | OAuth2AuthenticationFilter Header 注入可能超限 | `OAuth2AuthenticationFilter.java` |
| P2-04 | audit | AuditLogEventHandler `LocalDateTime.now()` 性能瓶颈 | `AuditLogEventHandler.java` |
| P2-05 | audit | ClickHouseBatchWriter 重试无上限保护 | `ClickHouseBatchWriter.java` |
| P2-06 | audit | 风险评分模型缺少业务扩展点 | `AuditLogEventHandler.java` |
| P2-07 | org | `initCache()` 分页 5000 条/页初始化可能长时间阻塞 | `OrgDepartmentManagerImpl.java` |
| P2-08 | org | `getChildrenByParentId()` 返回列表未按 sort 排序 | `OrgDepartmentManagerImpl.java` |
| P2-09 | message | `renderTemplate()` 特殊字符处理不完善 | `MessageSendService.java` |
| P2-10 | message | `revoke()` 仅更新状态未实际撤回已发送消息 | `MessageSendService.java` |
| P2-11 | license | `LicenseVerify.verify()` 高频调用产生大量日志 | `LicenseVerify.java` |
| P2-12 | system | `FileController.singleUploadForAnonymous()` 与 `singleUpload()` 逻辑重复 | `FileController.java` |

---

## 五、P3 级发现（技术债务）

| 编号 | 模块 | 问题 |
|------|------|------|
| P3-01 | auth | JavaDoc 注释格式不统一 |
| P3-02 | auth | `carlos-auth-boot` README.md 缺失 |
| P3-03 | tools | `CodeGeneratorService` `@Scope("prototype")` 可改为单例 |
| P3-04 | tools | 多模块模板 TODO 未实现 | `CodeGeneratorService.java` |
| P3-05 | tools | `ToolsApplication` JavaFX 与微服务架构不匹配 |
| P3-06 | system | `SysDictManagerImpl` 使用 `org.apache.commons.lang3.StringUtils` 冗余依赖 |

---

## 六、代码亮点

| 模块 | 亮点 | 说明 |
|------|------|------|
| **gateway** | WAF 性能优化 | ThreadLocal Matcher + BitSet 快速检查 |
| **gateway** | 防重放攻击 | HMAC-SHA256 + 时间戳 + Nonce 三重验证 |
| **gateway** | 用户上下文传递 | 完整的用户信息 Header 中继 |
| **auth** | BCrypt 强制 | 无弱编码器，安全基线达标 |
| **auth** | 暴力破解防护 | IP + 账号双重 Redis 计数 + 滑动窗口 |
| **auth** | Token 掩码 | `maskToken()` 防止日志泄露 |
| **auth** | MFA 风险评估 | 异地登录 + 非工作时间检测 |
| **audit** | 双缓冲批量写入 | 每次 flush 创建新 ArrayList，避免 clear() 污染 |
| **audit** | 风险评分模型 | 基础60+状态20+主体10+渠道10 = 100分 |
| **audit** | 本地磁盘备份 | ClickHouse 不可用时的兜底方案 |
| **org** | 五层 Redis 缓存 | Hash/Children/Desc/Anc/Code 完整树形结构 |
| **org** | 跨级移动缓存重建 | 先清理旧关系再重建新关系 |
| **message** | 多渠道发送 | ChannelAdapter + ChannelFactory 扩展设计 |
| **message** | 异步发送 | 独立 Bean 避免 @Async 自调用代理失效 |

---

## 七、修复优先级路线图

### 本周内（P0 + 高优先级 P1）
```
├─ AUTH-001: SecurityUser 安全状态硬编码 → 添加 accountLocked/expired 字段
├─ GWY-001: WafFilter blockRequest 响应完整性 → 修复 Mono 链式处理
├─ GWY-002: OAuth2AuthenticationFilter 异常吞掉 → 嵌套 onErrorResume 兜底
├─ AUD-001: ClickHouseBatchWriter 双缓冲并发 → 加锁保护切换
├─ AUD-002: ClickHouseBatchWriter 关闭时丢数据 → 强制 flush + shutdownNow
├─ ORG-001: 部门删除缓存不一致 → 递归清理子孙缓存
├─ MSG-001: 事务包裹外部调用 → 拆分为两阶段（事务内仅保存）
├─ AUTH-002/003: 完成 CaptchaController / SocialCallbackController JWT 生成
└─ AUTH-004: LoginAttemptManager 硬编码 → 改用枚举常量
```

### 下个迭代（P1 剩余 + P2）
```
├─ GWY-003: ThreadLocal Mac 线程安全 → 改为每个线程独立初始化
├─ GWY-004: UserContextRelayFilter Header 签名 → 添加 HMAC 签名
├─ MSG-002: 异步时序 → 使用 TransactionSynchronizationManager
├─ MSG-003: 消息 ID 冲突 → 改用雪花 ID
├─ LIC-001: TrueLicense 迁移 → 评估替换方案
├─ SYS-001: SysDict 缓存 → 实现 ICacheManager 方法
├─ SYS-002: MenuController 返回 Result → 统一响应格式
└─ P2 全部 12 项纳入计划
```

---

> _报告生成时间: 2026-05-27_  
> _扫描人: Carlos Framework 专属开发助手_  
> _扫描范围: gateway(95) + auth(226) + audit(96) + org(358) + message(157) + license(22) + system(302) + tools(82) = 1338 文件_
