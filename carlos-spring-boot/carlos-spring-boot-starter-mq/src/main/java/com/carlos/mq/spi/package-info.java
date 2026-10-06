/**
 * MQ 传输 SPI 包 —— 框架与具体 MQ 实现之间的唯一接缝。
 *
 * <h2>如何新增一种 MQ 传输（调用方零改动）</h2>
 * <ol>
 *   <li>在 {@link com.carlos.mq.support.MqType} 中登记新类型常量。</li>
 *   <li>新建实现类 {@code implements MqTransport}，按顺序实现：
 *       <ul>
 *         <li>{@code getType()} / {@code getName()} / {@code capabilities()}</li>
 *         <li>{@code start()} / {@code stop()} / {@code isRunning()} / {@code ping()}</li>
 *         <li>{@code send(MqMessage)}（基础能力，必须实现）</li>
 *         <li>按需覆写 {@code sendDelayed} / {@code sendAsync} / {@code subscribe}；
 *             <b>不支持的能力不要覆写</b>——默认实现会抛
 *             {@link com.carlos.mq.exception.MqCapabilityNotSupportedException}。</li>
 *       </ul>
 *   </li>
 *   <li>把实现注册为 Spring bean（例如在传输专属的 {@code @AutoConfiguration} 中
 *       {@code @Bean}）；{@link com.carlos.mq.spi.MqTransportRegistry} 会自动收集。</li>
 *   <li>配置 {@code carlos.mq.type=<新类型>}（若容器中只有该实现，{@code auto} 亦可）。</li>
 * </ol>
 *
 * <p>
 * <b>业务代码无需任何改动</b>：它只依赖
 * {@link com.carlos.mq.core.MqTemplate} 与 {@code @MqListener} 注解。
 * </p>
 *
 * <h2>两条不可违反的契约</h2>
 * <ul>
 *   <li><b>能力要么真做，要么不声明</b>：不得声明无法兑现的能力，也不得在被请求
 *       未声明能力时静默降级（这是本项目明确禁止的失败模式）。</li>
 *   <li><b>健康检查必须真实</b>：{@code ping()} 应做一次真实往返，不得仅返回
 *       「bean 非空 &amp;&amp; 已初始化」。</li>
 * </ul>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.mq.spi;
