# Carlos Framework 业务模块质量报告

**生成时间**: 2026-05-25 08:00
**扫描 Commit**: `c37f4715`
**昨日 Commit**: `5fd02b1a`
**扫描范围**: 业务集成层（gateway / tools / audit / auth / license / message / org / system）
**发现问题总数**: 22 个（P0: 1 个, P1: 7 个, P2: 10 个, P3: 4 个）
**今日新增问题**: 0 个
**今日修复问题**: 0 个

---

## 一、执行摘要

### 1.1 今日变更概览

- **Git 拉取状态**: 失败（使用本地代码继续扫描）
- **Commit**: `c37f4715` — `docs: 框架核心代码扫描报告 2026-05-25`
- **变更文件数**: 1 个（仅 `docs/reports/framework-scan-report-2026-05-25-1.md`）
- **新增代码**: +0 行 / 删除代码: -0 行（**今日无业务模块代码变更**）

> 报告基于最新 Commit `c37f4715` 扫描，排除 `docs/reports` 和 `carlos-ui` 课程文件后，确认业务集成层无任何代码变更。

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 安全漏洞 | 1 | 0 | 1 | 1 | 3 |
| 需求与设计缺陷 | 0 | 2 | 2 | 0 | 4 |
| 代码规范违规 | 0 | 1 | 2 | 0 | 3 |
| 性能风险 | 0 | 2 | 1 | 0 | 3 |
| 功能未完善 | 0 | 2 | 1 | 1 | 4 |
| 文档缺失 | 0 | 0 | 2 | 1 | 3 |
| 技术债务 | 0 | 0 | 1 | 1 | 2 |
| **合计** | **1** | **7** | **10** | **4** | **22** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-auth | 0 | 1 | 1 | 0 | 2 |
| carlos-gateway | 0 | 0 | 1 | 1 | 2 |
| carlos-audit | 0 | 1 | 2 | 0 | 3 |
| carlos-message | 0 | 1 | 3 | 0 | 4 |
| carlos-org | 0 | 2 | 0 | 0 | 2 |
| carlos-system | 0 | 1 | 2 | 1 | 4 |
| carlos-license | 1 | 0 | 0 | 1 | 2 |
| carlos-tools | 0 | 1 | 1 | 1 | 3 |

### 1.4 今日重点关注

- **[TOP 1] P0-001 `carlos-license` 生产环境打包私钥泄露风险 — 仍未修复**（license-generate 仍在聚合中）
- **[TOP 2] P1-002 `carlos-audit` 4个核心查询方法仍为 TODO 空实现 — 仍未修复**
- **[TOP 3] P1-003 `carlos-message` send() 大事务 + 同步外部调用 — 仍未修复**

---

## 二、昨日 P0/P1 问题修复状态追踪

### 2.1 P0-Critical 问题修复状态

| 问题 ID | 模块 | 问题简述 | 修复状态 | 说明 |
|---------|------|----------|----------|------|
| **P0-001** | **carlos-license** | **父 POM 聚合 license-generate 模块** | ❌ **未修复** | `pom.xml` 中 `carlos-spring-boot-starter-license-generate` 仍在 `<modules>` 和 `<dependencyManagement>` 中 |

### 2.2 P1-High 问题修复状态

| 问题 ID | 模块 | 问题简述 | 修复状态 | 说明 |
|---------|------|----------|----------|------|
| P1-002 | carlos-audit | AuditLogQueryService 4个核心方法 TODO | ❌ **未修复** | 4 个 `// TODO` 仍在原位置 |
| P1-003 | carlos-message | MessageSendService.send() 大事务+同步调用 | ❌ **未修复** | `@Transactional` 仍在方法上，同步调用未改为异步 |
| P1-004 | carlos-org | OrgDepartmentManagerImpl 内存分页 | ❌ **未修复** | `getUsersByDeptId` 仍使用 `allUsers.subList()` |
| P1-005 | carlos-system | SysConfigServiceImpl 缓存不一致 | ❌ **未修复** | `HOME_CONFIG`/`ALL_CONFIG` 仍无失效逻辑 |
| P1-006 | carlos-org | OrgDepartmentManagerImpl 缺少循环引用检测 | ❌ **未修复** | `modify()` 中仍无 `isCircularReference()` 校验 |
| P1-007 | carlos-message | MessageSendApiImpl 使用 BeanUtils.copyProperties | ❌ **未修复** | `BeanUtils.copyProperties` 仍在 L46/L64/L84 |
| P1-008 | carlos-tools | carlos-tools 被 carlos-integration 聚合 | ❌ **未修复** | 开发工具仍与业务模块在同一构建树 |

