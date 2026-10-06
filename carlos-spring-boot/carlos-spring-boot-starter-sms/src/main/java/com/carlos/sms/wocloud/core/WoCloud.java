package com.carlos.sms.wocloud.core;

import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * 初始化统一环境的单例类
 * <a href="https://dx.11185.cn/isms-doc/?t=0.4739874875910559">文档地址</a>
 * </p>
 *
 * @author Carlos
 * @date 2023/11/21 22:17
 */
public class WoCloud {

    /**
     * 发送单条短信
     */
    public static final String SEND_SMS = "/Sms/Api/Send";


    public static String endpoint = System.getenv().getOrDefault("WOCLOUD_ENDPOINT", "https://api.028lk.com");
    public static String accessKeyId = System.getenv("WOCLOUD_ACCESS_KEY_ID");

    private static String accessKeySecret = System.getenv("WOCLOUD_ACCESS_KEY_SECRET");

    private static volatile WoCloudClient client;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private WoCloud() {
    }


    /**
     * 初始化WoCloud环境
     *
     * @param accessKeyId     access key ID
     * @param accessKeySecret access key secret
     * @author Carlos
     */
    public static void init(final String accessKeyId, final String accessKeySecret) {
        WoCloud.setAccessKeyId(accessKeyId);
        WoCloud.setAccessKeySecret(accessKeySecret);
    }


    public static void setAccessKeyId(final String accessKeyId) {
        WoCloud.accessKeyId = accessKeyId;
    }


    public static void setAccessKeySecret(final String accessKeySecret) {
        WoCloud.accessKeySecret = accessKeySecret;
    }


    public static void setEndpoint(final String endpoint) {
        WoCloud.endpoint = endpoint;
    }

    /**
     * 返回(如果未初始化则初始化)统一客户端.
     *
     * @return the WoCloud Client
     * @author Carlos
     */
    public static WoCloudClient getClient(int retryInterval, int maxRetries) {
        if (WoCloud.client == null) {
            LOCK.lock();
            try {
                if (WoCloud.client == null) {
                    WoCloud.client = buildClient(retryInterval, maxRetries);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return WoCloud.client;
    }

    public static void setClient(final WoCloudClient client) {
        LOCK.lock();
        try {
            WoCloud.client = client;
        } finally {
            LOCK.unlock();
        }
    }

    private static WoCloudClient buildClient(int retryInterval, int maxRetries) {
        WoCloudClient.Builder builder = new WoCloudClient.Builder(WoCloud.accessKeyId);

        if (WoCloud.accessKeySecret != null) {
            builder.accessKeySecret(WoCloud.accessKeySecret);
        }

        builder.endpoint(WoCloud.endpoint);
        builder.setRetryInterval(retryInterval);
        builder.setMaxRetries(maxRetries);
        return builder.build();
    }
}
