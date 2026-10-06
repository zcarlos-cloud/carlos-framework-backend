package com.carlos.org.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgUserDepartment;
import com.carlos.org.api.pojo.ao.OrgUserDepartmentAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 用户部门 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgUserDepartmentFallbackFactory implements FallbackFactory<ApiOrgUserDepartment> {

    @Override
    public ApiOrgUserDepartment create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("用户部门服务调用失败: message:{}", message);
        return new ApiOrgUserDepartment() {

            @Override
            public Result<List<OrgUserDepartmentAO>> listByUserId(Long userId) {
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgUserDepartmentFallbackFactory userDepartmentFallbackFactory() {
    //     return new ApiOrgUserDepartmentFallbackFactory();
    // }
}
