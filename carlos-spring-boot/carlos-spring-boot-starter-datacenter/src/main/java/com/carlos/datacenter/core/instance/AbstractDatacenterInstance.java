package com.carlos.datacenter.core.instance;

import cn.hutool.core.util.StrUtil;
import com.carlos.datacenter.core.entity.DatacenterResponse;
import com.carlos.datacenter.core.entity.DatacenterResult;
import com.carlos.datacenter.core.supplier.SupplierInfo;
import com.carlos.datacenter.utils.DatacenterResultUtils;
import lombok.Getter;

/**
 * <p>
 *   数据平台服务抽象类
 * </p>
 *
 * @author Carlos
 * @date 2024-10-10 00:18
 */
public abstract class AbstractDatacenterInstance<C extends SupplierInfo> implements DatacenterInstance {

    @Getter
    private final String instanceId;

    private final C config;

    protected AbstractDatacenterInstance(C config) {
        this.instanceId = StrUtil.isEmpty(config.getInstanceId()) ? getSupplier() : config.getInstanceId();
        this.config = config;
    }

    protected C getConfig() {
        return config;
    }


    /**
     * 返回异常
     * @param errorMsg 异常信息
     * @return DatacenterResponse
     */
    public DatacenterResult<? extends DatacenterResponse> errorResp(String errorMsg) {
        return DatacenterResultUtils.error(errorMsg, config.getInstanceId());
    }
}
