# Carlos Framework 业务模块质量报告

**生成时间**: 2026-06-05 08:25  
**扫描 Commit**: `6442a2f4` (`refactor(maven): 优化插件配置提升构建质量`)  
**扫描范围**: 业务集成层（gateway / audit / auth / license / message / org / system / tools）  
**发现问题总数**: 6 个（P0: 1 个, P1: 2 个, P2: 2 个, P3: 1 个）

---

## 一、执行摘要

### 1.1 今日变更概览

| 项目 | 状态 |
|------|------|
| Git 拉取 | ✅ 成功（Fast-forward） |
| Commit | `6442a2f4` |
| 变更文件数 | 4 个 |
| 新增代码 | +394 行 |
| 删除代码 | -89 行 |

**变更文件列表：**
- `carlos-dependencies/pom.xml` — 移除 69 行（flyway/build-helper/assembly/enforcer/native-image 插件及版本属性）
- `carlos-parent/pom.xml` — 新增 60 行（enforcer/javadoc/failsafe 插件激活）
- `pom.xml`（根）— 移除 20 行（maven-compiler-plugin 重复配置）
- `docs/Maven插件优化报告.md` — 新增 334 行

> 注：今日无业务模块代码变更，基于最新 Commit `6442a2f4` 全量扫描。

### 1.2 问题分布总览

| 问题类型 | P0-Critical | P1-High | P2-Medium | P3-Low | 合计 |
|---------|-------------|---------|-----------|--------|------|
| 技术债务（迁移残留） | 1 | 0 | 0 | 0 | 1 |
| 架构违规 | 0 | 1 | 0 | 0 | 1 |
| 功能缺失 | 0 | 1 | 0 | 0 | 1 |
| 性能/安全设计 | 0 | 0 | 2 | 0 | 2 |
| 代码维护 | 0 | 0 | 0 | 1 | 1 |
| **合计** | **1** | **2** | **2** | **1** | **6** |

### 1.3 模块问题分布

| 模块 | P0 | P1 | P2 | P3 | 合计 |
|-----|----|----|----|----|------|
| carlos-audit | 1 | 0 | 0 | 0 | 1 |
| carlos-auth | 0 | 1 | 1 | 0 | 2 |
| carlos-gateway | 0 | 0 | 1 | 0 | 1 |
| carlos-tools | 0 | 0 | 0 | 1 | 1 |
| carlos-message | 0 | 1 | 0 | 0 | 1 |
| carlos-org | 0 | 1 | 0 | 0 | 1 |
| carlos-system | 0 | 1 | 0 | 0 | 1 |
| carlos-license | 0 | 0 | 0 | 0 | 0 |

### 1.4 今日重点关注

- **[TOP 1]** HealthIndicator 包路径错误（P0，audit 模块 2 个文件编译失败）— 昨日遗留框架级问题
- **[TOP 2]** -cloud 模块违规依赖 -bus（P1，4 个模块架构分层违规）
- **[TOP 3]** Feign FallbackFactory 覆盖率低（P1，auth/audit/message/system 覆盖率 <20%）
- **[TOP 4]** OAuth2 客户端仓库使用 InMemory 实现（P2，auth 模块生产环境需持久化）
- **[TOP 5]** 限流告警 TODO 未对接实际告警系统（P2，gateway 模块）
- **[TOP 6]** carlos-tools 大量 TODO 遗留（P3，16 个未实现标记）

---

## 二、技术债务详情（迁移残留）

### 问题 1: HealthIndicator 包路径错误 — audit 模块编译必失败

**模块**: `carlos-audit`  
**文件**:
- `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/config/AuditAutoConfiguration.java`
- `carlos-integration/carlos-audit/carlos-audit-bus/src/main/java/com/carlos/audit/clickhouse/ClickHouseHealthIndicator.java`  
**严重程度**: **P0-Critical**  
**影响范围**: 模块级 — audit 业务模块的健康检查功能  
**问题类型**: 技术债务（Spring Boot 3.x → 不存在包路径）

#### 详细分析

