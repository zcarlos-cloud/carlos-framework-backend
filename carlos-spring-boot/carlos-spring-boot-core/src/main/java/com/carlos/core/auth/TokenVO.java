package com.carlos.core.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * Oauth2获取Token返回信息封装
 * </p>
 *
 * @author carlos
 * @date 2021/11/5 10:24
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
public class TokenVO implements Serializable {

    @Schema(description = "请求头名称")
    private String name;

    @Schema(description = "访问令牌")
    private String token;


}
