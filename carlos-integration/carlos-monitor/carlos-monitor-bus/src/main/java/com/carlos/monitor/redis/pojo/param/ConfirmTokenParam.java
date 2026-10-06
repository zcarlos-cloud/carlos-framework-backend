package com.carlos.monitor.redis.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 高危操作确认令牌签发参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "高危操作确认令牌签发参数")
public class ConfirmTokenParam {

    @NotBlank(message = "操作摘要不能为空")
    @Size(max = 300, message = "操作摘要长度不能超过300")
    @Schema(description = "操作摘要（如 batch-delete:pattern=dept:*，与高危接口入参一致）")
    private String operation;
}
