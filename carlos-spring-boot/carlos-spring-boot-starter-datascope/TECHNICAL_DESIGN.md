# Carlos 数据权限模块 技术设计文档 V2.0

> **版本**: 2.0.0  
> **状态**: 设计阶段  
> **作者**: AI Assistant  
> **日期**: 2026-03-17

---

## 1. 设计目标

### 1.1 核心目标

1. **简化使用** - 减少注解配置参数，提供约定优于配置的体验
2. **高性能** - 引入多级缓存，减少重复查询
3. **多功能** - 支持行权限、列权限(脱敏)、数据范围权限
4. **动态规则** - 支持从数据库/配置中心加载权限规则
5. **可审计** - 完整的数据权限访问日志
6. **可扩展** - 插件化架构，支持自定义规则引擎

### 1.2 设计原则

| 原则   | 说明                         |
|------|----------------------------|
| 无侵入性 | 通过AOP和MyBatis拦截器实现，业务代码零侵入 |
| 性能优先 | 多级缓存 + 异步加载 + 规则预编译        |
| 灵活配置 | 注解 + YAML + 数据库 三种配置方式     |
| 开箱即用 | 提供合理的默认配置和通用规则模板           |

---

## 2. 核心架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           数据权限控制层 (API层)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │  @DataScope  │  │@DataScopes   │  │@DataMasking  │  │@DataFilter   │    │
│  │   (行权限)    │  │  (组合权限)   │  │  (列脱敏)     │  │  (动态规则)   │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│                          数据权限处理层 (Core层)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                      DataScopeAspect (AOP拦截)                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    DataScopeRuleEngine (规则引擎)                     │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │   │
│  │  │ RuleParser   │→ │RuleCompiler  │→ │RuleExecutor  │              │   │
│  │  │  (规则解析)   │  │  (规则编译)   │  │  (规则执行)   │              │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                    DataScopeContextManager (上下文管理)                │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐              │   │
│  │  │ThreadLocal   │  │ Rule Stack   │  │  Cache L1    │              │   │
│  │  │  (线程隔离)   │  │  (规则栈)     │  │  (方法级缓存) │              │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────────────┤
│                          数据权限策略层 (Strategy层)                          │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │RowScope      │  │ColumnScope   │  │RangeScope    │  │CustomScope   │    │
│  │Strategy      │  │Strategy      │  │Strategy      │  │Strategy      │    │
│  │  (行权限策略) │  │ (列权限策略)  │  │ (范围策略)    │  │ (自定义策略)  │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│                          数据提供层 (Provider层)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐    │
│  │UserProvider  │  │DeptProvider  │  │RoleProvider  │  │RegionProvider│    │
│  │ (用户数据)    │  │ (部门数据)    │  │ (角色数据)    │  │ (区域数据)    │    │
│  └──────────────┘  └──────────────┘  └──────────────┘  └──────────────┘    │
├─────────────────────────────────────────────────────────────────────────────┤
│                          缓存层 (Cache层)                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │  Caffeine    │  │   Redis      │  │  Local Map   │                      │
│  │ (JVM本地缓存) │  │ (分布式缓存)  │  │ (静态缓存)    │                      │
│  │  TTL: 5min   │  │  TTL: 30min  │  │  永不失效     │                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                          存储层 (Storage层)                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                      │
│  │  Database    │  │   YAML       │  │  Config Center│                      │
│  │ (规则存储)    │  │ (本地配置)    │  │  (配置中心)   │                      │
│  └──────────────┘  └──────────────┘  └──────────────┘                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 包结构重新设计

