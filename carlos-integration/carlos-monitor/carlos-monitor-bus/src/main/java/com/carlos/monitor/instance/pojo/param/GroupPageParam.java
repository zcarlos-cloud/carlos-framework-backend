package com.carlos.monitor.instance.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 监控实例分组 分页查询参数封装
 * </p>
 *
 * @author Carlos
 * @date 2026年10月1日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "监控实例分组分页查询参数")
public class GroupPageParam extends ParamPage {

    @Size(max = 100, message = "分组名称长度不能超过100")
    @Schema(description = "分组名称（模糊）")
    private String groupName;
}
