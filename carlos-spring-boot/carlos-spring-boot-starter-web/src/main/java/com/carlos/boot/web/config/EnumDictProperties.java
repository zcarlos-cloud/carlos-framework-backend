package com.carlos.boot.web.config;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * 枚举字典配置属性。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class EnumDictProperties {

    /** 是否启用枚举字典，默认关闭 */
    private boolean enabled = false;

    /** 扫描的包 */
    private Set<String> scanPackages = new HashSet<>();
}
