# carlos-spring-boot-starter-datascope

## 模块简介

`carlos-spring-boot-starter-datascope` 是 Carlos
框架的数据权限控制模块，提供了基于注解和AOP的多维度数据权限控制解决方案。该模块支持多种数据权限类型，可用于多租户系统、部门数据隔离、个人数据权限等场景。

## 主要功能

### 1. 数据权限注解支持

提供 `@DataScope` 和 `@DataScopes` 注解，支持在方法级别声明数据权限规则：

```java
@DataScope(
    field = "dept_id",           // 权限控制字段
    methodPoint = "selectPage",  // MyBatis Mapper方法名
    type = DataScopeType.DEPT_AND_SUB,  // 数据权限类型
    caller = UserMapper.class    // 调用的Mapper类
)
public Page<User> getUserList(PageParam param) {
    return userMapper.selectPage(param);
}
```

### 2. 多种数据权限类型

支持6种内置数据权限类型和自定义权限：

| 权限类型       | 枚举值            | 说明          |
|------------|----------------|-------------|
| 无数据权限      | `NONE`         | 不进行数据过滤     |
| 全部数据权限     | `ALL`          | 查看所有数据      |
| 本部门数据权限    | `CURRENT_DEPT` | 只能查看本部门数据   |
| 本部门及以下数据权限 | `DEPT_AND_SUB` | 查看本部门及子部门数据 |
| 本岗位数据权限    | `CURRENT_ROLE` | 只能查看本岗位相关数据 |
| 仅本人数据权限    | `CURRENT_USER` | 只能查看自己创建的数据 |
| 自定义数据权限    | `CUSTOM`       | 自定义权限处理器    |

### 3. AOP自动拦截

基于Spring AOP实现，自动拦截带有 `@DataScope` 注解的方法，在方法执行前后进行数据权限处理：

```java
@Aspect
public class DataScopeAspect implements AspectAbstract {

    @Pointcut("@annotation(DataScope) || @annotation(DataScopes)")
    public void pointcut() {}

    @Before("pointcut()")
    public void doBefore(JoinPoint joinPoint) {
        // 数据权限预处理
        handler.handle(scopes);
        handler.handleParam(joinPoint);
    }

    @After("pointcut()")
    public void doAfter(JoinPoint joinPoint) {
        // 清理权限上下文
        handler.exit();
    }
}
```

### 4. 自定义权限处理器

支持通过实现 `CustomScope` 接口定义自定义数据权限逻辑：

```java
public class CustomDeptScope implements CustomScope {

    @Override
    public void handle(DataScopeInfo scopeInfo) {
        // 获取当前用户信息
        UserContext userContext = CurrentUser.getUserContext();
        Long deptId = userContext.getDeptId();

        // 构建SQL条件
        String sqlCondition = String.format("dept_id = %d", deptId);
        scopeInfo.setCondition(sqlCondition);
    }
}
```

### 5. 多注解支持

支持在同一方法上使用多个 `@DataScope` 注解，实现复杂的数据权限组合：

```java
@DataScopes({
    @DataScope(field = "dept_id", type = DataScopeType.DEPT_AND_SUB, caller = UserMapper.class),
    @DataScope(field = "create_by", type = DataScopeType.CURRENT_USER, caller = UserMapper.class)
})
public List<User> getComplexData() {
    return userMapper.selectList();
}
```

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-datascope</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

### 2. 配置启用

在 `application.yml` 中启用数据权限模块：

```yaml
carlos:
  datascope:
    enabled: true  # 启用数据权限功能
```

### 3. 基本使用示例

#### Controller层使用：

```java
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    @DataScope(
        field = "dept_id",
        type = DataScopeType.DEPT_AND_SUB,
        caller = UserMapper.class
    )
    public Result<Page<User>> getUserList(PageParam param) {
        Page<User> page = userService.getUserPage(param);
        return Result.success(page);
    }

    @GetMapping("/my-data")
    @DataScope(
        field = "create_by",
        type = DataScopeType.CURRENT_USER,
        caller = UserMapper.class
    )
    public Result<List<User>> getMyData() {
        List<User> myData = userService.getMyData();
        return Result.success(myData);
    }
}
```

#### Service层使用：

```java
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @DataScope(field = "dept_id", type = DataScopeType.CURRENT_DEPT, caller = UserMapper.class)
    public Page<User> getUserPage(PageParam param) {
        return userMapper.selectPage(param);
    }
}
```

#### Mapper层配置：

```xml
<!-- UserMapper.xml -->
<select id="selectPage" resultType="com.carlos.entity.User">
    SELECT * FROM sys_user
    <where>
        ${dataScope}  <!-- 数据权限条件会自动注入到这里 -->
        <if test="param.keyword != null and param.keyword != ''">
            AND (username LIKE CONCAT('%', #{param.keyword}, '%')
                 OR nick_name LIKE CONCAT('%', #{param.keyword}, '%'))
        </if>
    </where>
    ORDER BY create_time DESC
</select>
```

