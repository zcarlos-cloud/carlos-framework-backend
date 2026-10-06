package com.carlos.sms.wocloud.core;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.comm.constant.Constant;
import org.dromara.sms4j.comm.exception.SmsBlendException;
import org.dromara.sms4j.comm.utils.SmsHttpUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class WoCloudClient {

    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;
    private final int retryInterval;
    private final int maxRetries;
    private int retry = 0;
    private final SmsHttpUtils http = SmsHttpUtils.instance();

    protected WoCloudClient(Builder b) {
        this.accessKeyId = b.accessKeyId;
        this.accessKeySecret = b.accessKeySecret;
        this.endpoint = b.endpoint;
        this.retryInterval = b.retryInterval;
        this.maxRetries = b.maxRetries;
    }


    /**
     * 发情请求
     *
     * @param uri  请求地址
     * @param data 参数
     * @return com.carlos.sms.wocloud.core.WoCloudResponse
     * @author Carlos
     * @date 2024/1/15 14:59
     */
    public WoCloudResponse request(final String uri, final Map<String, Object> data) throws SmsBlendException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", Constant.APPLICATION_JSON_UTF8);
        headers.put("Accept", Constant.ACCEPT);
        String url = this.endpoint + uri;

        try {
            log.debug("【wocloud】request info, url:{}, param:{}", url, data);
            JSONObject response = http.postJson(url, headers, data);
            WoCloudResponse smsResponse = response.toBean(WoCloudResponse.class);
            if (WoCloudErrorCode.CODE_0.getCode().equals(smsResponse.getCode()) || retry == maxRetries) {
                retry = 0;
                return smsResponse;
            }
            log.error("send sms failed:{}", smsResponse);
            throw new SmsBlendException("短信发送失败！");
            // return requestRetry(uri, data);
        } catch (SmsBlendException e) {
            log.error("【wocloud】request faild, message:{}", e.getMessage(), e);
            // return requestRetry(uri, data);
            throw new SmsBlendException("短信发送失败！");
        }
    }

    private WoCloudResponse requestRetry(String action, Map<String, Object> data) {
        http.safeSleep(retryInterval);
        retry++;
        log.warn("短信第 {} 次重新发送", retry);
        return request(action, data);
    }

    public static class Builder {

        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint;
        private int retryInterval;
        private int maxRetries;

        public Builder(final String accessKeyId) {
            this.accessKeyId = accessKeyId;
        }

        public Builder(final String accessKeyId, final String accessKeySecret) {
            this.accessKeyId = accessKeyId;
            this.accessKeySecret = accessKeySecret;
        }

        public Builder accessKeySecret(final String accessKeySecret) {
            this.accessKeySecret = accessKeySecret;
            return this;
        }

        public Builder endpoint(final String endpoint) {
            this.endpoint = endpoint;
            return this;
        }


        public Builder setRetryInterval(int retryInterval) {
            this.retryInterval = retryInterval;
            return this;
        }

        public Builder setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public WoCloudClient build() {
            return new WoCloudClient(this);
        }
    }
}