```
com.carlos.datascope
├── annotation                          # 注解定义
│   ├── DataScope.java                  # 数据权限注解(V2)
│   ├── DataScopes.java                 # 组合注解
│   ├── DataMasking.java                # 数据脱敏注解(新增)
│   ├── DataFilter.java                 # 动态过滤器(新增)
│   └── RuleMapping.java                # 规则映射(新增)
├── core                                # 核心层
│   ├── engine                          # 规则引擎
│   │   ├── DataScopeRuleEngine.java
│   │   ├── RuleParser.java
│   │   ├── RuleCompiler.java
│   │   └── RuleExecutor.java
│   ├── context                         # 上下文管理
│   │   ├── DataScopeContext.java
│   │   ├── DataScopeContextManager.java
│   │   └── DataScopeHolder.java
│   ├── model                           # 核心模型
│   │   ├── DataScopeRule.java
│   │   ├── DataScopeResult.java
│   │   ├── DataScopeType.java
│   │   └── RuleExpression.java
│   └── aspect                          # AOP切面
│       └── DataScopeAspect.java
├── strategy                            # 策略层
│   ├── DataScopeStrategy.java          # 策略接口
│   ├── RowScopeStrategy.java           # 行权限
│   ├── ColumnScopeStrategy.java        # 列权限(脱敏)
│   ├── RangeScopeStrategy.java         # 范围权限
│   └── CustomScopeStrategy.java        # 自定义
├── provider                            # 数据提供层
│   ├── DataScopeProvider.java          # 提供器接口
│   ├── UserDataProvider.java           # 用户数据
│   ├── DeptDataProvider.java           # 部门数据
│   ├── RoleDataProvider.java           # 角色数据
│   └── RegionDataProvider.java         # 区域数据
├── cache                               # 缓存层
│   ├── DataScopeCache.java             # 缓存接口
│   ├── CaffeineDataScopeCache.java     # Caffeine实现
│   └── RedisDataScopeCache.java        # Redis实现
├── repository                          # 存储层
│   ├── RuleRepository.java             # 规则存储接口
│   ├── YamlRuleRepository.java         # YAML存储
│   ├── DatabaseRuleRepository.java     # 数据库存储
│   └── CompositeRuleRepository.java    # 组合存储
├── interceptor                         # 拦截器
│   ├── MyBatisDataScopeInterceptor.java # MyBatis拦截器
│   └── ResultSetMaskingInterceptor.java # 结果集脱敏
├── audit                               # 审计(新增)
│   ├── DataScopeAuditLogger.java
│   ├── AuditEvent.java
│   └── AuditStorage.java
├── expression                          # 表达式引擎(新增)
│   ├── ExpressionEngine.java
│   ├── SpelExpressionEngine.java
│   └── SimpleExpressionEngine.java
├── spi                                 # SPI扩展点
│   └── DataScopeExtension.java
├── auto                                # 自动配置
│   └── DataScopeAutoConfiguration.java
└── util                                # 工具类
    └── DataScopeUtils.java
```

---

## 3. 核心模型设计

### 3.1 数据权限规则模型 (DataScopeRule)

```java
/**
 * 数据权限规则定义
 */
@Data
@Builder
public class DataScopeRule {
    
    /** 规则唯一标识 */
    private String ruleId;
    
    /** 规则名称 */
    private String ruleName;
    
    /** 规则类型: ROW-行权限, COLUMN-列权限, RANGE-范围权限 */
    private RuleType type;
    
    /** 适用的表名，支持通配符 * */
    private Set<String> tableNames;
    
    /** 适用的Mapper方法，支持通配符 * */
    private Set<String> mapperMethods;
    
    /** 权限维度 */
    private ScopeDimension dimension;
    
    /** 规则表达式 */
    private RuleExpression expression;
    
    /** 优先级，数字越小优先级越高 */
    private int priority;
    
    /** 是否启用 */
    private boolean enabled;
    
    /** 规则来源: ANNOTATION-注解, DATABASE-数据库, YAML-配置文件 */
    private RuleSource source;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /**
     * 权限维度枚举
     */
    public enum ScopeDimension {
        // 用户维度
        CURRENT_USER,           // 仅本人
        USER_AND_SUBORDINATE,   // 本人及下属
        
        // 部门维度  
        CURRENT_DEPT,           // 本部门
        DEPT_AND_CHILDREN,      // 本部门及子部门
        DEPT_TREE,              // 部门树(可配置层级)
        
        // 角色维度
        CURRENT_ROLE,           // 本角色
        ROLE_AND_INHERIT,       // 本角色及继承角色
        
        // 区域维度
        CURRENT_REGION,         // 本区域
        REGION_AND_CHILDREN,    // 本区域及子区域
        
        // 自定义维度
        CUSTOM,                 // 自定义
        DATA_RANGE              // 数据范围
    }
    
    /**
     * 规则类型枚举
     */
    public enum RuleType {
        ROW,        // 行权限
        COLUMN,     // 列权限(脱敏)
        RANGE       // 范围权限
    }
}
```

