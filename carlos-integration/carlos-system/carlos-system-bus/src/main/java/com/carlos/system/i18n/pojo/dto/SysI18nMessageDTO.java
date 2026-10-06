package com.carlos.system.i18n.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 国际化消息 数据传输对象
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@Data
@Accessors(chain = true)
public class SysI18nMessageDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 语言标识
     */
    private String lang;

    /**
     * 所属模块ID
     */
    private Long moduleId;

    /**
     * 消息键
     */
    private String msgKey;

    /**
     * 消息值
     */
    private String msgValue;

    /**
     * 描述说明
     */
    private String description;

    /**
     * 状态：1启用 0禁用
     */
    private Boolean msgStatus;

    /**
     * 创建者
     */
    private Long createBy;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新者
     */
    private Long updateBy;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
