package com.carlos.workflow.api.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 流程定义-发布状态枚举（对应 warm-flow {@code flow_definition.is_publish}）
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段在 AO/DTO 里仍是 {@code Integer}，JSON 下发数字码，
 * 前端经 {@code /workflow/app/enums} 按码值取文案/语义色。码值与 warm-flow
 * {@code PublishStatus} 保持一致（0 未发布 / 1 已发布 / 9 已失效）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "WorkflowPublishStatus")
@Getter
@AllArgsConstructor
public enum WorkflowPublishStatusEnum implements BaseEnum<Integer> {

    /**
     * 未发布
     */
    DRAFT(0, "未发布", "Draft", EnumColor.MUTED),

    /**
     * 已发布
     */
    PUBLISHED(1, "已发布", "Published", EnumColor.SUCCESS),

    /**
     * 已失效
     */
    INVALID(9, "已失效", "Invalid", EnumColor.DANGER);

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
