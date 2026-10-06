# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Initial open source release
- GitHub and Gitee open source files
- Security policy and vulnerability reporting guidelines

### Changed

- N/A

### Deprecated

- N/A

### Removed

- N/A

### Fixed

- N/A

### Security

- N/A

## [3.0.0] - 2026-06-10

### Added

- Spring Boot 4.0.6 upgrade
- Spring Cloud 2025.1.1 upgrade
- Spring Cloud Alibaba 2025.1.0.0 upgrade
- OAuth2 Authorization Server integration
- Comprehensive code generation tools
- Multi-level caching support (Redis + Redisson + Caffeine)
- 22+ starter modules with AI, translation, migration support
- 8 business modules (auth, gateway, message, system, org, audit)
- 20 sample applications
- Vue3 frontend (carlos-ui)
- License management system (TrueLicense)
- SkyWalking APM integration
- Chinese cryptography standards (SM2/SM4)
- TDD and agent-based development workflow

### Changed

- Migrated from Spring Boot 2.7 to 4.0.6
- Spring Cloud upgraded from 2025.0.1 to 2025.1.1
- Spring Cloud Alibaba upgraded from 2025.0.0.0 to 2025.1.0.0
- MyBatis-Plus upgraded from 3.5.15 to 3.5.16
- MyBatis-Plus Join upgraded from 1.5.4 to 1.5.7
- Seata upgraded from 2.0.0 to 2.5.0
- Redisson upgraded from 3.51.0 to 4.5.0
- Druid upgraded from 1.2.27 to 1.2.28
- Fastjson upgraded from 2.0.60 to 2.0.61
- BouncyCastle upgraded from 1.70 to 1.81
- Updated to Jakarta EE namespace (javax.* to jakarta.*)
- Refactored project structure: carlos-commons reduced to utils only
- carlos-spring-boot-core moved to carlos-spring-boot/
- Redis module restructured into aggregator with core/starter/reactive submodules
- OAuth2 starter renamed from `starter-oauth2` to `starter-security`

### Fixed

- MyBatis-Plus compatibility issues with Spring Boot 4.0
- JSqlParser deprecated API usage
- MySQL connector dependency updates
- Removed ghost dependencies (`carlos-auth-bus`, `carlos-message-client`) from carlos-integration/pom.xml

### Security

- Implemented OAuth2 security framework
- Added comprehensive security guidelines
- Built-in encryption support

## [2.0.0] - Previous Version

### Added

- Initial multi-module framework structure
- Basic Spring Boot starters
- MyBatis-Plus integration
- Redis support

### Changed

- Project architecture redesign

[unreleased]: https://github.com/yourusername/carlos-framework/compare/v3.0.0...HEAD

[3.0.0]: https://github.com/yourusername/carlos-framework/releases/tag/v3.0.0
