package com.carlos.message.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 消息接收人状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@AppEnum(code = "MessageReceiverStatus")
@Getter
@AllArgsConstructor
public enum MessageReceiverStatusEnum implements BaseEnum {

    PENDING(0, "待发送", "Pending", EnumColor.MUTED),
    SENDING(1, "发送中", "Sending", EnumColor.PRIMARY),
    SENT(2, "已发送", "Sent", EnumColor.PRIMARY),
    DELIVERED(3, "送达", "Delivered", EnumColor.SUCCESS),
    READ(4, "已读", "Read", EnumColor.SUCCESS),
    FAILED(5, "失败", "Failed", EnumColor.DANGER),
    REVOKED(6, "撤回", "Revoked", EnumColor.MUTED);

    @EnumValue
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

    public static MessageReceiverStatusEnum of(Integer code) {
        for (MessageReceiverStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}
