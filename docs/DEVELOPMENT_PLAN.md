# Carlos Framework 系统性开发计划

> **制定日期**: 2026-06-10
> **适用版本**: 3.0.0-SNAPSHOT
> **技术栈**: Spring Boot 4.0.6 + Spring Cloud 2025.1.1 + JDK 21
> **状态**: 待评审

---

## 核心三问

### 当前最紧迫要做的事是什么？

**立即修复框架核心层的 P0 级问题（10 个编译阻断 + 安全漏洞）**，特别是：

1. **编译阻断问题**：4 个模块 HealthIndicator 包名错误（P0-001）、MQ 模块 Jackson 包名错误（P0-002）
2. **高危安全漏洞**：MinIO 凭证泄露（P0-003）、Datascope SpEL 注入（P0-004）
3. **资源泄漏**：Snowflake 线程泄漏（P0-006）、AI ChatMemory 内存泄漏（P0-010）

### 为什么要按这个顺序做？

遵循 **"框架可用性 → 核心功能 → 规范质量 → 架构增强"** 的优先级原则：

1. **P0 问题不修复，框架无法编译或运行**：HealthIndicator 和 Jackson 包名错误直接导致 5 个 Starter
   模块无法通过编译，任何基于这些模块的应用都无法构建。
2. **安全漏洞必须在功能开发前封堵**：MinIO 凭证泄露和 SpEL 注入是生产环境的致命风险，如果在业务功能开发期间暴露，可能导致安全事故。
3. **业务模块依赖框架核心**：统一权限管理改造（125h 大需求）需要框架核心层稳定可靠。如果底层 Starter
   存在内存泄漏、缓存数据丢失、配置绑定失败等问题，上层业务功能将建立在不稳定的基础上。
4. **修复率 0% 的现状必须打破**：当前所有历史遗留问题修复率为 0%，如果不建立"先清债再前进"的纪律，技术债务将持续累积到不可控的程度。

### 如果不按这个顺序会发生什么？

- **场景 A（跳过 P0 直接做统一权限管理）**：开发过程中发现 MQ/MinIO/Disruptor/MongoDB 模块编译失败，被迫中断业务开发去修编译错误；同时
  MinIO 凭证泄露在开发测试期间被内部/外部发现，造成安全事件。
- **场景 B（跳过框架修复直接做业务模块）**：carlos-auth 的 InMemoryRegisteredClientRepository 问题导致集群部署失败；carlos-audit
  的 TODO 空壳导致审计系统无法交付；手写 SQL XML 问题在代码评审时被反复打回，浪费大量返工时间。
- **场景 C（多方向并行切换）**：当前的真实困境——开发者在框架修复、业务完善、前端开发、组件增强之间频繁切换，每个方向都只做了一半，没有任何一个里程碑彻底完成，团队士气下降，交付承诺无法兑现。

---

## 总体路线图（5 个阶段）

| 阶段          | 名称   | 周期         | 核心目标                       | 产出物                  | 完成标准                              |
|-------------|------|------------|----------------------------|----------------------|-----------------------------------|
| **Phase 1** | 框架止血 | Week 1     | 修复所有 P0 级问题，恢复编译和安全基线      | 修复后的 13 个 Starter 模块 | `mvn clean install` 全量通过，安全扫描无 P0 |
| **Phase 2** | 框架稳固 | Week 2-3   | 修复 P1+P2 问题，补齐配置/文档一致性     | 稳定的核心框架层             | 单元测试覆盖率 >= 50%，配置与文档一致            |
| **Phase 3** | 业务核心 | Week 4-6   | 修复业务模块 P1 问题，完成审计查询 + 认证集群 | 可运行的业务模块             | 审计查询可用，认证支持集群                     |
| **Phase 4** | 统一权限 | Week 7-10  | 执行已规划的统一权限管理改造             | 完整的权限管理后端            | 动态路由 + 按钮权限 + API 扫描              |
| **Phase 5** | 架构增强 | Week 11-12 | 组件特性利用 + 前端对接 + 质量巩固       | 框架能力升级               | 延时队列、多租户、MapStruct 优化落地           |

---

## Phase 1: 框架止血（Week 1）

**目标**：修复所有 P0 级问题，恢复框架编译能力和安全基线。

