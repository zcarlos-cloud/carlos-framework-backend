# carlos-openapi

## 一、模块简介

`carlos-openapi` 是 Carlos 框架的 API 文档生成模块，基于 SpringDoc OpenAPI 3 和 Knife4j 提供强大的 REST API
文档生成和管理功能。该模块支持自动生成 OpenAPI 规范文档，并提供美观的交互式文档界面。

## 二、主要功能

### 1. 自动 API 文档生成

基于 Spring Boot 3 的 SpringDoc OpenAPI 3 实现，自动扫描项目中的控制器和接口，生成符合 OpenAPI 3.0 规范的文档：

```java
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户管理", description = "用户相关操作接口")
public class UserController {

    @Operation(summary = "获取用户列表", description = "分页查询用户列表")
    @GetMapping("/list")
    public Result<Page<User>> getUserList(@Parameter(description = "分页参数") PageParam param) {
        // 业务逻辑
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @PostMapping("/create")
    public Result<User> createUser(@RequestBody @Valid CreateUserRequest request) {
        // 业务逻辑
    }
}
```

### 2. Knife4j 集成

集成 Knife4j（原 Swagger-Bootstrap-UI），提供美观、功能丰富的文档界面：

- **交互式 API 测试**：直接在文档界面调用 API
- **离线文档下载**：支持导出 Markdown、Word、PDF 格式文档
- **接口分组管理**：支持按模块、按标签分组显示接口
- **全局参数配置**：支持配置全局请求头、认证信息

### 3. 丰富的配置选项

支持全面的 OpenAPI 配置，包括基本信息、联系人、许可证等：

```yaml
openapi:
  enable: true
  title: "Carlos 框架 API 文档"
  description: "基于 Spring Boot 4 的微服务框架"
  version: "3.0.0"
  terms-of-service-url: "https://example.com/terms"
  license: "Apache 2.0"
  license-url: "https://www.apache.org/licenses/LICENSE-2.0"
  contact:
    name: "技术支持"
    email: "zhujunkeep@gmail.com"
    url: "https://example.com"
```

### 4. OAuth2 认证支持

支持在 API 文档中配置 OAuth2 认证信息，方便测试需要认证的接口：

```yaml
openapi:
  auth2:
    enable: true
    token-url: "http://localhost:8080/oauth2/token"
    client-id: "my-client"
    grant-type: "password"  # 支持: password, client_credentials, authorization_code
```

### 5. 自定义参数配置

支持配置全局请求参数，如认证令牌、租户ID等：

```yaml
openapi:
  parameters:
    - name: "Authorization"
      description: "Bearer Token"
      type: "header"
      data-type: "String"
      required: true
      default-value: "Bearer "
    - name: "X-Tenant-Id"
      description: "租户ID"
      type: "header"
      data-type: "String"
      required: false
```

### 6. 接口排序和分组

支持按标签排序，自动为接口分组，提供更好的文档组织结构：

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public GlobalOpenApiCustomizer orderGlobalOpenApiCustomizer() {
        return openApi -> {
            // 为标签添加排序扩展
            openApi.getTags().forEach(tag -> {
                Map<String, Object> extensions = new HashMap<>();
                extensions.put("x-order", RandomUtil.randomInt(0, 100));
                tag.setExtensions(extensions);
            });
        };
    }
}
```

Spring Boot 3 + OpenAPI 注解完整指南

## 三、依赖配置

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-openapi</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. 基本配置

在 `application.yml` 中启用并配置 OpenAPI：

```yaml
# 启用 OpenAPI
openapi:
  enable: true

  # 基本信息
  title: "Carlos 框架 API 文档"
  description: "基于 Spring Boot 4 的微服务框架"
  version: "3.0.0"

  # 扫描配置
  base-package: "com.carlos.controller"
  base-path: "/api/**"

  # 排除路径
  exclude-path:
    - "/error"
    - "/actuator/**"

  # 联系人信息
  contact:
    name: "技术支持"
    email: "zhujunkeep@gmail.com"
    url: "https://example.com"

  # 许可证信息
  license: "Apache 2.0"
  license-url: "https://www.apache.org/licenses/LICENSE-2.0"
