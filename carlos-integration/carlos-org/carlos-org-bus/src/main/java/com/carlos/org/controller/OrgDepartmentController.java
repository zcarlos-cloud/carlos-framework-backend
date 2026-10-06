package com.carlos.org.controller;


import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgDepartmentConvert;
import com.carlos.org.manager.OrgDepartmentManager;
import com.carlos.org.pojo.dto.OrgDepartmentDTO;
import com.carlos.org.pojo.dto.OrgDepartmentUserDTO;
import com.carlos.org.pojo.param.*;
import com.carlos.org.pojo.vo.OrgDepartmentTreeVO;
import com.carlos.org.pojo.vo.OrgDepartmentUserVO;
import com.carlos.org.pojo.vo.OrgDepartmentVO;
import com.carlos.org.service.OrgDepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * <p>
 * 部门 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/department")
@Tag(name = "部门管理")
public class OrgDepartmentController {

    public static final String BASE_NAME = "部门";

    private final OrgDepartmentService departmentService;

    private final OrgDepartmentManager departmentManager;


    /**
     * 新增部门
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增部门: ' + #param.deptName",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 20
    )
    @PreAuthorize("hasPermission('org:department', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgDepartmentCreateParam param) {
        OrgDepartmentDTO dto = OrgDepartmentConvert.INSTANCE.toDTO(param);
        departmentService.addOrgDepartment(dto);
    }


    /**
     * 删除部门
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除部门: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:department', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        departmentService.deleteOrgDepartment(param.getIds());
    }


    /**
     * 编辑部门
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改部门: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 25
    )
    @PreAuthorize("hasPermission('org:department', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgDepartmentUpdateParam param) {
        OrgDepartmentDTO dto = OrgDepartmentConvert.INSTANCE.toDTO(param);
        departmentService.updateOrgDepartment(dto);
    }


    /**
     * 部门列表（平铺）
     */
    @PreAuthorize("hasPermission('org:department', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgDepartmentVO> page(OrgDepartmentPageParam param) {
        Paging<OrgDepartmentDTO> dtoPaging = departmentManager.getPage(param);
        // 手动转换分页结果
        Paging<OrgDepartmentVO> voPaging = new Paging<>();
        voPaging.setCurrent(dtoPaging.getCurrent());
        voPaging.setSize(dtoPaging.getSize());
        voPaging.setTotal(dtoPaging.getTotal());
        voPaging.setPages(dtoPaging.getPages());
        voPaging.setRecords(OrgDepartmentConvert.INSTANCE.toVOList(dtoPaging.getRecords()));
        return voPaging;
    }


    /**
     * 部门树查询
     */
    @PreAuthorize("hasPermission('org:department', 'view')")
    @GetMapping("tree")
    @Operation(summary = BASE_NAME + "树")
    public Result<List<OrgDepartmentTreeVO>> tree() {
        List<OrgDepartmentDTO> dtoList = departmentService.getDepartmentTree();
        return Result.success(OrgDepartmentConvert.INSTANCE.buildTreeVOList(dtoList));
    }


    /**
     * 移动部门
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_MOVE",
        businessType = BusinessType.UPDATE,
        operation = "'移动部门: ' + #param.id + ' 到父部门 ' + #param.parentId",
        targetId = "#param.id",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('org:department', 'move')")
    @PostMapping("move")
    @Operation(summary = "移动" + BASE_NAME)
    public void move(@RequestBody @Validated OrgDepartmentMoveParam param) {
        departmentService.moveDepartment(param);
    }


    /**
     * 部门排序
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_SORT",
        businessType = BusinessType.UPDATE,
        operation = "'部门排序: ' + #param.id + ' -> ' + #param.orderNum",
        targetId = "#param.id",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 20
    )
    @PreAuthorize("hasPermission('org:department', 'sort')")
    @PostMapping("sort")
    @Operation(summary = BASE_NAME + "排序")
    public void sort(@RequestBody @Validated OrgDepartmentSortParam param) {
        departmentService.sortDepartment(param);
    }


    /**
     * 部门人员列表
     */
    @PreAuthorize("hasPermission('org:department', 'view')")
    @GetMapping("users")
    @Operation(summary = BASE_NAME + "人员列表")
    public Paging<OrgDepartmentUserVO> getDepartmentUsers(@RequestParam Long deptId, OrgDepartmentPageParam param) {
        Paging<OrgDepartmentUserDTO> dtoPaging = departmentService.getDepartmentUsers(deptId, param);
        // 手动转换分页结果
        Paging<OrgDepartmentUserVO> voPaging = new Paging<>();
        voPaging.setCurrent(dtoPaging.getCurrent());
        voPaging.setSize(dtoPaging.getSize());
        voPaging.setTotal(dtoPaging.getTotal());
        voPaging.setPages(dtoPaging.getPages());
        voPaging.setRecords(OrgDepartmentConvert.INSTANCE.toUserVOList(dtoPaging.getRecords()));
        return voPaging;
    }


    /**
     * 设置部门负责人
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_SET_LEADER",
        businessType = BusinessType.AUTH,
        operation = "'设置部门负责人: 部门 ' + #param.deptId + ' -> 用户 ' + #param.leaderId",
        targetId = "#param.deptId",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:department', 'setLeader')")
    @PostMapping("setLeader")
    @Operation(summary = "设置" + BASE_NAME + "负责人")
    public void setLeader(@RequestBody @Validated OrgDepartmentSetLeaderParam param) {
        departmentService.setDepartmentLeader(param);
    }


    /**
     * 部门角色配置（预留接口）
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_SET_ROLE",
        businessType = BusinessType.GRANT,
        operation = "'配置部门角色: 部门 ' + #deptId + ' -> 角色 ' + #roleId",
        targetId = "#deptId",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:department', 'setRole')")
    @PostMapping("setRole")
    @Operation(summary = BASE_NAME + "角色配置")
    public void setRole(@RequestParam Long deptId, @RequestParam Long roleId) {
        departmentService.setDepartmentRole(deptId, roleId);
    }


    /**
     * 启用/禁用部门
     */
    @Log(
        title = "部门管理",
        logType = "ORG_DEPARTMENT_CHANGE_STATE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'变更部门状态: ' + #param.id + ' -> ' + #param.deptStatus",
        targetId = "#param.id",
        targetType = "ORG_DEPARTMENT",
        riskLevel = 40
    )
    @PreAuthorize("hasPermission('org:department', 'changeState')")
    @PostMapping("changeState")
    @Operation(summary = "启用/禁用" + BASE_NAME)
    public void changeState(@RequestBody @Validated OrgDepartmentChangeStateParam param) {
        departmentService.changeState(param.getId(), param.getDeptStatus());
    }

}