## 详细功能说明

### 1. 权限控制字段配置

`@DataScope` 注解的 `field` 属性用于指定数据库表中用于权限控制的字段：

```java
// 使用部门ID字段进行权限控制
@DataScope(field = "dept_id", type = DataScopeType.DEPT_AND_SUB, caller = UserMapper.class)

// 使用创建人字段进行权限控制
@DataScope(field = "create_by", type = DataScopeType.CURRENT_USER, caller = UserMapper.class)

// 使用租户ID字段进行权限控制
@DataScope(field = "tenant_id", type = DataScopeType.ALL, caller = UserMapper.class)
```

### 2. 自定义权限处理器

当内置权限类型不满足需求时，可以使用自定义权限处理器：

```java
// 1. 实现 CustomScope 接口
@Component
public class CustomRoleScope implements CustomScope {

    @Override
    public void handle(DataScopeInfo scopeInfo) {
        UserContext userContext = CurrentUser.getUserContext();
        List<Long> roleIds = userContext.getRoleIds();

        if (roleIds.isEmpty()) {
            scopeInfo.setCondition("1 = 0");  // 无权限
        } else {
            String roleIdsStr = roleIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
            scopeInfo.setCondition("role_id IN (" + roleIdsStr + ")");
        }
    }
}

// 2. 在注解中指定自定义处理器
@DataScope(
    field = "role_id",
    type = DataScopeType.CUSTOM,
    handler = CustomRoleScope.class,
    caller = UserMapper.class
)
public List<User> getRoleData() {
    return userMapper.selectList();
}
```

### 3. 权限处理器链

模块内置了默认的权限处理器链，按以下顺序处理数据权限：

1. **权限类型解析**：根据 `DataScopeType` 确定权限范围
2. **用户上下文获取**：从 `CurrentUser` 获取当前用户信息
3. **SQL条件构建**：根据权限类型生成对应的SQL条件
4. **参数处理**：处理方法的参数，注入权限条件
5. **上下文清理**：方法执行完成后清理权限上下文

### 4. 与MyBatis-Plus集成

本模块与 `carlos-spring-boot-starter-mybatis` 模块深度集成，支持MyBatis-Plus的查询条件自动注入：

```java
// 在MyBatis-Plus的Service中使用
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> {

    @Override
    @DataScope(field = "dept_id", type = DataScopeType.DEPT_AND_SUB, caller = UserMapper.class)
    public Page<User> page(Page<User> page, Wrapper<User> queryWrapper) {
        return super.page(page, queryWrapper);
    }
}
```

## 配置项说明

| 配置项                        | 类型      | 默认值     | 说明         |
|----------------------------|---------|---------|------------|
| `carlos.datascope.enabled` | boolean | `false` | 是否启用数据权限模块 |

## 依赖项

- `carlos-spring-boot-core`：用户上下文管理、基础工具类
- `spring-boot-starter-aop`：AOP支持
- `spring-boot-starter`：Spring Boot基础依赖

## 注意事项

### 1. 性能考虑

- 数据权限会增加SQL查询的复杂度，建议在需要权限控制的表上建立合适的索引
- 复杂的权限条件可能影响查询性能，建议定期优化权限逻辑

### 2. 使用建议

- 在Controller层或Service层添加 `@DataScope` 注解，不要在Mapper层添加
- 为不同的业务场景设计合适的权限类型，避免过度使用自定义权限
- 在多租户系统中，建议结合 `tenant_id` 字段使用数据权限

### 3. 常见问题

**Q: 数据权限不生效怎么办？**
A: 检查以下事项：

1. 确认 `carlos.datascope.enabled=true`
2. 确认注解的 `caller` 属性正确指向Mapper类
3. 确认Mapper XML中存在 `${dataScope}` 占位符
4. 确认当前用户上下文已正确设置

**Q: 如何同时使用多个数据权限？**
A: 使用 `@DataScopes` 注解包裹多个 `@DataScope` 注解，权限条件会以AND关系组合

**Q: 如何在非Web环境使用数据权限？**
A: 需要手动设置用户上下文：

```java
// 设置用户上下文
UserContext userContext = new UserContext();
userContext.setUserId(1L);
userContext.setDeptId(100L);
CurrentUser.setUserContext(userContext);

// 执行业务逻辑
// ...

// 清理上下文
CurrentUser.remove();
```

### 4. 与carlos-spring-boot-core集成

本模块深度依赖 `carlos-spring-boot-core` 模块的 `CurrentUser` 工具类获取用户上下文信息，确保在使用前正确设置用户登录信息。

## 版本要求

- JDK 17+
- Spring Boot 3.5.8+
- MyBatis-Plus 3.5.12+

## 相关模块

- **carlos-spring-boot-core**：用户上下文管理、基础工具类
- **carlos-spring-boot-starter-mybatis**：MyBatis-Plus集成、数据访问层支持
- **carlos-auth**：OAuth2认证授权、用户信息管理
