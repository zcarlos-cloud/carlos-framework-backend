package com.carlos.datacenter.core.entity;


import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 *   元数据目录信息
 * </p>
 *
 * @author Carlos
 * @date 2024/9/13 15:58
 */
@Data
@Accessors(chain = true)
public class DatacenterMetadata {
    /**
     * 目录标识
     */
    private String catalogKey;
    /**
     * 元数据标识
     */
    private String metaKey;
    /**
     * 元数据编码
     */
    private String metaCode;
    /**
     * 元数据名称
     */
    private String metaName;
    /**
     * 元数据描述
     */
    private String description;
    /**
     * 元数据状态
     */
    private String state;
    /**
     * 数据总数
     */
    private Long dataCount;


}
