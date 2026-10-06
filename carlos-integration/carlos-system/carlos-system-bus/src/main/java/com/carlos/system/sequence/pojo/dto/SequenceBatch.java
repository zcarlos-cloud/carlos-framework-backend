package com.carlos.system.sequence.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 序列号号段传输对象
 * </p>
 *
 * <p>一次自增分配的连续流水区间（start ~ end 含端点），period 为分配时的日期段。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class SequenceBatch implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 号段起始流水值（含）
     */
    private long start;
    /**
     * 号段结束流水值（含）
     */
    private long end;
    /**
     * 号段可用编码数量
     */
    private int count;
    /**
     * 号段所属日期段（空串表示无日期段）
     */
    private String period;

}
