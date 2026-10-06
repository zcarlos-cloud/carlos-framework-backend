package com.carlos.sms.wocloud.core;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.sms.SmsUtil;
import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.template.SmsTemplate;
import com.carlos.sms.wocloud.config.WoCloudConfig;
import com.google.common.collect.Lists;
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
 * <p>类名: WoCloudSmsImpl
 * <p>说明：  企信短信实现
 *
 * @author :Carlos 2023/3/26  17:10
 **/
@Slf4j
public class WoCloudSmsImpl extends AbstractSmsBlend<WoCloudConfig> {
    private static final int DEFAULT_MAX_NUMBER = 50;

    public WoCloudSmsImpl(WoCloudConfig config, Executor pool, DelayedTime delayed) {
        super(config, pool, delayed);
    }

    public WoCloudSmsImpl(WoCloudConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return SmsServiceType.WOCLOUD.getCode();
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
        SmsTemplate currentTemplate = SmsUtil.getCurrentTemplate();
        return massTexting(Lists.newArrayList(phone), currentTemplate.getTemplateId(), messages);
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
        // 直接调用群发方式
        return massTexting(Lists.newArrayList(phone), templateId, messages);
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
     * @param phones     电话号码
     * @param templateId 模板id
     * @param messages   短信内容，请保持在500字以内。注意：使用短信模板进行提交时，请填写模板拼接后的完整内容。
     * @return org.dromara.sms4j.api.entity.SmsResponse
     * @author Carlos
     * @date 2023/11/21 22:24
     */
    @Override
    public SmsResponse massTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages) {
        if (phones.size() > DEFAULT_MAX_NUMBER) {
            throw new SmsBlendException("单次发送超过最大发送上限，建议每次群发短信人数低于1000");
        }
        SmsTemplate template = SmsUtil.getCurrentTemplate();
        String content = StrUtil.format(template.getContent(), messages);
        WoCloudConfig config = getConfig();
        WoCloudSendRequest request = new WoCloudSendRequest();
        request.setSecretName(config.getAccessKeyId());
        request.setSecretKey(config.getAccessKeySecret());
        request.setMobile(StrUtil.join(StrUtil.COMMA, phones));
        request.setContent(content);
        request.setTemplateId(templateId);
        String signature = template.getSignature();
        if (StrUtil.isBlank(signature)) {
            signature = config.getSignature();
        }
        request.setSignName(signature);
        // request.setExtCode("");
        // request.setTimeStamp(0L);
        // request.setTiming("");
        // request.setCustomId("");
        return getSmsResponse(BeanUtil.beanToMap(request));
    }


    /**
     * 获取短信发送响应信息
     *
     * @param data 请求参数
     * @return org.dromara.sms4j.api.entity.SmsResponse
     * @author Carlos
     * @date 2024/1/15 15:29
     */
    private SmsResponse getSmsResponse(Map<String, Object> data) {
        SmsResponse smsResponse = new SmsResponse();
        try {
            WoCloudResponse response = WoCloud.getClient(getConfig().getRetryInterval(), getConfig().getMaxRetries()).request(WoCloud.SEND_SMS, data);
            smsResponse.setSuccess(WoCloudErrorCode.CODE_0.getCode().equals(response.getCode()));
            smsResponse.setData(response);
            smsResponse.setConfigId(getConfigId());
        } catch (Exception e) {
            smsResponse.setSuccess(false);
        }
        return smsResponse;
    }

}
