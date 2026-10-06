package com.carlos.datasource.scope;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import com.carlos.datascope.DataScopeHandler;
import com.carlos.datascope.DataScopeInfo;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.HexValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * mybatis数据权限处理器
 *
 * @author Carlos
 * @date 2022/11/21 13:22
 */
@RequiredArgsConstructor
public class CustomDataPermissionHandler implements DataPermissionHandler {

    private final DataScopeHandler scopeHandler;

    /**
     * @param where             原SQL Where 条件表达式
     * @param mappedStatementId Mapper接口方法ID
     * @return 结果
     */
    @Override
    public Expression getSqlSegment(Expression where, final String mappedStatementId) {
        final DataScopeInfo scope = this.scopeHandler.getScopeInfo(mappedStatementId);
        if (scope == null) {
            return where;
        }

        final Set<Serializable> values = scope.getItems();
        if (CollectionUtil.isEmpty(values)) {
            // 权限值为空 代表没有权限
            return new AndExpression(where, new HexValue(" 1 = 0 "));
        }

        if (where == null) {
            where = new HexValue(" 1 = 1 ");
        }

        final Column column = new Column(scope.getColumn());
        if (values.size() == 1) {
            final EqualsTo selfEqualsTo = new EqualsTo();
            selfEqualsTo.setLeftExpression(column);
            selfEqualsTo.setRightExpression(new StringValue(StrUtil.join(StrUtil.COMMA, values)));
            return new AndExpression(where, selfEqualsTo);
        } else {
            // 使用 ExpressionList.withExpressions() 替代已弃用的构造函数
            final ExpressionList itemsList = new ExpressionList<>();
            itemsList.setExpressions(
                values.stream().map(i -> new StringValue((String) i)).collect(Collectors.toList()));
            final InExpression deptInExpression = new InExpression(column, itemsList);
            return new AndExpression(where, deptInExpression);
        }
    }
}
