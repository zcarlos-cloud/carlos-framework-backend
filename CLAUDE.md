# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 提供本代码库的开发指导和规范。

> **⚠️ 编码规范已迁移至 Skill**
>
> 详细的编码规范（分层架构、命名规范、数据查询、Redis 缓存、异常处理等）已整理为 Skill：
> `.claude/skills/carlos-framework-standard/SKILL.md`
>
> **编写代码前请务必阅读该 Skill！**

---

## 项目概述

**Carlos Framework** 是基于 Spring Boot 4.0.6 (JDK 21) 和 Spring Cloud Alibaba 的 Java 微服务框架。项目采用多模块 Maven
构建结构，具有清晰的分层架构：

- **carlos-dependencies**: 集中式依赖版本管理 (BOM)
- **carlos-parent**: 统一父 POM，包含构建配置和插件管理
- **carlos-commons**: 与框架无关的公共工具类 (utils)
- **carlos-spring-boot**: Spring Boot 集成层，包含 23 个模块（21 个 starter = 20 个 `carlos-spring-boot-starter-*` + `carlos-spring-cloud-starter`，另加 core 与 redis 聚合器）
- **carlos-integration**: 第三方集成与业务模块 (agent, audit, auth, gateway, job, license, message, org, system, tools)
- **carlos-samples**: 示例应用程序和测试模块 (18 个 = 17 个 carlos-sample-* + carlos-test)
- **carlos-ui**: 前端 Vue3 管理后台应用
- **carlos-framework-portal**: 前端 Vue3 应用门户（统一应用入口，SSO 单点登录）

这是一个 MIT 协议开源的框架/脚手架项目，旨在通过提供预构建的集成和工具来加速应用开发。

---

## Maven 命令

### 构建项目

```bash
# 构建所有模块 (pom.xml 中默认跳过测试)
mvn clean install

# 使用指定 profile 构建 (内部 Nexus 仓库)
mvn clean install -P carlos-public    # 公共服务器: zcarlos.com:8081
mvn clean install -P carlos-private   # 私有服务器: 192.168.3.30:8081

# 构建指定模块
cd carlos-commons/carlos-utils
mvn clean install

# 构建 Spring Boot starters
cd carlos-spring-boot
mvn clean install

# 部署到 Nexus
mvn clean deploy -P carlos-public
```

### 运行测试

```bash
# 运行单元测试 (pom.xml 配置中当前已跳过)
mvn test

# 运行指定测试
mvn test -Dtest=ClassName#methodName

# 运行集成测试
mvn verify
```

### 依赖管理

```bash
# 检查依赖更新
mvn versions:display-dependency-updates

# 检查插件更新
mvn versions:display-plugin-updates

# 升级到最新版本
mvn versions:use-latest-versions

# 查看有效 POM
mvn help:effective-pom
```

---

## 架构

### 模块结构

