package com.carlos.audit.config;

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
 * @date 2026年3月6日 下午9:31:12
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
     * 接口默认实现恒返回 null；不覆写的话，任何依赖「当前登录人」的能力都会退化为匿名。
     * 本模块的 {@code AuditSelfLogHelper} 自行读 {@code SecurityUtils}，不受影响；
     * 此方法主要服务于 {@code @Log} 与 {@code WebRequestContext.getUserContext()}。
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
