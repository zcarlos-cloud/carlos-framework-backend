package com.carlos.datascope.core.engine;

import com.carlos.datascope.annotation.DataScope;
import com.carlos.datascope.core.model.DataScopeResult;
import com.carlos.datascope.core.model.DataScopeRule;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 数据权限规则引擎接口
 * <p>
 * 定义规则解析、编译、执行的完整流程
 *
 * @author Carlos
 * @version 2.0
 */
public interface DataScopeRuleEngine {

    /**
     * 从注解解析规则
     *
     * @param method      方法
     * @param annotations 注解数组
     * @return 规则列表
     */
    List<DataScopeRule> parseFromAnnotations(Method method, DataScope[] annotations);

    /**
     * 从配置加载规则
     *
     * @param mapperMethod Mapper方法全名（类名.方法名）
     * @return 规则列表
     */
    List<DataScopeRule> loadFromConfig(String mapperMethod);

    /**
     * 编译规则
     *
     * @param rule 原始规则
     * @return 编译后的规则
     */
    CompiledRule compile(DataScopeRule rule);

    /**
     * 执行规则
     *
     * @param compiledRule 编译后的规则
     * @param context      执行上下文
     * @return 执行结果
     */
    DataScopeResult execute(CompiledRule compiledRule, ExecutionContext context);

    /**
     * 完整处理流程
     *
     * @param method      方法
     * @param annotations 注解数组
     * @param args        方法参数
     * @return 处理结果
     */
    DataScopeResult process(Method method, DataScope[] annotations, Object[] args);

    /**
     * 清除规则缓存
     */
    void clearCache();
}
