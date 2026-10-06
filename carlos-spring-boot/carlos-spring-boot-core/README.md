# carlos-spring-boot-core

## 模块简介

`carlos-spring-boot-core` 是 Carlos 框架的核心基础模块，提供了框架所需的基础抽象、工具类和基类。该模块是整个框架的基石，被其他大部分模块所依赖。

## 主要功能

### 1. 统一响应格式

提供标准化的 REST API 响应包装器：

```java
// 成功响应
Result<User> result =Result.success(user);

// 失败响应
Result<Void> result = Result.error("操作失败");

// 自定义状态码
Result<Void> result = Result.error(StatusCode.UNAUTHORIZED, "未授权");

// 获取数据（返回 Optional）
Optional<User> data = result.data();
```

**状态码枚举** (`StatusCode`)：

- `SUCCESS` (2000): 成功
- `LOGIN_EXCEPTION` (4000): 登录异常
- `UNAUTHORIZED` (4001): 未授权
- `NOT_PERMISSION` (4003): 无权限
- `NOT_FOUND` (4004): 未找到
- `FAIL` (5000): 失败
- `PARAMETER_EXCEPTION` (5001): 参数异常

### 2. 用户认证与授权

**用户上下文管理**：

```java
// 获取当前用户信息
UserContext userContext = CurrentUser.getUserContext();
Long userId = userContext.getUserId();
String userName = userContext.getUserName();
List<Long> roleIds = userContext.getRoleIds();
Long tenantId = userContext.getTenantId();

// 设置用户上下文
CurrentUser.setUserContext(userContext);

// 清除用户上下文
CurrentUser.remove();
```

**登录用户信息** (`LoginUserInfo`)：

- 用户 ID、账号、密码
- 角色列表
- 部门信息
- 客户端信息

**OAuth2 支持**：

- `TokenVO`: Token 响应包装
- `Oauth2TokenDTO`: OAuth2 令牌数据传输对象
- `AccessTokenParam`: OAuth2 令牌请求参数
- `TokenPayload`: JWT 令牌载荷
- `GrantTypeEnum`: OAuth2 授权类型枚举

### 3. 分页支持

```java
// 创建分页结果
Paging<User> paging = new Paging<>();
paging.setTotal(100L);
paging.setRecords(userList);
paging.setCurrent(1);
paging.setSize(10);

// 分页数据转换
PageConvert<UserVO, User> converter = user -> {
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    return vo;
};
Paging<UserVO> voPage = paging.convert(converter);
```

### 4. 请求参数包装

**基础参数类**：

- `ParamPage`: 分页参数（当前页、每页大小）
- `ParamId<T>`: ID 参数包装
- `ParamLongId`: Long 类型 ID 参数
- `ParamKeyWord`: 关键字搜索参数
- `ParamDate`: 日期范围参数
- `ParamDateKeyWord`: 日期范围 + 关键字参数
- `ParamIdList<T>`: ID 列表参数
- `ParamIdSet<T>`: ID 集合参数

```java
// 分页查询
@PostMapping("/list")
public Result<Paging<User>> list(@RequestBody ParamPage param) {
    Page<User> page = new Page<>(param.getCurrent(), param.getSize());
    // 查询逻辑
    return Result.success(paging);
}

// ID 参数
@PostMapping("/detail")
public Result<User> detail(@RequestBody ParamLongId param) {
    User user = userService.getById(param.getId());
    return Result.success(user);
}
```

### 5. 异常处理

**异常层次结构**：

- `GlobalException`: 全局异常基类
- `ServiceException`: 业务逻辑异常
- `DaoException`: 数据库操作异常
- `ComponentException`: 组件级异常
- `RestException`: REST API 层异常

```java
// 抛出业务异常
throw new ServiceException("用户不存在");

// 带错误码的异常
throw new ServiceException(StatusCode.UNAUTHORIZED, "未授权访问");
```

### 6. 基础类

**实体基类** (`BaseEntity`)：

