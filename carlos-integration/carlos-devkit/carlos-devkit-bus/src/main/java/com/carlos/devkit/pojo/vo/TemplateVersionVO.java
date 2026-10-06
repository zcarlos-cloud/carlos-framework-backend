package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板版本历史 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "模板版本视图对象")
public class TemplateVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "模板ID")
    private Long templateId;

    @Schema(description = "版本号")
    private Integer versionNo;

    @Schema(description = "文件路径快照")
    private String filePath;

    @Schema(description = "正文快照（仅详情接口下发）")
    private String content;

    @Schema(description = "正文大小（字节）")
    private Integer fileSize;

    @Schema(description = "MD5")
    private String md5;

    @Schema(description = "变更说明")
    private String changeLog;

    @Schema(description = "保存人")
    private Long createBy;

    @Schema(description = "保存时间")
    private LocalDateTime createTime;
}
