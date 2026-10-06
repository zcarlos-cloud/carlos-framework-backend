package com.carlos.datacenter.config;

import com.carlos.datacenter.core.factory.BaseProviderFactory;
import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;


/**
 * <p>
 * 数据平台供应商配置
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 22:26
 */
@AutoConfiguration
public class DatacenterSupplierConfig {

    /**
     * 数据平台实例初始化
     */
    @Bean
    protected DatacenterInstanceInitializer datacenterInstanceInitializer(List<BaseProviderFactory<? extends DatacenterInstance, ? extends SupplierInfo>> factoryList,
                                                                          DatacenterProperties datacenterProperties
    ) {
        return new DatacenterInstanceInitializer(factoryList, datacenterProperties);
    }

}
