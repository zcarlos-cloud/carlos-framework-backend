package com.carlos.system.sequence.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 业务编码/序列号规则 视图对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "编码规则")
public class SysSequenceRuleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "规则ID")
    private Long id;

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "编码前缀")
    private String prefix;

    @Schema(description = "日期段格式（空表示不含日期段）")
    private String datePattern;

    @Schema(description = "分隔符")
    private String separator;

    @Schema(description = "流水位数")
    private Integer seqLength;

    @Schema(description = "单次自增步长")
    private Integer step;

    @Schema(description = "重置周期编码：1-永不重置 2-按日 3-按月 4-按年")
    private Integer resetCycle;

    @Schema(description = "重置周期名称")
    private String resetCycleName;

    @Schema(description = "数据库兜底计数水位（正常态真实计数在 Redis，本值仅供排查参考）")
    private Long currentValue;

    @Schema(description = "兜底计数所属日期段")
    private String currentPeriod;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
