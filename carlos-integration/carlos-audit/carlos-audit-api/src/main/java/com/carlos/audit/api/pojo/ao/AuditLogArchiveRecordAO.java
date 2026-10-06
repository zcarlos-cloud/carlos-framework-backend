package com.carlos.audit.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计日志归档记录 API 响应对象
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
public class AuditLogArchiveRecordAO implements Serializable {

    /** 主键 */
    private Long id;

    /** 归档批次ID */
    private String archiveId;

    /** 归档日期 */
    private LocalDate archiveDate;

    /** 归档数据起始时间 */
    private LocalDateTime startTime;

    /** 归档数据结束时间 */
    private LocalDateTime endTime;

    /** 归档记录数 */
    private Long recordCount;

    /** 归档文件大小 */
    private Long fileSizeBytes;

    /** 存储路径 */
    private String storagePath;

    /** 存储类型 */
    private String storageType;

    /** 校验和 */
    private String verifyChecksum;

    /** 归档状态 */
    private String archiveStatus;

    /** 创建时间 */
    private LocalDateTime createdTime;
}
