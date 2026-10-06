package com.carlos.datacenter.core.entity;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 *   中台元数据字段信息
 * </p>
 *
 * @author Carlos
 * @date 2024/9/13 15:58
 */
@Data
@Accessors(chain = true)
public class DatacenterMetadataDetail {

    /**
     * 字段ID
     */
    private String fieldId;
    /**
     * 英文名
     */
    private String enName;
    /**
     * 中文名
     */
    private String chName;
    /**
     * 是否开放
     */
    private Boolean isOpen;
    /**
     * 字段描述
     */
    private String remark;

    /**
     * 数据类型 建议用枚举替代
     */
    private String dataType;
}
