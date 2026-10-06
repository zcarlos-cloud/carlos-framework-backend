package com.carlos.mq.exception;

import com.carlos.mq.core.MqCapability;
import com.carlos.mq.error.MqErrorCode;
import com.carlos.mq.support.MqType;

/**
 * 传输实现不支持所请求的能力
 * <p>
 * 本异常是「<b>不得静默降级</b>」这一硬约束的落地手段：当调用方请求的能力
 * （延迟、顺序、事务、标签过滤等）未被当前传输声明支持时，必须抛出本异常，
 * 而不是悄悄退化成一个语义不同的操作并返回「成功」。
 * </p>
 * <p>
 * 携带 {@link MqCapability} 与 {@link MqType}，便于调用方与监控按能力维度区分处理。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
public class MqCapabilityNotSupportedException extends MqException {

    private static final long serialVersionUID = 1L;

    /** 未被支持的能力 */
    private final transient MqCapability capability;

    /** 触发该错误的传输类型 */
    private final transient MqType transportType;

    /**
     * @param capability    未被支持的能力
     * @param transportType 传输类型
     */
    public MqCapabilityNotSupportedException(MqCapability capability, MqType transportType) {
        super(MqErrorCode.MQ_CAPABILITY_NOT_SUPPORTED,
            "传输 [%s] 不支持能力 [%s]".formatted(
                transportType == null ? "unknown" : transportType.getCode(),
                capability == null ? "unknown" : capability.getCode()));
        this.capability = capability;
        this.transportType = transportType;
    }

    public MqCapability getCapability() {
        return capability;
    }

    public MqType getTransportType() {
        return transportType;
    }
}
