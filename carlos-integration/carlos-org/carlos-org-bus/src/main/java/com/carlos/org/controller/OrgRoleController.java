package com.carlos.org.controller;


import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgRoleConvert;
import com.carlos.org.manager.OrgRoleManager;
import com.carlos.org.pojo.dto.OrgRoleDTO;
import com.carlos.org.pojo.dto.OrgRoleUserDTO;
import com.carlos.org.pojo.param.*;
import com.carlos.org.pojo.vo.OrgRoleDetailVO;
import com.carlos.org.pojo.vo.OrgRoleUserVO;
import com.carlos.org.pojo.vo.OrgRoleVO;
import com.carlos.org.service.OrgRoleOperationService;
import com.carlos.org.service.OrgRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


/**
 * <p>
 * 角色 rest服务接口
 * </p>
 * <p>实现所有角色管理需求</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/role")
@Tag(name = "角色管理")
public class OrgRoleController {

    public static final String BASE_NAME = "角色";

    private final OrgRoleService roleService;

    private final OrgRoleManager roleManager;

    private final OrgRoleOperationService roleOperationService;


    /**
     * 新增角色
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增角色: ' + #param.roleName",
        targetType = "ORG_ROLE",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:role', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgRoleCreateParam param) {
        OrgRoleDTO dto = OrgRoleConvert.INSTANCE.toDTO(param);
        roleService.addOrgRole(dto);
    }


    /**
     * 删除角色
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除角色: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_ROLE",
        riskLevel = 65
    )
    @PreAuthorize("hasPermission('org:role', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        roleService.deleteOrgRole(param.getIds());
    }


    /**
     * 编辑角色
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改角色: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_ROLE",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:role', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgRoleUpdateParam param) {
        OrgRoleDTO dto = OrgRoleConvert.INSTANCE.toDTO(param);
        roleService.updateOrgRole(dto);
    }


    /**
     * 角色列表
     */
    @PreAuthorize("hasPermission('org:role', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgRoleVO> page(OrgRolePageParam param) {
        Paging<OrgRoleDTO> dtoPaging = roleManager.getPage(param);
        // 手动转换分页结果
        Paging<OrgRoleVO> voPaging = new Paging<>();
        voPaging.setCurrent(dtoPaging.getCurrent());
        voPaging.setSize(dtoPaging.getSize());
        voPaging.setTotal(dtoPaging.getTotal());
        voPaging.setPages(dtoPaging.getPages());
        voPaging.setRecords(OrgRoleConvert.INSTANCE.toVOList(dtoPaging.getRecords()));
        return voPaging;
    }

    /**
     * 启用角色下拉选项（S3 公告发布范围等场景使用，非分页避免截断）
     */
    @PreAuthorize("hasPermission('org:role', 'view')")
    @GetMapping("options")
    @Operation(summary = "启用" + BASE_NAME + "下拉选项")
    public List<OrgRoleVO> options() {
        return OrgRoleConvert.INSTANCE.toVOList(roleManager.listEnabled());
    }


    /**
     * 角色详情
     */
    @PreAuthorize("hasPermission('org:role', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgRoleDetailVO detail(@RequestParam Long id) {
        OrgRoleDTO dto = roleManager.getDtoById(id);
        return OrgRoleConvert.INSTANCE.toDetailVO(dto);
    }


    /**
     * 启用/禁用角色
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_CHANGE_STATE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'变更角色状态: ' + #param.id + ' -> ' + #param.roleStatus",
        targetId = "#param.id",
        targetType = "ORG_ROLE",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('org:role', 'changeState')")
    @PostMapping("changeState")
    @Operation(summary = "启用/禁用" + BASE_NAME)
    public void changeState(@RequestBody @Validated OrgRoleChangeStateParam param) {
        // param.getId() 为 Serializable（雪花id字符串），具体转换见 OrgRoleService.changeState
        roleService.changeState(param.getId(), param.getRoleStatus());
    }


    /**
     * 配置数据权限
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_SET_DATA_SCOPE",
        businessType = BusinessType.AUTH,
        operation = "'设置角色数据范围: ' + #param.roleId + ' -> ' + #param.dataScope",
        targetId = "#param.roleId",
        targetType = "ORG_ROLE",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:role', 'setDataScope')")
    @PostMapping("setDataScope")
    @Operation(summary = "配置数据权限")
    public void setDataScope(@RequestBody @Validated OrgRoleSetDataScopeParam param) {
        roleService.setDataScope(param);
    }


    /**
     * 配置角色权限
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_ASSIGN_PERMISSIONS",
        businessType = BusinessType.GRANT,
        operation = "'分配角色操作权限: ' + #param.roleId",
        targetId = "#param.roleId",
        targetType = "ORG_ROLE",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('org:role', 'assignPermissions')")
    @PostMapping("assignPermissions")
    @Operation(summary = "配置" + BASE_NAME + "权限")
    public void assignPermissions(@RequestBody @Validated OrgRoleAssignPermissionParam param) {
        roleService.assignPermissions(param);
    }


    /**
     * 查看角色用户
     */
    @PreAuthorize("hasPermission('org:role', 'view')")
    @GetMapping("users")
    @Operation(summary = BASE_NAME + "用户列表")
    public Paging<OrgRoleUserVO> getRoleUsers(@RequestParam Long roleId, OrgRolePageParam param) {
        Paging<OrgRoleUserDTO> dtoPaging = roleService.getRoleUsers(roleId, param);
        // 手动转换分页结果
        Paging<OrgRoleUserVO> voPaging = new Paging<>();
        voPaging.setCurrent(dtoPaging.getCurrent());
        voPaging.setSize(dtoPaging.getSize());
        voPaging.setTotal(dtoPaging.getTotal());
        voPaging.setPages(dtoPaging.getPages());
        voPaging.setRecords(dtoPaging.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList()));
        return voPaging;
    }


    /**
     * 复制角色
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_COPY",
        businessType = BusinessType.INSERT,
        operation = "'复制角色: ' + #param.sourceRoleId + ' -> ' + #param.newRoleName",
        targetId = "#param.sourceRoleId",
        targetType = "ORG_ROLE",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:role', 'copy')")
    @PostMapping("copy")
    @Operation(summary = "复制" + BASE_NAME)
    public void copy(@RequestBody @Validated OrgRoleCopyParam param) {
        roleService.copyRole(param);
    }


    /**
     * 获取角色已授权的操作ID列表
     */
    @PreAuthorize("hasPermission('org:role', 'view')")
    @GetMapping("{roleId}/operation-ids")
    @Operation(summary = "获取" + BASE_NAME + "操作ID列表")
    public Result<List<Long>> getOperationIds(@PathVariable Long roleId) {
        List<Long> operationIds = roleOperationService.listOperationIdsByRoleId(roleId);
        return Result.success(operationIds);
    }


    /**
     * 保存角色操作权限（全量覆盖）
     */
    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_SAVE_OPERATIONS",
        businessType = BusinessType.GRANT,
        operation = "'保存角色操作权限: ' + #roleId",
        targetId = "#roleId",
        targetType = "ORG_ROLE",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('org:role', 'saveOperations')")
    @PutMapping("{roleId}/operations")
    @Operation(summary = "保存" + BASE_NAME + "操作权限")
    public void saveOperations(@PathVariable Long roleId,
                               @RequestBody @Validated OrgRoleSaveOperationsParam param) {
        // 通过 Service 保存，触发缓存失效
        OrgRoleAssignPermissionParam assignParam = new OrgRoleAssignPermissionParam();
        assignParam.setRoleId(roleId);
        assignParam.setOperationIds(param.getOperationIds());
        roleService.assignPermissions(assignParam);
    }


    /**
     * DTO转VO
     */
    private OrgRoleUserVO toUserVO(OrgRoleUserDTO dto) {
        if (dto == null) {
            return null;
        }
        OrgRoleUserVO vo = new OrgRoleUserVO();
        vo.setUserId(dto.getUserId());
        vo.setAccount(dto.getAccount());
        vo.setName(dto.getName());
        vo.setPhone(dto.getPhone());
        vo.setMainDeptName(dto.getMainDeptName());
        vo.setCreateTime(dto.getCreateTime());
        return vo;
    }

}
