package com.carlos.system.dict.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 系统字典 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class SysDictListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "字典code")
    private String dictCode;

    @Schema(description = "字典名称")
    private String dictName;

    @Schema(description = "字典描述")
    private String description;


    private List<DictItem> items;


    @Data
    @Accessors(chain = true)
    public static class DictItem {

        /**
         * 字典选项id
         */
        private Long id;
        /**
         * 字典项值
         */
        private String itemName;
        /**
         * 字典项key
         */
        private String itemCode;


    }
}