### 3.2 规则表达式模型 (RuleExpression)

```java
/**
 * 规则表达式，支持多种表达式类型
 */
@Data
@Builder
public class RuleExpression {
    
    /** 表达式类型: SQL-原始SQL, SPEL-SpringEL, SIMPLE-简单表达式 */
    private ExpressionType type;
    
    /** 表达式内容 */
    private String content;
    
    /** 表达式参数 */
    private Map<String, Object> params;
    
    /** 编译后的表达式(缓存) */
    private transient Object compiledExpression;
    
    /**
     * 表达式类型
     */
    public enum ExpressionType {
        /** SQL片段，直接拼接到WHERE条件 */
        SQL,
        /** SpringEL表达式，支持复杂逻辑 */
        SPEL,
        /** 简单表达式: field operator value */
        SIMPLE,
        /** 脚本表达式: Groovy/JavaScript */
        SCRIPT
    }
    
    /**
     * 预编译表达式
     */
    public void compile() {
        // 根据type进行预编译，提升执行性能
    }
}
```

### 3.3 数据权限结果模型 (DataScopeResult)

```java
/**
 * 数据权限处理结果
 */
@Data
@Builder
public class DataScopeResult {
    
    /** 是否允许访问 */
    private boolean allowed;
    
    /** SQL条件片段 */
    private String sqlCondition;
    
    /** 参数值列表 */
    private List<Object> paramValues;
    
    /** 需要脱敏的列 */
    private Set<String> maskingColumns;
    
    /** 脱敏规则映射 */
    private Map<String, MaskingRule> maskingRules;
    
    /** 应用的数据范围 */
    private DataRange dataRange;
    
    /** 拒绝原因 */
    private String denyReason;
    
    /** 匹配的规则的ID列表 */
    private List<String> matchedRuleIds;
    
    /**
     * 脱敏规则
     */
    @Data
    public static class MaskingRule {
        /** 脱敏类型: PHONE-手机号, ID_CARD-身份证, EMAIL-邮箱, BANK_CARD-银行卡, CUSTOM-自定义 */
        private MaskingType type;
        /** 自定义脱敏表达式 */
        private String pattern;
        /** 替换字符 */
        private char maskChar;
        /** 保留前缀位数 */
        private int keepPrefix;
        /** 保留后缀位数 */
        private int keepSuffix;
    }
    
    /**
     * 数据范围
     */
    @Data
    public static class DataRange {
        /** 最小值 */
        private Comparable<?> minValue;
        /** 最大值 */
        private Comparable<?> maxValue;
        /** 允许的离散值 */
        private Set<Object> allowedValues;
        /** 排除的值 */
        private Set<Object> excludedValues;
    }
}
```

---

## 4. 注解设计 (V2 简化版)

### 4.1 @DataScope (简化)

