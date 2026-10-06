# Carlos Framework 框架核心代码每日扫描任务

你是 Carlos Framework 专属开发助手。站在顶级架构师的角度，请执行框架核心代码层的每日深度扫描任务，覆盖依赖管理、构建配置、通用工具、Spring
Boot Starters。

---

## 任务目标

从顶级架构师的角度，对 `carlos-framework` 项目中**框架核心代码层**进行全量代码扫描，生成《框架核心代码评估报告》，保存到：

```
docs/reports/framework-scan-report-$(date +%Y-%m-%d)-N.md
```

---

## 执行步骤

### 1. 代码同步与环境准备

```bash
cd /home/carlos/.openclaw/agents/carlos-dev-assistant/workspace/carlos-framework
# 尝试拉取最新代码（网络可能不稳定）
git pull origin main 2>/dev/null || echo "[WARN] git pull 失败，使用本地代码继续扫描"
git rev-parse HEAD
git log --oneline -5
```

---

### 2. 扫描模块范围（仅限框架核心代码层）

> **扫描原则**：以下所有模块必须逐一扫描，不允许跳过任何模块。父 POM 关注依赖管理与构建配置，子模块关注功能设计、技术设计、代码质量与业务逻辑。

#### 核心代码层

| 模块                    | 扫描重点                                   |
|-----------------------|----------------------------------------|
| `carlos-dependencies` | BOM 依赖版本矩阵、版本冲突、过期依赖、已知 CVE            |
| `carlos-parent`       | 插件版本、构建配置、flatten-maven-plugin 配置、分发管理 |

#### Commons 通用基础层

| 模块                   | 扫描重点                                         |
|----------------------|----------------------------------------------|
| `carlos-commons` (父) | 模块聚合配置、依赖管理一致性                               |
| `carlos-utils`       | 工具类设计缺陷、重复造轮子、线程安全、空指针风险、与 Hutool/Guava 功能重叠 |

#### Spring Boot Starters 层（核心框架）

| 模块                                       | 扫描重点                                                                   |
|------------------------------------------|------------------------------------------------------------------------|
| `carlos-spring-boot` (父)                 | Starter 聚合完整性、版本一致性                                                    |
| `carlos-spring-boot-core`                | Result/ApiResponse 响应体系、UserInfo 上下文、异常体系、分页工具、BaseEnum、IdUtils、统一响应规范 |
| `carlos-spring-cloud-starter`            | Spring Cloud Alibaba 集成、Nacos 配置、负载均衡、服务发现                             |
| `carlos-spring-boot-starter-web`         | XSS 过滤器、请求包装器、CORS 配置、全局异常处理、参数校验、Jackson 配置                           |
| `carlos-spring-boot-starter-mybatis`     | MyBatis-Plus 配置、分页插件、ID 生成器、多数据源、SQL 注入风险、mybatis-plus-join 使用         |
| `carlos-spring-boot-starter-security`    | SecurityFilterChain 配置、权限评估器、方法级安全注解 `@PreAuthorize`、缓存同步              |
| `carlos-spring-boot-starter-encrypt`     | SM2/SM4 国密算法实现、密钥管理、加密模式、性能优化、与 Spring Boot 3 兼容性                      |
| `carlos-spring-boot-starter-ai`          | ChatMemory 内存管理、工具调用、流式响应、超时配置、Token 限制                                |
| `carlos-spring-boot-starter-apm`         | SkyWalking 集成、Trace 上下文传递、自定义标签、性能指标上报                                 |
| `carlos-spring-boot-starter-datacenter`  | 多租户数据隔离、租户上下文传递、动态数据源切换                                                |
| `carlos-spring-boot-starter-datascope`   | 数据权限范围控制、SQL 拦截、部门/岗位/个人级权限                                            |
| `carlos-spring-boot-starter-disruptor`   | RingBuffer 配置、生产者/消费者模式、异常处理、背压策略                                      |
| `carlos-spring-boot-starter-integration` | Spring Integration 通道、消息路由、转换器、网关配置                                    |
| `carlos-spring-boot-starter-json`        | Fastjson2/Jackson 配置、序列化安全、日期格式、枚举序列化                                  |
| `carlos-spring-boot-starter-log`         | 日志链路追踪、MDC 上下文、异步日志、日志脱敏、日志归档策略                                        |
| `carlos-spring-boot-starter-migration`   | Flyway/Liquibase 迁移脚本、版本控制、多环境迁移策略                                     |
| `carlos-spring-boot-starter-mongodb`     | MongoTemplate 配置、连接池、索引管理、事务支持、GridFS                                  |
| `carlos-spring-boot-starter-mq`          | RabbitMQ/Kafka/RocketMQ 配置、生产者确认、消费者幂等、死信队列、消息追踪                       |
| `carlos-spring-boot-starter-openapi`     | Knife4j/OpenAPI 配置、文档分组、接口鉴权展示、离线文档导出                                  |
| `carlos-spring-boot-starter-oss`         | 阿里云/腾讯云/华为云 OSS 适配、断点续传、STS 临时凭证、CDN 集成                                |
| `carlos-spring-boot-starter-sms`         | 短信厂商多路适配、模板管理、发送限流、验证码防刷、发送记录                                          |
| `carlos-spring-boot-starter-snowflake`   | 雪花算法 WorkerId 分配、时钟回拨处理、ID 反解                                          |
| `carlos-spring-boot-starter-translation` | 国际化配置、动态语言切换、翻译源适配、缓存策略                                                |

