package com.carlos.system.sequence.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 业务编码/序列号规则 数据源对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@TableName("sys_sequence_rule")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysSequenceRule implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 主键（雪花 ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 规则编码（业务标识，如 order_no；创建后不可修改）
     */
    @TableField("rule_code")
    private String ruleCode;
    /**
     * 规则名称
     */
    @TableField("rule_name")
    private String ruleName;
    /**
     * 编码前缀（如 PO，可空）
     */
    @TableField("prefix")
    private String prefix;
    /**
     * 日期段格式（如 yyyyMMdd，空表示不含日期段）
     */
    @TableField("date_pattern")
    private String datePattern;
    /**
     * 日期段与流水之间的分隔符（如 -，可空）
     */
    @TableField("separator")
    private String separator;
    /**
     * 流水位数
     */
    @TableField("seq_length")
    private Integer seqLength;
    /**
     * 单次自增步长
     */
    @TableField("step")
    private Integer step;
    /**
     * 重置周期：1-永不重置 2-按日 3-按月 4-按年
     */
    @TableField("reset_cycle")
    private SequenceResetCycleEnum resetCycle;
    /**
     * 数据库兜底计数（正常态由 Redis 承载，此列仅 Redis 不可用时自增、并作为计数水位线）
     */
    @TableField("current_value")
    private Long currentValue;
    /**
     * 兜底计数所属日期段
     */
    @TableField("current_period")
    private String currentPeriod;
    /**
     * 租户ID（预留，system 模块暂无租户拦截器）
     */
    @TableField("tenant_id")
    private Long tenantId;
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;
    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField(value = "is_deleted")
    private Boolean deleted;

}