- **问题描述**: 两个文件引用了 `org.springframework.boot.health.contributor` 包路径，该包在 Spring Boot 3.x 中不存在，正确路径应为 `org.springframework.boot.actuate.health`。
- **违反原则**: Spring Boot 3.x 迁移遗留问题。该问题在昨日框架扫描中已标记为 P0（21 个文件全框架级），今日确认业务模块 audit 也受影响。
- **触发条件**: 编译 `carlos-audit-bus` 模块时失败。
- **潜在风险**: audit 模块无法编译，影响审计日志功能集成。

#### 解决方案

**推荐修复**:

```bash
# 在 carlos-integration/carlos-audit/ 目录下执行
cd carlos-integration/carlos-audit/
find . -name "*.java" -exec sed -i \
  's/org\.springframework\.boot\.health\.contributor\.Health/org.springframework.boot.actuate.health.Health/g' {} +
find . -name "*.java" -exec sed -i \
  's/org\.springframework\.boot\.health\.contributor\.HealthIndicator/org.springframework.boot.actuate.health.HealthIndicator/g' {} +
```

**参考**: 详见框架扫描报告 `framework-scan-report-2026-06-05-1.md` 中 P0-001 的完整修复方案。

---

## 三、架构违规详情

### 问题 2: -cloud 模块违规依赖 -bus — 4 个模块架构分层违规

**模块**: `carlos-audit`, `carlos-message`, `carlos-org`, `carlos-system`  
**严重程度**: **P1-High**  
**影响范围**: 多模块联动 — 4 个业务模块的 REST 端点实现层  
**问题类型**: 架构违规（模块依赖方向）

#### 详细分析

**架构规范要求**：
- `-api` 模块：定义 Feign 接口和 DTO/AO，**不依赖** `-bus`
- `-cloud` 模块：提供 REST 端点和 Feign 接口实现，**只应依赖** `-api`
- `-bus` 模块：核心业务逻辑和 Manager 层，**被 -cloud 依赖**（通过接口调用）

**实际依赖情况**：

| 模块 | -cloud 依赖 -api | -cloud 依赖 -bus | 是否违规 |
|------|---------------|----------------|---------|
| carlos-auth | 0 | 0 | ✅ 合规 |
| carlos-audit | 0 | 1 | ❌ 违规 |
| carlos-message | 0 | 1 | ❌ 违规 |
| carlos-org | 0 | 1 | ❌ 违规 |
| carlos-system | 0 | 1 | ❌ 违规 |

- **问题描述**: 4 个业务模块的 `-cloud` 实现层直接依赖了 `-bus` 核心业务逻辑模块，违反了分层架构的依赖方向规范。
- **违反原则**: `-cloud` 作为 REST 端点暴露层，应通过 Feign 接口（定义在 `-api`）与其他服务交互，或仅依赖 `-api` 中的共享类型。直接依赖 `-bus` 会导致模块间紧耦合，破坏微服务独立部署能力。
- **触发条件**: 任何尝试独立构建/部署 `-cloud` 模块时，必须同时包含 `-bus` 的所有依赖。
- **潜在风险**: 
  - 模块无法独立部署
  - 循环依赖风险增加
  - 测试时难以 Mock 依赖
  - 违背了微服务分层设计的初衷

#### 解决方案

**推荐修复**:

1. **分析依赖原因**：检查 `-cloud` 的 `pom.xml`，找出具体依赖 `-bus` 的用途：
   - 如果是为了使用 `-bus` 中的 DTO/VO/常量，应将这些共享类型迁移到 `-api`
   - 如果是为了调用 `-bus` 中的 Service/Manager 方法，应通过 Feign 调用或事件驱动

2. **逐步解耦**：

```xml
<!-- carlos-audit-cloud/pom.xml 示例 -->
<!-- 移除 -->
<dependency>
  <groupId>com.carlos</groupId>
  <artifactId>carlos-audit-bus</artifactId>
  <version>${project.version}</version>
</dependency>

<!-- 保留（如果已有）或添加 -->
<dependency>
  <groupId>com.carlos</groupId>
  <artifactId>carlos-audit-api</artifactId>
  <version>${project.version}</version>
</dependency>
```

