package com.carlos.datacenter.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.datacenter.DatacenterConstant;
import com.carlos.datacenter.DatacenterFactory;
import com.carlos.datacenter.config.DatacenterProperties.InstanceConfig;
import com.carlos.datacenter.core.factory.BaseProviderFactory;
import com.carlos.datacenter.core.factory.ProviderFactoryHolder;
import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 数据中台实例初始化
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 23:31
 */
@Slf4j
public class DatacenterInstanceInitializer {
    private final List<BaseProviderFactory<? extends DatacenterInstance, ? extends SupplierInfo>> factoryList;

    private final DatacenterProperties datacenterProperties;

    public DatacenterInstanceInitializer(List<BaseProviderFactory<? extends DatacenterInstance, ? extends SupplierInfo>> factoryList,
                                         DatacenterProperties datacenterProperties) {
        this.factoryList = factoryList;
        this.datacenterProperties = datacenterProperties;
        onApplicationEvent();
    }

    /**
     * 注册数据平台实例
     *
     * @author Carlos
     * @date 2024-10-10 00:22
     */
    public void onApplicationEvent() {
        // 注册默认数据平台实例
        this.registerDefaultFactory();
        // 注册对象工厂
        ProviderFactoryHolder.registerFactory(factoryList);
        Map<String, InstanceConfig> instances = datacenterProperties.getInstances();

        // 解析数据平台实例配置
        for (String instanceId : instances.keySet()) {
            InstanceConfig config = instances.get(instanceId);
            String supplier = config.getSupplier();
            supplier = StrUtil.isEmpty(supplier) ? instanceId : supplier;
            // 实例对应数据平台工厂
            BaseProviderFactory<DatacenterInstance, SupplierInfo> providerFactory = (BaseProviderFactory<DatacenterInstance, SupplierInfo>) ProviderFactoryHolder.requireForSupplier(supplier);
            if (providerFactory == null) {
                log.warn("创建[{}]的数据平台服务失败，未找到供应商为[{}]的服务", instanceId, supplier);
                continue;
            }
            // 合并 typed 字段与扩展属性为统一 Map，保持下游 JSON 转换兼容
            Map<String, Object> merged = new HashMap<>();
            merged.put(DatacenterConstant.INSTANCE_ID_KEY, instanceId);
            merged.put(DatacenterConstant.SUPPLIER_KEY, supplier);
            if (config.getProperties() != null) {
                merged.putAll(config.getProperties());
            }
            // 转换为对应供应商配置类
            JSONObject configJson = new JSONObject(merged);
            SupplierInfo supplierInfo = JSONUtil.toBean(configJson, providerFactory.getConfigClass());
            // 根据配置信息生成数据平台实例
            DatacenterFactory.createDatacenterInstance(supplierInfo);
        }
    }

    /**
     * 注册默认数据平台实例
     */
    private void registerDefaultFactory() {
        // TODO: Carlos 2024-12-12 正式情况下取消注册
        // ProviderFactoryHolder.registerFactory(ShuNingFactory.instance());
    }
}
