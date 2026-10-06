package com.carlos.org.pojo.dto;


import com.carlos.org.pojo.enums.OrgTenantStateEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * <p>
 * 租户 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
public class OrgTenantDTO {
    /** 主键 */
    private Long id;
    /** 租户编码 */
    private String tenantCode;
    /** 租户名称 */
    private String tenantName;
    /** 联系人 */
    private String contactName;
    /** 联系电话 */
    private String contactPhone;
    /** 租户状态， 1：正常, 2: 停用 */
    private OrgTenantStateEnum tenantStatus;
    /** 过期时间（NULL 表示永不过期） */
    private LocalDateTime expireTime;
    /** 备注 */
    private String remark;
    /** 创建者 */
    private Long createBy;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 修改者 */
    private Long updateBy;
    /** 修改时间 */
    private LocalDateTime updateTime;
}