**切换规则**：Phase 1 结束前，**禁止**启动任何业务模块开发、禁止开始统一权限改造、禁止前端新功能开发。

### 任务清单

| 任务编号        | 描述                                                                        | 涉及模块                                                          | 前置依赖           | 验收标准                                         | 预估工时 |
|-------------|---------------------------------------------------------------------------|---------------------------------------------------------------|----------------|----------------------------------------------|------|
| P0-FIX-001  | 修复 4 个模块 HealthIndicator 包名错误 (`health.contributor` -> `actuator.health`) | starter-minio, starter-mongodb, starter-mq, starter-disruptor | -              | 编译通过，`@ConditionalOnClass` 正常生效              | 1h   |
| P0-FIX-002  | 修复 MQ 模块 Jackson 包名错误 (`tools.jackson` -> `com.fasterxml.jackson`)        | starter-mq                                                    | -              | MQ 模块编译通过                                    | 1h   |
| P0-FIX-003  | 移除 MinIO `/minio/init` 端点的凭证泄露                                            | starter-minio                                                 | -              | 端点不再返回 accessKey/secretKey，仅返回非敏感信息          | 1h   |
| P0-FIX-004  | 修复 Datascope SpEL 注入风险，替换 StandardEvaluationContext                       | starter-datascope                                             | -              | 使用 SimpleEvaluationContext 或禁用 TypeLocator   | 3h   |
| P0-FIX-005  | 修复 Datascope HexValue 误用，改为正确 SQL 条件注入                                    | starter-datascope                                             | P0-FIX-004     | SQL 条件正确拼接，JSqlParser 无异常                    | 2h   |
| P0-FIX-006  | 修复 Snowflake 线程泄漏 + NPE + 静态 key 竞争                                       | starter-snowflake                                             | -              | 实例变量替代 static，修复逻辑运算符，线程池正确关闭                | 3h   |
| P0-FIX-007  | 修复 Translation 缓存 batchGet 数据丢失                                           | starter-translation                                           | -              | L1 + L2 缓存合并正确，不丢失数据                         | 1h   |
| P0-FIX-008  | 修复 Translation Redis 强依赖但 marked provided                                 | starter-translation                                           | P0-FIX-007     | 提供纯 Caffeine 回退实现或调整依赖 scope                 | 2h   |
| P0-FIX-009  | 修复 Datacenter GET body 逻辑颠倒                                               | starter-datacenter                                            | -              | `isNotBlank` 逻辑正确                            | 0.5h |
| P0-FIX-010  | 修复 AI ChatMemory 内存泄漏，添加 TTL/LRU                                          | starter-ai                                                    | -              | 使用 Caffeine/Guava Cache 替代 ConcurrentHashMap | 2h   |
| P0-TEST-001 | 全量编译验证                                                                    | 所有模块                                                          | P0-FIX-001~010 | `mvn clean install` 成功                       | 1h   |
| P0-TEST-002 | 安全扫描复查                                                                    | 所有 Starter                                                    | P0-FIX-001~010 | 无 P0 级安全问题                                   | 1h   |

**Phase 1 总工时**：约 18.5h（建议 3-4 工作日完成）

---

## Phase 2: 框架稳固（Week 2-3）

**目标**：修复 P1+P2 问题，补齐配置与文档一致性，建立测试基线。

**切换规则**：Phase 2 结束前，**禁止**修改任何业务模块代码（除非发现框架问题阻塞业务编译）。

### 任务清单

