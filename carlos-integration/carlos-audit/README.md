# Carlos Audit 通用审计模块

基于 **Disruptor + ClickHouse** 的高性能分布式审计日志模块，支持日写入千万级审计数据。

## 核心特性

- **高性能写入**: Disruptor 无锁队列，单机支持 10万+ TPS
- **海量存储**: ClickHouse 列式存储，支持 PB 级数据
- **实时分析**: 物化视图预聚合，毫秒级统计查询
- **混合存储**: MyBatis-Plus(PostgreSQL配置) + ClickHouse Client(日志存储)
- **声明式使用**: 基于 `@AuditLog` 注解的 AOP 拦截

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos.audit</groupId>
    <artifactId>carlos-audit-bus</artifactId>
</dependency>
```

### 2. 配置 ClickHouse

```yaml
carlos:
  audit:
    enabled: true
    clickhouse:
      host: localhost
      port: 8123
      database: default
      username: default
      password: ""
```

### 3. 执行建表脚本

```bash
# 执行 sql/audit_ck.txt 中的 ClickHouse 建表语句
```

### 4. 使用 @AuditLog 注解

```java
@RestController
public class OrderController {
    
    @AuditLog(
        type = AuditLogTypeEnum.ORDER_PAY,
        category = AuditLogCategoryEnum.BUSINESS,
        operation = "'订单支付: ' + #param.orderNo",
        targetId = "#param.orderNo",
        targetType = AuditLogTargetTypeEnum.ORDER,
        riskLevel = 70
    )
    @PostMapping("/order/pay")
    public OrderVO pay(@RequestBody OrderPayParam param) {
        return orderService.pay(param);
    }
}
```

## 架构说明

```
@AuditLog 注解 -> AOP 拦截 -> Disruptor RingBuffer -> EventHandler 
                                                              |
                                                              v
ClickHouse BatchWriter -> 双缓冲 -> 批量写入 ClickHouse
```

## 配置项

| 配置项                                                    | 默认值       | 说明                           |
|---------------------------------------------------------|-------------|------------------------------|
| carlos.audit.enabled                                    | true        | 是否启用审计日志                    |
| carlos.audit.disruptor.buffer-size                      | 1048576     | RingBuffer 大小                |
| carlos.audit.disruptor.consumer-count                   | 4           | 消费者线程数                      |
| carlos.audit.batch-writer.batch-size                    | 500         | 批量写入大小                      |
| carlos.audit.batch-writer.flush-interval                | 1000        | 刷新间隔(ms)                    |
| carlos.audit.batch-writer.max-buffer-size               | 10000       | 最大缓冲区大小                    |
| carlos.audit.batch-writer.back-pressure-strategy        | DROP_OLDEST | 背压策略                         |
| carlos.audit.batch-writer.max-concurrent-flushes        | 4           | 最大并发刷新任务数                 |
| carlos.audit.batch-writer.local-backup-on-failure       | true        | 失败时是否写入本地文件备份          |
| carlos.audit.batch-writer.recovery-on-startup           | false       | 启动时是否自动恢复备份文件          |
| carlos.audit.batch-writer.adaptive-batch-size           | false       | 是否启用自适应批量大小              |
| carlos.audit.clickhouse.max-execution-time-seconds      | 10          | 查询最大执行时间（秒）              |
| carlos.audit.clickhouse.max-rows-to-read                | 10000000    | 单次查询最大读取行数                |
| carlos.audit.archive.enabled                            | true        | 是否启用归档                      |
| carlos.audit.archive.cold-days                          | 7           | 冷数据天数（超过此天数将被归档）     |
| carlos.audit.archive.cron                               | 0 0 2 * * ? | 归档定时任务 Cron 表达式           |
| carlos.audit.archive.storage-type                       | OSS         | 归档存储类型                     |

## API 接口

### 审计日志查询

- `POST /audit/log/query/logs/page` - 分页查询审计日志
- `GET /audit/log/query/logs/{id}` - 查询审计日志详情
- `GET /audit/log/query/stats/realtime` - 实时统计
- `GET /audit/log/query/trail/{principalId}` - 用户行为轨迹
- `GET /audit/log/query/risks` - 风险事件查询

### 审计日志管理

- `POST /audit/log/main/add` - 新增审计日志
- `POST /audit/log/main/delete` - 删除审计日志
- `POST /audit/log/main/update` - 更新审计日志
- `GET /audit/log/main/detail` - 审计日志详情
- `GET /audit/log/main/page` - 审计日志分页列表

### 审计日志配置

- `POST /audit/log/config/add` - 新增审计配置
- `POST /audit/log/config/delete` - 删除审计配置
- `POST /audit/log/config/update` - 更新审计配置
- `GET /audit/log/config/detail` - 审计配置详情
- `GET /audit/log/config/page` - 审计配置分页列表

### 审计日志归档记录

- `POST /audit/log/archive/record/add` - 新增归档记录
- `POST /audit/log/archive/record/delete` - 删除归档记录
- `POST /audit/log/archive/record/update` - 更新归档记录
- `GET /audit/log/archive/record/detail` - 归档记录详情
- `GET /audit/log/archive/record/page` - 归档记录分页列表
- `POST /audit/log/archive/record/restore` - 恢复归档数据至 ClickHouse

### 使用示例

- `POST /api/audit/demo/login` - 登录审计示例
- `POST /api/audit/demo/order/pay` - 订单支付审计示例
- `GET /api/audit/demo/export` - 数据导出审计示例
- `POST /api/audit/demo/config/update` - 配置变更审计示例
- `POST /api/audit/demo/file/upload` - 文件上传审计示例

## 文档

- [文档中心](./docs/README.md)

## License

Copyright (c) Carlos Team
