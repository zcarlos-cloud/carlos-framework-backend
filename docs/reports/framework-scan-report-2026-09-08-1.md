# Carlos Framework 框架核心代码扫描报告

**生成时间**: 2026-09-08 23:06 (CST)
**扫描 Commit**: `f9d1bac7`（`docs: 移除前端公共模块章节（已迁移到 carlos-framework-ui 仓库）`）
**扫描范围**: 框架核心代码层（carlos-dependencies / carlos-parent / carlos-commons / carlos-spring-boot 全部 Starter）
**对比基线**: 上次扫描 `framework-scan-report-2026-08-16-0911.md`（commit `3bad6bfa`）
**发现问题总数**: 20 个（P0: 2 个, P1: 4 个, P2: 11 个, P3: 3 个）——与上次完全一致，无新增/无关闭

---

## 一、执行摘要

### 1.1 今日变更概览

- Git 拉取状态: ✅ 成功（`git pull --rebase` token 认证，`FETCH_HEAD...HEAD` = `0 0`，远端无新提交）
- Commit: `f9d1bac7`
- 距上次扫描提交数: 5 个（自 `3bad6bfa`）
- **核心层源码变更: 0 个文件**（`git diff 3bad6bfa HEAD -- carlos-dependencies carlos-parent carlos-commons carlos-spring-boot` 为空）

### 1.2 核心结论

**今日核心代码层（dependencies / commons / spring-boot）零源码变更**，距上次扫描（08-16）已 23 天。5 个新提交全部集中在仓库级重构与文档层，未触及任何 Starter 源码：

| Commit | 类型 | 说明 | 是否触及核心层 |
|--------|------|------|--------------|
| `4cf7d185` | refactor(split) | 拆分独立仓库，移除 carlos-ui/carlos-portal 及共享 AI 配置 | ❌（前端目录，非 Maven 核心模块） |
| `e4fc49cf` | docs | 更新文档中的前端仓库名称引用 | ❌ |
| `bc5d6b2d` | chore(config) | 更新 GitLab 仓库地址为 192.168.3.40 | ❌ |
| `f9d1bac7` | docs | 移除前端公共模块章节 | ❌ |
| `1b22ada2` | docs(scan) | 上次扫描报告提交 | ❌ |

根 `pom.xml` 的 `<modules>` 列表（dependencies / parent / commons / spring-boot / integration / samples）与上次扫描基线**逐字节一致**，构建配置无变化。

**问题清单与上次完全一致（20 个），两个 P0 问题已 3 个月未修复。**

### 1.3 今日重点关注（按严重度排序）

- **[TOP 1]** P0-1 HealthIndicator 非标准包路径残留（19 文件 / 29 处 import）——首次发现 2026-06-04，**已 3 个月未修复**
- **[TOP 2]** P0-2 Datascope 旧版类文件未清理（`com/carlos/datascope/` 根包下 10 个旧类仍存在）——首次发现 2026-07-03
- **[TOP 3]** P1-4 SslUtil SSL 校验绕过工具仍处于活跃状态（曾修复被 revert，至今无 `@Deprecated` 标记）

---

## 二、P0 问题重新验证（本次重点复核）

由于核心层无源码变更，本次扫描对两个 P0 及关键 P1 做了全量 grep 复核，确认均**仍未修复**：

### P0-1 HealthIndicator 非标准包路径残留 ✅ 仍存在（数量有变化：19 文件 / 29 处 import）

**复核结果**：`grep -rn "org.springframework.boot.health.contributor"` 命中 **29 处 import**（分布在 19 个文件）。上次报告记录为「19 文件」，本次精确统计为 **19 文件 / 29 处 import**（部分文件同时 import `Health` 与 `HealthIndicator` 两个类）。文件清单：

