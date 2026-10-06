package com.carlos.workflow.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.workflow.api.ApiWorkflowDefinition;
import com.carlos.workflow.api.pojo.ao.WorkflowDefinitionAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * <p>
 * 工作流流程定义 Feign 熔断降级工厂。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Slf4j
@Component
public class ApiWorkflowDefinitionFallbackFactory implements FallbackFactory<ApiWorkflowDefinition> {

    @Override
    public ApiWorkflowDefinition create(Throwable cause) {
        String message = cause.getMessage();
        log.error("调用工作流服务失败: message:{}", message);
        return new ApiWorkflowDefinition() {

            @Override
            public Result<List<WorkflowDefinitionAO>> listPublished() {
                log.error("调用工作流服务失败(listPublished): message:{}", message);
                return Result.success(Collections.emptyList());
            }
        };
    }

}
