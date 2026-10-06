package com.carlos.system.upload.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 文件列表分页查询参数
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件列表分页查询参数")
public class UploadRecordPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    @Size(max = 200, message = "文件名称长度不能超过200")
    @Schema(description = "文件名称模糊查询")
    private String originalName;

    @Size(max = 100, message = "文件用途长度不能超过100")
    @Schema(description = "文件用途")
    private String uses;

    @Schema(description = "上传人ID")
    private Long createBy;

    @Size(max = 20, message = "文件扩展名长度不能超过20")
    @Schema(description = "文件扩展名")
    private String fileExtension;

    @Size(max = 20, message = "存储类型长度不能超过20")
    @Schema(description = "存储类型")
    private String storageType;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;
}
