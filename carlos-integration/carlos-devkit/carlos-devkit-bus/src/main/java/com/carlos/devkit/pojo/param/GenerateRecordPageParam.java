package com.carlos.devkit.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 生成记录分页查询参数
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Schema(description = "生成记录分页查询参数")
public class GenerateRecordPageParam extends ParamPage {

    @Schema(description = "业务类型：1=代码生成 2=工程生成")
    private Integer bizType;

    @Size(max = 64, message = "模板组编码长度不能超过64")
    @Schema(description = "模板组编码")
    private String templateGroupCode;

    @Size(max = 128, message = "项目名长度不能超过128")
    @Schema(description = "项目名（模糊）")
    private String projectName;

    @Schema(description = "状态：1=成功 0=失败")
    private Integer status;
}
