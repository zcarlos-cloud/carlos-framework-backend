package com.carlos.devkit.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 模板类型 / 生成业务类型枚举
 * </p>
 * <p>
 * 同时承载两个语义完全一致的字段：
 * <ul>
 *   <li>{@code dev_template_group.template_type} —— 模板组属于代码生成还是工程生成</li>
 *   <li>{@code dev_generate_record.biz_type} —— 生成记录属于代码生成还是工程生成</li>
 * </ul>
 * 两处码值相同（1=代码生成 2=工程生成），共用一个字典，前端只认 {@code DevkitTemplateType}。
 * </p>
 * <p>
 * 承载字段为 {@code Integer}（库表 SMALLINT），JSON 下发数字码，前端经
 * {@code /devkit/app/enums} 取文案与语义色。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@AppEnum(code = "DevkitTemplateType")
@Getter
@AllArgsConstructor
public enum TemplateTypeEnum implements BaseEnum<Integer> {

    /**
     * 代码生成（按数据库表生成代码）
     */
    CODE(1, "代码生成", "Code Generation", EnumColor.PRIMARY),

    /**
     * 工程生成（按坐标生成工程骨架）
     */
    PROJECT(2, "工程生成", "Project Scaffold", EnumColor.SUCCESS);

    private final Integer code;

    private final String desc;

    /**
     * 英文描述：随 /app/enums 下发，前端按当前语言取用
     */
    private final String descEn;

    /**
     * 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表
     */
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