```

---

## 四、核心注解详解

1. 全局配置注解

@OpenAPIDefinition - 定义全局 API 信息

```java
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "用户管理系统 API",
        version = "1.0.0",
        description = "基于 Spring Boot 3 的用户管理服务",
        contact = @Contact(name = "技术支持", email = "zhujunkeep@gmail.com"),
        license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "本地环境"),
        @Server(url = "https://api.example.com", description = "生产环境")
    },
    tags = {
        @Tag(name = "用户管理", description = "用户相关接口"),
        @Tag(name = "订单管理", description = "订单相关接口")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Token 认证"
)
public class OpenApiConfig {
}
```

---

2. Controller 层注解

@Tag - 控制器分组

```java
@ApiSupport(order = 284)
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户增删改查接口")
public class UserController {
}
```

@Hidden - 隐藏控制器或方法

```java
@RestController
@Hidden  // 整个控制器不显示在文档中
public class InternalController {
    
    @GetMapping("/public")
    @Hidden  // 仅隐藏此方法
    public String hiddenMethod() {
        return "hidden";
    }
}
```

---

3. 方法层注解

@Operation - 接口描述

```java
@ApiOperationSupport(order = 33)
@Operation(
    summary = "获取用户详情",           // 接口简短描述
    description = "根据用户ID获取详细信息", // 详细描述
    operationId = "getUserById",      // 操作唯一标识
    tags = {"用户管理"},               // 覆盖类级别的 tag
    method = "GET",                   // HTTP 方法
    deprecated = false,               // 是否废弃
    hidden = false,                   // 是否隐藏
    
    // 安全要求
    security = @SecurityRequirement(name = "bearerAuth"),
    
    // 请求参数
    parameters = {
        @Parameter(name = "id", description = "用户ID", required = true, in = ParameterIn.PATH),
        @Parameter(name = "includeDeleted", description = "是否包含已删除用户", in = ParameterIn.QUERY)
    },
    
    // 请求体（用于 POST/PUT）
    requestBody = @RequestBody(
        description = "用户信息",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = UserDTO.class),
            examples = @ExampleObject(
                name = "示例",
                value = "{\"username\": \"zhangsan\", \"age\": 25}"
            )
        )
    ),
    
    // 响应定义
    responses = {
        @Result(
            responseCode = "200",
            description = "查询成功",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserVO.class)
            )
        ),
        @Result(responseCode = "404", description = "用户不存在"),
        @Result(responseCode = "500", description = "服务器内部错误")
    }
)
@GetMapping("/{id}")
public UserVO getUser(@PathVariable Long id, @RequestParam(defaultValue = "false") Boolean includeDeleted) {
    return userService.getById(id);
}
```

@Results / @Result - 响应定义

```java
@Results(value = {
    @Result(
        responseCode = "201",
        description = "创建成功",
        content = @Content(
            schema = @Schema(implementation = UserVO.class),
            examples = @ExampleObject(
                value = "{\"id\": 1, \"username\": \"zhangsan\"}"
            )
        ),
        headers = @Header(
            name = "Location",
            description = "新资源URL",
            schema = @Schema(type = "string")
        )
    ),
    @Result(
        responseCode = "400", 
        description = "请求参数错误",
        content = @Content(
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),
    @Result(responseCode = "409", description = "用户已存在")
})
@PostMapping
public ResponseEntity<UserVO> createUser(@RequestBody UserDTO dto) {
    // ...
}
```

---

4. 参数注解

@Parameter - 参数描述

```java
@GetMapping("/search")
public List<UserVO> search(
    @Parameter(
        name = "keyword",
        description = "搜索关键词",
        required = true,
        example = "张三",
        in = ParameterIn.QUERY,
        schema = @Schema(type = "string", minLength = 2, maxLength = 50)
    ) 
    @RequestParam String keyword,
    
    @Parameter(
        description = "页码",
        example = "1",
        schema = @Schema(type = "integer", defaultValue = "1", minimum = "1")
    )
    @RequestParam(defaultValue = "1") Integer pageNum,
    
    @Parameter(
        description = "每页大小",
        schema = @Schema(allowableValues = {"10", "20", "50"}, defaultValue = "10")
    )
    @RequestParam(defaultValue = "10") Integer pageSize,
    
    @Parameter(hidden = true)  // 隐藏参数
    @RequestHeader("X-Internal-Token") String internalToken
) {
    return userService.search(keyword, pageNum, pageSize);
}
```

特殊参数类型：

```java
// 文件上传
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public String upload(
    @Parameter(description = "用户头像", required = true)
    @RequestPart("file") MultipartFile file,
    
    @Parameter(description = "用户ID")
    @RequestPart("userId") String userId
) {
    return "success";
}

// Spring Data Pageable（分页）
@GetMapping("/page")
@Operation(summary = "分页查询")
public Page<UserVO> page(
    @ParameterObject  // 将 Pageable 展开为 page, size, sort 参数 [^27^]
    @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC)
    Pageable pageable
) {
    return userService.page(pageable);
}
```

---

5. 模型注解（DTO/VO）

@Schema - 模型定义

```java
@Data
@Schema(
    name = "UserDTO",
    description = "用户数据传输对象",
    requiredMode = Schema.RequiredMode.REQUIRED  // 全局要求所有字段必填
)
public class UserDTO {
    