#### Redis 缓存层（子聚合器）

| 模块                                          | 扫描重点                                                    |
|---------------------------------------------|---------------------------------------------------------|
| `carlos-spring-boot-redis` (父)              | Redis Starter 聚合配置                                      |
| `carlos-spring-boot-starter-redis`          | Redisson 配置、缓存策略、多级缓存一致性 (Caffeine+Redis)、Key 命名规范、过期策略 |
| `carlos-spring-boot-starter-redis-core`     | Redis 核心工具类、序列化、连接池、分布式锁实现                              |
| `carlos-spring-boot-starter-redis-reactive` | Reactive Redis 支持、Mono/Flux 链式调用、背压处理                   |

---

### 3. 扫描维度与检查清单

> **架构评估原则**：以顶级架构师视角审视框架基础设施。对每一个模块，先结合其**定义与定位**
> 分析现有实现，再通过技术调研判断设计是否足够优秀。若设计不优秀，输出整改方案；若设计优秀，再评估功能完整度。全程贯穿通用性、扩展性、耦合性、安全性四大质量属性分析，最后进行技术债务评估。

---

#### 3.1 架构设计评估（最高优先级）

**对每个模块执行以下评估流程：**

**步骤一：定位与职责对齐**

- [ ] 该模块的**设计定位**是否清晰？（如 `starter-web` 定位是 Web 基础设施增强，`starter-redis` 定位是统一缓存方案）
- [ ] 现有代码是否**忠实于定位**，还是存在职责蔓延？（如 Web Starter 中混入业务逻辑、Redis Starter 中混入业务缓存策略）
- [ ] 模块对外暴露的**API/配置接口**是否与定位匹配？是否存在过度暴露或暴露不足？

**步骤二：设计优秀度评判**

- [ ] **设计模式运用**：是否采用合理的模式解决领域问题？（如策略模式处理多厂商适配、模板方法定义标准流程、责任链处理过滤器）
- [ ] **抽象层完整性**：核心能力是否提炼了稳定的抽象接口？具体实现是否依赖于抽象而非具体？
- [ ] **自动配置设计**：`@AutoConfiguration` 类的编排是否合理？条件注解（`@ConditionalOnXxx`）是否精确表达了启用条件？配置顺序（
  `@AutoConfigureBefore`/`@AutoConfigureAfter`）是否正确？
- [ ] **配置属性设计**：`@ConfigurationProperties` 类的结构是否层次清晰？默认值是否合理？是否与 Spring Boot 官方 Starter
  的配置风格一致？
- [ ] **与 Spring Boot 3.x 的契合度**：是否充分利用 Spring Boot 3 的新特性（如 RestClient、HTTP
  Interface、ProblemDetail、结构化日志、Observability）？是否仍使用已废弃的 2.x 写法？

**步骤三：扩展性评估**

- [ ] **SPI 扩展点**：是否提供 `spring.factories` / `META-INF/services` / 条件 Bean 等扩展机制？使用者能否在不修改源码的情况下扩展功能？
- [ ] **策略可插拔**：多实现场景（如多厂商 OSS、多厂商短信）是否支持通过配置切换实现类？
- [ ] **钩子与回调**：是否提供生命周期钩子（如初始化前后回调、执行前后拦截）供使用者介入？
- [ ] ** starter 间组合能力**：当前 Starter 能否与其他 Starter 灵活组合，而不产生强制依赖？

**步骤四：耦合性评估**

