package com.carlos.message.config;

import com.carlos.core.auth.UserContext;
import com.carlos.core.interfaces.ApplicationExtend;
import com.carlos.security.config.SecurityApplicationExtend;
import com.carlos.security.util.SecurityUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 自定义系统扩展实现
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnMissingBean(ApplicationExtend.class)
public class ApplicationExtendImpl implements ApplicationExtend {


    @Override
    public Serializable getUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    /**
     * 提供登录人上下文
     * <p>
     * 接口默认实现恒返回 null；不覆写的话，任何依赖「当前登录人」的能力都会退化为匿名 ——
     * 首当其冲是 {@code @Log} 审计日志（会记成 principalType=ANONYMOUS、操作人为空）。
     * </p>
     * <p>
     * 需要 {@code carlos.resource-server.enabled=true} 才有值；取不到时返回 null，不抛异常。
     * </p>
     */
    @Override
    public UserContext getUserContext() {
        return SecurityApplicationExtend.fromSecurityUser(SecurityUtils.getCurrentUser());
    }

}
