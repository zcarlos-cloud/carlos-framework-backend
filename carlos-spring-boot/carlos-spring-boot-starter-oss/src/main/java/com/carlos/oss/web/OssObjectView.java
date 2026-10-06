package com.carlos.oss.web;

import cn.hutool.extra.servlet.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.View;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * <p>
 * OSS 对象视图
 * 将 OSS 对象以流的方式写到前端（用于文件下载）
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
public class OssObjectView implements View {

    private final InputStream inputStream;
    private final String attachmentName;

    public OssObjectView(InputStream inputStream, String attachmentName) {
        this.inputStream = inputStream;
        this.attachmentName = attachmentName;
    }

    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
        try (InputStream is = inputStream) {
            JakartaServletUtil.write(response, is, MediaType.APPLICATION_OCTET_STREAM_VALUE, attachmentName);
        } catch (IOException e) {
            throw new RuntimeException("Failed to render OSS object", e);
        }
    }
}
