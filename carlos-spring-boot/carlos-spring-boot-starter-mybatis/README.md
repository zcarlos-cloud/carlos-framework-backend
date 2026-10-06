# carlos-spring-boot-starter-mybatis

## 模块简介

`carlos-spring-boot-starter-mybatis` 是 Carlos 框架的数据访问层模块，提供了 MyBatis-Plus
集成、多数据源支持、分页查询、数据权限控制、自动填充等功能。该模块基于 MyBatis-Plus 3.5.15 和 Dynamic DataSource 4.3.1 构建，支持
MySQL 和达梦数据库。

## 主要功能

### 1. 基础服务层

**BaseService 接口**提供通用的 CRUD 操作：

```java
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User>
    implements UserService {

    // 继承了所有 MyBatis-Plus 的方法
    public User getById(Long id) {
        return super.getById(id);
    }

    // 使用 Lambda 查询
    public List<User> findByName(String name) {
        return list(queryWrapper().eq(User::getName, name));
    }

    // 使用 Lambda 更新
    public boolean updateStatus(Long id, Integer status) {
        return update(updateWrapper()
            .eq(User::getId, id)
            .set(User::getStatus, status));
    }
}
```

**便捷方法**：

```java
// 查询包装器
LambdaQueryWrapper<User> wrapper = queryWrapper();
wrapper.eq(User::getStatus, 1)
       .like(User::getName, "张")
       .orderByDesc(User::getCreateTime);

// 更新包装器
LambdaUpdateWrapper<User> updateWrapper = updateWrapper();
updateWrapper.eq(User::getId, 1L)
             .set(User::getStatus, 0);

// 分页信息构建器
PageInfo<User> pageInfo = pageInfo(param);
```

### 2. 分页查询

#### 基础分页

```java
@PostMapping("/page")
public Result<PageInfo<User>> page(@RequestBody ParamPage param) {
    PageInfo<User> pageInfo = new PageInfo<>(param);
    userService.page(pageInfo);
    return Result.success(pageInfo);
}
```

#### 带排序的分页

```java
@PostMapping("/page")
public Result<PageInfo<User>> page(@RequestBody ParamPageOrder param) {
    // 创建排序映射
    OrderMapping orderMapping = new OrderMapping(true); // true 表示开启下划线模式

    // 手动映射字段
    orderMapping.mapping("userName", "user_name");
    orderMapping.mapping("createTime", "create_time");

    // 或使用反射自动映射
    orderMapping.mapping("userName", User.class);

    // 创建分页对象
    PageInfo<User> pageInfo = new PageInfo<>(param, orderMapping);

    // 执行查询
    userService.page(pageInfo, queryWrapper().eq(User::getStatus, 1));

    return Result.success(pageInfo);
}
```

#### 前端请求示例

```json
{
  "current": 1,
  "size": 10,
  "sorts": [
    {
      "column": "userName",
      "asc": false
    },
    {
      "column": "createTime",
      "asc": true
    }
  ]
}
```

#### 分页结果转换

```java
// MyBatis-Plus PageInfo 转换为框架 Paging
PageInfo<User> page = userService.page(pageInfo);
Paging<UserVO> result = MybatisPage.convert(page, user -> {
    UserVO vo = new UserVO();
    BeanUtils.copyProperties(user, vo);
    return vo;
});
```

### 3. 自动填充

**自动填充审计字段**：

```java
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    // 插入时自动填充
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    // 插入和更新时自动填充
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // 乐观锁版本号
    @Version
    private Integer version;

    // 逻辑删除标记
    @TableLogic
    private Integer isDeleted;
}
```

**自动填充规则**：

| 字段           | 插入 | 更新 | 填充值     |
|--------------|----|----|---------|
| `createTime` | ✓  | -  | 当前时间    |
| `createBy`   | ✓  | -  | 当前用户 ID |
| `updateTime` | ✓  | ✓  | 当前时间    |
| `updateBy`   | ✓  | ✓  | 当前用户 ID |

**支持的时间类型**：

- `java.util.Date`
- `java.time.LocalDateTime`
- `Long` (时间戳)