### 2.3 关键代码复扫确认

**复扫时间**: 2026-05-25 08:00

1. **P0-001 license-generate 聚合确认** ✅（仍在）
   ```bash
   $ grep -l "carlos-spring-boot-starter-license-generate" carlos-integration/carlos-license/pom.xml
   carlos-integration/carlos-license/pom.xml
   ```

2. **P1-002 AuditLogQueryService TODO 确认** ✅（仍在）
   ```bash
   $ grep -n "TODO" AuditLogQueryService.java
   48:  // TODO: 实现 ClickHouse 查询
   69:  // TODO: 实现查询
   83:  // TODO: 从物化视图查询统计
   117: // TODO: 实现查询
   ```

3. **P1-007 BeanUtils.copyProperties 确认** ✅（仍在）
   ```bash
   $ grep -n "BeanUtils.copyProperties" MessageSendApiImpl.java
   46: BeanUtils.copyProperties(param, createParam);
   64: BeanUtils.copyProperties(param, createParam);
   84: BeanUtils.copyProperties(param, createParam);
   ```

---

## 三、安全漏洞详情（遗留未修复）

### 问题 P0-001: `carlos-license` 父 POM 聚合了证书生成模块

**模块**: `carlos-license`
**文件**: `carlos-integration/carlos-license/pom.xml`
**行号**: L17-L42
**严重程度**: 🔴 **P0-Critical**
**影响范围**: 多模块联动
**CWE**: CWE-1104
**修复状态**: ❌ 未修复

#### 相关代码

```xml
<!-- carlos-integration/carlos-license/pom.xml -->
<modules>
    <module>carlos-license-core</module>
    <module>carlos-spring-boot-starter-license-generate</module>
    <module>carlos-spring-boot-starter-license-verify</module>
</modules>
```

#### 详细分析

- **问题描述**: `carlos-spring-boot-starter-license-generate` 是仅用于开发环境的证书生成模块，被聚合在 `carlos-license` 父 POM 中。全量构建时会编译并安装该模块，存在生产环境误引入风险。
- **触发条件**: `mvn clean install` 全量构建，或生产服务 POM 误引用
- **潜在风险**: 私钥泄露、证书被任意签发、商业授权体系失效

#### 解决方案

1. 将 `license-generate` 从 `carlos-license/pom.xml` 的 `<modules>` 和 `<dependencyManagement>` 中移除
2. 为其创建独立的聚合 POM，仅在开发环境构建
3. 在 CI/CD 构建生产制品时，使用 `-pl` 参数排除该模块

---

## 四、性能风险详情（遗留未修复）

### 问题 P1-003: `MessageSendService.send()` 事务边界过大且同步调用外部渠道

**模块**: `carlos-message`
**文件**: `carlos-integration/carlos-message/carlos-message-bus/.../MessageSendService.java`
**行号**: L56-L180
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

`send()` 方法标记 `@Transactional`，内部对每个接收人×每个渠道都执行一次 `messageReceiverManager.save()`，随后立即同步调用 `adapter.send()`（外部 HTTP/RPC/SMS 网关）。

#### 潜在风险

- 大事务：100接收人×2渠道=200次INSERT，事务持有时间长
- 连接池耗尽：外部同步调用阻塞在事务内，数据库连接长期占用
- 超时回滚：外部渠道超时可能触发事务回滚，但部分消息已发出

#### 解决方案

1. 缩小事务边界：仅批量保存 MessageRecord + MessageReceiver
2. 使用 `saveBatch()` 一次性写入所有接收人
3. 渠道调用移出事务，通过异步线程池执行
4. 增加 `messageId + receiverId + channelCode` 唯一索引防重复发送

---

### 问题 P1-004: `OrgDepartmentManagerImpl` 部门用户查询使用内存分页

**模块**: `carlos-org`
**文件**: `carlos-integration/carlos-org/carlos-org-bus/.../OrgDepartmentManagerImpl.java`
**行号**: L270-L290
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

