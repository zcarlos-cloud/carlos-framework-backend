package com.carlos.datascope.interceptor;

import com.carlos.datascope.core.context.DataScopeContext;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.model.DataScopeResult;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SetOperationList;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * MyBatis 数据权限拦截器
 * <p>
 * 拦截SQL执行，自动注入数据权限条件
 *
 * @author Carlos
 * @version 2.0
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
        String newSql;
        try {
            newSql = injectCondition(originalSql, sqlCondition);
        } catch (Exception e) {
            log.error("Failed to inject data scope condition", e);
            return invocation.proceed();
        }

        // 6. 重建BoundSql和MappedStatement
        BoundSql newBoundSql = rebuildBoundSql(ms, boundSql, newSql, result);
        MappedStatement newMs = rebuildMappedStatement(ms, newBoundSql);

        // 7. 替换参数并执行
        invocation.getArgs()[0] = newMs;

        log.debug("Data scope SQL injected:\nOriginal: {}\nNew: {}", originalSql, newSql);

        return invocation.proceed();
    }

    /**
     * 注入权限条件到SQL
     */
    private String injectCondition(String originalSql, String condition) throws Exception {
        Statement statement = CCJSqlParserUtil.parse(originalSql);

        if (statement instanceof Select select) {
            processSelect(select, condition);
            return select.toString();
        }

        // UPDATE 和 DELETE 暂不处理
        return originalSql;
    }

    /**
     * 处理Select
     */
    private void processSelect(Select select, String condition) throws Exception {
        if (select instanceof PlainSelect plainSelect) {

            Expression where = plainSelect.getWhere();
            Expression dataScopeExpr = CCJSqlParserUtil.parseCondExpression(condition);

            if (where == null) {
                plainSelect.setWhere(dataScopeExpr);
            } else {
                AndExpression and = new AndExpression(where, dataScopeExpr);
                plainSelect.setWhere(and);
            }
        } else if (select instanceof SetOperationList setOpList) {
            for (Select sel : setOpList.getSelects()) {
                processSelect(sel, condition);
            }
        }
    }

    /**
     * 重建BoundSql
     */
    private BoundSql rebuildBoundSql(MappedStatement ms, BoundSql originalBoundSql,
                                     String newSql, DataScopeResult result) {
        List<ParameterMapping> parameterMappings = new ArrayList<>(originalBoundSql.getParameterMappings());

        // 添加数据权限参数
        List<Object> paramValues = result.getParamValues();
        for (int i = 0; i < paramValues.size(); i++) {
            ParameterMapping.Builder builder = new ParameterMapping.Builder(
                ms.getConfiguration(),
                "_ds_param_" + i,
                paramValues.get(i) != null ? paramValues.get(i).getClass() : Object.class
            );
            parameterMappings.add(builder.build());
        }

        BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), newSql, parameterMappings,
            originalBoundSql.getParameterObject());

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
