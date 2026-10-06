package com.carlos.auth.session.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 踢下线 参数
 * </p>
 *
 * <p>sessionIds 与 username 至少传一个；username 表示踢掉该用户的全部会话。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "踢下线参数")
public class OnlineUserKickParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 50, message = "单次最多踢下线50个会话")
    @Schema(description = "会话ID列表（授权ID）")
    private List<String> sessionIds;

    @Size(max = 64, message = "用户名长度不能超过64")
    @Schema(description = "用户名（踢掉该用户全部会话）")
    private String username;

}
