package com.carlos.system.i18n.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 国际化消息 分页查询参数
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "国际化消息分页查询参数")
public class SysI18nPageParam extends ParamPage {

    @Size(max = 20, message = "语言标识长度不能超过20")
    @Schema(description = "语言标识")
    private String lang;

    @Schema(description = "所属模块ID")
    private Long moduleId;

    @Size(max = 200, message = "消息键长度不能超过200")
    @Schema(description = "消息键")
    private String msgKey;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean msgStatus;

}
