package com.carlos.job.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 调度中心配置
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@ConfigurationProperties(prefix = JobProperties.PREFIX)
public class JobProperties {

    public static final String PREFIX = "carlos.job";

    /**
     * 回调业务服务时携带的访问令牌（与业务端 carlos.job.client.access-token 配对）
     */
    private String accessToken;

    /**
     * 回调连接超时（毫秒）
     */
    private Integer connectTimeoutMs = 5000;

    /**
     * 回调读取超时（毫秒），长耗时任务需调大
     */
    private Integer readTimeoutMs = 300000;

    /**
     * 启动时是否执行 job_info 与 Quartz 对账（以 job_info 为准修复执行面漂移）
     */
    private Boolean reconcileOnStartup = true;

    /**
     * 任务失败报警配置
     */
    private Alarm alarm = new Alarm();

    /**
     * 任务失败报警配置
     */
    @Data
    public static class Alarm {

        /**
         * 是否开启任务失败报警（对接 carlos-message 消息中心）
         */
        private Boolean enabled = false;

        /**
         * 报警消息模板编码（需在 carlos-message 中预先配置）
         */
        private String templateCode = "JOB_FAIL_ALARM";

        /**
         * 发送系统标识
         */
        private String senderSystem = "carlos-job";

    }

}
