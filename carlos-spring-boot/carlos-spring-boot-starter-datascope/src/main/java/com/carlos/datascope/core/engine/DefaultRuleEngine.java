package com.carlos.datascope.core.engine;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.cache.DataScopeCache;
import com.carlos.datascope.core.model.DataScopeResult;
import com.carlos.datascope.core.model.DataScopeRule;
import com.carlos.datascope.core.model.RuleExpression;
import com.carlos.datascope.expression.ExpressionEngine;
import com.carlos.datascope.provider.DataScopeProvider;
import com.carlos.datascope.repository.RuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 默认规则引擎实现
 * <p>
 * 实现规则的解析、编译、执行的完整流程
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultRuleEngine implements DataScopeRuleEngine {

    private final RuleRepository ruleRepository;
    private final DataScopeProvider dataProvider;
    private final DataScopeCache cache;
    private final ExpressionEngine expressionEngine;

    /**
     * 编译后的规则缓存
     */
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
                .ruleId(generateRuleId(method, annotation))
                .ruleName(method.getName() + "_" + annotation.dimension())
                .type(DataScopeRule.RuleType.ROW)
                .tableNames(new HashSet<>(Arrays.asList(annotation.tables())))
                .mapperMethods(Collections.emptySet())
                .dimension(annotation.dimension())
                .field(annotation.field())
                .expression(buildExpression(annotation))
                .priority(annotation.priority())
                .enabled(true)
                .source(DataScopeRule.RuleSource.ANNOTATION)
                .extensions(new HashMap<>())
                .createTime(java.time.LocalDateTime.now())
                .updateTime(java.time.LocalDateTime.now())
                .build();

            rules.add(rule);
        }

        return rules;
    }

    @Override
    public List<DataScopeRule> loadFromConfig(String mapperMethod) {
        // 先从缓存获取
        String cacheKey = "rules:config:" + mapperMethod;
        @SuppressWarnings("unchecked")
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
            try {
                if (rule.getExpression() != null && rule.getExpression().isValid()) {
                    Object compiled = expressionEngine.compile(rule.getExpression().getContent());
                    return CompiledRule.success(rule, compiled);
                }
                return CompiledRule.success(rule, null);
            } catch (Exception e) {
                log.error("Failed to compile rule: {}", rule.getRuleId(), e);
                return CompiledRule.failure(rule, e.getMessage());
            }
        });
    }

    @Override
    public DataScopeResult execute(CompiledRule compiledRule, ExecutionContext context) {
        DataScopeRule rule = compiledRule.getRule();

        // 检查规则是否有效
        if (!rule.isEnabled()) {
            return DataScopeResult.allowed();
        }

        // 根据维度执行不同的处理逻辑
        return switch (rule.getDimension()) {
            case ALL -> DataScopeResult.allowed();
            case NONE -> DataScopeResult.denied("No data scope permission (dimension=NONE)");
            case CURRENT_USER -> executeCurrentUser(rule, context);
            case USER_AND_SUBORDINATE -> executeUserAndSubordinate(rule, context);
            case CURRENT_DEPT -> executeCurrentDept(rule, context);
            case DEPT_AND_CHILDREN -> executeDeptAndChildren(rule, context);
            case DEPT_TREE -> executeDeptTree(rule, context);
            case CURRENT_ROLE -> executeCurrentRole(rule, context);
            case ROLE_AND_INHERIT -> executeRoleAndInherit(rule, context);
            case CURRENT_REGION -> executeCurrentRegion(rule, context);
            case REGION_AND_CHILDREN -> executeRegionAndChildren(rule, context);
            case DATA_RANGE -> executeDataRange(rule, context);
            case CUSTOM -> executeCustom(rule, context, compiledRule);
            case AUTO -> executeAuto(rule, context);
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

        // 去重（按ruleId）
        Map<String, DataScopeRule> uniqueRules = new LinkedHashMap<>();
        for (DataScopeRule rule : allRules) {
            uniqueRules.putIfAbsent(rule.getRuleId(), rule);
        }
        allRules = new ArrayList<>(uniqueRules.values());

        // 4. 按优先级排序
        allRules.sort(Comparator.comparingInt(DataScopeRule::getPriority));

        // 5. 构建执行上下文
        ExecutionContext context = buildContext(args);

        // 6. 执行规则并合并结果
        DataScopeResult result = DataScopeResult.allowed();

        for (DataScopeRule rule : allRules) {
            CompiledRule compiled = compile(rule);
            DataScopeResult ruleResult = execute(compiled, context);

            result = result.merge(ruleResult);

            // 如果被拒绝，直接返回
            if (!result.isAllowed()) {
                log.debug("Data scope denied by rule: {}", rule.getRuleId());
                break;
            }
        }

        return result;
    }

    @Override
    public void clearCache() {
        compiledRuleCache.clear();
        cache.clear();
        log.info("Data scope rule cache cleared");
    }

    // ==================== 私有方法 ====================

    private String generateRuleId(Method method, DataScope annotation) {
        return method.getDeclaringClass().getName() + "." + method.getName() + "@" + annotation.dimension();
    }

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
        context.setArgs(args);

        // 注入当前用户
        Serializable userId = dataProvider.getCurrentUserId();
        context.setCurrentUserId(userId);
        context.setVariable("userId", userId);
        context.setVariable("user", dataProvider.getCurrentUser());

        // 注入部门信息
        Serializable deptId = dataProvider.getCurrentDeptId();
        context.setCurrentDeptId(deptId);
        context.setVariable("deptId", deptId);
        context.setVariable("dept", dataProvider.getCurrentDept());

        // 注入角色信息
        Set<Serializable> roleIds = dataProvider.getCurrentRoleIds();
        context.setCurrentRoleIds(roleIds);
        context.setVariable("roleIds", roleIds);
        context.setVariable("roles", dataProvider.getCurrentRoles());

        // 注入方法参数
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                context.setVariable("arg" + i, args[i]);
                context.setVariable("p" + i, args[i]);
            }
        }

        return context;
    }

    // ==================== 维度执行方法 ====================

    private DataScopeResult executeCurrentUser(DataScopeRule rule, ExecutionContext context) {
        Serializable userId = context.getCurrentUserId();
        if (userId == null) {
            return DataScopeResult.denied("User not authenticated");
        }

        String field = resolveField(rule, "create_by");
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " = ?")
            .paramValues(List.of(userId))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeUserAndSubordinate(DataScopeRule rule, ExecutionContext context) {
        Serializable userId = context.getCurrentUserId();
        if (userId == null) {
            return DataScopeResult.denied("User not authenticated");
        }

        // 获取用户ID和下属ID
        String cacheKey = "user:subordinates:" + userId;
        @SuppressWarnings("unchecked")
        Set<Serializable> userIds = cache.get(cacheKey);

        if (userIds == null) {
            userIds = dataProvider.getUserAndSubordinateIds(userId);
            cache.put(cacheKey, userIds, 10, TimeUnit.MINUTES);
        }

        if (userIds == null || userIds.isEmpty()) {
            return DataScopeResult.denied("No subordinate users found");
        }

        String field = resolveField(rule, "create_by");
        String placeholders = userIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(", "));

        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " IN (" + placeholders + ")")
            .paramValues(new ArrayList<>(userIds))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeCurrentDept(DataScopeRule rule, ExecutionContext context) {
        Serializable deptId = context.getCurrentDeptId();
        if (deptId == null) {
            return DataScopeResult.denied("Department not found");
        }

        String field = resolveField(rule, "dept_id");
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " = ?")
            .paramValues(List.of(deptId))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeDeptAndChildren(DataScopeRule rule, ExecutionContext context) {
        Serializable deptId = context.getCurrentDeptId();
        if (deptId == null) {
            return DataScopeResult.denied("Department not found");
        }

        // 从缓存获取子部门
        String cacheKey = "dept:children:" + deptId;
        @SuppressWarnings("unchecked")
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
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeDeptTree(DataScopeRule rule, ExecutionContext context) {
        // 与 DEPT_AND_CHILDREN 类似，但支持层级限制
        return executeDeptAndChildren(rule, context);
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
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeRoleAndInherit(DataScopeRule rule, ExecutionContext context) {
        Set<Serializable> roleIds = dataProvider.getCurrentRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return DataScopeResult.denied("No roles assigned");
        }

        // 获取继承的角色ID
        Set<Serializable> allRoleIds = new HashSet<>(roleIds);
        for (Serializable roleId : roleIds) {
            Set<Serializable> inherited = dataProvider.getInheritedRoleIds(roleId);
            if (inherited != null) {
                allRoleIds.addAll(inherited);
            }
        }

        String field = resolveField(rule, "role_id");
        String placeholders = allRoleIds.stream()
            .map(id -> "?")
            .collect(Collectors.joining(", "));

        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " IN (" + placeholders + ")")
            .paramValues(new ArrayList<>(allRoleIds))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeCurrentRegion(DataScopeRule rule, ExecutionContext context) {
        String regionCode = dataProvider.getCurrentRegionCode();
        if (regionCode == null || regionCode.isEmpty()) {
            return DataScopeResult.denied("Region not found");
        }

        String field = resolveField(rule, "region_code");
        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " = ?")
            .paramValues(List.of(regionCode))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeRegionAndChildren(DataScopeRule rule, ExecutionContext context) {
        String regionCode = dataProvider.getCurrentRegionCode();
        if (regionCode == null || regionCode.isEmpty()) {
            return DataScopeResult.denied("Region not found");
        }

        String cacheKey = "region:children:" + regionCode;
        @SuppressWarnings("unchecked")
        Set<String> regionCodes = cache.get(cacheKey);

        if (regionCodes == null) {
            regionCodes = dataProvider.getRegionChildrenCodes(regionCode);
            regionCodes.add(regionCode);
            cache.put(cacheKey, (Set<Serializable>) (Set<?>) regionCodes, 30, TimeUnit.MINUTES);
        }

        String field = resolveField(rule, "region_code");
        String placeholders = regionCodes.stream()
            .map(code -> "?")
            .collect(Collectors.joining(", "));

        return DataScopeResult.builder()
            .allowed(true)
            .sqlCondition(field + " IN (" + placeholders + ")")
            .paramValues(new ArrayList<>(regionCodes))
            .matchedRuleIds(List.of(rule.getRuleId()))
            .build();
    }

    private DataScopeResult executeDataRange(DataScopeRule rule, ExecutionContext context) {
        // 数据范围权限，从规则参数中获取范围
        return DataScopeResult.allowed();
    }

    private DataScopeResult executeCustom(DataScopeRule rule, ExecutionContext context, CompiledRule compiled) {
        if (rule.getExpression() == null || !rule.getExpression().isValid()) {
            return DataScopeResult.denied("Custom rule without expression");
        }

        try {
            boolean allowed = expressionEngine.evaluate(compiled.getCompiledExpression(), context);
            if (!allowed) {
                return DataScopeResult.denied("Custom expression evaluated to false");
            }
            return DataScopeResult.allowed();
        } catch (Exception e) {
            log.error("Failed to evaluate custom expression", e);
            return DataScopeResult.denied("Custom expression evaluation error: " + e.getMessage());
        }
    }

    private DataScopeResult executeAuto(DataScopeRule rule, ExecutionContext context) {
        // 自动推断：优先使用部门维度
        if (context.getCurrentDeptId() != null) {
            return executeDeptAndChildren(rule, context);
        }
        // 其次是用户维度
        if (context.getCurrentUserId() != null) {
            return executeCurrentUser(rule, context);
        }
        // 默认拒绝
        return DataScopeResult.denied("Cannot auto-detect dimension: no user or dept context");
    }

    private String resolveField(DataScopeRule rule, String defaultField) {
        if (rule.getField() != null && !rule.getField().isEmpty()) {
            return rule.getField();
        }
        return defaultField;
    }
}
