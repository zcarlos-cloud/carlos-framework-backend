package com.carlos.job.security;

import com.carlos.core.auth.SignatureUtil;

/**
 * <p>
 * 内部调用令牌校验（恒定时间比较，防时序侧信道）
 * </p>
 *
 * <p>契约：期望令牌为空表示未开启校验（内网部署），此时一律放行；非空时请求必须携带
 * {@code X-Job-Token} 且与期望值完全一致。</p>
 *
 * <p>比较实现统一收敛到 {@link SignatureUtil#constantTimeEquals} —— 此前仓库里散落着 3 份
 * 各写各的常量时间比较，容易被后人改坏其中一份而无人察觉。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
public final class JobTokenValidator {

    private JobTokenValidator() {
    }

    /**
     * 校验内部调用令牌
     *
     * @param expected 期望令牌（配置项），为空表示未开启校验
     * @param actual   请求携带的令牌
     * @return 是否通过
     */
    public static boolean valid(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null) {
            return false;
        }
        return SignatureUtil.constantTimeEquals(expected, actual);
    }

}
