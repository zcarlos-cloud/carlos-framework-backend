# carlos-datacenter

数据中心集成组件，提供与外部数据中心系统的数据同步和交互功能。

## 功能特性

- **多数据源支持**: 支持配置多个数据中心实例
- **数据同步**: 提供数据推送和拉取功能
- **加密传输**: 支持数据加密传输
- **异步处理**: 支持异步数据同步任务
- **失败重试**: 自动重试机制，确保数据可靠传输

## 快速开始

### Maven依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-datacenter</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 配置示例

```yaml
carlos:
  docking:
    datacenter:
      enabled: true
      instances:
        # 数据中心实例1
        datacenter-1:
          instance-id: datacenter-1
          supplier: ShuNing
          base-url: http://datacenter.example.com:8080/api
          app-key: your-app-key
          app-secret: your-app-secret
          timeout: 30000
          encrypt:
            enabled: true
            key: your-encrypt-key
            iv: your-encrypt-iv
        # 数据中心实例2
        datacenter-2:
          instance-id: datacenter-2
          supplier: Custom
          base-url: http://datacenter2.example.com:8080/api
          app-key: your-app-key-2
          app-secret: your-app-secret-2
```

## 使用示例

### 数据推送

```java
@Autowired
private DatacenterService datacenterService;

public void pushData() {
    DataPushRequest request = new DataPushRequest();
    request.setInstanceId("datacenter-1");
    request.setDataType("user");
    request.setData(userData);

    DataPushResponse response = datacenterService.pushData(request);
    if (response.isSuccess()) {
        System.out.println("数据推送成功");
    }
}
```

### 数据拉取

```java
public void pullData() {
    DataPullRequest request = new DataPullRequest();
    request.setInstanceId("datacenter-1");
    request.setDataType("organization");
    request.setStartTime(startTime);
    request.setEndTime(endTime);

    DataPullResponse response = datacenterService.pullData(request);
    List<Organization> organizations = response.getData();
}
```

## 支持的数据中心供应商

| 供应商     | 说明      |
|---------|---------|
| ShuNing | 数宁数据中心  |
| Custom  | 自定义数据中心 |

## 依赖模块

- **carlos-spring-boot-core**: 核心基础功能
- **carlos-spring-boot-starter-encrypt**: 数据加密支持

## 注意事项

- 生产环境建议启用数据加密传输
- 合理配置超时时间，避免长时间阻塞
- 建议使用异步方式处理大批量数据同步
- 注意监控同步失败情况，及时处理异常

## 版本要求

- JDK 17+
- Spring Boot 3.x
