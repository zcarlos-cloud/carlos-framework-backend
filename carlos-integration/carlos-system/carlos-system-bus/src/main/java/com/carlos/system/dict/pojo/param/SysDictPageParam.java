package com.carlos.system.dict.pojo.param;


import com.carlos.core.param.ParamPage;
import com.carlos.system.dict.pojo.enums.DictTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;


/**
 * <p>
 * 系统字典 列表查询参数封装
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "系统字典列表查询参数")
public class SysDictPageParam extends ParamPage {

    @Size(max = 100, message = "字典名称长度不能超过100")
    @Schema(description = "字典名称")
    private String dictName;
    @Size(max = 500, message = "描述长度不能超过500")
    @Schema(description = "描述")
    private String description;
    @Schema(description = "字典类型 数字类型 字符类型")
    private DictTypeEnum type;
    @Schema(description = "开始时间")
    private LocalDateTime start;
    @Schema(description = "结束时间")
    private LocalDateTime end;
}
