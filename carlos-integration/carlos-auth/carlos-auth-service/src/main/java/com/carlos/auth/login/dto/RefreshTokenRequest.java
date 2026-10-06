package com.carlos.auth.login.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 刷新令牌请求
 *
 * <p>公共客户端（无密钥、强制 PKCE）无法通过标准 {@code /oauth2/token} 端点刷新令牌
 * （Spring Authorization Server 要求公共客户端在令牌端点使用 code_verifier 完成客户端认证，
 * 而 code_verifier 仅存在于授权码换取令牌环节），因此提供本端点，
 * 以刷新令牌本身作为凭证换取新的访问令牌。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-09-17
 */
@Data
@Schema(description = "刷新令牌请求")
public class RefreshTokenRequest {

    /**
     * 刷新令牌
     */
    @NotBlank(message = "刷新令牌不能为空")
    @Size(max = 2048, message = "刷新令牌长度不能超过2048")
    @Schema(description = "刷新令牌", example = "Rx4hgKquYm4r2TPj...")
    private String refreshToken;
}
