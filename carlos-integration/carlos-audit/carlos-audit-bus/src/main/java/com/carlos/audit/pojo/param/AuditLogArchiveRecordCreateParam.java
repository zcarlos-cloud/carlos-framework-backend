package com.carlos.audit.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
@Schema(description = "审计日志归档记录（管理冷数据归档）新增参数")
public class AuditLogArchiveRecordCreateParam {
    @NotBlank(message = "归档批次ID，UUID不能为空")
    @Schema(description = "归档批次ID，UUID")
    private String archiveId;
    @NotNull(message = "归档日期不能为空")
    @Schema(description = "归档日期")
    private LocalDate archiveDate;
    @NotNull(message = "归档数据起始时间不能为空")
    @Schema(description = "归档数据起始时间")
    private LocalDateTime startTime;
    @NotNull(message = "归档数据结束时间不能为空")
    @Schema(description = "归档数据结束时间")
    private LocalDateTime endTime;
    @NotNull(message = "归档记录数不能为空")
    @Schema(description = "归档记录数")
    private Long recordCount;
    @NotNull(message = "归档文件大小，字节不能为空")
    @Schema(description = "归档文件大小，字节")
    private Long fileSizeBytes;
    @NotBlank(message = "存储路径不能为空")
    @Schema(description = "存储路径")
    private String storagePath;
    @NotBlank(message = "存储类型：OSS/S3/LOCAL不能为空")
    @Schema(description = "存储类型：OSS/S3/LOCAL")
    private String storageType;
    @Schema(description = "校验和，MD5或SHA256")
    private String verifyChecksum;
    @NotBlank(message = "状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中不能为空")
    @Schema(description = "状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中")
    private String archiveStatus;
    @NotNull(message = "创建时间不能为空")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;
}
