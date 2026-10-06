# Carlos Message 文档目录

> **文档类型**: DEV
> **状态**: 已确认
> **版本**: v1.0.0
> **作者**: Carlos Team
> **日期**: 2026-03-11
> **标签**: #message #DEV

---

## 文档清单

| 文件                                                   | 说明                   | 状    |
|------------------------------------------------------|----------------------|------|
| [DATABASE_SCHEMA.md](./DATABASE_SCHEMA.md)           | 数据库设计文档（表结构、索引、示例数据） | 已评审优 |
| [database_init.sql](./database_init.sql)             | 数据库初始化脚本（完整建初始数据     | 已生   |
| [database_upgrade_v2.sql](./database_upgrade_v2.sql) | 数据库升级脚本（V1.0 -> V2.0 | 待生   |

## 快速开

### 1. 初始化数据库

```bash
# 方式1：使用mysql命令
mysql -u root -p < database_init.sql

# 方式2：使用MySQL客户端工具执
# 推荐使用 Navicat / DataGrip / MySQL Workbench 执行 database_init.sql
```

### 2. 验证安装

```sql
USE carlos_message;
SHOW TABLES;
-- 应该看到6张表：message_channel, message_receiver, message_record, message_send_log, message_template, message_type
```

### 3. 查看初始数据

```sql
-- 查看消息类型
SELECT * FROM message_type;

-- 查看表结
DESC message_record;
DESC message_receiver;
```

## 核心表关

```
message_type (消息类型)
    
    └── message_template (消息模板) ──
                                      
message_channel (渠道配置)             
                                     
    └── message_receiver (接收 ◄────
            
            ├── message_record (消息记录)
            
            └── message_send_log (发送日
```

## 版本历史

### V2.0 (2026-03-11)

**优化内容*

1. 修复 `message_record` 表状态字段歧义问题（改为统计字段
2. 统一所有表的审计字段（create_by/update_by/create_time/update_time
3. 完善所有表的索引设计（添加复合索引优化高频查询
4. 优化字段长度和必填属性（提高灵活性）
5. 增加 `message_send_log.receiver_id` 字段（精确关联接收人
6. 统一 `message_type` `message_record` 的关联方式（都使type_code
7. 表名统一`message_*` 前缀（与模块名保持一致）

## 待办事项

- [ ] 生成 database_upgrade_v2.sql（从旧版本升级的脚本
- [ ] 编写数据清理定时任务脚本
- [ ] 编写分库分表示例配置

**维护: AI Assistant**  
**最后更 2026-03-11**
