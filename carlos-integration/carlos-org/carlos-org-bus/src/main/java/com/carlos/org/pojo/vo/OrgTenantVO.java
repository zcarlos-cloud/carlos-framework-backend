package com.carlos.org.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 租户 VO
 * </p>
 * <p>租户列表, 租户详情</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgTenantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Schema(description = "主键")
    private Long id;

    /**
     * 租户编码
     */
    @Schema(description = "租户编码")
    private String tenantCode;

    /**
     * 租户名称
     */
    @Schema(description = "租户名称")
    private String tenantName;

    /**
     * 联系人
     */
    @Schema(description = "联系人")
    private String contactName;

    /**
     * 联系电话
     */
    @Schema(description = "联系电话")
    private String contactPhone;

    /**
     * 租户状态， 1：正常, 2: 停用
     */
    @Schema(description = "租户状态， 1：正常, 2: 停用")
    private Integer tenantStatus;

    /**
     * 租户状态名称
     */
    @Schema(description = "租户状态名称")
    private String tenantStatusName;

    /**
     * 过期时间（NULL 表示永不过期）
     */
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    /**
     * 备注
     */
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建者
     */
    @Schema(description = "创建者")
    private Long createBy;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

}