| 任务编号        | 描述                                          | 涉及模块                      | 前置依赖           | 验收标准                                                       | 预估工时 |
|-------------|---------------------------------------------|---------------------------|----------------|------------------------------------------------------------|------|
| P1-FIX-001  | 修复 OpenAPI 配置前缀不匹配                          | starter-openapi           | -              | `@ConditionalOnProperty` 与 `@ConfigurationProperties` 前缀一致 | 0.5h |
| P1-FIX-002  | 修复 MinIO/MQ 配置类构造函数注入自身 Bean                | starter-minio, starter-mq | -              | 移除 `@RequiredArgsConstructor`，使用方法参数注入                     | 1h   |
| P1-FIX-003  | 补齐 AI 配置属性或更新 README                        | starter-ai                | -              | 配置属性与 README 一致                                            | 4h   |
| P1-FIX-004  | 修复 APM SleuthConfig 手动构建 brave.Tracing 冲突   | starter-apm               | -              | 移除手动构建，改用 Spring Boot 原生配置                                 | 2h   |
| P1-FIX-005  | 修复 APM Logback 配置不兼容 1.5.x                  | starter-apm               | -              | 使用 `SizeAndTimeBasedRollingPolicy`                         | 1h   |
| P1-FIX-006  | 修复 MQ RabbitMQ 默认凭证不安全                      | starter-mq                | -              | 默认值设为 null，强制用户配置                                          | 0.5h |
| P1-FIX-007  | 修复 Snowflake `isUseSystemClock` 命名歧义        | starter-snowflake         | P0-FIX-006     | 重命名为 `useSystemClock`                                      | 1h   |
| P1-FIX-008  | 修复 Translation 继承浪费，改为组合                    | starter-translation       | P0-FIX-007~008 | 父类 Caffeine 缓存不再浪费                                         | 2h   |
| P1-FIX-009  | 修复 Translation `isBasicType()` 遗漏 jakarta.* | starter-translation       | P0-FIX-007~008 | 添加 jakarta 包名前缀判断                                          | 0.5h |
| P1-FIX-010  | 修复 Disruptor 忙等待 CPU 飙升                     | starter-disruptor         | P0-FIX-001     | 引入指数退避                                                     | 2h   |
| P1-FIX-011  | 修复 Disruptor registerCapacityMetrics 线程泄漏   | starter-disruptor         | P0-FIX-001     | 使用 Spring TaskScheduler，`@PreDestroy` 关闭                   | 1h   |
| P1-FIX-012  | 修复 AI 流式回调异常处理                              | starter-ai                | P0-FIX-010     | 不抛出异常，通过回调通知                                               | 1h   |
| P1-FIX-013  | 修复 APM 多个配置属性未使用                            | starter-apm               | P1-FIX-004~005 | 移除未使用属性或实现对应功能                                             | 3h   |
| P2-FIX-001  | 修复 aspectjweaver scope 为 provided           | starter-apm               | P1-FIX-004     | scope 改为 compile 或提供文档说明                                   | 0.5h |
| P2-FIX-002  | 修复 propagationType List 但只检查单一值             | starter-apm               | P1-FIX-004     | 支持多值配置                                                     | 1h   |
| P2-FIX-003  | 修复 logback 重复定义                             | starter-apm               | P1-FIX-005     | 日志格式不覆盖                                                    | 0.5h |
| P2-FIX-004  | 补齐 AI 其他 4 个提供商依赖                           | starter-ai                | P1-FIX-003     | 至少引入 anthropic/ollama/local-ai 等                           | 2h   |
| P2-FIX-005  | 修复 Translation `catch Throwable`            | starter-translation       | P1-FIX-008~009 | 改为 `catch Exception`                                       | 0.5h |
| P2-FIX-006  | 修复 Translation `redisExpireMinutes` 未生效     | starter-translation       | P1-FIX-008~009 | 配置正确读取并生效                                                  | 1h   |
| P2-FIX-007  | 修复 datacenter 配置松散                          | starter-datacenter        | P0-FIX-009     | 使用强类型配置类                                                   | 2h   |
| P2-FIX-008  | 修复 datacenter 日志泄露                          | starter-datacenter        | P0-FIX-009     | 敏感字段脱敏后输出                                                  | 0.5h |
| P2-FIX-009  | 修复未使用 `@AutoConfiguration`                  | starter-datacenter        | P0-FIX-009     | 符合 Spring Boot 3.x 最佳实践                                    | 0.5h |
| P2-FIX-010  | 修复两个同名 `DataScopeProperties`                | starter-datascope         | P0-FIX-004~005 | 合并或重命名，消除冲突                                                | 1h   |
| P2-FIX-011  | 修复 snowflake `getSnowflake()` 漏掉节点 31       | starter-snowflake         | P1-FIX-007     | 循环条件改为 `i <= 31`                                           | 0.5h |
| P2-TEST-001 | 为修复的模块补充单元测试                                | 多个 Starter                | -              | 核心逻辑覆盖率 >= 50%                                             | 8h   |
| P2-DOC-001  | 同步 AGENTS.md / CLAUDE.md 版本矩阵               | docs                      | -              | 与 carlos-dependencies/pom.xml 一致                           | 2h   |

