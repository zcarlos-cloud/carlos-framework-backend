package com.carlos.boot.web.config;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 资源扫描配置属性。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class ResourceScanProperties {

    /** 是否启用资源扫描，默认关闭 */
    private boolean enabled = false;

    /** 扫描的包 */
    private Set<String> scanPackages = new HashSet<>();

    /** 资源路径前缀 */
    private String pathPrefix = "";
}
