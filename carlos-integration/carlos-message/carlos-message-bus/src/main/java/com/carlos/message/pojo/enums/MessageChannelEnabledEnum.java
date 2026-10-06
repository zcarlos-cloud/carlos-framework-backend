package com.carlos.message.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 消息渠道启用状态枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段 {@code MessageChannel.enabled} 仍是 {@code Integer}，
 * JSON 下发数字码，前端经 {@code /message/app/enums} 按码值取文案/语义色。
 * 码值与 {@code message_channel.is_enabled} 保持一致（0 禁用 / 1 启用 / 2 故障）。
 * </p>
 *
 * <p>
 * ⚠️ 故障（2）由后端发送链路自动置位、没有独立接口，人工只能在 0/1 之间切换。
 * 字典消费者（前端表单）应只提供 0/1 两个选项，故障态仅用于搜索栏排查。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "MessageChannelEnabled")
@Getter
@AllArgsConstructor
public enum MessageChannelEnabledEnum implements BaseEnum<Integer> {

    /**
     * 禁用
     */
    DISABLED(0, "禁用", "Disabled", EnumColor.DANGER),

    /**
     * 启用
     */
    ENABLED(1, "启用", "Enabled", EnumColor.SUCCESS),

    /**
     * 故障（后端自动置位，人工不可设置）
     */
    FAULTED(2, "故障", "Faulted", EnumColor.WARNING);

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
