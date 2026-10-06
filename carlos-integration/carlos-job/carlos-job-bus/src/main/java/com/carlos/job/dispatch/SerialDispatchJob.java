package com.carlos.job.dispatch;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.PersistJobDataAfterExecution;

/**
 * <p>
 * 串行派发任务：同一任务在 Quartz 集群内互斥执行（默认并发模式）
 * </p>
 * <p>
 * {@code @DisallowConcurrentExecution} 在 JDBC Store 集群下保证同一 JobDetail
 * 全集群同时只有一个实例在跑；{@code @PersistJobDataAfterExecution} 持久化 JobDataMap 变更。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class SerialDispatchJob extends AbstractDispatchJob {

}
