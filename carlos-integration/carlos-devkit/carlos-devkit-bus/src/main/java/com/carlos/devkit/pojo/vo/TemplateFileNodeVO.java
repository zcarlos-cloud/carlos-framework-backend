package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 模板文件树节点 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模板文件树节点")
public class TemplateFileNodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "节点 key（目录=路径，文件=文件ID）")
    private String key;

    @Schema(description = "节点标题（目录名 / 文件名）")
    private String title;

    @Schema(description = "节点类型：dir=目录 file=文件")
    private String nodeType;

    @Schema(description = "文件ID（仅文件节点）")
    private Long fileId;

    @Schema(description = "组内相对路径")
    private String filePath;

    @Schema(description = "文件类型：1=模板 2=静态资源（仅文件节点）")
    private Integer fileType;

    @Schema(description = "是否循环渲染（仅文件节点）")
    private Boolean loopFlag;

    @Schema(description = "文件大小（仅文件节点）")
    private Integer fileSize;

    @Schema(description = "状态：1=启用 0=停用（仅文件节点）")
    private Integer status;

    @Schema(description = "子节点")
    private List<TemplateFileNodeVO> children;
}