`getUsersByDeptId()` 先从数据库全量加载某部门下的所有用户，然后在 Java 内存中执行 `subList` 分页。大型部门（数万用户）将导致 OOM。

#### 解决方案

1. 修改 Mapper XML，在 SQL 层使用 `LIMIT`/`OFFSET` 分页
2. 使用 MyBatis-Plus Join 插件在 SQL 层完成分页

---

## 五、功能未完善详情（遗留未修复）

### 问题 P1-002: `AuditLogQueryService` 4 个核心查询方法为空实现

**模块**: `carlos-audit`
**文件**: `carlos-integration/carlos-audit/carlos-audit-bus/.../AuditLogQueryService.java`
**行号**: L35-L117
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

审计日志写入链路已实现（Disruptor + ClickHouse），但查询侧几乎完全不可用：

```java
public Paging<AuditLogMainDTO> pageQuery(...) {
    // TODO: 实现 ClickHouse 查询
    return new Paging<>();
}

public AuditLogMainDTO getById(Long id) {
    // TODO: 实现查询
    return null;
}

public AuditLogStatsVO getRealtimeStats(...) {
    // TODO: 从物化视图查询统计
    return new AuditLogStatsVO();
}

public List<AuditLogMainDTO> listRiskEvents(...) {
    // TODO: 实现查询
    return Collections.emptyList();
}
```

#### 潜在风险

无法满足等保/ISO 审计要求、安全事件无法追溯、运营分析缺失。

#### 解决方案

1. 使用 ClickHouse JDBC 实现分页查询
2. `getRealtimeStats` 查询物化视图 `audit_log_stats_mv_local`
3. `listRiskEvents` 添加 `risk_level >= #{minRiskLevel}` 条件过滤
4. 添加 Caffeine 本地缓存（TTL 1分钟）缓解高频查询压力

---

## 六、需求与设计缺陷详情（遗留未修复）

### 问题 P1-005: `SysConfigServiceImpl` 配置变更后本地缓存不一致

**模块**: `carlos-system`
**文件**: `carlos-integration/carlos-system/carlos-system-bus/.../SysConfigServiceImpl.java`
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

`HOME_CONFIG` 和 `ALL_CONFIG` 是静态 Caffeine 本地缓存（TTL 30分钟），但 `deleteSysConfig` 和 `updateSysConfig` 在数据变更后**未调用缓存失效**。

#### 解决方案

1. 在增删改成功后立即清理相关缓存
2. 引入 Redis Pub/Sub 或 Spring Cloud Bus 事件广播缓存失效

---

### 问题 P1-006: `OrgDepartmentManagerImpl` 缺少部门树循环引用检测

**模块**: `carlos-org`
**文件**: `carlos-integration/carlos-org/carlos-org-bus/.../OrgDepartmentManagerImpl.java`
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

`modify()` 方法允许变更部门 `parentId`，但未校验新父节点是否为自己或子孙节点，可能导致循环引用和 StackOverflowError。

#### 解决方案

在 `modify` 和 `add` 中增加循环引用校验：

```java
private boolean isCircularReference(Long deptId, Long newParentId) {
    if (deptId == null || newParentId == null) return false;
    if (deptId.equals(newParentId)) return true;
    Set<Long> descendants = getDescIdsFromCache(deptId);
    return descendants != null && descendants.contains(newParentId);
}
```

---

## 七、代码规范违规详情（遗留未修复）

### 问题 P1-007: `MessageSendApiImpl` 使用 `BeanUtils.copyProperties` 而非 MapStruct

**模块**: `carlos-message`
**文件**: `carlos-integration/carlos-message/carlos-message-bus/.../MessageSendApiImpl.java`
**行号**: L46, L64, L84
**严重程度**: 🟠 **P1-High**
**修复状态**: ❌ 未修复

#### 问题描述

框架强制规范要求对象转换必须使用 **MapStruct**，但 `MessageSendApiImpl` 中仍使用 `BeanUtils.copyProperties`（基于反射，性能差、类型不安全）。

#### 解决方案

定义 MapStruct Converter 并替换所有 `BeanUtils.copyProperties` 调用。

---

## 八、技术债务详情

### 8.1 模块间依赖合规性

| 模块 | 违规依赖 | 规范要求 | 状态 |
|----|------|------|------|
| `carlos-integration` | 聚合了 `carlos-tools`（JavaFX GUI 工具） | 开发工具应与业务模块分离 | ❌ 未修复 |
| `carlos-license` | 聚合了 `license-generate` | 生产严禁包含 generate | ❌ 未修复 |