**用户 ID 获取**：

- 从 `ApplicationExtend.getUserId()` 获取当前用户 ID
- 支持 `Long`、`String`、`Integer`、`BigInteger` 类型

### 4. ID 生成策略

**雪花算法 ID**：

```java
@Data
@TableName("sys_user")
public class User {
    // 使用雪花算法生成 Long 类型 ID
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
}
```

**日期 + 雪花算法 ID**：

```java
@Data
@TableName("sys_order")
public class Order {
    // 使用日期 + 雪花算法生成 32 位字符串 ID
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;  // 格式: 202601251430123456789012345678
}
```

### 5. 多数据源支持

#### 配置多数据源

```yaml
spring:
  datasource:
    dynamic:
      primary: master  # 默认数据源
      strict: false    # 严格模式
      datasource:
        # 主数据源
        master:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db_master?useUnicode=true&characterEncoding=utf-8&useSSL=false
          username: root
          password: password
          druid:
            initial-size: 5
            min-idle: 5
            max-active: 20
            max-wait: 60000

        # 从数据源
        slave:
          driver-class-name: com.mysql.cj.jdbc.Driver
          url: jdbc:mysql://localhost:3306/db_slave?useUnicode=true&characterEncoding=utf-8&useSSL=false
          username: root
          password: password

        # 达梦数据库
        dm:
          driver-class-name: dm.jdbc.driver.DmDriver
          url: jdbc:dm://localhost:5236/SYSDBA
          username: SYSDBA
          password: SYSDBA001
```

#### 使用数据源

**方法级别切换**：

```java
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> {

    // 使用主数据源（默认）
    public User getById(Long id) {
        return super.getById(id);
    }

    // 切换到从数据源
    @DS("slave")
    public List<User> listFromSlave() {
        return list();
    }

    // 切换到达梦数据库
    @DS("dm")
    public List<User> listFromDM() {
        return list();
    }
}
```

**类级别切换**：

```java
@Service
@DS("slave")  // 整个类使用从数据源
public class ReportServiceImpl extends BaseServiceImpl<ReportMapper, Report> {

    // 使用从数据源
    public List<Report> list() {
        return super.list();
    }

    // 临时切换到主数据源
    @DS("master")
    public boolean save(Report report) {
        return super.save(report);
    }
}
```

### 6. 数据权限控制

**集成 carlos-spring-boot-starter-datascope 模块**实现行级数据权限：

```java
@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User> {

    // 自动应用数据权限过滤
    public List<User> list() {
        // SQL 会自动添加 WHERE dept_id IN (1, 2, 3)
        return super.list();
    }
}
```

**工作原理**：

1. `CarlosDataPermissionHandler` 拦截 SQL 执行
2. 从 `DataScopeHandler` 获取当前用户的数据权限范围
3. 自动修改 SQL 的 WHERE 子句，添加权限过滤条件
4. 支持单值（=）和多值（IN）条件
5. 无权限时返回 `1 = 0` 条件，查询结果为空

**权限条件示例**：

```sql
-- 原始 SQL
SELECT * FROM sys_user WHERE status = 1

-- 添加数据权限后
SELECT * FROM sys_user WHERE status = 1 AND dept_id IN (1, 2, 3)

-- 无权限时
SELECT * FROM sys_user WHERE status = 1 AND 1 = 0
```

### 7. 防止全表操作

**BlockAttackInnerInterceptor** 防止误操作：

```java
// 以下操作会抛出异常
userService.remove(new LambdaQueryWrapper<>());  // 全表删除
userService.update(updateWrapper());              // 全表更新

// 正确做法：添加条件
userService.remove(queryWrapper().eq(User::getStatus, 0));
userService.update(updateWrapper().eq(User::getId, 1L).set(User::getStatus, 1));
```

### 8. 乐观锁

**使用 @Version 注解**：

