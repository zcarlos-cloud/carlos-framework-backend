package com.carlos.snowflake;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 *      基础信息
 * </p>
 *
 * @author carlos
 * @date 2021/12/13 13:10
 */
@Data
@NoArgsConstructor
public class SnowflakeInfo implements Serializable {

    /** 标签 */
    private String tag;

    /** ip */
    private String ip;

    /**
     *  dataCenterId
     */
    private Long dataCenterId;

    /**
     *  workerId
     */
    private Long workerId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 过期时间 */
    private LocalDateTime expireTime;


}
