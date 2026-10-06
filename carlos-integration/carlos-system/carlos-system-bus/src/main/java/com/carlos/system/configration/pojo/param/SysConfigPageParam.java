package com.carlos.system.configration.pojo.param;


import com.carlos.core.param.ParamPage;
import com.carlos.system.enums.ConfigValueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;


/**
 * <p>
 * 系统配置 列表查询参数封装
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:55
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统配置列表查询参数")
public class SysConfigPageParam extends ParamPage {

    @Size(max = 100, message = "参数名称长度不能超过100")
    @Schema(description = "参数名称")
    private String configName;
    @Size(max = 100, message = "参数编码长度不能超过100")
    @Schema(description = "参数编码")
    private String configCode;
    @Schema(description = "值类型")
    private ConfigValueType valueType;
    @Schema(description = "状态")
    private Boolean configStatus;

}
