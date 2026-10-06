package com.carlos.message.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 消息渠道配置 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("message_channel")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MessageChannel extends Model<MessageChannel> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 渠道编码
     */
    @TableField(value = "channel_code")
    private String channelCode;
    /**
     * 渠道名称
     */
    @TableField(value = "channel_name")
    private String channelName;
    /**
     * 渠道类型: 1-短信 2-邮件 3-钉钉 4-企业微信 5-站内信
     */
    @TableField(value = "channel_type")
    private Integer channelType;
    /**
     * 服务商编码
     */
    @TableField(value = "provider")
    private String provider;
    /**
     * 渠道配置JSON
     */
    @TableField(value = "channel_config")
    private String channelConfig;
    /**
     * 每秒最大请求数
     */
    @TableField(value = "rate_limit_qps")
    private Integer rateLimitQps;
    /**
     * 突发流量限制
     */
    @TableField(value = "rate_limit_burst")
    private Integer rateLimitBurst;
    /**
     * 最大重试次数
     */
    @TableField(value = "retry_times")
    private Integer retryTimes;
    /**
     * 重试间隔(ms)
     */
    @TableField(value = "retry_interval")
    private Integer retryInterval;
    /**
     * 权重
     */
    @TableField(value = "weight")
    private Integer weight;
    /**
     * 是否启用: 0-禁用 1-启用 2-故障
     */
    @TableField(value = "is_enabled")
    private Integer enabled;
    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;
    /**
     * 创建者编号
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 更新者编号
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

}
