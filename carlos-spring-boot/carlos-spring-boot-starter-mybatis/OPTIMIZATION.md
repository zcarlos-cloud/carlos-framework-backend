# MyBatis 模块改造优化文档

## 改造概述

本次改造对 `carlos-spring-boot-starter-mybatis` 模块进行了全面优化，提升了代码质量、扩展性和性能。

## 改造内容清单

### 1. 新增配置属性类（MybatisProperties）

**文件**: `config/MybatisProperties.java`

**功能**:

- 支持通过 `application.yml` 配置 Mybatis 相关参数
- 配置项包括：
    - 分页配置（最大页大小、默认页大小、是否优化大页码）
    - 字段填充配置（创建时间、更新时间、创建人、更新人等字段名）
    - 拦截器配置（分页、乐观锁、防全表删除、性能分析开关）
    - 批量操作配置（批次大小、超时时间、异步阈值）

**配置示例**:

```yaml
carlos:
  mybatis:
    enabled: true
    db-type: mysql
    pagination:
      max-size: 500
      default-size: 10
    fill:
      create-time-field: createTime
      update-time-field: updateTime
    interceptor:
      pagination: true
      optimistic-locker: true
      block-attack: true
      performance: false
      slow-sql-threshold: 1000
    batch:
      batch-size: 500
      optimized: true
```

### 2. 完善 SerializableTypeHandler

**文件**: `typehandler/SerializableTypeHandler.java`

**优化内容**:

- 完成原本为空的方法实现
- 支持多种类型的自动转换：
    - 数值类型：Long、Integer、Short、BigInteger、BigDecimal
    - 字符串类型：String
    - 时间戳类型：Long 类型的时间戳
- 添加详细的日志记录
- 增强异常处理

### 3. 重构 MyBatisPlusConfig

**文件**: `config/MyBatisPlusConfig.java`

**优化内容**:

- 支持配置驱动（使用 MybatisProperties）
- 拦截器根据配置动态启用/禁用
- 分页插件支持动态设置最大限制
- 性能监控拦截器可通过配置启用
- 代码结构更清晰，易于维护

### 4. 优化 DefaultMetaObjectHandler

**文件**: `config/DefaultMetaObjectHandler.java`

**优化内容**:

- 使用策略模式替代冗长的 `while(true)` 代码块
- 提取时间填充策略到 `TIME_FILLER_MAP`
- 提取用户ID转换策略到 `USER_ID_FILLER_MAP`
- 支持配置驱动的字段填充开关
- 添加详细的调试日志
- 提供扩展点：支持注册自定义填充策略

### 5. 新增批量操作服务

**文件**:

- `base/BatchService.java`（接口）
- `base/BatchServiceImpl.java`（实现）

**功能特性**:

- 批量插入优化（`saveBatchOptimized`）
- 分批插入（`saveBatchPartitioned`）
- 批量更新非空字段（`updateBatchByIdSelective`）
- 批量 Upsert（`upsertBatch`）
- 异步批量操作（`asyncSaveBatch`）
- 大数据量分批处理（`processInPartitions`）
- 支持配置驱动的批次大小

### 6. 新增性能监控拦截器

**文件**: `interceptor/PerformanceInterceptor.java`

**功能**:

- 基于 Mybatis Plus 的 InnerInterceptor
- 可配置慢 SQL 阈值
- 支持打印所有 SQL（调试用）
- 预留监控指标上报扩展点

### 7. 优化 PageInfo 分页

**文件**:

- `pagination/PageInfo.java`
- `pagination/CursorPageInfo.java`（新增）

**优化内容**:

- 支持大数据量分页优化（大页码阈值检测）
- 添加游标分页支持（`CursorPageInfo`）
- 支持分页大小限制（防止大数据量查询）
- 便捷的 VO 转换方法（`toVoPage`）
- 静态工厂方法（`of`、`empty`）
- 排序字段自动驼峰转下划线

### 8. 优化 PropertyColumnUtil

**文件**: `utils/PropertyColumnUtil.java`

**优化内容**:

- 使用 `ConcurrentHashMap` + 过期机制替代简单的 Map
- 支持 LRU 淘汰策略（最大 1000 个类）
- 自动过期（30分钟）
- 定时清理线程（每10分钟）
- 提供缓存统计和刷新接口
- 线程安全

## 文件变更列表

### 新增文件

1. `config/MybatisProperties.java`
2. `base/BatchService.java`
3. `base/BatchServiceImpl.java`
4. `interceptor/PerformanceInterceptor.java`
5. `pagination/CursorPageInfo.java`

### 修改文件

1. `config/MyBatisPlusConfig.java` - 支持配置驱动
2. `config/DefaultMetaObjectHandler.java` - 优化代码逻辑
3. `typehandler/SerializableTypeHandler.java` - 完善空实现
4. `pagination/PageInfo.java` - 增强分页功能
5. `pagination/MybatisPage.java` - 优化工具方法
6. `utils/PropertyColumnUtil.java` - 缓存优化
7. `base/BaseService.java` - 继承 BatchService
8. `base/BaseServiceImpl.java` - 继承 BatchServiceImpl

## 兼容性说明

- 所有改造保持向后兼容
- 原有代码无需修改即可继续使用
- 新功能通过配置启用，默认关闭
- BaseService 和 BaseServiceImpl 增加 BatchService 能力

## 性能提升

1. **批量操作**：支持大批量数据分批处理，防止内存溢出
2. **缓存优化**：PropertyColumnUtil 缓存有自动过期，避免内存泄漏
3. **分页优化**：大数据量分页支持流式查询，避免深分页性能问题
4. **配置灵活**：按需启用功能，减少不必要的拦截器开销

## 使用建议

1. 生产环境建议启用 `block-attack` 防全表删除
2. 大数据量场景使用 `BatchService` 的批量操作方法
3. 分页查询建议设置合理的 `max-size` 限制
4. 开发环境可启用 `performance` 监控慢 SQL
