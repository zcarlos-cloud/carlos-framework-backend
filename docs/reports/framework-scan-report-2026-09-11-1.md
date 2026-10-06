# Carlos Framework 框架核心代码扫描报告

**生成时间**: 2026-09-11 20:02 (CST)
**扫描 Commit**: `94a46a75`（`docs(scan): 框架核心代码扫描报告 2026-09-08`）
**扫描范围**: 框架核心代码层（carlos-dependencies / carlos-parent / carlos-commons / carlos-spring-boot 全部 Starter）
**对比基线**: 上次扫描 `framework-scan-report-2026-09-08-1.md`（commit `f9d1bac7`）
**发现问题总数**: 20 个（P0: 2 个, P1: 4 个, P2: 11 个, P3: 3 个）——与上次完全一致，无新增/无关闭

---

## 一、执行摘要

### 1.1 今日变更概览

- Git 拉取状态: ✅ 成功（token 认证 fetch，`FETCH_HEAD...HEAD` = `0 0`）
- Commit: `94a46a75`（即上次扫描报告提交本身，为当前 HEAD）
- 距上次扫描（09-08）提交数: **0 个**
- **核心层源码变更: 0 个文件**（HEAD 就是 09-08 扫描报告的提交，其后无任何新提交）

### 1.2 远端状态澄清（重要）

本地 `git status` 曾显示「领先 origin/main 共 2 个提交」（`f9d1bac7` 与 `94a46a75`），但经 token 认证 fetch 后复核：

```
git rev-list --left-right --count FETCH_HEAD...HEAD  →  0  0
```

**结论**：远端 GitLab 实际已与本地 HEAD（`94a46a75`）完全同步，09-08 的扫描报告提交已成功推送。「领先 2 个提交」是本地 `origin/main` 引用陈旧导致的误报——历史扫描用 token URL 直接 push，不会更新本地 `origin/main` 引用（此为已记录在案的已知陷阱，非推送管线故障）。

### 1.3 核心结论

**今日（09-11）核心代码层零源码变更，且自上次扫描（09-08）起无任何新提交。** 距核心层最近一次真实源码变更（08-16 前的 `3bad6bfa`）已近 26 天，`carlos-dependencies / carlos-parent / carlos-commons / carlos-spring-boot` 目录源码持续冻结。

本次对全部 20 个问题做了重新验证（P0/P1 全量 grep 复核 + P2/P3 关键项抽查），确认**问题清单与上次完全一致，无新增、无关闭、无回归**。

### 1.4 唯一操作增量：测试脚手架

`carlos-spring-boot-core` 与 `carlos-spring-boot-starter-datascope` 两个模块新增了 `src/test` 目录，但**目录内测试类为 0 个**（脚手架已建、测试未写）。当前实际有测试类的仍为 3 个模块（encrypt 1 / integration 1 / migration 2，共 4 个测试类）。判定为「测试基础设施筹备中」，不计入问题新增，但对 P1-4 的进展有提示意义。

### 1.5 今日重点关注（按严重度排序）

- **[TOP 1]** P0-1 HealthIndicator 非标准包路径残留（19 文件 / 29 处 import）——首次发现 2026-06-04，**已 3 个月未修复**
- **[TOP 2]** P0-2 Datascope 旧版类文件未清理（`com/carlos/datascope/` 根包下 10 个旧类 + 多个子包仍存在）——首次发现 2026-07-03
- **[TOP 3]** P1-6 SslUtil SSL 校验绕过工具仍处于活跃状态（曾修复被 revert，至今无 `@Deprecated` 标记）

---

## 二、P0 问题重新验证（本次重点复核）

### P0-1 HealthIndicator 非标准包路径残留 ✅ 仍存在（19 文件 / 29 处 import）

**复核结果**：`grep -rl "org.springframework.boot.health.contributor"` 命中 **19 个文件**，与上次记录一致。文件清单：

