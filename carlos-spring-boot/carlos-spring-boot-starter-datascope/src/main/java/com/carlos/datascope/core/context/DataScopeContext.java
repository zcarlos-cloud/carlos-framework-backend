package com.carlos.datascope.core.context;

import com.carlos.datascope.core.model.DataScopeResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限上下文
 * <p>
 * 保存一次请求的数据权限处理信息
 *
 * @author Carlos
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataScopeContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 上下文ID
     */
    private String contextId;

    /**
     * 处理结果
     */
    private DataScopeResult result;

    /**
     * 目标方法
     */
    private transient Method method;

    /**
     * 方法参数
     */
    private transient Object[] args;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行耗时(ms)
     */
    private Long executionTime;

    /**
     * 是否已脱敏处理
     */
    private boolean maskingApplied;

    /**
     * 执行过程中的日志
     */
    @Builder.Default
    private List<String> executionLogs = new ArrayList<>();

    /**
     * 创建新的上下文
     *
     * @return DataScopeContext
     */
    public static DataScopeContext create() {
        return DataScopeContext.builder()
            .contextId(java.util.UUID.randomUUID().toString())
            .startTime(LocalDateTime.now())
            .executionLogs(new ArrayList<>())
            .build();
    }

    /**
     * 记录执行日志
     *
     * @param message 日志消息
     */
    public void log(String message) {
        if (executionLogs == null) {
            executionLogs = new ArrayList<>();
        }
        executionLogs.add(LocalDateTime.now() + " - " + message);
    }

    /**
     * 标记完成
     */
    public void complete() {
        this.endTime = LocalDateTime.now();
        if (this.startTime != null) {
            this.executionTime = java.time.Duration.between(this.startTime, this.endTime).toMillis();
        }
    }

    /**
     * 检查是否有规则
     *
     * @return true 有规则
     */
    public boolean hasRules() {
        return result != null && result.getMatchedRuleIds() != null && !result.getMatchedRuleIds().isEmpty();
    }

    /**
     * 检查是否需要脱敏
     *
     * @return true 需要脱敏
     */
    public boolean needsMasking() {
        return result != null && result.getMaskingColumns() != null && !result.getMaskingColumns().isEmpty();
    }
}
