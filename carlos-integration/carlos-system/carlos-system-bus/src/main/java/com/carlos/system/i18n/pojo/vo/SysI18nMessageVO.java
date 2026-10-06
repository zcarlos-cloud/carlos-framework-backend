package com.carlos.system.i18n.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 国际化消息 显示层对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysI18nMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "语言标识")
    private String lang;

    @Schema(description = "所属模块ID")
    private Long moduleId;

    @Schema(description = "模块编码")
    private String moduleCode;

    @Schema(description = "消息键")
    private String msgKey;

    @Schema(description = "消息值")
    private String msgValue;

    @Schema(description = "描述说明")
    private String description;

    @Schema(description = "状态：true启用 false禁用")
    private Boolean msgStatus;

}
