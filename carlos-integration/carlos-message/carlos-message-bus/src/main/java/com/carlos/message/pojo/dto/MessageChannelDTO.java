package com.carlos.message.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 消息渠道配置 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@Accessors(chain = true)
public class MessageChannelDTO {
    /** 主键ID */
    private Long id;
    /** 渠道编码 */
    private String channelCode;
    /** 渠道名称 */
    private String channelName;
    /** 渠道类型: 1-短信 2-邮件 3-钉钉 4-企业微信 5-站内信 */
    private Integer channelType;
    /** 服务商编码 */
    private String provider;
    /** 渠道配置JSON */
    private String channelConfig;
    /** 每秒最大请求数 */
    private Integer rateLimitQps;
    /** 突发流量限制 */
    private Integer rateLimitBurst;
    /** 最大重试次数 */
    private Integer retryTimes;
    /** 重试间隔(ms) */
    private Integer retryInterval;
    /** 权重 */
    private Integer weight;
    /** 是否启用: 0-禁用 1-启用 2-故障 */
    private Integer enabled;
    /** 创建者编号 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新者编号 */
    private Long updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
