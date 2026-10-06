package com.carlos.devkit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 生成产物预览结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "生成产物预览结果")
public class GeneratedResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "产物根目录名")
    private String rootName;

    @Schema(description = "产物文件数")
    private Integer fileCount;

    @Schema(description = "产物总字节数")
    private Long totalSize;

    @Schema(description = "生成耗时（毫秒）")
    private Long durationMs;

    @Schema(description = "产物文件清单")
    private List<GeneratedFileVO> files;
}
