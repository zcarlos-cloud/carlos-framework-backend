package com.carlos.audit.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogArchiveRecordVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "归档批次ID，UUID")
    private String archiveId;
    @Schema(description = "归档日期")
    private LocalDate archiveDate;
    @Schema(description = "归档数据起始时间")
    private LocalDateTime startTime;
    @Schema(description = "归档数据结束时间")
    private LocalDateTime endTime;
    @Schema(description = "归档记录数")
    private Long recordCount;
    @Schema(description = "归档文件大小，字节")
    private Long fileSizeBytes;
    @Schema(description = "存储路径")
    private String storagePath;
    @Schema(description = "存储类型：OSS/S3/LOCAL")
    private String storageType;
    @Schema(description = "校验和，MD5或SHA256")
    private String verifyChecksum;
    @Schema(description = "状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中")
    private String archiveStatus;
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

}