```java
/**
 * 数据权限注解 V2 - 简化配置
 */
@Repeatable(DataScopes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataScope {
    
    /**
     * 权限维度，默认从配置文件读取
     */
    ScopeDimension dimension() default ScopeDimension.AUTO;
    
    /**
     * 权限控制字段，默认智能推断
     * - 用户维度: create_by / user_id
     * - 部门维度: dept_id / department_id
     * - 区域维度: region_code / area_code
     */
    String field() default "";
    
    /**
     * 适用的表名，默认所有表
     */
    String[] tables() default {};
    
    /**
     * 排除的表名
     */
    String[] excludeTables() default {};
    
    /**
     * 自定义规则表达式 (SpEL)
     * 示例: "@dataScope.isOwner(#entity) or @dataScope.hasRole('admin')"
     */
    String condition() default "";
    
    /**
     * 自定义处理器
     */
    Class<? extends DataScopeStrategy> handler() default DataScopeStrategy.class;
    
    /**
     * 优先级，数字越小优先级越高
     */
    int priority() default 100;
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 备注说明
     */
    String remark() default "";
}

/**
 * 组合注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataScopes {
    DataScope[] value();
}
```

### 4.2 @DataMasking (新增)

```java
/**
 * 数据脱敏注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataMasking {
    
    /**
     * 脱敏字段配置
     */
    MaskingField[] fields();
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 脱敏字段
     */
    @interface MaskingField {
        /** 字段名 */
        String name();
        /** 脱敏类型 */
        MaskingType type();
        /** 自定义正则 */
        String pattern() default "";
        /** 保留前缀 */
        int keepPrefix() default -1;
        /** 保留后缀 */
        int keepSuffix() default -1;
    }
    
    enum MaskingType {
        PHONE,      // 手机号: 138****8888
        ID_CARD,    // 身份证: 110101********1234
        EMAIL,      // 邮箱: t***@qq.com
        BANK_CARD,  // 银行卡: 6222 **** **** 8888
        NAME,       // 姓名: 张**
        ADDRESS,    // 地址: 北京市********
        CUSTOM      // 自定义
    }
}
```

### 4.3 使用示例

```java
@RestController
@RequestMapping("/user")
public class UserController {
    
    /**
     * 查询用户列表 - 本部门及子部门数据
     */
    @GetMapping("/list")
    @DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN)
    public Result<Page<User>> list(UserQuery query) {
        return Result.success(userService.page(query));
    }
    
    /**
     * 查询用户详情 - 仅本人或管理员可查看
     */
    @GetMapping("/{id}")
    @DataScope(condition = "@ds.isCurrentUser(#id) or @ds.hasRole('admin')")
    public Result<User> detail(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }
    
    /**
     * 查询敏感数据 - 带脱敏
     */
    @GetMapping("/sensitive")
    @DataScope(dimension = ScopeDimension.CURRENT_DEPT)
    @DataMasking(fields = {
        @DataMasking.MaskingField(name = "phone", type = MaskingType.PHONE),
        @DataMasking.MaskingField(name = "idCard", type = MaskingType.ID_CARD),
        @DataMasking.MaskingField(name = "email", type = MaskingType.EMAIL)
    })
    public Result<List<UserSensitive>> sensitiveList() {
        return Result.success(userService.sensitiveList());
    }
    
    /**
     * 复杂场景 - 多权限组合
     */
    @GetMapping("/complex")
    @DataScopes({
        @DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN, tables = {"sys_user"}),
        @DataScope(dimension = ScopeDimension.CURRENT_USER, tables = {"sys_user_detail"}),
        @DataScope(dimension = ScopeDimension.ALL, tables = {"sys_dict"})
    })
    public Result<ComplexData> complexQuery() {
        return Result.success(userService.complexQuery());
    }
}
```

---

## 5. 规则引擎设计

### 5.1 规则引擎架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     DataScopeRuleEngine                          │
│                      (规则引擎入口)                               │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐         │
│  │   parse()   │ →  │  compile()  │ →  │  execute()  │         │
│  │   规则解析   │    │   规则编译   │    │   规则执行   │         │
│  └─────────────┘    └─────────────┘    └─────────────┘         │
└─────────────────────────────────────────────────────────────────┘
           │                   │                   │
           ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│  RuleParser (规则解析器)                                         │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. 从注解提取规则                                         │    │