```
carlos-framework/                          # 根聚合 POM
├── carlos-dependencies/                   # 依赖版本管理 (BOM)
├── carlos-parent/                         # 父 POM (构建配置、插件)
├── carlos-commons/                        # 与框架无关的工具类
│   └── carlos-utils/                      # 公共工具函数
├── carlos-spring-boot/                    # Spring Boot 集成层（23 个模块 = 21 starters + core + redis 聚合器）
│   ├── carlos-spring-boot-core/           # 核心框架组件
│   ├── carlos-spring-boot-redis/          # Redis 聚合器 (含3个子模块)
│   ├── carlos-spring-boot-starter-ai/
│   ├── carlos-spring-boot-starter-apm/
│   ├── carlos-spring-boot-starter-datacenter/
│   ├── carlos-spring-boot-starter-datascope/
│   ├── carlos-spring-boot-starter-disruptor/
│   ├── carlos-spring-boot-starter-encrypt/
│   ├── carlos-spring-boot-starter-integration/
│   ├── carlos-spring-boot-starter-json/
│   ├── carlos-spring-boot-starter-log/
│   ├── carlos-spring-boot-starter-migration/
│   ├── carlos-spring-boot-starter-mongodb/
│   ├── carlos-spring-boot-starter-mq/
│   ├── carlos-spring-boot-starter-mybatis/
│   ├── carlos-spring-boot-starter-openapi/
│   ├── carlos-spring-boot-starter-oss/
│   ├── carlos-spring-boot-starter-security/
│   ├── carlos-spring-boot-starter-sms/
│   ├── carlos-spring-boot-starter-snowflake/
│   ├── carlos-spring-boot-starter-translation/
│   ├── carlos-spring-boot-starter-web/
│   └── carlos-spring-cloud-starter/       # Spring Cloud Alibaba
├── carlos-integration/                    # 第三方集成与业务模块（10 个）
│   ├── carlos-license/                    # 软件许可证 (TrueLicense)
│   │   ├── carlos-license-core/
│   │   ├── carlos-spring-boot-starter-license-generate/
│   │   └── carlos-spring-boot-starter-license-verify/
│   ├── carlos-tools/                      # GUI 工具 (代码生成器)
│   ├── carlos-auth/                       # OAuth2 认证授权
│   ├── carlos-gateway/                    # Spring Cloud Gateway
│   ├── carlos-message/                    # 消息中心
│   ├── carlos-system/                     # 系统管理
│   ├── carlos-org/                        # 组织管理
│   ├── carlos-audit/                      # 审计日志
│   ├── carlos-agent/                      # AI 助手代理层（管理平台 ↔ DeepSeek Harness 运行时）
│   └── carlos-job/                        # 定时任务调度中心
├── carlos-samples/                        # 示例和测试（18 个模块 = 17 个 carlos-sample-* + carlos-test）
│   ├── carlos-sample-apm/
│   ├── carlos-sample-cloud/
│   ├── carlos-sample-datacenter/
│   ├── carlos-sample-datascope/
│   ├── carlos-sample-disruptor/
│   ├── carlos-sample-docking/
│   ├── carlos-sample-encrypt/
│   ├── carlos-sample-log/
│   ├── carlos-sample-mongodb/
│   ├── carlos-sample-mq/
│   ├── carlos-sample-mybatis/
│   ├── carlos-sample-openapi/
│   ├── carlos-sample-oss/
│   ├── carlos-sample-redis/
│   ├── carlos-sample-sms/
│   ├── carlos-sample-snowflake/
│   ├── carlos-sample-web/
│   └── carlos-test/                       # 测试应用
├── carlos-ui/                             # 前端 Vue3 管理后台
└── carlos-framework-portal/                         # 前端 Vue3 应用门户 (SSO 统一入口)
    ├── src/
    │   ├── router/                        # 路由 + SSO 登录守卫
    │   ├── store/modules/auth/            # Token/PKCE 状态管理
    │   ├── store/modules/portal/          # 应用列表状态
    │   ├── service/                       # API 请求 (Axios + Bearer)
    │   ├── utils/pkce.ts                  # PKCE 工具
    │   ├── views/portal/                  # 门户首页 (应用卡片网格)
    │   └── views/oauth2-callback/         # OAuth2 回调页
    ├── e2e/                               # Playwright E2E 测试
    └── docs/                              # SSO 调试指南等文档
```

**构建顺序：**

1. carlos-dependencies (BOM)
2. carlos-parent (父 POM)
3. carlos-commons (1 个模块)
4. carlos-spring-boot (23 个模块 = 21 starters + core + redis 聚合器)
5. carlos-integration (10 个业务模块，含 license 3 个子模块与 carlos-tools)
6. carlos-samples (18 个模块 = 17 个 carlos-sample-* + carlos-test)

### 核心组件模块

**Commons 层 (与框架无关)：**

- `carlos-utils`: 公共工具类和辅助函数

**Spring Boot 集成层 (23 个模块 = 21 starters + core + redis 聚合器)：**

*核心基础设施：*

- `carlos-spring-boot-core`: 基础抽象、注解、AOP、异常、分页、响应包装器
- `carlos-spring-boot-starter-web`: Spring Boot 自动配置和 starter 支持
- `carlos-spring-cloud-starter`: Spring Cloud Alibaba 集成 (Nacos, Sentinel)
- `carlos-spring-boot-starter-json`: JSON 序列化 (Fastjson 2.0.61)

*数据访问：*

- `carlos-spring-boot-starter-mybatis`: MyBatis-Plus 集成，支持多数据源
- `carlos-spring-boot-starter-mongodb`: MongoDB 集成
- `carlos-spring-boot-redis`: Redis + Redisson + Caffeine 聚合器 (含 core/starter/reactive 3 个子模块)
- `carlos-spring-boot-starter-datascope`: 数据权限/范围控制

*存储与消息：*

- `carlos-spring-boot-starter-oss`: OSS (对象存储服务) 抽象
- `carlos-spring-boot-starter-mq`: 消息队列抽象

*安全与认证：*

- `carlos-spring-boot-starter-encrypt`: 加密工具 (国密 SM2, SM4)
- `carlos-spring-boot-starter-security`: Spring Security / OAuth2 认证和授权

