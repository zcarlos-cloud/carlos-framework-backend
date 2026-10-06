package com.carlos.org.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUserRole;
import com.carlos.org.api.pojo.ao.OrgUserRoleAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 用户角色 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgUserRoleFallbackFactory implements FallbackFactory<ApiOrgUserRole> {

    @Override
    public ApiOrgUserRole create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("用户角色服务调用失败: message:{}", message);
        return new ApiOrgUserRole() {

            @Override
            public Result<List<OrgUserRoleAO>> listByUserId(Long userId) {
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgUserRoleFallbackFactory userRoleFallbackFactory() {
    //     return new ApiOrgUserRoleFallbackFactory();
    // }
}