```java
@Data
@TableName("sys_user")
public class User {
    @TableId
    private Long id;

    private String name;

    @Version
    private Integer version;
}

// 更新操作
User user = userService.getById(1L);  // version = 1
user.setName("新名称");
userService.updateById(user);         // version = 2

// SQL: UPDATE sys_user SET name = ?, version = 2
//      WHERE id = ? AND version = 1
```

### 9. 逻辑删除

**使用 @TableLogic 注解**：

```java
@Data
@TableName("sys_user")
public class User {
    @TableId
    private Long id;

    private String name;

    @TableLogic
    private Integer isDeleted;  // 0-未删除, 1-已删除
}

// 删除操作（实际是更新）
userService.removeById(1L);
// SQL: UPDATE sys_user SET is_deleted = 1 WHERE id = 1

// 查询操作（自动过滤已删除）
userService.list();
// SQL: SELECT * FROM sys_user WHERE is_deleted = 0
```

### 10. 多表关联查询

**使用 MyBatis-Plus Join**：

```java
@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
}

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> {

    public List<UserVO> listWithDept() {
        return baseMapper.selectJoinList(UserVO.class,
            new MPJLambdaWrapper<User>()
                .selectAll(User.class)
                .select(Dept::getName, UserVO::getDeptName)
                .leftJoin(Dept.class, Dept::getId, User::getDeptId)
                .eq(User::getStatus, 1)
        );
    }
}
```

### 11. 属性与列名映射

**PropertyColumnUtil** 工具类：

```java
// 获取实体类的属性-列名映射
Map<String, String> mapping = PropertyColumnUtil.getPropertyColumnMap(User.class);
// 结果: {id=id, name=name, createTime=create_time, ...}

// 获取单个属性的列名
String column = PropertyColumnUtil.getColumn(User.class, "createTime");
// 结果: create_time
```

### 12. 自定义类型处理器

**SerializableTypeHandler** 处理 Serializable 对象：

```java
@Data
@TableName("sys_config")
public class Config {
    @TableId
    private Long id;

    // 使用自定义类型处理器
    @TableField(typeHandler = SerializableTypeHandler.class)
    private Map<String, Object> settings;
}
```

## 配置说明

### MyBatis-Plus 配置

```yaml
mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml

  # 实体类包路径
  type-aliases-package: com.carlos.*.entity

  # 全局配置
  global-config:
    # 数据库配置
    db-config:
      # 主键类型（雪花算法）
      id-type: ASSIGN_ID
      # 逻辑删除字段
      logic-delete-field: isDeleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      # 表名前缀
      table-prefix: sys_

  # 配置
  configuration:
    # 驼峰转下划线
    map-underscore-to-camel-case: true
    # 缓存
    cache-enabled: false
    # 日志
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

### Druid 连接池配置

```yaml
spring:
  datasource:
    druid:
      # 初始连接数
      initial-size: 5
      # 最小空闲连接数
      min-idle: 5
      # 最大活跃连接数
      max-active: 20
      # 获取连接等待超时时间
      max-wait: 60000
      # 配置间隔多久进行一次检测，检测需要关闭的空闲连接
      time-between-eviction-runs-millis: 60000
      # 配置连接在池中最小生存时间
      min-evictable-idle-time-millis: 300000
      # 验证连接有效性的 SQL
      validation-query: SELECT 1
      # 申请连接时执行 validationQuery 检测连接是否有效
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      # 打开 PSCache
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      # 配置监控统计拦截的 filters
      filters: stat,wall,slf4j
      # 通过 connectProperties 属性打开 mergeSql 功能；慢 SQL 记录
      connection-properties: druid.stat.mergeSql=true;druid.stat.slowSqlMillis=5000
      # 配置 DruidStatFilter
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
      # 配置 DruidStatViewServlet
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        reset-enable: false
        login-username: admin
        login-password: admin
```

## 依赖引入

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-mybatis</artifactId>
    <version>${carlos.version}</version>
</dependency>
```

## 依赖项