**Phase 2 总工时**：约 39h（建议 8-10 工作日完成，可与 Phase 1 部分并行）

---

## Phase 3: 业务核心修复（Week 4-6）

**目标**：修复业务模块 P1 级问题，使核心业务能力可用。

**切换规则**：Phase 3 开始前，必须完成 Phase 1 的所有 P0 修复并通过编译。Phase 3 结束前，**禁止**开始统一权限管理的数据库重构。

### 任务清单

| 任务编号         | 描述                                                | 涉及模块                                                 | 前置依赖       | 验收标准                                                     | 预估工时 |
|--------------|---------------------------------------------------|------------------------------------------------------|------------|----------------------------------------------------------|------|
| BIZ-P1-001   | 替换 `InMemoryRegisteredClientRepository` 为数据库持久化实现 | carlos-auth-service                                  | Phase 1 完成 | 集群部署客户端信息不丢失，支持动态注册                                      | 8h   |
| BIZ-P1-002   | 实现 `AuditLogQueryService` 4 个核心方法                 | carlos-audit-bus                                     | Phase 1 完成 | pageQuery/getById/getRealtimeStats/listRiskEvents 返回正确数据 | 12h  |
| BIZ-P1-003   | 迁移剩余手写 SQL XML 为 MyBatis-Plus API                 | carlos-system-bus, carlos-org-bus                    | Phase 1 完成 | 所有 XML 迁移完成或经评审保留，CI 禁止新增 XML                            | 16h  |
| BIZ-P2-001   | 修复验证码发送失败仍返回 true                                 | carlos-auth-service                                  | -          | 发送失败返回 false 并删除已保存验证码                                   | 2h   |
| BIZ-P2-002   | 实现邮箱验证码发送                                         | carlos-auth-service                                  | -          | 集成 spring-boot-starter-mail                              | 4h   |
| BIZ-P2-003   | 修复 SysRegionManagerImpl 缓存失效                      | carlos-system-bus                                    | -          | 更新后缓存正确失效                                                | 2h   |
| BIZ-P2-004   | 取消 ExtendAuthenticationProvider 事件发布注释            | carlos-auth-service                                  | -          | 认证成功/失败事件正常发布                                            | 1h   |
| BIZ-P2-005   | 配置审计数据保留策略（TTL）                                   | carlos-audit-bus                                     | BIZ-P1-002 | ClickHouse 表添加 TTL，支持配置化保留天数                             | 3h   |
| BIZ-P2-006   | 增加验证码防重放防刷机制                                      | carlos-auth-service                                  | BIZ-P2-001 | 请求签名 + IP 限制 + 图形验证码前置                                   | 6h   |
| BIZ-TEST-001 | 业务模块单元测试 + 集成测试                                   | carlos-auth, carlos-audit, carlos-system, carlos-org | -          | 核心 Service 覆盖率 >= 60%                                    | 12h  |

**Phase 3 总工时**：约 66h（建议 15 工作日完成）

---

## Phase 4: 统一权限管理改造（Week 7-10）

**目标**：执行已规划的统一权限管理改造（基于已有需求文档 `docs/统一权限管理-需求清单与研发计划.md`）。

**切换规则**：Phase 4 开始前，必须完成 Phase 3 的所有 P1 修复。Phase 4 按已有里程碑执行，不扩展新需求。

### 任务清单（基于已有需求文档精简）

