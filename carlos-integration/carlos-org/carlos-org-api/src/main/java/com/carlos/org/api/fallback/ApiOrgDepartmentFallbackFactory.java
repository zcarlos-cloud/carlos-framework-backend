package com.carlos.org.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartment;
import com.carlos.org.api.pojo.ao.OrgDepartmentAO;
import com.carlos.org.api.pojo.ao.OrgUserAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;



/**
 * <p>
 * 部门 api 降级
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Slf4j
public class ApiOrgDepartmentFallbackFactory implements FallbackFactory<ApiOrgDepartment> {

    @Override
    public ApiOrgDepartment create(Throwable throwable) {
        String message = throwable.getMessage();
        log.error("部门服务调用失败: message:{}", message);
        return new ApiOrgDepartment() {

            @Override
            public Result<OrgDepartmentAO> getDepartmentById(Long id) {
                return Result.success(null);
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByDeptId(Long deptId) {
                log.error("部门服务调用失败(listUsersByDeptId): deptId:{}, message:{}", deptId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByDeptIdAndTenant(Long deptId, Long tenantId) {
                log.error("部门服务调用失败(listUsersByDeptIdAndTenant): deptId:{}, tenantId:{}, message:{}",
                    deptId, tenantId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByDeptIdRecursive(Long deptId) {
                log.error("部门服务调用失败(listUsersByDeptIdRecursive): deptId:{}, message:{}", deptId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgUserAO>> listUsersByDeptIdRecursiveAndTenant(Long deptId, Long tenantId) {
                log.error("部门服务调用失败(listUsersByDeptIdRecursiveAndTenant): deptId:{}, tenantId:{}, message:{}",
                    deptId, tenantId, message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgDepartmentAO>> listAllDepartmentOptions() {
                log.error("部门服务调用失败(listAllDepartmentOptions): message:{}", message);
                return Result.success(Collections.emptyList());
            }

            @Override
            public Result<List<OrgDepartmentAO>> listDepartmentOptions(Collection<Long> ids) {
                log.error("部门服务调用失败(listDepartmentOptions): size:{}, message:{}",
                    ids == null ? 0 : ids.size(), message);
                return Result.success(Collections.emptyList());
            }
        };
    }


    // 将此段配置放入配置类中进行Bean加载
    // @Bean
    // public ApiOrgDepartmentFallbackFactory departmentFallbackFactory() {
    //     return new ApiOrgDepartmentFallbackFactory();
    // }
}