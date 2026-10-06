package com.carlos.datacenter.core.entity;

import lombok.Data;

/**
 * <p>
 *   中台响应信息
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 23:13
 */
@Data
public class DatacenterResult<T extends DatacenterResponse> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 数据中台响应体
     */
    private T data;

    /**
     * 数据中台编码
     */
    private String instanceId;
}
