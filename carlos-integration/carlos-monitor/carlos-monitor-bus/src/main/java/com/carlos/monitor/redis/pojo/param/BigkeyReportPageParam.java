package com.carlos.monitor.redis.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 大key/热key 报告 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "大key/热key报告分页查询参数")
public class BigkeyReportPageParam extends ParamPage {

    @NotNull(message = "实例ID不能为空")
    @Schema(description = "实例ID")
    private Long instanceId;

    @Size(max = 20, message = "报告类型长度不能超过20")
    @Schema(description = "报告类型：BIGKEY/HOTKEY")
    private String reportType;

    @Size(max = 20, message = "扫描状态长度不能超过20")
    @Schema(description = "扫描状态：RUNNING/SUCCESS/FAILED")
    private String scanStatus;

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;
}
