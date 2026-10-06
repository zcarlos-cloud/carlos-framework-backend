package com.carlos.boot.web.resource.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * API 接口资源描述。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
@Accessors(chain = true)
public class ApiResource {

    /** 资源名称 */
    private String name;
    /** 资源路径前缀 */
    private String pathPrefix;
    /** 接口路径 */
    private String path;
    /** 请求方式 */
    private RequestMethod method;
    /** 是否隐藏 */
    private Boolean hidden;
    /** 资源描述 */
    private String description;
    /**
     * 权限码（取自方法上的 {@code @PreAuthorize("hasPermission('res','act')")}）
     * <p>为 null 表示该方法未声明权限码，由存储端按路径约定兜底生成。</p>
     */
    private String permCode;
}
