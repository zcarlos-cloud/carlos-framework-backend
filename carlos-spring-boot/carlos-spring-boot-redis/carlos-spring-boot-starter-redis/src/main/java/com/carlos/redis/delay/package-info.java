/**
 * 延时队列原语层。
 *
 * <h2>分层定位</h2>
 * <p>
 * 基于 Redisson {@code RDelayedQueue} 提供「任意精度延时入队 + 到期回调」的物理能力。
 * <b>本层不知道到期条目要送去哪里</b>——它只把「桶名 + 载荷」交给上层注入的回调，
 * 「延时 → Stream」的编排属于 {@code com.carlos.mq} 层。
 * </p>
 *
 * <h2>与业务调度的边界（重要）</h2>
 * <p>
 * {@code max-delay} 默认 <b>1 小时</b>，是刻意的架构边界：延时消息只服务于
 * <b>消息自身</b>的延迟（失败重试、事件去抖、聚合窗口）。任何「到点触发业务动作」
 * （日终对账、定时清理、报表生成）都应属于 {@code carlos-job}（Quartz）——
 * 那类需求需要调度中心视图、错过补偿与人工干预。
 * </p>
 *
 * <h2>关闭语义</h2>
 * <p>
 * {@code stop()} <b>不会</b>销毁 Redisson 的内部延时结构，也<b>不做 drain</b>：
 * 未到期项留在 Redis 中由下次启动继续转移。这是为了避免
 * {@code RDelayedQueue.destroy()} 删除内部 ZSET 导致未到期消息全部丢失。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
package com.carlos.redis.delay;
