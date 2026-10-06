package com.carlos.mq.error;

import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MQ 错误码护栏测试
 * <p>
 * {@link MqErrorCode} 是 MQ 错误码的唯一真源：编码格式、段位、唯一性以及与框架
 * {@link CommonErrorCode} 的不冲突性都必须被断言，避免码表分叉或撞码。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@DisplayName("MqErrorCode 错误码规范")
class MqErrorCodeTest {

    @Test
    @DisplayName("所有错误码应为 5 位数字，且模块编码为 00 通用段")
    void should_be_five_digit_with_module_code_00() {
        assertAll(Arrays.stream(MqErrorCode.values())
            .map(code -> () -> {
                assertTrue(code.getCode().matches("\\d{5}"),
                    code.name() + " 的码必须是 5 位数字，实际=" + code.getCode());
                assertEquals("00", code.getCode().substring(1, 3),
                    code.name() + " 应属于 00 通用段");
            }));
    }

    @Test
    @DisplayName("错误级别首位应为 1/2/3/5 之一")
    void should_have_valid_error_level() {
        Set<Character> validLevels = Set.of('1', '2', '3', '5');
        for (MqErrorCode code : MqErrorCode.values()) {
            assertTrue(validLevels.contains(code.getCode().charAt(0)),
                code.name() + " 的错误级别非法：" + code.getCode());
        }
    }

    @Test
    @DisplayName("码值在本枚举内唯一")
    void should_have_unique_codes() {
        Set<String> seen = new HashSet<>();
        for (MqErrorCode code : MqErrorCode.values()) {
            assertTrue(seen.add(code.getCode()), "重复的错误码：" + code.getCode());
        }
    }

    @Test
    @DisplayName("不得与 CommonErrorCode 已占用的码值冲突")
    void should_not_collide_with_common_error_code() {
        Set<String> common = Arrays.stream(CommonErrorCode.values())
            .map(ErrorCode::getCode)
            .collect(Collectors.toSet());

        for (MqErrorCode code : MqErrorCode.values()) {
            assertFalse(common.contains(code.getCode()),
                "与 CommonErrorCode 撞码：" + code.name() + "=" + code.getCode());
        }
    }

    @Test
    @DisplayName("不得占用已满的 50001-50009 系统段")
    void should_avoid_occupied_system_range() {
        for (MqErrorCode code : MqErrorCode.values()) {
            int numeric = Integer.parseInt(code.getCode());
            assertFalse(numeric >= 50001 && numeric <= 50009,
                code.name() + " 落在已占用的 50001-50009 范围内");
        }
    }

    @Test
    @DisplayName("关键错误码应可构造异常，且异常携带相同码值")
    void should_produce_exception_with_same_code() {
        MqErrorCode capabilityCode = MqErrorCode.MQ_CAPABILITY_NOT_SUPPORTED;
        assertEquals("20002", capabilityCode.getCode());
        assertEquals(501, capabilityCode.getHttpStatus());
        assertEquals("20002", capabilityCode.exception().getErrorCode());
    }
}