    @Schema(
        description = "用户ID",
        example = "10001",
        accessMode = Schema.AccessMode.READ_ONLY,  // 仅响应，不接收
        hidden = false
    )
    private Long id;
    
    @Schema(
        description = "用户名",
        example = "zhangsan",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minLength = 3,
        maxLength = 20,
        pattern = "^[a-zA-Z0-9_]+$"
    )
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @Schema(
        description = "年龄",
        example = "25",
        minimum = "0",
        maximum = "150"
    )
    @Min(0)
    @Max(150)
    private Integer age;
    
    @Schema(
        description = "用户状态",
        example = "ACTIVE",
        allowableValues = {"ACTIVE", "INACTIVE", "DELETED"}
    )
    private UserStatus status;
    
    @Schema(
        description = "创建时间",
        type = "string",
        format = "date-time",
        example = "2024-01-15T10:30:00Z"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    
    @Schema(
        description = "订单列表",
        implementation = OrderVO.class  // 指定复杂类型
    )
    private List<OrderVO> orders;
    
    @Schema(hidden = true)  // 隐藏字段
    private String password;
}

// 枚举定义
@Schema(enumAsRef = true, description = "用户状态")  // 枚举作为引用 [^27^]
public enum UserStatus {
    ACTIVE("激活"),
    INACTIVE("未激活"),
    DELETED("已删除");
    
    private final String desc;
    
    UserStatus(String desc) {
        this.desc = desc;
    }
    