- `carlos-spring-boot-starter-encrypt`: EncryptHealthIndicator.java, EncryptConfig.java
- `carlos-spring-boot-starter-oss`: OssHealthIndicator.java, OssAutoConfiguration.java
- `carlos-spring-boot-starter-mq`: MqHealthIndicator.java, MqAutoConfiguration.java
- `carlos-spring-boot-starter-mybatis`: MybatisHealthIndicator.java, MyBatisPlusConfig.java
- `carlos-spring-cloud-starter`: ServiceHealthIndicator.java
- `carlos-spring-boot-starter-disruptor`: DisruptorAutoConfiguration.java, DisruptorHealthIndicator.java
- `carlos-spring-boot-starter-flowable`: FlowableHealthIndicator.java, FlowableAutoConfiguration.java
- `carlos-spring-boot-starter-mongodb`: MongoHealthIndicator.java, MongoAutoConfiguration.java
- `carlos-spring-boot-starter-redis`（redis 聚合器内）: RedisHealthIndicator.java, RedisCacheConfig.java
- `carlos-audit-bus`（integration）: AuditAutoConfiguration.java, ClickHouseHealthIndicator.java

**修复方式**（不变，一行命令）：
```bash
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.Health;|org.springframework.boot.actuate.health.Health;|g' {} +
find carlos-spring-boot carlos-integration -name "*.java" -exec sed -i \
  's|org.springframework.boot.health.contributor.HealthIndicator;|org.springframework.boot.actuate.health.HealthIndicator;|g' {} +
```

### P0-2 Datascope 旧版类文件未清理 ✅ 仍存在（根包下 10 个旧类）

**复核结果**：`com/carlos/datascope/` 根包下仍残留旧版类文件，且与新版 `core/aspect/`、`provider/` 目录并行共存：

- 旧版根包（未删除）：`DataScopeAspect.java`、`DataScopeProvider.java`、`DefaultDataScopeProvider.java`、`DataScope.java`、`DataScopes.java`、`DataScopeType.java`、`DataScopeInfo.java`、`DataScopeConsumer.java`、`DataScopeHandler.java`、`CustomScope.java`（共 10 个 .java）
- 新版（正确路径）：`core/aspect/DataScopeAspect.java`、`provider/DataScopeProvider.java`、`provider/DefaultDataScopeProvider.java`

其中 `DataScopeAspect` / `DataScopeProvider` / `DefaultDataScopeProvider` 三个类在旧根包与新版路径下**同时存在**，构成类结构歧义。修复：删除旧根包下 10 个类文件（确认无引用后）。

### P1-4 SslUtil SSL 校验绕过工具 ✅ 仍处于不安全状态

**复核结果**：`SslUtil.java` 中 `IGNORE_SSL_TRUST_MANAGER_X509`（空 X509TrustManager，L22）与 `getIgnoreSslHostnameVerifier()`（`return true`，L59）均仍存在，且**无任何 `@Deprecated` 标记或生产禁用保护**。曾于 2026-06-30 修复（`3be03b06`）后同日 revert（`63a51f90`），已 3 个月处于回退态。

### P1-1 minio Starter 模块 ✅ 仍缺失

`find carlos-spring-boot -maxdepth 1 -iname "*minio*"` 结果为空。当前 `carlos-spring-boot` 下共 24 个模块（22 starters + core + redis 聚合器），无 minio 模块。

---

## 三、完整问题清单（与上次一致，供跟踪）

### P0-Critical（2 个）

| # | 标题 | 模块 | 首次发现 | 状态 |
|---|------|------|---------|------|
| P0-1 | HealthIndicator 非标准包路径残留（19 文件/29 处 import） | 多模块 | 2026-06-04 | ❌ 3 个月未修复 |
| P0-2 | Datascope 旧版类文件未清理（10 类重复） | starter-datascope | 2026-07-03 | ❌ 未修复 |

### P1-High（4 个）

| # | 标题 | 模块 | 类型 |
|---|------|------|------|
| P1-1 | carlos-spring-boot-starter-minio 模块完全缺失 | — | 功能缺失 |
| P1-2 | 全量 Starter 测试覆盖率极低（24 Starter 仅 3 模块有测试） | 全模块 | 测试基础设施 |
| P1-3 | 全量 Starter 缺失 spring-configuration-metadata.json | 全模块 | 技术债务 |
| P1-4 | SslUtil 提供 SSL 证书校验绕过工具（MITM 风险，曾修复被 revert） | carlos-utils | 安全 |