*集成：*

- `carlos-spring-boot-starter-integration`: 第三方集成框架 (钉钉、荣之通)
- `carlos-spring-boot-starter-datacenter`: 数据中心集成/同步
- `carlos-spring-boot-starter-sms`: 短信发送抽象，支持多提供商

*可观测性：*

- `carlos-spring-boot-starter-log`: 日志增强
- `carlos-spring-boot-starter-apm`: APM 集成 (SkyWalking 9.5.0)

*工具类：*

- `carlos-spring-boot-starter-openapi`: OpenAPI/Swagger 文档支持 (Knife4j)
- `carlos-spring-boot-starter-snowflake`: 分布式 ID 生成
- `carlos-spring-boot-starter-disruptor`: Disruptor 高性能队列组件
- `carlos-spring-boot-starter-migration`: 数据库迁移 (Liquibase)
- `carlos-spring-boot-starter-translation`: 字段翻译
- `carlos-spring-boot-starter-ai`: AI / LangChain4j 集成

**Integration 层 (10 个业务模块)：**

- `carlos-agent`: AI 助手代理层 (管理平台 ↔ DeepSeek Harness 运行时，含 boot/bus 2 个子模块)
- `carlos-audit`: 审计日志 (api/bus/cloud/boot 4 个子模块)
- `carlos-auth`: OAuth2 认证授权 (api/service/boot/cloud 4 个子模块)
- `carlos-gateway`: Spring Cloud Gateway 网关
- `carlos-job`: 定时任务调度中心 (api/boot/bus/client/cloud 5 个子模块)
- `carlos-license`: 基于 TrueLicense 的软件许可证系统 (3 个子模块)
    - `carlos-license-core`: 核心许可证功能
    - `carlos-spring-boot-starter-license-generate`: 许可证生成 (仅开发使用)
    - `carlos-spring-boot-starter-license-verify`: 许可证验证 (生产使用)
- `carlos-message`: 统一消息中心 (api/core/bus/cloud 4 个子模块)
- `carlos-org`: 组织与用户中心 (api/bus/cloud/boot 4 个子模块)
- `carlos-system`: 系统管理 (api/bus/cloud/boot 4 个子模块)
- `carlos-tools`: GUI 桌面工具 (代码生成器、项目脚手架、基于 Swing)

**Samples (18 个模块)：**

- 17 个 carlos-sample-* 示例应用 (apm, cloud, datacenter, datascope, disruptor, docking, encrypt, log, mongodb, mq, mybatis, openapi, oss, redis, sms, snowflake, web)
- `carlos-test`: 演示框架用法的测试应用

**前端：**

- `carlos-ui`: Vue 3.5 + Vite 7.3 + Ant Design Vue 管理后台前端应用
- `carlos-framework-portal`: Vue 3.5 + Vite 7.3 + Ant Design Vue 应用门户前端 (SSO 统一入口)

### 技术栈 (来自 carlos-dependencies/pom.xml)

| 组件                   | 版本         | 说明                     |
|----------------------|------------|------------------------|
| Spring Boot          | 4.0.6      |                        |
| Spring Cloud         | 2025.1.1   |                        |
| PostgreSQL           | 14+        | 主业务数据库                 |
| ClickHouse           | -          | 审计日志存储                 |
| Spring Cloud Alibaba | 2025.1.0.0 | Nacos, Sentinel, Seata |
| JDK                  | 21         | 最低版本要求                 |
| Maven                | 3.8+       | 最低版本要求                 |
| MyBatis-Plus         | 3.5.16     | 带 Join 扩展 1.5.7        |
| Seata                | 2.5.0      | 分布式事务                  |
| Hutool               | 5.8.40     | 工具库                    |
| MapStruct            | 1.6.3      | Bean 映射                |
| Guava                | 33.4.8-jre |                        |
| Druid                | 1.2.28     | 数据库连接池                 |
| SkyWalking           | 9.5.0      | APM 追踪                 |
| Redisson             | 4.5.0      | Redis 客户端              |
| Fastjson             | 2.0.61     | JSON 序列化               |
| BouncyCastle         | 1.81       | 加密算法库                  |

### 应用分层架构

