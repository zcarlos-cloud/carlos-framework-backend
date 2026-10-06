package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板组 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模板组视图对象")
public class TemplateGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模板组编码")
    private String groupCode;

    @Schema(description = "模板组名称")
    private String groupName;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "模板类型：1=代码生成 2=工程生成")
    private Integer templateType;

    @Schema(description = "技术栈编码")
    private String techStack;

    @Schema(description = "适用数据库")
    private String dbType;

    @Schema(description = "模板根目录名")
    private String templateRoot;

    @Schema(description = "逻辑删除字段清单（逗号分隔）")
    private String logicDeleteFields;

    @Schema(description = "乐观锁版本字段清单（逗号分隔）")
    private String versionFields;

    @Schema(description = "通用字段清单（逗号分隔）")
    private String commonFields;

    @Schema(description = "是否内置模板")
    private Boolean builtIn;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;

    @Schema(description = "排序值")
    private Integer sortNum;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "模板文件数")
    private Integer fileCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
