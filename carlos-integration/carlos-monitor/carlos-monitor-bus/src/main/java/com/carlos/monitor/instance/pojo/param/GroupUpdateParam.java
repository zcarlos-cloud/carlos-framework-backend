package com.carlos.monitor.instance.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 监控实例分组 修改参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "监控实例分组修改参数")
public class GroupUpdateParam {

    @NotNull(message = "分组ID不能为空")
    @Schema(description = "分组ID")
    private Long id;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 100, message = "分组名称长度不能超过100")
    @Schema(description = "分组名称")
    private String groupName;

    @Size(max = 500, message = "分组描述长度不能超过500")
    @Schema(description = "分组描述")
    private String description;

    @Schema(description = "排序号")
    private Integer orderNum;

    @Schema(description = "授权角色ID列表（全量替换）")
    private List<Long> roleIds;
}
