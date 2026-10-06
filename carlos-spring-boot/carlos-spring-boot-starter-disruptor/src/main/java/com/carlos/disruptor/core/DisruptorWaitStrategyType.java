package com.carlos.disruptor.core;

import com.lmax.disruptor.*;

public enum DisruptorWaitStrategyType {
    BLOCKING,
    SLEEPING,
    YIELDING,
    BUSY_SPIN;

    /**
     * 转换为 Disruptor WaitStrategy
     */
    public WaitStrategy toStrategy() {
        return switch (this) {
            case BLOCKING -> new BlockingWaitStrategy();
            case SLEEPING -> new SleepingWaitStrategy();
            case YIELDING -> new YieldingWaitStrategy();
            case BUSY_SPIN -> new BusySpinWaitStrategy();
        };
    }
}
