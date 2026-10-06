package com.carlos.system.favorite.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 通用用户收藏 视图对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "收藏视图对象")
public class SysFavoriteVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "业务类型")
    private String targetType;

    @Schema(description = "目标标识")
    private String targetId;

    @Schema(description = "排序号")
    private Integer sortNo;

    @Schema(description = "收藏时间")
    private LocalDateTime createTime;

}
