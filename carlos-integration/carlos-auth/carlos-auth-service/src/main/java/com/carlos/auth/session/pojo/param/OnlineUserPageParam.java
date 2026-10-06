package com.carlos.auth.session.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 在线用户 分页查询参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "在线用户分页查询参数")
public class OnlineUserPageParam extends ParamPage {

    @Size(max = 64, message = "用户名长度不能超过64")
    @Schema(description = "用户名（模糊匹配）")
    private String username;

}
