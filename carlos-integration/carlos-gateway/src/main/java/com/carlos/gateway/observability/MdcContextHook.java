package com.carlos.gateway.observability;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

/**
 * Reactor Context → MDC 全局桥接钩子安装器
 * <p>
 * 使用 Reactor 官方的 Context Lifting 模式，通过 {@link Hooks#onEachOperator(String, java.util.function.Function)}
 * 把 {@link MdcContextLifter} 包到每一个算子链的订阅者上。
 * <p>
 * 幂等性：{@code Hooks.onEachOperator(key, hook)} 内部按 key 覆盖注册，同一个 key
 * 重复安装不会叠加算子链，因此重复调用 {@link #install()} 是安全的。
 *
 * @author Carlos
 * @date 2026-09-18
 */
@Slf4j
public class MdcContextHook {

    /**
     * 钩子注册键（JVM 全局唯一，改动会导致重复安装）
     */
    private static final String HOOK_KEY = "carlos.gateway.mdcContextLifter";

    private static volatile boolean installed;

    /**
     * 安装全局钩子，重复调用是安全的空操作
     */
    public void install() {
        if (installed) {
            log.debug("[MDC] Reactor Context Hook 已安装, 跳过重复注册 - key: {}", HOOK_KEY);
            return;
        }
        Hooks.onEachOperator(HOOK_KEY,
            Operators.lift((scannable, subscriber) -> new MdcContextLifter<>(subscriber)));
        installed = true;
        log.info("[MDC] Reactor Context -> MDC Hook 安装完成 - key: {}", HOOK_KEY);
    }

    /**
     * 卸载全局钩子（应用关闭时调用）
     */
    @PreDestroy
    public void uninstall() {
        Hooks.resetOnEachOperator(HOOK_KEY);
        installed = false;
        log.info("[MDC] Reactor Context -> MDC Hook 已卸载 - key: {}", HOOK_KEY);
    }
}
