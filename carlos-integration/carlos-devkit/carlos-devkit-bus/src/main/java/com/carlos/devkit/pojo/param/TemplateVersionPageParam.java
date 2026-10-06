package com.carlos.devkit.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 模板版本历史分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "模板版本历史分页查询参数")
public class TemplateVersionPageParam extends ParamPage {

    @NotNull(message = "模板文件ID不能为空")
    @Schema(description = "模板文件ID")
    private Long templateId;
}
