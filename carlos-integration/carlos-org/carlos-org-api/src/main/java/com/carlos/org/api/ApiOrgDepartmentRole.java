package com.carlos.org.api;

import com.carlos.core.response.Result;
import com.carlos.org.ServiceNameConstant;
import com.carlos.org.api.fallback.ApiOrgDepartmentRoleFallbackFactory;
import com.carlos.org.api.pojo.ao.OrgDepartmentRoleAO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * <p>
 * 部门角色 feign 提供接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@FeignClient(value = ServiceNameConstant.SERVICE_NAME, contextId = "departmentRole", path = "/api/org/department/role", fallbackFactory = ApiOrgDepartmentRoleFallbackFactory.class)
public interface ApiOrgDepartmentRole {

    /**
     * 根据部门ID查询角色关联列表
     *
     * @param deptId 部门ID
     * @return 角色关联列表
     */
    @GetMapping("/listByDeptId")
    Result<List<OrgDepartmentRoleAO>> listByDeptId(@RequestParam("deptId") Long deptId);

}