| 任务编号        | 描述                                                                            | 涉及模块                            | 前置依赖        | 验收标准                 | 预估工时 |
|-------------|-------------------------------------------------------------------------------|---------------------------------|-------------|----------------------|------|
| PERM-M1-001 | 新建 `sys_operation` 和 `org_role_operation` 表                                   | 数据库                             | Phase 3 完成  | DDL 正确，索引完整          | 3h   |
| PERM-M1-002 | 迁移 `sys_menu_operate` 历史数据                                                    | 数据库                             | PERM-M1-001 | 数据迁移完成，perm_code 唯一  | 4h   |
| PERM-M1-003 | 删除旧表 `sys_menu_operate` / `sys_resource` / `sys_resource_category`            | 数据库                             | PERM-M1-002 | 旧表安全删除               | 2h   |
| PERM-M2-001 | 删除 `system.resource` 包全部代码                                                    | carlos-system-bus               | PERM-M1-003 | 编译通过，无残留引用           | 2h   |
| PERM-M2-002 | `SysOperation` 全套 CRUD（Entity/DTO/Param/VO/Controller/Service/Manager/Mapper） | carlos-system-bus               | PERM-M1-001 | 接口通，Swagger 正常       | 12h  |
| PERM-M2-003 | `/operation/scan` 自动扫描接口                                                      | carlos-system-bus               | PERM-M2-002 | 扫描正确，重复不覆盖           | 6h   |
| PERM-M2-004 | `/route/getUserRoutes` + `/route/getConstantRoutes`                           | carlos-system-bus               | -           | 返回正确路由结构             | 3h   |
| PERM-M2-005 | `/role/{roleId}/operations` 角色权限接口                                            | carlos-system-bus               | PERM-M2-002 | 授权保存/查询正确            | 4h   |
| PERM-M2-006 | `/user/info` 增强返回 buttons + operations                                        | carlos-system-bus / carlos-auth | PERM-M2-005 | 返回字段完整               | 3h   |
| PERM-M2-007 | JWT Token 精简                                                                  | carlos-auth                     | -           | Token 体积减小，关键信息保留    | 2h   |
| PERM-M2-008 | `PermissionService` 缓存优化（Redis + Caffeine）                                    | starter-security                | PERM-M2-005 | 鉴权耗时 < 5ms           | 6h   |
| PERM-M2-009 | `CarlosPermissionEvaluator` 适配新表                                              | starter-security                | PERM-M2-008 | `@PreAuthorize` 正常工作 | 3h   |
| PERM-M3-001 | 前端动态路由对接                                                                      | carlos-ui                       | PERM-M2-004 | 动态路由渲染正确             | 4h   |
| PERM-M3-002 | 前端按钮权限（v-auth 指令 + hasButton）                                                 | carlos-ui                       | PERM-M2-006 | 无权限按钮不显示             | 3h   |
| PERM-M3-003 | 前端权限管理页面                                                                      | carlos-ui                       | PERM-M2-002 | 列表/筛选/展示正确           | 6h   |
| PERM-M3-004 | 前端 API 扫描页面                                                                   | carlos-ui                       | PERM-M2-003 | 扫描/差异/批量入库           | 5h   |
| PERM-M3-005 | 前端统一权限树（角色权限分配）                                                               | carlos-ui                       | PERM-M2-005 | 树形展示/勾选/回显正确         | 8h   |
| PERM-M4-001 | 端到端测试                                                                         | 全链路                             | PERM-M3-005 | 全流程通过                | 6h   |
| PERM-M4-002 | 性能测试（登录/鉴权耗时）                                                                 | 全链路                             | PERM-M2-008 | 满足 NFR-001/002       | 2h   |
| PERM-M4-003 | 回归测试                                                                          | 全链路                             | -           | 旧功能正常                | 4h   |

**Phase 4 总工时**：约 86h（建议 20 工作日完成，后端前端并行）

---

## Phase 5: 架构增强与质量巩固（Week 11-12）

**目标**：利用组件特性、前端对接完成、建立长期质量机制。

**切换规则**：Phase 5 开始前，必须完成 Phase 4 的联调测试。

### 任务清单

| 任务编号     | 描述                                           | 涉及模块                       | 前置依赖       | 验收标准                      | 预估工时 |
|----------|----------------------------------------------|----------------------------|------------|---------------------------|------|
| ARCH-001 | 引入 Redisson RDelayedQueue 实现延时消息             | carlos-message             | Phase 4 完成 | 替代定时任务轮询                  | 6h   |
| ARCH-002 | 使用 MapStruct 替换关键路径的 BeanUtil.copyProperties | 全项目                        | Phase 4 完成 | 至少替换 10 处高频调用             | 4h   |
| ARCH-003 | 启用 MyBatis-Plus 多租户插件                        | carlos-org / carlos-system | Phase 4 完成 | 消除手动 tenant_id 条件         | 8h   |
| ARCH-004 | 使用 Hutool DesensitizedUtil 替换自定义脱敏           | carlos-auth / carlos-org   | Phase 4 完成 | 敏感数据脱敏统一                  | 2h   |
| ARCH-005 | 前端按钮权限全量替换                                   | carlos-ui                  | Phase 4 完成 | 所有页面按钮接入 v-auth           | 4h   |
| ARCH-006 | 补充各模块 docs 目录                                | 所有模块                       | -          | 模板填充或移除                   | 4h   |
| ARCH-007 | carlos-gateway 限流熔断配置文档                      | carlos-gateway             | -          | README 含配置说明              | 2h   |
| ARCH-008 | carlos-license 生产环境安全警告                      | carlos-license             | -          | README 明确标注生产禁用 generate  | 1h   |
| ARCH-009 | 建立 CI 检查（禁止新增 XML Mapper、TODO 阈值）            | CI/CD                      | -          | Jenkins/GitHub Actions 配置 | 4h   |
| ARCH-010 | 全量单元测试覆盖率达标                                  | 全项目                        | -          | 核心模块 >= 80%               | 16h  |

