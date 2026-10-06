package com.carlos.org.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgDepartmentRoleConvert;
import com.carlos.org.manager.OrgDepartmentRoleManager;
import com.carlos.org.pojo.dto.OrgDepartmentRoleDTO;
import com.carlos.org.pojo.param.OrgDepartmentRoleCreateParam;
import com.carlos.org.pojo.param.OrgDepartmentRolePageParam;
import com.carlos.org.pojo.param.OrgDepartmentRoleUpdateParam;
import com.carlos.org.pojo.vo.OrgDepartmentRoleVO;
import com.carlos.org.service.OrgDepartmentRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 部门角色 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/department/role")
@Tag(name = "部门角色")
public class OrgDepartmentRoleController {

    public static final String BASE_NAME = "部门角色";

    private final OrgDepartmentRoleService departmentRoleService;

    private final OrgDepartmentRoleManager departmentRoleManager;


    @Log(
        title = "部门角色",
        logType = "ORG_DEPARTMENT_ROLE_CREATE",
        businessType = BusinessType.GRANT,
        operation = "'新增部门角色授权: 部门 ' + #param.deptId + ' -> 角色 ' + #param.roleId",
        targetType = "ORG_DEPARTMENT_ROLE",
        riskLevel = 55
    )
    @PreAuthorize("hasPermission('org:department:role', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgDepartmentRoleCreateParam param) {
        OrgDepartmentRoleDTO dto = OrgDepartmentRoleConvert.INSTANCE.toDTO(param);
        departmentRoleService.addOrgDepartmentRole(dto);
    }


    @Log(
        title = "部门角色",
        logType = "ORG_DEPARTMENT_ROLE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除部门角色授权: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_DEPARTMENT_ROLE",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:department:role', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        departmentRoleService.deleteOrgDepartmentRole(param.getIds());
    }


    @Log(
        title = "部门角色",
        logType = "ORG_DEPARTMENT_ROLE_UPDATE",
        businessType = BusinessType.GRANT,
        operation = "'修改部门角色授权: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_DEPARTMENT_ROLE",
        riskLevel = 55
    )
    @PreAuthorize("hasPermission('org:department:role', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgDepartmentRoleUpdateParam param) {
        OrgDepartmentRoleDTO dto = OrgDepartmentRoleConvert.INSTANCE.toDTO(param);
        departmentRoleService.updateOrgDepartmentRole(dto);
    }


    @PreAuthorize("hasPermission('org:department:role', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgDepartmentRoleVO detail(Long id) {
        return OrgDepartmentRoleConvert.INSTANCE.toVO(departmentRoleManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:department:role', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgDepartmentRoleVO> page(OrgDepartmentRolePageParam param) {
        return departmentRoleManager.getPage(param);
    }
}
