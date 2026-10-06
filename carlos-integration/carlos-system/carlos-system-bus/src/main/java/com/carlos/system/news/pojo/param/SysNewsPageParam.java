package com.carlos.system.news.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 公告分页查询参数（管理端）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告分页查询参数")
public class SysNewsPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    @Size(max = 256, message = "标题长度不能超过256")
    @Schema(description = "标题（模糊）")
    private String title;

    @Min(value = 1, message = "公告类型取值 1-公告 2-通知 3-更新日志")
    @Max(value = 3, message = "公告类型取值 1-公告 2-通知 3-更新日志")
    @Schema(description = "类型：1-公告 2-通知 3-更新日志")
    private Integer newsType;

    @Min(value = 1, message = "公告状态取值 1-草稿 2-已发布 3-已下线")
    @Max(value = 3, message = "公告状态取值 1-草稿 2-已发布 3-已下线")
    @Schema(description = "状态：1-草稿 2-已发布 3-已下线")
    private Integer status;

}
