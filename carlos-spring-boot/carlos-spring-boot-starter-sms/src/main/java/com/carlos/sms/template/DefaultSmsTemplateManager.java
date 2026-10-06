package com.carlos.sms.template;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;


/**
 * <p>
 * 默认模板管理器，从配置中加载模板
 * </p>
 *
 * @author Carlos
 * @date 2024/3/11 14:56
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultSmsTemplateManager implements SmsTemplateManager {

    private final Map<String, SmsTemplate> templates;

    @Override
    public List<SmsTemplate> load() {
        return null;
    }

    @Override
    public SmsTemplate getByCode(String code) {
        SmsTemplate smsTemplate = templates.get(code);
        if (smsTemplate == null) {
            log.warn("Can't find template by code: {}", code);
        }
        return smsTemplate;
    }
}
