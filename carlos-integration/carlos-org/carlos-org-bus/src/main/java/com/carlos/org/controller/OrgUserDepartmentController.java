package com.carlos.org.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgUserDepartmentConvert;
import com.carlos.org.manager.OrgUserDepartmentManager;
import com.carlos.org.pojo.dto.OrgUserDepartmentDTO;
import com.carlos.org.pojo.param.OrgUserDepartmentCreateParam;
import com.carlos.org.pojo.param.OrgUserDepartmentPageParam;
import com.carlos.org.pojo.param.OrgUserDepartmentUpdateParam;
import com.carlos.org.pojo.vo.OrgUserDepartmentVO;
import com.carlos.org.service.OrgUserDepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 用户部门 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/user/department")
@Tag(name = "用户部门")
public class OrgUserDepartmentController {

    public static final String BASE_NAME = "用户部门";

    private final OrgUserDepartmentService userDepartmentService;

    private final OrgUserDepartmentManager userDepartmentManager;


    @Log(
        title = "用户部门",
        logType = "ORG_USER_DEPARTMENT_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增用户部门归属: 用户 ' + #param.userId + ' -> 部门 ' + #param.deptId",
        targetType = "ORG_USER_DEPARTMENT",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:user:department', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgUserDepartmentCreateParam param) {
        OrgUserDepartmentDTO dto = OrgUserDepartmentConvert.INSTANCE.toDTO(param);
        userDepartmentService.addOrgUserDepartment(dto);
    }


    @Log(
        title = "用户部门",
        logType = "ORG_USER_DEPARTMENT_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除用户部门归属: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_USER_DEPARTMENT",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user:department', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        userDepartmentService.deleteOrgUserDepartment(param.getIds());
    }


    @Log(
        title = "用户部门",
        logType = "ORG_USER_DEPARTMENT_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改用户部门归属: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER_DEPARTMENT",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:user:department', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgUserDepartmentUpdateParam param) {
        OrgUserDepartmentDTO dto = OrgUserDepartmentConvert.INSTANCE.toDTO(param);
        userDepartmentService.updateOrgUserDepartment(dto);
    }


    @PreAuthorize("hasPermission('org:user:department', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgUserDepartmentVO detail(Long id) {
        return OrgUserDepartmentConvert.INSTANCE.toVO(userDepartmentManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:user:department', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgUserDepartmentVO> page(OrgUserDepartmentPageParam param) {
        return userDepartmentManager.getPage(param);
    }
}
