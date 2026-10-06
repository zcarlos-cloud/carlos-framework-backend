package com.carlos.workflow.api.pojo.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 发起流程参数。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
public class ApiWorkflowStartParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 流程编码，必须是已发布的流程定义
     */
    @NotBlank(message = "流程编码不能为空")
    @Size(max = 40, message = "流程编码长度不能超过40个字符")
    private String flowCode;

    /**
     * 业务id，与业务单据一一对应
     */
    @NotBlank(message = "业务id不能为空")
    @Size(max = 40, message = "业务id长度不能超过40个字符")
    private String businessId;

    /**
     * 发起人唯一标识，缺省时由引擎的 PermissionHandler 提供
     */
    @Size(max = 80, message = "发起人标识长度不能超过80个字符")
    private String handler;

    /**
     * 发起说明
     */
    @Size(max = 500, message = "发起说明长度不能超过500个字符")
    private String message;

    /**
     * 流程变量
     */
    private Map<String, Object> variable;

    /**
     * 首个任务的处理人，缺省时按流程设计中的办理人表达式计算
     */
    private List<String> nextHandlers;

}
