package com.carlos.job.client.handler;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 任务执行结果
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
public class JobResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 结果说明（失败时为失败原因）
     */
    private String message;

    public static JobResult ok() {
        return new JobResult().setSuccess(Boolean.TRUE);
    }

    public static JobResult ok(String message) {
        return new JobResult().setSuccess(Boolean.TRUE).setMessage(message);
    }

    public static JobResult fail(String message) {
        return new JobResult().setSuccess(Boolean.FALSE).setMessage(message);
    }

}
