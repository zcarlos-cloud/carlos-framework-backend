package com.carlos.system.dict.pojo.param;


import com.carlos.system.dict.pojo.enums.DictTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 系统字典 更新参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统字典修改参数")
public class SysDictUpdateParam {

    @NotNull(message = "主键不能为空")
    @Schema(description = "主键")
    private Long id;
    @Size(max = 100, message = "字典名称长度不能超过100")
    @Schema(description = "字典名称")
    private String dictName;
    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;
    @Schema(description = "字典类型 数字类型 字符类型")
    private DictTypeEnum type;


    // @Valid 级联到元素类型 Item，与新增参数同口径 —— 否则更新时 Item 上的
    // @NotBlank(itemName/itemCode) 不生效，能改成没有名称、没有编码的字典项
    @Valid
    @Schema(description = "字典选项")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "id")
        private Long id;
        @NotBlank(message = "字典项值不能为空")
        @Size(max = 100, message = "字典项值长度不能超过100")
        @Schema(description = "字典项值")
        private String itemName;
        @NotBlank(message = "字典项code不能为空")
        @Size(max = 100, message = "字典项code长度不能超过100")
        @Schema(description = "字典项key")
        private String itemCode;
        @Size(max = 500, message = "描述长度不能超过500")
        @Schema(description = "描述")
        private String description;
        @Schema(description = "排序")
        private Integer orderNum;
        @Schema(description = "是否开启")
        private Boolean enable;

    }

}