```
                        外部服务调用
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  API 接口层 (api)   │  Feign 接口定义，对外暴露服务              │
│  - ApiXxx.java      │  提供熔断降级工厂 (fallback)              │
│  - AO/Param         │  用于微服务间调用                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
                        HTTP 请求
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  API 实现层 (apiimpl)│  Feign 接口的 REST 实现                  │
│  - ApiXxxImpl.java  │  同时暴露给三方服务的 HTTP 端点            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Controller 层    │  接收 Web 请求，完成基本参数校验           │
│  (controller/)    │  Param → DTO 转换，调用 Service           │
│                   │  避免在 Controller 中加入任何复杂逻辑      │
│                   │  使用 MapStruct Convert 进行对象转换       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Service 层       │  业务逻辑服务层，纯粹的进行业务串联        │
│  (service/)       │  处理业务流程，避免直接数据操作            │
│                   │  通过 Manager 层进行数据获取               │
│                   │  作为 Controller 和 Manager 的桥梁         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Manager 层       │  数据查询封装层，继承 BaseService          │
│  (manager/)       │  实现增删改查等原子操作                    │
│                   │  与 Mapper 层交互，处理数据持久化          │
│                   │  接口在 manager/，实现在 manager/impl/     │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  Mapper/Repository│  数据访问层，使用 MyBatis 实现             │
│  (mapper/)        │  与 MySQL、ES、MongoDB、Oracle 等交互      │
│  (resources/mapper/)│ XML 映射文件                            │
└─────────────────────────────────────────────────────────────────┘
```

**分层领域模型：**

| 模型         | 名称       | 所在包           | 说明                                                                |
|------------|----------|---------------|-------------------------------------------------------------------|
| **Param**  | 参数对象     | `pojo.param`  | 前端参数接收对象，按操作细分：`XxxCreateParam`, `XxxUpdateParam`, `XxxPageParam` |
| **DTO**    | 数据传输对象   | `pojo.dto`    | 服务层与数据层之间传输的对象                                                    |
| **VO**     | 视图对象     | `pojo.vo`     | 显示层对象，响应给前端，需标注 Swagger 注解                                        |
| **AO**     | API 对象   | `pojo.ao`     | API 接口响应对象，用于 Feign 调用方                                           |
| **Entity** | 实体对象     | `pojo.entity` | 与数据库表结构一一对应，DO（Data Object）                                       |
| **Excel**  | Excel 对象 | `pojo.excel`  | 导入导出专用对象                                                          |
| **Enum**   | 枚举       | `pojo.enums`  | 业务枚举类型，需实现 `BaseEnum` 接口，使用 `@AppEnum` 注解标记                       |

### 模块结构说明

#### API 模块 (`{service}-api`)

```
src/main/java/com/carlos/{service}/
├── api/                           # Feign 接口定义
│   ├── ApiXxx.java               # 对外暴露的 Feign 接口
│   └── fallback/                 # 熔断降级工厂
│       └── ApiXxxFallbackFactory.java
└── pojo/
    ├── ao/                       # API 响应对象
│   └── XxxAO.java
    └── param/                    # API 请求参数
        └── ApiXxxParam.java
```

#### Business 模块 (`{service}-bus`)

```
src/main/java/com/carlos/{service}/
├── apiimpl/                       # Feign 接口实现
│   └── ApiXxxImpl.java
├── config/                        # 配置类
├── controller/                    # Web 控制器层
├── convert/                       # MapStruct 对象转换
├── exception/                     # 自定义异常
├── manager/                       # 数据查询封装层
│   ├── XxxManager.java
│   └── impl/XxxManagerImpl.java
├── mapper/                        # MyBatis Mapper 接口
├── pojo/                          # 领域对象
│   ├── dto/
│   ├── enums/
│   ├── entity/
│   ├── excel/
│   ├── param/
│   └── vo/
└── service/                       # Service 层（只有接口）

src/main/resources/
└── mapper/                        # MyBatis XML 映射文件
    └── {service}/
        └── XxxMapper.xml
```

### 依赖版本管理

框架采用集中式依赖管理方案：

1. **carlos-dependencies**: 使用 `<dependencyManagement>` 定义所有依赖版本
2. **carlos-parent**: 导入 carlos-dependencies BOM 并提供构建配置
3. **所有模块**: 继承自 carlos-parent，引用依赖时无需指定版本

使用 `${revision}` 占位符模式 (当前为 `3.0.0-SNAPSHOT`) 和 `flatten-maven-plugin` 集中管理版本。

### 配置模式

框架使用 Spring Boot 的自动配置机制。模块通常提供：

- 带 `@Configuration` 和 `@ConditionalOnProperty` 的 AutoConfiguration 类
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
- 使用 `carlos.*` 前缀命名空间的 `@ConfigurationProperties` 类

配置结构示例：

