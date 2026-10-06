package com.carlos.gateway.observability;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

/**
 * Reactor Context → SLF4J MDC 桥接订阅者
 * <p>
 * 在每个信号（onSubscribe / onNext / onError / onComplete）投递前，把 Reactor Context 中的
 * traceId / spanId 同步进当前线程的 MDC，使任意线程上的日志都能自动带上链路信息。
 * <p>
 * 只在每个算子（operator）上包装才能覆盖整条链：链内部的 filter、doFinally 各自持有
 * 独立的 Subscriber，只包装最外层拿不到链内部的 MDC。
 * <p>
 * MDC 清理策略：
 * <ul>
 *   <li>每次信号：Context 有值则 {@code put}，无值则 {@code remove}，不等则更新——
 *       避免线程池线程上残留上一个请求的值</li>
 *   <li>终结信号：转发完成后清理追踪键，缩小残留窗口</li>
 *   <li>只操作两个追踪键，不用 {@code MDC.clear()}——业务组件可能写入自己的 MDC 条目</li>
 * </ul>
 *
 * @param <T> 元素类型
 * @author Carlos
 * @date 2026-09-18
 */
public class MdcContextLifter<T> implements CoreSubscriber<T> {

    /**
     * Context 缺键时的取值占位
     */
    private static final String EMPTY_VALUE = "";

    private final CoreSubscriber<? super T> delegate;

    public MdcContextLifter(CoreSubscriber<? super T> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Context currentContext() {
        return delegate.currentContext();
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        copyToMdc();
        delegate.onSubscribe(subscription);
    }

    @Override
    public void onNext(T value) {
        copyToMdc();
        delegate.onNext(value);
    }

    @Override
    public void onError(Throwable throwable) {
        copyToMdc();
        try {
            delegate.onError(throwable);
        } finally {
            clearTraceKeys();
        }
    }

    @Override
    public void onComplete() {
        copyToMdc();
        try {
            delegate.onComplete();
        } finally {
            clearTraceKeys();
        }
    }

    /**
     * 把当前 Context 中的追踪信息同步到 MDC
     */
    private void copyToMdc() {
        Context context = delegate.currentContext();
        sync(TraceContextKeys.TRACE_ID, context);
        sync(TraceContextKeys.SPAN_ID, context);
    }

    /**
     * 同步单个键
     *
     * @param key     Context 与 MDC 共用的键名
     * @param context 当前订阅者持有的 Reactor Context
     */
    private void sync(String key, Context context) {
        String value = context.getOrDefault(key, EMPTY_VALUE);
        if (value.equals(MDC.get(key))) {
            return;
        }
        if (value.isEmpty()) {
            MDC.remove(key);
        } else {
            MDC.put(key, value);
        }
    }

    /**
     * 清理追踪键
     */
    private void clearTraceKeys() {
        MDC.remove(TraceContextKeys.TRACE_ID);
        MDC.remove(TraceContextKeys.SPAN_ID);
    }
}
