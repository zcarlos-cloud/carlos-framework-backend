package com.carlos.auth.sso.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * SSO Token 交换请求参数
 *
 * <p>用于基于已有有效 Token，为目标客户端申请新的访问令牌（免密跳转场景）。</p>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-05-19
 */
@Data
@Schema(description = "SSO Token 交换请求参数")
public class SsoTokenExchangeParam {

    /**
     * 源访问令牌（当前有效的 Token）
     */
    @NotBlank(message = "源访问令牌不能为空")
    @Size(max = 2048, message = "源访问令牌长度不能超过2048")
    @Schema(description = "源访问令牌", requiredMode = Schema.RequiredMode.REQUIRED, example = "eyJhbGciOiJSUzI1NiJ9...")
    private String sourceToken;

    /**
     * 目标客户端ID
     */
    @NotBlank(message = "目标客户端ID不能为空")
    @Size(max = 64, message = "目标客户端ID长度不能超过64")
    @Schema(description = "目标客户端ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "carlos-app")
    private String targetClientId;

    /**
     * 请求的授权范围（可选，为空则使用目标客户端默认范围）
     */
    @Size(max = 10, message = "请求范围数量不能超过 10 个")
    @Schema(description = "请求的授权范围（可选）", example = "[\"read\", \"write\"]")
    private Set<String> requestedScopes;
}
