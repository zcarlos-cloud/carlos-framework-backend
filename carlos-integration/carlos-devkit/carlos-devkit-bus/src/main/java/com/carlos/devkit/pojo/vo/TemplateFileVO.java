package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板文件 显示层对象（含正文，用于在线编辑）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模板文件视图对象")
public class TemplateFileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "所属模板组ID")
    private Long groupId;

    @Schema(description = "模板组名称")
    private String groupName;

    @Schema(description = "组内相对路径")
    private String filePath;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "扩展名")
    private String fileExt;

    @Schema(description = "文件类型：1=模板 2=静态资源")
    private Integer fileType;

    @Schema(description = "是否循环渲染")
    private Boolean loopFlag;

    @Schema(description = "文件正文")
    private String content;

    @Schema(description = "正文编码：TEXT / BASE64")
    private String contentEncoding;

    @Schema(description = "正文大小（字节）")
    private Integer fileSize;

    @Schema(description = "正文 MD5")
    private String md5;

    @Schema(description = "状态：1=启用 0=停用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
