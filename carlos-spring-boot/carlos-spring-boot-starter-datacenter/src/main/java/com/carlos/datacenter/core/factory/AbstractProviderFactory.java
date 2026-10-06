package com.carlos.datacenter.core.factory;

import com.carlos.datacenter.core.instance.DatacenterInstance;
import com.carlos.datacenter.core.supplier.SupplierInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 数据平台建造者
 * @param <S>
 * @param <C>
 */
public abstract class AbstractProviderFactory<S extends DatacenterInstance, C extends SupplierInfo> implements BaseProviderFactory<S, C> {

    private Class<C> configClass;

    public AbstractProviderFactory() {
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericSuperclass;
            Type[] typeArguments = paramType.getActualTypeArguments();
            if (typeArguments.length > 1 && typeArguments[1] instanceof Class) {
                configClass = (Class<C>) typeArguments[1];
            }
        }
    }

    /**
     * 获取配置类
     * @return 配置类
     */
    @Override
    public Class<C> getConfigClass() {
        return configClass;
    }

}
