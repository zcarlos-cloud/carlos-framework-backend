package com.carlos.monitor.instance.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 监控分组授权 参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@Schema(description = "监控分组授权参数")
public class GroupAuthParam {

    @NotNull(message = "授权角色ID列表不能为空")
    @Schema(description = "授权角色ID列表（全量替换，空列表表示清空授权）")
    private List<Long> roleIds;
}
