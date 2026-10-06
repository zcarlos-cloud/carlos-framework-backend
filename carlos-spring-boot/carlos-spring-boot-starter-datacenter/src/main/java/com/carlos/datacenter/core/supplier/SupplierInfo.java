package com.carlos.datacenter.core.supplier;

/**
 * <p>
 *   数据平台供应商配置
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 22:20
 */
public interface SupplierInfo {

    /**
     * 获取实例id
     */
    String getInstanceId();

    /**
     * 获取实例名称
     */
    String getInstanceName();

    /**
     * 获取供应商唯一编码
     */
    String getSupplier();

}
