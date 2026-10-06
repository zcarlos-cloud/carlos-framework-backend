package com.carlos.system.i18n.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * 国际化消息聚合对象（key-value 格式）
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Schema(description = "国际化消息聚合对象")
public class SysI18nMessagesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "语言标识")
    private String lang;

    @Schema(description = "消息映射表")
    private Map<String, String> messages;

}
