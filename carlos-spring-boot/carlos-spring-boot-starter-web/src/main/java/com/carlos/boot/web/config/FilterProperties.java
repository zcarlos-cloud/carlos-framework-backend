package com.carlos.boot.web.config;

import com.carlos.boot.web.WebConstant;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * Servlet Filter 配置属性。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class FilterProperties {

    /** 请求体缓存 Filter */
    private FilterEntry bodyCache = new FilterEntry(-100, true);

    /** XSS 防护 Filter */
    private FilterEntry xss = new FilterEntry(-99, false);

    @Data
    public static class FilterEntry {
        private boolean enabled;
        private int order;
        private String[] urlPatterns = {"/*"};
        private Set<String> excludeUrls = new HashSet<>();

        public FilterEntry() {
        }

        public FilterEntry(int order, boolean enabled) {
            this.order = order;
            this.enabled = enabled;
        }

        /**
         * 获取排除路径（自动合并框架通用排除路径）。
         */
        public Set<String> getEffectiveExcludes() {
            Set<String> result = new HashSet<>(WebConstant.COMMON_EXCLUDE_PATTERNS);
            if (excludeUrls != null) {
                result.addAll(excludeUrls);
            }
            return result;
        }
    }
}
