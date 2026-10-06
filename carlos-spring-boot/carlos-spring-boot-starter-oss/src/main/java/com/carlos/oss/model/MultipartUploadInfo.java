package com.carlos.oss.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 未完成的分片上传信息
 *
 * @author Carlos
 * @date 2026-06-30
 */
@Data
@Builder
public class MultipartUploadInfo {

    /** 桶名称 */
    private String bucketName;

    /** 对象名称 */
    private String objectName;

    /** 上传 ID（用于 continue 或 abort） */
    private String uploadId;

    /** 上传发起时间 */
    private LocalDateTime initiated;
}
