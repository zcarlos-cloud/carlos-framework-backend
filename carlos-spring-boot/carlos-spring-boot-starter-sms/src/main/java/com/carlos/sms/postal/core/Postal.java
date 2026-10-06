package com.carlos.sms.postal.core;

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
public class Postal {

    /**
     * 发送单条短信
     */
    public static final String SEND_SMS = "/send/sms";
    /**
     * 发送批量短信
     */
    public static final String SEND_BATCH_SMS = "/send/batchSms";

    /**
     * 模仿SDK版本
     */
    public static final String VERSION = "1.0.0";

    public static String signingAlgorithm = "hmac-sha256";
    public static String endpoint = System.getenv().getOrDefault("POSTAL_ENDPOINT", "https://dx.11185.cn:13011/isms-send-api");
    public static String accessKeyId = System.getenv("POSTAL_ACCESS_KEY_ID");

    private static String accessKeySecret = System.getenv("POSTAL_ACCESS_KEY_SECRET");


    private static volatile PostalClient client;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private Postal() {
    }


    /**
     * 初始化Postal环境(HMAC验证模式).
     *
     * @param accessKeyId     access key ID
     * @param accessKeySecret access key secret
     * @author Carlos
     */
    public static void init(final String accessKeyId, final String accessKeySecret) {
        Postal.setAccessKeyId(accessKeyId);
        Postal.setAccessKeySecret(accessKeySecret);
    }


    public static void setAccessKeyId(final String accessKeyId) {
        Postal.accessKeyId = accessKeyId;
    }


    public static void setAccessKeySecret(final String accessKeySecret) {
        Postal.accessKeySecret = accessKeySecret;
    }


    public static void setEndpoint(final String endpoint) {
        Postal.endpoint = endpoint;
    }

    /**
     * 返回(如果未初始化则初始化)统一客户端.
     *
     * @return the Postal Client
     * @author Carlos
     */
    public static PostalClient getClient(int retryInterval, int maxRetries) {
        if (Postal.client == null) {
            LOCK.lock();
            try {
                if (Postal.client == null) {
                    Postal.client = buildClient(retryInterval, maxRetries);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return Postal.client;
    }

    public static void setClient(final PostalClient client) {
        LOCK.lock();
        try {
            Postal.client = client;
        } finally {
            LOCK.unlock();
        }
    }

    private static PostalClient buildClient(int retryInterval, int maxRetries) {
        PostalClient.Builder builder = new PostalClient.Builder(Postal.accessKeyId);

        if (Postal.accessKeySecret != null) {
            builder.accessKeySecret(Postal.accessKeySecret);
        }

        builder.endpoint(Postal.endpoint);
        builder.setRetryInterval(retryInterval);
        builder.setMaxRetries(maxRetries);
        return builder.build();
    }
}
