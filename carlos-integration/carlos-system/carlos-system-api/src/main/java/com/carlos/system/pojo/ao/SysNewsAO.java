package com.carlos.system.pojo.ao;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 公告/站内通知 API 对象（Feign 调用方使用）
 * </p>
 *
 * <p>S3 公告管理：由原 2022 年孤儿类重写为 sys_news 表模型，
 * 供其他服务（如 carlos-message 推送集成）查询公告内容。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "公告 API 对象")
public class SysNewsAO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "类型：1-公告 2-通知 3-更新日志")
    private Integer newsType;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "发布范围：1-全员 2-指定角色")
    private Integer publishScope;

    @Schema(description = "指定角色的角色ID JSON 数组")
    private String scopeRoleIds;

    @Schema(description = "是否置顶")
    private Boolean isTop;

    @Schema(description = "状态：1-草稿 2-已发布 3-已下线")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "过期时间，NULL 表示永不过期")
    private LocalDateTime expireTime;

    @Schema(description = "租户ID")
    private Long tenantId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
