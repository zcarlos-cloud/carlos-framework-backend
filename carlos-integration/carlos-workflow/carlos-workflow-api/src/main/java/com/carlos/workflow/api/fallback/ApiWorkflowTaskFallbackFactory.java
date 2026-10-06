package com.carlos.workflow.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.workflow.api.ApiWorkflowTask;
import com.carlos.workflow.api.pojo.ao.WorkflowHisTaskAO;
import com.carlos.workflow.api.pojo.ao.WorkflowTaskAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 工作流待办任务 Feign 熔断降级工厂。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
public class ApiWorkflowTaskFallbackFactory implements FallbackFactory<ApiWorkflowTask> {

    @Override
    public ApiWorkflowTask create(Throwable cause) {
        String message = cause.getMessage();
        log.error("调用工作流服务失败: message:{}", message);
        return new ApiWorkflowTask() {

            @Override
            public Result<WorkflowTaskAO> load(Long taskId) {
                log.error("调用工作流服务失败(load): taskId:{}, message:{}", taskId, message);
                return Result.error("工作流服务不可用");
            }

            @Override
            public Result<WorkflowHisTaskAO> loadHistory(Long hisTaskId) {
                log.error("调用工作流服务失败(loadHistory): hisTaskId:{}, message:{}", hisTaskId, message);
                return Result.error("工作流服务不可用");
            }
        };
    }

}