- [ ] **模块间耦合**：当前模块是否依赖了不该依赖的模块？（如通用 Starter 依赖业务模块、Commons 模块依赖 Spring Boot）
- [ ] **第三方库耦合**：是否对特定第三方库存在过度耦合？（如核心工具类只能配合 MyBatis-Plus 使用、序列化只能配合 Fastjson）
- [ ] **Spring 生态耦合**：与 Spring Boot 的绑定是否适度？是否有条件地支持非 Spring 环境？
- [ ] **内部类耦合**：模块内部各组件之间的依赖是否通过接口隔离？是否存在"上帝类"？

**步骤五：若设计不优秀，输出整改方案**

- 问题描述：现有设计违背了什么原则（SOLID/开闭/单一职责/依赖倒置）
- 优秀设计参考：业界同类框架的成熟做法（如 Spring Boot 官方 Starter、Apache 组件）
- 整改方案：具体重构路径，附伪代码或架构图说明
- 影响面评估：整改对现有使用者的兼容性影响

---

#### 3.2 功能完整度评估（设计优秀时执行）

当 3.1 评估判定该模块设计优秀后，继续评估：

- [ ] **自动配置注册完整性**：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
  是否完整？是否有遗漏的 AutoConfiguration 类？
- [ ] **配置元数据完整性**：是否提供 `spring-configuration-metadata.json` 或
  `additional-spring-configuration-metadata.json`，支持 IDE 自动补全？
- [ ] **配置属性覆盖度**：所有可通过外部配置调整的参数，是否都定义了对应的 `@ConfigurationProperties` 字段？是否存在硬编码的魔法值？
- [ ] **健康检查**：是否实现了 `HealthIndicator`？健康检查逻辑是否覆盖核心依赖（如 Redis 连接、数据库连接、MQ 连接）？
- [ ] **监控与指标**：是否通过 Micrometer 暴露关键指标？关键操作是否有计时器、计数器、仪表盘？
- [ ] **优雅关闭**：是否实现 `DisposableBean` / `@PreDestroy` / 优雅停机钩子，确保资源正确释放？
- [ ] **异常处理**：是否提供统一的全局异常处理？异常信息是否对使用者友好（不泄露内部实现细节）？
- [ ] **日志规范**：是否遵循框架统一的日志规范？关键路径是否有合理的日志级别和上下文（MDC/TraceId）？
- [ ] **示例覆盖度**：是否有对应的 `carlos-sample-xxx` 示例模块？示例是否覆盖了最常见的使用场景和高级用法？

---

#### 3.3 通用性评估

- [ ] **API 通用性**：暴露的公共 API 是否足够通用，能满足大多数使用场景？是否存在为特定业务场景定制的 API 污染了通用
  Starter？
- [ ] **配置通用性**：配置项命名是否符合 Spring Boot 约定（`spring.xxx` / `carlos.xxx`）？配置语义是否自解释？
- [ ] **多环境适配**：是否支持不同环境（开发/测试/生产）的无缝切换？配置覆盖机制是否完善？
- [ ] **版本兼容性**：是否与不同版本的 Spring Boot / JDK 兼容？兼容性边界是否明确文档化？
- [ ] **工具类通用性**：`carlos-utils` 中的工具类是否足够通用？是否与 Hutool/Guava/Apache Commons 存在大量重叠？重叠部分是否有必要保留？

---

#### 3.4 安全架构评估（最高优先级）

- [ ] **安全设计模式**：安全机制是否内建于设计之中（Security by Design），而非事后补丁？
- [ ] **密钥管理**：SM2/SM4 密钥、数据库密码、AK/SK 等敏感配置是否支持外部化（配置中心/环境变量/密钥管理服务）？严禁硬编码
- [ ] **序列化安全**：Fastjson/Jackson 是否配置了安全的反序列化策略？（autoType 白名单、`ObjectMapper` 可见性限制为
  `PUBLIC_ONLY`）
- [ ] **Web 安全**：`starter-web` 的 XSS 过滤器、CSRF 防护、CORS 策略是否完整？是否存在绕过风险？
- [ ] **数据安全**：敏感数据（Token/密码/身份证号）在日志中是否脱敏？Redis 中是否存储明文敏感数据？
- [ ] **国密合规**：`starter-encrypt` 的国密实现是否符合国密标准？密钥生命周期管理是否安全？
- [ ] **反序列化防护**：是否校验反序列化类白名单？是否存在不安全的 `ObjectInputStream` 使用？
- [ ] **文件上传安全**：文件类型白名单、大小限制、路径遍历防护是否到位？
- [ ] **静态工具类安全**：静态工具类是否持有状态导致线程安全问题？是否缓存了敏感数据？
- [ ] **License 安全**：生产环境是否误包含 `carlos-spring-boot-starter-license-generate`？

---

#### 3.5 技术债务评估

