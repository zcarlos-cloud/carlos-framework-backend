# Carlos Framework 数据存储架构

> 本文档定义 Carlos Framework 的数据库选型与用途，供 AI 生成 SQL、Schema 设计及运维时参考。

---

## 数据库选型

| 数据库            | 用途          | 说明                                       |
|----------------|-------------|------------------------------------------|
| **PostgreSQL** | 主业务数据库      | 关系型数据库，存储系统管理、组织用户、认证授权、消息中心、审计配置等全部业务数据 |
| **ClickHouse** | 审计日志存储      | 列式存储，用于高性能写入与查询海量审计日志                    |
| **Redis**      | 缓存 / 会话 / 锁 | 分布式缓存、Token 会话、限流、分布式锁等                  |
| **MongoDB**    | 可选文档存储      | 部分 starter 提供支持，当前业务模块未作为默认存储            |

---

## PostgreSQL（业务数据库）

- **默认版本**: PostgreSQL 14+
- **字符集**: UTF8
- **驱动**: `org.postgresql.Driver`（由 `carlos-spring-boot-starter-mybatis` 统一管理）
- **ORM**: MyBatis-Plus 3.5.16，默认 `DbType.POSTGRE_SQL`
- **连接池**: Druid 1.2.28
- **建表脚本**: 各模块位于 `carlos-integration/{module}/docs/02-design/database/*_postgresql.sql`
- **数据库初始化脚本**: [`docs/db/00-init-database.sql`](./00-init-database.sql)

### 覆盖模块

| 模块         | 脚本文件                                                                               | 表数量 |
|------------|------------------------------------------------------------------------------------|-----|
| 系统管理       | `carlos-integration/carlos-system/docs/02-design/database/system_postgresql.sql`   | 11  |
| 组织/用户/岗位   | `carlos-integration/carlos-org/docs/02-design/database/org_postgresql.sql`         | 17  |
| 认证授权       | `carlos-integration/carlos-auth/docs/02-design/database/auth_postgresql.sql`       | 13  |
| 审计（配置/元数据） | `carlos-integration/carlos-audit/docs/02-design/database/audit_postgresql.sql`     | 3   |
| 消息中心       | `carlos-integration/carlos-message/docs/02-design/database/message_postgresql.sql` | 6   |
| 定时任务       | `carlos-integration/carlos-job/docs/sql/*.sql`                                     | 3   |
| 工作流审批      | `carlos-integration/carlos-workflow/docs/sql/workflow_postgresql.sql` + `workflow_form_postgresql.sql` | 8 |

> 注：旧 MySQL 建表脚本与根目录 `docs/db/postgresql/` 汇总副本已删除，各模块以 `*_postgresql.sql` 为唯一规范来源。
> `carlos-job` 与 `carlos-workflow` 这两个新建模块沿用 `docs/sql/` 目录（其余模块为 `docs/02-design/database/`），
> 两种位置都只表示「脚本在源码仓库、手工执行」，不参与构建。

**生成 SQL 时请注意：**

1. 所有业务表均位于 PostgreSQL，避免使用 MySQL 专属语法（如 `` ` `` 反引号、`NOW()`、`AUTO_INCREMENT`、`ON UPDATE CURRENT_TIMESTAMP`、`GROUP_CONCAT`、`FIND_IN_SET`、`ON DUPLICATE KEY UPDATE` 等）。
2. 主键使用 `BIGINT`，由应用层雪花 ID（`IdType.ASSIGN_ID`）赋值，**不依赖数据库自增**。
3. 布尔/状态字段使用 `SMALLINT`（0/1），对应代码中的 `Integer`/`Boolean` 类型。
4. 日期时间使用 `TIMESTAMP` 或 `TIMESTAMPTZ`，由 MyBatis-Plus 自动填充，不使用 `ON UPDATE CURRENT_TIMESTAMP`。
5. JSON 类型优先使用 `JSONB`。
6. 分页由 MyBatis-Plus 根据 `DbType.POSTGRE_SQL` 自动生成 `LIMIT/OFFSET`。

### 类型映射速查

| Java / MyBatis-Plus    | PostgreSQL   | 备注        |
|------------------------|--------------|-----------|
| `Long` / `BIGINT`      | `BIGINT`     | 主键、雪花 ID  |
| `Integer` / `SMALLINT` | `SMALLINT`   | 状态、布尔、小整数 |
| `String` / `VARCHAR`   | `VARCHAR(n)` | 字符串       |
| `String` / `TEXT`      | `TEXT`       | 长文本       |
| `LocalDateTime`        | `TIMESTAMP`  | 日期时间      |
| `BigDecimal`           | `DECIMAL`    | 金额、小数     |
| `String` / `JSON`      | `JSONB`      | JSON 数据   |

---

## ClickHouse（审计日志存储）

- **用途**: 存储 `carlos-audit` 模块产生的高并发审计日志
- **架构**: Disruptor 异步队列 + ClickHouse 批量写入
- **建表脚本**: `carlos-integration/carlos-audit/sql/audit_ck.txt`
- **详细文档**: [`carlos-integration/carlos-audit/README.md`](../carlos-integration/carlos-audit/README.md)

**生成 ClickHouse SQL 时请注意：**

1. 使用 ClickHouse 引擎（如 `MergeTree` 系列），避免 InnoDB/MyISAM 等 MySQL 引擎语法。
2. 主键/排序键设计需符合 `ORDER BY` 和 `PARTITION BY` 规范。
3. 批量插入优先使用 `INSERT INTO ... VALUES` 或 JDBC batch。
4. 聚合查询善用物化视图（Materialized View）预聚合。

---

## 环境变量约定

业务模块的数据源配置统一使用以下环境变量（不再使用 MYSQL_* 前缀）：

| 变量名           | 说明       | 示例                                        |
|---------------|----------|-------------------------------------------|
| `DB_URL`      | JDBC URL | `jdbc:postgresql://localhost:5432/carlos` |
| `DB_USERNAME` | 用户名      | `postgres`                                |
| `DB_PWD`      | 密码       | `password`                                |

---

## 相关文档

- [`docs/db/00-init-database.sql`](./00-init-database.sql) — PostgreSQL 数据库初始化脚本
- [`docs/ARCH-MySQL-to-PostgreSQL-迁移评估报告.md`](../ARCH-MySQL-to-PostgreSQL-迁移评估报告.md) — MySQL → PostgreSQL 迁移记录
- [`carlos-integration/carlos-audit/README.md`](../carlos-integration/carlos-audit/README.md) — 审计模块与 ClickHouse 配置
