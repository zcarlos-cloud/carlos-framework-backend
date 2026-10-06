package com.carlos.org.api.pojo.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 租户 API 响应对象
 * </p>
 * <p>供 Feign 调用方（如 carlos-auth 登录租户校验）使用</p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrgTenantAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（租户ID）
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户状态， 1：正常, 2: 停用
     */
    private Integer tenantStatus;

    /**
     * 过期时间（NULL 表示永不过期）
     */
    private LocalDateTime expireTime;

}
