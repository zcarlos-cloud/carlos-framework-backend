package com.carlos.boot.web.util;

import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>
 * 响应写入工具类。
 * </p>
 * <p>
 * 提供将对象写入 HTTP 响应（JSON 格式）以及获取当前请求对应的 HttpServletResponse 的便捷方法。
 * </p>
 *
 * @author carlos
 * @date 2020/4/10 16:47
 */
public final class ResponseWriter {

    private ResponseWriter() {
        throw new AssertionError();
    }

    /**
     * 输出 JSON 到浏览器。
     *
     * @param response 响应体
     * @param object   对象内容
     * @author carlos
     * @date 2020/4/10 16:47
     */
    public static void writeJson(HttpServletResponse response, Object object) {
        response.reset();
        response.setContentType(ContentType.JSON.toString());
        response.setCharacterEncoding("UTF-8");
        JakartaServletUtil.write(response, JSONUtil.toJsonStr(object), ContentType.JSON.toString());
    }

    /**
     * 获取当前请求的 HttpServletResponse。
     *
     * @return HttpServletResponse，无请求上下文时返回 null
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getResponse();
    }

}
