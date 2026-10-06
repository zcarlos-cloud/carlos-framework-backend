package com.carlos.org.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 重置密码参数
 * </p>
 *
 * @author carlos
 * @date 2026-03-03
 */
@Data
@Accessors(chain = true)
@Schema(description = "重置密码参数")
public class OrgUserResetPwdParam {

    @NotNull(message = "用户id不能为空")
    @Schema(description = "用户ID")
    private Long id;

    @NotBlank(message = "新密码不能为空")
    @Size(max = 128, message = "新密码长度不能超过128")
    @Schema(description = "新密码（SM2加密后）")
    private String newPwd;

}
