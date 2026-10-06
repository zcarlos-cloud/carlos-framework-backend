package com.carlos.monitor.alarm.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.monitor.alarm.pojo.param.AlarmConfirmParam;
import com.carlos.monitor.alarm.pojo.param.AlarmRecordPageParam;
import com.carlos.monitor.alarm.pojo.param.AlarmSilenceCreateParam;
import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;
import com.carlos.monitor.alarm.service.AlarmRecordService;
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

/**
 * <p>
 * 告警记录与静默 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/alarms")
@Tag(name = "告警记录")
public class AlarmRecordController {

    private final AlarmRecordService alarmRecordService;

    @PreAuthorize("hasPermission('monitor:alarm', 'view')")
    @GetMapping("/records/page")
    @Operation(summary = "告警记录分页", description = "状态/级别/时间/实例过滤")
    public Result<Paging<AlarmRecordVO>> recordsPage(AlarmRecordPageParam param) {
        return Result.success(alarmRecordService.getPage(param));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'confirm')")
    @PutMapping("/records/{id}/confirm")
    @Operation(summary = "确认告警", description = "FIRING → CONFIRMED，条件更新防并发覆盖")
    public Result<Boolean> confirm(@PathVariable Long id, @RequestBody @Validated AlarmConfirmParam param) {
        alarmRecordService.confirm(id, param.getHandleRemark());
        return Result.success(true);
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'silence')")
    @PostMapping("/silences")
    @Operation(summary = "新增静默")
    public Result<Long> addSilence(@RequestBody @Validated AlarmSilenceCreateParam param) {
        return Result.success(alarmRecordService.addSilence(param));
    }

    @PreAuthorize("hasPermission('monitor:alarm', 'silence')")
    @DeleteMapping("/silences/{id}")
    @Operation(summary = "删除静默")
    public Result<Boolean> deleteSilence(@PathVariable Long id) {
        return Result.success(alarmRecordService.deleteSilence(id));
    }
}
