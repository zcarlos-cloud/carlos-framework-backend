# Carlos Framework 全面分析报告

> 生成日期: 2026-07-19 | 分析范围: 59 个 Maven 模块, 2365 个 Java 文件, 303 个 Vue/TS 文件

---

## 执行摘要

Carlos Framework (v3.0.0-SNAPSHOT) 是一个基于 Spring Boot 4.0.6 + JDK 21 的内部微服务框架。总体架构设计合理，分层清晰，依赖方向正确。但存在三个需要立即关注的领域：

1. **安全**: 硬编码凭据和密钥分布在整个代码库中 (3 个 CRITICAL)
2. **测试**: 测试几乎为零 — 36 个测试文件对应 2365 个源文件 (1.55%)，且全局跳过
3. **代码质量**: Service 层 Redis 操作违规、不可变性破坏、超大文件

综合健康评分: **6.2/10**

---

## 一、架构分析

| 维度            | 状态     | 详情                                                                                                                  |
|---------------|--------|---------------------------------------------------------------------------------------------------------------------|
| 依赖方向          | ✅ 清洁   | 无循环，carlos-dependencies → carlos-parent → carlos-commons → carlos-spring-boot → carlos-integration → carlos-samples |
| 跨模块耦合         | ✅ 规范   | 全部使用 Feign API 接口 (`ApiXxx`)，无直接导入 Manager/Mapper 内部类                                                               |
| Controller 分层 | 🔴 违规  | 大部分 Controller 直接注入并调用 Manager，跳过了文档定义的 Service 层                                                                   |
| 模块命名          | 🟡 不一致 | `carlos-auth-service` 非标准命名（应为 `carlos-auth-bus`）                                                                   |
| Starter 配置    | 🟢 良好  | 21/22 个 starter 有 AutoConfiguration.imports                                                                         |

### 主要建议

- 强制 Controller → Service → Manager → Mapper 分层或更新文档反映实际模式
- 重命名 `carlos-auth-service` → `carlos-auth-bus`

---

## 二、安全审查

| 严重程度        | 数量 | 最关键的发现                                |
|-------------|----|---------------------------------------|
| 🔴 CRITICAL | 3  | SM4 密钥/IV 硬编码、部署文档中硬编码凭据              |
| 🟠 HIGH     | 4  | 运行时默认密码、MinIO 默认凭据、OAuth2 弱密钥、SSRF 漏洞 |
| 🟡 MEDIUM   | 5  | 错误信息泄露、API 文档公开、控制器缺少认证、弱默认密码         |
| 🟢 POSITIVE | 7  | SQL 注入防护到位、CSRF 正确配置、路径遍历防护、XSS 预防    |

### 紧急行动

1. 移除所有 YAML 文件中的硬编码 SM4 密钥默认值（6+ 文件）
2. 清理 `docs/06-deploy/config/` 中的硬编码密码（数据库、Redis、MinIO）
3. 为 `AbstractOssTemplate.putObjectFromUrl()` 添加 SSRF 防护
4. 为 `OssController` 和 `FileController` 添加 `@PreAuthorize`

---

## 三、代码质量

| 严重程度        | 数量 | 关键问题                                                                   |
|-------------|----|------------------------------------------------------------------------|
| 🔴 CRITICAL | 3  | Service 层直接操作 Redis（MfaService, UserLoginService）、RuntimeException 使用  |
| 🟠 HIGH     | 5  | 超大文件（UserLoginService 929行, OrgUserService 858行）、Entity Mutation、命名不一致 |
| 🟡 MEDIUM   | 5  | @Data + 手动 getter/setter 冲突、软删除未用 @TableLogic、Builder 模式缺失             |

### 通过项

- ✅ 全部使用 Lombok（无手写 getter/setter）
- ✅ 全部使用 `@ConfigurationProperties`（无 @Value）
- ✅ 无 `System.out.print`（全部使用 @Slf4j）
- ✅ 无 `Optional.get()` 不安全调用
- ✅ 枚举实现 `BaseEnum` + `@AppEnum`
- ✅ SQL 参数全部使用 `#{}`（无 `${}` 字符串替换）

---

## 四、数据库设计

| 严重程度        | 数量 | 关键发现                                                                             |
|-------------|----|----------------------------------------------------------------------------------|
| 🔴 CRITICAL | 2  | `is_deleted` 类型不匹配（SQL SMALLINT vs Entity Boolean）、auth 模块 CHAR(32) vs 标准 BIGINT |
| 🟠 HIGH     | 4  | sys_service_register 用 VARCHAR ID、TIMESTAMPTZ 缺失、外键索引缺失、唯一约束缺少 tenant_id         |
| 🟡 MEDIUM   | 3  | 审计列不一致、Boolean 索引低效、`dept_code` 唯一约束缺少租户范围                                       |
| 🟢 LOW      | 4  | 排序字段命名不一致、冗余索引、缺少 login_time 索引                                                  |

---

## 五、测试覆盖

| 指标        | 数值                                                                                  |
|-----------|-------------------------------------------------------------------------------------|
| 测试文件总数    | 36                                                                                  |
| 源文件总数     | 2365                                                                                |
| 测试/源文件比   | **1.55%**                                                                           |
| 全局测试跳过    | **是** (carlos-parent/pom.xml `skipTests=true`)                                      |
| JaCoCo 配置 | **无**                                                                               |
| 零测试模块     | carlos-system (283文件), carlos-gateway (98文件), carlos-license (22文件), 21/22 starters |

### 测试质量最高模块

- `carlos-audit` (15 个测试文件) — 唯一有有意义覆盖率的模块
- `carlos-org` (4 个测试文件) — `OrgRoleServiceTest`、`OrgUserServiceTest` 质量优秀 (BCDE 覆盖)
- `carlos-auth` (6 个测试文件) — 包含真正的 `@SpringBootTest` 集成测试

### 快速修复

1. 从 `carlos-parent/pom.xml` 移除 `skipTests=true`
2. 添加 JaCoCo 插件（最低 80% 分支覆盖）
3. 立即为 `carlos-utils` (9 文件) 编写测试（低垂果实 → 目标 90%+）
4. 修复 `SM4UtilTest`：将 `System.out.println` 替换为断言

---

## 六、改善路线图

### 第 1 周（安全 + 测试基础）

- [ ] 移除所有硬编码凭据/密钥（安全 #1-#3）
- [ ] 移除 `skipTests=true`，添加 JaCoCo
- [ ] 修复 carlos-utils 测试覆盖

### 第 2-4 周（架构 + 代码质量）

- [ ] 解决 Controller-Manager 分层违规
- [ ] 重命名 `carlos-auth-service` → `carlos-auth-bus`
- [ ] 将 MFA/Login Service Redis 操作下沉到 Manager 层
- [ ] 拆分 `UserLoginService` (929行) 和 `OrgUserService` (858行)

### 第 2 个月（数据库 + 安全硬化）

- [ ] 统一 `is_deleted` 列类型为 BOOLEAN
- [ ] 全局替换 TIMESTAMP → TIMESTAMPTZ
- [ ] 添加缺失的外键索引
- [ ] 为 OssController/FileController 添加认证授权
- [ ] 添加 SSRF 防护

### 第 3 个月（测试 + 文档）

- [ ] carlos-system 达到 70% 测试覆盖率
- [ ] carlos-gateway 达到 70% 测试覆盖率
- [ ] carlos-spring-boot-core 达到 80% 测试覆盖率
- [ ] 所有 22 个 starter 模块添加冒烟测试
