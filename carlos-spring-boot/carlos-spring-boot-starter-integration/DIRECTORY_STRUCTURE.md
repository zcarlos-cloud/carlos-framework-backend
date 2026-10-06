# 模块目录结构说明

## 新目录结构（已重组）

```
com.carlos.integration/
├── common/                              # 公共
  ├── config/
    └── integrationConfig.java           # 通用配置
  └── exception/
      ├── integrationException.java        # 基础异常
      ├── integrationRequestException.java
      └── integrationRequestParamException.java

├── core/                                # 核心框架
  ├── client/                          # 客户
    ├── integrationClientRegistry.java   # 客户端注册中
    ├── DynamicHttpClient.java       # 动态客户端接口
    └── RequestBuilder.java          # 请求构建
  
  ├── config/                          # 配置
    ├── integrationRestClientAutoConfiguration.java  # 自动配置 
    └── integrationRestClientProperties.java         # 配置属
  
  ├── interceptor/                     # 拦截
    ├── BasicAuthInterceptor.java    # 基础认证拦截
    └── integrationLoggingInterceptor.java
  
  └── support/                         # 支持工具
      ├── integrationRestClientException.java
      └── RestClientBuilderUtils.java  # 工具

└── module/                              # 业务模块
    └── dingtalk/                        # 钉钉模块
        ├── api/                         # API声明
          ├── DingtalkApiClient.java   # @HttpExchange 接口 
          └── dto/                     # DTO包（32个类
              ├── DingtalkBaseRequest.java
              ├── DingtalkBaseResponse.java
              ├── DingtalkTokenResponse.java
              ├── DingtalkUser*.java
              ├── DingtalkDept*.java
              └── DingtalkMessage*.java
        
        ├── config/                      # 配置
          ├── DingtalkProperties.java
          ├── DingtalkRestClientConfig.java  # 钉钉RestClient配置 
          ├── DingtalkAccessTokenManager.java
          └── DingtalkConstant.java
        
        ├── service/                     # 服务
          ├── DingtalkRestClientService.java  # 新版服务 
          └── DingtalkService.java       # 旧版服务（@Deprecated
        
        ├── support/                     # 支持
          ├── DingtalkInitWorker.java
          ├── DingtalkUtil.java
          ├── DingtalkMsgType.java
          ├── SendMessageRequest.java
          └── msg/                     # 消息类型
              ├── Msg.java
              ├── TextMsg.java
              ├── ImageMsg.java
              ├── MarkdownMsg.java
              └── ...
        
        └── exception/                   # 钉钉异常
            ├── integrationDingtalkException.java
            ├── integrationRequestDingtalkException.java
            └── integrationRequestParamDingtalkException.java
```

## 目录设计原则

### 1. 分层架构

| 层级         | 职责          | 包路                                         |
|------------|-------------|--------------------------------------------|
| **common** | 公共代码，跨模块共享  | `com.carlos.integration.common.*`          |
| **core**   | 核心框架，提供基础能力 | `com.carlos.integration.core.*`            |
| **module** | 业务模块，按系统划分  | `com.carlos.integration.module.{system}.*` |

### 2. 模块内部结构

每个模块（如 dingtalk）内部按职责分层

| 子包          | 职责        | 示例                                          |
|-------------|-----------|---------------------------------------------|
| `api`       | 声明式接口和DTO | `DingtalkApiClient`, `DingtalkUserResponse` |
| `config`    | 配置        | `DingtalkRestClientConfig`                  |
| `service`   | 业务服务      | `DingtalkRestClientService`                 |
| `support`   | 辅助        | `DingtalkUtil`, `DingtalkInitWorker`        |
| `exception` | 异常定义      | `integrationDingtalkException`              |

### 3. 核心类职

|                                          | 所在包                       | 职责                 |
|------------------------------------------|---------------------------|--------------------|
| `integrationClientRegistry`              | `core.client`             | 统一管理所有第三方客户端的注册和获  |
| `DynamicHttpClient`                      | `core.client`             | 提供完全动态配置的客户端接      |
| `integrationRestClientAutoConfiguration` | `core.config`             | Spring Boot 自动配置入口 |
| `DingtalkApiClient`                      | `module.dingtalk.api`     | 钉钉 API 的声明式接口定义    |
| `DingtalkRestClientService`              | `module.dingtalk.service` | 钉钉业务服务实现           |

## 使用示例

### 1. 使用钉钉服务

```java
import com.carlos.integration.module.dingtalk.service.DingtalkRestClientService;

@Autowired
private DingtalkRestClientService dingtalkService;

public void test() {
    var user = dingtalkService.getUserInfo("user123");
}
```

### 2. 动态注册客户端

```java
import com.carlos.integration.core.client.integrationClientRegistry;

@Autowired
private integrationClientRegistry registry;

public void register() {
    MyApiClient client = registry.registerPredefinedClient(
        "myClient", MyApiClient.class, config);
}
```

### 3. 工具类使

```java
import com.carlos.integration.core.support.RestClientBuilderUtils;

MyApiClient client = RestClientBuilderUtils.createService(
    "https://api.example.com", MyApiClient.class);
```

## 新增模块指南

如需添加新的第三方系统（如企业微信），按以下步骤

1. `com.carlos.integration.module` 下创建新包（`wecom`
2. 按标准结构创建子包：`api`, `config`, `service`, `support`, `exception`
3. `api` 包中定义 `@HttpExchange` 接口
4. `config` 包中创建配置
5. `service` 包中实现业务逻辑
6. 更新 `AutoConfiguration.imports` 注册新配

示例

```
com.carlos.integration.module.wecom/
├── api/
  ├── WeComApiClient.java
  └── dto/
├── config/
  ├── WeComProperties.java
  └── WeComRestClientConfig.java
├── service/
  └── WeComRestClientService.java
└── exception/
    └── integrationWeComException.java
```