**Phase 5 总工时**：约 51h（建议 10 工作日完成）

---

## 执行纪律

### 每日纪律

1. **晨会 15 分钟**：昨日完成什么、今日计划、阻塞问题
2. **下班前提交**：当天修复的问题必须提交，commit message 遵循规范
3. **问题不隔夜**：P0 级问题发现后 4 小时内必须修复或升级上报

### 每周纪律

1. **周一规划**：明确本周要完成的 Task 编号，写入周报
2. **周五复盘**：检查本周计划完成率，未完成的 Task 分析原因并调整下周计划
3. **周末扫描**：运行全量代码扫描，更新问题清单

### 阶段切换规则（强制）

- **Phase N 结束标准**：该阶段所有 Task 标记为完成，并通过该阶段的验收标准
- **切换审批**：进入下一阶段前，需要代码评审通过 + 测试通过 + 负责人确认
- **禁止越级**：不允许在 Phase 1 未完成时启动 Phase 2 的任务（紧急安全补丁除外）
- **例外流程**：如发现新的 P0 级问题，无论当前处于哪个阶段，必须立即暂停当前工作修复 P0

---

## 风险矩阵

| 风险                 | 影响 | 可能性 | 应对措施                         |
|--------------------|----|-----|------------------------------|
| P0 修复引入新的编译错误      | 高  | 中   | 每个 P0 修复后单独编译验证，不批量合并        |
| 统一权限改造数据库迁移数据丢失    | 高  | 低   | 迁移前完整备份，新旧表并行运行至少 1 周        |
| 业务模块 P1 修复影响现有功能   | 中  | 中   | 每个修复必须有对应的回归测试               |
| 前端/后端进度不匹配导致阻塞     | 中  | 高   | Phase 4 后端先完成 M2 再让前端介入 M3   |
| 组件特性利用（如多租户）影响现有查询 | 中  | 中   | ARCH-003 在独立分支验证，通过后再合并      |
| 团队人员变动导致计划中断       | 高  | 低   | 每个 Task 必须有文档记录，代码注释完整       |
| 需求变更导致统一权限改造范围膨胀   | 高  | 中   | 严格按已有需求文档执行，新需求排入 Phase 5 之后 |

---

## 进度跟踪机制

### 可视化看板

使用 GitHub Issues / GitLab Issues 或 Markdown 看板，按以下列管理：

```
待启动 | 进行中 | 待评审 | 已完成 | 阻塞
```

### 每周报告模板

```markdown
## Week X 进度报告

### 计划完成
- [ ] Task-001 (预估 3h, 实际 Xh)
- [ ] Task-002 (预估 2h, 实际 Xh)

### 实际完成
- [x] Task-001
- [ ] Task-002 (完成 50%, 阻塞原因: XXX)

### 新增问题
- P0-XXX: 描述 (发现日期, 处理中/已修复)

### 风险升级
- 无 / XXX 风险可能性从低升为高

### 下周计划
- Task-003, Task-004, Task-005
```

---

## 附录：问题清单索引

### 框架核心层问题（47 个）

#### P0-Critical（10 个）

