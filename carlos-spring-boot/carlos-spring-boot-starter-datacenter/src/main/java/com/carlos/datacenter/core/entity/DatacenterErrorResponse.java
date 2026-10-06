package com.carlos.datacenter.core.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 *   数据中台error信息
 * </p>
 *
 * @author Carlos
 * @date 2024-10-09 23:12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DatacenterErrorResponse implements DatacenterResponse {

    /** 错误信息 */
    String error;

}
