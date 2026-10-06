package com.carlos.system.news.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 我的公告分页查询参数（用户端）
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我的公告分页查询参数")
public class SysNewsMyPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    @Min(value = 1, message = "公告类型取值 1-公告 2-通知 3-更新日志")
    @Max(value = 3, message = "公告类型取值 1-公告 2-通知 3-更新日志")
    @Schema(description = "类型：1-公告 2-通知 3-更新日志")
    private Integer newsType;

    @Schema(description = "仅看未读")
    private Boolean onlyUnread;

}