```yaml
carlos:
  boot:
    cors:
      allowed-origins: [...]
    enums:
      scan-package: com.carlos
      enabled: true
  encrypt:
    sm4:
      enabled: true
      encrypt-mode: cbc
  docking:
    dingtalk:
      enabled: ${DINGTALK_ENABLE:false}
```

### 许可证模块架构

`carlos-license` 模块 (位于 `carlos-integration/`) 采用安全设计：

- `carlos-license-core`: 共享模型和抽象
- `carlos-spring-boot-starter-license-generate`: 证书生成 (不应包含在生产部署中)
- `carlos-spring-boot-starter-license-verify`: 证书验证 (应用中应包含此模块)

使用 TrueLicense 验证硬件指纹 (IP, MAC, CPU 序列号、主板序列号) 和时间限制。

---

## Spring Boot 4.x 迁移说明

本框架已从 Spring Boot 2.7 迁移到 Spring Boot 4.0.6。主要迁移变更包括：

### Jakarta EE 命名空间

- 所有 `javax.*` 导入已迁移到 `jakarta.*`
- 主要包: `jakarta.servlet`, `jakarta.validation`, `jakarta.annotation`

### Spring Security 6.x

- 使用 `SecurityFilterChain` 替代弃用的 `WebSecurityConfigurerAdapter`
- 使用 `requestMatchers()` 替代 `antMatchers()`
- 新的 Lambda DSL 配置风格
- 使用 `@EnableMethodSecurity` 替代 `@EnableGlobalMethodSecurity`

### MyBatis-Plus 3.5.x

- 使用 `mybatis-plus-spring-boot3-starter` 以兼容 Spring Boot 4
- `PaginationInnerInterceptor` 需要显式指定数据库类型
- `IdentifierGenerator.nextId()` 返回 `Long` 而不是 `Number`

### MySQL Connector

- `mysql-connector-java` (groupId: `mysql`) 已替换为 `mysql-connector-j` (groupId: `com.mysql`)

### Spring Authorization Server (OAuth2)

- 基于 Spring Authorization Server 1.x
- **重要**: Password 授予类型不再支持
- 使用 `authorization_code` + PKCE 进行安全认证

### AutoConfiguration 注册

- 使用新的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 格式
- 不再使用弃用的 `spring.factories` 文件

---

## 开发规范速查

### 强制规范（违反会导致代码被拒绝）

| 规范              | 要求                                              | 禁止                    |
|-----------------|-------------------------------------------------|-----------------------|
| **数据查询**        | 使用 MyBatis-Plus + mybatis-plus-join 在 Manager 层 | 严禁直接编写 SQL            |
| **Redis 缓存**    | 在 Manager 层统一实现                                 | Service 层禁止直接操作 Redis |
| **异常处理**        | 使用框架定义异常类（ServiceException 等）                   | 严禁使用 RuntimeException |
| **Lombok**      | 使用注解生成 getter/setter                            | 严禁手写 get/set 方法       |
| **属性注入**        | 使用 @ConfigurationProperties                     | 严禁使用 @Value           |
| **Service 返回值** | 返回 DTO 或 Entity                                 | 严禁返回 VO               |
| **枚举类**         | 实现 BaseEnum，使用 @AppEnum                         | 普通枚举定义                |

### 不可变性 (Immutability)

始终创建新对象，**严禁**修改原对象 (Mutation)：

```java
// 错误：修改原对象 (Mutation)
public User updateUserName(User user, String name) {
    user.setName(name);  // 直接修改了原对象！
    return user;
}

// 正确：不可变性 (Immutability)
public User updateUserName(User user, String name) {
    return User.builder()
        .id(user.getId())
        .name(name)
        .email(user.getEmail())
        .build();
}
```

### 代码格式

1. **大括号规范**：左大括号前不换行，后换行；右大括号前换行
2. **空格规范**：`if/for/while` 等保留字与括号之间必须加空格
3. **缩进**：采用 4 个空格缩进，禁止使用 tab 字符
4. **注释**：双斜线与注释内容之间有且仅有一个空格：`// 这是注释`
5. **行长度**：单行字符数限制不超过 120 个
6. **方法行数**：单个方法的总行数不超过 80 行

### 添加新模块

**Spring Boot Starters：**

1. 在 `carlos-spring-boot/` 下创建模块
2. 在 `carlos-spring-boot/pom.xml` 中添加 `<module>` 条目
3. 设置父项目为 `carlos-spring-boot`，版本为 `${revision}`
4. 遵循命名约定: `carlos-spring-boot-starter-{function}`
5. 在 `carlos-dependencies/pom.xml` 中添加依赖管理条目

