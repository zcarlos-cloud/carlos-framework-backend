package com.carlos.system.pojo.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统字典详情 数据传输对象，service和manager向外传输对象
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DictItemAO {
    /**
     * 字典选项id
     */
    private Long id;

    /**
     * 字典id
     */
    private Long dictId;
    /**
     * 字典项值
     */
    private String itemName;
    /**
     * 字典项key
     */
    private String itemCode;
    /**
     * 描述
     */
    private String description;
    /**
     * 排序
     */
    private Integer orderNum;
    /**
     * 字典状态
     */
    private Boolean enable;
}
