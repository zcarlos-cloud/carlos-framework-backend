package com.carlos.message.core.channel;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 渠道类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@AppEnum(code = "ChannelType")
@Getter
@AllArgsConstructor
public enum ChannelType implements BaseEnum {

    /**
     * 短信
     */
    SMS(1, "短信", "SMS", EnumColor.MUTED),

    /**
     * 邮件
     */
    EMAIL(2, "邮件", "Email", EnumColor.MUTED),

    /**
     * 钉钉
     */
    DINGTALK(3, "钉钉", "DingTalk", EnumColor.MUTED),

    /**
     * 企业微信
     */
    WECHAT_WORK(4, "企业微信", "WeChat Work", EnumColor.MUTED),

    /**
     * 站内信
     */
    WEBSOCKET(5, "站内信", "In-app Message", EnumColor.MUTED);

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
    public static ChannelType of(Integer code) {
        for (ChannelType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
