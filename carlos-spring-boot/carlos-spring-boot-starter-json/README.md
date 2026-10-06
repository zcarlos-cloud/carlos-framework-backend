# carlos-spring-boot-starter-json

JSON 序列化组件，提供统一的 JSON 处理能力，支持 Jackson、Fastjson2 和 Gson 多引擎切换。

## 功能特性

- **多引擎支持**: 支持 Jackson（默认）、Fastjson2、Gson 三种 JSON 引擎
- **配置切换**: 通过配置文件轻松切换底层 JSON 引擎
- **统一接口**: 提供统一的 `JsonService` 接口和 `JsonUtils` 工具类，屏蔽底层实现差异
- **自动配置**: Spring Boot 自动配置，零配置即可使用
- **Web 响应包装**: 可选的 API 响应统一包装功能
- **类型安全**: 支持泛型反序列化
- **日期处理**: 统一的日期格式化配置
- **空值处理**: 灵活的 null 值处理策略
- **JSON 路径提取**: 支持使用 JsonPath 提取 JSON 中的数据
- **对象深拷贝**: 提供深拷贝工具方法

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-json</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 配置示例

#### 基础配置

```yaml
carlos:
  json:
    # 是否启用（默认 true）
    enabled: true
    # JSON 引擎：JACKSON（默认）、FASTJSON2、GSON
    engine: JACKSON
    # 编码格式
    charset: UTF-8
    # 日期格式
    date-format: yyyy-MM-dd HH:mm:ss
    # 时区
    time-zone: GMT+8
    # 是否格式化输出
    pretty-print: false
    # 是否忽略 null 值
    ignore-null: true

    # 序列化配置
    serialization:
      indent-output: false
      fail-on-empty-beans: false
      long-to-string: true  # Long 转 String，解决精度丢失

    # 反序列化配置
    deserialization:
      fail-on-unknown-properties: false
      allow-single-quotes: true
      allow-comments: false
      allow-unescaped-control-chars: true

    # Web 响应包装配置
    web-response:
      wrap-response: false  # 是否启用响应包装
      success-code: 200
      message-field: message
      data-field: data
      exclude-paths:
        - /actuator
        - /swagger
```

#### 切换 JSON 引擎

```yaml
# 使用 Fastjson2
carlos:
  json:
    engine: FASTJSON2

# 使用 Gson
carlos:
  json:
    engine: GSON
```

## 使用示例

### 使用 JsonUtils（推荐）

```java
import com.carlos.json.JsonUtils;

public class JsonExample {
    
    public void examples() {
        User user = new User();
        user.setId(1L);
        user.setName("张三");
        user.setCreateTime(LocalDateTime.now());

        // ============ 序列化 ============
        // 转为 JSON 字符串
        String json = JsonUtils.toJson(user);
        // 输出: {"id":"1","name":"张三","createTime":"2025-01-15 10:30:00"}

        // 转为格式化的 JSON 字符串
        String prettyJson = JsonUtils.toJsonPretty(user);

        // 转为 JSON 字节数组
        byte[] jsonBytes = JsonUtils.toJsonBytes(user);

        // 包含 null 值的序列化
        String jsonWithNulls = JsonUtils.toJsonWithNulls(user);

        // ============ 反序列化 ============
        // JSON 转对象
        User parsedUser = JsonUtils.fromJson(json, User.class);

        // JSON 转 List
        List<User> userList = JsonUtils.toList(jsonArray, User.class);

        // JSON 转 Map
        Map<String, Object> map = JsonUtils.toMap(json);

        // 支持泛型
        Map<String, User> userMap = JsonUtils.fromJson(json, new TypeReference<Map<String, User>>() {});

        // ============ 类型转换 ============
        // Map 转对象
        User userFromMap = JsonUtils.mapToObject(map, User.class);

        // 对象转 Map
        Map<String, Object> mapFromUser = JsonUtils.objectToMap(user);

        // 对象类型转换
        UserDTO dto = JsonUtils.convertValue(user, UserDTO.class);

        // ============ 节点操作 ============
        // 提取 JSON 中的值（支持 JsonPath）
        String name = JsonUtils.extract(json, "name");
        String city = JsonUtils.extract(json, "address.city", String.class);

        // 合并两个 JSON 对象
        String merged = JsonUtils.merge(json1, json2);

        // ============ 验证和检查 ============
        // 验证 JSON 格式
        boolean isValid = JsonUtils.isValidJson(json);

        // 检查是否可序列化
        boolean canSerialize = JsonUtils.canSerialize(user);

        // ============ 便捷方法 ============
        // 深拷贝
        User copiedUser = JsonUtils.deepCopy(user);
        UserDTO copiedDto = JsonUtils.deepCopy(user, UserDTO.class);

        // 比较两个对象
        boolean equals = JsonUtils.equals(user1, user2);

        // 美化 JSON
        String prettified = JsonUtils.prettify(compactJson);

        // 压缩 JSON
        String compacted = JsonUtils.compact(prettyJson);

        // ============ 引擎相关 ============
        // 获取当前引擎名称
        String engineName = JsonUtils.getEngineName(); // "Jackson"

        // 获取底层引擎实例
        ObjectMapper objectMapper = (ObjectMapper) JsonUtils.getEngine();
    }
}
```

