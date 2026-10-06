package com.carlos.job.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 自动注册任务结果 API 对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
@Schema(description = "自动注册任务结果")
public class JobRegisterAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次调用是否真的创建了任务
     */
    @Schema(description = "是否创建了任务")
    private Boolean created;

    /**
     * 任务此前已存在（含并发撞唯一键收敛）
     */
    @Schema(description = "任务是否已存在")
    private Boolean existed;

    /**
     * 漂移态：调度器已存在同键任务但控制面缺失，本次未创建
     */
    @Schema(description = "是否为漂移态（Quartz 有 / job_info 无）")
    private Boolean drifted;

    /**
     * 最终任务状态 NORMAL / PAUSED
     */
    @Schema(description = "最终任务状态")
    private String status;

    /**
     * 结果说明
     */
    @Schema(description = "结果说明")
    private String message;

}
