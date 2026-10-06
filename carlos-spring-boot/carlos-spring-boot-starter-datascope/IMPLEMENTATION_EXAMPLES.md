# Carlos 数据权限模块 V2 实现示例

> 本文档提供基于新技术设计的核心代码实现示例

---

## 1. 核心注解实现

### 1.1 @DataScope (V2 简化版)

```java
package com.carlos.datascope.annotation;

import com.carlos.datascope.core.model.ScopeDimension;
import com.carlos.datascope.strategy.DataScopeStrategy;

import java.lang.annotation.*;

/**
 * 数据权限注解 V2 - 简化配置，智能推断
 *
 * @author Carlos
 * @version 2.0
 */
@Repeatable(DataScopes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataScope {
    
    /**
     * 权限维度，默认自动推断
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
     * 示例: "@ds.isOwner(#entity) or @ds.hasRole('admin')"
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
```

### 1.2 @DataMasking (新增)

```java
package com.carlos.datascope.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 *
 * @author Carlos
 * @version 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface DataMasking {
    
    /**
     * 脱敏字段配置
     */
    Field[] fields();
    
    /**
     * 是否启用
     */
    boolean enabled() default true;
    
    /**
     * 脱敏字段
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({})
    @interface Field {
        /** 字段名 */
        String name();
        /** 脱敏类型 */
        Type type();
        /** 自定义正则 */
        String pattern() default "";
        /** 保留前缀 */
        int keepPrefix() default -1;
        /** 保留后缀 */
        int keepSuffix() default -1;
    }
    
    enum Type {
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

---

## 2. 核心模型实现

### 2.1 ScopeDimension (权限维度)

```java
package com.carlos.datascope.core.model;

import lombok.Getter;

/**
 * 数据权限维度枚举
 *
 * @author Carlos
 */
@Getter
public enum ScopeDimension {
    
    AUTO("自动推断", "根据上下文自动选择合适的权限维度"),
    
    // 用户维度
    CURRENT_USER("仅本人", "只能查看本人创建的数据"),
    USER_AND_SUBORDINATE("本人及下属", "查看本人及下属的数据"),
    
    // 部门维度
    CURRENT_DEPT("本部门", "只能查看本部门的数据"),
    DEPT_AND_CHILDREN("本部门及子部门", "查看本部门及子部门的数据"),
    DEPT_TREE("部门树", "可配置层级的部门树数据"),
    
    // 角色维度
    CURRENT_ROLE("本角色", "只能查看本角色相关的数据"),
    ROLE_AND_INHERIT("角色及继承", "本角色及继承角色的数据"),
    
    // 区域维度
    CURRENT_REGION("本区域", "只能查看本区域的数据"),
    REGION_AND_CHILDREN("本区域及子区域", "查看本区域及子区域的数据"),
    
    // 数据范围维度
    DATA_RANGE("数据范围", "指定的数据范围"),
    
    // 特殊维度
    ALL("全部数据", "查看所有数据，不受限制"),
    NONE("无权限", "无任何数据权限"),
    CUSTOM("自定义", "自定义权限规则");
    
    private final String label;
    private final String description;
    
    ScopeDimension(String label, String description) {
        this.label = label;
        this.description = description;
    }
    
    /**
     * 是否需要用户上下文
     */
    public boolean requireUserContext() {
        return this != ALL && this != NONE && this != CUSTOM;
    }
    
    /**
     * 是否属于部门维度
     */
    public boolean isDeptDimension() {
        return this == CURRENT_DEPT || this == DEPT_AND_CHILDREN || this == DEPT_TREE;
    }
    
    /**
     * 是否属于用户维度
     */
    public boolean isUserDimension() {
        return this == CURRENT_USER || this == USER_AND_SUBORDINATE;
    }
}
```

### 2.2 DataScopeRule (数据权限规则)

```java
package com.carlos.datascope.core.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 数据权限规则定义
 *
 * @author Carlos
 */
