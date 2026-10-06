package com.carlos.system.upload.pojo.param;

import com.carlos.core.param.ParamPage;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件存储配置分页查询参数
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文件存储配置分页查询参数")
public class FileStorageConfigPageParam extends ParamPage {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "配置名称长度不能超过100")
    @Schema(description = "配置名称")
    private String configName;

    @Size(max = 20, message = "存储类型长度不能超过20")
    @Schema(description = "存储类型 S3/LOCAL")
    private String storageType;

    @Schema(description = "状态 1启用 0禁用")
    private Integer configStatus;
}