**公共工具类：**

1. 在 `carlos-commons/` 下创建模块
2. 在 `carlos-commons/pom.xml` 中添加 `<module>` 条目
3. 设置父项目为 `carlos-commons`，版本为 `${revision}`
4. 遵循命名约定: `carlos-{function}`
5. 确保无 Spring Boot 依赖（保持与框架无关）

### 模块依赖规则

**分层结构：**

```
carlos-ui (前端应用)
    ↓
carlos-samples (18 个测试应用 = 17 个 carlos-sample-* + carlos-test)
    ↓
carlos-integration (10 个业务模块)
    ↓
carlos-spring-boot (23 个模块 = 21 starters + core + redis 聚合器)
    ↓
carlos-commons (utils)
    ↓
carlos-parent (构建配置)
    ↓
carlos-dependencies (BOM)
```

**禁止事项：**

- ❌ Commons 模块依赖 Spring Boot
- ❌ 循环依赖
- ❌ 子模块硬编码版本号
- ❌ 生产环境包含 license-generate 模块
- ❌ Service 层直接引用 Mapper

---

## Git 工作流

### 提交信息格式

```
<type>: <description>

<optional body>
```

类型 (Types): feat, fix, refactor, docs, test, chore, perf, ci

**示例：**

```
feat: 添加用户登录功能

- 实现用户名密码登录
- 集成 JWT token
- 添加登录日志记录
```

### 拉取请求 (Pull Request) 工作流

创建 PR 时：

1. 分析完整的提交历史（不仅是最近一次提交）
2. 使用 `git diff [base-branch]...HEAD` 查看所有变更
3. 起草详尽的 PR 摘要
4. 包含带有 TODO 的测试计划
5. 如果是新分支，使用 `-u` 参数推送

### 功能实现工作流

1. **规划先行**
    - 使用 **planner** 智能体创建实现计划
    - 识别依赖关系与风险
    - 拆分为多个阶段

2. **测试驱动开发 (TDD)**
    - 先编写测试 (RED)
    - 运行测试 - 应当失败 (FAIL)
    - 编写最简实现代码 (GREEN)
    - 运行测试 - 应当通过 (PASS)
    - 重构 (IMPROVE)
    - 验证 80% 以上的覆盖率

3. **代码评审**
    - 在编写代码后立即使用 **code-reviewer** 智能体
    - 解决严重 (CRITICAL) 和高 (HIGH) 等级的问题
    - 尽可能修复中 (MEDIUM) 等级的问题

---

## 测试要求

### 基本原则（AIR）

- **A**：Automatic（自动化）
- **I**：Independent（独立性）
- **R**：Repeatable（可重复）

### 强制要求

1. 单元测试应该是全自动执行的，并且非交互式的，必须使用 `assert` 来验证
2. 单元测试用例之间决不能互相调用，也不能依赖执行的先后次序
3. 单元测试是可以重复执行的，不能受到外界环境的影响
4. 测试粒度至多是类级别，一般是方法级别
5. 核心业务、核心应用、核心模块的增量代码确保单元测试通过
6. 单元测试代码必须写在 `src/test/java`，不允许写在业务代码目录下

### 最低测试覆盖率: 80%

测试类型（全部必选）：

1. **单元测试** - 独立函数、工具类、组件 (JUnit 5)
2. **集成测试** - API 端点、数据库操作 (MockMvc)
3. **端到端测试** - 关键用户流程 (Playwright)

### 测试命名约定

- 单元测试类: `*Test.java` (位于 `src/test/java`)
- 集成测试类: `*IT.java` (使用 maven-failsafe-plugin)
- 测试方法: 使用 `should_xxx_when_xxx` 或 `test_xxx` 格式

```java
@Test
@DisplayName("应该成功创建用户当参数合法时")
void should_create_user_successfully_when_params_valid() {
    // given
    UserCreateParam param = new UserCreateParam();
    param.setUsername("testuser");
    param.setEmail("test@example.com");

    // when
    User user = userService.createUser(param);

    // then
    assertNotNull(user);
    assertEquals("testuser", user.getUsername());
    verify(userMapper, times(1)).insert(any(User.class));
}
```

### BCDE 原则

编写单元测试遵守 BCDE 原则：

- **B**：Border，边界值测试
- **C**：Correct，正确的输入，并得到预期的结果
- **D**：Design，与设计文档相结合
- **E**：Error，强制错误信息输入

### Mock 使用规范

