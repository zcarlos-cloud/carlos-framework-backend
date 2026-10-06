package com.carlos.system.news.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 公告新增参数
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Schema(description = "公告新增参数")
public class SysNewsCreateParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "公告标题不能为空")
    @Size(max = 256, message = "公告标题长度不能超过256")
    @Schema(description = "标题")
    private String title;

    @NotNull(message = "公告类型不能为空")
    @Schema(description = "类型：1-公告 2-通知 3-更新日志")
    private Integer newsType;

    @NotBlank(message = "公告内容不能为空")
    @Size(max = 20000, message = "公告内容长度不能超过20000")
    @Schema(description = "内容")
    private String content;

    @NotNull(message = "发布范围不能为空")
    @Schema(description = "发布范围：1-全员 2-指定角色")
    private Integer publishScope;

    @Size(max = 20, message = "指定角色最多选择20个")
    @Schema(description = "指定角色的角色标识列表（publishScope=2 时必填）")
    private List<String> scopeRoleIds;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "过期时间，空表示永不过期")
    private LocalDateTime expireTime;

}
