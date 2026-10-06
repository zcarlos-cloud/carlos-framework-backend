package com.carlos.sms.template;


import java.util.List;

/**
 * <p>
 * 短信模板管理
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 14:49
 */
public interface SmsTemplateManager {


    /**
     * 加载所有模板
     *
     * @return java.util.List<com.carlos.sms.template.SmsTemplate>
     * @throws
     * @author Carlos
     * @date 2024/3/11 14:54
     */
    List<SmsTemplate> load();


    /**
     * 根据编号获取模板
     *
     * @param code 模板编号
     * @return com.carlos.sms.template.SmsTemplate
     * @throws
     * @author Carlos
     * @date 2024/3/11 14:55
     */
    SmsTemplate getByCode(String code);
}