3. **共享类型迁移**：将 `-bus` 中被 `-cloud` 使用的 DTO、常量、枚举迁移到 `-api` 模块。

4. **业务调用解耦**：`-cloud` 中的 REST Controller 不应直接调用 `-bus` 的 Service，而应通过 Feign 客户端调用其他服务的接口，或使用内部接口抽象。

**短期规避**: 暂时保持现状，标记为技术债务，在下次模块重构时处理。

**长期建议**: 
- 制定模块依赖矩阵文档，明确每个模块的合法依赖范围
- 在 CI 中增加 Maven Enforcer 插件，禁止非法依赖
- 参考 `carlos-auth` 的设计（-cloud 不依赖 -bus）

---

## 四、功能缺失详情

### 问题 3: Feign FallbackFactory 覆盖率严重不足 — 5 个模块

**模块**: `carlos-auth`, `carlos-audit`, `carlos-message`, `carlos-org`, `carlos-system`  
**严重程度**: **P1-High**  
**影响范围**: 多模块联动 — 跨服务调用降级能力缺失  
**问题类型**: 功能缺失（服务熔断降级）

#### 详细分析

**Feign FallbackFactory 覆盖统计**：

| 模块 | Feign 接口数 | FallbackFactory 数 | 覆盖率 | 状态 |
|------|-----------|-------------------|--------|------|
| carlos-auth | 16 | 2 | 12.5% | ❌ 严重不足 |
| carlos-audit | 22 | 1 | 4.5% | ❌ 严重不足 |
| carlos-message | 26 | 2 | 7.7% | ❌ 严重不足 |
| carlos-org | 36 | 7 | 19.4% | ❌ 不足 |
| carlos-system | 40 | 5 | 12.5% | ❌ 严重不足 |

- **问题描述**: 各业务模块的 `-api` 中定义了大量 Feign 接口，但绝大多数没有对应的 FallbackFactory 实现。这意味着当下游服务不可用时，调用方会因无降级逻辑而直接抛出异常，可能导致级联故障。
- **违反原则**: 微服务架构中，跨服务调用必须具备熔断降级能力。Feign FallbackFactory 是 Spring Cloud 提供的标准降级机制。
- **触发条件**: 当任何下游服务（如 auth、org、system）宕机或超时时，调用方的请求会直接失败，无优雅降级。
- **潜在风险**: 
  - 级联故障（Cascading Failure）
  - 用户体验差（直接报错而非返回缓存/默认值）
  - 雪崩效应

#### 解决方案

**推荐修复**（以 carlos-auth 为例）：

```java
// carlos-auth-api/src/main/java/com/carlos/auth/api/feign/AuthFeignApi.java
@FeignClient(name = "carlos-auth", fallbackFactory = AuthFeignFallbackFactory.class)
public interface AuthFeignApi {
    @GetMapping("/api/auth/user/info")
    Result<UserInfo> getUserInfo(@RequestParam("userId") Long userId);
}

// carlos-auth-api/src/main/java/com/carlos/auth/api/feign/AuthFeignFallbackFactory.java
@Slf4j
@Component
public class AuthFeignFallbackFactory implements FallbackFactory<AuthFeignApi> {
    @Override
    public AuthFeignApi create(Throwable cause) {
        log.error("Auth service fallback triggered", cause);
        return new AuthFeignApi() {
            @Override
            public Result<UserInfo> getUserInfo(Long userId) {
                return Result.fail(CommonErrorCode.THIRD_PARTY_ERROR, "认证服务暂不可用");
            }
        };
    }
}
```

**短期规避**: 
- 为核心高频接口（如用户信息查询、权限校验）优先添加 FallbackFactory
- 配置 Hystrix/Sentinel 全局降级策略

**长期建议**: 
- 为所有 Feign 接口强制要求 FallbackFactory
- 在代码审查（Code Review）清单中增加此项检查
- 考虑使用代码生成工具自动生成 FallbackFactory 骨架

