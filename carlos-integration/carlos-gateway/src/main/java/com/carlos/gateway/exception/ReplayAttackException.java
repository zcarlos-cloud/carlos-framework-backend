package com.carlos.gateway.exception;

import com.carlos.core.response.CommonErrorCode;

/**
 * <p>
 * 重放攻击异常
 * 用于处理请求重放攻击检测场景
 * 返回 HTTP 403 Forbidden
 * </p>
 *
 * @author carlos
 * @date 2026/3/24
 */
public class ReplayAttackException extends GatewayException {

    private static final long serialVersionUID = 1L;

    /**
     * 攻击类型
     */
    private final AttackType attackType;

    /**
     * 请求 ID/Nonce
     */
    private final String requestId;

    public ReplayAttackException(String message) {
        super(CommonErrorCode.FORBIDDEN, message);
        this.attackType = AttackType.UNKNOWN;
        this.requestId = null;
    }

    public ReplayAttackException(String message, AttackType attackType, String requestId) {
        super(CommonErrorCode.FORBIDDEN, message);
        this.attackType = attackType;
        this.requestId = requestId;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public String getRequestId() {
        return requestId;
    }

    /**
     * 攻击类型枚举
     */
    public enum AttackType {
        /**
         * 重复的 Nonce
         */
        DUPLICATE_NONCE,
        /**
         * 时间戳过期
         */
        EXPIRED_TIMESTAMP,
        /**
         * 签名无效
         */
        INVALID_SIGNATURE,
        /**
         * 未知类型
         */
        UNKNOWN
    }
}
