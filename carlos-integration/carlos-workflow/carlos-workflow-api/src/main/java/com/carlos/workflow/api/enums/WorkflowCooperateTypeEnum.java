package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 协作类型枚举（对应 warm-flow {@code flow_his_task.cooperate_type}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /workflow/app/enums} 按码值取文案/语义色。码值与 warm-flow
 * {@code CooperateType} 保持一致。语义色按「多人协作」分档：单人办理灰/蓝，
 * 会签与票签绿，加签与减签橙。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowCooperateType")
@Getter
@AllArgsConstructor
public enum WorkflowCooperateTypeEnum implements BaseEnum<Integer> {

    /**
     * 审批
     */
    APPROVAL(1, "审批", "Approval", EnumColor.MUTED),

    /**
     * 转办
     */
    TRANSFER(2, "转办", "Transfer", EnumColor.PRIMARY),

    /**
     * 委派
     */
    DEPUTE(3, "委派", "Depute", EnumColor.PRIMARY),

    /**
     * 会签
     */
    COUNTERSIGN(4, "会签", "Countersign", EnumColor.SUCCESS),

    /**
     * 票签
     */
    VOTE(5, "票签", "Vote", EnumColor.SUCCESS),

    /**
     * 加签
     */
    ADD_SIGNATURE(6, "加签", "Add Signature", EnumColor.WARNING),

    /**
     * 减签
     */
    REDUCTION_SIGNATURE(7, "减签", "Reduce Signature", EnumColor.WARNING);

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
