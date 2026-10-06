package com.carlos.sms.wocloud.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class WoCloudSendRequest {

    /**
     * api密匙账号
     */
    @JsonProperty("secretName")
    private String secretName;
    /**
     * api密匙
     */
    @JsonProperty("secretKey")
    private String secretKey;
    /**
     * Unix时间戳(毫秒ms),当明文鉴权时，此项无需填写
     */
    @JsonProperty("timeStamp")
    private Long timeStamp;
    /**
     * 11位手机号码，多个号码请用英文逗号隔开 例：18300000000,18300000001
     */
    @JsonProperty("mobile")
    private String mobile;
    /**
     * 短信内容，请保持在500字以内
     */
    @JsonProperty("content")
    private String content;
    /**
     * 模板id
     */
    @JsonProperty("templateId")
    private String templateId;
    /**
     * 扩展号，由6位以内的数字组成
     */
    @JsonProperty("extCode")
    private String extCode;
    /**
     * 短信签名，例：【签名】
     */
    @JsonProperty("signName")
    private String signName;
    /**
     * 定时时间，格式：yyyyMMddHHmmss
     */
    @JsonProperty("timing")
    private String timing;
    /**
     * 自定义id，36位以内的字母数字组成
     */
    @JsonProperty("customId")
    private String customId;
}
