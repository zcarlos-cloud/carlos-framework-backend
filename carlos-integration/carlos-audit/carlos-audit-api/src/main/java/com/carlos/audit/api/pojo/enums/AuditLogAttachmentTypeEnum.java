package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志附件类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogAttachmentType")
@Getter
@AllArgsConstructor
public enum AuditLogAttachmentTypeEnum implements BaseEnum {

    /**
     * 图片
     */
    IMAGE(1, "图片"),

    /**
     * 视频
     */
    VIDEO(2, "视频"),

    /**
     * 文件
     */
    FILE(3, "文件"),

    /**
     * 快照
     */
    SNAPSHOT(4, "快照"),

    /**
     * 音频
     */
    AUDIO(5, "音频");

    @EnumValue
    private final Integer code;

    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
