package com.carlos.gateway.config;

import com.carlos.core.auth.UserContext;
import com.carlos.core.base.DepartmentInfo;
import com.carlos.core.base.RegionInfo;
import com.carlos.core.base.UserInfo;
import com.carlos.core.interfaces.ApplicationExtend;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

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
public class GatewatApplicationExtendImpl implements ApplicationExtend {


    @Override
    public String getUserId() {
        UserContext userContext = getUserContext();
        if (userContext == null) {
            return null;
        }
        Serializable userId = userContext.getUserId();
        if (userId == null) {
            return null;
        }
        return (String) userId;
    }

    @Override
    public @Nullable
    UserContext getUserContext() {
        try {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public DepartmentInfo getDepartmentByCode(String code, int limit) {

        return null;
    }


    @Override
    public UserInfo getUserById(Serializable userId) {

        return null;
    }

    @Override
    public RegionInfo getRegionInfo(String regionCode, Integer limit) {

        return null;
    }

}
