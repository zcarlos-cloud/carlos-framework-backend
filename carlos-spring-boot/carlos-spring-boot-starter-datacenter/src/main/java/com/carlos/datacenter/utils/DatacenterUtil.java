package com.carlos.datacenter.utils;

import com.carlos.datacenter.DatacenterFactory;
import com.carlos.datacenter.core.factory.BaseProviderFactory;
import com.carlos.datacenter.core.factory.ProviderFactoryHolder;
import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;
import com.carlos.datacenter.exception.DatacenterInstanceException;
import lombok.extern.slf4j.Slf4j;


/**
 * <p>
 *   数据中台工具类
 * </p>
 *
 * @author Carlos
 * @date 2024-12-11 16:08
 */
@Slf4j
public class DatacenterUtil {


    public static DatacenterInstance getInstance(String configId) {
        DatacenterInstance instance = DatacenterFactory.getDatacenterInstance(configId);
        if (instance == null) {
            throw new DatacenterInstanceException("未找到可用数据平台!");
        }
        return instance;
    }

    // 获取实例列表
    public static void listInstance() {


    }


    /**
     * 手动注册工厂实例
     *
     * @param factory 工厂实例
     * @author Carlos
     * @date 2024-12-12 11:29
     */
    public static void registerFactory(BaseProviderFactory<? extends DatacenterInstance, ? extends SupplierInfo> factory) {
        if (factory == null) {
            throw new DatacenterInstanceException("注册供应商工厂失败，工厂实例不能为空");
        }
        ProviderFactoryHolder.registerFactory(factory);
    }
}
