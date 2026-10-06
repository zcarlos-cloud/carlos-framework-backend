package com.carlos.audit.api.pojo.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 审计日志归档记录 API 请求参数
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
@Accessors(chain = true)
public class ApiAuditLogArchiveParam implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 归档批次ID */
    @NotBlank(message = "归档批次ID不能为空")
    private String archiveId;

    /** 归档日期 */
    @NotNull(message = "归档日期不能为空")
    private LocalDate archiveDate;

    /** 归档数据起始时间 */
    @NotNull(message = "归档起始时间不能为空")
    private LocalDateTime startTime;

    /** 归档数据结束时间 */
    @NotNull(message = "归档结束时间不能为空")
    private LocalDateTime endTime;

    /** 归档记录数 */
    @Positive(message = "归档记录数必须大于0")
    private Long recordCount;

    /** 归档文件大小 */
    @Positive(message = "归档文件大小必须大于0")
    private Long fileSizeBytes;

    /** 存储路径 */
    @NotBlank(message = "存储路径不能为空")
    private String storagePath;

    /** 存储类型 */
    @NotBlank(message = "存储类型不能为空")
    private String storageType;

    /** 校验和 */
    @NotBlank(message = "校验和不能为空")
    private String verifyChecksum;

    /** 归档状态 */
    @NotBlank(message = "归档状态不能为空")
    private String archiveStatus;
}
