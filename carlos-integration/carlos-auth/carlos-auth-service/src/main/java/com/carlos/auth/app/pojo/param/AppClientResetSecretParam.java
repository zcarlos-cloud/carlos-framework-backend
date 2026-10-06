package com.carlos.auth.app.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 应用信息 更新参数封装
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
@Schema(name = "应用秘钥重置参数", description = "应用秘钥重置参数")
public class AppClientResetSecretParam {
    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
}
