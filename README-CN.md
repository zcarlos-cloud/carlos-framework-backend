<p align="center">
  <img src="docs/brand/carlos-banner.svg" alt="Carlos Framework" width="840" />
</p>

# Carlos 框架

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![官网](https://img.shields.io/badge/官网-www.zcarlos.com-6366F1.svg)](https://www.zcarlos.com)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)

[English](./README.md) | 中文 | [📚 官方文档](http://192.168.3.40:28080/project/carlos-framework-docs)

## 项目概述

**Carlos Framework** 是一个基于 Spring Boot 4.0.6 (JDK 21) 和 Spring Cloud Alibaba 的综合 Java 微服务框架。它提供了一个结构清晰的多模块
Maven 项目，包含 92 个 Maven 模块（`pom.xml` 计数，不含根聚合 POM），旨在加速企业级应用开发。

### 核心特性

- 🚀 **Spring Boot 4.0.6** & **Spring Cloud 2025.1.1** - 最新 Spring 生态
- 🏗️ **多模块架构** - 92 个组织良好的 Maven 模块
- 📦 **21 个 Starter 模块** - 开箱即用的集成（20 个 `carlos-spring-boot-starter-*` + `carlos-spring-cloud-starter`）
- 🔒 **OAuth2 认证服务器** - 内置认证授权系统
- 📊 **许可证管理** - 软件许可证验证（TrueLicense）
- 🛠️ **代码生成工具** - GUI 代码生成器和脚手架
- 📝 **数据权限控制** - 细粒度数据范围控制
- 💾 **多级缓存** - Redis + Redisson + Caffeine
- 📈 **APM 集成** - SkyWalking 支持
- 🔐 **国密算法** - SM2/SM4 加密标准

### 技术栈

| 技术                   | 版本                | 说明                   |
|----------------------|-------------------|----------------------|
| Spring Boot          | 4.0.6             | 应用框架                 |
| Spring Cloud         | 2025.1.1          | 微服务协调                |
| Spring Cloud Alibaba | 2025.1.0.0        | Nacos、Sentinel、Seata |
| MyBatis-Plus         | 3.5.16            | 增强 ORM 及 Join 扩展     |
| Redis                | 4.5.0 (Redisson)  | 分布式缓存                |
| Seata                | 2.5.0             | 分布式事务                |
| SkyWalking           | 9.5.0             | 应用性能监控               |
| Hutool               | 5.8.40            | 工具库                  |
| MapStruct            | 1.6.3             | Bean 映射              |
| Guava                | 33.4.8-jre        | Google 核心库           |

## 快速开始

### 环境要求

- JDK 21 或更高版本
- Maven 3.8 或更高版本
- MySQL / PostgreSQL / MongoDB（可选）
- Redis（可选）
- Nacos（可选，用于 Spring Cloud）

### 构建项目

```bash
# 克隆仓库
git clone https://github.com/yourusername/carlos-framework.git
cd carlos-framework

# 构建所有模块
mvn clean install

# 使用指定 profile 构建（内部 Nexus）
mvn clean install -P carlos-public    # 公共服务器: zcarlos.com:8081
mvn clean install -P carlos-private   # 私有服务器: 192.168.3.30:8081
```

### 运行测试

```bash
# 运行单元测试
mvn test

# 运行集成测试
mvn verify

# 运行指定测试
mvn test -Dtest=ClassName#methodName
```

## 模块结构

```
carlos-framework/                          # 根聚合器
├── carlos-dependencies/                   # 依赖管理 (BOM)
├── carlos-parent/                         # 父 POM
├── carlos-commons/                        # 与框架无关的工具类
│   └── carlos-utils/                      # 公共工具函数
├── carlos-spring-boot/                    # Spring Boot 集成 (23 个模块 = 21 starters + core + redis 聚合器)
│   ├── carlos-spring-boot-core/           # 核心框架组件
│   ├── carlos-spring-boot-redis/          # Redis + Redisson + Caffeine 聚合器
│   ├── carlos-spring-boot-starter-mybatis/  # MyBatis-Plus 集成
│   ├── carlos-spring-boot-starter-security/   # Spring Security / OAuth2
│   ├── carlos-spring-boot-starter-ai/     # AI / LangChain4j
│   └── ... (其他 18 个 starter 模块)
├── carlos-integration/                    # 第三方集成与业务模块（10 个）
│   ├── carlos-agent/                     # AI 助手代理层（DeepSeek Harness 运行时）
│   ├── carlos-audit/                     # 审计日志
│   ├── carlos-auth/                      # OAuth2 认证授权
│   ├── carlos-gateway/                   # Spring Cloud Gateway
│   ├── carlos-job/                       # 定时任务调度中心
│   ├── carlos-license/                   # 软件许可证管理
│   ├── carlos-message/                   # 消息中心
│   ├── carlos-org/                       # 组织与用户中心
│   ├── carlos-system/                    # 系统管理
│   └── carlos-tools/                     # GUI 代码生成器
├── carlos-samples/                        # 示例和测试 (18 个模块 = 17 个 carlos-sample-* + carlos-test)
│   ├── carlos-sample-apm/
│   ├── carlos-sample-web/
│   ├── carlos-sample-mybatis/
│   ├── carlos-sample-redis/
│   ├── carlos-sample-encrypt/
│   └── ... (其他 12 个 sample + carlos-test)
└── carlos-ui/                             # 前端 Vue3 应用
```

### 架构分层

框架遵循清晰的分层架构：

```
API 接口层 → API 实现层 → Controller → Service → Manager → Mapper
```

### 数据模型

- **Param** - 请求参数 (CreateParam、UpdateParam、PageParam)
- **DTO** - 数据传输对象 (服务层)
- **VO** - 视图对象 (响应给前端)
- **Entity** - 数据库实体 (DO)
- **Enum** - 业务枚举 (必须实现 BaseEnum)
- **Excel** - 导入导出对象

## 配置

### application.yml 示例

```yaml
spring:
  application:
    name: carlos-application
  datasource:
    url: jdbc:mysql://localhost:3306/carlos?useUnicode=true&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

carlos:
  boot:
    cors:
      allowed-origins:
        - "*"
    enums:
      scan-package: com.carlos
      enabled: true
  encrypt:
    sm4:
      enabled: true
      encrypt-mode: cbc
  oauth2:
    enabled: true
    jwt:
      issuer: http://localhost:8080
```

### Maven Profiles

提供两个部署 profile：

- `carlos-public` - 公共 Nexus 仓库 (zcarlos.com:8081)
- `carlos-private` - 私有 Nexus 仓库 (192.168.3.30:8081)

## 使用指南

### 创建新模块

1. **Spring Boot Starter:**

```bash
cd carlos-spring-boot/
# 创建新模块，遵循命名规范：
# carlos-spring-boot-starter-{function}
```

2. **微服务模块：**

```bash
cd carlos-integration/
# 创建三个子模块：
# {service}-api  (Feign 接口)
# {service}-bus  (业务逻辑)
# {service}-boot (启动应用)
```

### 使用代码生成器

GUI 代码生成器位于 `carlos-integration/carlos-tools/`：

```bash
cd carlos-integration/carlos-tools
# 运行应用
mvn spring-boot:run
# 或运行 com.carlos.fx.ToolsApplication.main()
```

功能：

- 数据库代码生成 (MySQL、MongoDB、Elasticsearch)
- 项目脚手架
- 加密工具
- GitLab 集成

### 使用 MapStruct 进行对象映射

```java
@Mapper(componentModel = "spring")
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserDTO toDTO(UserCreateParam param);
    UserVO toVO(UserDTO dto);
    List<UserVO> toVOList(List<UserDTO> dtoList);
}
```

## OAuth2 认证服务器

Carlos 框架内置 OAuth2 认证服务器：

### 配置

```yaml
carlos:
  oauth2:
    enabled: true
    authorization-server:
      enabled: true
      access-token-time-to-live: 2h
      refresh-token-time-to-live: 7d
    clients:
      - client-id: my-client
        client-secret: my-secret
        authorization-grant-types: [authorization_code, refresh_token]
        redirect-uris: [http://localhost:8080/authorized]
        scopes: [read, write]
```

### 端点

- `POST /oauth2/token` - 获取访问令牌
- `POST /oauth2/revoke` - 撤销令牌
- `GET /oauth2/jwks` - JWK 密钥集
- `GET /oauth2/authorize` - 授权端点

### 当前用户信息

```java
// 获取当前用户信息
UserContext userContext = OAuth2Util.extractUserContext();
Long userId = OAuth2Util.getCurrentUserId();
String userName = OAuth2Util.getCurrentUserName();

// 方法级安全控制
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { }
```

## 许可证管理

框架包含基于 TrueLicense 的软件许可证验证系统：

```bash
# 许可证生成（仅开发）
carlos-spring-boot-starter-license-generate

# 许可证验证（生产）
carlos-spring-boot-starter-license-verify
```

**重要：** 生产构建中切勿包含许可证生成模块。

## 最佳实践

### 编码规范

- 遵循阿里巴巴 Java 编码规范
- 函数保持小巧（< 50 行）
- 不可变性 - 始终创建新对象，从不修改
- 全面的错误处理
- 验证所有用户输入

### 安全检查清单

- [ ] 无硬编码凭证
- [ ] 所有输入已验证
- [ ] SQL 注入防护（使用 MyBatis-Plus `#{ }`）
- [ ] XSS 防护
- [ ] 启用 CSRF 保护
- [ ] 认证/授权已验证
- [ ] 所有端点已设置速率限制
- [ ] 错误消息不泄露敏感信息

### 测试要求

- 最低 80% 代码覆盖率
- 单元测试、集成测试和端到端测试
- 测试驱动开发 (TDD) 方法
- 独立且可重复的测试

## 文档

- [CLAUDE.md](./CLAUDE.md) - 开发指南和规范
- [OPTIMIZATION_REPORT.md](./OPTIMIZATION_REPORT.md) - 优化历史记录
- [README-REF.md](./README-REF.md) - 快速参考
- 模块 README - 每个模块都有自己的 README.md

## 贡献

我们欢迎贡献！请查看 [CONTRIBUTING.md](./CONTRIBUTING.md) 了解详情。

### 开发工作流

1. 使用 **planner** 智能体进行规划
2. 使用 **tdd-guide** 智能体进行测试驱动开发
3. 使用 **code-reviewer** 智能体进行代码审查
4. 使用 **security-reviewer** 智能体进行安全审查

### 提交规范

```
类型: 描述

可选正文
```

类型: `feat`、`fix`、`refactor`、`docs`、`test`、`chore`、`perf`、`ci`

## 社区

- Issues: [GitHub Issues](https://github.com/yourusername/carlos-framework/issues)
- Discussions: [GitHub Discussions](https://github.com/yourusername/carlos-framework/discussions)
- Gitee 镜像: [Gitee 仓库](https://gitee.com/yourusername/carlos-framework)

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](./LICENSE) 文件了解详情。

## 致谢

- Spring Boot 和 Spring Cloud 团队
- 阿里巴巴 Spring Cloud 团队
- MyBatis-Plus 团队
- Redisson 团队
- 所有贡献者和支持者

## Star 历史

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/carlos-framework&type=Date)](https://star-history.com/#yourusername/carlos-framework&Date)

---

**注意：** Carlos Framework 以 MIT 协议开源，官网 [www.zcarlos.com](https://www.zcarlos.com)。生产使用前请审查并调整配置。
