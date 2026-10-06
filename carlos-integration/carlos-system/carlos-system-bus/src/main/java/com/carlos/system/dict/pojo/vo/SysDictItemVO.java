package com.carlos.system.dict.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 系统字典详情 显示层对象，向页面传输的对象
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SysDictItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "字典id")
    private Long dictId;

    @Schema(description = "字典项值")
    private String itemName;

    @Schema(description = "字典项key")
    private String itemCode;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "排序")
    private Integer orderNum;

    @Schema(description = "状态（1启用 0不启用）")
    private Boolean enable;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;


    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
