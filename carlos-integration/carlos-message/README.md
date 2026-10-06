# carlos-message v3.0.0

<h4 align="center">Spring Boot/Spring Cloud & Alibaba 统一消息中心</h4>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/version-3.0.0-blue.svg" alt="Version"></a>
  <a href="#"><img src="https://img.shields.io/badge/Spring%20Boot-3.5.9-green.svg" alt="Spring Boot"></a>
  <a href="#"><img src="https://img.shields.io/badge/JDK-17+-orange.svg" alt="JDK"></a>
  <a href="#"><img src="https://img.shields.io/badge/License-Apache%202.0-red.svg" alt="License"></a>
</p>

---

## 目录

- [简介](#-简介)
- [特性](#-特性)
- [架构设计](#-架构设计)
- [快速开始](#-快速开始)
- [模块说明](#-模块说明)
- [文档](#-文档)
- [路线图](#-路线图)
- [贡献](#-贡献)

---

## 简介

Carlos Message 是一个企业级统一消息中心，为业务系统提供一站式消息发送解决方案。支持短信、邮件、钉钉、企业微信、站内信等多种渠道，提供模板管理、定时发送、批量发送、消息追踪、已读回执、敏感词过滤、防刷防重放等完整功能。

### 核心能力

- **多渠道支持**: 短信、邮件、钉钉、企业微信、WebSocket站内信
- **模板管理**: 支持 FreeMarker 变量替换、条件渲染的模板引擎
- **定时调度**: 支持定时发送、延时消息（Redis ZSet 优先级队列）
- **消息追踪**: 完整的发送记录和状态追踪（PENDING -> SENDING -> SENT/DELIVERED/READ/FAILED/REVOKED）
- **失败重试**: 指数退避自动重试机制，确保消息送达
- **限流熔断**: Redis 滑动窗口限流，渠道故障自动转移
- **已读回执**: WebSocket 实时推送已读状态，支持批量标记
- **敏感词过滤**: 基于 DFA 算法的内容安全审核
- **防刷防重放**: 接收人级别频率限制 + 消息内容指纹去重
- **监控告警**: Micrometer 指标采集，渠道故障自动告警
- **SPI 插件化**: 支持通过 Java SPI 动态加载第三方渠道适配器

---

## 特性

| 特性           | 状态 | 说明                                        |
|--------------|----|-------------------------------------------|
| 短信发送         |    | 基于 sms4j 多厂商支持（阿里云、腾讯云等）                  |
| 邮件发送         |    | 基于 Spring Mail / Jakarta Mail，支持 HTML     |
| 钉钉通知         |    | 支持工作通知（Markdown），自动 Token 获取              |
| 企业微信         |    | 待实现                                       |
| WebSocket站内信 |    | STOMP + 原生 WebSocket 双协议，支持撤回指令推送         |
| 定时消息         |    | MySQL 定时调度 + Redis ZSet 延时队列              |
| 延时消息         |    | Redis ZSet 按优先级排序，每秒扫描转移                  |
| 批量发送         |    | MyBatis-Plus saveBatch 批量写入               |
| 消息撤回         |    | 支持 PENDING/SENDING/SENT 状态撤回，WebSocket 推送 |
| 已读回执         |    | HTTP/WebSocket 标记已读，通知发送人                 |
| 渠道健康检查       |    | 每30秒自动检测，连续3次失败标记故障                       |
| 渠道路由         |    | 权重随机选择 + 故障转移                             |
| 限流熔断         |    | Redis ZSet 滑动窗口限流，Lua 脚本保证原子性             |
| 指数退避重试       |    | 初始间隔 * 乘数^(重试次数-1)，最大30秒                  |
| 敏感词过滤        |    | DFA 算法，支持动态添加敏感词                          |
| 防刷防重放        |    | 接收人频率限制 + MD5 内容指纹去重                      |
| 监控指标         |    | Micrometer Counter/Timer/Gauge            |
| 配置热刷新        |    | 每30秒自动重载渠道配置，无需重启                         |
| 统计报表         |    | 今日/范围统计、渠道分布查询                            |
| SPI 插件化      |    | META-INF/services 动态加载渠道适配器               |

---

## 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                    统一消息中心 (Unified Message Center)           │
├─────────────────────────────────────────────────────────────────┤
│  接入层                                                          │
│  ├── REST API (同步/异步/定时/批量发送)                            │
│  ├── Feign Client (微服务间调用)                                   │
│  ├── WebSocket (实时推送 / 已读回执 / 撤回指令)                      │
│  └── MQ Consumer (Redis Stream 消费)                              │
├─────────────────────────────────────────────────────────────────┤
│  业务层                                                          │
│  ├── 消息模板引擎 (FreeMarker 变量替换)                             │
│  ├── 渠道路由策略 (权重随机 + 负载均衡 + 故障转移)                    │
│  ├── 消息优先级队列 (高/中/低优先级 Stream)                          │
│  ├── 消息定时调度 (MySQL 定时 + Redis ZSet 延时)                     │
│  ├── 批量发送优化 (saveBatch 批量写入)                              │
│  ├── 发送频率控制 (Redis 滑动窗口限流)                               │
│  ├── 敏感词过滤 (DFA 算法)                                         │
│  ├── 防刷防重放 (频率限制 + MD5 指纹)                                │
│  └── 指数退避重试 (Retry Queue + 定时任务)                          │
├─────────────────────────────────────────────────────────────────┤
│  渠道适配层                                                       │
│  ├── 短信 (sms4j 多服务商)                                         │
│  ├── 邮件 (Spring Mail / MimeMessage)                             │
│  ├── 即时通讯 (钉钉 OpenAPI / 企业微信)                              │
│  └── 站内信 (WebSocket STOMP / 原生 WebSocket)                      │
├─────────────────────────────────────────────────────────────────┤
│  存储层                                                          │
│  ├── 消息主库 (MySQL - 消息元数据)                                  │
│  ├── 缓存层 (Redis - 限流、模板缓存、消息记录缓存、用户连接)             │
│  └── 队列层 (Redis Stream + ZSet - 即时/延时/定时/重试队列)           │
└─────────────────────────────────────────────────────────────────┘
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+

### 1. 初始化数据库

```bash
# 创建数据库
mysql -u root -p < carlos-message-bus/src/main/resources/message_v2.sql
```

### 2. 配置渠道

编辑 `application.yml`:

```yaml
carlos:
  message:
    enabled: true
    consumer-id: message-consumer-1
    queue:
      type: redis
      stream-prefix: stream:message
      consumer-group: message-consumer-group
      consumer-count: 3
    rate-limiter:
      enabled: true
      default-qps: 100
      channels:
        SMS: 50
        EMAIL: 30
        DINGTALK: 100
    retry:
      max-attempts: 3
      initial-interval: 1000
      multiplier: 2
      max-interval: 30000

spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

### 3. 启动服务

```bash
cd carlos-message-cloud
mvn spring-boot:run
```

### 4. 发送消息

```java
@Autowired
private MessageSendService messageSendService;

public void sendMessage() {
    MessageCreateParam param = new MessageCreateParam();
    param.setTemplateCode("ORDER_SUCCESS");
    param.setTemplateParams(Map.of(
        "userName", "张三",
        "orderNo", "202403110001",
        "amount", "199.00"
    ));
    param.setReceivers(List.of(
        new MessageCreateParam.ReceiverParam()
            .setReceiverId("user_123")
            .setReceiverNumber("13800138000")
    ));
    param.setChannelCodes(List.of("SMS", "DINGTALK"));
    param.setSenderId("system");
    param.setSenderSystem("ORDER_SYSTEM");

    Result<String> result = messageSendService.send(param);
    System.out.println("消息ID: " + result.getData());
}
```

---

## 模块说明

```
carlos-message/
├── carlos-message-api          # API接口定义（Feign接口、AO/Param）
├── carlos-message-core         # 核心模块（协议、枚举、ChannelAdapter接口、Queue/Retry/Template/Limiter接口）
├── carlos-message-bus          # 业务模块（Controller/Service/Manager/Mapper/Channel实现/Queue实现）
├── carlos-message-boot         # Spring Boot 单体启动入口
└── carlos-message-cloud        # Spring Cloud 微服务启动入口
```

| 模块                   | 说明                     | 依赖                                                                    |
|----------------------|------------------------|-----------------------------------------------------------------------|
| carlos-message-core  | 核心协议和接口定义              | 无                                                                     |
| carlos-message-api   | Feign 接口定义，AO/Param 对象 | carlos-message-core                                                   |
| carlos-message-bus   | 服务端业务实现                | carlos-message-core, carlos-message-api, carlos-spring-boot-starter-* |
| carlos-message-boot  | 单机版启动入口                | carlos-message-bus                                                    |
| carlos-message-cloud | 微服务版启动入口               | carlos-message-bus, spring-cloud-starter-*                            |

---

## 文档

- [模块文档中心](./docs/README.md) - 文档导航
- [设计规格说明书](./docs/02-design/architecture/ARCH-20260311-消息模块设计规格说明书.md) - 详细架构设计
- [开发方案](./docs/01-product/PRD/PRD-20260311-消息模块开发方案.md) - PRD/开发规划
- [数据库设计](./docs/02-design/database/DB-20260311-消息模块数据库设计.md) - 表结构设计
- [快速开始](./docs/03-develop/guide/DEV-消息模块快速开始.md) - 本地开发指南
- [开发任务清单](./docs/03-develop/guide/DEV-消息模块开发任务.md) - 任务进度跟踪

---

## 路线图

### Phase 1: 基础能力建设（已完成）

- [x] 基础框架搭建（api / core / bus / boot / cloud）
- [x] 完善短信、钉钉渠道真实 SDK 调用
- [x] 新增 Email、WebSocket 渠道适配器
- [x] 实现消息发送核心流程（同步/异步/定时）
- [x] Redis Stream + ZSet 消息队列

### Phase 2: 功能增强（已完成）

- [x] FreeMarker 模板引擎替换 Hutool 简单替换
- [x] Redis 滑动窗口限流（Lua 原子性）
- [x] 指数退避重试策略 + 重试队列
- [x] 消息状态机（PENDING -> SENDING -> SENT/DELIVERED/READ/FAILED/REVOKED）
- [x] 批量发送优化（saveBatch）
- [x] 延时消息优先级队列（ZSet score = timestamp * 100 + priority）

### Phase 3: 企业级特性（已完成）

- [x] 渠道健康检查（每30秒，连续3次失败标记故障，恢复自动启用）
- [x] ChannelRouter 渠道路由（权重随机 + 故障转移）
- [x] 消息撤回增强（WebSocket 推送撤回指令）
- [x] 已读回执（HTTP/WebSocket，通知发送人）
- [x] 敏感词过滤（DFA 算法）
- [x] 防刷防重放（接收人频率限制 + MD5 指纹去重）
- [x] Micrometer 监控指标（发送量/耗时/活跃数/队列长度/渠道健康）
- [x] 配置热刷新（每30秒自动重载）
- [x] 渠道故障告警（WebSocket /topic/alerts）
- [x] 统计报表（今日/范围/渠道分布）
- [x] SPI 插件化（META-INF/services 动态加载）

### Phase 4: 生态扩展（持续）

- [ ] 企业微信渠道适配器
- [ ] 飞书渠道适配器
- [ ] 移动推送渠道（极光推送）
- [ ] 国际化支持
- [ ] 消息分析报表（可视化大屏）
- [ ] 管理后台前端页面

---

## 贡献

欢迎提交 Issue 和 Pull Request！

### 开发规范

1. 遵循 [Carlos Framework 开发规范](../../AGENTS.md)
2. 所有代码必须通过单元测试（目标覆盖率 80%+）
3. 提交前执行 `mvn clean verify`

### 代码提交

```bash
# 1. Fork 本仓库
# 2. 创建特性分支
git checkout -b feature/your-feature

# 3. 提交更改
git commit -am "feat: your feature"

# 4. 推送到分支
git push origin feature/your-feature

# 5. 创建 Pull Request
```

---

## 许可证

[Apache License 2.0](../../LICENSE)

---

## 联系我们

如有问题或建议，欢迎通过以下方式联系：

- 提交 [GitHub Issue](../../issues)
- 发送邮件至: support@carlos.com

---

<p align="center">Made with  by Carlos Team</p>
