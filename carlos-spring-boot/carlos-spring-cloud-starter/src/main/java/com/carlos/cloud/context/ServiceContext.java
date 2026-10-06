package com.carlos.cloud.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 微服务上下文
 * </p>
 *
 * <p>
 * 用于在服务间传递通用上下文信息，支持：
 * <ul>
 *   <li>租户 ID</li>
 *   <li>用户 ID</li>
 *   <li>请求 ID</li>
 *   <li>自定义属性</li>
 * </ul>
 * </p>
 *
 * <p>
 * 基于 TransmittableThreadLocal 实现，支持线程池场景下的上下文传递。
 * </p>
 *
 * @author carlos
 * @date 2024/01/15
 */
public class ServiceContext {

    /**
     * 上下文键名常量
     */
    public static class Keys {
        public static final String TENANT_ID = "tenantId";
        public static final String USER_ID = "userId";
        public static final String REQUEST_ID = "requestId";
        public static final String USER_NAME = "userName";
        public static final String DEPT_ID = "deptId";
        public static final String CLIENT_IP = "clientIp";
    }

    private static final TransmittableThreadLocal<Map<String, Object>> CONTEXT = new TransmittableThreadLocal<>();

    /**
     * 获取当前上下文
     */
    public static Map<String, Object> get() {
        Map<String, Object> context = CONTEXT.get();
        if (context == null) {
            context = new HashMap<>();
            CONTEXT.set(context);
        }
        return context;
    }

    /**
     * 设置上下文
     */
    public static void set(Map<String, Object> context) {
        CONTEXT.set(context);
    }

    /**
     * 获取指定键的值
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) get().get(key);
    }

    /**
     * 获取指定键的值（带默认值）
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(String key, T defaultValue) {
        return (T) get().getOrDefault(key, defaultValue);
    }

    /**
     * 设置键值对
     */
    public static void set(String key, Object value) {
        get().put(key, value);
    }

    /**
     * 清除上下文
     */
    public static void clear() {
        CONTEXT.remove();
    }

    // ==================== 快捷方法 ====================

    /**
     * 获取租户 ID
     */
    public static String getTenantId() {
        return get(Keys.TENANT_ID);
    }

    /**
     * 设置租户 ID
     */
    public static void setTenantId(String tenantId) {
        set(Keys.TENANT_ID, tenantId);
    }

    /**
     * 获取用户 ID
     */
    public static String getUserId() {
        return get(Keys.USER_ID);
    }

    /**
     * 设置用户 ID
     */
    public static void setUserId(String userId) {
        set(Keys.USER_ID, userId);
    }

    /**
     * 获取用户名
     */
    public static String getUserName() {
        return get(Keys.USER_NAME);
    }

    /**
     * 设置用户名
     */
    public static void setUserName(String userName) {
        set(Keys.USER_NAME, userName);
    }

    /**
     * 获取部门 ID
     */
    public static String getDeptId() {
        return get(Keys.DEPT_ID);
    }

    /**
     * 设置部门 ID
     */
    public static void setDeptId(String deptId) {
        set(Keys.DEPT_ID, deptId);
    }

    /**
     * 获取客户端 IP
     */
    public static String getClientIp() {
        return get(Keys.CLIENT_IP);
    }

    /**
     * 设置客户端 IP
     */
    public static void setClientIp(String clientIp) {
        set(Keys.CLIENT_IP, clientIp);
    }
}
