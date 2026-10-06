package com.carlos.job;

/**
 * <p>
 * 任务调度内部端点常量（调度中心与业务客户端共用的唯一来源）
 * </p>
 *
 * <p>{@code /inner/**} 为服务间内部端点，不走网关路由，靠服务发现直连，统一以
 * {@link #TOKEN_HEADER} 做鉴权。</p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
public interface JobEndpointConstant {

    /**
     * 内部调用鉴权请求头
     */
    String TOKEN_HEADER = "X-Job-Token";

    /**
     * 调度中心回调业务服务的执行端点（由业务侧 carlos-job-client 提供）
     */
    String EXECUTE_PATH = "/inner/job/execute";

    /**
     * 业务服务向调度中心注册任务的端点（由 carlos-job-bus 提供）
     */
    String REGISTER_PATH = "/inner/job/register";

}
