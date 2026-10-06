package com.carlos.org.apiimpl;


import com.carlos.core.response.Result;
import com.carlos.org.api.ApiOrgDepartmentRole;
import com.carlos.org.api.pojo.ao.OrgDepartmentRoleAO;
import com.carlos.org.convert.OrgDepartmentRoleConvert;
import com.carlos.org.manager.OrgDepartmentRoleManager;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 部门角色 api接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/org/department/role")
@Tag(name = "部门角色Feign接口")
public class ApiOrgDepartmentRoleImpl implements ApiOrgDepartmentRole {

    private final OrgDepartmentRoleManager departmentRoleManager;

    @Override
    public Result<List<OrgDepartmentRoleAO>> listByDeptId(@RequestParam("deptId") Long deptId) {
        List<OrgDepartmentRoleDTO> dtoList = departmentRoleManager.listByDeptId(deptId);
        return Result.success(OrgDepartmentRoleConvert.INSTANCE.toAOList(dtoList));
    }
}
