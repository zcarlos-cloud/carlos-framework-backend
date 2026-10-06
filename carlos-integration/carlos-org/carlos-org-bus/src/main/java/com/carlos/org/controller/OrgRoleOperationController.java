package com.carlos.org.controller;

import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.org.service.OrgRoleOperationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 角色操作权限 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/org/role")
@Validated
@Tag(name = "角色操作权限")
public class OrgRoleOperationController {

    private final OrgRoleOperationService orgRoleOperationService;

    @PreAuthorize("hasPermission('org:role:operation', 'view')")
    @GetMapping("operation-ids")
    @Operation(summary = "获取角色已授权的操作ID列表")
    public List<Long> getRoleOperationIds(@RequestParam @NotNull(message = "角色id不能为空") Long roleId) {
        return orgRoleOperationService.listOperationIdsByRoleId(roleId);
    }

    @Log(
        title = "角色管理",
        logType = "ORG_ROLE_OPERATION_SAVE",
        businessType = BusinessType.GRANT,
        operation = "'保存角色操作权限: ' + #roleId",
        targetId = "#roleId",
        targetType = "ORG_ROLE",
        riskLevel = 70
    )
    @PreAuthorize("hasPermission('org:role:operation', 'saveOperations')")
    @PostMapping("saveOperations")
    @Operation(summary = "保存角色的操作权限")
    public void saveRoleOperations(@RequestParam @NotNull(message = "角色id不能为空") Long roleId,
                                   @RequestBody @NotNull(message = "操作id列表不能为空") Set<Long> operationIds) {
        orgRoleOperationService.saveRoleOperations(roleId, operationIds);
    }

}
