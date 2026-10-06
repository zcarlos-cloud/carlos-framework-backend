package com.carlos.org.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * <p>
 * 超管初始化入参
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "超管初始化参数")
public class OrgInitSuperAdminParam {

    /**
     * 超管密码，必须显式传入，且满足复杂度要求。
     */
    @NotBlank(message = "超管密码不能为空")
    @Size(min = 12, max = 64, message = "密码长度必须在 12-64 之间")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]).+$",
            message = "密码必须包含大小写字母、数字和特殊字符")
    @Schema(description = "超管密码，长度 12-64，必须包含大小写字母、数字和特殊字符")
    private String password;

}