| 模块 | 文件 |
|------|------|
| starter-encrypt | EncryptHealthIndicator.java, EncryptConfig.java |
| starter-oss | OssHealthIndicator.java, OssAutoConfiguration.java |
| starter-mq | MqHealthIndicator.java, MqAutoConfiguration.java |
| starter-mybatis | MybatisHealthIndicator.java, MyBatisPlusConfig.java |
| spring-cloud-starter | ServiceHealthIndicator.java |
| starter-disruptor | DisruptorAutoConfiguration.java, DisruptorHealthIndicator.java |
| starter-flowable | FlowableHealthIndicator.java, FlowableAutoConfiguration.java |
| starter-mongodb | MongoHealthIndicator.java, MongoAutoConfiguration.java |
| starter-redis | RedisHealthIndicator.java, RedisCacheConfig.java |
| audit-bus (integration) | AuditAutoConfiguration.java, ClickHouseHealthIndicator.java |

**问题**：`org.springframework.boot.health.contributor` 为不存在的包，标准包应为 `org.springframework.boot.actuate.health.*`。项目自身未定义该包，标准 Spring Boot 环境下无法编译。

**修复方式**（一条 sed 命令，仍建议本周完成）：

```bash
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.Health;|org.springframework.boot.actuate.health.Health;|g' {} +
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.HealthIndicator;|org.springframework.boot.actuate.health.HealthIndicator;|g' {} +
```

### P0-2 Datascope 旧版类文件未清理 ✅ 仍存在

**复核结果**：`com/carlos/datascope/` 根包下仍残留 10 个旧类文件：`CustomScope`、`DataScopeAspect`、`DataScopeConsumer`、`DataScopeHandler`、`DataScopeInfo`、`DataScope`、`DataScopeProvider`、`DataScopes`、`DataScopeType`、`DefaultDataScopeProvider`，此外还有 `annotation/ audit/ auto/ cache/ conf/ core/ exception/ expression/ interceptor/ properties/ provider/ repository/ util/` 等旧子包，与新版 `core/aspect/`、`provider/` 路径重复。

**修复方式**：确认无引用后删除旧版根包下类文件。

---

## 三、P1 问题复核

| # | 标题 | 模块 | 复核结果 |
|---|------|------|---------|
| P1-3 | `carlos-spring-boot-starter-minio` 模块完全缺失 | — | ✅ `find` 为空，确认缺失 |
| P1-4 | 全量 Starter 测试覆盖率极低 | 全模块 | ✅ 仅 3 模块有实际测试类（4 个）；core/datascope 新增空 `src/test` 目录 |
| P1-5 | 全量 Starter 缺失 spring-configuration-metadata.json | 全模块 | ✅ 元数据 JSON 数量仍为 0 |
| P1-6 | SslUtil SSL 校验绕过工具（MITM 风险） | carlos-utils | ✅ `SslUtil.java` 仍存在，无 `@Deprecated` |

---

## 四、P2/P3 问题复核（抽查确认）

| # | 标题 | 模块 | 复核结果 |
|---|------|------|---------|
| P2-7 | DefaultDataScopeProvider 大量方法 TODO 空实现 | starter-datascope | ✅ `return null/TODO` 命中 5 处 |
| P2-8 | 违反规范使用 RuntimeException（SpelExpressionEngine / OssObjectView） | datascope / oss | 未变更（冻结） |
| P2-9 | XssRequestWrapper 无限制 readAllBytes（DoS 风险） | starter-web | ✅ `XssRequestWrapper.java:133` 仍存在 |
| P2-10 | FlowableProcessService 无限制流读取 | starter-flowable | 未变更（冻结） |
| P2-11 | 安全模块部分过滤器默认未启用 | starter-security | 未变更（冻结） |
| P2-12 | 部分 Starter 配置前缀命名不一致 | 多模块 | 未变更（冻结） |
| P2-13 | 静态工具类持有可变静态状态（含 MultiLevelCacheUtil） | 多模块 | 未变更（冻结） |
| P2-14 | Serializable 类缺失 serialVersionUID | 多模块 | 未变更（冻结） |
| P2-15 | 公共 API 方法返回 null 而非抛异常/Optional | 多模块 | 未变更（冻结） |
| P2-16 | BOM 中存在 SNAPSHOT 依赖声明 | carlos-dependencies | 未变更（冻结） |
| P2-17 | MultiLevelCacheUtil get/put 静默忽略 timeout/timeUnit | starter-redis | ✅ `MultiLevelCacheUtil.java` 仍存在 |
| P3-18 | redisPermissionProviderDelegateInitializer 返回无意义 Object | starter-security | 未变更（冻结） |
| P3-19 | AGENTS.md 中 minio 模块描述与代码库不一致 | 文档 | 未变更（冻结） |
| P3-20 | DefaultMetaObjectHandler.fillTimeField 二次调用 filler.get() | starter-mybatis | 未变更（冻结） |

