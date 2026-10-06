package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 流程定义-激活状态枚举（对应 warm-flow {@code flow_definition.activity_status}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /workflow/app/enums} 按码值取文案/语义色。码值与 warm-flow
 * {@code ActivityStatus} 保持一致（0 挂起 / 1 激活）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowActivityStatus")
@Getter
@AllArgsConstructor
public enum WorkflowActivityStatusEnum implements BaseEnum<Integer> {

    /**
     * 挂起
     */
    SUSPENDED(0, "挂起", "Suspended", EnumColor.WARNING),

    /**
     * 激活
     */
    ACTIVE(1, "激活", "Active", EnumColor.SUCCESS);

    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
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
