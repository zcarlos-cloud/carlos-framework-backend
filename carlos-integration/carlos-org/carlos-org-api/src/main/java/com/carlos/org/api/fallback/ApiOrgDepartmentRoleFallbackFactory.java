package com.carlos.org.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartmentRole;
import com.carlos.org.api.pojo.ao.OrgDepartmentRoleAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 部门角色 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgDepartmentRoleFallbackFactory implements FallbackFactory<ApiOrgDepartmentRole> {

    @Override
    public ApiOrgDepartmentRole create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("部门角色服务调用失败: message:{}", message);
        return new ApiOrgDepartmentRole() {

            @Override
            public Result<List<OrgDepartmentRoleAO>> listByDeptId(Long deptId) {
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgDepartmentRoleFallbackFactory departmentRoleFallbackFactory() {
    //     return new ApiOrgDepartmentRoleFallbackFactory();
    // }
}
