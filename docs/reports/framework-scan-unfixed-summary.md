# Carlos Framework 未修复问题汇总

**生成时间**: 2026-09-18
**扫描报告**: `framework-scan-report-2026-09-18-1.md`
**扫描 Commit**: `d30e15cc`
**当前未修复问题总数**: 20 个（P0: 2, P1: 4, P2: 11, P3: 3）

> **说明**: 2026-09-18 扫描，距上次（09-15）0 个新提交，**框架核心层（dependencies/commons/spring-boot）零源码变更**。核心 Starter 问题清单与上次完全一致（20 个），无新增/无关闭。两个 P0 问题已持续 3 个月未修复（HealthIndicator 包路径残留、datascope 旧类未清理）。SslUtil MITM 风险工具仍处于活跃状态（曾修复被 revert）。本次 P0/P1 全量 grep 复核确认：P0-1 仍为 19 文件 / 29 处 import；P0-2 旧版根包下仍残留 10 个旧类文件；minio 缺失、config metadata 为 0、SslUtil 无 @Deprecated 均维持不变。远端状态：经 token 认证 fetch 后 `FETCH_HEAD...HEAD` = `0 0`，远端已与本地同步。

---

## 一、按优先级分布

| 优先级 | 数量 | 说明 |
|--------|------|------|
| P0-Critical | 2 | 编译阻断 / 类路径冲突，历史遗留 3 个月未修复 |
| P1-High | 4 | 功能缺失、测试/配置基础设施、安全绕过工具 |
| P2-Medium | 11 | 空实现、安全绕过工具、代码规范违反、TTL 静默忽略 |
| P3-Low | 3 | 技术债务、文档不一致、日志缺陷 |

---

## 二、P0-Critical（2 个）

| # | 标题 | 模块 | 首次发现 | 状态 |
|---|------|------|---------|------|
| 1 | HealthIndicator 非标准包路径残留（19 文件 / 29 处 import） | 多模块 | 2026-06-04 | ⚠️ 3 个月未修复 |
| 2 | Datascope 旧包路径类文件未清理（根包 10 类重复） | starter-datascope | 2026-07-03 | ⚠️ 未修复 |

### 详情

**问题 1**: 29 处 import `org.springframework.boot.health.contributor.HealthIndicator` / `Health`（错误包），标准包应为 `org.springframework.boot.actuate.health.*`。分布在 19 个文件：encrypt(2)、oss(2)、mq(2)、mybatis(2)、spring-cloud-starter(1)、disruptor(2)、flowable(2)、mongodb(2)、redis(2)、audit-bus(2)。项目代码库中未定义该包，标准 Spring Boot 环境下无法编译。

**修复方式**: 全局替换 import 路径。
```bash
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.Health;|org.springframework.boot.actuate.health.Health;|g' {} +
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.HealthIndicator;|org.springframework.boot.actuate.health.HealthIndicator;|g' {} +
```

**问题 2**: 旧版 `com/carlos/datascope/` 根包下的 DataScopeAspect / DataScopeProvider / DefaultDataScopeProvider / DataScope / DataScopes / DataScopeType / DataScopeInfo / DataScopeConsumer / DataScopeHandler / CustomScope（共 10 个旧类）未删除，与新版 `core/aspect/`、`provider/` 路径重复。

**修复方式**: 删除旧版根包下 10 个类文件（确认无引用后）。

---

## 三、P1-High（4 个）

| # | 标题 | 模块 | 类型 |
|---|------|------|------|
| 3 | carlos-spring-boot-starter-minio 模块完全缺失 | — | 功能缺失 |
| 4 | 全量 Starter 测试覆盖率极低（24 Starter 仅 3 模块有测试） | 全模块 | 测试基础设施 |
| 5 | 全量 Starter 缺失 spring-configuration-metadata.json | 全模块 | 技术债务 |
| 6 | SslUtil 提供 SSL 证书校验绕过工具（MITM 风险，曾修复被 revert） | carlos-utils | 安全 |