- [ ] **依赖版本债务**：依赖版本是否严重落后（与当前最新稳定版差距 > 2 个小版本）？是否存在已知 CVE？
- [ ] **迁移残留债务**：是否存在 Spring Boot 2.x → 3.x 迁移残留？（`javax.*` 包、`WebSecurityConfigurerAdapter`、废弃的
  `spring.factories` 等）
- [ ] **BOM 一致性债务**：同一依赖在 BOM 和实际模块中版本是否冲突？`carlos-dependencies` 是否及时更新？
- [ ] **Maven 构建债务**：Maven 插件版本是否过时？构建配置是否有冗余或废弃的配置？
- [ ] **代码重复债务**：多个 Starter 之间是否存在重复配置或重复逻辑？（如多个 Starter 各自实现了一套相似的配置加载逻辑）
- [ ] **测试债务**：核心逻辑是否缺少单元测试？测试覆盖率是否达标？
- [ ] **文档债务**：模块 README、JavaDoc、CHANGELOG 是否及时更新？
- [ ] **SNAPSHOT 债务**：是否存在不应出现在框架发布版本的 SNAPSHOT 依赖？

---

#### 3.6 评估决策树（执行逻辑）

```
对每个模块：
  ├─ 步骤1: 分析模块定义与当前代码 → 输出"职责对齐度"评估
  ├─ 步骤2: 判断设计是否优秀
  │   ├─ 优秀 → 进入步骤3（功能完整度评估）
  │   └─ 不优秀 → 输出整改方案（问题+优秀参考+重构路径），标记 P0/P1
  ├─ 步骤3: 评估通用性、扩展性、耦合性 → 输出质量属性评分
  ├─ 步骤4: 安全架构评估 → 安全问题单独标记，最高优先级
  └─ 步骤5: 技术债务评估 → 输出债务清单与偿还建议
```

---

### 4. 深度代码分析要求

利用网络搜索工具，执行技术调研，对每个发现的问题，执行深层次的分析，必须提供以下信息：

1. **问题定位**：文件路径 + 行号范围（精确到方法或代码块）
2. **相关代码**：完整的代码片段（不少于 5 行上下文）
3. **详细分析**：
    - 问题类型（设计缺陷 / 功能缺失 / 可优化 / 安全漏洞 / 技术债务）
    - 严重程度（P0-Critical / P1-High / P2-Medium / P3-Low）
    - 影响范围（单个方法 / 单个类 / 整个模块 / 框架级）
    - 触发条件（什么场景下会出问题）
    - 潜在风险（性能下降 / 安全漏洞 / 功能异常 / 维护困难）
4. **解决方案**：
    - 推荐修复方案（提供具体代码示例）
    - 短期规避措施（如无法立即修复）
    - 长期改进建议（架构层面）
    - 参考文档或最佳实践链接

---

### 5. 报告生成格式

报告文件：`docs/reports/framework-scan-report-YYYY-MM-DD-N.md`