│  │  2. 从数据库加载规则                                        │    │
│  │  3. 从YAML加载规则                                         │    │
│  │  4. 合并去重排序                                           │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│  RuleCompiler (规则编译器)                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. SpEL表达式编译 → StandardEvaluationContext           │    │
│  │  2. SQL片段编译    → SqlNode (预解析)                     │    │
│  │  3. 简单表达式编译  → CompiledExpression                 │    │
│  │  4. 缓存编译结果                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
┌─────────────────────────────────────────────────────────────────┐
│  RuleExecutor (规则执行器)                                       │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │  1. 构建执行上下文 (EvaluationContext)                    │    │
│  │  2. 注入内置变量: user, dept, role, params               │    │
│  │  3. 按优先级执行规则                                       │    │
│  │  4. 合并执行结果                                          │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 规则执行流程

```java
/**
 * 规则引擎核心接口
 */
public interface DataScopeRuleEngine {
    
    /**
     * 解析规则
     */
    List<DataScopeRule> parse(Method method, DataScope[] annotations);
    
    /**
     * 编译规则
     */
    CompiledRule compile(DataScopeRule rule);
    
    /**
     * 执行规则
     */
    DataScopeResult execute(CompiledRule rule, ExecutionContext context);
    
    /**
     * 完整处理流程
     */
    default DataScopeResult process(Method method, DataScope[] annotations, Object[] args) {
        // 1. 解析规则
        List<DataScopeRule> rules = parse(method, annotations);
        
        // 2. 按优先级排序
        rules.sort(Comparator.comparingInt(DataScopeRule::getPriority));
        
        // 3. 构建执行上下文
        ExecutionContext context = buildContext(args);
        
        // 4. 编译并执行规则
        DataScopeResult result = DataScopeResult.builder()
            .allowed(true)
            .sqlCondition("1=1")
            .build();
        
        for (DataScopeRule rule : rules) {
            if (!rule.isEnabled()) continue;
            
            CompiledRule compiled = compile(rule);
            DataScopeRule ruleResult = execute(compiled, context);
            
            // 合并结果
            result = merge(result, ruleResult);
        }
        
        return result;
    }
}
```

---

## 6. 缓存设计

### 6.1 多级缓存架构

```
┌─────────────────────────────────────────────────────────────────┐
│                      多级缓存架构                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   请求 → L1 Cache → L2 Cache → L3 Source                        │
│          (本地)      (Redis)      (数据库)                       │
│            ↑           ↑            ↑                           │
│            │           │            │                           │
│   ┌────────┴───┐  ┌────┴────┐  ┌───┴────────┐                  │
│   │ Method级   │  │ 用户级   │  │ 部门/角色级 │                  │
│   │ TTL: 0     │  │ TTL: 5m │  │ TTL: 30m   │                  │
│   └────────────┘  └─────────┘  └────────────┘                  │
│                                                                 │
│   缓存键设计:                                                   │
│   - L1: framework:datascope:method:{className}:{methodName}:{userId} │
│   - L2: framework:datascope:user:{userId}:{dimension}                │
│   - L3: framework:datascope:dept:{deptId}:users                      │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 缓存配置

```yaml
carlos:
  datascope:
    cache:
      enabled: true
      # L1 本地缓存 (Caffeine)
      local:
        enabled: true
        max-size: 1000
        ttl: 5m  # 方法级缓存时间
      # L2 分布式缓存 (Redis)
      redis:
        enabled: true
        ttl: 30m  # 用户数据缓存时间
      # 缓存预热
      warm-up:
        enabled: true
        # 启动时预热的规则
        rules:
          - dimension: DEPT_AND_CHILDREN
          - dimension: CURRENT_ROLE
