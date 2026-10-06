package com.carlos.org.pojo.param;


import com.carlos.org.pojo.enums.OrgUserGenderEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 系统用户 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年2月28日 下午1:25:36
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统用户新增参数")
public class OrgUserCreateParam {
    @NotBlank(message = "用户名不能为空")
    // 与前端 src/constants/reg.ts 的 REG_USER_NAME 保持一致：中英文、数字、下划线、中划线，长度4-16
    @Pattern(regexp = "^[\\u4E00-\\u9FA5a-zA-Z0-9_-]{4,16}$", message = "用户名仅允许中英文、数字、下划线、中划线，长度4-16")
    @Size(max = 20, message = "用户名长度不能超过20")
    @Schema(description = "用户名")
    private String account;
    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过20")
    @Schema(description = "昵称")
    private String nickname;
    @NotBlank(message = "密码不能为空")
    @Size(max = 128, message = "密码长度不能超过128")
    @Schema(description = "密码")
    private String pwd;
    @Size(max = 18, message = "证件号码长度不能超过18")
    @Schema(description = "证件号码")
    private String identify;
    // 与前端 src/constants/reg.ts 的 REG_PHONE 保持一致
    @Pattern(regexp = "^[1](([3][0-9])|([4][01456789])|([5][012356789])|([6][2567])|([7][0-8])|([8][0-9])|([9][012356789]))[0-9]{8}$", message = "手机号码格式不正确")
    @Size(max = 11, message = "手机号码长度不能超过11")
    @Schema(description = "手机号码")
    private String phone;
    @Size(max = 100, message = "详细地址长度不能超过100")
    @Schema(description = "详细地址")
    private String address;
    @Schema(description = "性别，0：保密, 1：男，2：女，默认0")
    private OrgUserGenderEnum gender;
    // 与前端 src/constants/reg.ts 的 REG_EMAIL 保持一致
    @Pattern(regexp = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", message = "邮箱格式不正确")
    @Size(max = 30, message = "邮箱长度不能超过30")
    @Schema(description = "邮箱")
    private String email;
    @Size(max = 200, message = "头像长度不能超过200")
    @Schema(description = "头像")
    private String avatar;
    @Size(max = 200, message = "备注长度不能超过200")
    @Schema(description = "备注")
    private String description;
    @Schema(description = "主部门id")
    private Long mainDeptId;
}
