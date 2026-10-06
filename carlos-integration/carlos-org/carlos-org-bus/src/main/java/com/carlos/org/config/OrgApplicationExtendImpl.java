package com.carlos.org.config;

import com.carlos.core.auth.UserContext;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.security.config.SecurityApplicationExtend;
import com.carlos.security.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * <p>
 * 自定义系统扩展实现
 * </p>
 *
 * @author Carlos
 * @date 2023-7-18 23:37:15
 */
@Slf4j
@AllArgsConstructor

public class OrgApplicationExtendImpl implements ApplicationExtend {

    @Override
    public Serializable getUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * 提供登录人上下文
     * <p>
     * 接口默认实现恒返回 null；不覆写的话，任何依赖「当前登录人」的能力都会退化为匿名 ——
     * 首当其冲是 {@code @Log} 审计日志（会记成 principalType=ANONYMOUS、操作人为空）
     * 与 {@code WebRequestContext.getUserContext()}。
     * </p>
     * <p>
     * 身份来自 {@code SecurityContextHolder}，需要 {@code carlos.resource-server.enabled=true}
     * 该过滤器才会注册；取不到时返回 null 而不是抛异常，由调用方决定降级策略。
     * </p>
     */
    @Override
    public UserContext getUserContext() {
        return SecurityApplicationExtend.fromSecurityUser(SecurityUtils.getCurrentUser());
    }
}
