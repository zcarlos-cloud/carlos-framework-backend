package com.carlos.datacenter.core.factory;

import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;

/**
 * <p>数据平台对象建造者</p>
 * @author carlos
 * @param <S> 数据平台实例
 * @param <C> 数据平台配置对象
 */
public interface BaseProviderFactory<S extends DatacenterInstance, C extends SupplierInfo> {

    /**
     * 创建数据平台实现对象
     * @param c 数据平台配置对象
     * @return 数据平台实现对象
     */
    S createDatacenter(C c);

    /**
     * 获取配置类
     * @return 配置类
     */
    Class<C> getConfigClass();

    /**
     * 获取供应商
     * @return 供应商
     */
    String getSupplier();

}
