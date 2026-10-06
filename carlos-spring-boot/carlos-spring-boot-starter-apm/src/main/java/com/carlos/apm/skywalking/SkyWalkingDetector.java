package com.carlos.apm.skywalking;

import lombok.extern.slf4j.Slf4j;

/**
 * SkyWalking Agent 自动检测器
 * <p>
 * 通过反射检测 SkyWalking Java Agent 是否已加载，实现渐进式可观测性：
 * <ul>
 *   <li>Agent 不存在 → 使用内置 Brave 链路追踪（Layer 2）</li>
 *   <li>Agent 存在 → SkyWalking 接管传播 + 日志增强（Layer 3）</li>
 * </ul>
 * <p>
 * 检测时机：类加载时（static 块），确保全局唯一且高效。
 *
 * @author Carlos
 * @date 2026-06-16
 */
@Slf4j
public final class SkyWalkingDetector {

    /**
     * 是否已检测到 SkyWalking Agent
     */
    private static final boolean AGENT_PRESENT;

    /**
     * 是否支持上下文传播（Toolkit API 可用）
     */
    private static final boolean TOOLKIT_AVAILABLE;

    static {
        AGENT_PRESENT = detectAgent();
        TOOLKIT_AVAILABLE = detectToolkit();
        if (AGENT_PRESENT) {
            log.info("[APM] 检测到 SkyWalking Agent，增强追踪能力已激活");
        }
        if (TOOLKIT_AVAILABLE) {
            log.debug("[Carlos APM] SkyWalking Toolkit API 可用");
        }
    }

    private SkyWalkingDetector() {
        // 工具类禁止实例化
    }

    /**
     * 检测 SkyWalking Agent 核心类是否存在
     */
    private static boolean detectAgent() {
        try {
            Class.forName("org.apache.skywalking.apm.agent.core.boot.AgentBoot");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (LinkageError e) {
            log.debug("[Carlos APM] SkyWalking Agent 类加载失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检测 SkyWalking Toolkit API 是否可用
     */
    private static boolean detectToolkit() {
        try {
            Class.forName("org.apache.skywalking.apm.toolkit.trace.TraceContext");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (LinkageError e) {
            log.debug("[Carlos APM] SkyWalking Toolkit 类加载失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 判断 SkyWalking Agent 是否已部署
     *
     * @return true 如果 Agent 存在
     */
    public static boolean isAgentPresent() {
        return AGENT_PRESENT;
    }

    /**
     * 判断 SkyWalking Toolkit API 是否可用
     * <p>
     * Toolkit 可以在不部署 Agent 的情况下使用（仅提供 MDC/日志增强），
     * 但完整的追踪上下文传播需要 Agent。
     *
     * @return true 如果 Toolkit 可用
     */
    public static boolean isToolkitAvailable() {
        return TOOLKIT_AVAILABLE;
    }

    /**
     * 判断是否应启用 SkyWalking 增强模式
     * <p>
     * 增强模式需要 Agent + Toolkit 都可用。
     *
     * @return true 如果应启用增强模式
     */
    public static boolean shouldEnhance() {
        return AGENT_PRESENT && TOOLKIT_AVAILABLE;
    }
}
