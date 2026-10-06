/**
 * Redis Stream 原语层。
 *
 * <h2>分层定位</h2>
 * <p>
 * 本包提供 Redis Stream 的<b>物理操作</b>：读写条目、消费组、待处理队列（PEL）与认领、
 * 修剪、指标与健康检查。<b>不含任何 MQ 语义</b>——参数只有 {@code key}/{@code group}/
 * {@code recordId}，不认识 {@code topic}、重试、死信、能力声明。
 * 命名规约（topic → key）、重试与死信策略由 {@code com.carlos.mq} 层负责。
 * </p>
 * <p>
 * 判定红线：<b>把 Redis 换掉还有意义的概念属于上层</b>。
 * </p>
 *
 * <h2>两条必须遵守的约定</h2>
 * <ol>
 *   <li><b>写入默认带长度上限</b>（{@code XADD MAXLEN ~ N}）。这是与缓存/token 共用
 *       Redis 实例时把无界积压变成固定预算的核心手段，不是可选优化。</li>
 *   <li><b>字段值为显式 JSON 字符串</b>，key/字段名用字符串序列化器。
 *       不要把 {@code ConfigurableRedisSerializer}（默认 Jackson）挂到 Stream 上——
 *       它会把 Long 等标量写成带引号字符串，导致字段读出为转义字符串。</li>
 * </ol>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.redis.stream;
