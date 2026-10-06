# Carlos Migration - 数据库版本迁移管理

基于 Liquibase 的数据库版本控制与变更管理组件，为 Carlos Framework 提供企业级的数据库迁移解决方案。

## 特性

- ✅ **多数据源支持** - 支持主数据源和多个从数据源的独立迁移管理
- ✅ **异步迁移** - 支持多数据源并行执行，加速启动过程
- ✅ **环境隔离** - 通过 contexts 区分开发/测试/生产环境的变更
- ✅ **回滚支持** - 原生支持按标签或数量回滚变更
- ✅ **状态监控** - 提供迁移状态查询和报告功能
- ✅ **Spring Boot 3.x 原生支持** - 无缝集成 Spring Boot 生态

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-migration</artifactId>
    <version>3.0.0-SNAPSHOT</version>
</dependency>
```

### 2. 基础配置

```yaml
# application.yml
carlos:
  migration:
    enabled: true
    primary:
      change-log: db/changelog/db.changelog-master.yaml
      contexts: develop  # 或 test/production
```

### 3. 编写变更日志

在 `src/main/resources/db/changelog/` 下创建变更日志：

```yaml
# db.changelog-master.yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-user-table.yaml
```

```yaml
# changes/001-create-user-table.yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: carlos
      changes:
        - createTable:
            tableName: sys_user
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
      rollback:
        - dropTable:
            tableName: sys_user
```

## 配置详解

### 完整配置示例

```yaml
carlos:
  migration:
    # 总开关
    enabled: true
    
    # 异步迁移（多数据源推荐开启）
    async: false
    async-thread-pool-size: 2
    
    # 管理端点（需引入 spring-web）
    endpoint-enabled: true
    endpoint-path: /migration
    
    # 主数据源配置
    primary:
      enabled: true
      change-log: db/changelog/db.changelog-master.yaml
      contexts: "develop"
      labels: ""
      default-schema: ""
      drop-first: false  # ⚠️ 危险操作，仅开发环境使用
      validate-on-migrate: true
      run-on-startup: true
      database-change-log-table: DATABASECHANGELOG
      database-change-log-lock-table: DATABASECHANGELOGLOCK
      parameters:
        schema.name: public
        table.prefix: t_
    
    # 多数据源配置
    multi:
      slave:
        enabled: true
        change-log: db/changelog/slave/db.changelog-master.yaml
        contexts: "develop"
        run-on-startup: true
```

## 变更日志规范

### 文件命名规范

```
db/changelog/
├── db.changelog-master.yaml      # 主文件，include 所有变更
└── changes/
    ├── 001-init-schema.yaml      # 初始化
    ├── 002-create-tables.yaml    # 建表
    ├── 003-add-indexes.yaml      # 索引
    └── 004-seed-data.yaml        # 数据
```

### ChangeSet 规范

```yaml
databaseChangeLog:
  - changeSet:
      id: 1                    # 唯一标识，递增
      author: carlos           # 开发者标识
      context: develop,test    # 适用环境
      comment: 创建用户表       # 变更说明
      changes:
        # 变更内容
      rollback:
        # 回滚内容
```

## 常用变更类型

### 创建表

```yaml
- createTable:
    tableName: user
    remarks: 用户表
    columns:
      - column:
          name: id
          type: BIGINT
          autoIncrement: true
          constraints:
            primaryKey: true
            nullable: false
      - column:
          name: name
          type: VARCHAR(64)
          constraints:
            nullable: false
```

### 添加列

```yaml
- addColumn:
    tableName: user
    columns:
      - column:
          name: email
          type: VARCHAR(128)
```

### 创建索引

```yaml
- createIndex:
    tableName: user
    indexName: idx_user_name
    columns:
      - column:
          name: name
```

### 执行 SQL

```yaml
- sql:
    sql: INSERT INTO config VALUES (1, 'key', 'value')
    rollbackSql: DELETE FROM config WHERE id = 1
```

### 引用 SQL 文件

```yaml
- sqlFile:
    path: db/scripts/seed-data.sql
    encoding: UTF-8
```

## 程序化 API

### 使用 MigrationService

```java
@Autowired
private MigrationService migrationService;

// 获取迁移状态
Map<String, LiquibaseStatus> status = migrationService.getAllStatus();

// 回滚到指定标签
migrationService.rollback("primary", "v1.0.0");

// 回滚指定数量的变更集
migrationService.rollbackCount("primary", 3);

// 验证变更
migrationService.validate("primary");
```

## 多环境配置

### 开发环境

```yaml
# application-develop.yml
carlos:
  migration:
    primary:
      contexts: develop
      drop-first: true  # 开发环境可清空重建
```

### 测试环境

```yaml
# application-test.yml
carlos:
  migration:
    primary:
      contexts: test
      drop-first: false
```

### 生产环境

```yaml
# application-production.yml
carlos:
  migration:
    primary:
      contexts: production
      drop-first: false      # ⚠️ 严禁开启
      validate-on-migrate: true
```

## 最佳实践

1. **变更集不可修改** - 已执行的变更集严禁修改，否则会导致 checksum 验证失败

2. **每个变更集包含回滚** - 始终提供 rollback 操作，便于紧急回退

3. **向前兼容** - 优先设计向前兼容的变更（如添加 nullable 列），避免回滚

4. **小步快跑** - 将大变更拆分为多个小变更集，降低回滚风险

5. **Code Review** - 所有迁移脚本必须经过 Code Review 后才能合并

6. **版本控制** - 所有变更日志纳入 Git 版本控制

## 问题排查

### Checksum 验证失败

原因：修改了已执行的变更集
解决：

- 开发环境：执行 `liquibase clearCheckSums`
- 生产环境：严禁修改，应新建变更集进行修正

### 锁定超时

原因：上次迁移异常中断，锁未释放
解决：手动清理 `DATABASECHANGELOGLOCK` 表

### 变更执行失败

查看日志获取详细错误，常见原因：

- SQL 语法错误
- 表/列已存在
- 外键约束冲突

## 参考资料

- [Liquibase 官方文档](https://docs.liquibase.com/)
- [Liquibase Change Types](https://docs.liquibase.com/change-types/home.html)
