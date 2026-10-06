package com.carlos.audit.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.carlos.audit.pojo.enums.AuditLogArchiveStateEnum;
import com.carlos.audit.pojo.enums.AuditLogStorageTypeEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志归档记录（管理冷数据归档） 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("audit_log_archive_record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogArchiveRecord extends Model<AuditLogArchiveRecord> implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;
    /**
     * 归档批次ID，UUID
     */
    @TableField(value = "archive_id")
    private String archiveId;
    /**
     * 归档日期
     */
    @TableField(value = "archive_date")
    private LocalDate archiveDate;
    /**
     * 归档数据起始时间
     */
    @TableField(value = "start_time")
    private LocalDateTime startTime;
    /**
     * 归档数据结束时间
     */
    @TableField(value = "end_time")
    private LocalDateTime endTime;
    /**
     * 归档记录数
     */
    @TableField(value = "record_count")
    private Long recordCount;
    /**
     * 归档文件大小，字节
     */
    @TableField(value = "file_size_bytes")
    private Long fileSizeBytes;
    /**
     * 存储路径
     */
    @TableField(value = "storage_path")
    private String storagePath;
    /**
     * 存储类型：OSS/S3/LOCAL
     */
    @TableField(value = "storage_type")
    private AuditLogStorageTypeEnum storageType;
    /**
     * 校验和，MD5或SHA256
     */
    @TableField(value = "verify_checksum")
    private String verifyChecksum;
    /**
     * 状态：SUCCESS-成功/FAILED-失败/PROCESSING-处理中
     */
    @TableField(value = "archive_status")
    private AuditLogArchiveStateEnum archiveStatus;
    /**
     * 创建时间
     */
    @TableField(value = "created_time")
    private LocalDateTime createdTime;


}
