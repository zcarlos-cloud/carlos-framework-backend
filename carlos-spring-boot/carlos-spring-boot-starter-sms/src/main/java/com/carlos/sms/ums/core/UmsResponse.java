package com.carlos.sms.ums.core;

import lombok.Data;


/**
 * <p>
 * 一信通短信响应结果
 * </p>
 *
 * @author Carlos
 * @date 2023/11/21 20:46
 */
@Data
public class UmsResponse {

    /**
     * 错误代码：0 成功
     */
    private Integer result;
    private String description;
    /** 回执结果，每条回执按分号隔开，每个字段按逗号隔开，每条回执共包含3个字段，第1个字段为流水号，第2个字段为被发送的手机号码，第3个字段为回执结果，0表示成功，其他值表示失败 */
    private String out;
    private String taskId;
}
