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
 * Passkey 注册完成请求（/register/finish）
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "Passkey 注册完成请求")
public class PasskeyRegisterFinishRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "凭据ID不能为空")
    @Size(max = 512, message = "凭据ID长度不能超过512")
    @Schema(description = "凭据ID（base64url）")
    private String id;

    @Size(max = 512, message = "凭据ID原始值长度不能超过512")
    @Schema(description = "凭据ID原始值（base64url）")
    private String rawId;

    @Schema(description = "凭据类型，固定 public-key")
    private String type;

    @Schema(description = "认证器附加信息，如 platform/cross-platform")
    private String authenticatorAttachment;

    @NotNull(message = "证明响应不能为空")
    @Valid
    @Schema(description = "证明响应")
    private AttestationResponse response;

    /**
     * 证明响应内部类
     */
    @Data
    @Schema(description = "Passkey 证明响应")
    public static class AttestationResponse implements Serializable {

        private static final long serialVersionUID = 1L;

        @NotBlank(message = "证明对象不能为空")
        @Size(max = 8192, message = "证明对象长度不能超过8192")
        @Schema(description = "证明对象（base64url）")
        private String attestationObject;

        @NotBlank(message = "客户端数据不能为空")
        @Size(max = 8192, message = "客户端数据长度不能超过8192")
        @Schema(description = "客户端数据（base64url）")
        private String clientDataJSON;

        @Schema(description = "传输方式数组，如 [\"internal\", \"hybrid\"]")
        private java.util.List<String> transports;
    }

}
