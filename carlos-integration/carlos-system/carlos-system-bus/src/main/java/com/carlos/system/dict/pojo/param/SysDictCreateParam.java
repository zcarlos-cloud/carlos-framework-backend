package com.carlos.system.dict.pojo.param;


import com.carlos.system.dict.pojo.enums.DictTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * <p>
 * 系统字典 新增参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统字典新增参数")
public class SysDictCreateParam {

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称长度不能超过100")
    @Schema(description = "字典名称")
    private String dictName;

    @NotBlank(message = "字典编码不能为空")
    @Size(max = 100, message = "字典编码长度不能超过100")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_.-]{1,63}$", message = "字典编码须以字母开头，仅允许字母、数字、下划线、点、中划线，长度2~64位")
    @Schema(description = "字典编码")
    private String dictCode;

    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;

    // @Valid 用于级联到元素类型 Item —— 否则 Item 上的 @NotBlank(itemName/itemCode)
    // 全都不生效，能建出没有名称、没有编码的字典项
    @Valid
    @Schema(description = "字典选项")
    private List<Item> items;

    @NotNull(message = "字典类型不能为空")
    @Schema(description = "字典类型 数字类型 字符类型")
    private DictTypeEnum type;


    @Data
    public static class Item {

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

    }
}
