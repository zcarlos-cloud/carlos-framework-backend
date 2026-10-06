# RestClient 迁移指南

## 概述

本模块已升级为使**Spring RestClient** 替代原有**OpenFeign**，解决了 Feign 配置与项Feign 相互干扰的问题

## 主要变化

### 1. 依赖变更

**移除依赖*

- `spring-cloud-starter-openfeign`

**新增依赖*

- `spring-boot-starter-web` (包含 RestClient)

### 2. 核心架构

```
旧架构：
Controller FeignClient HTTP

新架构：
Controller @HttpExchange 接口 RestClient HTTP
```

## 钉钉模块使用方式

### 方式一：使用预定义接口（推荐）

```java
// 自动注入钉钉 API 客户
@Autowired
private DingtalkApiClient dingtalkApiClient;

// 使用
DingtalkTokenResponse token = dingtalkApiClient.getToken(appkey, appsecret);
```

### 方式二：使用 DingtalkRestClientService

```java
@Autowired
private DingtalkRestClientService dingtalkService;

// 获取用户信息
DingtalkUserResponse.DingtalkUser user = dingtalkService.getUserInfo(userId);

// 发送消
DingtalkMessageRequest request = new DingtalkMessageRequest();
// ... 设置消息内容
dingtalkService.sendMessage(request);
```

### 方式三：动态创建客户端

```java
@Autowired
private DockingClientRegistry registry;

// 注册预定义接
DingtalkApiClient client = registry.registerPredefinedClient(
    "customDingtalk",
    DingtalkApiClient.class,
    clientConfig
);

// 注册动态客户端
DynamicHttpClient dynamicClient = registry.registerDynamicClient(
    "dynamicClient",
    clientConfig
);
```

## 配置方式

### application.yml

```yaml
carlos:
  docking:
    # 钉钉配置
    dingtalk:
      enabled: true
      host: https://oapi.dingtalk.com
      appkey: ${DINGTALK_APPKEY}
      appsecret: ${DINGTALK_APPSECRET}
      agent-id: ${DINGTALK_AGENT_ID}
      
    # RestClient 全局配置
    restclient:
      enabled: true
      defaults:
        connect-timeout: 5s
        read-timeout: 30s
      clients:
        # 自定义客户端示例
        wecom:
          enabled: true
          type: predefined
          interface-class: com.example.WeComApiClient
          base-url: https://qyapi.weixin.qq.com
          auth:
            type: bearer
            token: ${WECOM_TOKEN}
```

## 定义新的 API 接口

```java
@HttpExchange(url = "/api", contentType = "application/json")
public interface MyApiClient {
    
    @GetExchange("/users/{id}")
    User getUser(@PathVariable String id);
    
    @PostExchange("/users")
    User createUser(@RequestBody User user);
    
    @PutExchange("/users/{id}")
    User updateUser(@PathVariable String id, @RequestBody User user);
    
    @DeleteExchange("/users/{id}")
    void deleteUser(@PathVariable String id);
}
```

## 使用工具类快速创

```java
// 快速创建服务代
MyApiClient client = RestClientBuilderUtils.createService(
    "https://api.example.com",
    MyApiClient.class
);

// 带配置的创建
RestClientBuilderUtils.ServiceConfig config = new RestClientBuilderUtils.ServiceConfig();
config.setConnectTimeout(Duration.ofSeconds(10));
config.setReadTimeout(Duration.ofSeconds(60));

MyApiClient clientWithConfig = RestClientBuilderUtils.createService(
    "https://api.example.com",
    MyApiClient.class,
    config
);
```

## 编程式使RestClient

```java
@Autowired
private RestClient.Builder restClientBuilder;

public void callApi() {
    RestClient client = restClientBuilder
        .baseUrl("https://api.example.com")
        .defaultHeader("Authorization", "Bearer " + token)
        .build();
    
    String result = client.get()
        .uri("/users/{id}", id)
        .retrieve()
        .body(String.class);
}
```

## 优势对比

| 特    | OpenFeign       | Spring RestClient           |
|------|-----------------|-----------------------------|
| 配置干扰 | 全局配置易干          | 完全隔离，独立配                    |
| 依赖重量 | 依赖 Spring Cloud | Spring Boot 原生支持            |
| 声明式接 | @FeignClient    | @HttpExchange               |
| 动态代  | 支持              | 支持（HttpServiceProxyFactory） |
| 性能   | 良好              | 更优（支持虚拟线程）                  |

## 迁移检查清

- [ ] 检查是否移除了 OpenFeign 依赖
- [ ] 检查钉钉配置是否正
- [ ] 检查自定义 Feign 客户端是否需要迁
- [ ] 测试钉钉相关功能是否正常
- [ ] 更新相关文档

## 兼容性说

- 旧版 `DingtalkService` 仍然可用（标记为 Deprecated
- 新版 `DingtalkRestClientService` 提供相同功能
- 两者可共存，但建议使用新版
