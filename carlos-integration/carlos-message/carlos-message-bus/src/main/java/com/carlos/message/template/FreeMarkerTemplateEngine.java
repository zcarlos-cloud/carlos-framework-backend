package com.carlos.message.template;

import com.carlos.message.core.template.TemplateEngine;
import com.carlos.message.manager.MessageTemplateManager;
import com.carlos.message.pojo.dto.MessageTemplateDTO;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

/**
 * <p>
 * FreeMarker 模板引擎实现
 * </p>
 *
 * @author Carlos
 * @date 2026/5/30
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FreeMarkerTemplateEngine implements TemplateEngine {

    private final Configuration freeMarkerConfig;
    private final MessageTemplateManager messageTemplateManager;

    @Override
    public String render(String templateCode, Map<String, Object> params) {
        MessageTemplateDTO template = messageTemplateManager.getByTemplateCode(templateCode);
        if (template == null) {
            log.warn("模板不存在: {}", templateCode);
            return null;
        }
        String content = template.getContentTemplate();
        if (content == null) {
            log.warn("模板内容为空: {}", templateCode);
            return null;
        }
        return renderContent(content, params);
    }

    @Override
    public String renderContent(String templateContent, Map<String, Object> params) {
        if (templateContent == null || templateContent.isEmpty()) {
            return templateContent;
        }
        if (params == null || params.isEmpty()) {
            return templateContent;
        }
        try {
            Template template = new Template("inline-" + System.nanoTime(),
                new StringReader(templateContent), freeMarkerConfig);
            StringWriter writer = new StringWriter();
            template.process(params, writer);
            return writer.toString();
        } catch (TemplateException | java.io.IOException e) {
            log.error("FreeMarker 模板渲染失败, content={}", templateContent, e);
            return templateContent;
        }
    }

    @Override
    public boolean validateParams(String templateCode, Map<String, Object> params) {
        MessageTemplateDTO template = messageTemplateManager.getByTemplateCode(templateCode);
        if (template == null || template.getParamSchema() == null) {
            return true;
        }
        String schema = template.getParamSchema();
        if (schema == null || schema.isEmpty()) {
            return true;
        }
        try {
            cn.hutool.json.JSONArray arr = cn.hutool.json.JSONUtil.parseArray(schema);
            for (int i = 0; i < arr.size(); i++) {
                String key = arr.getStr(i);
                if (key != null && !params.containsKey(key)) {
                    log.warn("模板参数缺失: templateCode={}, missingParam={}", templateCode, key);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warn("模板参数 schema 解析失败: {}", schema, e);
            return true;
        }
    }
}
