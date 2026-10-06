package com.carlos.auth.passkey.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * Passkey 认证完成请求（/authenticate/finish）
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "Passkey 认证完成请求")
public class PasskeyAuthenticateFinishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "凭据ID不能为空")
    @Size(max = 512, message = "凭据ID长度不能超过512")
    @Schema(description = "凭据ID（base64url）")
    private String id;

    @Size(max = 512, message = "凭据ID原始值长度不能超过512")
    @Schema(description = "凭据ID原始值（base64url），通常与 id 相同")
    private String rawId;

    @Schema(description = "凭据类型，固定 public-key")
    private String type;

    @NotNull(message = "断言响应不能为空")
    @Valid
    @Schema(description = "断言响应")
    private AssertionResponse response;

    /**
     * 断言响应内部类
     */
    @Data
    @Schema(description = "Passkey 断言响应")
    public static class AssertionResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank(message = "认证器数据不能为空")
        @Size(max = 4096, message = "认证器数据长度不能超过4096")
        @Schema(description = "认证器数据（base64url）")
        private String authenticatorData;

        @NotBlank(message = "客户端数据不能为空")
        @Size(max = 8192, message = "客户端数据长度不能超过8192")
        @Schema(description = "客户端数据（base64url）")
        private String clientDataJSON;

        @NotBlank(message = "签名不能为空")
        @Size(max = 4096, message = "签名长度不能超过4096")
        @Schema(description = "签名（base64url）")
        private String signature;

        @Size(max = 512, message = "用户句柄长度不能超过512")
        @Schema(description = "用户句柄（base64url），可发现凭据模式下返回")
        private String userHandle;
    }

}
