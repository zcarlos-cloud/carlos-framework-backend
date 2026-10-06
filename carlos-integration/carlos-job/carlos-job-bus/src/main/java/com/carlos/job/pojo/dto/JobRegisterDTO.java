package com.carlos.job.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 自动注册任务结果
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
@Data
@Accessors(chain = true)
public class JobRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 本次调用是否真的创建了任务
     */
    private Boolean created;

    /**
     * 任务此前已存在（含并发撞唯一键收敛）
     */
    private Boolean existed;

    /**
     * 漂移态：调度器已存在同键任务但控制面缺失，本次未创建
     */
    private Boolean drifted;

    /**
     * 最终任务状态 NORMAL / PAUSED
     */
    private String status;

    /**
     * 结果说明
     */
    private String message;

}
