package com.carlos.monitor.instance.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.instance.convert.MonitorGroupConvert;
import com.carlos.monitor.instance.manager.MonitorInstanceGroupManager;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceGroupDTO;
import com.carlos.monitor.instance.pojo.param.GroupAuthParam;
import com.carlos.monitor.instance.pojo.param.GroupCreateParam;
import com.carlos.monitor.instance.pojo.param.GroupPageParam;
import com.carlos.monitor.instance.pojo.param.GroupUpdateParam;
import com.carlos.monitor.instance.pojo.vo.InstanceGroupVO;
import com.carlos.monitor.instance.service.MonitorGroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 监控实例分组管理 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/groups")
@Tag(name = "监控实例分组管理")
public class MonitorGroupController {

    private final MonitorGroupService groupService;

    private final MonitorInstanceGroupManager groupManager;

    @PreAuthorize("hasPermission('monitor:instance', 'view')")
    @GetMapping("/page")
    @Operation(summary = "分组分页")
    public Result<Paging<InstanceGroupVO>> page(GroupPageParam param) {
        return Result.success(groupManager.getPage(param));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'view')")
    @GetMapping("/list")
    @Operation(summary = "分组列表", description = "分组树/下拉用，含授权角色ID")
    public Result<List<InstanceGroupVO>> list() {
        List<MonitorInstanceGroupDTO> groups = groupService.listAll();
        return Result.success(MonitorGroupConvert.INSTANCE.toVO(groups));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "分组详情")
    public Result<InstanceGroupVO> detail(@PathVariable Long id) {
        return Result.success(MonitorGroupConvert.INSTANCE.toVO(groupService.getDetail(id)));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'add')")
    @PostMapping
    @Operation(summary = "新增分组")
    public Result<Long> add(@RequestBody @Validated GroupCreateParam param) {
        MonitorInstanceGroupDTO dto = MonitorGroupConvert.INSTANCE.toDTO(param);
        return Result.success(groupService.addGroup(dto));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'edit')")
    @PutMapping
    @Operation(summary = "编辑分组")
    public Result<Boolean> update(@RequestBody @Validated GroupUpdateParam param) {
        MonitorInstanceGroupDTO dto = MonitorGroupConvert.INSTANCE.toDTO(param);
        return Result.success(groupService.updateGroup(dto));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分组")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(groupService.deleteGroup(id));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'edit')")
    @PutMapping("/{id}/auth")
    @Operation(summary = "分组授权", description = "全量替换分组-角色授权")
    public Result<Boolean> auth(@PathVariable Long id, @RequestBody @Validated GroupAuthParam param) {
        groupService.updateAuth(id, param.getRoleIds());
        return Result.success(true);
    }
}
