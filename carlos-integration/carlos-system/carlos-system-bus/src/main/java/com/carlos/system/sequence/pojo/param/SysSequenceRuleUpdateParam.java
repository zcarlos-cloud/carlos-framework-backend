package com.carlos.system.sequence.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 业务编码/序列号规则 更新参数
 * </p>
 *
 * <p>规则编码（ruleCode）是业务标识与 Redis 键的一部分，创建后不可修改，故不在本参数中。
 * 字符串字段传空串表示「清空该项」（null 会被 MyBatis-Plus 的 NOT_NULL 更新策略忽略而保持原值）。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "编码规则更新参数")
public class SysSequenceRuleUpdateParam {

    @NotNull(message = "规则ID不能为空")
    @Schema(description = "规则ID")
    private Long id;

    @Size(max = 128, message = "规则名称最长 128 位")
    @Schema(description = "规则名称")
    private String ruleName;

    @Size(max = 32, message = "前缀最长 32 位")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "前缀仅允许字母、数字、下划线、短横线")
    @Schema(description = "编码前缀，传空串清空")
    private String prefix;

    @Size(max = 32, message = "日期段格式最长 32 位")
    @Schema(description = "日期段格式，传空串清空（清空后流水号不再含日期，重置周期须改为「永不重置」）")
    private String datePattern;

    @Size(max = 8, message = "分隔符最长 8 位")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "分隔符仅允许字母、数字、下划线、短横线")
    @Schema(description = "分隔符，传空串清空")
    private String separator;

    @Min(value = 1, message = "流水位数最小 1")
    @Max(value = 12, message = "流水位数最大 12")
    @Schema(description = "流水位数（左补零）")
    private Integer seqLength;

    @Min(value = 1, message = "步长最小 1")
    @Max(value = 10000, message = "步长最大 10000")
    @Schema(description = "单次自增步长")
    private Integer step;

    @Min(value = 1, message = "重置周期最小 1")
    @Max(value = 4, message = "重置周期最大 4")
    @Schema(description = "重置周期：1-永不重置 2-按日 3-按月 4-按年")
    private Integer resetCycle;

}