@Data
@Builder
public class DataScopeRule implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 规则唯一标识 */
    private String ruleId;
    
    /** 规则名称 */
    private String ruleName;
    
    /** 规则类型: ROW-行权限, COLUMN-列权限 */
    private RuleType type;
    
    /** 适用的表名，支持通配符 * */
    private Set<String> tableNames;
    
    /** 适用的Mapper方法，支持通配符 * */
    private Set<String> mapperMethods;
    
    /** 权限维度 */
    private ScopeDimension dimension;
    
    /** 权限控制字段 */
    private String field;
    
    /** 规则表达式 */
    private RuleExpression expression;
    
    /** 优先级，数字越小优先级越高 */
    private int priority;
    
    /** 是否启用 */
    private boolean enabled;
    
    /** 规则来源 */
    private RuleSource source;
    
    /** 扩展属性 */
    private Map<String, Object> extensions;
    
    /** 创建时间 */
    private LocalDateTime createTime;
    
    /** 更新时间 */
    private LocalDateTime updateTime;
    
    /**
     * 规则类型
     */
    public enum RuleType {
        ROW,        // 行权限
        COLUMN,     // 列权限
        RANGE       // 范围权限
    }
    
    /**
     * 规则来源
     */
    public enum RuleSource {
        ANNOTATION,     // 注解
        DATABASE,       // 数据库
        YAML,           // 配置文件
        PROGRAMMATIC    // 编程式
    }
    
    /**
     * 检查规则是否匹配指定表
     */
    public boolean matchesTable(String tableName) {
        if (tableNames == null || tableNames.isEmpty()) {
            return true; // 未指定表名，匹配所有
        }
        return tableNames.contains(tableName) || tableNames.contains("*");
    }
    
    /**
     * 检查规则是否匹配指定Mapper方法
     */
    public boolean matchesMethod(String methodName) {
        if (mapperMethods == null || mapperMethods.isEmpty()) {
            return true; // 未指定方法，匹配所有
        }
        return mapperMethods.contains(methodName) || mapperMethods.contains("*");
    }
}
```

### 2.3 DataScopeResult (数据权限结果)

```java
package com.carlos.datascope.core.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

/**
 * 数据权限处理结果
 *
 * @author Carlos
 */
@Data
@Builder
public class DataScopeResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /** 是否允许访问 */
    private boolean allowed;
    
    /** SQL条件片段 */
    private String sqlCondition;
    
    /** 参数值列表 */
    @Builder.Default
    private List<Object> paramValues = new ArrayList<>();
    
    /** 需要脱敏的列 */
    @Builder.Default
    private Set<String> maskingColumns = new HashSet<>();
    
    /** 脱敏规则映射 */
    @Builder.Default
    private Map<String, MaskingRule> maskingRules = new HashMap<>();
    
    /** 拒绝原因 */
    private String denyReason;
    
    /** 匹配的规则的ID列表 */
    @Builder.Default
    private List<String> matchedRuleIds = new ArrayList<>();
    
    /**
     * 获取空权限结果(拒绝访问)
     */
    public static DataScopeResult denied(String reason) {
        return DataScopeResult.builder()
            .allowed(false)
            .denyReason(reason)
            .sqlCondition("1 = 0")
            .build();
    }
    
    /**
     * 获取全权限结果(允许访问所有)
     */
    public static DataScopeResult allowed() {
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition("1 = 1")
            .build();
    }
    
    /**
     * 合并两个结果
     */
    public DataScopeResult merge(DataScopeResult other) {
        if (other == null) {
            return this;
        }
        
        DataScopeResult result = new DataScopeResult();
        result.setAllowed(this.allowed && other.allowed);
        result.setSqlCondition(mergeCondition(this.sqlCondition, other.sqlCondition));
        
        // 合并参数值
        List<Object> mergedParams = new ArrayList<>(this.paramValues);
        mergedParams.addAll(other.paramValues);
        result.setParamValues(mergedParams);
        
        // 合并脱敏规则
        Set<String> mergedColumns = new HashSet<>(this.maskingColumns);
        mergedColumns.addAll(other.maskingColumns);
        result.setMaskingColumns(mergedColumns);
        
        Map<String, MaskingRule> mergedRules = new HashMap<>(this.maskingRules);
        mergedRules.putAll(other.maskingRules);
        result.setMaskingRules(mergedRules);
        
        // 合并规则ID
        List<String> mergedRuleIds = new ArrayList<>(this.matchedRuleIds);
        mergedRuleIds.addAll(other.matchedRuleIds);
        result.setMatchedRuleIds(mergedRuleIds);
        
        return result;
    }
    
    private String mergeCondition(String cond1, String cond2) {
        if (cond1 == null || cond1.isEmpty()) return cond2;
        if (cond2 == null || cond2.isEmpty()) return cond1;
        if ("1 = 1".equals(cond1)) return cond2;
        if ("1 = 1".equals(cond2)) return cond1;
        return "(" + cond1 + ") AND (" + cond2 + ")";
    }
    
    /**
     * 脱敏规则
     */
    @Data
    @Builder
    public static class MaskingRule implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private MaskingType type;
        private String pattern;
        private char maskChar;
        private int keepPrefix;
        private int keepSuffix;
    }
    
    public enum MaskingType {
        PHONE, ID_CARD, EMAIL, BANK_CARD, NAME, ADDRESS, CUSTOM
    }
}
```

---

## 3. 规则引擎实现

### 3.1 DataScopeRuleEngine (规则引擎接口)

```java
package com.carlos.datascope.core.engine;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.core.model.DataScopeResult;
import com.carlos.datascope.core.model.DataScopeRule;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限规则引擎接口
 *
 * @author Carlos
 */