### P2-Medium（11 个）

| # | 标题 | 模块 |
|---|------|------|
| P2-1 | DefaultDataScopeProvider 大量方法仍为 TODO 空实现（return null） | starter-datascope |
| P2-2 | 违反规范使用 RuntimeException（SpelExpressionEngine / OssObjectView） | datascope / oss |
| P2-3 | XssRequestWrapper 无限制 readAllBytes（DoS 风险） | starter-web |
| P2-4 | FlowableProcessService 无限制流读取 | starter-flowable |
| P2-5 | 安全模块部分过滤器默认未启用 | starter-security |
| P2-6 | 部分 Starter 配置前缀命名不一致 | 多模块 |
| P2-7 | 静态工具类持有可变静态状态（含 MultiLevelCacheUtil） | 多模块 |
| P2-8 | Serializable 类缺失 serialVersionUID | 多模块 |
| P2-9 | 公共 API 方法返回 null 而非抛异常/Optional | 多模块 |
| P2-10 | BOM 中存在 SNAPSHOT 依赖声明 | carlos-dependencies |
| P2-11 | MultiLevelCacheUtil get/put 静默忽略 timeout/timeUnit（TTL 不生效） | starter-redis |

### P3-Low（3 个）

| # | 标题 | 模块 |
|---|------|------|
| P3-1 | redisPermissionProviderDelegateInitializer 返回无意义 Object（副作用 Bean） | starter-security |
| P3-2 | AGENTS.md 中 minio 模块描述与代码库实际不一致 | 文档 |
| P3-3 | DefaultMetaObjectHandler.fillTimeField 二次调用 filler.get() | starter-mybatis |

> 各问题的完整代码片段、详细分析、修复方案见上次报告 `framework-scan-report-2026-08-16-0911.md`。核心层源码无变更，所有详情仍然有效。

---

## 四、Starter 健康度评分（无变化，沿用基线）

| 模块 | 设计规范 | 功能完整 | 代码质量 | 安全性 | 综合 |
|------|---------|---------|---------|-------|------|
| carlos-spring-boot-core | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| carlos-spring-boot-starter-web | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-security | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.8/5.0 |
| carlos-spring-boot-starter-mybatis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.0/5.0 |
| carlos-spring-boot-starter-redis | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐☆ | 3.8/5.0 |
| carlos-spring-boot-starter-encrypt | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | 4.5/5.0 |
| carlos-spring-boot-starter-datascope | ⭐⭐⭐☆☆ | ⭐⭐☆☆☆ | ⭐⭐⭐☆☆ | ⭐⭐⭐☆☆ | 2.8/5.0 |
| 其余 17 Starter | — | — | — | — | 未变化 |

> 评分未变化（无源码变更），仅供跟踪。datascope 因 P0-2 + P2-1 + P2-2 三问题叠加评分最低。

---

## 五、待办事项与修复计划

### 5.1 今日新增待办

无（核心层零源码变更，无新问题）。

### 5.2 历史遗留跟踪（跨 23 天，全部未动）

- [ ] **P0-1** HealthIndicator 包路径全局替换 ⏳ **3 个月未修复**（一条 sed 命令即可完成，强烈建议本周执行）
- [ ] **P0-2** 删除 datascope 旧版 10 个类文件 ⏳ **未修复**
- [ ] **P1-4** SslUtil 重新落地生产禁用方案（查清 revert 原因）⏳ **3 个月未修复**
- [ ] P1-1 确认 minio Starter 设计决策 ⏳ 未决
- [ ] P1-3 为核心 Starter 生成配置元数据 JSON ⏳ 未启动
- [ ] P1-2 为核心 Starter（security/redis/web）补充测试 ⏳ 未启动
- [ ] P2-1 实现 DefaultDataScopeProvider 默认读方法 ⏳ 未启动
- [ ] P2-11 MultiLevelCacheUtil 移除误导性 TTL 重载 ⏳ 未启动

---

*本报告由 Hermes 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
*下次扫描: 每日 09:00 (Asia/Shanghai) 自动执行。*
