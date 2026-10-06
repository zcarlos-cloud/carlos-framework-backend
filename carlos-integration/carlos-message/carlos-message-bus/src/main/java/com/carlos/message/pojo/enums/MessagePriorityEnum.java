package com.carlos.message.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 消息优先级枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段（{@code MessageRecord.priority} 等）仍是
 * {@code Integer}，JSON 下发数字码，前端经 {@code /message/app/enums} 按码值取文案/语义色。
 * 码值与库表保持一致（1 紧急 / 2 高 / 3 普通 / 4 低），与
 * {@code MessageSendParam} / {@code MessageCreateParam} 的默认值 3（普通）一致。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "MessagePriority")
@Getter
@AllArgsConstructor
public enum MessagePriorityEnum implements BaseEnum<Integer> {

    /**
     * 紧急
     */
    URGENT(1, "紧急", "Urgent", EnumColor.DANGER),

    /**
     * 高
     */
    HIGH(2, "高", "High", EnumColor.WARNING),

    /**
     * 普通
     */
    NORMAL(3, "普通", "Normal", EnumColor.MUTED),

    /**
     * 低
     */
    LOW(4, "低", "Low", EnumColor.MUTED);

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
