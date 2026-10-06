package com.carlos.devkit.pojo.param;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 工程生成参数（预览与下载共用）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@Schema(description = "工程生成参数")
public class ProjectGenRunParam {

    @NotNull(message = "模板组ID不能为空")
    @Schema(description = "模板组ID")
    private Long templateGroupId;

    @NotBlank(message = "构件ID不能为空")
    @Size(max = 64, message = "构件ID长度不能超过64")
    @Pattern(regexp = "^[a-z][a-z0-9-]*$", message = "构件ID只能由小写字母、数字和中划线组成，且以字母开头")
    @Schema(description = "Maven 构件ID（artifactId），如 demo-service")
    private String artifactId;

    @Size(max = 128, message = "项目名长度不能超过128")
    @Schema(description = "项目名（产物根目录名），留空时取构件ID")
    private String projectName;

    @NotBlank(message = "groupId 不能为空")
    @Size(max = 128, message = "groupId 长度不能超过128")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9.]*$", message = "groupId 只能由字母、数字和点组成")
    @Schema(description = "Maven groupId（同时作为包名），如 com.carlos.demo")
    private String mavenGroupId;

    @Size(max = 64, message = "版本号长度不能超过64")
    @Schema(description = "Maven 版本号，默认 1.0.0-SNAPSHOT")
    private String version = "1.0.0-SNAPSHOT";

    @Size(max = 512, message = "项目描述长度不能超过512")
    @Schema(description = "项目描述")
    private String describe;

    @Size(max = 64, message = "作者长度不能超过64")
    @Schema(description = "项目负责人，默认 Carlos")
    private String author = "Carlos";
}
