package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 通用批量 ID 参数（删除等按 ID 批量操作）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "批量ID参数")
public class DevkitIdsParam implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "ID列表不能为空")
    @Schema(description = "ID列表")
    private List<Long> ids;
}
