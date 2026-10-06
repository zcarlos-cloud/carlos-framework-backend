package com.carlos.sms.postal.core;

import cn.hutool.json.JSONUtil;
import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.postal.config.PostalConfig;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.comm.delayedTime.DelayedTime;
import org.dromara.sms4j.comm.exception.SmsBlendException;
import org.dromara.sms4j.provider.service.AbstractSmsBlend;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * <p>类名: PostalSmsImpl
 * <p>说明：  postalSms短信实现
 *
 * @author Carlos 2023/3/26  17:10
 **/
@Slf4j
public class PostalSmsImpl extends AbstractSmsBlend<PostalConfig> {

    public PostalSmsImpl(PostalConfig config, Executor pool, DelayedTime delayed) {
        super(config, pool, delayed);
    }

    public PostalSmsImpl(PostalConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return SmsServiceType.POSTAL.getCode();
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        if ("".equals(getConfig().getTemplateId())) {
            throw new SmsBlendException("配置文件模板id不能为空！");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);

        return sendMessage(phone, getConfig().getTemplateId(), map);
    }

    @Override
    public SmsResponse sendMessage(String phone, LinkedHashMap<String, String> messages) {
        return null;
    }

    /**
     * 单条发送
     *
     * @param phone      参数0
     * @param templateId 参数1
     * @param messages   参数2
     * @return org.dromara.sms4j.api.entity.SmsResponse
     * @author Carlos
     * @date 2023/11/21 22:24
     */
    @Override
    public SmsResponse sendMessage(String phone, String templateId, LinkedHashMap<String, String> messages) {
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("phone", List.of(phone));
        data.put("signId", getConfig().getSignature());
        data.put("templateId", templateId);
        data.put("varValues", JSONUtil.toJsonStr(messages));
        return getSmsResponse(data, Postal.SEND_SMS);
    }

    /**
     * 群发短信
     *
     * @param phones  参数0
     * @param message 参数1
     * @return org.dromara.sms4j.api.entity.SmsResponse
     * @author Carlos
     * @date 2023/11/21 22:24
     */
    @Override
    public SmsResponse massTexting(List<String> phones, String message) {
        if ("".equals(getConfig().getTemplateId())) {
            throw new SmsBlendException("配置文件模板id不能为空！");
        }
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);

        return massTexting(phones, getConfig().getTemplateId(), map);
    }

    /**
     * 群发短信
     *
     * @param phones     参数0
     * @param templateId 参数1
     * @param messages   参数2
     * @return org.dromara.sms4j.api.entity.SmsResponse
     * @author Carlos
     * @date 2023/11/21 22:24
     */
    @Override
    public SmsResponse massTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages) {
        // 邮政云短信批量发送限制50条
        if (phones.size() > 50) {
            throw new SmsBlendException("单次发送超过最大发送上限，建议每次群发短信人数低于1000");
        }
        Map<String, Object> data = new LinkedHashMap<>(4);
        data.put("to", phones);
        data.put("signId", getConfig().getSignature());
        data.put("templateId", templateId);
        data.put("varValuesJsonArray", JSONUtil.toJsonStr(messages));
        return getSmsResponse(data, Postal.SEND_BATCH_SMS);
    }

    private SmsResponse getSmsResponse(Map<String, Object> data, String uri) {
        SmsResponse smsResponse = new SmsResponse();
        try {
            PostalResponse response = Postal.getClient(getConfig().getRetryInterval(), getConfig().getMaxRetries()).request(uri, data);
            smsResponse.setSuccess("200".equals(response.getErrorCode()));
            smsResponse.setData(response);
            smsResponse.setConfigId(getConfigId());
        } catch (Exception e) {
            log.error("发送短信失败: {}", e.getMessage(), e);
            smsResponse.setSuccess(false);
        }
        return smsResponse;
    }

}
