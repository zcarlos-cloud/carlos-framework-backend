# Carlos Framework MySQL → PostgreSQL 迁移评估报告

> 生成日期：2026-05-28
> 分析范围：carlos-framework 全量代码库
> **迁移状态：已完成（2026-05-28）**

---

## 迁移改造清单（已完成）

| 改造项               | 文件/位置                                                                       | 状态 |
|-------------------|-----------------------------------------------------------------------------|----|
| **BOM 依赖替换**      | `carlos-dependencies/pom.xml` mysql-connector-j → postgresql 42.7.5         | ✅  |
| **Starter 依赖替换**  | `carlos-spring-boot-starter-mybatis/pom.xml` mysql → postgresql             | ✅  |
| **Tools 依赖替换**    | `carlos-tools/pom.xml` mysql → postgresql                                   | ✅  |
| **Sample 依赖替换**   | `carlos-sample-mybatis/pom.xml`, `carlos-sample-flowable/pom.xml`           | ✅  |
| **默认 dbType**     | `MybatisProperties.java` DbType.MYSQL → POSTGRE_SQL                         | ✅  |
| **数据源配置**         | 4 个业务 boot 模块 + deploy 文档 + 3 个 sample                                      | ✅  |
| **环境变量名**         | MYSQL_URL → DB_URL, MYSQL_USERNAME → DB_USERNAME, MYSQL_PWD → DB_PWD        | ✅  |
| **MySQL 专属参数清理**  | rewriteBatchedStatements / zeroDateTimeBehavior / useSSL / serverTimezone 等 | ✅  |
| **重复参数清理**        | org-boot / message-boot 中重复的 URL 参数                                         | ✅  |
| **NOW() 函数**      | OrgRoleMapper.xml 已删除，MP 自动填充替代                                             | ✅  |
| **DbTypeEnum 工具** | `carlos-tools` GUI 代码生成器增加 PostgreSQL 支持                                    | ✅  |

---

## 一、迁移必要性分析

### 1.1 建议迁移的场景（PostgreSQL 优势）

| 维度             | PostgreSQL              | MySQL            | 对 Carlos Framework 的 relevance   |
|----------------|-------------------------|------------------|----------------------------------|
| **复杂查询/联表**    | 优化器更智能，CTE、窗口函数成熟       | 8.0+ 有改善，但复杂场景仍弱 | `mybatis-plus-join` 若启用，PG 性能更稳定 |
| **JSON/JSONB** | 原生 JSONB 索引、操作符丰富       | JSON 功能较基础       | 未来扩展配置存储、动态表单有优势                 |
| **GIS/地理数据**   | PostGIS 业界标准            | 功能较弱             | 若业务涉及位置服务，PG 是必选                 |
| **扩展性**        | 插件生态丰富（全文搜索、向量数据库）      | 相对封闭             | 未来 AI 向量检索（pgvector）可直接集成        |
| **严格性**        | 默认严格，数据一致性更强            | historically 宽松  | 对企业级框架，数据可靠性更高                   |
| **许可证**        | PostgreSQL License（更宽松） | GPL/Commercial   | 商业分发无顾虑                          |

### 1.2 不建议迁移的场景（MySQL 优势）

| 场景          | 说明                          |
|-------------|-----------------------------|
| 团队熟悉度低      | 若团队无 PG 运维经验，学习成本不可忽视       |
| 已有 MySQL 生态 | 若已部署 MySQL 集群、监控、备份体系，迁移成本高 |
| 简单 CRUD 为主  | 当前框架以标准 CRUD 为主，PG 优势无法体现   |
| 阿里云 RDS 成本  | MySQL RDS 在国内云厂商价格通常更低      |

### 1.3 结论

- **若 Carlos Framework 定位是内部脚手架、以标准 CRUD 为主** → 迁移收益有限，必要性不高。
- **若计划支持复杂查询、JSON 存储、GIS、向量检索** → PostgreSQL 是正确选择。

---

## 二、全量代码扫描结果

### 2.1 依赖层面

| 模块                                 | 文件                            | 依赖项               | 版本    | 说明              |
|------------------------------------|-------------------------------|-------------------|-------|-----------------|
| carlos-dependencies                | `carlos-dependencies/pom.xml` | mysql-connector-j | 9.5.0 | 版本管理根节点         |
| carlos-spring-boot-starter-mybatis | `pom.xml`                     | mysql-connector-j | 继承    | 多数据源 starter 包含 |
| carlos-tools                       | `pom.xml`                     | mysql-connector   | 未明确   | GUI 代码生成器使用     |
| carlos-sample-mybatis              | `pom.xml`                     | mysql-connector-j | 继承    | 示例模块            |
| carlos-sample-flowable             | `pom.xml`                     | mysql-connector-j | 继承    | Flowable 示例     |

**同时存在达梦驱动**：`carlos-spring-boot-starter-mybatis` 中包含 `DmJdbcDriver18`。

### 2.2 配置层面（数据源配置）