public interface DataScopeRuleEngine {
    
    /**
     * 从注解解析规则
     */
    List<DataScopeRule> parseFromAnnotations(Method method, DataScope[] annotations);
    
    /**
     * 从配置加载规则
     */
    List<DataScopeRule> loadFromConfig(String mapperMethod);
    
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
    DataScopeResult process(Method method, DataScope[] annotations, Object[] args);
    
    /**
     * 清除规则缓存
     */
    void clearCache();
}
```

### 3.2 DefaultRuleEngine (默认实现)

```java
package com.carlos.datascope.core.engine;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.cache.DataScopeCache;
import com.carlos.datascope.core.model.DataScopeResult;
import com.carlos.datascope.core.model.DataScopeRule;
import com.carlos.datascope.core.model.RuleExpression;
import com.carlos.datascope.core.model.ScopeDimension;
import com.carlos.datascope.expression.ExpressionEngine;
import com.carlos.datascope.provider.DataScopeProvider;
import com.carlos.datascope.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 默认规则引擎实现
 *
 * @author Carlos
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultRuleEngine implements DataScopeRuleEngine {
    
    private final RuleRepository ruleRepository;
    private final DataScopeProvider dataProvider;
    private final DataScopeCache cache;
    private final ExpressionEngine expressionEngine;
    
    private final Map<String, CompiledRule> compiledRuleCache = new ConcurrentHashMap<>();
    
    @Override
    public List<DataScopeRule> parseFromAnnotations(Method method, DataScope[] annotations) {
        if (annotations == null || annotations.length == 0) {
            return Collections.emptyList();
        }
        
        List<DataScopeRule> rules = new ArrayList<>();
        for (DataScope annotation : annotations) {
            if (!annotation.enabled()) {
                continue;
            }
            
            DataScopeRule rule = DataScopeRule.builder()
                .ruleId(UUID.randomUUID().toString())
                .ruleName(method.getName() + "_" + annotation.dimension())
                .type(DataScopeRule.RuleType.ROW)
                .tableNames(new HashSet<>(Arrays.asList(annotation.tables())))
                .dimension(annotation.dimension())
                .field(annotation.field())
                .expression(buildExpression(annotation))
                .priority(annotation.priority())
                .enabled(true)
                .source(DataScopeRule.RuleSource.ANNOTATION)
                .build();
            
            rules.add(rule);
        }
        
        return rules;
    }
    
    @Override
    public List<DataScopeRule> loadFromConfig(String mapperMethod) {
        // 先从缓存获取
        String cacheKey = "rules:config:" + mapperMethod;
        List<DataScopeRule> cached = cache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // 从仓库加载
        List<DataScopeRule> rules = ruleRepository.findByMapperMethod(mapperMethod);
        
        // 缓存结果
        cache.put(cacheKey, rules, 5, TimeUnit.MINUTES);
        
        return rules;
    }
    
    @Override
    public CompiledRule compile(DataScopeRule rule) {
        String cacheKey = "compiled:" + rule.getRuleId();
        
        return compiledRuleCache.computeIfAbsent(cacheKey, k -> {
            CompiledRule compiled = new CompiledRule();
            compiled.setRule(rule);
            
            // 编译表达式
            if (rule.getExpression() != null) {
                compiled.setCompiledExpression(
                    expressionEngine.compile(rule.getExpression().getContent())
                );
            }
            
            return compiled;
        });
    }
    
    @Override
    public DataScopeResult execute(CompiledRule compiledRule, ExecutionContext context) {
        DataScopeRule rule = compiledRule.getRule();
        
        // 根据维度执行不同的处理逻辑
        return switch (rule.getDimension()) {
            case ALL -> DataScopeResult.allowed();
            case NONE -> DataScopeResult.denied("No data scope permission");
            case CURRENT_USER -> executeCurrentUser(rule, context);
            case CURRENT_DEPT -> executeCurrentDept(rule, context);
            case DEPT_AND_CHILDREN -> executeDeptAndChildren(rule, context);
            case CURRENT_ROLE -> executeCurrentRole(rule, context);
            case CUSTOM -> executeCustom(rule, context, compiledRule);
            default -> executeAuto(rule, context);
        };
    }
    
    @Override
    public DataScopeResult process(Method method, DataScope[] annotations, Object[] args) {
        // 1. 从注解解析规则
        List<DataScopeRule> annotationRules = parseFromAnnotations(method, annotations);
        
        // 2. 从配置加载规则
        String mapperMethod = method.getDeclaringClass().getName() + "." + method.getName();
        List<DataScopeRule> configRules = loadFromConfig(mapperMethod);
        
        // 3. 合并规则并去重
        List<DataScopeRule> allRules = new ArrayList<>();
        allRules.addAll(annotationRules);
        allRules.addAll(configRules);
        
        // 4. 按优先级排序
        allRules.sort(Comparator.comparingInt(DataScopeRule::getPriority));
        
        // 5. 构建执行上下文
        ExecutionContext context = buildContext(args);
        
        // 6. 执行规则并合并结果
        DataScopeResult result = DataScopeResult.allowed();
        
        for (DataScopeRule rule : allRules) {
            if (!rule.isEnabled()) continue;
            
            CompiledRule compiled = compile(rule);
            DataScopeRule ruleResult = execute(compiled, context);
            
            result = result.merge(ruleResult);
            
            // 如果被拒绝，直接返回
            if (!result.isAllowed()) {
                break;
            }
        }
        
        return result;
    }
    
    @Override
    public void clearCache() {
        compiledRuleCache.clear();
        cache.clear();
    }
    
    // ============= 私有方法 =============
    
    private RuleExpression buildExpression(DataScope annotation) {
        if (!annotation.condition().isEmpty()) {
            return RuleExpression.builder()
                .type(RuleExpression.ExpressionType.SPEL)
                .content(annotation.condition())
                .build();
        }
        return null;
    }
    
    private ExecutionContext buildContext(Object[] args) {
        ExecutionContext context = new ExecutionContext();
        
        // 注入当前用户
        context.setVariable("user", dataProvider.getCurrentUser());
        context.setVariable("userId", dataProvider.getCurrentUserId());
        
        // 注入部门信息
        context.setVariable("dept", dataProvider.getCurrentDept());
        context.setVariable("deptId", dataProvider.getCurrentDeptId());
        
        // 注入角色信息
        context.setVariable("roles", dataProvider.getCurrentRoles());
        
        // 注入方法参数
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
                context.setVariable("p" + i, args[i]);
            }
        }
        
        return context;
    }
    
    private DataScopeResult executeCurrentUser(DataScopeRule rule, ExecutionContext context) {
        Serializable userId = dataProvider.getCurrentUserId();
        if (userId == null) {
            return DataScopeResult.denied("User not authenticated");
        }
        
        String field = resolveField(rule, "create_by");
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " = ?")
            .paramValues(Collections.singletonList(userId))
            .matchedRuleIds(Collections.singletonList(rule.getRuleId()))
            .build();
    }
    
    private DataScopeResult executeCurrentDept(DataScopeRule rule, ExecutionContext context) {
        Serializable deptId = dataProvider.getCurrentDeptId();
        if (deptId == null) {
            return DataScopeResult.denied("Department not found");
        }
        
        String field = resolveField(rule, "dept_id");
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " = ?")
            .paramValues(Collections.singletonList(deptId))
            .matchedRuleIds(Collections.singletonList(rule.getRuleId()))
            .build();
    }
    
    private DataScopeResult executeDeptAndChildren(DataScopeRule rule, ExecutionContext context) {
        Serializable deptId = dataProvider.getCurrentDeptId();
        if (deptId == null) {
            return DataScopeResult.denied("Department not found");
        }
        
        // 从缓存获取子部门
        String cacheKey = "dept:children:" + deptId;
        Set<Serializable> deptIds = cache.get(cacheKey);
        
        if (deptIds == null) {
            deptIds = dataProvider.getDeptChildrenIds(deptId);
            deptIds.add(deptId); // 包含当前部门
            cache.put(cacheKey, deptIds, 30, TimeUnit.MINUTES);
        }
        
        String field = resolveField(rule, "dept_id");
        String placeholders = deptIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(", "));
        
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " IN (" + placeholders + ")")
            .paramValues(new ArrayList<>(deptIds))
            .matchedRuleIds(Collections.singletonList(rule.getRuleId()))
            .build();
    }
    
    private DataScopeResult executeCurrentRole(DataScopeRule rule, ExecutionContext context) {
        Set<Serializable> roleIds = dataProvider.getCurrentRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return DataScopeResult.denied("No roles assigned");
        }
        
        String field = resolveField(rule, "role_id");
        String placeholders = roleIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(", "));
        
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " IN (" + placeholders + ")")
            .paramValues(new ArrayList<>(roleIds))
            .matchedRuleIds(Collections.singletonList(rule.getRuleId()))
            .build();
    }
    
    private DataScopeResult executeCustom(DataScopeRule rule, ExecutionContext context, CompiledRule compiled) {
        if (rule.getExpression() == null) {
            return DataScopeResult.denied("Custom rule without expression");
        }
        
        // 使用表达式引擎执行
        boolean allowed = expressionEngine.evaluate(
            compiled.getCompiledExpression(), 
            context
        );
        
        if (!allowed) {
            return DataScopeResult.denied("Custom expression evaluated to false");
        }
        
        return DataScopeResult.allowed();
    }
    
    private DataScopeResult executeAuto(DataScopeRule rule, ExecutionContext context) {
        // 自动推断：优先使用部门维度
        if (dataProvider.getCurrentDeptId() != null) {
            return executeDeptAndChildren(rule, context);
        }
        // 其次是用户维度
        if (dataProvider.getCurrentUserId() != null) {
            return executeCurrentUser(rule, context);
        }
        // 默认拒绝
        return DataScopeResult.denied("Cannot auto-detect dimension");
    }
    
    private String resolveField(DataScopeRule rule, String defaultField) {
        if (rule.getField() != null && !rule.getField().isEmpty()) {
            return rule.getField();
        }
        return defaultField;
    }
}
```

---

## 4. AOP 切面实现

```java
package com.carlos.datascope.core.aspect;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.annotation.DataScopes;
import com.carlos.datascope.audit.DataScopeAuditLogger;
import com.carlos.datascope.core.context.DataScopeContext;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.engine.DataScopeRuleEngine;
import com.carlos.datascope.core.model.DataScopeResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 数据权限AOP切面
 *
 * @author Carlos
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class DataScopeAspect {
    
    private final DataScopeRuleEngine ruleEngine;
    private final DataScopeAuditLogger auditLogger;
    
    /**
     * 切入点：标记了@DataScope或@DataScopes的方法
     */
    @Pointcut("@annotation(com.carlos.datascope.annotation.DataScope) || " +
              "@annotation(com.carlos.datascope.annotation.DataScopes)")
    public void dataScopePointcut() {}
    
    /**
     * 环绕通知
     */
    @Around("dataScopePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        
        // 获取注解
        DataScopes dataScopes = method.getAnnotation(DataScopes.class);
        DataScope[] scopes;
        if (dataScopes != null) {
            scopes = dataScopes.value();
        } else {
            scopes = method.getAnnotationsByType(DataScope.class);
        }
        
        if (scopes.length == 0) {
            return joinPoint.proceed();
        }
        
        long startTime = System.currentTimeMillis();
        DataScopeContext context = null;
        
        try {
            // 1. 执行规则引擎
            DataScopeResult result = ruleEngine.process(method, scopes, joinPoint.getArgs());
            
            // 2. 检查权限
            if (!result.isAllowed()) {
                log.warn("Data scope denied: method={}, reason={}", 
                    method.getName(), result.getDenyReason());
                
                // 记录审计日志
                auditLogger.logDenied(method, result, System.currentTimeMillis() - startTime);
                
                throw new DataScopeDeniedException(result.getDenyReason());
            }
            
            // 3. 创建上下文
            context = DataScopeContext.builder()
                .result(result)
                .method(method)
                .args(joinPoint.getArgs())
                .build();
            
            // 4. 设置到ThreadLocal
            DataScopeContextHolder.set(context);
            
            // 5. 执行目标方法
            Object resultObj = joinPoint.proceed();
            
            // 6. 处理脱敏
            resultObj = handleMasking(resultObj, result);
            
            // 7. 记录审计日志
            auditLogger.logSuccess(method, result, System.currentTimeMillis() - startTime);
            
            return resultObj;
            
        } finally {
            // 清理上下文
            DataScopeContextHolder.clear();
        }
    }
    
    /**
     * 处理数据脱敏
     */
    private Object handleMasking(Object result, DataScopeResult scopeResult) {
        if (result == null || scopeResult.getMaskingColumns().isEmpty()) {
            return result;
        }
        
        // 使用脱敏处理器处理结果
        return MaskingHandler.handle(result, scopeResult.getMaskingRules());
    }
}
```

---

## 5. MyBatis 拦截器实现

```java
package com.carlos.datascope.interceptor;

