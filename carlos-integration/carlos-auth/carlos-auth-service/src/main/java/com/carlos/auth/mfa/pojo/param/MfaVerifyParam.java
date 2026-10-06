package com.carlos.auth.mfa.pojo.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MFA 验证请求参数
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-04-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaVerifyParam {

    @NotBlank(message = "密钥不能为空")
    @Size(max = 512, message = "密钥长度不能超过512")
    private String secret;

    @NotBlank(message = "验证码不能为空")
    @Pattern(regexp = "\\d{6}", message = "验证码必须为6位数字")
    private String code;
}
