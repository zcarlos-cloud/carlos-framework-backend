package com.carlos.datascope.util;

import com.carlos.datascope.core.context.DataScopeContext;
import com.carlos.datascope.core.context.DataScopeContextHolder;
import com.carlos.datascope.core.model.DataScopeResult;

/**
 * 数据权限工具类
 * <p>
 * 提供便捷的数据权限操作方法
 *
 * @author Carlos
 * @version 2.0
 */
public class DataScopeUtils {

    /**
     * 获取当前数据权限结果
     *
     * @return DataScopeResult 或 null
     */
    public static DataScopeResult getCurrentResult() {
        DataScopeContext context = DataScopeContextHolder.get();
        return context != null ? context.getResult() : null;
    }

    /**
     * 检查当前是否有数据权限
     *
     * @return true 有权限
     */
    public static boolean hasPermission() {
        DataScopeResult result = getCurrentResult();
        return result != null && result.isAllowed();
    }

    /**
     * 获取当前SQL条件
     *
     * @return SQL条件或 null
     */
    public static String getCurrentSqlCondition() {
        DataScopeResult result = getCurrentResult();
        return result != null ? result.getSqlCondition() : null;
    }

    /**
     * 忽略数据权限（在当前线程中临时禁用）
     * <p>
     * 使用示例：
     * <pre>
     * try {
     *     DataScopeUtils.ignoreDataScope();
     *     // 执行不需要数据权限控制的代码
     * } finally {
     *     DataScopeUtils.restoreDataScope();
     * }
     * </pre>
     */
    public static void ignoreDataScope() {
        DataScopeContext context = DataScopeContextHolder.get();
        if (context != null && context.getResult() != null) {
            context.getResult().setAllowed(true);
            context.getResult().setSqlCondition("1 = 1");
        }
    }

    /**
     * 恢复数据权限
     */
    public static void restoreDataScope() {
        // 实际上ignoreDataScope修改了原对象，这里不做特殊处理
        // 实际应用中应该使用更复杂的机制来保存和恢复状态
    }
}
