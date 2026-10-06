package com.carlos.job.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 调度器元数据 显示对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "调度器元数据")
public class SchedulerMetaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "调度器名称")
    private String schedulerName;

    @Schema(description = "调度器实例ID")
    private String instanceId;

    @Schema(description = "是否集群模式")
    private Boolean clustered;

    @Schema(description = "是否处于待机(standby)状态")
    private Boolean standby;

    @Schema(description = "是否已关闭")
    private Boolean shutdown;

    @Schema(description = "运行起始时间")
    private LocalDateTime runningSince;

    @Schema(description = "调度线程池大小")
    private Integer threadPoolSize;

    @Schema(description = "累计执行任务数")
    private Integer executedJobCount;

    @Schema(description = "当前正在执行的任务数")
    private Integer runningJobCount;

}
