package com.carlos.monitor.alarm.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleCreateParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRulePageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRuleUpdateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleTemplateVO;
import com.carlos.monitor.alarm.pojo.vo.AlarmRuleVO;
import com.carlos.monitor.alarm.service.AlarmRuleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 告警规则 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/alarms/rules")
@Tag(name = "告警规则")
public class AlarmRuleController {

    private final AlarmRuleService alarmRuleService;

    @PreAuthorize("hasPermission('monitor:alarm', 'view')")
    @GetMapping("/page")
    @Operation(summary = "规则分页")
    public Result<Paging<AlarmRuleVO>> page(AlarmRulePageParam param) {
        return Result.success(alarmRuleService.getPage(param));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'view')")
    @GetMapping("/templates")
    @Operation(summary = "内置规则模板", description = "Redis 内存>85%、命中率<90%、主从断链、连接数、DB 池、实例/服务 DOWN、错误率，一键选用")
    public Result<List<AlarmRuleTemplateVO>> templates() {
        return Result.success(alarmRuleService.listTemplates());
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'view')")
    @GetMapping("/{id}")
    @Operation(summary = "规则详情")
    public Result<AlarmRuleVO> detail(@PathVariable Long id) {
        return Result.success(alarmRuleService.getDetail(id));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'rule')")
    @PostMapping
    @Operation(summary = "新增规则")
    public Result<Long> add(@RequestBody @Validated AlarmRuleCreateParam param) {
        return Result.success(alarmRuleService.addRule(param));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'rule')")
    @PutMapping
    @Operation(summary = "编辑规则")
    public Result<Boolean> update(@RequestBody @Validated AlarmRuleUpdateParam param) {
        return Result.success(alarmRuleService.updateRule(param));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'rule')")
    @DeleteMapping("/{id}")
    @Operation(summary = "删除规则")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(alarmRuleService.deleteRule(id));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'rule')")
    @PutMapping("/{id}/enabled")
    @Operation(summary = "启停规则")
    public Result<Boolean> updateEnabled(@PathVariable Long id, @RequestParam("enabled") boolean enabled) {
        return Result.success(alarmRuleService.updateEnabled(id, enabled));
    }
}
