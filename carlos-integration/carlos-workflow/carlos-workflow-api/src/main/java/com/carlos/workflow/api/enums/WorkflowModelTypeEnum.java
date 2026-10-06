package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 流程定义-设计器模型枚举（对应 warm-flow {@code flow_definition.model_value}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code String}，JSON 直接下发模型名，
 * 前端经 {@code /workflow/app/enums} 取中英文文案。取值与 warm-flow
 * {@code ModelEnum} 保持一致（CLASSICS 经典模型 / MIMIC 仿钉钉模型）。
 * 该值只做展示与新建单选，不作为状态标签上色，故不声明语义色（默认 MUTED）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowModelType")
@Getter
@AllArgsConstructor
public enum WorkflowModelTypeEnum implements BaseEnum<String> {

    /**
     * 经典模型
     */
    CLASSICS("CLASSICS", "经典模型", "Classic Model"),

    /**
     * 仿钉钉模型
     */
    MIMIC("MIMIC", "仿钉钉模型", "DingTalk-like Model");

    private final String code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