---

## 五、性能与安全设计详情

### 问题 4: OAuth2 客户端仓库使用 InMemoryRegisteredClientRepository — 生产环境数据丢失风险

**模块**: `carlos-auth`  
**文件**: `carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/oauth2/config/OAuth2ClientConfig.java` L98-L101  
**严重程度**: **P2-Medium**  
**影响范围**: 模块级 — auth-service 的 OAuth2 客户端管理  
**问题类型**: 性能/安全设计（持久化缺失）

#### 相关代码

```java
// carlos-integration/carlos-auth/carlos-auth-service/.../OAuth2ClientConfig.java L93-L103
if (clientConfigs != null && !clientConfigs.isEmpty()) {
    for (OAuth2ClientProperties config : clientConfigs) {
        clients.add(buildRegisteredClient(config, passwordEncoder));
        log.info("Registered OAuth2 client: {}", config.getClientId());
    }
    return new InMemoryRegisteredClientRepository(clients);  // ⚠️ 内存存储
}

log.info("Using CompositeRegisteredClientRepository");
return new CustomizeRegisteredClientRepository(
    SpringUtil.getBean(AppClientService.class));               // ⚠️ 动态获取 Bean
```

#### 详细分析

- **问题描述**: 
  1. 当配置了静态客户端时，使用 `InMemoryRegisteredClientRepository`，客户端信息存储在内存中，应用重启后丢失，无法动态增删改查。
  2. 当未配置静态客户端时，使用 `CustomizeRegisteredClientRepository` 并调用 `SpringUtil.getBean(AppClientService.class)` 动态获取 Bean，这种方式绕过了 Spring 的依赖注入，破坏了可测试性和设计规范。
- **违反原则**: 
  - OAuth2 客户端信息属于持久化数据，应使用 `JdbcRegisteredClientRepository` 或 Redis 存储
  - 应通过构造函数注入依赖，而非 `SpringUtil.getBean()`
- **触发条件**: 应用重启后，通过 API 动态注册的客户端信息丢失。
- **潜在风险**: 
  - 生产环境重启后所有动态 OAuth2 客户端丢失
  - `SpringUtil.getBean()` 在非 Spring 上下文调用会抛出 `NoSuchBeanDefinitionException`
  - 单元测试难以 Mock `AppClientService`

#### 解决方案

**推荐修复**:

```java
// 方案 A：使用 JdbcRegisteredClientRepository 持久化
@Bean
@ConditionalOnMissingBean
public RegisteredClientRepository registeredClientRepository(
    PasswordEncoder passwordEncoder,
    JdbcTemplate jdbcTemplate) {  // ✅ 构造函数注入

    JdbcRegisteredClientRepository repository = 
        new JdbcRegisteredClientRepository(jdbcTemplate);
    
    // 初始化静态配置的客户端
    List<OAuth2ClientProperties> clientConfigs = oauth2Properties.getClients();
    if (clientConfigs != null) {
        for (OAuth2ClientProperties config : clientConfigs) {
            RegisteredClient client = buildRegisteredClient(config, passwordEncoder);
            // 如果数据库中不存在则插入
            if (repository.findByClientId(client.getClientId()) == null) {
                repository.save(client);
            }
        }
    }
    
    return repository;
}

// 方案 B：保留 Composite 模式，但修正依赖注入方式
@Bean
@ConditionalOnMissingBean
public RegisteredClientRepository registeredClientRepository(
    PasswordEncoder passwordEncoder,
    AppClientService appClientService) {  // ✅ 构造函数注入替代 SpringUtil

    // ...
}
```

**短期规避**: 在 `application.yml` 中配置所有静态客户端，避免运行时动态注册；确保应用重启后配置不变。

