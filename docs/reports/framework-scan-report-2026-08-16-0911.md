# Carlos Framework 框架核心代码扫描报告

**扫描时间**: 2026-08-16 09:11 (CST)
**扫描 Commit**: `3bad6bfa`（`feat(auth): 添加普通账号密码登录接口并完善认证配置`）
**扫描范围**: carlos-dependencies（BOM）、carlos-commons/carlos-utils、carlos-spring-boot（24 个 Starter）
**对比基线**: 上次扫描 `framework-scan-report-2026-08-15-0913.md`（commit `62cd3d99`）

---

## 一、执行摘要

| 项目                     | 数值                      |
|------------------------|-------------------------|
| 本次发现问题总数               | **20**                  |
| P0-Critical（编译阻断/安全严重） | 2                       |
| P1-High（设计缺陷/功能缺失/安全）  | 4                       |
| P2-Medium（代码质量/技术债务）   | 11                      |
| P3-Low（技术债务/文档）        | 3                       |
| 本次复核后标记已解决             | 1（JsonAutoConfig 三引擎切换） |

**核心结论**：本次扫描距上次仅隔 1 天，远端推进 14 个提交，但框架核心（dependencies/commons/spring-boot）仅 4 个文件变更（+43/-15 行），其余集中在 carlos-auth / carlos-framework-portal / carlos-system 等集成层（不在本次核心扫描范围内）。核心 Starter 的问题清单与上次基本一致：

- **两个 P0 问题仍两月未修复**（HealthIndicator 包路径残留、datascope 旧类未清理）。
- **SslUtil 曾于 2026-06-30 修复后又被 revert**，MITM 风险工具当前仍处于活跃状态，需重点关注。
- **修正一处误报**：上次报告的 P1「JsonAutoConfig 三引擎切换缺陷」经复核，当前代码已通过 `@ConditionalOnProperty(havingValue)` 使三引擎互斥，切换逻辑正确，属误报，已从清单移除。
- **新增 2 个问题**：新增缓存功能 `MultiLevelCacheUtil` 的 TTL 参数被静默忽略（P2）；`DefaultMetaObjectHandler` 日志二次调用时间填充函数（P3）。

---

## 二、P0-Critical（2 个）

### P0-1 HealthIndicator 非标准包路径残留（19 文件 / 10 模块）

- **模块**: mybatis、redis、mongodb、mq、flowable、encrypt、disruptor、oss、spring-cloud-starter、audit-bus
- **文件**: 19 个 `.java`，示例：
    - `carlos-spring-boot/carlos-spring-boot-starter-encrypt/src/main/java/com/carlos/encrypt/health/EncryptHealthIndicator.java`
    - `carlos-spring-boot/carlos-spring-boot-starter-mybatis/src/main/java/com/carlos/datasource/config/MyBatisPlusConfig.java`
    - `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseHealthIndicator.java`
    - （其余 16 个见上次报告清单，本次全量 grep 复核数量仍为 19）
- **问题**: 所有文件 `import org.springframework.boot.health.contributor.HealthIndicator` / `Health`，该包在 Spring Boot 标准体系中不存在，正确包为 `org.springframework.boot.actuate.health.*`。
- **影响**: 项目代码库中未定义该包，标准 Spring Boot 4 环境下这些模块无法编译。
- **修复**: 全局替换 import（一行命令，见上次报告 §P0-1）。首次发现 2026-06-04，已两月未修复，建议本周内完成。

### P0-2 Datascope 旧包路径类文件未清理（3 个类重复）

- **模块**: starter-datascope
- **文件**:
    - 旧版（未删除）：
        - `src/main/java/com/carlos/datascope/DataScopeAspect.java`
        - `src/main/java/com/carlos/datascope/DataScopeProvider.java`
        - `src/main/java/com/carlos/datascope/DefaultDataScopeProvider.java`
    - 新版（正确路径）：
        - `src/main/java/com/carlos/datascope/core/aspect/DataScopeAspect.java`
        - `src/main/java/com/carlos/datascope/provider/DataScopeProvider.java`
        - `src/main/java/com/carlos/datascope/provider/DefaultDataScopeProvider.java`
- **问题**: 旧版 3 个类未删除，与新版路径重复，造成类结构混乱与维护歧义。
- **修复**: 删除旧版 3 个类文件。首次发现 2026-07-03，未修复。