- **MyBatis-Plus**: 3.5.15 (Spring Boot 3 兼容)
- **MyBatis-Plus Join**: 1.5.4 (多表关联查询)
- **Dynamic DataSource**: 4.3.1 (多数据源支持)
- **Druid**: 1.2.27 (连接池和监控)
- **MySQL Driver**: 8.0.33
- **Dameng Driver**: 8.1.3.62 (达梦数据库)
- **carlos-spring-boot-core**: 核心基础模块
- **carlos-spring-boot-starter-datascope**: 数据权限模块（可选）
- **carlos-snowflake**: 雪花算法 ID 生成

## 使用示例

### 完整示例

```java
// 1. 定义实体
@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String email;
    private Integer status;
    private Long deptId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Version
    private Integer version;

    @TableLogic
    private Integer isDeleted;
}

// 2. 定义 Mapper
@Mapper
public interface UserMapper extends BaseMapper<User> {
}

// 3. 定义 Service
public interface UserService extends BaseService<User> {
    PageInfo<User> pageQuery(ParamPageOrder param);
}

@Service
public class UserServiceImpl extends BaseServiceImpl<UserMapper, User>
    implements UserService {

    @Override
    public PageInfo<User> pageQuery(ParamPageOrder param) {
        OrderMapping orderMapping = new OrderMapping(true);
        orderMapping.mapping("name", User.class);
        orderMapping.mapping("createTime", User.class);

        PageInfo<User> pageInfo = new PageInfo<>(param, orderMapping);

        return this.page(pageInfo, queryWrapper()
            .eq(User::getStatus, 1)
            .orderByDesc(User::getCreateTime));
    }
}

// 4. 定义 Controller
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public Result<User> create(@RequestBody User user) {
        userService.save(user);
        return Result.success(user);
    }

    @GetMapping("/{id}")
    public Result<User> getById(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PutMapping
    public Result<Void> update(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }

    @PostMapping("/page")
    public Result<PageInfo<User>> page(@RequestBody ParamPageOrder param) {
        return Result.success(userService.pageQuery(param));
    }
}
```

## 注意事项

1. **自动填充**: 需要实现 `ApplicationExtend` 接口提供用户 ID
2. **数据权限**: 需要引入 `carlos-spring-boot-starter-datascope` 模块并配置数据权限规则
3. **多数据源**: 使用 `@DS` 注解时，事务需要特别注意
4. **乐观锁**: 更新时必须先查询获取最新版本号
5. **逻辑删除**: 查询时会自动过滤已删除数据，如需查询所有数据需要特殊处理
6. **防止全表操作**: 更新和删除必须带条件，否则会抛出异常
7. **分页排序**: 前端传递的字段名需要与实体类属性名一致
8. **连接池监控**: Druid 监控页面访问 `/druid/index.html`

## Spring Boot 3.x 兼容性

本模块基于 Spring Boot 3.x 和 MyBatis-Plus 3.5.x 构建，主要变更：

### MyBatis-Plus Spring Boot 3 Starter

使用 `mybatis-plus-spring-boot3-starter` 替代旧版本：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
```

### 分页插件数据库类型

从 Spring Boot 3.x 版本开始，`PaginationInnerInterceptor` 需要显式指定数据库类型：

```java
// 会自动配置，无需手动干预
interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
```

支持的 `DbType` 值：

- `DbType.MYSQL` - MySQL
- `DbType.POSTGRE_SQL` - PostgreSQL
- `DbType.ORACLE` - Oracle
- `DbType.DM` - 达梦数据库
- `DbType.SQL_SERVER` - SQL Server
- 其他...

### JSqlParser 兼容性

使用 JSqlParser 4.x 版本，注意 `ExpressionList` API 变化：

```java
// Spring Boot 3.x / JSqlParser 4.x 推荐用法
ExpressionList itemsList = new ExpressionList<>();
itemsList.setExpressions(expressions);
```

## 版本要求

- JDK 17+
- Spring Boot 3.5.9+
- Maven 3.8+
- MySQL 8.0+ 或 达梦 8.1+

## 相关模块

- `carlos-spring-boot-core`: 核心基础模块
- `carlos-spring-boot-starter-datascope`: 数据权限模块
- `carlos-snowflake`: 雪花算法 ID 生成
- `carlos-utils`: 工具模块
