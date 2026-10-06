package com.carlos.devkit.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 模板文件类型枚举（{@code dev_template.file_type}）
 * </p>
 * <p>
 * 承载字段为 {@code Integer}（库表 SMALLINT），JSON 下发数字码。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@AppEnum(code = "DevkitTemplateFileType")
@Getter
@AllArgsConstructor
public enum TemplateFileTypeEnum implements BaseEnum<Integer> {

    /**
     * 模板文件（.ftl，需渲染）
     */
    TEMPLATE(1, "模板文件", "Template", EnumColor.PRIMARY),

    /**
     * 静态资源（直接拷贝，不渲染）
     */
    STATIC(2, "静态资源", "Static Resource", EnumColor.MUTED);

    private final Integer code;

    private final String desc;

    private final String descEn;

    private final EnumColor color;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
