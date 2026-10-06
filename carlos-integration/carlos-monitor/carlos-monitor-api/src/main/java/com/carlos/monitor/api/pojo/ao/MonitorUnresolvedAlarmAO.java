package com.carlos.monitor.api.pojo.ao;

import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 未恢复告警计数 API提供的对象(API Object)
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
public class MonitorUnresolvedAlarmAO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 未恢复（FIRING/CONFIRMED）告警数量 */
    private Long unresolvedCount;
}
