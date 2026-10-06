package com.carlos.workflow.api.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 「我发起的」流程实例分页参数。
 * </p>
 * <p>
 * 过滤口径：{@code flowName} 模糊匹配（流程定义侧的名称，{@code flow_instance} 上不存名称），
 * {@code flowStatus} 与 {@code businessId} 等值匹配；三者为空时不过滤。
 * </p>
 * <p>
 * <b>「当前用户」不由前端传入</b>：发起人取自登录上下文（{@code flow_instance.create_by}），
 * 由服务端过滤，避免越权查看他人的申请。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我发起的流程实例分页参数")
public class ApiWorkflowInstancePageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /**
     * 流程名称（模糊匹配，取自流程定义）
     */
    @Schema(description = "流程名称，模糊匹配")
    @Size(max = 100, message = "流程名称长度不能超过100个字符")
    private String flowName;

    /**
     * 流程状态（等值匹配）
     */
    @Schema(description = "流程状态，等值匹配")
    @Size(max = 20, message = "流程状态长度不能超过20个字符")
    private String flowStatus;

    /**
     * 业务id（等值匹配）
     */
    @Schema(description = "业务id，等值匹配")
    @Size(max = 40, message = "业务id长度不能超过40个字符")
    private String businessId;

}