| 编号     | 模块                         | 问题                         | 状态                    |
|--------|----------------------------|----------------------------|-----------------------|
| P0-001 | minio/mongodb/mq/disruptor | HealthIndicator 包名错误       | 待修复                   |
| P0-002 | mq                         | Jackson 包名错误               | 待修复                   |
| P0-003 | minio                      | MinIO 凭证泄露                 | 待修复                   |
| P0-004 | datascope                  | SpEL 注入风险                  | 待修复                   |
| P0-005 | datascope                  | HexValue 误用 SQL 条件注入       | 待修复                   |
| P0-006 | snowflake                  | 线程泄漏 + NPE + 静态 key 竞争     | 待修复                   |
| P0-007 | translation                | 缓存 batchGet 数据丢失           | 已修复 (commit b72eaccf) |
| P0-008 | translation                | Redis 强依赖但 marked provided | 已修复 (commit b72eaccf) |
| P0-009 | datacenter                 | GET body 逻辑颠倒              | 待修复                   |
| P0-010 | ai                         | ChatMemory 内存泄漏            | 已修复 (commit a602c15c) |

#### P1-High（14 个）

| 编号     | 模块          | 问题                                     | 状态                    |
|--------|-------------|----------------------------------------|-----------------------|
| P1-001 | openapi     | @ConditionalOnProperty 前缀不匹配           | 待修复                   |
| P1-002 | minio/mq    | 配置类构造函数注入自身 Bean                       | 待修复                   |
| P1-003 | ai          | CarlosAiProperties 与 README 严重不匹配      | 待修复                   |
| P1-004 | apm         | SleuthConfig 手动构建 brave.Tracing 冲突     | 待修复                   |
| P1-005 | apm         | Logback 配置不兼容 1.5.x                    | 待修复                   |
| P1-006 | mq          | RabbitMQ 默认凭证不安全                       | 待修复                   |
| P1-007 | snowflake   | isUseSystemClock 命名导致绑定歧义              | 待修复                   |
| P1-008 | translation | RedissonTranslationCacheManager 继承浪费   | 已修复 (commit b72eaccf) |
| P1-009 | translation | NestedField.isBasicType() 遗漏 jakarta.* | 已修复 (commit 168074ee) |
| P1-010 | disruptor   | DisruptorTemplate 忙等待 CPU 飙升           | 已修复 (commit a65c1e91) |
| P1-011 | disruptor   | registerCapacityMetrics 资源泄漏           | 已修复 (commit be1eff44) |
| P1-012 | ai          | 流式回调异常处理不当                             | 待修复                   |
| P1-013 | openapi     | 配置前缀不匹配（同 P1-001）                      | 待修复                   |
| P1-014 | apm         | 多个配置属性未使用                              | 待修复                   |

#### P2-Medium（14 个）

| 编号     | 模块          | 问题                                            | 状态                    |
|--------|-------------|-----------------------------------------------|-----------------------|
| P2-001 | apm         | aspectjweaver scope 为 provided                | 待修复                   |
| P2-002 | apm         | propagationType List 但只检查单一值                  | 待修复                   |
| P2-003 | apm         | logback-skywalking.xml CONSOLE_LOG_PATTERN 重复 | 待修复                   |
| P2-004 | ai          | 只引入 openai 依赖                                 | 待修复                   |
| P2-005 | ai          | AiDocumentServiceImpl 未限制输入流大小                | 待修复                   |
| P2-006 | translation | TranslationAspect 双重切点                        | 已修复 (commit 8e59aca2) |
| P2-007 | translation | catch Throwable 吞掉 Error                      | 已修复 (commit 168074ee) |
| P2-008 | translation | redisExpireMinutes 未生效                        | 已修复 (commit 168074ee) |
| P2-009 | translation | TranslationAspect 切点过于宽泛                      | 已修复 (commit 8e59aca2) |
| P2-010 | datacenter  | 配置结构极度松散                                      | 待修复                   |
| P2-011 | datacenter  | afterPropertiesSet 打印完整配置泄露凭据                 | 待修复                   |
| P2-012 | datacenter  | 未使用 @AutoConfiguration                        | 待修复                   |
| P2-013 | datascope   | 两个同名 DataScopeProperties                      | 待修复                   |
| P2-014 | snowflake   | getSnowflake() 漏掉节点 31                        | 待修复                   |

#### P3-Low（9 个）

