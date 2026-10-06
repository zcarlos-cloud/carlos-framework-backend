package com.carlos.system.upload.pojo.param;

import com.carlos.system.upload.pojo.enums.BucketPolicyEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Bucket操作参数
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Schema(description = "Bucket操作参数")
public class BucketCreateParam {

    @NotBlank(message = "Bucket名称不能为空")
    @Size(min = 3, max = 63, message = "Bucket名称长度必须在3~63位之间")
    @Pattern(regexp = "^[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]$", message = "Bucket名称须为小写字母、数字、中划线或点，且以字母或数字开头和结尾")
    @Schema(description = "Bucket名称")
    private String name;

    @NotNull(message = "访问策略不能为空")
    @Schema(description = "访问策略")
    private BucketPolicyEnum policy;
}
