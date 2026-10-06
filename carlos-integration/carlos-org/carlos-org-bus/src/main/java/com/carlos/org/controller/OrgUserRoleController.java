package com.carlos.org.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgUserRoleConvert;
import com.carlos.org.manager.OrgUserRoleManager;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import com.carlos.org.pojo.param.OrgUserRoleCreateParam;
import com.carlos.org.pojo.param.OrgUserRolePageParam;
import com.carlos.org.pojo.param.OrgUserRoleUpdateParam;
import com.carlos.org.pojo.vo.OrgUserRoleVO;
import com.carlos.org.service.OrgUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 用户角色 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/user/role")
@Tag(name = "用户角色")
public class OrgUserRoleController {

    public static final String BASE_NAME = "用户角色";

    private final OrgUserRoleService userRoleService;

    private final OrgUserRoleManager userRoleManager;


    @Log(
        title = "用户角色",
        logType = "ORG_USER_ROLE_CREATE",
        businessType = BusinessType.GRANT,
        operation = "'新增用户角色授权: 用户 ' + #param.userId + ' -> 角色 ' + #param.roleId",
        targetType = "ORG_USER_ROLE",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user:role', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgUserRoleCreateParam param) {
        OrgUserRoleDTO dto = OrgUserRoleConvert.INSTANCE.toDTO(param);
        userRoleService.addOrgUserRole(dto);
    }


    @Log(
        title = "用户角色",
        logType = "ORG_USER_ROLE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除用户角色授权: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_USER_ROLE",
        riskLevel = 65
    )
    @PreAuthorize("hasPermission('org:user:role', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        userRoleService.deleteOrgUserRole(param.getIds());
    }


    @Log(
        title = "用户角色",
        logType = "ORG_USER_ROLE_UPDATE",
        businessType = BusinessType.GRANT,
        operation = "'修改用户角色授权: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER_ROLE",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user:role', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgUserRoleUpdateParam param) {
        OrgUserRoleDTO dto = OrgUserRoleConvert.INSTANCE.toDTO(param);
        userRoleService.updateOrgUserRole(dto);
    }


    @PreAuthorize("hasPermission('org:user:role', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgUserRoleVO detail(Long id) {
        return OrgUserRoleConvert.INSTANCE.toVO(userRoleManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('org:user:role', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgUserRoleVO> page(OrgUserRolePageParam param) {
        return userRoleManager.getPage(param);
    }
}