| 模块                     | 文件                     | 数据库类型 | MySQL 专属参数                                                                                                           |
|------------------------|------------------------|-------|----------------------------------------------------------------------------------------------------------------------|
| carlos-auth-boot       | `application-boot.yml` | MySQL | useUnicode, characterEncoding, autoReconnect, rewriteBatchedStatements, zeroDateTimeBehavior, useSSL, serverTimezone |
| carlos-system-boot     | `application-boot.yml` | MySQL | 同上                                                                                                                   |
| carlos-org-boot        | `application-boot.yml` | MySQL | 同上 + nullCatalogMeansCurrent（重复参数）                                                                                   |
| carlos-message-boot    | `application-boot.yml` | MySQL | 同上 + nullCatalogMeansCurrent（重复参数）                                                                                   |
| carlos-test            | `application.yml`      | MySQL | useUnicode, characterEncoding, userSSL(拼写错误), serverTimezone                                                         |
| carlos-sample-mybatis  | `application.yml`      | MySQL | useUnicode, characterEncoding, useSSL, serverTimezone                                                                |
| carlos-sample-flowable | `application.yml`      | MySQL | useUnicode, characterEncoding, serverTimezone, nullCatalogMeansCurrent                                               |

**需清理的 MySQL 专属参数**：

- `rewriteBatchedStatements=true` — MySQL 批处理优化
- `zeroDateTimeBehavior=convertToNull` — MySQL 零日期处理
- `nullCatalogMeansCurrent=true` — MySQL 目录解析
- `useSSL=true` — MySQL SSL 连接
- `serverTimezone=GMT%2B8` — MySQL 时区设置

### 2.3 MyBatis-Plus 配置

| 文件                       | 配置项                        | 值              | 说明             |
|--------------------------|----------------------------|----------------|----------------|
| `MybatisProperties.java` | dbType                     | `DbType.MYSQL` | 默认数据库类型为 MySQL |
| `MyBatisPlusConfig.java` | PaginationInnerInterceptor | 动态 dbType      | 从配置属性获取        |

### 2.4 代码层面 — MySQL 专属语法

| 文件                  | 位置        | 语法      | 数量  | 修改方式                         |
|---------------------|-----------|---------|-----|------------------------------|
| `OrgRoleMapper.xml` | 第 24/34 行 | `NOW()` | 2 处 | 改为参数传入，或使用 MyBatis-Plus 自动填充 |

**重要发现**：框架中几乎没有使用 MySQL 专属语法：

- 无 `GROUP_CONCAT`、`FIND_IN_SET`
- 无 `ON DUPLICATE KEY UPDATE`
- 无反引号 `` ` `` 引用标识符
- 无 MySQL 风格 `LIMIT` 硬编码（分页由 MyBatis-Plus 拦截器生成）

### 2.5 注解式原生 SQL

| 文件                         | 方法              | SQL 语句                                                       | 兼容性       |
|----------------------------|-----------------|--------------------------------------------------------------|-----------|
| `SecurityAlertMapper.java` | selectByUserId  | `SELECT * FROM security_alert ... ORDER BY create_time DESC` | 语法通用，无需修改 |
| `SecurityAlertMapper.java` | selectUnhandled | `SELECT * FROM security_alert WHERE handled = 0 ...`         | 语法通用，无需修改 |
| `TrustedDeviceMapper.java` | 多个方法            | `SELECT * FROM auth_trusted_device WHERE ...`                | 语法通用，无需修改 |
| `UserMapper.java`          | selectByStatus  | `SELECT * FROM sys_user WHERE status = #{status}`            | 语法通用，无需修改 |

### 2.6 MyBatis-Plus Join 使用

| 指标                          | 数据      |
|-----------------------------|---------|
| 继承 `MPJBaseMapper` 的 Mapper | 23 个    |
| 实际使用 join 查询的代码             | **0 处** |

**结论**：`mybatis-plus-join` 已引入但未实际使用，属于资源浪费，可评估移除。

### 2.7 实体类统计

| 指标                                | 数据           |
|-----------------------------------|--------------|
| 使用 `@TableName` 的实体类              | 41 个         |
| 使用 `@TableField` 的实体类             | 全部           |
| 使用 `FieldFill.INSERT/UPDATE` 的实体类 | 全部（自动填充配置正确） |

---

## 三、迁移难度评估：低

### 3.1 实际需要修改的内容

| 层级               | 改动项                                 | 数量    | 复杂度       |
|------------------|-------------------------------------|-------|-----------|
| **依赖**           | `mysql-connector-j` → `postgresql`  | 1 处   | 低         |
| **配置**           | 6 个 boot 模块 + 示例的 JDBC URL 和驱动类     | ~10 处 | 低（改动模式相同） |
| **MyBatis-Plus** | 默认 `dbType` 改为 `DbType.POSTGRE_SQL` | 1 处   | 低         |
| **代码**           | 替换 2 处 `NOW()`                      | 2 处   | 低         |
| **原生 SQL**       | 验证 9 处注解式 SQL 的兼容性                  | 9 处   | 无需修改      |

