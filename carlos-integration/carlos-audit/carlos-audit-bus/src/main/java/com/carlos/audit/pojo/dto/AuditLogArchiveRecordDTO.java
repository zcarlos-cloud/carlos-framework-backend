package com.carlos.audit.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
public class AuditLogArchiveRecordDTO {
    /** 主键 */
    private Long id;
    /** 归档批次ID，UUID */
    private String archiveId;
    /** 归档日期 */
    private LocalDate archiveDate;
    /** 归档数据起始时间 */
    private LocalDateTime startTime;
    /** 归档数据结束时间 */
    private LocalDateTime endTime;
    /** 归档记录数 */
    private Long recordCount;
    /** 归档文件大小，字节 */
    private Long fileSizeBytes;
    /** 存储路径 */
    private String storagePath;
    /** 存储类型：OSS/S3/LOCAL */
    private String storageType;
    /** 校验和，MD5或SHA256 */
    private String verifyChecksum;
    /** 状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中 */
    private String archiveStatus;
    /** 创建时间 */
    private LocalDateTime createdTime;
}
