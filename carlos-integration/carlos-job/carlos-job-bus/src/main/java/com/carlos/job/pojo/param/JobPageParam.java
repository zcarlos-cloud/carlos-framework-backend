package com.carlos.job.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 定时任务 列表查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "定时任务列表查询参数")
public class JobPageParam extends ParamPage {

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

    @Size(max = 16, message = "任务状态长度不能超过 16")
    @Schema(description = "任务状态: NORMAL-正常 PAUSED-暂停")
    private String status;

}
