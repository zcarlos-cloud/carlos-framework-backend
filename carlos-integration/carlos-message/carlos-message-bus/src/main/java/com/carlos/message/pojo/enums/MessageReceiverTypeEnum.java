package com.carlos.message.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 消息接收者类型枚举
 * </p>
 *
 * <p>
 * 本枚举是**字典源**：承载字段（{@code MessageReceiver.receiverType} 等）仍是
 * {@code Integer}，JSON 下发数字码，前端经 {@code /message/app/enums} 按码值取文案。
 * 码值与库表保持一致（1 用户 / 2 部门 / 3 角色）。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@AppEnum(code = "MessageReceiverType")
@Getter
@AllArgsConstructor
public enum MessageReceiverTypeEnum implements BaseEnum<Integer> {

    /**
     * 用户
     */
    USER(1, "用户", "User"),

    /**
     * 部门
     */
    DEPARTMENT(2, "部门", "Department"),

    /**
     * 角色
     */
    ROLE(3, "角色", "Role");

    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
