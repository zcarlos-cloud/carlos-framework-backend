package com.carlos.system.sequence.apiimpl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiSequence;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.pojo.ao.SequenceBatchAO;
import com.carlos.system.sequence.service.SysSequenceRuleService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 业务编码/序列号 feign 接口实现
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sys/sequence")
@Tag(name = "编码规则feign接口")
@Hidden
public class ApiSequenceImpl implements ApiSequence {

    private final SysSequenceRuleService sequenceRuleService;

    @Override
    @GetMapping("next")
    @Operation(summary = "生成下一个业务编码")
    public Result<String> next(@RequestParam("ruleCode") String ruleCode) {
        if (StrUtil.isBlank(ruleCode)) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY.exception();
        }
        return Result.success(sequenceRuleService.nextCode(ruleCode));
    }

    @Override
    @GetMapping("batch")
    @Operation(summary = "批量预取号段")
    public Result<SequenceBatchAO> nextBatch(@RequestParam("ruleCode") String ruleCode,
                                             @RequestParam("count") int count) {
        if (StrUtil.isBlank(ruleCode)) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY.exception();
        }
        if (count < 1 || count > ApiSequence.MAX_BATCH_SIZE) {
            throw SystemErrorCode.SYS_PARAM_SEQUENCE_BATCH_SIZE_INVALID.exception();
        }
        return Result.success(sequenceRuleService.nextBatch(ruleCode, count));
    }

}
