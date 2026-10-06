package com.carlos.system.upload.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * 文件统计结果VO
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Schema(description = "文件统计结果")
public class FileStatisticsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "总文件数")
    private Long totalCount;

    @Schema(description = "总存储大小（字节）")
    private Long totalSize;

    @Schema(description = "可读总存储大小")
    private String readableTotalSize;

    @Schema(description = "今日上传数")
    private Long todayUploadCount;

    @Schema(description = "按类型分布")
    private Map<String, Long> typeDistribution;
}
