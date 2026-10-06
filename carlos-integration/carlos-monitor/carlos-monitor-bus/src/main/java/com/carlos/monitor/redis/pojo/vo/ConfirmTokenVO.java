package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 高危操作确认令牌 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "高危操作确认令牌视图对象")
public class ConfirmTokenVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "一次性确认令牌")
    private String token;

    @Schema(description = "有效期（秒）")
    private Integer ttlSeconds;
}
