package com.carlos.audit.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * <p>
 * 审计日志配置（动态TTL与采样策略） 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年3月6日 下午9:31:12
 */
@Data
@Accessors(chain = true)
public class AuditLogConfigDTO {
    /** 主键 */
    private Long id;
    /** 日志类型，如：USER_LOGIN */
    private String logType;
    /** 保留天数 */
    private Integer retentionDays;
    /** 采样率 0.00-1.00，1.00为全量 */
    private BigDecimal samplingRate;
    /** 是否异步写入：0-同步/1-异步 */
    private Boolean asyncWrite;
    /** 是否存储数据变更：0-否/1-是 */
    private Boolean storeDataChange;
    /** 是否存储技术上下文：0-否/1-是 */
    private Boolean storeTechnical;
    /** 租户ID，0表示系统级配置 */
    private String tenantId;
    /** 创建者编号 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新者编号 */
    private Long updateBy;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
