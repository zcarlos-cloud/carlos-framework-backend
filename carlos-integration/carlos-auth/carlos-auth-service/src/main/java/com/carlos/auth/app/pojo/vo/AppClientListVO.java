package com.carlos.auth.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 应用信息 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2025-3-12 14:00:14
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppClientListVO implements Serializable {
    private static final long serialVersionUID = 1L;
    @Schema(description = "主键")
    private Long id;
    @Schema(description = "应用编号")
    private String appKey;
    @Schema(description = "应用名称")
    private String appName;
    @Schema(description = "应用logo")
    private String appLogo;
    @Schema(description = "应用密钥")
    private String appSecret;
}
