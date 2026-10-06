package com.carlos.sms.template;

import lombok.Data;

/**
 * <p>
 * 短信模板
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 14:50
 */
@Data
public class SmsTemplate {

    /**
     * 编码
     */
    private String code;

    /**
     * id, 对应短信平台id
     */
    private String templateId;
    /**
     * 短信对应签名
     */
    private String signature;
    /**
     * 模板内容
     */
    private String content;
}
