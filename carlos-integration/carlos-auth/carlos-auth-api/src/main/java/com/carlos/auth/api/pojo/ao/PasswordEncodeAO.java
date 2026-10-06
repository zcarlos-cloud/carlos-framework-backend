package com.carlos.auth.api.pojo.ao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 密码编码请求 AO
 *
 * @author carlos
 * @date 2026/06/20
 */
@Data
@Accessors(chain = true)
public class PasswordEncodeAO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 明文密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 512, message = "密码长度不能超过512")
    private String password;
}
