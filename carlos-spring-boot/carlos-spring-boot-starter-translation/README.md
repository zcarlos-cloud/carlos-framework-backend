# Carlos Translation Starter

Carlos 字段翻译模块 - 提供用户、部门、字典、区域、枚举等字段的自动翻译功能。

## 功能特性

- **注解驱动**：通过简单的注解即可实现字段自动翻译
- **批量处理**：支持集合数据的批量翻译，减少数据库查询次数
- **多级缓存**：内置 Caffeine + Redis 两级缓存，提升性能
- **递归翻译**：支持嵌套对象的递归翻译处理
- **灵活配置**：支持源码字段、目标字段的自定义映射

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.carlos</groupId>
    <artifactId>carlos-spring-boot-starter-translation</artifactId>
</dependency>
```

### 2. 启用翻译功能

在 `application.yml` 中配置：

```yaml
carlos:
  translation:
    enabled: true
    cache-enabled: true
    cache:
      local-size: 10000          # 本地缓存最大条目数
      local-expire-minutes: 10   # 本地缓存过期时间（分钟）
      redis-expire-minutes: 30   # Redis缓存过期时间（分钟）
```

### 3. 实现 ApplicationExtend 接口

确保你的 `ApplicationExtend` 实现类实现了批量查询方法：

```java
@Component
public class MyApplicationExtend implements ApplicationExtend, ApplicationExtendBatch {
    
    @Override
    public Map<Serializable, UserInfo> getUserByIds(Set<Serializable> ids) {
        // 批量查询用户信息
    }
    
    @Override
    public Map<String, Dict> getDictVos(String type, Set<String> codes) {
        // 批量查询字典信息
    }
    
    @Override
    public Map<Serializable, DepartmentInfo> getDepartmentByIds(Set<Serializable> ids) {
        // 批量查询部门信息
    }
    
    @Override
    public Map<String, RegionInfo> getRegionByCodes(Set<String> codes) {
        // 批量查询区域信息
    }
    
    // ... 其他原有方法
}
```

### 4. 在 VO 中使用翻译注解

```java
@Data
public class UserVO {
    private Long id;
    
    @TransUser(source = "createBy", target = "createByName")
    private Long createBy;
    private String createByName;
    
    @TransDept(source = "deptId", type = TransDept.OutputType.FULLNAME)
    private Long deptId;
    private String deptName;
    
    @TransDict(type = "user_status", target = "statusName")
    private String status;
    private String statusName;
    
    @TransRegion(source = "regionCode", type = TransRegion.OutputType.FULLNAME, limit = 2)
    private String regionCode;
    private String regionName;
    
    @TransEnum(enumClass = UserTypeEnum.class, target = "typeName")
    private Integer type;
    private String typeName;
}
```

### 5. 在 Controller 中启用翻译

```java
@RestController
@Translated  // 类级别启用翻译
public class UserController {
    
    @GetMapping("/users/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        return Result.success(userService.getUser(id));
    }
    
    @GetMapping("/users")
    @Translated  // 方法级别启用翻译
    public ApiResponse<List<UserVO>> listUsers() {
        return Result.success(userService.listUsers());
    }
}
```

## 注解详解

### @Translated

标记在类或方法上，启用字段翻译功能。支持方法级配置覆盖类级配置。

```java
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Translated {
    boolean cacheEnabled() default true;   // 是否启用缓存
    long cacheMinutes() default 30;        // 缓存时间（分钟）
}
```

**注解属性说明：**

| 属性             | 类型      | 默认值  | 说明                                    |
|----------------|---------|------|---------------------------------------|
| `cacheEnabled` | boolean | true | 是否启用缓存。设置为 false 时直接查询数据库，不读取/写入缓存    |
| `cacheMinutes` | long    | 30   | 缓存过期时间（分钟）。仅当 `cacheEnabled=true` 时生效 |

**优先级规则：**

1. 方法级别注解 > 类级别注解
2. 注解配置 > 全局配置 (`application.yml`)

**使用示例：**

```java
@RestController
@Translated(cacheEnabled = true, cacheMinutes = 60)  // 类级别默认配置
public class UserController {
    
    @GetMapping("/users/{id}")
    public ApiResponse<UserVO> getUser(@PathVariable Long id) {
        // 使用类级别的缓存配置（启用缓存，60分钟）
        return Result.success(userService.getUser(id));
    }
    
    @GetMapping("/users/realtime")
    @Translated(cacheEnabled = false)  // 方法级别覆盖：禁用缓存
    public ApiResponse<List<UserVO>> listRealtimeUsers() {
        // 实时查询，不走缓存
        return Result.success(userService.listRealtimeUsers());
    }
    
