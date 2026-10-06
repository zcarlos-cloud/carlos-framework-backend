package com.carlos.system.menu.apiimpl;

import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSysOperation;
import com.carlos.system.menu.convert.SysOperationConvert;
import com.carlos.system.menu.manager.SysOperationManager;
import com.carlos.system.menu.pojo.dto.SysOperationDTO;
import com.carlos.system.pojo.ao.SysOperationAO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 统一操作 Feign 接口实现
 * </p>
 *
 * @author carlos
 * @date 2026-07-11
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/operation")
@Tag(name = "统一操作Feign接口")
public class ApiSysOperationImpl implements ApiSysOperation {

    private final SysOperationManager sysOperationManager;

    @Override
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的操作列表")
    public Result<List<SysOperationAO>> listEnabledOperations() {
        List<SysOperationDTO> all = sysOperationManager.listAll();
        List<SysOperationAO> aoList = SysOperationConvert.INSTANCE.toAOList(all);
        return Result.success(aoList);
    }

}
