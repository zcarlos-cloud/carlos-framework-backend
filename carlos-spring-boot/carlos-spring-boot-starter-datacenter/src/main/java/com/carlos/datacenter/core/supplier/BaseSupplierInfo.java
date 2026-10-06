package com.carlos.datacenter.core.supplier;

import lombok.Data;

/**
 * <p>
 * 供应商基础属性
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 23:18
 */
@Data
public abstract class BaseSupplierInfo implements SupplierInfo {

    /**
     * 实例id
     */
    private String instanceId;

    /**
     * 实例名称
     */
    private String instanceName;

    /**
     * 实例描述
     */
    private String description;

    /**
     * 是否可用
     */
    private Boolean enable = true;


}