| 编号     | 模块        | 问题                                 | 状态  |
|--------|-----------|------------------------------------|-----|
| P3-001 | apm       | SleuthConfig 包名仍使用 sleuth          | 待修复 |
| P3-002 | apm       | MdcUtil.clear() 不清理自定义 key         | 待修复 |
| P3-003 | ai        | aiChatService 参数名残留旧版              | 待修复 |
| P3-004 | mongodb   | 命名残留 MyBatis 痕迹                    | 待修复 |
| P3-005 | mq        | MqUtil 静态字段持有 MqTemplate           | 待修复 |
| P3-006 | mq        | KafkaMqClient 反射调用 lambda 复杂       | 待修复 |
| P3-007 | minio     | publicEndpoint 默认空字符串              | 待修复 |
| P3-008 | minio     | MinIO SDK 版本 8.5.7 偏旧              | 待修复 |
| P3-009 | snowflake | SnowflakeLocalCacheManager IP 哈希冲突 | 待修复 |

### 业务模块问题（13 个）

#### P1-High（3 个）

| 编号     | 模块                    | 问题                                       | 状态                                                                                    |
|--------|-----------------------|------------------------------------------|---------------------------------------------------------------------------------------|
| P1-001 | carlos-auth-service   | InMemoryRegisteredClientRepository 不支持集群 | 待修复                                                                                   |
| P1-002 | carlos-audit-bus      | AuditLogQueryService 4 个核心方法为 TODO       | 待修复                                                                                   |
| P1-003 | carlos-system/org-bus | 手写 SQL XML 文件违反规范                        | 部分修复 (SysDictMapper, SysDictItemMapper, OrgPermissionMapper, OrgDepartmentMapper 已转换) |

#### P2-Medium（6 个）

| 编号     | 模块                  | 问题                                   | 状态  |
|--------|---------------------|--------------------------------------|-----|
| P2-001 | carlos-auth-service | 验证码发送失败仍返回 true                      | 待修复 |
| P2-002 | carlos-auth-service | 邮箱验证码发送未实现                           | 待修复 |
| P2-003 | carlos-system-bus   | SysRegionManagerImpl FIXME 缓存失效      | 待修复 |
| P2-004 | carlos-auth-service | ExtendAuthenticationProvider 事件发布被注释 | 待修复 |
| P2-005 | carlos-audit-bus    | 审计数据保留策略未配置                          | 待修复 |
| P2-006 | carlos-auth-service | 验证码防重放防刷缺失                           | 待修复 |

#### P3-Low（4 个）

| 编号     | 模块             | 问题              | 状态  |
|--------|----------------|-----------------|-----|
| P3-001 | 所有模块           | docs 目录模板未填充    | 待修复 |
| P3-002 | carlos-tools   | GitLab TODO 未清理 | 待修复 |
| P3-003 | carlos-gateway | 缺少限流熔断配置文档      | 待修复 |
| P3-004 | carlos-license | 缺少生产环境注意事项      | 待修复 |

---

## 工作量汇总

| 阶段            | 任务数    | 预估总工时      | 建议周期             |
|---------------|--------|------------|------------------|
| Phase 1: 框架止血 | 12     | 18.5h      | 3-4 工作日          |
| Phase 2: 框架稳固 | 25     | 39h        | 8-10 工作日         |
| Phase 3: 业务核心 | 10     | 66h        | 15 工作日           |
| Phase 4: 统一权限 | 20     | 86h        | 20 工作日           |
| Phase 5: 架构增强 | 10     | 77         | 10 工作日           |
| **合计**        | **77** | **260.5h** | **约 12 周（3 个月）** |

**建议团队配置**：

- 后端开发 2 人（负责 Phase 1-3 + Phase 4 后端）
- 前端开发 1 人（负责 Phase 4 前端，Week 7 介入）
- 测试/QA 1 人（负责 Phase 3 起介入测试）

---

## 参考文档

| 文档                                                       | 说明                |
|----------------------------------------------------------|-------------------|
| `docs/reports/framework-scan-report-2026-06-10-2.md`     | 框架核心扫描报告（47 个问题）  |
| `docs/reports/business-scan-report-2026-06-09-1.md`      | 业务模块质量报告（13 个问题）  |
| `docs/reports/component-analysis-report-2026-06-09-1.md` | 组件特性利用分析（28 条待利用） |
| `docs/统一权限管理-需求清单与研发计划.md`                               | 统一权限管理需求文档        |
| `CLAUDE.md`                                              | 项目架构与开发规范         |
| `.claude/skills/carlos-framework-standard/SKILL.md`      | 编码规范（必读）          |