### 详情

**问题 3**: minio Starter 模块在代码库中不存在（`find carlos-spring-boot -name "*minio*"` 为空），可能已合并到 oss Starter。需确认设计决策。

**问题 4**: 24 个 Starter 仅 encrypt/integration/migration 三个模块有测试，其余 21 模块零覆盖。

**问题 5**: 全项目配置元数据 JSON 文件数为 0，IDE 无法自动补全框架配置。

**问题 6**: `SslUtil` 的 X509TrustManager 空实现 + HostnameVerifier 永远 true，若误用于生产将导致 MITM。**曾于 2026-06-30 修复（commit 3be03b06）后同日被 revert（commit 63a51f90），当前回退到不安全状态，无 @Deprecated 标记**。修复：重新落地生产禁用方案，或至少标记 `@Deprecated` 注明仅限测试。

---

## 四、P2-Medium（11 个）

| # | 标题 | 模块 |
|---|------|------|
| 7 | DefaultDataScopeProvider 大量方法仍为 TODO 空实现（return null） | starter-datascope |
| 8 | 违反规范使用 RuntimeException（SpelExpressionEngine / OssObjectView） | datascope / oss |
| 9 | XssRequestWrapper 无限制 readAllBytes（DoS 风险） | starter-web |
| 10 | FlowableProcessService 无限制流读取 | starter-flowable |
| 11 | 安全模块部分过滤器默认未启用 | starter-security |
| 12 | 部分 Starter 配置前缀命名不一致 | 多模块 |
| 13 | 静态工具类持有可变静态状态（含 MultiLevelCacheUtil） | 多模块 |
| 14 | Serializable 类缺失 serialVersionUID | 多模块 |
| 15 | 公共 API 方法返回 null 而非抛异常/Optional | 多模块 |
| 16 | BOM 中存在 SNAPSHOT 依赖声明 | carlos-dependencies |
| 17 | MultiLevelCacheUtil get/put 静默忽略 timeout/timeUnit（TTL 不生效） | starter-redis |

---

## 五、P3-Low（3 个）

| # | 标题 | 模块 |
|---|------|------|
| 18 | redisPermissionProviderDelegateInitializer 返回无意义 Object（副作用 Bean） | starter-security |
| 19 | AGENTS.md 中 minio 模块描述与代码库实际不一致 | 文档 |
| 20 | DefaultMetaObjectHandler.fillTimeField 二次调用 filler.get()（日志值不一致） | starter-mybatis |

---

## 六、已修复 / 已排除（历史确认）

| # | 标题 | 原优先级 | 结论 |
|---|------|---------|------|
| — | ResourceServerAutoConfiguration 同类型 Bean 条件冲突 | P1 | 已改为 name-based 条件 + `@Primary`（08-15 确认） |
| — | JsonAutoConfig 三引擎切换设计缺陷 | P1 | 复核确认为误报：@ConditionalOnProperty(havingValue) 已使三引擎互斥（08-16 移除） |

---

## 七、操作建议

### 紧急 — 本批次修复（强烈建议本周完成，均已拖延 3 个月）
1. **P0 #1**: HealthIndicator 包路径全局替换（19 文件，一条 sed 命令）
2. **P0 #2**: 删除 datascope 旧版 10 个类文件
3. **P1 #6**: SslUtil 重新落地生产禁用方案（查清 revert 原因）

### 计划 — 下批次修复（建议本月完成）
4. **P1 #5**: 生成配置元数据 JSON
5. **P1 #4**: 为核心 Starter 补充测试
6. **P2 #7**: 实现 DefaultDataScopeProvider 默认方法（从 SecurityContext 取用户/部门/角色）
7. **P2 #17**: MultiLevelCacheUtil 移除误导性 TTL 重载或改为抛异常

### 待评估
8. **P1 #3**: 确认 minio Starter 设计决策

---

*最后更新: 2026-09-18 基于 framework-scan-report-2026-09-18-1.md（无源码变更，清单沿用 09-15）*