```

---

## 7. MyBatis 拦截器设计

### 7.1 拦截器链路

```java
/**
 * MyBatis 数据权限拦截器
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query", 
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", 
        args = {MappedStatement.class, Object.class})
})
public class MyBatisDataScopeInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取当前线程的数据权限上下文
        DataScopeContext context = DataScopeContextHolder.get();
        if (context == null || !context.hasRules()) {
            return invocation.proceed();
        }
        
        // 2. 获取MappedStatement
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        
        // 3. 检查是否需要处理
        if (!shouldProcess(ms, context)) {
            return invocation.proceed();
        }
        
        // 4. 获取SQL
        BoundSql boundSql = ms.getBoundSql(invocation.getArgs()[1]);
        String originalSql = boundSql.getSql();
        
        // 5. 应用数据权限规则
        DataScopeResult result = applyRules(context, ms, originalSql);
        
        // 6. 如果没有权限，直接返回空结果
        if (!result.isAllowed()) {
            return handleDenied(invocation, result);
        }
        
        // 7. 重写SQL
        String newSql = rewriteSql(originalSql, result);
        
        // 8. 创建新的BoundSql
        BoundSql newBoundSql = createNewBoundSql(ms, newSql, boundSql, result);
        
        // 9. 创建新的MappedStatement
        MappedStatement newMs = createNewMappedStatement(ms, newBoundSql);
        
        // 10. 替换参数并执行
        invocation.getArgs()[0] = newMs;
        return invocation.proceed();
    }
    
    /**
     * 重写SQL，注入权限条件
     */
    private String rewriteSql(String originalSql, DataScopeResult result) {
        // 使用JSqlParser解析和修改SQL
        Statement statement = CCJSqlParserUtil.parse(originalSql);
        
        if (statement instanceof Select) {
            Select select = (Select) statement;
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            
            Expression where = plainSelect.getWhere();
            Expression dataScopeCondition = CCJSqlParserUtil.parseCondExpression(
                result.getSqlCondition()
            );
            
            if (where == null) {
                plainSelect.setWhere(dataScopeCondition);
            } else {
                AndExpression and = new AndExpression(where, dataScopeCondition);
                plainSelect.setWhere(and);
            }
        }
        
        return statement.toString();
    }
}
```

---

## 8. 审计功能设计

### 8.1 审计事件模型

```java
/**
 * 数据权限审计事件
 */
@Data
@Builder
public class DataScopeAuditEvent {
    
    /** 事件ID */
    private String eventId;
    
    /** 时间戳 */
    private LocalDateTime timestamp;
    
    /** 用户ID */
    private Long userId;
    
    /** 用户名 */
    private String username;
    
    /** 操作类型: QUERY-查询, UPDATE-更新, DELETE-删除 */
    private OperationType operationType;
    
    /** 目标表 */
    private String targetTable;
    
    /** Mapper方法 */
    private String mapperMethod;
    
    /** 应用的规则ID列表 */
    private List<String> appliedRules;
    
    /** SQL条件 */
    private String sqlCondition;
    
    /** 影响行数 */
    private Integer affectedRows;
    
    /** 执行时长(ms) */
    private Long executionTime;
    
    /** 是否被拦截 */
    private boolean denied;
    
    /** 拦截原因 */
    private String denyReason;
    
    /** 客户端IP */
    private String clientIp;
    
    /** 请求ID */
    private String requestId;
}
```

### 8.2 审计日志存储策略

```yaml
carlos:
  datascope:
    audit:
      enabled: true
      # 存储方式: LOGGER-日志, DATABASE-数据库, MQ-消息队列
      storage: LOGGER
      # 采样率 (0-100)
      sample-rate: 100
      # 异步处理
      async:
        enabled: true
        core-pool-size: 2
        max-pool-size: 5
      # 只记录被拒绝的请求
      log-denied-only: false
