package com.carlos.boot.web.config;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一响应包装配置属性。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class ResponseWrapProperties {

    /** 是否启用响应包装 */
    private boolean enabled = true;

    /** 需要排除包装的路径（Ant 风格） */
    private List<String> excludePaths = new ArrayList<>();

    /** 需要排除的包名（默认排除 Swagger/OpenAPI/Actuator 等框架端点） */
    private List<String> excludePackages = new ArrayList<>();
}