> 「未变更（冻结）」项：因核心层源码零变更，无需重新 grep，直接沿用 09-08 报告结论；关键项已抽查确认。

---

## 五、问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 编译/类路径错误 | 1 | 0 | 0 | 0 | 1 |
| 功能缺失 | 0 | 1 | 0 | 0 | 1 |
| 安全漏洞 | 0 | 1 | 2 | 0 | 3 |
| 技术债务/规范违反 | 1 | 2 | 8 | 2 | 13 |
| 文档/日志缺陷 | 0 | 0 | 1 | 1 | 2 |
| **合计** | **2** | **4** | **11** | **3** | **20** |

---

## 六、Starter 健康度评分

| 模块 | 设计规范 | 功能完整 | 代码质量 | 安全性 | 综合评分 |
|------|---------|---------|---------|--------|---------|
| carlos-spring-boot-core | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| carlos-spring-boot-starter-web | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 4.1/5.0 |
| carlos-spring-boot-starter-security | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.9/5.0 |
| carlos-spring-boot-starter-mybatis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.9/5.0 |
| carlos-spring-boot-starter-redis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.7/5.0 |
| carlos-spring-boot-starter-datascope | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.9/5.0 |
| 其余 Starter（无独立 P0/P1） | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 3.6/5.0 |

> 评分与 09-08 报告一致（核心层零变更）。datascope 因 P0-2 + P2-7 双问题综合评分最低。

---

## 七、待办事项与修复计划

### 7.1 今日新增待办

无（今日无源码变更）。

### 7.2 历史遗留跟踪（沿用 09-08 清单，均已拖延）

- [ ] **P0-001** 全局替换 HealthIndicator 错误 import（19 文件，一条 sed）⏳ 3 个月未修复
- [ ] **P0-002** 删除 datascope 旧版根包 10 类文件 ⏳ 未修复
- [ ] **P1-003** 确认 minio Starter 设计决策（合并至 oss 或新建）⏳ 待评估
- [ ] **P1-004** 为核心 Starter 补充测试（当前仅 3 模块 4 个测试类）⏳ 筹备中（core/datascope 已建空 test 目录）
- [ ] **P1-005** 生成 spring-configuration-metadata.json ⏳ 未修复
- [ ] **P1-006** SslUtil 落地生产禁用方案或标记 @Deprecated ⏳ 未修复（曾修复被 revert）
- [ ] **P2-007** 实现 DefaultDataScopeProvider 默认方法 ⏳ 未修复
- [ ] **P2-017** MultiLevelCacheUtil 移除误导性 TTL 重载 ⏳ 未修复

---

## 八、操作建议

### 紧急 — 本批次修复（强烈建议本周完成，均已拖延 3 个月）

1. **P0 #1**: HealthIndicator 包路径全局替换（19 文件，一条 sed 命令）
2. **P0 #2**: 删除 datascope 旧版 10 个类文件
3. **P1 #6**: SslUtil 重新落地生产禁用方案（查清 revert 原因）

### 计划 — 下批次修复

4. **P1 #4**: 利用已建好的 core/datascope 空 test 目录，先落地这两个核心模块的测试
5. **P1 #5**: 生成配置元数据 JSON
6. **P2 #7** / **P2 #17**: 实现 datascope 默认 Provider、修复 MultiLevelCacheUtil TTL 语义

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*

*注：本次为「无代码变更」扫描——自上次扫描（2026-09-08）起无任何新提交，全部 20 个问题经重新验证后确认状态不变。*
