package com.carlos.monitor.instance.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 通用高危操作确认令牌签发 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月7日
 */
@Data
@Accessors(chain = true)
@Schema(description = "通用高危操作确认令牌签发参数")
public class ConfirmTokenIssueParam {

    @NotNull(message = "实例ID不能为空")
    @Schema(description = "实例ID（令牌绑定对象；微服务日志调整为服务对应 MICROSERVICE 实例ID）")
    private Long instanceId;

    @NotBlank(message = "操作摘要不能为空")
    @Size(max = 300, message = "操作摘要长度不能超过300")
    @Schema(description = "操作摘要（与高危消费端一致，如 db:session:kill:123、"
        + "service:logger:svc:ip:port:com.x:DEBUG、batch-delete:pattern=a:*、sql-console:write:{指纹}）")
    private String operation;
}