---

## 三、P1-High（4 个）

### P1-1 carlos-spring-boot-starter-minio 模块完全缺失

- **问题**: 代码库中无 minio Starter 目录（`find carlos-spring-boot -maxdepth 1 -name "*minio*"` 结果为空）。AGENTS.md 中却声明存在该模块。可能已合并到 oss Starter。
- **影响**: 依赖 `carlos-spring-boot-starter-minio` 的下游项目无法解析依赖。
- **修复**: 确认设计决策——若已并入 oss，删除文档/依赖中的 minio 引用；否则补建模块。

### P1-2 全量 Starter 测试覆盖率极低（24 Starter 仅 3 模块有测试）

- **问题**: 24 个 Starter 仅 encrypt / integration / migration 三个模块有测试，其余 21 模块零覆盖。核心 starter（security、redis、web）无单测，回归风险高。

### P1-3 全量 Starter 缺失 spring-configuration-metadata.json

- **问题**: 全项目配置元数据 JSON 文件数为 0。IDE 无法对框架自定义配置（`carlos.*`）自动补全与校验。

### P1-4 SslUtil 提供 SSL 证书校验绕过工具（MITM 风险）——曾修复后被 revert

- **模块**: carlos-utils
- **文件**: `carlos-commons/carlos-utils/src/main/java/com/carlos/util/SslUtil.java`
- **问题**: `IGNORE_SSL_TRUST_MANAGER_X509` 为 X509TrustManager 空实现，`getIgnoreSslHostnameVerifier()` 恒返回 true。若误用于生产将导致中间人攻击。
- **重要背景**: 该问题曾于 2026-06-30 修复（commit `3be03b06`「fix(P0-004): SslUtil 重构 SSL 验证，生产环境禁止跳过证书校验」），但同日被 revert（commit `63a51f90`），当前回退到不安全状态。
- **修复**: 重新落地生产禁用方案，或至少标记 `@Deprecated` 并注明仅限测试。

---

## 四、P2-Medium（11 个）

| #     | 标题                                                            | 模块                  | 说明                                                                                                                                                                                                     |
|-------|---------------------------------------------------------------|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P2-1  | DefaultDataScopeProvider 大量 TODO 空实现（return null）             | starter-datascope   | `provider/DefaultDataScopeProvider.java` 11 处 `return null` + TODO（从 SecurityContext 取用户/部门/角色/区域等核心读方法全部未实现），旧版 `datascope/DefaultDataScopeProvider.java` 另有 5 处 `return null`。作为默认 Provider 运行时会静默失败 |
| P2-2  | 违反规范使用 RuntimeException（SpelExpressionEngine / OssObjectView） | datascope / oss     | 编码规范强制禁止原生 RuntimeException                                                                                                                                                                            |
| P2-3  | XssRequestWrapper 无限制 readAllBytes（DoS 风险）                    | starter-web         | 请求体读取无大小上限                                                                                                                                                                                             |
| P2-4  | FlowableProcessService 无限制流读取                                 | starter-flowable    | 流读取无边界限制                                                                                                                                                                                               |
| P2-5  | 安全模块部分过滤器默认未启用                                                | starter-security    | XSS/CSRF 等过滤器默认关闭，安全特性形同虚设                                                                                                                                                                             |
| P2-6  | 部分 Starter 配置前缀命名不一致                                          | 多模块                 | 同一模块内混用 `carlos.xxx` 与 `xxx.blends` 前缀                                                                                                                                                                 |
| P2-7  | 静态工具类持有可变静态状态                                                 | 多模块                 | 含本次新增的 `MultiLevelCacheUtil.localCachedMap`（非 volatile 静态字段）                                                                                                                                           |
| P2-8  | Serializable 类缺失 serialVersionUID                             | 多模块                 | —                                                                                                                                                                                                      |
| P2-9  | 公共 API 方法返回 null 而非抛异常/Optional                               | 多模块                 | —                                                                                                                                                                                                      |
| P2-10 | BOM 中存在 SNAPSHOT 依赖声明                                         | carlos-dependencies | —                                                                                                                                                                                                      |
| P2-11 | **新增** MultiLevelCacheUtil.get/put 静默忽略 timeout/timeUnit      | starter-redis       | `get(key,loader,timeout,timeUnit)` 与 `put(key,value,timeout,timeUnit)` 两个分支均调用 `fastPut`，TTL 参数完全不生效（代码注释亦自认 RLocalCachedMap 不支持单 key TTL）。调用方误以为缓存会过期，实际长期不失效，存在数据陈旧风险                                |

