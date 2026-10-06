package com.carlos.monitor.db.proxy;

import cn.hutool.core.util.StrUtil;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLExplainStatement;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.visitor.SQLASTVisitorAdapter;
import com.alibaba.druid.sql.visitor.SQLEvalVisitorUtils;
import com.carlos.monitor.collect.spi.SqlFingerprintUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <p>
 * SQL 安全守卫（Druid AST 解析，严禁正则判断 SQL 类型）
 * 校验规则（设计 3.4/2.4）：
 * <ol>
 *   <li>单语句校验（多语句拒绝）</li>
 *   <li>语句类型白名单：只读模式 SELECT/EXPLAIN；写模式追加 UPDATE/DELETE</li>
 *   <li>UPDATE/DELETE 必须含非恒真 WHERE（SQLEvalVisitor 求值恒真/空条件拒绝）</li>
 *   <li>黑名单：SELECT INTO（OUTFILE）、危险函数（pg_terminate_backend/xp_cmdshell 等）、
 *       系统 schema（pg_catalog/information_schema）写操作；
 *       DDL/DCL/CALL/LOAD/COPY 等不在白名单内，天然拒绝</li>
 * </ol>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Slf4j
@Component
public class SqlGuard {

    /**
     * SQL 模式
     */
    public enum SqlMode {
        /**
         * 只读：SELECT/EXPLAIN
         */
        READONLY,
        /**
         * 写：追加 UPDATE/DELETE
         */
        WRITE
    }

    /**
     * SQL 类型（白名单内）
     */
    public enum SqlType {
        SELECT, EXPLAIN, UPDATE, DELETE
    }

    /**
     * 校验结果
     */
    public record GuardResult(boolean pass, SqlType sqlType, String normalized, String blockReason) {

        public static GuardResult blocked(String reason) {
            return new GuardResult(false, null, null, reason);
        }

        public static GuardResult passed(SqlType sqlType, String normalized) {
            return new GuardResult(true, sqlType, normalized, null);
        }
    }

    /**
     * 危险函数黑名单（小写比对）
     */
    private static final Set<String> DANGEROUS_FUNCTIONS = Set.of(
        "pg_terminate_backend", "pg_cancel_backend", "pg_reload_conf", "pg_rotate_logfile",
        "xp_cmdshell", "xp_dirtree", "xp_regread", "xp_regwrite",
        "load_file", "sys_eval", "sys_exec", "sys_get",
        "utl_file", "utl_http", "dbms_export_extension",
        "set_config", "lo_import", "lo_export", "dblink", "dblink_exec");

