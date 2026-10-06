package com.carlos.message.core.protocol;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 消息状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@AppEnum(code = "MessageStatus")
@Getter
@AllArgsConstructor
public enum MessageStatus implements BaseEnum {

    /**
     * 待发送
     */
    PENDING(0, "待发送", "Pending", EnumColor.MUTED),

    /**
     * 发送中
     */
    SENDING(1, "发送中", "Sending", EnumColor.PRIMARY),

    /**
     * 已发送
     */
    SENT(2, "已发送", "Sent", EnumColor.PRIMARY),

    /**
     * 送达
     */
    DELIVERED(3, "送达", "Delivered", EnumColor.SUCCESS),

    /**
     * 已读
     */
    READ(4, "已读", "Read", EnumColor.SUCCESS),

    /**
     * 失败
     */
    FAILED(5, "失败", "Failed", EnumColor.DANGER),

    /**
     * 撤回
     */
    REVOKED(6, "撤回", "Revoked", EnumColor.MUTED);

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

    /**
     * 根据code获取枚举
     *
     * @param code code
     * @return 枚举
     */
    public static MessageStatus of(Integer code) {
        for (MessageStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
