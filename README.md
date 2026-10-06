<p align="center">
  <img src="docs/brand/carlos-banner.svg" alt="Carlos Framework" width="840" />
</p>

# Carlos Framework

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Website](https://img.shields.io/badge/Website-www.zcarlos.com-6366F1.svg)](https://www.zcarlos.com)
[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.8+-red.svg)](https://maven.apache.org/)

[English](./README.md) | [中文](./README-CN.md) | [📚 官方文档](http://192.168.3.40:28080/project/carlos-framework-docs)

## Overview

**Carlos Framework** is a comprehensive Java microservice framework based on Spring Boot 4.0.6 (JDK 21) and Spring Cloud
Alibaba. It provides a well-structured multi-module Maven project with 92 Maven modules (`pom.xml` count excluding the
root aggregator POM) designed to accelerate enterprise application development.

### Key Features

- 🚀 **Spring Boot 4.0.6** & **Spring Cloud 2025.1.1** - Latest Spring ecosystem
- 🏗️ **Multi-module Architecture** - 92 well-organized Maven modules
- 📦 **21 Starter Modules** - Out-of-the-box integrations (20 × `carlos-spring-boot-starter-*` + `carlos-spring-cloud-starter`)
- 🔒 **OAuth2 Authorization Server** - Built-in authentication & authorization
- 📊 **License Management** - Software license verification (TrueLicense)
- 🛠️ **Code Generation Tools** - GUI-based code generator and scaffolding
- 📝 **Data Permission** - Granular data scope control
- 💾 **Multi-level Caching** - Redis + Redisson + Caffeine
- 📈 **APM Integration** - SkyWalking support
- 🔐 **Chinese Cryptography** - SM2/SM4 encryption standards

### Technology Stack

| Technology           | Version           | Description                        |
|----------------------|-------------------|------------------------------------|
| Spring Boot          | 4.0.6             | Application framework              |
| Spring Cloud         | 2025.1.1          | Microservices coordination         |
| Spring Cloud Alibaba | 2025.1.0.0        | Nacos, Sentinel, Seata             |
| MyBatis-Plus         | 3.5.16            | Enhanced ORM with Join extension   |
| Redis                | 4.5.0 (Redisson)  | Distributed caching                |
| Seata                | 2.5.0             | Distributed transactions           |
| SkyWalking           | 9.5.0             | Application performance monitoring |
| Hutool               | 5.8.40            | Utility library                    |
| MapStruct            | 1.6.3             | Bean mapping                       |
| Guava                | 33.4.8-jre        | Google core libraries              |

### Middleware & Dependencies

**Database & ORM**

- MySQL Connector: 8.0.33
- Druid: 1.2.27
- MyBatis-Plus Join: 1.5.4
- Dynamic DataSource: 4.3.1
- MongoDB Plus: 2.1.9
- DM JDBC: 8.1.3.62

**Cache & Storage**

- Redisson: 3.51.0
- Caffeine: 3.1.8
- MinIO: 8.5.7
- Aliyun OSS: 2.20.26

**Message Queue**

- RocketMQ: 5.3.1
- RocketMQ Client: 2.0.2

**Workflow & High Performance**

- Disruptor: 3.4.4

**Security & Encryption**

- Spring Security OAuth2: 6.2.7
- BouncyCastle: 1.70

**Monitoring & Diagnostics**

- Arthas: 4.1.5
- Spring Boot Admin: 3.5.7

**Utilities**

- Fastjson2: 2.0.60
- Gson: 2.11.0
- OkHttp: 4.12.0
- Apache POI: 5.2.5
- EasyExcel: 4.1.5
- Knife4j: 4.6.0-SNAPSHOT
- Liquibase: 4.29.0

## Quick Start

### Prerequisites

- JDK 21 or higher
- Maven 3.8 or higher
- MySQL / PostgreSQL / MongoDB (optional)
- Redis (optional)
- Nacos (optional, for Spring Cloud)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/yourusername/carlos-framework.git
cd carlos-framework

# Build all modules
mvn clean install

# Build with specific profile (for internal Nexus)
mvn clean install -P carlos-public    # Public server: zcarlos.com:8081
mvn clean install -P carlos-private   # Private server: 192.168.3.30:8081
```

### Running Tests

```bash
# Run unit tests
mvn test

# Run integration tests
mvn verify

# Run specific test
mvn test -Dtest=ClassName#methodName
```

## Module Structure

```
carlos-framework/                          # Root aggregator
├── carlos-dependencies/                   # Dependency management (BOM)
├── carlos-parent/                         # Parent POM
├── carlos-commons/                        # Framework-agnostic utilities
│   └── carlos-utils/                      # Common utility functions
├── carlos-spring-boot/                    # Spring Boot integration (23 modules = 21 starters + core + redis aggregator)
│   ├── carlos-spring-boot-core/           # Core framework components
│   ├── carlos-spring-boot-redis/          # Redis + Redisson + Caffeine aggregator
│   ├── carlos-spring-boot-starter-mybatis/  # MyBatis-Plus integration
│   ├── carlos-spring-boot-starter-security/   # Spring Security / OAuth2
│   ├── carlos-spring-boot-starter-ai/     # AI / LangChain4j
│   └── ... (18 more starter modules)
├── carlos-integration/                    # Third-party integrations & business modules (10 modules)
│   ├── carlos-agent/                     # AI assistant agent layer (DeepSeek Harness runtime)
│   ├── carlos-audit/                     # Audit logging
│   ├── carlos-auth/                      # OAuth2 authentication
│   ├── carlos-gateway/                   # Spring Cloud Gateway
│   ├── carlos-job/                       # Scheduled job scheduling center
│   ├── carlos-license/                   # Software license management
│   ├── carlos-message/                   # Message center
│   ├── carlos-org/                       # Organization & user center
│   ├── carlos-system/                    # System management
│   └── carlos-tools/                     # GUI code generator
├── carlos-samples/                        # Sample applications (18 modules = 17 carlos-sample-* + carlos-test)
│   ├── carlos-sample-apm/
│   ├── carlos-sample-web/
│   ├── carlos-sample-mybatis/
│   ├── carlos-sample-redis/
│   ├── carlos-sample-encrypt/
│   └── ... (12 more carlos-sample-* + carlos-test)
└── carlos-ui/                             # Frontend Vue3 application
```

### Architecture Layers

The framework follows a clean layered architecture:

```
API Interface Layer → API Implementation → Controller → Service → Manager → Mapper
```

### Data Models

- **Param** - Request parameters (CreateParam, UpdateParam, PageParam)
- **DTO** - Data Transfer Objects (Service layer)
- **VO** - View Objects (Response to frontend)
- **Entity** - Database entities (DO)
- **Enum** - Business enumerations (must implement BaseEnum)
- **Excel** - Import/Export objects

## Configuration

### Application.yml Example

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

Two deployment profiles are provided:

- `carlos-public` - Public Nexus repository (zcarlos.com:8081)
- `carlos-private` - Private Nexus repository (192.168.3.30:8081)

## Usage

### Creating a New Module

1. **Spring Boot Starter:**

```bash
cd carlos-spring-boot/
# Create new module following the naming convention:
# carlos-spring-boot-starter-{function}
```

2. **Microservice Module:**

```bash
cd carlos-integration/
# Create three submodules:
# {service}-api  (Feign interfaces)
# {service}-bus  (Business logic)
# {service}-boot (Boot application)
```

### Using the Code Generator

The GUI code generator is located in `carlos-integration/carlos-tools/`:

```bash
cd carlos-integration/carlos-tools
# Run the application
mvn spring-boot:run
# Or run com.carlos.fx.ToolsApplication.main()
```

Features:

- Database code generation (MySQL, MongoDB, Elasticsearch)
- Project scaffolding
- Encryption tools
- GitLab integration

### Object Mapping with MapStruct

```java
@Mapper(componentModel = "spring")
public interface UserConvert {
    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserDTO toDTO(UserCreateParam param);
    UserVO toVO(UserDTO dto);
    List<UserVO> toVOList(List<UserDTO> dtoList);
}
```

## OAuth2 Authorization Server

Carlos Framework includes a built-in OAuth2 authorization server:

### Configuration

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

### Endpoints

- `POST /oauth2/token` - Get access token
- `POST /oauth2/revoke` - Revoke token
- `GET /oauth2/jwks` - JWK set
- `GET /oauth2/authorize` - Authorization endpoint

### Current User

```java
// Get current user info
UserContext userContext = OAuth2Util.extractUserContext();
Long userId = OAuth2Util.getCurrentUserId();
String userName = OAuth2Util.getCurrentUserName();

// Method-level security
@PreAuthorize("hasRole('ADMIN')")
public void adminOnlyMethod() { }
```

## License Management

The framework includes a software license verification system using TrueLicense:

```bash
# License generation (development only)
carlos-spring-boot-starter-license-generate

# License verification (production)
carlos-spring-boot-starter-license-verify
```

**Important:** Never include the license generation module in production builds.

## Best Practices

### Code Style

- Follow Alibaba Java Coding Guidelines
- Keep functions small (< 50 lines)
- Immutability - Always create new objects, never mutate
- Comprehensive error handling
- Validate all user input

### Security Checklist

- [ ] No hard-coded credentials
- [ ] All inputs validated
- [ ] SQL injection prevention (use MyBatis-Plus `#{ }`)
- [ ] XSS prevention
- [ ] CSRF protection enabled
- [ ] Authentication/authorization verified
- [ ] Rate limiting on all endpoints
- [ ] Error messages don't leak sensitive data

### Testing Requirements

- Minimum 80% code coverage
- Unit tests, integration tests, and e2e tests
- Test-Driven Development (TDD) approach
- Independent and repeatable tests

## Documentation

- [CLAUDE.md](./CLAUDE.md) - Development guide and specifications
- [OPTIMIZATION_REPORT.md](./OPTIMIZATION_REPORT.md) - Optimization history
- [README-REF.md](./README-REF.md) - Quick reference
- Module READMEs - Each module has its own README.md

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](./CONTRIBUTING.md) for details.

### Development Workflow

1. Plan with **planner** agent
2. Test-Driven Development with **tdd-guide** agent
3. Code review with **code-reviewer** agent
4. Security review with **security-reviewer** agent

### Commit Convention

```
type: description

optional body
```

Types: `feat`, `fix`, `refactor`, `docs`, `test`, `chore`, `perf`, `ci`

## Community

- Issues: [GitHub Issues](https://github.com/yourusername/carlos-framework/issues)
- Discussions: [GitHub Discussions](https://github.com/yourusername/carlos-framework/discussions)
- Gitee Mirror: [Gitee Repository](https://gitee.com/yourusername/carlos-framework)

## License

This project is licensed under the MIT License - see the [LICENSE](./LICENSE) file for details.

## Acknowledgments

- Spring Boot and Spring Cloud teams
- Alibaba Spring Cloud team
- MyBatis-Plus team
- Redisson team
- All contributors and supporters

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=yourusername/carlos-framework&type=Date)](https://star-history.com/#yourusername/carlos-framework&Date)

---

**Note:** Carlos Framework is open source under the MIT License. Please review and adapt the configuration before production use.
