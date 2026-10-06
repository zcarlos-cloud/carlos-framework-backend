package com.carlos.sms.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 短信厂商标识枚举类
 * </p>
 *
 * @author Carlos
 * @date 2023/11/20 18:40
 */
@Getter
@AllArgsConstructor
public enum SmsServiceType {

    /**
     * 短信厂商类型
     */
    UNISMS("unisms", "合一短信"),
    ALIBABA("alibaba", "阿里云短信"),
    CLOOPEN("cloopen", "容联云短信"),
    CTYUN("ctyun", "天翼云短信"),
    EMAY("emay", "亿美软通短信"),
    HUAWEI("huawei", "华为云短信"),
    JDCLOUD("jdcloud", "京东云短信"),
    NETEASE("netease", "网易云信"),
    TENCENT("tencent", "腾讯云短信"),
    YUNPIAN("yunpian", "云片短信"),
    ZHUTONG("zhutong", "助通短信"),
    POSTAL("postal", "邮政云"),
    WOCLOUD("wocloud", "智慧信息企信"),
    UMS("ums", "一信通"),
    YNCHINA("ynchina", "云南短信");

    private final String code;

    private final String desc;

}
