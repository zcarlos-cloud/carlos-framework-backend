package com.carlos.boot.web.config;

import com.carlos.boot.web.WebConstant;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

/**
 * HandlerInterceptor 配置属性。
 *
 * @author Carlos
 * @since 3.0.0
 */
@Data
public class InterceptorProperties {

    /** 请求日志拦截器 */
    private RequestLogProps requestLog = new RequestLogProps();

    @Data
    public static class RequestLogProps {
        /** 是否启用 */
        private boolean enabled = true;

        /** 日志输出类型 */
        private LogType logType = LogType.REQUEST;

        /** 包含的路径 */
        private Set<String> includePaths = new HashSet<>();

        /** 排除路径（自动合并框架通用排除路径） */
        private Set<String> excludePaths = new HashSet<>();

        /** 日志打印类型 */
        public enum LogType {
            /** 不打印 */
            NONE,
            /** 打印请求信息 */
            REQUEST,
            /** 打印请求和响应信息 */
            BOTH
        }

        /**
         * 获取有效排除路径（自动合并通用排除路径）。
         */
        public Set<String> getEffectiveExcludePaths() {
            Set<String> result = new HashSet<>(WebConstant.COMMON_EXCLUDE_PATTERNS);
            if (excludePaths != null) {
                result.addAll(excludePaths);
            }
            return result;
        }
    }
}
