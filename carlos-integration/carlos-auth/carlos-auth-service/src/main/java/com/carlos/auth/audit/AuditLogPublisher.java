package com.carlos.auth.audit;

import com.carlos.audit.api.ApiAuditLogMain;
import com.carlos.audit.api.constant.AuditMqTopics;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.mq.core.MqSendOptions;
import com.carlos.mq.core.MqTemplate;
import com.carlos.mq.core.SendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

/**
 * <p>
 * 审计日志的<b>统一投递点</b>
 * </p>
 *
 * <h2>为什么要有这一个类</h2>
 * <p>
 * carlos-auth 原有 8 处各自直接调用 {@code apiAuditLogMain.saveAuditLog(param)}。
 * 把它们统一收敛到一个出口，换来三件事：<b>单一切换点</b>（{@code carlos.auth.audit.mode}
 * 一处决定走 MQ 还是 Feign）、<b>单一回退点</b>（出问题只改这一处）、
 * <b>单一安全边界</b>（下面的「绝不抛出」只需在这一个地方保证）。
 * </p>
 *
 * <h2>三条硬性约束（改动本类时不得破坏）</h2>
 * <ol>
 *   <li><b>绝不抛异常</b>：8 处调用点全部位于<b>登录/登出热路径</b>上，
 *       审计侧的任何故障都不得影响认证结果。本类内部整体捕获，最坏情况只落日志。</li>
 *   <li><b>参数必须在请求线程内构建完成</b>再传入。审计参数里的 IP/UA/追踪号依赖
 *       {@code HttpServletRequest} 或 {@code RequestContextHolder}，二者在异步线程中
 *       不可用（{@code RequestContextHolder} 会返回 null，scoped 代理会抛异常）。
 *       调用方若有 {@code @Async}，必须在进入异步之前把参数构造完。</li>
 *   <li><b>用同步 send 而非 sendAsync</b>：审计的价值在「确定投递」，
 *       而异步失败回调在热路径上更难处理；且本平台的同步 send 只是一次 Redis 写入
 *       （亚毫秒），相比改造前的跨进程 HTTP 往返已经快一个数量级。</li>
 * </ol>
 *
 * <h2>关于主题</h2>
 * <p>
 * 主题<b>刻意不可配置</b>，直接引用 {@link AuditMqTopics#AUDIT_LOG}。
 * 若做成配置项，生产端与消费端就可能各配一个值而互不相识——消息发出去没人消费、
 * 双方都不报错，属于最难排查的一类故障。共用常量是零成本的正确做法。
 * </p>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AuditLogPublisher {

    private final AuditProperties auditProperties;

    /**
     * 用 ObjectProvider 而非直接注入：{@code MqTemplate} 的<b>行为</b>由运行期配置决定
     * （{@code carlos.mq.type=none} 时装的是不投递的兜底实现，被排除时则根本没有该 bean）。
     * 审计不能因为 MQ 的部署形态而阻断登录。
     */
    private final ObjectProvider<MqTemplate> mqTemplateProvider;

    /** Feign 回退路径（{@link AuditMode#FEIGN}）使用 */
    private final ApiAuditLogMain apiAuditLogMain;

    /**
     * 投递审计日志
     * <p><b>永不抛出</b>：任何失败都只落到日志。调用方无需 try/catch。</p>
     *
     * @param param 已构建完成的审计参数（可为 null，将被忽略）
     */
    public void publish(ApiAuditLogMainParam param) {
        if (param == null) {
            return;
        }
        if (auditProperties.getMode() == AuditMode.FEIGN) {
            publishByFeign(param);
            return;
        }
        publishByMq(param);
    }

    /**
     * 经框架级 MQ 投递
     * <p>
     * 消息头仅用于观测，不作路由依据；业务键取 {@code traceId}（可为 null，
     * 取到时可据它在 Stream 中定位这次请求相关的全部审计）。
     * </p>
     */
    private void publishByMq(ApiAuditLogMainParam param) {
        try {
            MqTemplate mqTemplate = mqTemplateProvider.getIfAvailable();
            if (mqTemplate == null) {
                log.warn("MQ 模板不可用，审计日志未投递（可容忍）：principal={}, logType={}",
                    param.getPrincipalName(), param.getLogType());
                return;
            }
            SendResult result = mqTemplate.send(AuditMqTopics.AUDIT_LOG, param,
                MqSendOptions.ofKey(param.getTraceId()));
            if (!result.isSuccess()) {
                // 不因投递失败影响调用方，但必须留痕（审计丢失可容忍、静默不可接受）
                log.warn("审计日志投递未成功（可容忍）：principal={}, logType={}, status={}, retryable={}",
                    param.getPrincipalName(), param.getLogType(), result.getStatus(),
                    result.isRetryable());
            }
        } catch (Exception e) {
            log.error("审计日志 MQ 投递异常，已降级为仅日志：principal={}",
                param.getPrincipalName(), e);
        }
    }

    /**
     * 经 Feign 同步投递（回退路径，行为与改造前一致）
     */
    private void publishByFeign(ApiAuditLogMainParam param) {
        try {
            apiAuditLogMain.saveAuditLog(param);
        } catch (Exception e) {
            log.error("审计日志 Feign 投递异常，已降级为仅日志：principal={}",
                param.getPrincipalName(), e);
        }
    }
}
