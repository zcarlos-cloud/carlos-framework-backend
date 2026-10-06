package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 流程实例状态枚举（对应 warm-flow {@code flow_instance.flow_status}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code String}，JSON 下发**字符串数字码**
 * （{@code "1"} 而非 {@code 1}），前端经 {@code /workflow/app/enums} 按码值取文案/语义色。
 * 码值与 warm-flow {@code FlowStatus} 保持一致，存在跳号（无 5 / 7）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowFlowStatus")
@Getter
@AllArgsConstructor
public enum WorkflowFlowStatusEnum implements BaseEnum<String> {

    /**
     * 待提交
     */
    DRAFT("0", "待提交", "To Submit", EnumColor.MUTED),

    /**
     * 审批中
     */
    RUNNING("1", "审批中", "In Approval", EnumColor.PRIMARY),

    /**
     * 审批通过
     */
    PASSED("2", "审批通过", "Approved", EnumColor.PRIMARY),

    /**
     * 自动完成
     */
    AUTO_COMPLETED("3", "自动完成", "Auto Completed", EnumColor.PRIMARY),

    /**
     * 终止
     */
    TERMINATED("4", "终止", "Terminated", EnumColor.MUTED),

    /**
     * 撤销
     */
    REVOKED("6", "撤销", "Revoked", EnumColor.DANGER),

    /**
     * 已完成
     */
    COMPLETED("8", "已完成", "Completed", EnumColor.SUCCESS),

    /**
     * 退回
     */
    RETURNED("9", "退回", "Returned", EnumColor.WARNING),

    /**
     * 失效
     */
    INVALID("10", "失效", "Invalid", EnumColor.MUTED),

    /**
     * 拿回
     */
    TAKEN_BACK("11", "拿回", "Taken Back", EnumColor.WARNING);

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
