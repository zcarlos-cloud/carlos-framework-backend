package com.carlos.system.menu.controller;

import com.carlos.system.menu.pojo.vo.SysOperationTreeVO;
import com.carlos.system.menu.service.SysOperationTreeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 操作权限树接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-27
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/operation")
@Tag(name = "操作权限树")
public class SysOperationTreeController {

    private final SysOperationTreeService sysOperationTreeService;

    @PreAuthorize("hasPermission('manage:operation', 'view')")
    @GetMapping("tree")
    @Operation(summary = "按菜单维度组织的操作树")
    public List<SysOperationTreeVO> tree() {
        return sysOperationTreeService.buildOperationTree();
    }

}
