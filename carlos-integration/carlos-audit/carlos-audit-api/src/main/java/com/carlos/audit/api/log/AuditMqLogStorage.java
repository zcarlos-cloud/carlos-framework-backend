package com.carlos.audit.api.log;

import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.storage.LogStorage;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 跨服务的操作日志存储器：把 {@code @Log} 产生的日志经 MQ 投递给 carlos-audit
 *
 * <h2>它解决什么问题</h2>
 * <p>
 * 「{@code @Log} 注解 → ClickHouse」这条链路原本只在 carlos-audit <b>自己的 JVM 内</b>成立：
 * 唯一写入 ClickHouse 的 {@code LogStorage} 实现（{@code AuditLogStorage}）是
 * carlos-audit-bus 里的 {@code @Component}。其他服务依赖 {@code starter-log} 后拿到的是兜底
 * {@code LoggingLogStorage}——只打一行 SLF4J，<b>审计库里什么都没有</b>，且不报错。
 * </p>
 * <p>
 * 本类补上这一段：转换后投递到 {@link AuditMqTopics#AUDIT_LOG}，由 carlos-audit 既有的
 * {@code AuditLogMqConsumer} 消费并沿原有链路写入（消费端与 Feign 入口复用同一段 service 代码）。
 * 主题常量与参数类型都定义在本模块，正是为了让生产端与消费端共享同一份契约。
 * </p>
 *
 * <h2>三条硬性约束（改动本类时不得破坏）</h2>
 * <ol>
 *   <li><b>绝不抛异常</b>：本类运行在业务服务的日志异步线程上，任何抛出都只会变成噪声或丢日志。
 *       失败一律降级为日志 —— 审计丢失不影响业务，这是明确的取舍。</li>
 *   <li><b>{@link #supportBatch()} 必须为 false</b>：消费端 {@code @MqListener} 的入参是
 *       <b>单条</b> {@link ApiAuditLogMainParam}，批量投递会在反序列化时失败。返回 false 后
 *       {@code LogDisruptorHandler} 走单条 {@link #store}，每条一次 Redis {@code XADD}（亚毫秒）。</li>
 *   <li><b>不在本层做脱敏</b>：请求参数的敏感字段剔除由 {@code @Log#excludeParams()} 在
 *       {@code LogAspect} 序列化时完成 —— 那才是唯一有效的时机（事后脱敏做不到「不落库」）。</li>
 * </ol>
 *
 * <h2>日志级别说明</h2>
 * <p>
 * 投递失败用 {@code warn}、异常用 {@code error}，与 carlos-auth 的
 * {@code AuditLogPublisher} 保持一致。注意框架的 logback 把 WARN 单独过滤，
 * 默认只落 {@code debug.log}（见 {@code warn-logs-invisible-in-info-error}）——
 * 排障时若怀疑日志没投出去，要看 debug.log。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AuditMqLogStorage implements LogStorage {

    /** 优先级高于 LoggingLogStorage（Integer.MAX_VALUE），确保被选中而非兜底 */
    private static final int PRIORITY = 40;

    private final MqTemplate mqTemplate;

    private final OperationLogParamConverter converter = new OperationLogParamConverter();

    @Override
    public boolean store(OperationLog operationLog) {
        if (operationLog == null) {
            return false;
        }

        ApiAuditLogMainParam param;
        try {
            param = converter.convert(operationLog);
        } catch (Exception e) {
            log.error("操作日志转换失败，已丢弃：logType={}", operationLog.getLogType(), e);
            return false;
        }

        if (param == null) {
            return false;
        }

        return send(param);
    }

    /**
     * 逐条投递
     * <p>
     * {@link #supportBatch()} 已声明为 false，正常不会被调用；仍然实现为逐条，
     * 是为了调用方忽略该声明时不至于静默丢日志。
     * </p>
     */
    @Override
    public boolean storeBatch(List<OperationLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return true;
        }

        boolean allSuccess = true;
        for (OperationLog operationLog : logs) {
            // 不用 & 短路之外的花样：要对每一条都尝试，不能因为前一条失败就跳过后续
            allSuccess = store(operationLog) && allSuccess;
        }
        return allSuccess;
    }

    /**
     * 投递单条审计日志
     * <p>
     * {@code MqTemplate} 的发送族<b>不抛异常</b>，失败经 {@link SendResult} 表达；
     * 但取不到 MQ、编码失败等仍可能抛出，故整体兜底。
     * </p>
     */
    private boolean send(ApiAuditLogMainParam param) {
        try {
            SendResult result = mqTemplate.send(AuditMqTopics.AUDIT_LOG, param,
                MqSendOptions.ofKey(param.getTraceId()));

            if (result == null || !result.isSuccess()) {
                log.warn("审计日志投递未成功（可容忍）：logType={}, principal={}, status={}",
                    param.getLogType(), param.getPrincipalId(),
                    result == null ? null : result.getStatus());
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("审计日志 MQ 投递异常，已降级为丢弃：logType={}", param.getLogType(), e);
            return false;
        }
    }

    @Override
    public boolean supportBatch() {
        // 消费端按单条负载消费，批量投递无法被正确反序列化
        return false;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getName() {
        return "AuditMqLogStorage(mq:" + AuditMqTopics.AUDIT_LOG + " → carlos-audit)";
    }
}
