package com.carlos.system.favorite.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * <p>
 * 收藏添加/取消参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "收藏添加/取消参数")
public class FavoriteToggleParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收藏类型不能为空")
    @Size(max = 64, message = "收藏类型长度不能超过64")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-_]{0,63}$", message = "收藏类型格式非法")
    @Schema(description = "业务类型：小写字母/数字/中划线，如 quick-nav、sys-news")
    private String targetType;

    @NotBlank(message = "收藏目标不能为空")
    @Size(max = 128, message = "目标标识长度不能超过128")
    @Schema(description = "目标标识：路由 key、公告 id 等")
    private String targetId;

}
