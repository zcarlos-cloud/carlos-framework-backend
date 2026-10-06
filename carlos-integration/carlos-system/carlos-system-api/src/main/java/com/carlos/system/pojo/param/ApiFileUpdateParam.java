package com.carlos.system.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 文件更新参数（Feign接口使用）
 *
 * @author Carlos
 * @date 2026-05-30
 */
@Data
@Schema(description = "文件更新参数")
public class ApiFileUpdateParam {

    @Schema(description = "源文件名")
    private String originalName;

    @Schema(description = "文件用途")
    private String uses;
}