    /**
     * 禁止写操作的系统 schema
     */
    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
        "pg_catalog", "information_schema", "mysql", "sys", "performance_schema");

    /**
     * 校验 SQL
     *
     * @param sql    原始 SQL
     * @param dbType 数据库类型（PG/MySQL 方言解析）
     * @param mode   只读/写模式
     * @return 校验结果
     */
    public GuardResult check(String sql, DbType dbType, SqlMode mode) {
        if (StrUtil.isBlank(sql)) {
            return GuardResult.blocked("SQL 不能为空");
        }
        // 1. 解析 + 单语句校验
        List<SQLStatement> statements;
        try {
            statements = SQLUtils.parseStatements(sql, dbType);
        } catch (Exception e) {
            return GuardResult.blocked("SQL 解析失败：" + StrUtil.maxLength(e.getMessage(), 200));
        }
        if (statements.isEmpty()) {
            return GuardResult.blocked("SQL 解析结果为空");
        }
        if (statements.size() > 1) {
            return GuardResult.blocked("只允许执行单条语句，检测到 " + statements.size() + " 条");
        }
        SQLStatement statement = statements.getFirst();

        // 2. 语句类型白名单
        SqlType sqlType = classify(statement);
        if (sqlType == null) {
            return GuardResult.blocked("语句类型不在白名单（DDL/DCL/CALL/LOAD/COPY 等一律拒绝）："
                + statement.getClass().getSimpleName());
        }
        if (mode == SqlMode.READONLY && sqlType != SqlType.SELECT && sqlType != SqlType.EXPLAIN) {
            return GuardResult.blocked("只读模式仅允许 SELECT/EXPLAIN");
        }
        if (mode == SqlMode.WRITE && sqlType == SqlType.EXPLAIN) {
            return GuardResult.blocked("写模式不执行 EXPLAIN，请用只读模式");
        }

        // 3. UPDATE/DELETE 恒真/空 WHERE 校验
        if (sqlType == SqlType.UPDATE || sqlType == SqlType.DELETE) {
            SQLExpr where = sqlType == SqlType.UPDATE
                ? ((SQLUpdateStatement) statement).getWhere()
                : ((SQLDeleteStatement) statement).getWhere();
            if (where == null) {
                return GuardResult.blocked(sqlType + " 必须包含 WHERE 条件（拒绝全表" + sqlType + "）");
            }
            if (isAlwaysTrue(dbType, where)) {
                return GuardResult.blocked(sqlType + " 的 WHERE 条件恒真（如 1=1），拒绝执行");
            }
        }

        // 4. SELECT INTO（OUTFILE/SELECT INTO 写文件）拒绝
        if (sqlType == SqlType.SELECT && containsSelectInto(statement)) {
            return GuardResult.blocked("禁止 SELECT ... INTO（OUTFILE/新表写入）");
        }

        // 5. 危险函数黑名单
        String dangerousFunction = findDangerousFunction(statement);
        if (dangerousFunction != null) {
            return GuardResult.blocked("命中危险函数黑名单：" + dangerousFunction);
        }

        // 6. 系统 schema 写操作拒绝
        if ((sqlType == SqlType.UPDATE || sqlType == SqlType.DELETE) && touchesSystemSchema(statement)) {
            return GuardResult.blocked("禁止对系统 schema（pg_catalog/information_schema/mysql/sys）执行写操作");
        }

        return GuardResult.passed(sqlType, SqlFingerprintUtil.normalize(sql, dbType));
    }

    /**
     * 语句类型分类（AST 判定，非正则）
     */
    private SqlType classify(SQLStatement statement) {
        if (statement instanceof SQLSelectStatement) {
            return SqlType.SELECT;
        }
        if (statement instanceof SQLExplainStatement) {
            return SqlType.EXPLAIN;
        }
        if (statement instanceof SQLUpdateStatement) {
            return SqlType.UPDATE;
        }
        if (statement instanceof SQLDeleteStatement) {
            return SqlType.DELETE;
        }
        return null;
    }

    /**
     * WHERE 恒真检测：Druid SQLEvalVisitor 常量求值，能求出 Boolean.TRUE 即恒真；
     * 求值失败/非布尔结果视为非恒真（保守放行，交给数据库执行）
     */
    private boolean isAlwaysTrue(DbType dbType, SQLExpr where) {
        try {
            Object value = SQLEvalVisitorUtils.eval(dbType, where);
            return Boolean.TRUE.equals(value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 是否包含 SELECT INTO（遍历全部查询块）
     */
    private boolean containsSelectInto(SQLStatement statement) {
        boolean[] found = {false};
        statement.accept(new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLSelectQueryBlock queryBlock) {
                if (queryBlock.getInto() != null) {
                    found[0] = true;
                }
                return !found[0];
            }
        });
        return found[0];
    }

    /**
     * 危险函数扫描（遍历全部函数调用）
     */
    private String findDangerousFunction(SQLStatement statement) {
        String[] found = {null};
        statement.accept(new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLMethodInvokeExpr method) {
                String name = method.getMethodName();
                if (name != null && DANGEROUS_FUNCTIONS.contains(name.toLowerCase(Locale.ROOT))) {
                    found[0] = name.toLowerCase(Locale.ROOT);
                    return false;
                }
                return super.visit(method);
            }
        });
        return found[0];
    }

    /**
     * 写操作是否触碰系统 schema（表源 schema 名校验）
     */
    private boolean touchesSystemSchema(SQLStatement statement) {
        boolean[] found = {false};
        statement.accept(new SQLASTVisitorAdapter() {
            @Override
            public boolean visit(SQLExprTableSource tableSource) {
                String schema = tableSource.getSchema();
                if (schema != null && SYSTEM_SCHEMAS.contains(schema.toLowerCase(Locale.ROOT))) {
                    found[0] = true;
                    return false;
                }
                return super.visit(tableSource);
            }
        });
        return found[0];
    }
}
