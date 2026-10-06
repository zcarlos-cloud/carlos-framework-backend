package com.carlos.auth.api.pojo.ao;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 密码匹配请求 AO
 *
 * @author carlos
 * @date 2022/4/25 15:27
 */
@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class PasswordMatchAO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 明文密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 512, message = "密码长度不能超过512")
    private String password;

    /**
     * 编码后的密码
     */
    @NotBlank(message = "编码密码不能为空")
    @Size(max = 256, message = "编码密码长度不能超过256")
    private String encodePassword;
}
