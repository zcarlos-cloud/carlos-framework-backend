package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 流转类型枚举（对应 warm-flow {@code flow_his_task.skip_type}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code String}，JSON 直接下发枚举名，
 * 前端经 {@code /workflow/app/enums} 取中英文文案与语义色。取值与 warm-flow
 * {@code SkipType} 保持一致（PASS / REJECT / NONE）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowSkipType")
@Getter
@AllArgsConstructor
public enum WorkflowSkipTypeEnum implements BaseEnum<String> {

    /**
     * 通过
     */
    PASS("PASS", "通过", "Pass", EnumColor.SUCCESS),

    /**
     * 退回
     */
    REJECT("REJECT", "退回", "Reject", EnumColor.DANGER),

    /**
     * 无动作
     */
    NONE("NONE", "无动作", "No Action", EnumColor.MUTED);

    private final String code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
