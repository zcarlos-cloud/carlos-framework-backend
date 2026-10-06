package com.carlos.message.core.template;

import java.util.Map;

/**
 * <p>
 * 模板引擎接口
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
public interface TemplateEngine {

    /**
     * 渲染模板
     *
     * @param templateCode 模板编码
     * @param params       参数
     * @return 渲染后的内容
     */
    String render(String templateCode, Map<String, Object> params);

    /**
     * 渲染模板内容
     *
     * @param templateContent 模板内容
     * @param params          参数
     * @return 渲染后的内容
     */
    String renderContent(String templateContent, Map<String, Object> params);

    /**
     * 验证模板参数
     *
     * @param templateCode 模板编码
     * @param params       参数
     * @return 是否有效
     */
    boolean validateParams(String templateCode, Map<String, Object> params);
}