    @JsonValue
    public String getDesc() {
        return desc;
    }
}
```

---

6. 回调与链接

@Callback - 回调定义

```java
@Operation(
    summary = "订阅事件",
    callbacks = {
        @Callback(
            name = "onEvent",
            callbackUrlExpression = "{$request.body#/callbackUrl}",
            operations = @CallbackOperation(
                summary = "事件通知",
                method = "post",
                requestBody = @RequestBody(
                    content = @Content(
                        schema = @Schema(implementation = EventDTO.class)
                    )
                ),
                responses = @Result(responseCode = "200", description = "接收成功")
            )
        )
    }
)
@PostMapping("/subscribe")
public void subscribe(@RequestBody SubscribeDTO dto) {
    // ...
}
```

---

## 五、完整实战示例

```java
@RestController
@ApiSupport(order = 1)
@RequestMapping("/api/v1/users")
@Tag(name = "用户管理", description = "用户 CRUD 操作")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Operation(
        summary = "创建用户",
        description = "创建新用户，用户名不能重复",
        responses = {
            @Result(
                responseCode = "201",
                description = "创建成功",
                content = @Content(schema = @Schema(implementation = UserVO.class))
            ),
            @Result(responseCode = "400", description = "参数校验失败"),
            @Result(responseCode = "409", description = "用户名已存在")
        }
    )
    @ApiOperationSupport(order = 33)
    @PostMapping
    public ResponseEntity<UserVO> create(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "用户信息",
            required = true,
            content = @Content(
                schema = @Schema(implementation = UserCreateDTO.class),
                examples = @ExampleObject(
                    name = "标准示例",
                    summary = "标准用户创建",
                    value = """
                        {
                            "username": "zhangsan",
                            "email": "zhangsan@example.com",
                            "phone": "13800138000"
                        }
                        """
                )
            )
        )
        @RequestBody @Valid UserCreateDTO dto
    ) {
        UserVO user = userService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @Operation(
        summary = "获取用户列表",
        description = "支持分页和条件查询",
        parameters = {
            @Parameter(name = "deptId", description = "部门ID", in = ParameterIn.QUERY),
            @Parameter(name = "status", description = "状态", in = ParameterIn.QUERY)
        }
    )
    @GetMapping
    public PageResult<UserVO> list(
        @ParameterObject @PageableDefault(size = 20) Pageable pageable,
        @Parameter(hidden = true) UserQueryParam queryParam
    ) {
        return userService.list(pageable, queryParam);
    }

    @Operation(
        summary = "更新用户",
        description = "根据ID更新用户信息",
        responses = {
            @Result(responseCode = "200", description = "更新成功"),
            @Result(responseCode = "404", description = "用户不存在", content = @Content)
        }
    )
    @PutMapping("/{id}")
    public UserVO update(
        @Parameter(description = "用户ID", required = true) @PathVariable Long id,
        @RequestBody @Valid UserUpdateDTO dto
    ) {
        return userService.update(id, dto);
    }

    @Operation(
        summary = "删除用户",
        description = "逻辑删除用户",
        deprecated = true,  // 标记为废弃
        responses = {
            @Result(responseCode = "204", description = "删除成功", content = @Content),
            @Result(responseCode = "404", description = "用户不存在")
        }
    )
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @Parameter(description = "用户ID", required = true) @PathVariable Long id
    ) {
        userService.delete(id);
    }
}
```

---

## 六、配置属性

```yaml
springdoc:
  # API 文档端点配置
  api-docs:
    enabled: true                    # 启用 API docs
    path: /v3/api-docs              # API docs 路径
    groups:
      enabled: true                 # 启用分组
    
  # Swagger UI 配置
  swagger-ui:
    enabled: true                   # 启用 UI
    path: /swagger-ui.html          # UI 访问路径
    config-url: /v3/api-docs/swagger-config
    url: /v3/api-docs
    display-request-duration: true  # 显示请求耗时
    disable-swagger-default-url: true  # 禁用默认 Petstore
    operations-sorter: method       # 按方法排序
    tags-sorter: alpha              # 标签按字母排序
    
  # 扫描配置
  packages-to-scan: com.example.controller  # 扫描包
  paths-to-match: /api/**          # 匹配路径
  
  # 分组配置
  group-configs:
    - group: 用户模块
      paths-to-match: /api/users/**
      packages-to-scan: com.example.user
    - group: 订单模块
      paths-to-match: /api/orders/**
      
  # 模型配置
  model-converters:
    pageable-converter:
      enabled: true                # 启用 Pageable 转换
    deprecating-converter:
      enabled: true                # 识别 @Deprecated
      
  # 显示配置
  show-login-endpoint: false       # 显示登录端点
  show-actuator: false             # 显示 Actuator
  model-and-view-allowed: false    # 允许 ModelAndView
```

---

## 七、访问地址

启动应用后访问：

- **Knife4j 文档界面**: `http://localhost:8080/doc.html`
- **原生 Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **OpenAPI YAML**: `http://localhost:8080/v3/api-docs.yaml`

---

## 八、与 Springfox 对比

| 特性   | Springfox (Swagger 2)    | SpringDoc (OpenAPI 3)           |
|------|--------------------------|---------------------------------|
| 注解包  | `io.swagger.annotations` | `io.swagger.v3.oas.annotations` |
| 主注解  | `@Api`                   | `@Tag`                          |
| 方法描述 | `@ApiOperation`          | `@Operation`                    |
| 参数描述 | `@ApiParam`              | `@Parameter`                    |
| 模型描述 | `@ApiModel`              | `@Schema`                       |
| 响应描述 | `@Result`                | `@Result`（保留）                   |
| 请求体  | 不支持                      | `@RequestBody`                  |
| 回调   | 不支持                      | `@Callback`                     |
| 链接   | 不支持                      | `@Link`                         |

更多详细信息可参考官方文档 [springdoc.org](https://springdoc.org/) 。

## 详细功能说明

### 1. 文档分组配置

支持按模块、按包路径进行文档分组：

```yaml
openapi:
  group-name: "默认分组"

  # 或者按模块分组
  groups:
    user:
      display-name: "用户管理"
      paths-to-match: "/api/user/**"
    order:
      display-name: "订单管理"
      paths-to-match: "/api/order/**"
```

### 2. 安全配置

支持 API Key、HTTP Basic、OAuth2 等多种认证方式：

```java
@Configuration
public class SecurityConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 认证")))
            .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
```

### 3. 自定义文档扩展

支持添加自定义扩展信息：

```java
@Bean
public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("API文档")
            .version("1.0.0")
            .description("系统API文档"))
        .addExtension("x-api-version", "v3")
        .addExtension("x-supported-languages", Arrays.asList("zh-CN", "en-US"));
}
```

### 4. 响应示例配置

支持配置响应示例，提高文档可读性：

```java
@Operation(summary = "获取用户", responses = {
    @Result(responseCode = "200", description = "成功",
        content = @Content(schema = @Schema(implementation = User.class),
        examples = @ExampleObject(
            name = "成功示例",
            value = "{\"code\":2000,\"message\":\"成功\",\"data\":{\"id\":1,\"username\":\"admin\"}}"
        ))),
    @Result(responseCode = "404", description = "用户不存在")
})
@GetMapping("/{id}")
public Result<User> getUser(@PathVariable Long id) {
    // 业务逻辑
}
```

## 配置项说明

| 配置项                            | 类型          | 默认值                          | 说明              |
|--------------------------------|-------------|------------------------------|-----------------|
| `openapi.enable`               | boolean     | `false`                      | 是否启用 OpenAPI    |
| `openapi.base-package`         | String      | -                            | 扫描的基本包，多个包用逗号分隔 |
| `openapi.base-path`            | Set<String> | `["/**"]`                    | 扫描的 URL 规则      |
| `openapi.exclude-path`         | Set<String> | `["/error", "/actuator/**"]` | 排除的 URL 规则      |
| `openapi.title`                | String      | -                            | 文档标题            |
| `openapi.description`          | String      | -                            | 文档描述            |
| `openapi.version`              | String      | -                            | API 版本          |
| `openapi.terms-of-service-url` | String      | -                            | 服务条款 URL        |
| `openapi.license`              | String      | -                            | 许可证名称           |
| `openapi.license-url`          | String      | -                            | 许可证 URL         |
| `openapi.contact.name`         | String      | -                            | 联系人姓名           |
| `openapi.contact.email`        | String      | -                            | 联系人邮箱           |
| `openapi.contact.url`          | String      | -                            | 联系人 URL         |
| `openapi.auth2.enable`         | boolean     | `false`                      | 是否启用 OAuth2     |
| `openapi.auth2.token-url`      | String      | -                            | Token 获取地址      |
| `openapi.auth2.client-id`      | String      | -                            | 客户端 ID          |
| `openapi.auth2.grant-type`     | String      | -                            | 授权类型            |

## 九、注意事项

### 1. 性能考虑

- 生产环境建议关闭 OpenAPI 文档生成，或通过配置文件控制
- 扫描的包路径尽量精确，避免扫描不必要的包
- 大量接口时，考虑按模块分组显示

### 2. 安全建议

- 生产环境不要暴露文档界面，或添加访问控制
- 敏感接口（如管理员接口）建议单独分组并设置访问权限
- 文档中的示例数据不要使用真实敏感信息

### 3. 使用建议

- 为所有公共接口添加完整的文档注解
- 使用统一的响应格式和错误码
- 为复杂的业务对象提供详细的 Schema 描述
- 及时更新文档，保持与代码同步

### 4. 常见问题

**Q: 文档界面无法访问怎么办？**
A: 检查以下事项：

1. 确认 `openapi.enable=true`
2. 确认没有安全拦截器阻止访问 `/doc.html` 或 `/swagger-ui.html`
3. 确认项目依赖了正确的 Spring Boot 和 SpringDoc 版本
4. 查看应用启动日志，确认 Knife4j 初始化成功

**Q: 部分接口没有显示在文档中？**
A: 可能的原因：

1. 接口不在 `base-package` 扫描范围内
2. 接口路径被 `exclude-path` 排除
3. 控制器类缺少 `@RestController` 或 `@Controller` 注解
4. 方法缺少 `@RequestMapping` 系列注解

**Q: 如何自定义文档样式？**
A: Knife4j 支持主题定制：

1. 在 `resources` 目录创建 `META-INF/resources/` 目录
2. 添加自定义 CSS 文件
3. 在配置中指定自定义样式文件路径

**Q: 如何导出离线文档？**
A: 使用 Knife4j 的导出功能：

1. 访问 `http://localhost:8080/doc.html`
2. 点击右上角"导出"按钮
3. 选择导出格式（Markdown、Word、PDF、OpenAPI JSON/YAML）

### 5. 与 carlos-spring-boot-core 集成

本模块使用 `carlos-spring-boot-core` 模块的工具类获取应用地址和端口信息，确保文档链接的正确性。

### 6. 版本要求

- JDK 17+
- Spring Boot 3.5.8+
- SpringDoc OpenAPI 2.x+
- Knife4j 4.x+

### 7.相关模块

- **carlos-spring-boot-core**：基础工具类、常量定义、响应格式
- **carlos-auth**：OAuth2 认证授权，为文档提供认证支持
- **carlos-gateway**：API 网关，文档可能需要网关相关配置
