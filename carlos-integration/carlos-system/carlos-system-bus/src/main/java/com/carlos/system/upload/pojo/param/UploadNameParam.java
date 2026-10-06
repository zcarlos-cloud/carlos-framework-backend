package com.carlos.system.upload.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;


/**
 * <p>
 * 新增参数封装
 * </p>
 *
 * @author Carlos
 * @date 2021-11-10 10:45:55
 */

@Data
public class UploadNameParam {

    @NotEmpty(message = "文件名不能为空")
    @Schema(description = "文件名")
    private Set<String> filenames;

    @NotNull(message = "文件用途不能为空")
    @Schema(description = "文件用途")
    private String using;

    @Schema(description = "任务id")
    private String taskId;

}
