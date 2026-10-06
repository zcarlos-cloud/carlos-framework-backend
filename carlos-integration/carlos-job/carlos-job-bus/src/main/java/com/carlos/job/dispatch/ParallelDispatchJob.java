package com.carlos.job.dispatch;

/**
 * <p>
 * 并行派发任务：允许同一任务并发执行多个实例
 * </p>
 * <p>
 * 不施加 {@code @DisallowConcurrentExecution}，适用于幂等、可重入的任务。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public class ParallelDispatchJob extends AbstractDispatchJob {

}
