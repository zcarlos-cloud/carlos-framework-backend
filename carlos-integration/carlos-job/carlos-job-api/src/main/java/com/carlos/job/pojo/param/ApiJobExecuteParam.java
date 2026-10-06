package com.carlos.job.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 调度中心回调业务服务的任务执行参数
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "任务执行回调参数")
public class ApiJobExecuteParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 任务处理器名称（业务模块中 JobHandler 的 Bean 名称）
     */
    @NotBlank(message = "任务处理器名称不能为空")
    @Schema(description = "任务处理器名称（JobHandler Bean 名称）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String handlerName;

    /**
     * 任务参数（JSON 字符串，由任务处理器自行解析）
     */
    @Size(max = 4000, message = "任务参数长度不能超过 4000")
    @Schema(description = "任务参数（JSON 字符串）")
    private String params;

}
