package com.carlos.log.annotation;

import com.carlos.log.enums.BusinessType;
import com.carlos.log.enums.LogLevel;
import com.carlos.log.enums.OperatorType;

import java.lang.annotation.*;

/**
 * 操作日志记录注解
 * <p>
 * 标注在方法上，自动记录操作日志。支持 SpEL 表达式动态解析参数值。
 * <p>
 * 使用示例：
 * <pre>
 * // 基本使用
 * {@literal @}Log(title = "用户管理", businessType = BusinessType.INSERT)
 * public void createUser(UserCreateParam param) { ... }
 *
 * // 使用 SpEL 表达式
 * {@literal @}Log(
 *     title = "订单管理",
 *     businessType = BusinessType.UPDATE,
 *     operation = "'支付订单: ' + #param.orderNo",
 *     targetId = "#param.orderNo",
 *     targetType = "ORDER",
 *     riskLevel = 50
 * )
 * public void payOrder(OrderPayParam param) { ... }
 *
 * // 条件记录
 * {@literal @}Log(
 *     title = "数据导出",
 *     businessType = BusinessType.EXPORT,
 *     condition = "#param.exportType == 'SENSITIVE'",
 *     riskLevel = 70
 * )
 * public void exportData(ExportParam param) { ... }
 * </pre>
 *
 * @author carlos
 * @since 3.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /**
     * 模块名称（如：用户管理、订单管理）
     */
    String title() default "";

    /**
     * 业务类型（默认其他）
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 日志类型（自定义标识，如：USER_CREATE、ORDER_PAY）
     * 默认使用 title_businessType 格式
     */
    String logType() default "";

    /**
     * 操作描述（支持 SpEL 表达式）
     * <p>
     * 示例：
     * <ul>
     *   <li>"'创建用户: ' + #param.username" - 拼接字符串</li>
     *   <li>"#param.description" - 直接使用参数属性</li>
     *   <li>"T(com.example.Util).format(#param)" - 调用静态方法</li>
     * </ul>
     */
    String operation() default "";

    /**
     * 日志级别
     */
    LogLevel logLevel() default LogLevel.INFO;

    /**
     * 操作人类别
     */
    OperatorType operatorType() default OperatorType.OTHER;

    /**
     * 是否保存请求参数
     */
    boolean saveRequestParams() default true;

    /**
     * 是否保存响应数据
     */
    boolean saveResponseData() default false;

    /**
     * 是否异步记录
     * <p>
     * 异步记录使用 Disruptor 队列，性能更好，但可能丢失少量日志
     * 同步记录保证不丢失，但会阻塞业务线程
     */
    boolean async() default true;

    /**
     * 风险等级（0-100）
     * <p>
     * 0-无风险, 30-低风险, 50-中风险, 70-高风险, 100-极高风险
     * <p>
     * 高风险操作会额外记录到安全日志，并触发告警
     */
    int riskLevel() default 0;

    /**
     * 目标对象ID（支持 SpEL 表达式）
     * <p>
     * 用于标识被操作的对象，如订单号、用户ID等
     * 示例："#param.orderNo", "#id"
     */
    String targetId() default "";

    /**
     * 目标对象类型
     * <p>
     * 用于分类标识操作对象，如：ORDER、USER、CONFIG
     */
    String targetType() default "";

    /**
     * 目标对象名称（支持 SpEL 表达式）
     * <p>
     * 示例："#param.username", "#result.name"
     */
    String targetName() default "";

    /**
     * 业务渠道（支持 SpEL 表达式）
     * <p>
     * 标识操作来源渠道，如：WEB、APP、MINI_PROGRAM、OPEN_API
     */
    String bizChannel() default "";

    /**
     * 业务场景（支持 SpEL 表达式）
     * <p>
     * 用于细分业务场景，如：ORDER_CREATE、PAYMENT_CONFIRM
     */
    String bizScene() default "";

    /**
     * 记录条件（支持 SpEL 表达式）
     * <p>
     * 当表达式返回 true 时才记录日志，用于条件记录
     * <p>
     * 示例："#param.needLog == true", "#result != null"
     */
    String condition() default "";

    /**
     * 排除参数字段（逗号分隔）
     * <p>
     * 用于排除敏感字段，如密码、token 等。剔除发生在<b>序列化之后、落库之前</b>，
     * 因此被排除的字段不会出现在审计库中。
     * <p>
     * 匹配规则为<b>忽略大小写的「包含」匹配</b>（与
     * {@code com.carlos.audit.util.SensitiveDataUtil} 一致）：排除词 {@code password}
     * 会同时命中 {@code password} 与 {@code newPassword}。嵌套对象与数组会递归处理。
     * <p>
     * <b>关于默认值的取法</b>：默认列表必须覆盖本代码库真实出现过的字段名，而不是想当然的写法。
     * 这些模块里密码字段实际叫 {@code pwd} / {@code newPwd} / {@code oldPwd} / {@code confirmPwd}
     * —— 它们的<b>字符串里根本没有 "password"</b>，只列 {@code password} 会全部漏掉。
     * 因此默认列表同时包含 {@code pwd} 与 {@code passwd}（经包含匹配即可覆盖上述四种写法）。
     * <p>
     * <b>本列表的边界（必须知道）</b>：只按<b>键名</b>匹配，看不进字符串值内部。
     * 若某个字段的值本身是一段 JSON（如消息渠道的 {@code channelConfig}，内含 {@code appSecret}），
     * 这里的排除词<b>不起作用</b> —— 那种端点必须显式 {@code saveRequestParams = false}。
     * <p>
     * 示例："password,token,secretKey"
     */
    String excludeParams() default "password,pwd,passwd,token,secret,credential,"
        + "apikey,api_key,accesskey,privatekey,private_key";

    /**
     * 最大参数长度限制
     * <p>
     * 超过此长度的参数会被截断，防止日志过大
     */
    int maxParamLength() default 4000;

    /**
     * 最大响应长度限制
     */
    int maxResponseLength() default 4000;

    /**
     * 是否记录方法执行耗时
     */
    boolean recordDuration() default true;

    /**
     * 同步等待超时时间（毫秒）
     * <p>
     * 仅当 async = false 时有效
     */
    long syncTimeout() default 5000;
}
