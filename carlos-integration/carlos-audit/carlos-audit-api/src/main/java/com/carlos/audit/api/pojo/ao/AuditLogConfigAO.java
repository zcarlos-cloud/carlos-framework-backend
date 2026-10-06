package com.carlos.audit.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 审计日志配置 API 响应对象
 *
 * @author Carlos
 * @date 2026-06-12
 */
@Data
public class AuditLogConfigAO implements Serializable {

    /** 主键 */
    private Long id;

    /** 日志类型 */
    private String logType;

    /** 保留天数 */
    private Integer retentionDays;

    /** 采样率 */
    private BigDecimal samplingRate;

    /** 是否异步写入 */
    private Boolean asyncWrite;

    /** 是否存储数据变更 */
    private Boolean storeDataChange;

    /** 是否存储技术上下文 */
    private Boolean storeTechnical;

    /** 租户ID */
    private String tenantId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
