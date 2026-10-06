package com.carlos.org.controller;


import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgUserConvert;
import com.carlos.org.manager.OrgUserManager;
import com.carlos.org.pojo.dto.OrgUserDTO;
import com.carlos.org.pojo.dto.OrgUserDeptDTO;
import com.carlos.org.pojo.dto.OrgUserRoleDTO;
import com.carlos.org.pojo.excel.OrgUserExcel;
import com.carlos.org.pojo.param.*;
import com.carlos.org.pojo.vo.OrgUserDeptVO;
import com.carlos.org.pojo.vo.OrgUserRoleVO;
import com.carlos.org.pojo.vo.OrgUserVO;
import com.carlos.org.position.pojo.dto.OrgUserPositionDTO;
import com.carlos.org.position.pojo.vo.OrgUserPositionVO;
import com.carlos.org.service.OrgUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;
import java.util.List;


/**
 * <p>
 * 系统用户 rest服务接口
 * </p>
 * <p>实现所有用户管理需求</p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/user")
@Tag(name = "系统用户管理")
public class OrgUserController {

    public static final String BASE_NAME = "系统用户";

    private final OrgUserService userService;

    private final OrgUserManager userManager;


    /**
     * 新增用户
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增用户: ' + #param.account",
        targetType = "ORG_USER",
        riskLevel = 20
    )
    @PreAuthorize("hasPermission('org:user', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgUserCreateParam param) {
        OrgUserDTO dto = OrgUserConvert.INSTANCE.toDTO(param);
        userService.addOrgUser(dto);
    }


    /**
     * 删除用户
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除用户: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_USER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        userService.deleteOrgUser(param.getIds());
    }


    /**
     * 编辑用户
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改用户: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER",
        riskLevel = 25
    )
    @PreAuthorize("hasPermission('org:user', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgUserUpdateParam param) {
        OrgUserDTO dto = OrgUserConvert.INSTANCE.toDTO(param);
        userService.updateOrgUser(dto);
    }


    /**
     * 用户详情查询
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgUserVO detail(@RequestParam Long id) {
        OrgUserDTO dto = userManager.getDtoById(id);
        return OrgUserConvert.INSTANCE.toVO(dto);
    }


    /**
     * 用户列表查询（分页）
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgUserVO> page(OrgUserPageParam param) {
        return userManager.getPage(param);
    }


    /**
     * 启用/禁用用户
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_CHANGE_STATE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'变更用户状态: ' + #param.id + ' -> ' + #param.userStatus",
        targetId = "#param.id",
        targetType = "ORG_USER",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('org:user', 'changeState')")
    @PostMapping("changeState")
    @Operation(summary = "启用/禁用" + BASE_NAME)
    public void changeState(@RequestBody @Validated OrgUserChangeStateParam param) {
        userService.changeState(param.getId(), param.getUserStatus());
    }


    /**
     * 解锁用户
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_UNLOCK",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'解锁用户: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user', 'unlock')")
    @PostMapping("unlock")
    @Operation(summary = "解锁" + BASE_NAME)
    public void unlock(@RequestBody @Validated OrgUserUnlockParam param) {
        userService.unlockOrgUser(param.getId());
    }


    /**
     * 重置密码
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_RESET_PASSWORD",
        businessType = BusinessType.UPDATE,
        operation = "'重置用户密码: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user', 'resetPwd')")
    @PostMapping("resetPassword")
    @Operation(summary = "重置密码")
    public void resetPassword(@RequestBody @Validated OrgUserResetPwdParam param) {
        userService.resetPassword(param.getId(), param.getNewPwd());
    }


    /**
     * 修改密码（用户自己修改）
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_CHANGE_PASSWORD",
        businessType = BusinessType.UPDATE,
        operation = "'用户自助修改密码: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_USER",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user', 'changePassword')")
    @PostMapping("changePassword")
    @Operation(summary = "修改密码")
    public void changePassword(@RequestBody @Validated OrgUserChangePwdParam param) {
        userService.changePassword(param);
    }


    /**
     * 用户导入
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_IMPORT",
        businessType = BusinessType.IMPORT,
        operation = "'导入用户'",
        targetType = "ORG_USER",
        riskLevel = 40,
        saveRequestParams = false
    )
    @PreAuthorize("hasPermission('org:user', 'import')")
    @PostMapping("import")
    @Operation(summary = "用户导入")
    public Result<List<Serializable>> importUser(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.importUser(file));
    }


    /**
     * 用户导出
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_EXPORT",
        businessType = BusinessType.EXPORT,
        operation = "'导出用户列表'",
        targetType = "ORG_USER",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("export")
    @Operation(summary = "用户导出")
    public void exportUser(OrgUserPageParam param, HttpServletResponse response) {
        userService.exportUser(param, response);
    }


    /**
     * 用户分配部门
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_ASSIGN_DEPARTMENTS",
        businessType = BusinessType.GRANT,
        operation = "'用户分配部门: ' + #param.userId + ' -> ' + #param.departmentIds",
        targetId = "#param.userId",
        targetType = "ORG_USER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user', 'assignDept')")
    @PostMapping("assignDepartments")
    @Operation(summary = "用户分配部门")
    public void assignDepartments(@RequestBody @Validated OrgUserAssignDeptParam param) {
        userService.assignDepartments(param);
    }


    /**
     * 用户分配角色
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_ASSIGN_ROLES",
        businessType = BusinessType.GRANT,
        operation = "'用户分配角色: ' + #param.userId + ' -> ' + #param.roleIds",
        targetId = "#param.userId",
        targetType = "ORG_USER",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('org:user', 'assignRole')")
    @PostMapping("assignRoles")
    @Operation(summary = "用户分配角色")
    public void assignRoles(@RequestBody @Validated OrgUserAssignRoleParam param) {
        userService.assignRoles(param);
    }


    /**
     * 用户分配岗位
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_ASSIGN_POSITIONS",
        businessType = BusinessType.GRANT,
        operation = "'用户分配岗位: ' + #param.userId + ' -> ' + #param.positionId",
        targetId = "#param.userId",
        targetType = "ORG_USER",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:user', 'assignPosition')")
    @PostMapping("assignPositions")
    @Operation(summary = "用户分配岗位")
    public void assignPositions(@RequestBody @Validated OrgUserAssignPositionParam param) {
        userService.assignPositions(param);
    }


    /**
     * 下载用户导入模板
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("import-template")
    @Operation(summary = "下载用户导入模板")
    public void downloadImportTemplate(HttpServletResponse response) {
        userService.downloadImportTemplate(response);
    }


    /**
     * 用户导入预览
     */
    @PreAuthorize("hasPermission('org:user', 'importPreview')")
    @PostMapping("import-preview")
    @Operation(summary = "用户导入预览")
    public Result<List<OrgUserExcel>> previewImportUser(@RequestParam("file") MultipartFile file) {
        return Result.success(userService.previewImportUser(file));
    }


    /**
     * 用户导入确认写入
     */
    @Log(
        title = "用户管理",
        logType = "ORG_USER_IMPORT_CONFIRM",
        businessType = BusinessType.IMPORT,
        operation = "'确认写入导入用户'",
        targetType = "ORG_USER",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('org:user', 'importConfirm')")
    @PostMapping("import-confirm")
    @Operation(summary = "用户导入确认写入")
    public Result<List<Serializable>> confirmImportUser(@RequestBody List<OrgUserExcel> previewList) {
        return Result.success(userService.confirmImportUser(previewList));
    }


    /**
     * 获取用户已分配部门
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("departments")
    @Operation(summary = "获取用户部门列表")
    public List<OrgUserDeptVO> getUserDepartments(@RequestParam Long userId) {
        List<OrgUserDeptDTO> dtoList = userService.getUserDepartments(userId);
        return OrgUserConvert.INSTANCE.toDeptVOList(dtoList);
    }


    /**
     * 获取用户已分配角色
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("roles")
    @Operation(summary = "获取用户角色列表")
    public List<OrgUserRoleVO> getUserRoles(@RequestParam Long userId) {
        List<OrgUserRoleDTO> dtoList = userService.getUserRoles(userId);
        return OrgUserConvert.INSTANCE.toRoleVOList(dtoList);
    }


    /**
     * 获取用户已分配岗位
     */
    @PreAuthorize("hasPermission('org:user', 'view')")
    @GetMapping("positions")
    @Operation(summary = "获取用户岗位列表")
    public List<OrgUserPositionVO> getUserPositions(@RequestParam Long userId) {
        List<OrgUserPositionDTO> dtoList = userService.getUserPositions(userId);
        return OrgUserConvert.INSTANCE.toPositionVOList(dtoList);
    }

}
