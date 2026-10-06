package com.carlos.workflow.api.pojo.param;

import com.carlos.core.param.ParamPage;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 流程定义分页查询参数，管理端列表页使用。
 * </p>
 * <p>
 * 过滤口径：{@code flowName} 模糊匹配，{@code flowCode} / {@code category} / {@code isPublish} 等值匹配；
 * 三者为空时不过滤。结果固定按创建时间倒序，不提供前端排序入口。
 * </p>
 *
 * @author Carlos Framework Team
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiWorkflowDefinitionPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    /**
     * 流程名称（模糊匹配）
     */
    @Size(max = 100, message = "流程名称长度不能超过100个字符")
    private String flowName;

    /**
     * 流程编码（等值匹配）
     */
    @Size(max = 40, message = "流程编码长度不能超过40个字符")
    private String flowCode;

    /**
     * 流程类别（等值匹配）
     */
    @Size(max = 100, message = "流程类别长度不能超过100个字符")
    private String category;

    /**
     * 是否发布（0未发布 1已发布 9已失效）
     */
    private Integer isPublish;

}