```java
public class User extends BaseEntity {
    private String name;
    private String email;
    // ...
}
```

**控制器基类** (`BaseController`)：

```java
@RestController
@RequestMapping("/user")
public class UserController extends BaseController {
    // 自动包含日志记录功能
}
```

### 7. 常量管理

**核心常量**：

- `BaseConstants`: 基础常量（顶级 ID、操作类型、状态枚举等）
- `Constants`: 通用常量（成功/失败标记、分页参数等）
- `SecurityConstants`: 安全常量（用户/租户 ID、授权头等）
- `TokenConstants`: Token 常量（认证头、Bearer 前缀等）
- `CacheConstants`: 缓存常量（过期时间、缓存键前缀等）
- `DictConstants`: 字典常量（字典类型、是/否、显示/隐藏等）
- `HttpConstants`: HTTP 常量（字符集、请求类型、状态码等）
- `ServiceConstants`: 服务常量（服务 ID）
- `TenantConstants`: 租户常量（租户字段名、数据源标识等）
- `SqlConstants`: SQL 常量（批量大小、SQL 函数等）

### 8. 类型转换

**Convert 工具类**提供全面的类型转换：

```java
// 基本类型转换
String str = Convert.toStr(123);
Integer num = Convert.toInt("123");
Long longVal = Convert.toLong("123");
Boolean bool = Convert.toBool("true");

// 数组转换
Integer[] intArray = Convert.toIntArray("1,2,3");
Long[] longArray = Convert.toLongArray("1,2,3");

// BigDecimal 转换
BigDecimal decimal = Convert.toBigDecimal("123.45");

// 枚举转换
MyEnum enumVal = Convert.toEnum(MyEnum.class, "VALUE");

// 全角/半角转换
String halfWidth = Convert.toSBC("１２３");
String fullWidth = Convert.toDBC("123");

// 中文数字转换
String chinese = Convert.digitToChinese(123);
```

### 9. 多租户支持

```java
// 租户信息
TenantInfo tenantInfo = new TenantInfo();
tenantInfo.setTenantId(1L);
tenantInfo.setTenantName("租户名称");
tenantInfo.setTenantCode("TENANT001");

// 从用户上下文获取租户 ID
Long tenantId = CurrentUser.getUserContext().getTenantId();
```

### 10. AOP 支持

**AspectAbstract 接口**定义了 AOP 通知方法：

```java
@Aspect
@Component
public class MyAspect implements AspectAbstract {

    @Override
    public void pointcut() {
        // 定义切点
    }

    @Override
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        // 环绕通知
        return joinPoint.proceed();
    }

    @Override
    public void doBefore(JoinPoint joinPoint) {
        // 前置通知
    }

    @Override
    public void doAfter(JoinPoint joinPoint) {
        // 后置通知
    }

    @Override
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        // 返回后通知
    }

    @Override
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        // 异常通知
    }
}
```

### 11. Spring Boot 集成

**BootUtil 工具类**：

```java
// 获取服务器 IP 地址
String ip = BootUtil.getServerIp();

// 获取配置属性
String value = BootUtil.getProperty("app.name");

// 获取激活的 Profile
String profile = BootUtil.getActiveProfile();

// 获取服务器端口
Integer port = BootUtil.getServerPort();

// 获取上下文路径
String contextPath = BootUtil.getContextPath();

// 获取应用主页 URL
String homeUrl = BootUtil.getHomeUrl();
```

### 12. 字典与枚举

**字典枚举接口** (`DictEnum`)：

```java
@Dict
public enum UserStatus implements DictEnum {
    ACTIVE("1", "启用"),
    INACTIVE("0", "禁用");

    private final String code;
    private final String name;

    // 构造函数和 getter
}
```

**应用枚举注解** (`@AppEnum`)：

```java
@AppEnum
public enum OrderStatus implements BaseEnum {
    PENDING(1, "待处理"),
    COMPLETED(2, "已完成");

    private final Integer code;
    private final String desc;

    // 构造函数和 getter
}
```