- 使用 `@Mock` 和 `@InjectMocks` 创建 Mock 对象
- 使用 `@ExtendWith(MockitoExtension.class)` 启用 Mockito
- 明确验证 Mock 对象的交互行为
- 避免过度 Mock，只 Mock 外部依赖

### 数据库测试

- 对于数据库相关的查询，更新，删除等操作，不能假设数据库里的数据是存在的
- 和数据库相关的单元测试，可以设定自动回滚机制，不给数据库造成脏数据
- 或者对单元测试产生的数据有明确的前后缀标识

---

## 安全规约

### 强制安全检查 (提交前必须检查)

- [ ] 无硬编码凭据（API 密钥、密码、令牌/Tokens）
- [ ] 所有用户输入均已验证
- [ ] 预防 SQL 注入（使用 MyBatis-Plus，使用 `#{ }` 而非 `${ }`）
- [ ] 预防 XSS（对 HTML 进行净化处理）
- [ ] 已启用 CSRF 保护
- [ ] 身份验证/授权已验证
- [ ] 所有端点均已设置速率限制
- [ ] 错误消息不泄露敏感数据

### 安全规范详情

1. **权限控制**：隶属于用户个人的页面或者功能必须进行权限控制校验
2. **数据脱敏**：用户敏感数据禁止直接展示，必须对展示数据进行脱敏
3. **SQL 注入**：用户输入的 SQL 参数严格使用参数绑定或 `#{ }`
4. **参数校验**：用户请求传入的任何参数必须做有效性验证
5. **XSS 防护**：禁止向 HTML 页面输出未经安全过滤或未正确转义的用户数据
6. **CSRF**：表单、AJAX 提交必须执行 CSRF 安全验证
7. **防重放**：使用平台资源（短信、邮件、电话、下单、支付）必须实现防重放机制
8. **防刷策略**：发贴、评论、发送即时消息等场景必须实现防刷、文本内容违禁词过滤

### 凭据管理

```java
// 严禁：硬编码凭据
private String apiKey = "sk-proj-xxxxx";

// 严禁：使用 @Value 注解
@Value("${api.key}")
private String apiKey;

// ✅ 推荐：使用 Properties 类
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String apiKey;
}

// 在启动时验证
@PostConstruct
public void init() {
    if (StringUtils.isEmpty(appProperties.getApiKey())) {
        throw new IllegalStateException("API_KEY not configured");
    }
}
```

---

## 代码生成工具

`carlos-tools` 模块 (位于 `carlos-integration/`) 提供基于 Swing 的 GUI，用于：

- 数据库代码生成 (从 MySQL 模式生成 MyBatis/MongoDB/Elasticsearch 代码)
- 项目脚手架
- 加密工具
- GitLab 集成

运行方式: 执行 `com.carlos.fx.ToolsApplication.main()` 方法

### 加密标准

本框架在某些模块中使用国密标准 (SM2/SM4) 而不是 RSA/AES。

---

## 重要注意事项

- **Git 仓库**: 根目录是 Git 仓库
- **多 Profile 支持**: 存在两个 Nexus 仓库 profile
    - `carlos-public`: 公共服务器 zcarlos.com:8081
    - `carlos-private`: 私有服务器 192.168.3.30:8081
- **需要 JDK 21**: 这是 Spring Boot 4 项目，需要 JDK 21+
- **需要 Maven 3.8+**: 最低 Maven 版本为 3.8
- **仅限内部使用**: 此框架仅限内部使用
- **父版本**: 在子 POM 中使用 `${revision}`，不要使用硬编码版本
- **安全注意**: 切勿在生产制品中包含 `carlos-spring-boot-starter-license-generate` 模块
- **分层架构**: 框架遵循清晰的分层架构
- **Spring Boot 4.x**: 框架已从 Spring Boot 2.7 升级到 4.0.6

---

## 近期更新

### carlos-auth 模块 (2026-01-25 新增)

基于 Spring Security OAuth2 Authorization Server 的综合 OAuth2 认证和授权模块。

**主要特性：**

- OAuth2 Authorization Server，支持多种授权类型
- OAuth2 Resource Server，支持 JWT 验证
- 自定义 JWT token 增强，包含用户上下文
- 与 carlos-spring-boot-core 认证系统集成
- 基于 @PreAuthorize 注解的方法级安全控制

**配置示例：**

```yaml
carlos:
  oauth2:
    enabled: true
    authorization-server:
      enabled: true
      access-token-time-to-live: 2h
    jwt:
      issuer: http://localhost:8080
```