### 3.2 字段类型映射

| MySQL 类型   | PostgreSQL 类型          | 说明                                      |
|------------|------------------------|-----------------------------------------|
| `TINYINT`  | `SMALLINT` 或 `BOOLEAN` | MyBatis-Plus `DbType.POSTGRE_SQL` 已自动处理 |
| `DATETIME` | `TIMESTAMP`            | MyBatis-Plus 自动处理                       |
| `VARCHAR`  | `VARCHAR`              | 一致                                      |
| `TEXT`     | `TEXT`                 | 一致                                      |
| `BIGINT`   | `BIGINT`               | 一致                                      |
| `JSON`     | `JSONB`                | 建议新表使用 JSONB                            |

### 3.3 MyBatis-Plus 分页兼容性

MyBatis-Plus 对 PostgreSQL 支持完善，`PaginationInnerInterceptor` 会根据 `dbType` 自动生成 `LIMIT/OFFSET` 分页语法，无需额外处理。

---

## 四、迁移工作量估算

| 阶段            | 内容                                      | 工作量           |
|---------------|-----------------------------------------|---------------|
| **准备**        | 添加 PG 驱动依赖、修改 starter-mybatis 默认 dbType | 0.5 天         |
| **配置**        | 修改 6 个 boot 模块 + 示例的 application.yml    | 0.5 天         |
| **代码修复**      | 替换 2 处 `NOW()`，验证原生 SQL                 | 0.5 天         |
| **Schema 迁移** | 建表语句转换、索引迁移                             | 1-2 天（取决于表数量） |
| **测试**        | 全量接口回归测试、分页验证、性能基线                      | 2-3 天         |
| **文档**        | 更新部署文档、运维手册                             | 0.5 天         |

**总计：约 5-7 人天（不含数据迁移和团队学习成本）**

---

## 五、迁移建议

### 5.1 若决定迁移

1. **选择试点模块**：先以 `carlos-system` 做试点，验证通过后推广。
2. **分阶段实施**：
    - 第一阶段：框架层改造（依赖 + starter-mybatis 配置）
    - 第二阶段：各业务模块配置切换
    - 第三阶段：Schema 迁移 + 全量回归测试
3. **数据迁移方案**：
    - 离线迁移：`pgloader` / 自研脚本
    - 在线迁移：双写 + 增量同步（复杂度高）

### 5.2 若暂时不迁移

可先做好兼容准备，为未来切换留余地：

1. 将 `dbType` 改为配置项，避免硬编码 `DbType.MYSQL`
2. 移除 XML 中的 `NOW()`，统一使用 MyBatis-Plus 自动填充
3. 避免新增 MySQL 专属语法（如 `GROUP_CONCAT`、`ON DUPLICATE KEY UPDATE`）
4. 评估移除未使用的 `mybatis-plus-join` 依赖

---

## 六、风险清单

| 风险                          | 等级 | 说明                    |
|-----------------------------|----|-----------------------|
| `mybatis-plus-join` 未使用但已引入 | 低  | 移除或保留均不影响 PG 迁移       |
| `carlos-tools` 可能使用旧版驱动     | 低  | 统一更新为 `postgresql` 驱动 |
| 团队 PostgreSQL 运维经验          | 中  | 需评估团队学习成本             |
| 现有 MySQL 数据迁移               | 中  | 取决于数据量和停机窗口要求         |
| 达梦数据库双驱动共存                  | 低  | PG 与达梦驱动可共存，无冲突       |

---

## 附录：扫描到的 MySQL 专属语法详细位置

### A.1 XML 中的 NOW()

```
carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgRoleMapper.xml:24
  → UPDATE ... SET update_time = NOW()

carlos-integration/carlos-org/carlos-org-bus/src/main/resources/mapper/org/OrgRoleMapper.xml:34
  → INSERT ... VALUES (..., NOW(), NOW())
```

### A.2 所有数据源配置文件

```
carlos-integration/carlos-auth/carlos-auth-boot/src/main/resources/application-boot.yml
carlos-integration/carlos-system/carlos-system-boot/src/main/resources/application-boot.yml
carlos-integration/carlos-org/carlos-org-boot/src/main/resources/application-boot.yml
carlos-integration/carlos-message/carlos-message-boot/src/main/resources/application-boot.yml
carlos-samples/carlos-test/src/main/resources/application.yml
carlos-samples/carlos-sample-mybatis/src/main/resources/application.yml
carlos-samples/carlos-sample-flowable/src/main/resources/application.yml
```

### A.3 注解式原生 SQL 位置

```
carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/audit/mapper/SecurityAlertMapper.java
carlos-integration/carlos-auth/carlos-auth-service/src/main/java/com/carlos/auth/mfa/mapper/TrustedDeviceMapper.java
carlos-samples/carlos-sample-mybatis/src/main/java/com/carlos/sample/mybatis/mapper/UserMapper.java
```