import com.carlos.datascope.core.context.DataScopeContext;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.model.DataScopeResult;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.*;

/**
 * MyBatis 数据权限拦截器
 *
 * @author Carlos
 */
@Slf4j
@Intercepts({
    @Signature(type = Executor.class, method = "query", 
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update", 
        args = {MappedStatement.class, Object.class})
})
public class MyBatisDataScopeInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 获取数据权限上下文
        DataScopeContext context = DataScopeContextHolder.get();
        if (context == null) {
            return invocation.proceed();
        }
        
        DataScopeResult result = context.getResult();
        if (result == null || !result.isAllowed()) {
            return invocation.proceed();
        }
        
        // 2. 获取MappedStatement和SQL
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        
        // 3. 检查是否需要注入
        String sqlCondition = result.getSqlCondition();
        if (sqlCondition == null || "1 = 1".equals(sqlCondition)) {
            return invocation.proceed();
        }
        
        // 4. 获取原始SQL
        BoundSql boundSql = ms.getBoundSql(parameter);
        String originalSql = boundSql.getSql();
        
        // 5. 重写SQL
        String newSql = injectCondition(originalSql, sqlCondition);
        
        // 6. 重建BoundSql
        BoundSql newBoundSql = rebuildBoundSql(ms, boundSql, newSql, result);
        
        // 7. 重建MappedStatement
        MappedStatement newMs = rebuildMappedStatement(ms, newBoundSql);
        
        // 8. 替换参数并执行
        invocation.getArgs()[0] = newMs;
        
        log.debug("Data scope SQL injected: original=[{}], new=[{}]", originalSql, newSql);
        
        return invocation.proceed();
    }
    
    /**
     * 注入权限条件到SQL
     */
    private String injectCondition(String originalSql, String condition) {
        try {
            Statement statement = CCJSqlParserUtil.parse(originalSql);
            
            if (statement instanceof Select) {
                Select select = (Select) statement;
                injectIntoSelect(select, condition);
                return select.toString();
            }
            
            // UPDATE 和 DELETE 暂不处理，或根据实际情况处理
            return originalSql;
            
        } catch (Exception e) {
            log.error("Failed to inject data scope condition", e);
            return originalSql;
        }
    }
    
    /**
     * 注入到SELECT语句
     */
    private void injectIntoSelect(Select select, String condition) {
        if (select.getSelectBody() instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            
            Expression where = plainSelect.getWhere();
            Expression dataScopeExpr = new HexValue(" " + condition + " ");
            
            if (where == null) {
                plainSelect.setWhere(dataScopeExpr);
            } else {
                AndExpression and = new AndExpression(where, dataScopeExpr);
                plainSelect.setWhere(and);
            }
        }
        // 处理SetOperationList (UNION等)
    }
    
    /**
     * 重建BoundSql
     */
    private BoundSql rebuildBoundSql(MappedStatement ms, BoundSql originalBoundSql, 
                                     String newSql, DataScopeResult result) {
        // 创建新的参数映射列表
        List<ParameterMapping> parameterMappings = new ArrayList<>(
            originalBoundSql.getParameterMappings()
        );
        
        // 添加数据权限参数
        List<Object> paramValues = result.getParamValues();
        for (int i = 0; i < paramValues.size(); i++) {
            ParameterMapping.Builder builder = new ParameterMapping.Builder(
                ms.getConfiguration(), 
                "_ds_param_" + i, 
                paramValues.get(i).getClass()
            );
            parameterMappings.add(builder.build());
        }
        
        // 创建新的BoundSql
        BoundSql newBoundSql = new BoundSql(
            ms.getConfiguration(), 
            newSql, 
            parameterMappings, 
            originalBoundSql.getParameterObject()
        );
        
        // 复制额外参数
        originalBoundSql.getAdditionalParameters().forEach(newBoundSql::setAdditionalParameter);
        
        // 设置数据权限参数值
        for (int i = 0; i < paramValues.size(); i++) {
            newBoundSql.setAdditionalParameter("_ds_param_" + i, paramValues.get(i));
        }
        
        return newBoundSql;
    }
    
    /**
     * 重建MappedStatement
     */
    private MappedStatement rebuildMappedStatement(MappedStatement ms, BoundSql boundSql) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
            ms.getConfiguration(),
            ms.getId(),
            parameterObject -> boundSql,
            ms.getSqlCommandType()
        );
        
        // 复制原有配置
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null) {
            builder.keyProperty(String.join(",", ms.getKeyProperties()));
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        
        return builder.build();
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 配置属性
    }
}
```

---

## 6. 缓存实现

```java
package com.carlos.datascope.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存实现
 *
 * @author Carlos
 */
