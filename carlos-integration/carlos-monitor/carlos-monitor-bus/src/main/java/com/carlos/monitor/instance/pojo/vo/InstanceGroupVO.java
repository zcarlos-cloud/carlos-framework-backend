package com.carlos.monitor.instance.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 监控实例分组 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "监控实例分组视图对象")
public class InstanceGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键（雪花ID）")
    private Long id;

    @Schema(description = "分组名称")
    private String groupName;

    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "排序号")
    private Integer orderNum;

    @Schema(description = "授权角色ID列表")
    private List<Long> roleIds;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;
}
