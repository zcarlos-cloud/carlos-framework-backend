package com.carlos.sms.ums.core;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.carlos.sms.SmsUtil;
import com.carlos.sms.enums.SmsServiceType;
import com.carlos.sms.template.SmsTemplate;
import com.carlos.sms.ums.config.UmsConfig;
import com.google.common.collect.Lists;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.comm.exception.SmsBlendException;
import org.dromara.sms4j.provider.service.AbstractSmsBlend;

import java.util.*;

public class UmsSmsImpl extends AbstractSmsBlend<UmsConfig> implements SmsBlend {

    public static final String SEND_SMS = "/sms/Api/Send.do";

    public UmsSmsImpl(UmsConfig config) {
        super(config);
    }

    @Override
    public String getSupplier() {
        return SmsServiceType.UMS.getCode();
    }

    @Override
    public SmsResponse sendMessage(String phone, String message) {
        // 直接调用群发方法
        return massTexting(Lists.newArrayList(phone), message);
    }

    @Override
    public SmsResponse sendMessage(String phone, LinkedHashMap<String, String> messages) {
        return massTexting(Lists.newArrayList(phone), getConfig().getTemplateId(), messages);
    }

    @Override
    public SmsResponse sendMessage(String phone, String templateId, LinkedHashMap<String, String> messages) {
        return massTexting(Lists.newArrayList(phone), templateId, messages);
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String message) {
        return massTexting(phones, getConfig().getTemplateId(), new LinkedHashMap<>());
    }

    @Override
    public SmsResponse massTexting(List<String> phones, String templateId, LinkedHashMap<String, String> messages) {
        if (phones.size() > 1000) {
            throw new SmsBlendException("单次发送超过最大发送上限，建议每次群发短信人数低于1000");
        } else {
            SmsTemplate template = SmsUtil.getCurrentTemplate();
            String content = StrUtil.format(template.getContent(), messages);
            UmsConfig config = this.getConfig();
            String url = config.getEndpoint() + SEND_SMS;
            Map<String, Object> map = new HashMap<>(4);
            map.put("SpCode", config.getSpCode());
            map.put("LoginName", config.getAccessKeyId());
            map.put("Password", config.getAccessKeySecret());
            map.put("MessageContent", content);
            map.put("UserNumber", StrUtil.join(StrUtil.COMMA, phones));
            map.put("SerialNumber", DateUtil.format(new Date(), DatePattern.PURE_DATETIME_MS_PATTERN) + RandomUtil.randomNumbers(3));
            map.put("f", "1");
            HttpRequest get = HttpUtil.createGet(url);
            get.form(map);
            get.charset(CharsetUtil.CHARSET_GBK);
            HttpResponse execute = get.execute();
            String body = execute.body();
            Map<String, String> resmap = HttpUtil.decodeParamMap(body, CharsetUtil.CHARSET_GBK);
            SmsResponse smsResponse = new SmsResponse();
            smsResponse.setSuccess(true);
            smsResponse.setData(BeanUtil.toBean(resmap, UmsResponse.class));
            smsResponse.setConfigId(getConfigId());
            return smsResponse;
        }
    }

}