### Spring Boot 4.0 升级 (2026-04-27 完成)

框架已从 Spring Boot 2.7 升级到 Spring Boot 4.0.6。

**主要变更：**

- 所有 `javax.*` 包已迁移到 `jakarta.*`
- 从 `WebSecurityConfigurerAdapter` 迁移到 `SecurityFilterChain`
- 使用 `mybatis-plus-spring-boot3-starter`
- `mysql-connector-java` → `mysql-connector-j`
- Spring Cloud 升级至 2025.1.1，Spring Cloud Alibaba 升级至 2025.1.0.0

### 架构重构 (2026-02-01)

- `carlos-spring-boot-framework` → `carlos-framework`
- `carlos-spring-boot-dependencies` → `carlos-dependencies`
- `carlos-spring-boot-parent` → `carlos-parent`
- `carlos-spring-boot-commons` → `carlos-commons`
- `carlos-spring-boot-starters` → `carlos-spring-boot`
- 创建 `carlos-integration/` 用于第三方集成
- 创建 `carlos-samples/` 用于示例和测试

### Redis 模块增强 (2026-02-01)

- 将 `carlos-spring-boot-starter-redisson` 合并到 `carlos-spring-boot-starter-redis`
- 添加 Caffeine 本地缓存集成
- 添加多级缓存支持 (Caffeine L1 + Redis L2)
- 统一缓存方案：一个模块中包含 Redis + Redisson + Caffeine

### carlos-framework-portal 应用门户 (2026-07 新增)

独立的前端门户应用，作为统一应用入口。基于 OAuth2 Authorization Code + PKCE 实现 SSO 单点登录。

**架构定位：**

```
carlos-framework-portal (前端门户, :5200)
    ↓ SSO 登录
carlos-auth (IdP 认证中心, :9511)
    ↓ API 调用
carlos-gateway (网关, :9510) → 各后端微服务
```

**技术栈：** Vue 3.5 + Vite 7.3 + Ant Design Vue 4.x + Pinia + UnoCSS

**SSO 流程：**

1. 用户访问门户 → 路由守卫检测无 Token → 重定向到 IdP `/oauth2/authorize`（携带 PKCE code_challenge）
2. 用户在 IdP 登录 → IdP 签发授权码 code → 302 回调到门户 `/oauth2/callback`
3. 门户用 code + code_verifier 换取 Access Token（JWT）
4. 门户携带 Bearer Token 调用后端 API（通过 Gateway）

**后端支撑（carlos-auth 中）：**

- `com.carlos.auth.portal.controller.PortalController` — Portal API 端点
- `com.carlos.auth.portal.service.PortalAppService` — 应用列表业务逻辑
- `com.carlos.auth.portal.manager.PortalAppManager` — 应用数据查询
- OAuth2 客户端注册: `carlos-framework-portal`（public client, PKCE, 无 secret）

**服务端口分配：**
| 服务 | 端口 | 说明 |
|------|------|------|
| carlos-framework-portal | 5200 | 应用门户前端 (Vite dev) |
| carlos-auth | 9511 | OAuth2 认证中心 (IdP) |
| carlos-gateway | 9510 | API 网关 |
| carlos-ui | 5173 | 管理后台前端 |

**相关文档：** `carlos-framework-portal/docs/SSO-DEBUG-GUIDE.md`

### 当前版本

- Spring Boot: **4.0.6**
- Spring Cloud: **2025.1.1**
- Spring Cloud Alibaba: **2025.1.0.0**
- MyBatis-Plus: **3.5.16**
- MyBatis-Plus Join: **1.5.7**
- SkyWalking: **9.5.0**
- Hutool: **5.8.40**
- MapStruct: **1.6.3**
- Guava: **33.4.8-jre**
- Redisson: **4.5.0**
- Seata: **2.5.0**
- Druid: **1.2.28**
- Fastjson: **2.0.61**
- BouncyCastle: **1.81**

---

## 参考文档

| 文档                                                  | 说明                  |
|-----------------------------------------------------|---------------------|
| `.claude/skills/carlos-framework-standard/SKILL.md` | **编码规范（必读）**        |
| `AGENTS.md`                                         | 项目概述、架构说明、安全注意事项    |
| `carlos-framework-portal/docs/SSO-DEBUG-GUIDE.md`   | Portal SSO 调试指南     |
| `OPTIMIZATION_REPORT.md`                            | 详细的优化历史和建议          |
| `README-REF.md`                                     | 框架快速参考              |
| 模块 README                                           | 每个模块都有自己的 README.md |
