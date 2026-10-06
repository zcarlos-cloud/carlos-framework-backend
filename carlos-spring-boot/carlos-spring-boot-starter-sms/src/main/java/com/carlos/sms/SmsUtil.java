package com.carlos.sms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.carlos.sms.exception.SmsException;
import com.carlos.sms.exception.SmsNotSupportException;
import com.carlos.sms.exception.SmsTemplateException;
import com.carlos.sms.ratelimit.SmsRateLimiter;
import com.carlos.sms.template.SmsTemplate;
import com.carlos.sms.template.SmsTemplateManager;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.core.factory.SmsFactory;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * <p>
 * 短信工具类
 * </p>
 *
 * @author Carlos
 * @date 2024/1/15 15:26
 */
@Slf4j
public class SmsUtil {

    private static SmsTemplateManager templateManager;
    private static SmsRateLimiter rateLimiter;

    public SmsUtil(SmsTemplateManager templateManager) {
        SmsUtil.templateManager = templateManager;
    }

    public SmsUtil(SmsTemplateManager templateManager, SmsRateLimiter rateLimiter) {
        SmsUtil.templateManager = templateManager;
        SmsUtil.rateLimiter = rateLimiter;
    }

    public static void setRateLimiter(SmsRateLimiter rateLimiter) {
        SmsUtil.rateLimiter = rateLimiter;
    }


    private static final TransmittableThreadLocal<SmsTemplate> CURRENT_TEMPLATE = new TransmittableThreadLocal<>();

    public static SmsTemplate getCurrentTemplate() {
        return CURRENT_TEMPLATE.get();
    }


    /**
     * 发送单条短信
     *
     * @param phone        手机号
     * @param templateCode 模板编码
     * @param messages     key为模板变量名称 value为模板变量值
     * @author Carlos
     * @date 2023/11/23 1:39
     */
    public static void sendByTemplateKey(String phone, String templateCode, LinkedHashMap<String, String> messages) {
        sendByTemplateKey(phone, templateCode, null, messages, false);
    }

    /**
     * 群发短信
     *
     * @param phones       手机号
     * @param templateCode 模板编码
     * @param messages     key为模板变量名称 value为模板变量值
     * @author Carlos
     * @date 2023/11/23 1:39
     */
    public static void sendByTemplateKey(List<String> phones, String templateCode, LinkedHashMap<String, String> messages) {
        sendByTemplateKey(phones, templateCode, null, messages, false);
    }


    /**
     * 发送单条短信
     *
     * @param phone        手机号
     * @param templateCode 模板编码
     * @param messages     key为模板变量名称 value为模板变量值
     * @param sync         是否异步发送
     * @author Carlos
     * @date 2023/11/23 1:39
     */
    public static void sendByTemplateKey(String phone, String templateCode, String configId, LinkedHashMap<String, String> messages, boolean sync) {
        sendByTemplateKey(List.of(phone), templateCode, configId, messages, sync);
    }


    /**
     * 群发短信
     *
     * @param phones       手机号
     * @param templateCode 模板编码
     * @param messages     key为模板变量名称 value为模板变量值
     * @param sync         是否异步发送
     * @author Carlos
     * @date 2023/11/23 1:39
     */
    public static void sendByTemplateKey(List<String> phones, String templateCode, String configId, LinkedHashMap<String, String> messages, boolean sync) {
        if (CollUtil.isEmpty(phones)) {
            return;
        }

        // P2-013 修复：短信发送限流检查
        if (rateLimiter != null) {
            for (String phone : phones) {
                if (!rateLimiter.allowSend(phone)) {
                    throw new SmsException("短信发送过于频繁，请稍后再试");
                }
            }
        }

        // 检查当前是否配置可支持短信
        if (CollUtil.isEmpty(SmsFactory.getAll())) {
            throw new SmsNotSupportException();
        }
        // 检查短信模板是否存在
        SmsTemplate template = templateManager.getByCode(templateCode);
        if (template == null) {
            throw new SmsTemplateException("短信模板不存在！");
        }
        // 将短信配置设置到ThreadLocal
        CURRENT_TEMPLATE.set(template);
        SmsBlend smsBlend;
        if (StrUtil.isBlank(configId)) {
            smsBlend = SmsFactory.getSmsBlend();
        } else {
            smsBlend = SmsFactory.getSmsBlend(configId);
        }
        if (smsBlend == null) {
            throw new SmsException("未找到可用的短信配置!");
        }

        String templateId = template.getTemplateId();

        if (sync) {
            if (phones.size() > 1) {
                smsBlend.massTexting(phones, templateId, messages);
            } else {
                smsBlend.sendMessageAsync(phones.getFirst(), templateId, messages);
            }
        } else {
            if (phones.size() > 1) {
                smsBlend.massTexting(phones, templateId, messages);
            } else {
                smsBlend.sendMessage(phones.getFirst(), templateId, messages);
            }
        }
        // 记录发送成功
        if (rateLimiter != null) {
            for (String phone : phones) {
                rateLimiter.recordSend(phone);
            }
        }

        // 移除线程缓存
        CURRENT_TEMPLATE.remove();
    }
}
