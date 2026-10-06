package com.carlos.datacenter;

import cn.hutool.core.util.StrUtil;
import com.carlos.datacenter.core.factory.BaseProviderFactory;
import com.carlos.datacenter.core.factory.ProviderFactoryHolder;
import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;
import com.carlos.datacenter.exception.DatacenterInstanceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * <p>
 *   构造工厂，用于获取一个厂商的数据平台实现对象
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 22:21
 */
public class DatacenterFactory {

    /**
     * <p>框架维护的所有数据平台对象</p>
     * <p>key: instanceId，数据平台对象的唯一标识</p>
     * <p>value: 数据平台对象</p>
     */
    private static final Map<String, DatacenterInstance> INSTANCES = new ConcurrentHashMap<>();

    private DatacenterFactory() {
    }

    /**
     * 创建数据平台实例
     * <p>创建各个厂商的实现类
     *
     * @param config 数据平台配置
     * @author Carlos
     */
    public static void createDatacenterInstance(SupplierInfo config) {
        DatacenterInstance instance = create(config);
        // 注册数据平台实例
        register(instance);
    }

    /**
     * 创建数据平台实例
     *
     * @param config 配置信息
     * @return com.carlos.integration.datacenter.core.instance.DatacenterInstance
     * @author Carlos
     * @date 2024-12-12 14:11
     */
    private static DatacenterInstance create(SupplierInfo config) {
        BaseProviderFactory factory = ProviderFactoryHolder.requireForSupplier(config.getSupplier());
        if (factory == null) {
            throw new DatacenterInstanceException("不支持当前供应商配置");
        }
        DatacenterInstance instance = factory.createDatacenter(config);
        return instance;
    }

    /**
     * 通过instanceId获取数据平台对象
     *
     * @param instanceId 唯一标识
     * @return 返回数据平台对象。如果未找到则返回null
     */
    public static DatacenterInstance getDatacenterInstance(String instanceId) {
        return INSTANCES.get(instanceId);
    }

    /**
     * 通过供应商标识获取单个数据平台对象
     * <p>当供应商有多个数据平台对象时无法保证获取顺序</p>
     *
     * @param supplier 供应商标识
     * @return 返回数据平台对象。如果未找到则返回null
     */
    public static DatacenterInstance getBySupplier(String supplier) {
        if (StrUtil.isEmpty(supplier)) {
            throw new DatacenterInstanceException("供应商标识不能为空");
        }
        return INSTANCES.values().stream().filter(instance -> supplier.equals(instance.getSupplier())).findFirst().orElse(null);
    }

    /**
     * 通过供应商标识获取数据平台对象列表
     *
     * @param supplier 供应商标识
     * @return 返回数据平台对象列表。如果未找到则返回空列表
     */
    public static List<DatacenterInstance> getListBySupplier(String supplier) {
        List<DatacenterInstance> list;
        if (StrUtil.isEmpty(supplier)) {
            throw new DatacenterInstanceException("供应商标识不能为空");
        }
        list = INSTANCES.values().stream().filter(instance -> supplier.equals(instance.getSupplier())).collect(Collectors.toList());
        return list;
    }

    /**
     * 获取全部数据平台对象
     *
     * @return 数据平台对象列表
     */
    public static List<DatacenterInstance> getAll() {
        return new ArrayList<>(INSTANCES.values());
    }

    /**
     * 注册数据平台对象
     *
     * @param instance 数据平台对象
     */
    public static void register(DatacenterInstance instance) {
        if (instance == null) {
            throw new DatacenterInstanceException("数据平台对象不能为空");
        }
        INSTANCES.put(instance.getInstanceId(), instance);
    }

    /**
     * 注册数据平台对象
     *
     * @param instance 数据平台对象
     */
    public static void register(DatacenterInstance instance, Integer weight) {
        if (instance == null) {
            throw new DatacenterInstanceException("数据平台对象不能为空");
        }
        INSTANCES.put(instance.getInstanceId(), instance);
    }

    /**
     * 以instanceId为标识，当数据平台对象不存在时，进行注册
     *
     * @param instance 数据平台对象
     * @return 是否注册成功
     * <p>当对象不存在时，进行注册并返回true</p>
     * <p>当对象已存在时，返回false</p>
     */
    public static boolean registerIfAbsent(DatacenterInstance instance) {
        if (instance == null) {
            throw new DatacenterInstanceException("数据平台对象不能为空");
        }
        String instanceId = instance.getInstanceId();
        if (INSTANCES.containsKey(instanceId)) {
            return false;
        }
        INSTANCES.put(instanceId, instance);
        return true;
    }

    /**
     * registerIfAbsent
     * <p> 以instanceId为标识，当数据平台对象不存在时，进行注册。并添加至系统的负载均衡器
     *
     * @param instance 数据平台对象
     * @param weight   权重
     * @return 是否注册成功
     * <p>当对象不存在时，进行注册并返回true</p>
     * <p>当对象已存在时，返回false</p>
     * @author Carlos
     */
    public static boolean registerIfAbsent(DatacenterInstance instance, Integer weight) {
        if (instance == null) {
            throw new DatacenterInstanceException("数据平台对象不能为空");
        }
        String instanceId = instance.getInstanceId();
        if (INSTANCES.containsKey(instanceId)) {
            return false;
        }
        INSTANCES.put(instanceId, instance);
        return true;
    }

    /**
     * 注销数据平台对象
     * <p>与此同时会注销掉负载均衡器中已经存在的对象</p>
     *
     * @param instanceId 标识
     * @return 是否注销成功
     * <p>当instanceId存在时，进行注销并返回true</p>
     * <p>当instanceId不存在时，返回false</p>
     */
    public static boolean unregister(String instanceId) {
        DatacenterInstance blend = INSTANCES.remove(instanceId);
        return blend != null;
    }


}
