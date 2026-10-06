package com.carlos.mq.core;

/**
 * 异步发送完成回调
 * <p>
 * 采用<b>单一回调</b>而非「onSuccess/onException」双回调：失败也以
 * {@link SendResult}（含 {@link SendStatus} 与异常）承载，与同步发送的失败表达方式一致，
 * 调用方只需检查结果状态，不必写两套分支。
 * </p>
 * <p>因此本接口是函数式接口，可直接用 lambda：{@code callback -> ...}。</p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@FunctionalInterface
public interface MqSendCallback {

    /**
     * 发送完成（无论成功或失败）
     *
     * @param sendResult 发送结果；失败时 {@code isSuccess()} 为 false 且可能带异常
     */
    void onComplete(SendResult sendResult);
}
