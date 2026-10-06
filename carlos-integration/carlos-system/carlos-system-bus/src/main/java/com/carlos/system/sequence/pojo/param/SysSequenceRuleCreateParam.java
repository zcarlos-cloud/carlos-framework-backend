package com.carlos.system.sequence.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 业务编码/序列号规则 新增参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@Schema(description = "编码规则新增参数")
public class SysSequenceRuleCreateParam {

    /**
     * 规则编码会拼进 Redis 键，故限定为字母开头的字母/数字/下划线，避免 glob 元字符（* ? [ ]）与冒号
     */
    @NotBlank(message = "规则编码不能为空")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]{1,63}$",
        message = "规则编码仅允许字母开头的字母、数字、下划线，长度 2~64 位")
    @Schema(description = "规则编码（业务标识，如 order_no，创建后不可修改）")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称最长 128 位")
    @Schema(description = "规则名称")
    private String ruleName;

    @Size(max = 32, message = "前缀最长 32 位")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "前缀仅允许字母、数字、下划线、短横线")
    @Schema(description = "编码前缀（如 PO），留空表示无前缀")
    private String prefix;

    @Size(max = 32, message = "日期段格式最长 32 位")
    @Schema(description = "日期段格式，取值 yyyy / yyyyMM / yyyyMMdd / yyyyMMddHH / yyyyMMddHHmmss，留空表示不含日期段")
    private String datePattern;

    @Size(max = 8, message = "分隔符最长 8 位")
    @Pattern(regexp = "^[A-Za-z0-9_-]*$", message = "分隔符仅允许字母、数字、下划线、短横线")
    @Schema(description = "日期段与流水之间的分隔符（如 -），留空表示无分隔符")
    private String separator;

    @NotNull(message = "流水位数不能为空")
    @Min(value = 1, message = "流水位数最小 1")
    @Max(value = 12, message = "流水位数最大 12")
    @Schema(description = "流水位数（左补零），如 4 表示 0001")
    private Integer seqLength;

    @NotNull(message = "步长不能为空")
    @Min(value = 1, message = "步长最小 1")
    @Max(value = 10000, message = "步长最大 10000")
    @Schema(description = "单次自增步长，默认 1")
    private Integer step;

    @NotNull(message = "重置周期不能为空")
    @Min(value = 1, message = "重置周期最小 1")
    @Max(value = 4, message = "重置周期最大 4")
    @Schema(description = "重置周期：1-永不重置 2-按日 3-按月 4-按年")
    private Integer resetCycle;

}
