package com.carlos.job.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.job.api.ApiJob;
import com.carlos.job.pojo.ao.JobInfoAO;
import com.carlos.job.pojo.param.ApiJobCreateParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 定时任务管理 Feign 熔断降级工厂
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Slf4j
@Component
public class ApiJobFallbackFactory implements FallbackFactory<ApiJob> {

    @Override
    public ApiJob create(Throwable cause) {
        log.error("调用调度中心服务失败: {}", cause.getMessage(), cause);
        return new ApiJob() {
            @Override
            public Result<Boolean> create(ApiJobCreateParam param) {
                return Result.error("调度中心服务不可用");
            }

            @Override
            public Result<Boolean> runOnce(String jobName, String jobGroup) {
                return Result.error("调度中心服务不可用");
            }

            @Override
            public Result<Boolean> pause(String jobName, String jobGroup) {
                return Result.error("调度中心服务不可用");
            }

            @Override
            public Result<Boolean> resume(String jobName, String jobGroup) {
                return Result.error("调度中心服务不可用");
            }

            @Override
            public Result<Boolean> delete(String jobName, String jobGroup) {
                return Result.error("调度中心服务不可用");
            }

            @Override
            public Result<JobInfoAO> info(String jobName, String jobGroup) {
                return Result.error("调度中心服务不可用");
            }
        };
    }

}
