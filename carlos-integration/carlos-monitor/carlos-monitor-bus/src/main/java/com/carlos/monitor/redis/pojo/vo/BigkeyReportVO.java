package com.carlos.monitor.redis.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 大key/热key 报告 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "大key/热key报告视图对象")
public class BigkeyReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "实例ID")
    private Long instanceId;

    @Schema(description = "报告类型：BIGKEY/HOTKEY")
    private String reportType;

    @Schema(description = "触发方式：MANUAL/SCHEDULED")
    private String triggerType;

    @Schema(description = "扫描状态：RUNNING/SUCCESS/FAILED")
    private String scanStatus;

    @Schema(description = "已扫描 key 数量")
    private Long scannedKeys;

    @Schema(description = "采样率（%）")
    private Integer sampleRate;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
