package com.carlos.system.sequence.pojo.dto;

import com.carlos.system.sequence.pojo.enums.SequenceResetCycleEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 业务编码/序列号规则 数据传输对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class SysSequenceRuleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String prefix;

    private String datePattern;

    private String separator;

    private Integer seqLength;

    private Integer step;

    private SequenceResetCycleEnum resetCycle;

    private Long currentValue;

    private String currentPeriod;

    private Long tenantId;

}
