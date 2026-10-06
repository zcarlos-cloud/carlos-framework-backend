package com.carlos.job.client.handler;

/**
 * <p>
 * 任务处理器，业务模块实现该接口并注册为 Spring Bean 即可被调度中心调度
 * </p>
 * <p>
 * 处理器名称默认取 Spring Bean 名称，如需自定义可重写 {@link #name()}
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
public interface JobHandler {

    /**
     * 处理器名称，返回 null 时使用 Spring Bean 名称
     *
     * @return 处理器名称
     */
    default String name() {
        return null;
    }

    /**
     * 执行任务
     *
     * @param params 任务参数（任务配置中的 JSON 字符串，由实现自行解析）
     * @return 执行结果
     * @throws Exception 执行异常（调度中心会记录为执行失败）
     */
    JobResult execute(String params) throws Exception;

}
