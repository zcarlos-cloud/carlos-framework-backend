package com.carlos.system.dict.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;


/**
 * <p>
 * 系统字典详情 新增参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@Schema(description = "系统字典详情新增参数")
public class SysDictItemCreateParam {

    @NotNull(message = "字典id不能为空")
    @Schema(description = "字典id")
    private Long dictId;
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
