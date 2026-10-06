package com.carlos.system.news.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告/站内通知 视图对象
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "公告视图对象")
public class SysNewsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型编码：1-公告 2-通知 3-更新日志")
    private Integer newsType;

    @Schema(description = "类型名称")
    private String newsTypeName;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "发布范围编码：1-全员 2-指定角色")
    private Integer publishScope;

    @Schema(description = "发布范围名称")
    private String publishScopeName;

    @Schema(description = "指定角色的角色ID JSON 数组")
    private String scopeRoleIds;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "状态编码：1-草稿 2-已发布 3-已下线")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否已读（仅用户端接口填充）")
    private Boolean isRead;

}
