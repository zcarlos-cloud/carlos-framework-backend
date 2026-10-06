package com.carlos.system.config;

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
 * @author carlos
 * @date 2022-11-8 19:30:24
 */
@Slf4j
@AllArgsConstructor
public class SystemApplicationExtendImpl implements ApplicationExtend {

    @Override
    public UserContext getUserContext() {
        return SecurityApplicationExtend.fromSecurityUser(SecurityUtils.getCurrentUser());
    }
}
