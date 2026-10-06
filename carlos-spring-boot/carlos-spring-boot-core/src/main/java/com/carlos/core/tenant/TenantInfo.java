package com.carlos.core.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 多租户信息
 * </p>
 *
 * @author carlos
 * @date 2022/11/6 15:58
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class TenantInfo implements Serializable {

    /**
     * 租户id
     */
    private Serializable tenantId;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 租户编码
     */
    private String tenantCode;
}
