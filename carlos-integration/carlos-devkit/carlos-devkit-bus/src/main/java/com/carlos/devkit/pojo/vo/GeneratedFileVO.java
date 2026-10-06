package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 生成产物的单个文件（预览用；二进制文件只给大小不给内容）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "生成产物文件")
public class GeneratedFileVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "相对产物根目录的路径")
    private String path;

    @Schema(description = "文件内容（仅文本文件下发）")
    private String content;

    @Schema(description = "是否文本文件")
    private Boolean text;

    @Schema(description = "文件大小（字节）")
    private Integer size;
}
