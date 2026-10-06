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
public class DatacenterMetadataCatalog {
    /**
     * 目录唯一标识
     */
    private String catalogKey;
    /**
     * 目录code
     */
    private String catalogCode;
    /**
     * 目录名称
     */
    private String catalogName;
    /**
     * 目录描述
     */
    private String description;
}