    @GetMapping("/users/hot")
    @Translated(cacheEnabled = true, cacheMinutes = 120)  // 方法级别覆盖：120分钟缓存
    public ApiResponse<List<UserVO>> listHotUsers() {
        // 热点数据，使用更长的缓存时间
        return Result.success(userService.listHotUsers());
    }
}
```

### @TransUser

翻译用户ID字段为用户名或完整用户信息。

```java
public @interface TransUser {
    String source() default "";        // 用户ID来源字段（默认当前字段）
    Type type() default Type.NAME;     // 输出类型：FULL/NAME/NICKNAME/REALNAME/PHONE/EMAIL
    String target() default "";        // 输出到指定字段（为空则替换原值）
}
```

### @TransDept

翻译部门ID字段为部门名称或完整路径。

```java
public @interface TransDept {
    String source() default "";                   // 部门ID来源字段
    OutputType type() default OutputType.NAME;    // 输出类型：FULL/NAME/FULLNAME
    String target() default "";                   // 输出到指定字段
    String separator() default "/";               // 层级分隔符（用于 FULLNAME）
    int limit() default Integer.MAX_VALUE;        // 向上层级限制
}
```

### @TransDict

翻译字典编码为字典名称或完整信息。

```java
public @interface TransDict {
    String type();                                 // 字典类型（必填）
    InputType inputType() default InputType.CODE;  // 输入类型：CODE/ID
    OutputType outputType() default OutputType.NAME; // 输出类型：FULL/NAME/CODE
    String target() default "";                    // 输出到指定字段
}
```

### @TransRegion

翻译区域编码为区域名称或完整路径。

```java
public @interface TransRegion {
    String source() default "";                    // 区域编码来源字段
    OutputType type() default OutputType.FULLNAME; // 输出类型：FULL/NAME/FULLNAME
    String target() default "";                    // 输出到指定字段
    String separator() default "/";                // 层级分隔符
    int limit() default 3;                         // 向上层级限制
}
```

### @TransEnum

翻译枚举编码为描述或完整信息。

```java
public @interface TransEnum {
    Class<? extends BaseEnum> enumClass();         // 枚举类（必填）
    OutputType type() default OutputType.DESC;     // 输出类型：FULL/CODE/DESC
    String target() default "";                    // 输出到指定字段
}
```

### @TransNested

递归处理嵌套对象的翻译注解。

```java
public @interface TransNested {
    boolean recursive() default true;    // 是否递归翻译嵌套对象
    int maxDepth() default 3;            // 最大递归深度
}
```

## 使用示例

### 示例 1：基本用户翻译

```java
@Data
public class OrderVO {
    private Long id;
    
    // 将 createBy（用户ID）翻译为 createByName（用户名）
    @TransUser(source = "createBy", target = "createByName")
    private Long createBy;
    private String createByName;
}
```

### 示例 2：部门完整路径

```java
@Data
public class EmployeeVO {
    private Long id;
    
    // 将 deptId 翻译为 "总公司/技术部/研发组" 格式
    @TransDept(source = "deptId", type = TransDept.OutputType.FULLNAME, 
               separator = "-", limit = 3)
    private Long deptId;
    private String deptPath;
}
```

### 示例 3：字典翻译

```java
@Data
public class TaskVO {
    private Long id;
    
    // 翻译状态字典（type="task_status"）
    @TransDict(type = "task_status", target = "statusText")
    private String status;
    private String statusText;
}
```

### 示例 4：嵌套对象翻译

```java
@Data
public class ProjectVO {
    private Long id;
    
    @TransUser(source = "managerId", target = "managerName")
    private Long managerId;
    private String managerName;
    
    // 递归翻译成员列表
    @TransNested
    private List<MemberVO> members;
}

@Data
public class MemberVO {
    @TransUser(source = "userId", target = "userName")
    private Long userId;
    private String userName;
    
    @TransDept(source = "deptId", type = TransDept.OutputType.NAME)
    private Long deptId;
    private String deptName;
}
```

### 示例 5：枚举翻译

```java
// 定义枚举
@AppEnum(name = "用户类型", code = "user_type")
public enum UserTypeEnum implements BaseEnum {
    ADMIN(1, "管理员"),
    USER(2, "普通用户");
    
    private final Integer code;
    private final String desc;
    // ...
}

