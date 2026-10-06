package com.carlos.datascope.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 数据权限上下文持有者
 * <p>
 * 使用TransmittableThreadLocal支持线程池上下文传递
 *
 * @author Carlos
 * @version 2.0
 */
public class DataScopeContextHolder {

    /**
     * ThreadLocal上下文
     */
    private static final TransmittableThreadLocal<DataScopeContext> CONTEXT = new TransmittableThreadLocal<>();

    /**
     * 获取当前上下文
     *
     * @return DataScopeContext 或 null
     */
    public static DataScopeContext get() {
        return CONTEXT.get();
    }

    /**
     * 设置上下文
     *
     * @param context 上下文
     */
    public static void set(DataScopeContext context) {
        CONTEXT.set(context);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    /**
     * 检查是否有上下文
     *
     * @return true 有上下文
     */
    public static boolean hasContext() {
        return CONTEXT.get() != null;
    }

    /**
     * 获取或创建上下文
     *
     * @return DataScopeContext
     */
    public static DataScopeContext getOrCreate() {
        DataScopeContext context = CONTEXT.get();
        if (context == null) {
            context = DataScopeContext.create();
            CONTEXT.set(context);
        }
        return context;
    }
}
