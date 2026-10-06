package com.carlos.datascope.audit;

import com.carlos.datascope.core.model.DataScopeResult;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * 数据权限审计日志记录器
 * <p>
 * 记录数据权限的访问日志，用于安全审计
 *
 * @author Carlos
 * @version 2.0
 */
@Slf4j
public class DataScopeAuditLogger {

    /**
     * 记录成功的访问
     *
     * @param method   方法
     * @param result   结果
     * @param duration 执行时长(ms)
     */
    public void logSuccess(Method method, DataScopeResult result, long duration) {
        log.debug("[DataScope Audit] SUCCESS - method={}, rules={}, duration={}ms",
            method.getName(),
            result.getMatchedRuleIds(),
            duration
        );

        // TODO: 可以扩展为异步写入数据库或消息队列
    }

    /**
     * 记录被拒绝的访问
     *
     * @param method   方法
     * @param result   结果
     * @param duration 执行时长(ms)
     */
    public void logDenied(Method method, DataScopeResult result, long duration) {
        log.warn("[DataScope Audit] DENIED - method={}, reason={}, duration={}ms",
            method.getName(),
            result.getDenyReason(),
            duration
        );

        // TODO: 可以扩展为异步写入数据库或消息队列
    }
}
