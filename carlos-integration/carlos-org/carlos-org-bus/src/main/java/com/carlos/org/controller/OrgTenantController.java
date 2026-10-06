package com.carlos.org.controller;


import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.convert.OrgTenantConvert;
import com.carlos.org.manager.OrgTenantManager;
import com.carlos.org.pojo.dto.OrgTenantDTO;
import com.carlos.org.pojo.param.OrgTenantChangeStatusParam;
import com.carlos.org.pojo.param.OrgTenantCreateParam;
import com.carlos.org.pojo.param.OrgTenantInitAdminParam;
import com.carlos.org.pojo.param.OrgTenantPageParam;
import com.carlos.org.pojo.param.OrgTenantUpdateParam;
import com.carlos.org.pojo.vo.OrgTenantVO;
import com.carlos.org.service.OrgTenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * <p>
 * 租户 rest服务接口
 * </p>
 * <p>实现租户管理与租户管理员初始化</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("org/tenant")
@Tag(name = "租户管理")
public class OrgTenantController {

    public static final String BASE_NAME = "租户";

    private final OrgTenantService tenantService;

    private final OrgTenantManager tenantManager;


    /**
     * 新增租户
     */
    @Log(
        title = "租户管理",
        logType = "ORG_TENANT_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增租户: ' + #param.tenantCode",
        targetType = "ORG_TENANT",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:tenant', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated OrgTenantCreateParam param) {
        OrgTenantDTO dto = OrgTenantConvert.INSTANCE.toDTO(param);
        tenantService.addOrgTenant(dto);
    }


    /**
     * 删除租户
     */
    @Log(
        title = "租户管理",
        logType = "ORG_TENANT_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除租户: ' + #param.ids",
        targetId = "#param.ids",
        targetType = "ORG_TENANT",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('org:tenant', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        tenantService.deleteOrgTenant(param.getIds());
    }


    /**
     * 编辑租户
     */
    @Log(
        title = "租户管理",
        logType = "ORG_TENANT_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改租户: ' + #param.id",
        targetId = "#param.id",
        targetType = "ORG_TENANT",
        riskLevel = 30
    )
    @PreAuthorize("hasPermission('org:tenant', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated OrgTenantUpdateParam param) {
        OrgTenantDTO dto = OrgTenantConvert.INSTANCE.toDTO(param);
        tenantService.updateOrgTenant(dto);
    }


    /**
     * 租户列表
     */
    @PreAuthorize("hasPermission('org:tenant', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<OrgTenantVO> page(OrgTenantPageParam param) {
        Paging<OrgTenantDTO> dtoPaging = tenantManager.getPage(param);
        // 手动转换分页结果
        Paging<OrgTenantVO> voPaging = new Paging<>();
        voPaging.setCurrent(dtoPaging.getCurrent());
        voPaging.setSize(dtoPaging.getSize());
        voPaging.setTotal(dtoPaging.getTotal());
        voPaging.setPages(dtoPaging.getPages());
        voPaging.setRecords(OrgTenantConvert.INSTANCE.toVOList(dtoPaging.getRecords()));
        return voPaging;
    }


    /**
     * 租户详情
     */
    @PreAuthorize("hasPermission('org:tenant', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public OrgTenantVO detail(@RequestParam Long id) {
        OrgTenantDTO dto = tenantManager.getDtoById(id);
        return OrgTenantConvert.INSTANCE.toVO(dto);
    }


    /**
     * 停用/启用租户
     */
    @Log(
        title = "租户管理",
        logType = "ORG_TENANT_CHANGE_STATUS",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'停用/启用租户: ' + #param.id + ' -> ' + #param.tenantStatus",
        targetId = "#param.id",
        targetType = "ORG_TENANT",
        riskLevel = 50
    )
    @PreAuthorize("hasPermission('org:tenant', 'changeState')")
    @PostMapping("change-status")
    @Operation(summary = "停用/启用" + BASE_NAME)
    public void changeStatus(@RequestBody @Validated OrgTenantChangeStatusParam param) {
        tenantService.changeStatus(param.getId(), param.getTenantStatus());
    }


    /**
     * 初始化租户管理员
     */
    @Log(
        title = "租户管理",
        logType = "ORG_TENANT_INIT_ADMIN",
        businessType = BusinessType.INSERT,
        operation = "'初始化租户管理员: 租户 ' + #param.tenantId + ', 账号 ' + #param.account",
        targetId = "#param.tenantId",
        targetType = "ORG_TENANT",
        riskLevel = 60
    )
    @PreAuthorize("hasPermission('org:tenant', 'initAdmin')")
    @PostMapping("init-admin")
    @Operation(summary = "初始化" + BASE_NAME + "管理员")
    public void initAdmin(@RequestBody @Validated OrgTenantInitAdminParam param) {
        tenantService.initTenantAdmin(param);
    }

}