// 在VO中使用
@Data
public class UserVO {
    @TransEnum(enumClass = UserTypeEnum.class, type = TransEnum.OutputType.DESC,
               target = "typeName")
    private Integer type;
    private String typeName;
}
```

## 缓存说明

### 缓存架构

```
┌─────────────────────────────────────────────────────┐
│                    翻译缓存架构                      │
├─────────────────────────────────────────────────────┤
│  L1: Caffeine 本地缓存                               │
│     - 最大条目数：10000（可配置）                     │
│     - 过期时间：10分钟（可配置）                      │
│     - 作用：减少同 JVM 内的重复查询                   │
├─────────────────────────────────────────────────────┤
│  L2: Redis 分布式缓存                                │
│     - 过期时间：30分钟（可配置）                      │
│     - 作用：集群环境下共享缓存数据                    │
└─────────────────────────────────────────────────────┘
```

### 缓存 Key 格式

| 数据类型 | Key 格式                              | 示例                             |
|------|-------------------------------------|--------------------------------|
| 用户   | `framework:translation:user:{id}`          | `framework:translation:user:1001`     |
| 字典   | `framework:translation:dict:{type}:{code}` | `framework:translation:dict:status:1` |
| 部门   | `framework:translation:dept:{id}`          | `framework:translation:dept:2001`     |
| 区域   | `framework:translation:region:{code}`      | `framework:translation:region:440300` |

## 性能优化建议

1. **批量查询优先**：translation 会自动收集同类型数据批量查询，建议在 `ApplicationExtend` 中实现批量查询接口

2. **合理使用缓存**：对于不经常变动的数据（如字典、区域），可以适当延长缓存时间

3. **避免过度嵌套**：嵌套对象翻译会增加递归开销，建议控制嵌套深度

4. **及时清理缓存**：数据变更时，可通过 `TranslationCacheManager` 清理相关缓存：

```java
@Autowired
private TranslationCacheManager cacheManager;

// 使单个缓存失效
cacheManager.evict(CacheKeys.userKey(userId));

// 批量使缓存失效
cacheManager.batchEvict(keys);

// 清空本地缓存
cacheManager.clearLocal();
```

## 高级用法

### 编程式控制翻译上下文

除了在注解中配置缓存参数外，还可以通过 `TranslationContext` 编程式地控制翻译行为。

```java
import com.carlos.boot.translation.core.TranslationContext;
import com.carlos.boot.translation.service.TranslationService;

@Service
public class UserService {
    
    @Autowired
    private TranslationService translationService;
    
    public UserVO getUserWithCache(Long id) {
        UserVO user = userMapper.selectById(id);
        
        // 使用默认配置进行翻译（启用缓存，30分钟）
        translationService.translate(user);
        
        return user;
    }
    
    public UserVO getUserWithoutCache(Long id) {
        UserVO user = userMapper.selectById(id);
        
        // 禁用缓存进行翻译
        TranslationContext.disableCache();
        try {
            translationService.translate(user);
        } finally {
            TranslationContext.clear();
        }
        
        return user;
    }
    
    public UserVO getUserWithCustomCache(Long id) {
        UserVO user = userMapper.selectById(id);
        
        // 使用自定义缓存配置（启用缓存，120分钟）
        TranslationContext.setCacheConfig(true, 120);
        try {
            translationService.translate(user);
        } finally {
            TranslationContext.clear();
        }
        
        return user;
    }
}
```

### 手动控制缓存失效

```java
@Autowired
private TranslationCacheManager cacheManager;

// 使单个用户缓存失效
cacheManager.evict(CacheKeys.userKey(userId));

// 使多个缓存同时失效
List<String> keys = Arrays.asList(
    CacheKeys.userKey(1L),
    CacheKeys.userKey(2L),
    CacheKeys.deptKey(10L)
);
cacheManager.batchEvict(keys);

// 清空所有本地缓存（不影响 Redis）
cacheManager.clearLocal();
```

## 配置属性

| 属性                                              | 默认值     | 说明              |
|-------------------------------------------------|---------|-----------------|
| `carlos.translation.enabled`                    | `true`  | 是否启用翻译功能        |
| `carlos.translation.cache-enabled`              | `true`  | 是否启用缓存          |
| `carlos.translation.cache.local-size`           | `10000` | 本地缓存最大条目数       |
| `carlos.translation.cache.local-expire-minutes` | `10`    | 本地缓存过期时间（分钟）    |
| `carlos.translation.cache.redis-expire-minutes` | `30`    | Redis缓存过期时间（分钟） |

## 注意事项

1. **必须实现 ApplicationExtend**：翻译功能依赖 `ApplicationExtend` 接口获取基础数据

2. **字段可见性**：被翻译的字段不能是 `static` 或 `final` 的

3. **循环引用**：避免在 VO 中配置循环引用，可能导致翻译死循环

4. **线程安全**：`TranslationMetadata` 使用 `ConcurrentHashMap` 缓存类元数据，线程安全

## 版本历史

| 版本    | 日期         | 变更                                            |
|-------|------------|-----------------------------------------------|
| 3.0.0 | 2025-01-20 | 初始版本，从 carlos-spring-boot-starter-web 分离为独立模块 |
