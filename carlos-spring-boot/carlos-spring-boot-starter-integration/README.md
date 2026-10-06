# carlos-spring-boot-starter-integration

第三方系统对接组件，基于 **Spring RestClient** 提供与钉钉、企业微信等第三方平台的集成功能

## 特

- **完全隔离**：基Spring RestClient，与项目 Feign 配置完全隔离
- **声明式接*：支`@HttpExchange` 注解定义接口，开发体验与 Feign 相当
- **动态注*：支持运行时动态注册和注销客户
- **轻量*：仅依赖 Spring Boot 原生能力，无额外传递依
- **高性能**：原生支Java 虚拟线程
- **可扩*：支持拦截器、认证、日志等扩展

## 快速开

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-integration</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. 配置钉钉

```yaml
carlos:
  integration:
    dingtalk:
      enabled: true
      host: https://oapi.dingtalk.com
      appkey: ${DINGTALK_APPKEY}
      appsecret: ${DINGTALK_APPSECRET}
      agent-id: ${DINGTALK_AGENT_ID}
      corp-id: ${DINGTALK_CORP_ID}
```

### 3. 使用钉钉服务

```java
@Autowired
private DingtalkRestClientService dingtalkService;

public void test() {
    // 获取用户信息
    var user = dingtalkService.getUserInfo("user123");
    
    // 发送消
    DingtalkMessageRequest request = new DingtalkMessageRequest();
    // ... 设置消息内容
    dingtalkService.sendMessage(request);
}
```

## 高级用法

### 自定API 接口

```java
@HttpExchange(url = "/api/v1", contentType = "application/json")
public interface MyApiClient {
    
    @GetExchange("/users/{id}")
    User getUser(@PathVariable String id);
    
    @PostExchange("/users")
    User createUser(@RequestBody User user);
}
```

### 注册Spring 容器

```java
@Bean
public MyApiClient myApiClient() {
    return RestClientBuilderUtils.createService(
        "https://api.example.com",
        MyApiClient.class
    );
}
```

### 动态注册客户端

```java
@Autowired
private DockingClientRegistry registry;

public void register() {
    // 注册预定义接
    MyApiClient client = registry.registerPredefinedClient(
        "myClient",
        MyApiClient.class,
        config
    );
    
    // 注册动态客户端
    DynamicHttpClient dynamicClient = registry.registerDynamicClient(
        "dynamicClient",
        config
    );
}
```

## 架构说明

```
┌─────────────────────────────────────────────────────
                  应用(Service)                    
         DingtalkRestClientService                 
└────────────────────────┬────────────────────────────
                         
┌────────────────────────▼────────────────────────────
                  接口(API)                       
           DingtalkApiClient (@HttpExchange)       
└────────────────────────┬────────────────────────────
                         
┌────────────────────────▼────────────────────────────
               RestClient 实例                      
   (独立配置，与项目 Feign 完全隔离)                 
└────────────────────────┬────────────────────────────
                         
┌────────────────────────▼────────────────────────────
               HTTP 客户端实                      
        JDK HttpClient / OkHttp / ...              
└─────────────────────────────────────────────────────
```

## 配置详解

### 全局配置

```yaml
carlos:
  integration:
    restclient:
      enabled: true
      defaults:
        connect-timeout: 5s
        read-timeout: 30s
        follow-redirects: true
        headers:
          X-Custom-Header: value
```

### 客户端配

```yaml
carlos:
  docking:
    restclient:
      clients:
        custom-client:
          enabled: true
          type: predefined  # dynamic
          interface-class: com.example.MyApiClient
          base-url: https://api.example.com
          connect-timeout: 10s
          read-timeout: 60s
          headers:
            X-API-Key: ${API_KEY}
          auth:
            type: bearer
            token: ${TOKEN}
          interceptors:
            - com.example.CustomInterceptor
```

## 迁移说明

OpenFeign 迁移RestClient，请参[RESTCLIENT_MIGRATION_GUIDE.md](RESTCLIENT_MIGRATION_GUIDE.md)

## 版本要求

- JDK 17+
- Spring Boot 3.2+（当前项目使3.5.9

## Feign 对比

| 特    | OpenFeign      | Spring RestClient |
|------|----------------|-------------------|
| 配置干扰 | 全局配置易干         | 完全隔离              |
| 依赖   | 需 Spring Cloud | Spring Boot 原生    |
| 声明   | @FeignClient   | @HttpExchange     |
| 性能   | 良好             | 更优                |
| 虚拟线程 | 不支             | 原生支持              |

## 许可

MIT
