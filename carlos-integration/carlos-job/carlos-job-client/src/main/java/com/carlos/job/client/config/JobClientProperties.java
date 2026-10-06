package com.carlos.job.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 任务客户端配置
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@ConfigurationProperties(prefix = JobClientProperties.PREFIX)
public class JobClientProperties {

    public static final String PREFIX = "carlos.job.client";

    /**
     * 是否启用任务客户端（内部执行端点）
     */
    private Boolean enabled = true;

    /**
     * 内部执行端点访问令牌，非空时调度中心回调必须携带 X-Job-Token 请求头
     */
    private String accessToken;

    /**
     * 注解声明非法（签名不支持、处理器名重名、缺少调度配置等）时是否终止启动，默认 true
     *
     * <p>置为 false 时降级为记 ERROR 并跳过该处理器，用于线上急需重启但注解写错时的应急降噪。</p>
     */
    private Boolean failFastOnInvalid = true;

    /**
     * 是否开启注解任务自动注册（依赖 {@link #adminUrl}，未配置调度中心地址时自动跳过）
     */
    private Boolean autoRegister = true;

    /**
     * 调度中心地址（如 {@code http://127.0.0.1:9516}），配置后才会上报注解任务
     */
    private String adminUrl;

    /**
     * 注册请求连接超时（毫秒）
     */
    private int registerConnectTimeoutMs = 3000;

    /**
     * 注册请求读取超时（毫秒）。注册是短请求，<b>不要</b>复用任务回调的 300s 超时
     */
    private int registerReadTimeoutMs = 10000;

    /**
     * 单个任务的最大注册尝试次数（含首次）
     */
    private int registerMaxAttempts = 3;

    /**
     * 注册重试间隔基准（毫秒），第 n 次失败后等待 {@code n * backoff}
     */
    private int registerRetryBackoffMs = 2000;

}
