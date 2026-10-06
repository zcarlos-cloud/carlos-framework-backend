package com.carlos.auth.qrcode;

/**
 * 扫码登录状态机
 *
 * <pre>
 *                ┌──────────┐   生成二维码    ┌──────────┐
 *                │  INIT    │───────────────►│ PENDING  │ (TTL = 120s)
 *                └──────────┘                └────┬─────┘
 *                                                 │
 *                              ┌──────────────────┼───────────────┐
 *                              │                  │               │
 *                              ▼                  ▼               ▼
 *                        ┌──────────┐     ┌──────────┐    ┌──────────┐
 *                        │ EXPIRED  │     │ SCANNED  │    │ CANCELED │
 *                        │ (超时)   │     │(待确认)  │    │(用户取消) │
 *                        └──────────┘     └────┬─────┘    └──────────┘
 *                                              │
 *                                    ┌─────────┼─────────┐
 *                                    ▼                    ▼
 *                              ┌──────────┐        ┌──────────┐
 *                              │CONFIRMED │        │ CANCELED │
 *                              │(签发Token)│        │(APP取消) │
 *                              └──────────┘        └──────────┘
 * </pre>
 *
 * @author Carlos
 * @version 3.0.0
 * @since 2026-07-01
 */
public enum QrCodeState {

    /**
     * 初始状态（未生成）
     */
    INIT,

    /**
     * 待扫码：二维码已生成，等待 APP 扫码
     */
    PENDING,

    /**
     * 已扫码：APP 已扫码，等待用户在 APP 确认
     */
    SCANNED,

    /**
     * 已确认：用户在 APP 确认登录，签发 Token
     */
    CONFIRMED,

    /**
     * 已取消：用户在 APP 取消登录
     */
    CANCELED,

    /**
     * 已过期：二维码超时
     */
    EXPIRED;

    /**
     * 判断状态是否可流转到目标状态
     *
     * @param target 目标状态
     * @return true-允许流转
     */
    public boolean canTransitionTo(QrCodeState target) {
        return switch (this) {
            case INIT -> target == PENDING;
            case PENDING -> target == SCANNED || target == EXPIRED || target == CANCELED;
            case SCANNED -> target == CONFIRMED || target == CANCELED || target == EXPIRED;
            // 终态不可流转
            case CONFIRMED, CANCELED, EXPIRED -> false;
        };
    }

    /**
     * 是否为终态
     */
    public boolean isTerminal() {
        return this == CONFIRMED || this == CANCELED || this == EXPIRED;
    }
}