### 13. 工具类

**ExecutorUtil - 线程池工具**：

```java
// 创建线程池
ExecutorService executor = ExecutorUtil.newExecutor(
    10,     // 核心线程数
    20,     // 最大线程数
    100,    // 队列大小
    "my-pool"  // 线程名前缀
);
```

**SpelUtil - SpEL 表达式解析**：

```java
// 解析 SpEL 表达式
Object result = SpelUtil.parse("#user.name", context);
```

**PhoneUtil - 手机号脱敏**：

```java
// 手机号脱敏（隐藏中间 4 位）
String masked = PhoneUtil.desensitize("13812345678");
// 结果: 138****5678
```

### 14. 扩展接口

**ApplicationExtend 接口**提供应用级扩展：

```java
public interface ApplicationExtend {
    String getToken();                          // 获取当前请求 Token
    Long getUserId();                           // 获取当前用户 ID
    String getUserName();                       // 获取当前用户名
    Dict getDictVo(String code);               // 根据编码获取字典
    Dict getDictById(Long id);                 // 根据 ID 获取字典
    UserInfo getUserById(Long userId);         // 根据 ID 获取用户
    UserContext getUserContext();              // 获取用户上下文
    DepartmentInfo getDepartmentById(Long id); // 根据 ID 获取部门
    RegionInfo getRegionInfo(Long id);         // 获取区域信息
    DepartmentInfo getDepartmentByCode(String code); // 根据编码获取部门
}
```

### 15. 注解

**@LogIgnore**: 排除方法/类的操作日志记录

```java
@LogIgnore
@GetMapping("/internal")
public Result<Void> internalMethod() {
    // 此方法不会被记录操作日志
    return Result.success();
}
```

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-core</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- `spring-boot-starter-aop`: AOP 支持
- `carlos-utils`: 工具模块
- `lombok`: 代码生成
- `swagger-annotations-jakarta`: OpenAPI 文档
- `jackson-annotations`: JSON 序列化
- `guava`: Google 工具库
- `jakarta.validation-api`: 验证注解
- `mybatis-plus-annotation`: MyBatis-Plus 注解
- `spring-context-indexer`: Spring 组件索引

## 设计模式

- **工厂模式**: Result 类的静态工厂方法
- **建造者模式**: 使用 Lombok 构建对象
- **策略模式**: AspectAbstract 接口
- **模板方法**: BaseEntity 和 BaseController
- **ThreadLocal 模式**: CurrentUser 线程安全用户上下文
- **函数式接口**: PageConvert 数据转换

## 使用场景

- 构建标准化 REST API
- 用户认证与授权管理
- 分页查询与数据转换
- 跨层异常处理
- 多租户应用支持
- 类型转换与工具方法
- Spring Boot 集成

## 注意事项

1. **线程安全**: `CurrentUser` 使用 `TransmittableThreadLocal` 确保线程安全，支持线程池场景
2. **异常处理**: 建议在全局异常处理器中统一处理 `GlobalException` 及其子类
3. **分页参数**: `ParamPage` 默认页码从 1 开始，每页大小默认为 10
4. **用户上下文**: 使用完毕后应调用 `CurrentUser.remove()` 清理，避免内存泄漏
5. **常量使用**: 优先使用框架提供的常量类，避免硬编码
6. **类型转换**: `Convert` 工具类在转换失败时会返回 null 或默认值，使用时注意空值判断

## 版本要求

- JDK 21+
- Spring Boot 3.5.8+
- Maven 3.8+

## 相关模块

- `carlos-utils`: 通用工具类
- `carlos-spring-boot-starter-web`: Spring Boot 自动配置
- `carlos-spring-boot-starter-mybatis`: MyBatis-Plus 集成
- `carlos-spring-boot-starter-redis`: Redis 缓存
- `carlos-auth`: OAuth2 认证授权