public class CaffeineDataScopeCache implements DataScopeCache {
    
    private final LoadingCache<String, Object> cache;
    
    public CaffeineDataScopeCache(long maxSize, long ttlMinutes) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maxSize)
            .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
            .recordStats()
            .build();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key) {
        return (T) cache.getIfPresent(key);
    }
    
    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }
    
    @Override
    public void put(String key, Object value, long ttl, TimeUnit unit) {
        // Caffeine不支持单个条目的TTL，这里使用统一配置
        cache.put(key, value);
    }
    
    @Override
    public void remove(String key) {
        cache.invalidate(key);
    }
    
    @Override
    public void clear() {
        cache.invalidateAll();
    }
    
    @Override
    public boolean contains(String key) {
        return cache.getIfPresent(key) != null;
    }
    
    public com.github.benmanes.caffeine.cache.stats.CacheStats stats() {
        return cache.stats();
    }
}
```

---

## 7. 自动配置类

```java
package com.carlos.datascope.auto;

import com.carlos.datascope.cache.CaffeineDataScopeCache;
import com.carlos.datascope.cache.DataScopeCache;
import com.carlos.datascope.cache.RedisDataScopeCache;
import com.carlos.datascope.core.aspect.DataScopeAspect;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.engine.DataScopeRuleEngine;
import com.carlos.datascope.core.engine.DefaultRuleEngine;
import com.carlos.datascope.expression.ExpressionEngine;
import com.carlos.datascope.expression.SpelExpressionEngine;
import com.carlos.datascope.interceptor.MyBatisDataScopeInterceptor;
import com.carlos.datascope.properties.DataScopeProperties;
import com.carlos.datascope.provider.DataScopeProvider;
import com.carlos.datascope.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 数据权限自动配置
 *
 * @author Carlos
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataScopeProperties.class)
@ConditionalOnProperty(prefix = "carlos.datascope", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataScopeAutoConfiguration {
    
    private final DataScopeProperties properties;
    
    @Bean
    @ConditionalOnMissingBean
    public DataScopeCache dataScopeCache(
            @Lazy @org.springframework.lang.Nullable StringRedisTemplate redisTemplate) {
        
        if (properties.getCache().getRedis().isEnabled() && redisTemplate != null) {
            return new RedisDataScopeCache(redisTemplate, 
                properties.getCache().getRedis().getTtl().toMinutes());
        }
        
        return new CaffeineDataScopeCache(
            properties.getCache().getLocal().getMaxSize(),
            properties.getCache().getLocal().getTtl().toMinutes()
        );
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ExpressionEngine expressionEngine() {
        return new SpelExpressionEngine();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RuleRepository ruleRepository() {
        return new YamlRuleRepository(properties);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DataScopeRuleEngine dataScopeRuleEngine(
            RuleRepository ruleRepository,
            DataScopeProvider dataProvider,
            DataScopeCache cache,
            ExpressionEngine expressionEngine) {
        return new DefaultRuleEngine(ruleRepository, dataProvider, cache, expressionEngine);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(org.apache.ibatis.plugin.Interceptor.class)
    public MyBatisDataScopeInterceptor myBatisDataScopeInterceptor() {
        return new MyBatisDataScopeInterceptor();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public DataScopeAspect dataScopeAspect(DataScopeRuleEngine ruleEngine) {
        return new DataScopeAspect(ruleEngine, null);
    }
}
```

---

## 8. 配置属性类

```java
package com.carlos.datascope.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.*;

/**
 * 数据权限配置属性
 *
 * @author Carlos
 */
@Data
@ConfigurationProperties(prefix = "carlos.datascope")
public class DataScopeProperties {
    
    /** 是否启用 */
    private boolean enabled = true;
    
    /** 默认权限维度 */
    private String defaultDimension = "CURRENT_USER";
    
    /** 默认权限字段映射 */
    private FieldMapping fieldMapping = new FieldMapping();
    
    /** 缓存配置 */
    private CacheConfig cache = new CacheConfig();
    
    /** 审计配置 */
    private AuditConfig audit = new AuditConfig();
    
    /** 全局规则 */
    private List<RuleConfig> globalRules = new ArrayList<>();
    
    /** 表级规则 */
    private Map<String, RuleConfig> tableRules = new HashMap<>();
    
    @Data
    public static class FieldMapping {
        private String user = "create_by";
        private String dept = "dept_id";
        private String role = "role_id";
        private String region = "region_code";
    }
    
    @Data
    public static class CacheConfig {
        private boolean enabled = true;
        private LocalCacheConfig local = new LocalCacheConfig();
        private RedisCacheConfig redis = new RedisCacheConfig();
    }
    
    @Data
    public static class LocalCacheConfig {
        private boolean enabled = true;
        private long maxSize = 1000;
        private Duration ttl = Duration.ofMinutes(5);
    }
    
    @Data
    public static class RedisCacheConfig {
        private boolean enabled = false;
        private Duration ttl = Duration.ofMinutes(30);
    }
    
    @Data
    public static class AuditConfig {
        private boolean enabled = true;
        private String storage = "LOGGER"; // LOGGER, DATABASE, MQ
        private int sampleRate = 100;
        private boolean async = true;
    }
    
    @Data
    public static class RuleConfig {
        private String dimension;
        private String field;
        private List<String> tables;
        private int priority = 100;
        private boolean enabled = true;
    }
}
```

---

## 9. 使用示例

### 9.1 基本使用

```java
@Service
public class UserServiceImpl implements UserService {
    
    /**
     * 查询本部门用户
     */
    @Override
    @DataScope(dimension = ScopeDimension.CURRENT_DEPT)
    public List<User> listDeptUsers() {
        return userMapper.selectList();
    }
    
    /**
     * 查询本部门及子部门用户
     */
    @Override
    @DataScope(dimension = ScopeDimension.DEPT_AND_CHILDREN)
    public Page<User> pageUsers(PageParam param) {
        return userMapper.selectPage(param);
    }
    
    /**
     * 管理员查看所有数据
     */
    @Override
    @DataScope(dimension = ScopeDimension.ALL)
    @PreAuthorize("hasRole('admin')")
    public List<User> listAllUsers() {
        return userMapper.selectList();
    }
}
```

### 9.2 自定义表达式

```java
@Service
public class OrderServiceImpl implements OrderService {
    
    /**
     * 查看订单 - 本人或同部门同事创建的订单
     */
    @Override
    @DataScope(condition = "create_by == #userId or dept_id == #deptId")
    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }
    
    /**
     * 复杂场景 - 使用SpEL
     */
    @Override
    @DataScope(condition = "@ds.isOwner(#id) or @ds.hasAnyRole('admin', 'manager')")
    public OrderDetail getOrderDetail(Long id) {
        return orderMapper.selectDetailById(id);
    }
}
```

### 9.3 数据脱敏

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    /**
     * 查询用户列表 - 带脱敏
     */
    @GetMapping
    @DataScope(dimension = ScopeDimension.CURRENT_DEPT)
    @DataMasking(fields = {
        @DataMasking.Field(name = "phone", type = DataMasking.Type.PHONE),
        @DataMasking.Field(name = "idCard", type = DataMasking.Type.ID_CARD),
        @DataMasking.Field(name = "email", type = DataMasking.Type.EMAIL)
    })
    public Result<List<UserVO>> list() {
        return Result.success(userService.list());
    }
}
```

---

**实现示例结束**
