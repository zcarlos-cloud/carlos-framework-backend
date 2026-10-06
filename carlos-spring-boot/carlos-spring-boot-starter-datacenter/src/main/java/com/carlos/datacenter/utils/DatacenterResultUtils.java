package com.carlos.datacenter.utils;

import com.carlos.datacenter.core.entity.DatacenterErrorResponse;
import com.carlos.datacenter.core.entity.DatacenterResponse;
import com.carlos.datacenter.core.entity.DatacenterResult;

/**
 * <p>
 * 响应信息工具类
 * </p>
 *
 * @author Carlos
 * @date 2024-10-10 07:59
 */
public class DatacenterResultUtils {
    private DatacenterResultUtils() {
    }   // 私有构造防止实例化

    public static DatacenterResult<? extends DatacenterResponse> error() {
        return error("error no response", null);
    }

    public static DatacenterResult<? extends DatacenterResponse> error(String instanceId) {
        return error("error no response", instanceId);
    }

    public static DatacenterResult<? extends DatacenterResponse> error(String detailMessage, String instanceId) {
        return resp(new DatacenterErrorResponse(detailMessage), false, instanceId);
    }

    public static DatacenterResult<? extends DatacenterResponse> success() {
        return success(null);
    }

    public static DatacenterResult<? extends DatacenterResponse> success(DatacenterResponse data) {
        return success(data, null);
    }

    public static DatacenterResult<? extends DatacenterResponse> resp(DatacenterResponse data, boolean success) {
        return resp(data, success, null);
    }

    public static DatacenterResult<? extends DatacenterResponse> success(DatacenterResponse data, String instanceId) {
        return resp(data, true, instanceId);
    }

    public static DatacenterResult<? extends DatacenterResponse> resp(boolean success) {
        return success ? success() : error();
    }

    public static DatacenterResult<? extends DatacenterResponse> resp(boolean success, String instanceId) {
        return resp(null, success, instanceId);
    }

    public static <T extends DatacenterResponse> DatacenterResult<? extends DatacenterResponse> resp(T data, boolean success, String instanceId) {
        DatacenterResult<T> error = new DatacenterResult<>();
        error.setSuccess(success);
        error.setData(data);
        error.setInstanceId(instanceId);
        return error;
    }
}
