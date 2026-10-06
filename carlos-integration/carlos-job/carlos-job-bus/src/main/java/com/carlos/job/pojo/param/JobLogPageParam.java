package com.carlos.job.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 任务执行日志 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "任务执行日志列表查询参数")
public class JobLogPageParam extends ParamPage {

    @Size(max = 100, message = "任务名称长度不能超过 100")
    @Schema(description = "任务名称（模糊）")
    private String jobName;

    @Size(max = 64, message = "任务分组长度不能超过 64")
    @Schema(description = "任务分组")
    private String jobGroup;

    @Size(max = 64, message = "目标服务名长度不能超过 64")
    @Schema(description = "目标服务名")
    private String serviceName;

    @Size(max = 128, message = "任务处理器名称长度不能超过 128")
    @Schema(description = "任务处理器名称")
    private String handlerName;

    @Min(value = 1, message = "执行状态取值 1-3")
    @Max(value = 3, message = "执行状态取值 1-3")
    @Schema(description = "执行状态: 1-成功 2-失败 3-执行中")
    private Integer status;

    @Schema(description = "开始时间")
    private LocalDateTime start;

    @Schema(description = "结束时间")
    private LocalDateTime end;

}
