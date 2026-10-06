package com.carlos.workflow.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.workflow.api.ApiWorkflowInstance;
import com.carlos.workflow.api.pojo.ao.WorkflowChartAO;
import com.carlos.workflow.api.pojo.ao.WorkflowInstanceAO;
import com.carlos.workflow.api.pojo.param.ApiWorkflowStartParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * 工作流流程实例 Feign 熔断降级工厂。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
public class ApiWorkflowInstanceFallbackFactory implements FallbackFactory<ApiWorkflowInstance> {

    @Override
    public ApiWorkflowInstance create(Throwable cause) {
        String message = cause.getMessage();
        log.error("调用工作流服务失败: message:{}", message);
        return new ApiWorkflowInstance() {

            @Override
            public Result<WorkflowInstanceAO> start(ApiWorkflowStartParam param) {
                log.error("调用工作流服务失败(start): flowCode:{}, message:{}",
                    param == null ? null : param.getFlowCode(), message);
                return Result.error("工作流服务不可用");
            }

            @Override
            public Result<WorkflowChartAO> chart(Long instanceId) {
                log.error("调用工作流服务失败(chart): instanceId:{}, message:{}", instanceId, message);
                return Result.error("工作流服务不可用");
            }
        };
    }

}
