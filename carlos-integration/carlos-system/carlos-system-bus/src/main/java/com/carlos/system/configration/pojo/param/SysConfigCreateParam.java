package com.carlos.system.configration.pojo.param;


import com.carlos.system.enums.ConfigValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 系统配置 新增参数封装
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统配置新增参数")
public class SysConfigCreateParam {

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 100, message = "参数名称长度不能超过100")
    @Schema(description = "参数名称")
    private String configName;
    @NotBlank(message = "参数编码不能为空")
    @Size(max = 100, message = "参数编码长度不能超过100")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "参数编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2~64位")
    @Schema(description = "参数编码")
    private String configCode;
    @NotBlank(message = "参数键值不能为空")
    @Size(max = 4000, message = "参数键值长度不能超过4000")
    @Schema(description = "参数键值")
    private String configValue;
    @NotNull(message = "值类型不能为空")
    @Schema(description = "值类型")
    private ConfigValueType valueType;
    @Schema(description = "状态")
    private Boolean configStatus;
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

}
