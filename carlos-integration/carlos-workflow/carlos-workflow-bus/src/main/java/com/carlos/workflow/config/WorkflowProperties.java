package com.carlos.workflow.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * 工作流组件配置项，配置前缀 carlos.workflow。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@ConfigurationProperties(prefix = "carlos.workflow")
public class WorkflowProperties {

    /**
     * 是否启用工作流组件，由自动配置类的 @ConditionalOnProperty 读取
     */
    private boolean enabled = true;

    /**
     * 办理人解析相关配置
     */
    private Assignee assignee = new Assignee();

    /**
     * 办理人解析配置
     */
    @Data
    public static class Assignee {

        /**
         * 节点配了办理人却解析不到任何人时是否直接拒绝创建待办任务（fail-closed），默认 true。
         *
         * <p>置为 false 时降级为 WARN 日志。⚠️ 降级有安全代价：该节点的
         * {@code flow_user.processed_by} 为空，引擎的 {@code checkAuth} 对办理人为空的任务不做交集校验，
         * 等于该节点审批对所有人开放，且任务不会出现在任何人的待办列表里。仅用于存量流程的过渡期。</p>
         */
        private boolean failFast = true;
    }

}
