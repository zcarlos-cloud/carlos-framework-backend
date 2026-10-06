package com.carlos.sms.postal.core;

import cn.hutool.json.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.comm.exception.SmsBlendException;
import org.dromara.sms4j.comm.utils.SmsHttpUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class PostalClient {

    public static final String USER_AGENT = "postal-java-sdk" + "/" + Postal.VERSION;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;
    private final int retryInterval;
    private final int maxRetries;
    private int retry = 0;
    private final SmsHttpUtils http = SmsHttpUtils.instance();

    protected PostalClient(Builder b) {
        this.accessKeyId = b.accessKeyId;
        this.accessKeySecret = b.accessKeySecret;
        this.endpoint = b.endpoint;
        this.retryInterval = b.retryInterval;
        this.maxRetries = b.maxRetries;
    }

    /**
     * request
     * <p>向 postal-sms发送请求
     *
     * @param uri 接口名称
     * @author Carlos
     */
    public PostalResponse request(final String uri, final Map<String, Object> data) throws SmsBlendException {
        Map<String, String> headers = new HashMap<>();
        // headers.put("User-Agent", USER_AGENT);
        // headers.put("Content-Type", Constant.APPLICATION_JSON_UTF8);
        // headers.put("Accept", Constant.ACCEPT);
        headers.put("appKey", accessKeyId);
        headers.put("appSecret", accessKeySecret);

        String url = this.endpoint + uri;

        try {
            JSONObject response = http.postFrom(url, headers, data);
            PostalResponse smsResponse = response.toBean(PostalResponse.class);
            if ("200".equals(smsResponse.getErrorCode()) || retry == maxRetries) {
                retry = 0;
                return smsResponse;
            }
            return requestRetry(uri, data);
        } catch (SmsBlendException e) {
            return requestRetry(uri, data);
        }
    }

    private PostalResponse requestRetry(String action, Map<String, Object> data) {
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

        public PostalClient build() {
            return new PostalClient(this);
        }
    }
}
