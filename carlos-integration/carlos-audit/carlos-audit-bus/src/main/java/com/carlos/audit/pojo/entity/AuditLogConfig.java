package com.carlos.audit.pojo.entity;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("audit_log_config")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogConfig extends Model<AuditLogConfig> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 日志类型，如：USER_LOGIN、ORDER_PAY 等
     * 支持业务系统自定义类型值
     */
    @TableField(value = "log_type")
    private String logType;
    /**
     * 保留天数
     */
    @TableField(value = "retention_days")
    private Integer retentionDays;
    /**
     * 采样率 0.00-1.00，1.00为全量
     */
    @TableField(value = "sampling_rate")
    private BigDecimal samplingRate;
    /**
     * 是否异步写入：0-同步/1-异步
     */
    @TableField(value = "async_write")
    private Boolean asyncWrite;
    /**
     * 是否存储数据变更：0-否/1-是
     */
    @TableField(value = "store_data_change")
    private Boolean storeDataChange;
    /**
     * 是否存储技术上下文：0-否/1-是
     */
    @TableField(value = "store_technical")
    private Boolean storeTechnical;
    /**
     * 逻辑删除：0-未删除/1-已删除
     */
    @TableLogic
    @TableField(value = "deleted")
    private Boolean deleted;
    /**
     * 租户ID，0表示系统级配置
     */
    @TableField(value = "tenant_id")
    private String tenantId;
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