### 8.2 TODO/FIXME 遗留（未修复）

| 文件 | 行号 | 标记内容 | 严重程度 | 状态 |
|----|----|--------|--------|------|
| `AuditLogQueryService.java` | L48, L69, L83, L117 | TODO: 实现 ClickHouse 查询/统计 | P1 | ❌ 未修复 |
| `SysConfigServiceImpl.java` | L92 | TODO: 文件类型配置转地址 | P2 | ❌ 未修复 |
| `LicenseCreatorController.java` | L32 | TODO: 用脚本代替接口获取硬件信息 | P2 | ❌ 未修复 |
| `KeyPairManager.java` | L211 | TODO: 引入 BouncyCastle 证书链 | P3 | ❌ 未修复 |
| `CaptchaController.java` | L156 | TODO: 查询用户自动注册 | P2 | ❌ 未修复 |
| `SysRegionManagerImpl.java` | L417, L423 | FIXME: 缓存失效/层级变更 | P2 | ❌ 未修复 |
| `SysRegionService.java` | L356 | FIXME: 方式优化 | P2 | ❌ 未修复 |
| `CodeGeneratorService.java` | L95 | TODO: 多模块模板目录 | P3 | ❌ 未修复 |
| `GitlabMainController.java` 等 | 大量 | TODO: FX 界面功能 | P3 | ❌ 未修复 |

---

## 九、业务模块健康度评分

| 模块 | 文档完整 | 需求合理 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|-----|--------|--------|--------|--------|--------|--------|
| carlos-auth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | **4.6/5.0** |
| carlos-gateway | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | **4.0/5.0** |
| carlos-audit | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐☆☆☆ | **3.8/5.0** |
| carlos-message | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | **3.6/5.0** |
| carlos-org | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | **3.8/5.0** |
| carlos-system | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | **3.6/5.0** |
| carlos-license | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐☆☆☆ | ⭐⭐⭐⭐☆ | **3.2/5.0** |
| carlos-tools | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | **3.0/5.0** |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 十、待办事项与修复计划

### 10.1 今日新增待办

（今日无新增问题）

### 10.2 历史遗留跟踪（未关闭）

- [ ] **[P0]** `carlos-license`: 将 `carlos-spring-boot-starter-license-generate` 从 `carlos-license/pom.xml` 移除，建立独立构建线 ❌ **未修复**
- [ ] **[P1]** `carlos-audit`: 实现 `AuditLogQueryService` 4 个核心查询方法 ❌ **未修复**
- [ ] **[P1]** `carlos-message`: 重构 `MessageSendService.send()`，缩小事务边界，异步发送 ❌ **未修复**
- [ ] **[P1]** `carlos-org`: 修复 `getUsersByDeptId` 内存分页为 SQL 分页 ❌ **未修复**
- [ ] **[P1]** `carlos-system`: 修复 `SysConfigServiceImpl` 缓存不一致 ❌ **未修复**
- [ ] **[P1]** `carlos-org`: 增加部门树循环引用检测 ❌ **未修复**
- [ ] **[P1]** `carlos-message`: `MessageSendApiImpl` BeanUtils → MapStruct ❌ **未修复**
- [ ] **[P1]** `carlos-tools`: 将 `carlos-tools` 移出 `carlos-integration` 聚合 ❌ **未修复**
- [ ] **[P2]** `carlos-system`: `SysDictManagerImpl` 实现 `ICacheManager` 缓存方法 ❌ **未修复**
- [ ] **[P2]** `carlos-auth`: `LoginAttemptManager` 阈值提取到 `@ConfigurationProperties` ❌ **未修复**
- [ ] **[P2]** `carlos-audit`: `ClickHouseBatchWriter` BeanUtils → MapStruct/手动赋值 ❌ **未修复**
- [ ] **[P2]** `carlos-gateway`: 补充 `README.md` ❌ **未修复**
- [ ] **[P2]** `carlos-integration`: 补充各模块 `docs/01-product/PRD/README.md` 空模板 ❌ **未修复**

### 10.3 已关闭历史待办

（无）

---

*本报告由 OpenClaw 定时任务 `carlos-framework-business-scan` 自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
