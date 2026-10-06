package com.carlos.test.controller;


import cn.hutool.core.io.FileUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.InputStream;

/**
 * <p>
 * 文件上传相关接口
 * </p>
 *
 * @author Carlos
 * @date 2021-12-8 16:13:43
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
@Tag(name = "文件测试")
public class FileController {

    @Operation(summary = "获取二进制文件流")
    @GetMapping("load")
    public void download(String id, HttpServletResponse response) {
        if (id == null) {
            return;
        }

        File file = new File("D:\\云津工作文件\\研发部管理办法集成\\技术文档\\民意速办后端代码走查报告.pdf");


        InputStream stream = FileUtil.getInputStream(file);
        // response.setCharacterEncoding(StandardCharsets.UTF_8.name
        // ());
        if (stream == null) {
            JakartaServletUtil.write(response, JSONUtil.toJsonStr(Result.error("文件读取失败")), ContentType.JSON.getValue());
            return;
        }

        String fileName = file.getName();
        // 获取文件content-type
        String mimeType = FileUtil.getMimeType(fileName);
        // MediaType mediaType = MediaType.parseMediaType(mimeType);
        try {
            JakartaServletUtil.write(response, stream, mimeType, fileName);
        } catch (Exception e) {
            log.error("文件下载失败：{}", fileName, e);
        }
    }

}
