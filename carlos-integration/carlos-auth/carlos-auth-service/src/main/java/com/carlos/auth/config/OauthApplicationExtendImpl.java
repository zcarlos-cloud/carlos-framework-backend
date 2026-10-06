package com.carlos.auth.config;

import com.carlos.core.interfaces.ApplicationExtend;
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
 * @date 2025-3-12 14:00:14
 */
@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnMissingBean(ApplicationExtend.class)
public class OauthApplicationExtendImpl implements ApplicationExtend {


    @Override
    public Serializable getUserId() {
        return SecurityUtils.getCurrentUserId();
    }

}
