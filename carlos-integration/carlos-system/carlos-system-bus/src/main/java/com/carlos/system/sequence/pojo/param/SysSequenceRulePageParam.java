package com.carlos.system.sequence.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 业务编码/序列号规则 列表查询参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "编码规则列表查询参数")
public class SysSequenceRulePageParam extends ParamPage {

    @Size(max = 64, message = "规则编码长度不能超过64")
    @Schema(description = "规则编码（模糊）")
    private String ruleCode;

    @Size(max = 128, message = "规则名称长度不能超过128")
    @Schema(description = "规则名称（模糊）")
    private String ruleName;

}