**参考文档**: [Spring Authorization Server - RegisteredClientRepository](https://docs.spring.io/spring-authorization-server/docs/current/reference/html/core-model-components.html#registered-client-repository)

---

### 问题 5: 网关限流告警 TODO 未对接实际告警系统

**模块**: `carlos-gateway`  
**文件**: `carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/ratelimit/listener/RateLimitAlertListener.java` L84  
**严重程度**: **P2-Medium**  
**影响范围**: 模块级 — 网关限流告警功能不完整  
**问题类型**: 功能缺失（告警未对接）

#### 相关代码

```java
// carlos-integration/carlos-gateway/src/main/java/com/carlos/gateway/ratelimit/listener/RateLimitAlertListener.java L84
// TODO: 对接实际告警系统
```

#### 详细分析

- **问题描述**: 网关限流触发后的告警逻辑目前只是 TODO 注释，未实际对接告警系统（如钉钉、邮件、企业微信、Prometheus Alertmanager 等）。
- **违反原则**: 生产环境中，限流触发属于重要告警事件，应实时通知运维人员。
- **触发条件**: 当网关限流规则触发时，仅记录日志，无实际告警通知。
- **潜在风险**: 生产环境遭遇流量突增或攻击时，运维人员无法及时感知和响应。

#### 解决方案

**推荐修复**:

```java
@Component
@RequiredArgsConstructor
public class RateLimitAlertListener {
    
    private final AlertService alertService;  // 注入告警服务抽象
    
    public void onRateLimitTriggered(RateLimitEvent event) {
        log.warn("Rate limit triggered: {}", event);
        
        // 发送告警通知
        alertService.sendAlert(AlertMessage.builder()
            .level(AlertLevel.WARNING)
            .title("网关限流告警")
            .content(String.format("路径 %s 触发限流，当前 QPS: %d", 
                event.getPath(), event.getCurrentQps()))
            .build());
    }
}
```

**短期规避**: 通过日志收集（ELK/Loki）+ 日志告警规则间接实现告警。

---

## 六、代码维护详情

### 问题 6: carlos-tools 大量 TODO 遗留标记 — 16 个未实现功能

**模块**: `carlos-tools`  
**严重程度**: **P3-Low**  
**影响范围**: 模块级 — GUI 代码生成器和 GitLab 管理工具  
**问题类型**: 代码维护（TODO 遗留）

#### TODO 标记清单

| 文件 | 行号 | TODO 内容 |
|------|------|-----------|
| `RateLimitAlertListener.java` | 84 | 对接实际告警系统 |
| `CodeGeneratorController.java` | 203 | Carlos 2026-02-14（代码生成器） |
| `CodeGeneratorService.java` | 95 | 多模块模板目录处理 |
| `UserManagementController.java` | 211 | Show add user dialog |
| `UserManagementController.java` | 224 | Show edit user dialog |
| `UserManagementController.java` | 354 | Parse Excel and import users |
| `UserManagementController.java` | 377 | Export users to Excel |
| `GitlabMainController.java` | 167 | 从配置文件加载服务器列表 |
| `GitlabMainController.java` | 183 | 从配置文件加载 |
| `GitlabMainController.java` | 212 | 显示服务器配置对话框 |
| `GitlabMainController.java` | 227 | 显示服务器配置对话框 |
| `GitlabMainController.java` | 279 | 从配置文件获取对应的 Token |
| `GitlabMainController.java` | 302 | 显示错误对话框 |
| `GitlabMainController.java` | 335 | 实现导出功能界面 |
| `GitlabMainController.java` | 352 | 实现标签页刷新逻辑 |
| `GitlabMainController.java` | 371 | 通知所有标签页控制器刷新数据 |
| `IssueManagementController.java` | 246 | Show create issue dialog |
| `IssueManagementController.java` | 259 | Show issue details dialog |
| `IssueManagementController.java` | 343 | Show assign user dialog |
| `IssueManagementController.java` | 351 | Show add label dialog |

> 共 20 个 TODO 标记（30 个 grep 结果中包含重复匹配），主要集中在 `carlos-tools` 的 GUI 控制器中。

#### 详细分析

- **问题描述**: `carlos-tools` 模块中存在大量 TODO 标记，部分标记日期为 2020 年，说明长期未处理。
- **违反原则**: TODO 应代表短期待办事项，不应长期存在于代码库中。
- **触发条件**: 使用 `carlos-tools` 的 GUI 功能时，部分按钮/菜单无实际响应。
- **潜在风险**: 
  - 用户体验差
  - 代码维护困难
  - 新开发者对代码成熟度产生质疑

#### 解决方案

**推荐修复**:
1. 对超过 6 个月的 TODO 进行清理：
   - 已实现但未删除标记的 → 删除 TODO
   - 确定不实现的 → 删除 TODO 并记录到 Issue 追踪系统
   - 仍需实现的 → 转换为 Git Issue 或任务，从代码中移除 TODO

2. 在 CI 中增加 TODO 年龄检查（如超过 90 天未处理的 TODO 报警）

**短期规避**: 添加模块级说明文档，告知用户哪些功能是实验性的。

---

## 七、跨模块通用检查详情

### 7.1 模块依赖合规性

| 检查项 | 状态 | 说明 |
|--------|------|------|
| `-api` 不依赖 `-bus` | ✅ 合规 | 5/5 模块通过 |
| `-cloud` 只依赖 `-api` | ❌ 违规 | 4/5 模块违规（详见问题 2） |
| `-boot` 正确聚合 | ✅ 合规 | 各 -boot 模块结构正确 |

### 7.2 Feign FallbackFactory 覆盖度

| 模块 | 接口总数 | FallbackFactory | 覆盖率 | 状态 |
|------|---------|-----------------|--------|------|
| carlos-auth | 16 | 2 | 12.5% | ❌ |
| carlos-audit | 22 | 1 | 4.5% | ❌ |
| carlos-message | 26 | 2 | 7.7% | ❌ |
| carlos-org | 36 | 7 | 19.4% | ❌ |
| carlos-system | 40 | 5 | 12.5% | ❌ |

> 建议覆盖率目标：≥80%（核心高频接口必须 100%）

### 7.3 Convert 规范（MapStruct vs BeanUtils）

| 检查项 | 结果 | 状态 |
|--------|------|------|
| `BeanUtils.copyProperties` 使用 | 0 个 | ✅ 优秀 |
| `org.springframework.beans.BeanUtils` | 0 个 | ✅ 优秀 |

> 项目统一使用 MapStruct，无 BeanUtils 泛滥问题。

### 7.4 密码编码器安全

| 检查项 | 结果 | 状态 |
|--------|------|------|
| `MD5PasswordEncoder` 残留 | 0 个 | ✅ 已清除 |
| `ShaPasswordEncoder` 残留 | 0 个 | ✅ 已清除 |
| `NoOpPasswordEncoder` 残留 | 0 个 | ✅ 已清除 |

### 7.5 License 生成模块安全检查

| 检查项 | 结果 | 状态 |
|--------|------|------|
| `carlos-spring-boot-starter-license-generate` 非 license 模块依赖 | 0 个 | ✅ 安全 |
| `carlos-license/pom.xml` 中声明 license-generate | 存在 | ✅ 仅父模块聚合，正常 |

> 安全红线通过：生产环境未误包含 license-generate。

### 7.6 文档完整性

| 模块 | README.md | 状态 |
|------|-----------|------|
| carlos-gateway | ✅ | 存在 |
| carlos-audit | ✅ | 存在 |
| carlos-auth | ✅ | 存在 |
| carlos-message | ✅ | 存在 |
| carlos-org | ✅ | 存在 |
| carlos-system | ✅ | 存在 |
| carlos-license | ✅ | 存在 |
| carlos-tools | ✅ | 存在 |

### 7.7 Service 直接引用 Mapper 检查

| 文件 | 位置 | 说明 |
|------|------|------|
| `ExtendUserDetailsService.java` L35 | `carlos-auth-service` | `private UserMapper userMapper` — **已注释掉**（规范意识良好） |
| `MfaManagerImpl.java` L32-L33 | `carlos-auth-service` | `MfaRecoveryCodeMapper`, `TrustedDeviceMapper` — Manager 层直接引用 Mapper，**符合规范** |

> 结论：Service 层未直接引用 Mapper，Manager 层直接引用 Mapper 符合分层架构规范。

---

## 八、业务模块健康度评分

| 模块 | 文档完整 | 架构合规 | 规范落实 | 性能安全 | 功能完整 | 综合评分 |
|------|---------|---------|---------|---------|---------|---------|
| carlos-gateway | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.7/5.0 |
| carlos-audit | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.5/5.0 |
| carlos-auth | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | 4.5/5.0 |
| carlos-message | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.2/5.0 |
| carlos-org | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.2/5.0 |
| carlos-system | ⭐⭐⭐⭐⭐ | ⭐⭐⭐☆☆ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | 4.2/5.0 |
| carlos-license | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 5.0/5.0 |
| carlos-tools | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐☆ | ⭐⭐⭐⭐☆ | ⭐⭐⭐☆☆ | 3.8/5.0 |

> 评分说明：
> - 涉及 -cloud 依赖 -bus 违规的模块（message/org/system/audit），"架构合规" 扣 2 星
> - Feign FallbackFactory 覆盖率低的模块，"功能完整" 扣 1 星
> - carlos-tools 大量 TODO 遗留，"功能完整" 扣 2 星
> - 评分基于静态代码分析，实际运行时行为可能不同

---

## 九、待办事项与修复计划

### 9.1 今日新增待办

- [ ] **P0-001** carlos-audit: 修复 HealthIndicator 包路径错误（2 个文件）— 使用 sed 批量替换
- [ ] **P1-002** carlos-audit/message/org/system: 解除 -cloud 对 -bus 的直接依赖，将共享类型迁移到 -api
- [ ] **P1-003** carlos-auth/audit/message/org/system: 为 Feign 接口补充 FallbackFactory（覆盖率提升至 ≥80%）
- [ ] **P2-004** carlos-auth: 将 `InMemoryRegisteredClientRepository` 替换为持久化实现（Jdbc/Redis），并修正 `SpringUtil.getBean()` 为构造函数注入
- [ ] **P2-005** carlos-gateway: 实现 `RateLimitAlertListener` 的告警对接（钉钉/邮件/Alertmanager）
- [ ] **P3-006** carlos-tools: 清理超过 6 个月的 TODO 标记（16+ 个）

### 9.2 历史遗留跟踪

- [ ] **P0-004** HealthIndicator 包路径错误（框架扫描昨日遗留 P0） ❌ 未修复（21 个文件，含 audit 2 个）
- [ ] **P1-005** spring-boot-jackson2 不存在依赖（框架扫描昨日遗留 P1） ❌ 未修复
- [ ] **P2-006** Fastjson autoType 安全风险（框架扫描昨日遗留 P2） ⏳ 持续跟踪

---

## 十、正面发现总结

### 10.1 架构合规亮点

| 检查项 | 结果 | 评价 |
|--------|------|------|
| 全部业务模块 README.md | ✅ 8/8 | 文档完整性优秀 |
| `-api` 不依赖 `-bus` | ✅ 5/5 | 接口层与业务层分离良好 |
| `BeanUtils.copyProperties` | 0 个 | MapStruct 使用规范 |
| MD5/SHA1/NoOp PasswordEncoder | 0 个 | 密码安全策略合规 |
| license-generate 非全局依赖 | 0 个 | 安全红线通过 |
| Service 直接引用 Mapper | 0 个 | 分层架构执行到位 |

### 10.2 代码质量亮点

- **carlos-auth OAuth2 配置**：`OAuth2ClientConfig` 的客户端配置支持动态 grant type、scope、redirect URI，设计灵活
- **carlos-gateway OAuth2 属性配置**：`OAuth2GatewayProperties` 支持 JWT/Opaque Token 双模式、PKCE、RBAC/ABAC 双模式，配置丰富
- **carlos-license**：模块结构清晰，安全红线合规，评分满分

---

*本报告由 OpenClaw 定时任务自动生成，问题定位基于静态代码分析，具体修复前请人工复核。*
