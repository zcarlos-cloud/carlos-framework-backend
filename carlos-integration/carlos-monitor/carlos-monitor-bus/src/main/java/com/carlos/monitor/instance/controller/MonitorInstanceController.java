package com.carlos.monitor.instance.controller;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.instance.convert.MonitorInstanceConvert;
import com.carlos.monitor.instance.manager.MonitorInstanceManager;
import com.carlos.monitor.instance.pojo.dto.InstanceDraftDTO;
import com.carlos.monitor.instance.pojo.dto.MonitorInstanceDTO;
import com.carlos.monitor.instance.pojo.param.InstanceCreateParam;
import com.carlos.monitor.instance.pojo.param.InstanceDiscoverParam;
import com.carlos.monitor.instance.pojo.param.InstancePageParam;
import com.carlos.monitor.instance.pojo.param.InstanceStatusParam;
import com.carlos.monitor.instance.pojo.param.InstanceUpdateParam;
import com.carlos.monitor.instance.pojo.param.TestConnectionParam;
import com.carlos.monitor.instance.pojo.vo.InstanceDraftVO;
import com.carlos.monitor.instance.pojo.vo.InstanceVO;
import com.carlos.monitor.instance.pojo.vo.TestConnectionVO;
import com.carlos.monitor.instance.service.MonitorInstanceService;
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
 * 监控实例管理 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/instances")
@Tag(name = "监控实例管理")
public class MonitorInstanceController {

    private final MonitorInstanceService instanceService;

    private final MonitorInstanceManager instanceManager;

    @PreAuthorize("hasPermission('monitor:instance', 'view')")
    @GetMapping("/page")
    @Operation(summary = "实例分页", description = "类型/环境/分组/健康/关键字过滤")
    public Result<Paging<InstanceVO>> page(InstancePageParam param) {
        return Result.success(instanceManager.getPage(param));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "实例详情", description = "password_cipher 不返回")
    public Result<InstanceVO> detail(@PathVariable Long id) {
        MonitorInstanceDTO dto = instanceService.getDetail(id);
        InstanceVO vo = MonitorInstanceConvert.INSTANCE.toVO(dto);
        vo.setHasPassword(StrUtil.isNotBlank(dto.getPasswordCipher()));
        return Result.success(vo);
    }

    @PreAuthorize("hasPermission('monitor:instance', 'add')")
    @PostMapping
    @Operation(summary = "新增实例", description = "保存前服务端强制一次连接测试，失败允许保存但标记 DOWN")
    public Result<Long> add(@RequestBody @Validated InstanceCreateParam param) {
        MonitorInstanceDTO dto = MonitorInstanceConvert.INSTANCE.toDTO(param);
        return Result.success(instanceService.addInstance(dto, param.getPassword()));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'edit')")
    @PutMapping
    @Operation(summary = "编辑实例", description = "连接信息变更时失效连接缓存")
    public Result<Boolean> update(@RequestBody @Validated InstanceUpdateParam param) {
        MonitorInstanceDTO dto = MonitorInstanceConvert.INSTANCE.toDTO(param);
        return Result.success(instanceService.updateInstance(dto, param.getPassword()));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'delete')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除实例", description = "逻辑删除 + 连接失效 + 停采")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(instanceService.deleteInstance(id));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'test')")
    @PostMapping("/test-connection")
    @Operation(summary = "测试连接", description = "入参为未保存的连接信息或 instanceId；返回版本/角色/节点/耗时")
    public Result<TestConnectionVO> testConnection(@RequestBody @Validated TestConnectionParam param) {
        return Result.success(MonitorInstanceConvert.INSTANCE.toVO(instanceService.testConnection(param)));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'edit')")
    @PutMapping("/{id}/status")
    @Operation(summary = "启停状态", description = "启停采集/数据管理/SQL 写")
    public Result<Boolean> updateStatus(@PathVariable Long id, @RequestBody InstanceStatusParam param) {
        return Result.success(instanceService.updateStatus(id, param));
    }

    @PreAuthorize("hasPermission('monitor:instance', 'add')")
    @PostMapping("/discover")
    @Operation(summary = "Nacos 配置自动发现", description = "读取目标应用 Nacos 配置生成实例草稿（不含密码）")
    public Result<List<InstanceDraftVO>> discover(@RequestBody @Validated InstanceDiscoverParam param) {
        List<InstanceDraftDTO> drafts = instanceService.discover(param.getAppName(), param.getGroup());
        return Result.success(MonitorInstanceConvert.INSTANCE.toDraftVO(drafts));
    }
}