```markdown
# Carlos Framework 框架核心代码扫描报告

**生成时间**: YYYY-MM-DD HH:MM
**扫描 Commit**: xxx
**扫描范围**: 框架核心代码层（dependencies / parent / commons / spring-boot starters）
**发现问题总数**: N 个（P0: a 个, P1: b 个, P2: c 个, P3: d 个）

---

## 一、执行摘要

### 1.1 今日变更概览
- Git 拉取状态: 成功/失败
- Commit: xxx
- 变更文件数: N 个
- 新增代码: +X 行 / 删除代码: -Y 行

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 设计缺陷 | | | | | |
| 功能缺失 | | | | | |
| 性能优化 | | | | | |
| 安全漏洞 | | | | | |
| 技术债务 | | | | | |

### 1.3 今日重点关注
- [TOP 1] xxx（P0，影响 xxx）
- [TOP 2] xxx（P1，影响 xxx）
- [TOP 3] xxx（P1，影响 xxx）

---

## 二、设计缺陷详情

### 问题 1: [简短标题]

**模块**: `carlos-xxx`
**文件**: `carlos-xxx/src/main/java/.../Xxx.java`
**行号**: L45-L78
**严重程度**: P0/P1/P2/P3
**影响范围**: 框架级/模块级/类级

#### 相关代码


// carlos-xxx/src/main/java/.../Xxx.java L45-L78
[此处粘贴完整代码片段]

#### 详细分析

- **问题描述**: ...
- **违反原则**: ...
- **触发条件**: ...
- **潜在风险**: ...

#### 解决方案

**推荐修复**:

[提供修复后的代码示例]

**短期规避**: ...（如无法立即修复）

**长期建议**: ...（架构层面改进）

**参考文档**: [链接]

---

## 三、功能未完善详情

...[同上格式]...

---

## 四、代码可优化点详情

...[同上格式]...

---

## 五、安全漏洞详情

...[同上格式，安全类问题必须提供 CWE 编号和修复优先级]...

---

## 六、依赖与技术债务

### 6.1 过期依赖清单

| 依赖 | 当前版本 | 最新稳定版 | 差距 | CVE | 建议操作 |
|----|------|-------|----|-----|------|
|    |      |       |    |     |      |

### 6.2 已弃用 API 使用

| 文件 | 行号 | 已弃用 API | 替代方案 |
|----|----|---------|------|
|    |    |         |      |

### 6.3 BOM 版本冲突

| 依赖 | dependencies 中版本 | 实际使用版本 | 模块 | 建议 |
|----|------------------|--------|----|----|
|    |                  |        |    |    |

---

## 七、Starter 健康度评分

| 模块                      | 设计规范  | 功能完整  | 代码质量  | 安全性   | 综合评分    |
|-------------------------|-------|-------|-------|-------|---------|
| carlos-spring-boot-core | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 4.8/5.0 |
| ...                     |       |       |       |       |         |

> 评分标准：5星=优秀，4星=良好，3星=一般，2星=较差，1星=需立即整改

---

## 八、待办事项与修复计划

### 8.1 今日新增待办

- [ ] P0-001 carlos-xxx: 修复 xxx 问题
- [ ] P1-002 carlos-xxx: 完善 xxx 功能
- [ ] P2-003 carlos-xxx: 优化 xxx 实现

### 8.2 历史遗留跟踪

（复制昨日报告中未关闭的待办，标记进度）

- [ ] P0-004 carlos-xxx: xxx 问题 ⏳ 修复中 / ✅ 已修复 / ❌ 搁置

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*

```

---

### 6. 报告提交

扫描完成后，执行：

```bash
cd /home/carlos/.openclaw/agents/carlos-dev-assistant/workspace/carlos-framework
git add docs/reports/
git commit -m "docs: 框架核心代码扫描报告 $(date +%Y-%m-%d)"
git push origin main 2>/dev/null || echo "[WARN] git push 失败，请手动提交报告"
```

---

## 扫描规范与约束

### 代码片段规范

- 引用的代码必须**包含完整的方法签名或类定义上下文**
- 必须标注**精确的文件路径和行号范围**
- 代码片段使用 ```java 语法高亮
- 若涉及多文件关联，需分别列出

### 问题分级标准

| 级别              | 定义                   | 响应时效     | 示例                             |
|-----------------|----------------------|----------|--------------------------------|
| **P0-Critical** | 会导致系统故障、数据丢失、安全漏洞的问题 | 24 小时内修复 | SQL 注入、内存泄漏、硬编码密钥、空指针崩溃        |
| **P1-High**     | 影响核心功能、性能严重下降、违反强制规范 | 本周内修复    | Service 直接调 Mapper、未加事务、缓存雪崩风险 |
| **P2-Medium**   | 影响代码质量、可维护性、存在潜在风险   | 本月内修复    | 魔法值硬编码、日志占位符缺失、重复代码            |
| **P3-Low**      | 轻微优化建议、风格问题、文档缺失     | 排期优化     | JavaDoc 缺失、变量命名不规范、import 未优化  |

### 分析深度要求

- **每个 P0/P1 问题**必须提供：代码片段 + 详细分析 + 完整解决方案
- **每个 P2 问题**必须提供：代码片段 + 分析 + 修复建议
- **每个 P3 问题**至少提供：问题描述 + 优化建议
- 单个模块发现超过 5 个问题，需单独输出子章节

### 特殊场景处理

- **无新 Commit**: 仍需基于当前代码全量扫描，报告标注 "今日无代码变更，基于最新 Commit xxx 扫描"
- **新增 Starter**: 优先深度扫描新增模块，标记为 "新增 Starter 重点审查"
- **大规模重构**: 若发现某模块正在被大规模重构，可标注 "该模块处于重构期，部分问题可能已被识别"

---

## 工作区路径

- **项目根目录**: `/home/carlos/.openclaw/agents/carlos-dev-assistant/workspace/carlos-framework`
- **报告输出目录**: `docs/reports/`
- **编码规范参考**: `.claude/skills/carlos-framework-standard/SKILL.md`
- **错误码规范参考**: `carlos-commons/carlos-spring-boot-core/ERROR_CODE_SPEC.md`
- **所有分析基于此目录下的实际代码，严禁虚构文件路径或代码内容**
