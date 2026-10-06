package com.carlos.job.pojo.ao;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 任务执行结果 API 对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@Schema(description = "任务执行结果")
public class JobResultAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private Boolean success;

    /**
     * 结果说明（失败时为失败原因）
     */
    @Schema(description = "结果说明")
    private String message;

    public static JobResultAO ok() {
        return new JobResultAO().setSuccess(Boolean.TRUE);
    }

    public static JobResultAO ok(String message) {
        return new JobResultAO().setSuccess(Boolean.TRUE).setMessage(message);
    }

    public static JobResultAO fail(String message) {
        return new JobResultAO().setSuccess(Boolean.FALSE).setMessage(message);
    }

}
