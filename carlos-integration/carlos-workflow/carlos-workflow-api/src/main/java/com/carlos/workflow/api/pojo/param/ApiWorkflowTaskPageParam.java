package com.carlos.workflow.api.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 待办 / 已办任务分页参数。
 * </p>
 * <p>
 * 两个列表的过滤口径一致（节点名称模糊、流程状态等值），因此共用同一个参数对象；
 * 「当前用户」不由前端传入，而是服务端从登录上下文取，避免越权查看他人的待办。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "待办/已办任务分页参数")
public class ApiWorkflowTaskPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /**
     * 节点名称（模糊匹配）
     */
    @Schema(description = "节点名称，模糊匹配")
    @Size(max = 100, message = "节点名称长度不能超过100个字符")
    private String nodeName;

    /**
     * 流程状态（等值匹配）
     */
    @Schema(description = "流程状态，等值匹配")
    @Size(max = 20, message = "流程状态长度不能超过20个字符")
    private String flowStatus;

}