```

---

## 9. 配置设计

### 9.1 完整配置示例

```yaml
carlos:
  datascope:
    # 是否启用
    enabled: true
    
    # 默认权限维度
    default-dimension: CURRENT_USER
    
    # 默认权限字段映射
    field-mapping:
      user: create_by
      dept: dept_id
      role: role_id
      region: region_code
    
    # 缓存配置
    cache:
      enabled: true
      local:
        max-size: 1000
        ttl: 5m
      redis:
        enabled: true
        ttl: 30m
    
    # 规则配置
    rules:
      # 全局规则
      global:
        - dimension: DEPT_AND_CHILDREN
          tables: ["sys_user", "sys_dept"]
          priority: 100
      
      # 按表配置
      table:
        sys_user:
          dimension: DEPT_AND_CHILDREN
          field: dept_id
        sys_order:
          dimension: CURRENT_USER
          field: user_id
    
    # 数据提供器配置
    provider:
      # 部门数据提供者
      dept:
        # 是否包含子部门
        include-children: true
        # 最大层级
        max-level: 10
      # 角色数据提供者
      role:
        # 是否包含继承角色
        include-inherit: true
    
    # 审计配置
    audit:
      enabled: true
      storage: LOGGER
      sample-rate: 100
    
    # 表达式引擎
    expression:
      # 引擎类型: SPEL, SIMPLE, MIXED
      engine-type: SPEL
      # 启用缓存
      cache-enabled: true
```

---

## 10. 性能优化策略

### 10.1 优化措施

| 优化点     | 策略                    | 预期收益       |
|---------|-----------------------|------------|
| 规则编译缓存  | 预编译SpEL/SQL表达式        | 减少90%编译耗时  |
| 多级缓存    | Caffeine + Redis 双层缓存 | 减少80%数据库查询 |
| 异步加载    | 部门/角色数据异步预热           | 降低请求延迟     |
| SQL批量注入 | 支持IN批量查询              | 减少N+1查询    |
| 懒加载     | 按需加载权限数据              | 减少内存占用     |
| 规则短路    | 优先级高的规则先执行，可短路        | 减少无效计算     |

### 10.2 性能指标目标

| 指标    | 目标值    | 测试场景     |
|-------|--------|----------|
| 规则解析  | < 1ms  | 单条规则解析   |
| 规则执行  | < 2ms  | 包含缓存命中   |
| SQL重写 | < 3ms  | 复杂SQL注入  |
| 整体延迟  | < 5ms  | P99 请求延迟 |
| 内存占用  | < 50MB | 1000规则缓存 |

---

## 11. 迁移指南

### 11.1 V1 → V2 迁移说明

| V1 配置                    | V2 配置                  | 说明         |
|--------------------------|------------------------|------------|
| `@DataScope.type`        | `@DataScope.dimension` | 属性名变更      |
| `@DataScope.methodPoint` | 移除                     | 自动推断，无需配置  |
| `@DataScope.caller`      | 移除                     | 自动推断，无需配置  |
| `@DataScope.field`       | `@DataScope.field`     | 支持自动推断，可省略 |
| `CustomScope`            | `DataScopeStrategy`    | 接口变更       |
| `DataScopeInfo`          | `DataScopeResult`      | 模型变更       |

### 11.2 兼容性支持

V2版本提供V1兼容层，可通过配置启用：

```yaml
carlos:
  datascope:
    compatibility:
      v1-enabled: true  # 启用V1兼容模式
```

---

## 12. 未来扩展

### 12.1 规划功能

| 功能        | 说明                  | 优先级 |
|-----------|---------------------|-----|
| 数据权限可视化配置 | 管理后台配置规则            | P1  |
| 数据权限分析报表  | 数据访问统计分析            | P2  |
| AI权限推荐    | 基于使用习惯推荐规则          | P3  |
| 跨服务数据权限   | Dubbo/Feign 传递权限上下文 | P1  |
| 数据权限测试工具  | 规则测试、模拟执行           | P2  |

---

**文档结束**