---

## 五、P3-Low（3 个）

| #    | 标题                                                                | 模块               | 说明                                                                                                                                                                                                                         |
|------|-------------------------------------------------------------------|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| P3-1 | redisPermissionProviderDelegateInitializer 返回无意义 Object（副作用 Bean） | starter-security | —                                                                                                                                                                                                                          |
| P3-2 | AGENTS.md 中 minio 模块描述与代码库实际不一致                                   | 文档               | —                                                                                                                                                                                                                          |
| P3-3 | **新增** DefaultMetaObjectHandler.fillTimeField 二次调用 filler.get()   | starter-mybatis  | `fillTimeField` 先 `filler.get()` 填充，随后 `log.debug` 再次调用 `filler.get()`，日志记录的是另一个时间值（`Long` 型 `System::currentTimeMillis` 两次必然不同），日志与真实落库值不一致；另有静态可变 `TIME_FILLER_MAP`/`USER_ID_FILLER_MAP`（HashMap 非线程安全 + 公开 register 方法） |

---

## 六、本次复核后标记已解决（1 个）

| 原编号     | 标题                       | 复核结论                                                                                                                                                                                                                                                                                              |
|---------|--------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 原 P1 #3 | JsonAutoConfig 三引擎切换设计缺陷 | **误报，已移除**。当前 `JsonAutoConfig` 三个引擎 Bean 均带 `@ConditionalOnProperty(havingValue=jackson/fastjson2/gson)`，其中 Jackson 默认 `matchIfMissing=true`。三条件互斥，`engine` 配置为 fastjson2/gson 时 Jackson Bean 因 property 条件不满足而跳过，切换逻辑正确。`@ConditionalOnMissingBean(JsonService.class)` 仅作为用户自定义 Bean 的覆盖保护，不构成阻塞 |

---

## 七、与上次扫描对比

| 维度 | 上次（08-15） | 本次（08-16） | 变化                                |
|----|-----------|-----------|-----------------------------------|
| P0 | 2         | 2         | 无（均两月未修复）                         |
| P1 | 5         | 4         | -1（JsonAutoConfig 误报移除）           |
| P2 | 10        | 11        | +1（MultiLevelCacheUtil TTL 忽略）    |
| P3 | 2         | 3         | +1（DefaultMetaObjectHandler 二次填充） |
| 合计 | 19        | 20        | +1                                |

**核心 Starter 变更**（自 62cd3d99 起，仅 4 文件）：

1. `MultiLevelCacheUtil.java` +23 行（新增 `evictByPrefix` 方法）
2. `starter-web/logback.xml` 22 行（logback 启动警告修复）
3. `starter-apm/logback-skywalking.xml` 10 行（logback 启动警告修复）
4. `DefaultMetaObjectHandler.java` +3 行（insertFill 补充 updateUser 填充）

其余 14 个新提交集中在 carlos-auth（P1/P2 修复×8）、carlos-framework-portal、carlos-system 等集成层，不在本次核心扫描范围。

---

## 八、操作建议

### 紧急（本周）

1. **P0-1** HealthIndicator 包路径全局替换（19 文件，一条 sed 命令）
2. **P0-2** 删除 datascope 旧版 3 个类文件
3. **P1-4** SslUtil 重新落地生产禁用方案（曾修复被 revert，需查清 revert 原因）

### 计划（本月）

4. **P1-1** 确认 minio Starter 设计决策
5. **P1-3** 为核心 Starter 生成配置元数据 JSON
6. **P1-2** 为核心 Starter（security/redis/web）补充测试
7. **P2-1** 实现 DefaultDataScopeProvider 默认读方法（从 SecurityContext 取用户/部门/角色）
8. **P2-11** MultiLevelCacheUtil 移除误导性 TTL 重载或改为抛异常/文档说明

---

*报告生成: 2026-08-16 09:11 CST*
*下次扫描: 每日 09:00 (Asia/Shanghai) 自动执行*
