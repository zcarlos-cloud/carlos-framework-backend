package com.carlos.system.sequence.controller;

import com.carlos.core.exception.BusinessException;
import com.carlos.system.sequence.manager.SysSequenceRuleManager;
import com.carlos.system.sequence.service.SysSequenceRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SysSequenceRuleController 单元测试
 *
 * <p>只覆盖新增的 {@code POST /sys/sequence/next}：它是前端表单「生成」按钮的入口，
 * 与 Feign 的 {@code ApiSequenceImpl#next} 必须同口径 —— 尤其是空 ruleCode 的拦截，
 * 漏掉的话请求会一路走到规则查询、以「规则不存在」的形式误导排查方向。</p>
 *
 * @author carlos
 */
@ExtendWith(MockitoExtension.class)
class SysSequenceRuleControllerTest {

    /**
     * 空 ruleCode 对应的错误码（SystemErrorCode.SYS_PARAM_SEQUENCE_RULE_CODE_EMPTY）
     */
    private static final String CODE_RULE_CODE_EMPTY = "10906";

    @Mock
    private SysSequenceRuleService sequenceRuleService;

    @Mock
    private SysSequenceRuleManager sequenceRuleManager;

    @InjectMocks
    private SysSequenceRuleController controller;

    @Test
    @DisplayName("ruleCode 为空/空白/纯空格时一律拒绝，且不触碰生成链路")
    void should_reject_blank_rule_code() {
        for (String ruleCode : Arrays.asList(null, "", "   ")) {
            BusinessException exception = assertThrows(BusinessException.class, () -> controller.next(ruleCode));

            assertEquals(CODE_RULE_CODE_EMPTY, exception.getErrorCode());
        }

        // 关键：校验必须发生在生成之前，否则空 ruleCode 会去查规则、报成「规则不存在」
        verifyNoInteractions(sequenceRuleService);
    }

    @Test
    @DisplayName("正常路径直接透传 service 生成的编码")
    void should_return_code_generated_by_service() {
        when(sequenceRuleService.nextCode("order_no")).thenReturn("PO20260927-0001");

        assertEquals("PO20260927-0001", controller.next("order_no"));

        verify(sequenceRuleService).nextCode("order_no");
    }
}
