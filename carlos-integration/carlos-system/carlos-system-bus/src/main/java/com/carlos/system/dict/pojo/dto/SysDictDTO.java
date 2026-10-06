package com.carlos.system.dict.pojo.dto;


import com.carlos.system.dict.pojo.enums.DictTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 系统字典 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
public class SysDictDTO {

    /**
     * 字典id
     */
    private Long id;
    /**
     * 字典名称
     */
    private String dictName;
    /**
     * 字典编码
     */
    private String dictCode;
    /**
     * 描述
     */
    private String description;
    /**
     * 字典类型 数字类型 字符类型
     */
    private DictTypeEnum type;

    private List<SysDictItemDTO> items;

}