### 使用 JsonService

```java
import com.carlos.json.core.JsonService;
import com.carlos.json.JsonFactory;

@Service
public class UserService {
    
    @Autowired
    private JsonService jsonService;  // 自动注入默认引擎

    public void example() {
        // 使用注入的 jsonService 进行操作
        String json = jsonService.toJson(user);
        User parsedUser = jsonService.fromJson(json, User.class);
    }

    public void useSpecificEngine() {
        // 显式获取特定引擎
        JsonService jacksonService = JsonFactory.getService(JsonEngineType.JACKSON);
        JsonService fastjsonService = JsonFactory.getService(JsonEngineType.FASTJSON2);
        JsonService gsonService = JsonFactory.getService(JsonEngineType.GSON);
    }
}
```

### Web 响应包装

启用响应包装后，Controller 的返回值会自动包装为统一格式：

```yaml
carlos:
  json:
    web-response:
      wrap-response: true
```

Controller 代码：

```java
@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public User getUser(@PathVariable Long id) {
        return userService.getById(id);
    }
}
```

响应结果：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "张三"
  },
  "timestamp": "2025-01-15T10:30:00",
  "success": true
}
```

手动创建响应：

```java
import com.carlos.core.response.Result;

@RestController
public class UserController {
    
    @GetMapping("/user/{id}")
    public Result<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }

    @PostMapping("/user")
    public Result<Void> createUser(@RequestBody User user) {
        userService.save(user);
        return Result.success("创建成功");
    }

    @GetMapping("/error")
    public Result<Void> error() {
        return Result.error(500, "服务器内部错误");
        // 或使用便捷方法
        // return Result.badRequest("参数错误");
        // return Result.unauthorized("未授权");
        // return Result.forbidden("禁止访问");
        // return Result.notFound("资源不存在");
    }
}
```

### 使用 JacksonUtil（原有功能保留）

```java
import com.carlos.json.jackson.JacksonUtil;

@Service
public class UserService {
    
    public void example() {
        // 序列化
        String json = JacksonUtil.toJson(user);
        String prettyJson = JacksonUtil.toJsonPretty(user);

        // 反序列化
        User user = JacksonUtil.fromJson(json, User.class);
        List<User> users = JacksonUtil.readList(jsonArray, User.class);
        Map<String, Object> map = JacksonUtil.readMap(json);

        // 类型转换
        UserDTO dto = JacksonUtil.convertValue(user, UserDTO.class);

        // 节点操作
        ObjectNode node = JacksonUtil.createObjectNode();
        node.put("name", "张三");
        node.put("age", 25);
    }
}
```

## Jackson 配置说明

### 常用注解

```java
@Data
public class User {
    @JsonProperty("user_id")
    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonIgnore
    private String password;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String remark;

    // 序列化时 Long 转为 String
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bigNumber;
}
```

### 字段命名策略

```yaml
carlos:
  json:
    field-mapping:
      # 属性命名策略
      property-naming-strategy: SNAKE_CASE  # 下划线命名
      # 其他选项：LOWER_CAMEL_CASE（驼峰，默认）、KEBAB_CASE（短横线）、UPPER_CAMEL_CASE（首字母大写）
```

## JSON 引擎对比

| 特性        | Jackson | Fastjson2 | Gson |
|-----------|---------|-----------|------|
| 性能        | 较快      | 最快        | 一般   |
| 功能丰富度     | 非常丰富    | 丰富        | 丰富   |
| 安全性       | 高       | 高（2.x版本）  | 高    |
| Spring 集成 | 原生支持    | 需配置       | 需配置  |
| 注解支持      | 完善      | 完善        | 完善   |
| 流式处理      | 支持      | 支持        | 支持   |

## 依赖模块

- **carlos-spring-boot-core**: 核心基础功能
- **Spring Boot Starter JSON**: Spring 默认 JSON 支持
- **Fastjson2**: 阿里巴巴高性能 JSON 库
- **Gson**: Google JSON 库
- **JsonPath**: JSON 路径提取库

## 版本要求

- JDK 17+
- Spring Boot 3.x
- Jackson 2.15+
- Fastjson2 2.0.60+
- Gson 2.11+

## 注意事项

- 生产环境建议使用 Jackson（Spring Boot 默认），安全性更好
- Fastjson2 适合对性能要求极高的场景
- Gson 适合简单场景或 Android 开发经验迁移
- 切换引擎后，部分高级特性可能有差异
- 注意日期时区配置，避免时间错乱
- 敏感字段使用 `@JsonIgnore` 注解排除
- 启用响应包装时，String 类型返回值会被特殊处理
