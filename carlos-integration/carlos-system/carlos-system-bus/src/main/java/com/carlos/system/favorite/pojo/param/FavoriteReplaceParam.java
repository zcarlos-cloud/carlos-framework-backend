package com.carlos.system.favorite.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 收藏覆盖保存参数（整体排序）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "收藏覆盖保存参数")
public class FavoriteReplaceParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "收藏类型不能为空")
    @Size(max = 64, message = "收藏类型长度不能超过64")
    @Pattern(regexp = "^[a-z0-9][a-z0-9-_]{0,63}$", message = "收藏类型格式非法")
    @Schema(description = "业务类型：小写字母/数字/中划线，如 quick-nav、sys-news")
    private String targetType;

    @NotNull(message = "收藏目标列表不能为空")
    @Size(max = 100, message = "收藏目标最多100个")
    @Schema(description = "目标标识列表（按期望顺序整体覆盖，会去重保序）")
    private List<@NotBlank(message = "收藏目标不能为空") @Size(max = 128, message = "目标标识长度不能超过128") String> targetIds;

}
