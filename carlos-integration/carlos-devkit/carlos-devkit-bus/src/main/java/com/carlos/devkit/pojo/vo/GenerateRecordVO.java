package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 生成记录 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "生成记录视图对象")
public class GenerateRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "业务类型：1=代码生成 2=工程生成")
    private Integer bizType;

    @Schema(description = "模板组ID")
    private Long templateGroupId;

    @Schema(description = "模板组编码")
    private String templateGroupCode;

    @Schema(description = "模板组名称")
    private String templateGroupName;

    @Schema(description = "数据源ID")
    private Long dsId;

    @Schema(description = "数据源名称")
    private String dsName;

    @Schema(description = "项目名")
    private String projectName;

    @Schema(description = "包名")
    private String packageName;

    @Schema(description = "生成参数 JSON 快照")
    private String genParams;

    @Schema(description = "产物文件数")
    private Integer fileCount;

    @Schema(description = "产物总字节数")
    private Long totalSize;

    @Schema(description = "生成耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "状态：1=成功 0=失败")
    private Integer status;

    @Schema(description = "失败原因")
    private String errorMsg;

    @Schema(description = "操作人")
    private Long createBy;

    @Schema(description = "生成时间")
    private LocalDateTime createTime;
}
