package com.carlos.ai.service.impl;

import com.carlos.ai.config.CarlosAiProperties;
import com.carlos.ai.enums.AiErrorCode;
import com.carlos.ai.exception.AiException;
import com.carlos.ai.service.AiDocumentService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * AI 文档解析服务实现
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class AiDocumentServiceImpl implements AiDocumentService {

    private final CarlosAiProperties properties;

    @Override
    public String parseDocument(InputStream inputStream, String fileName) {
        if (inputStream == null) {
            throw AiErrorCode.AI_DOCUMENT_PARSE_ERROR.exception("文档流不能为空");
        }
        if (fileName == null || fileName.isBlank()) {
            throw AiErrorCode.AI_DOCUMENT_PARSE_ERROR.exception("文件名不能为空");
        }
        try {
            long maxSize = properties.getDocument().getMaxSizeBytes();
            byte[] bytes = readWithLimit(inputStream, maxSize);

            String lowerName = fileName.toLowerCase();
            DocumentParser parser;
            if (lowerName.endsWith(".pdf")) {
                parser = new ApachePdfBoxDocumentParser();
            } else {
                throw AiErrorCode.AI_DOCUMENT_PARSE_ERROR.exception("不支持的文档格式: " + fileName);
            }
            Document document = parser.parse(new ByteArrayInputStream(bytes));
            return document.text();
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("文档解析失败, fileName={}", fileName, e);
            throw new AiException("文档解析失败", e);
        }
    }

    /**
     * 读取输入流并限制最大字节数，防止大文件导致内存耗尽
     *
     * @param inputStream 输入流
     * @param maxSize     最大允许字节数
     * @return 字节数组
     * @throws IOException IO 异常
     */
    private byte[] readWithLimit(InputStream inputStream, long maxSize) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            total += read;
            if (total > maxSize) {
                throw AiErrorCode.AI_DOCUMENT_PARSE_ERROR.exception(
                    "文档大小超过限制: " + maxSize + " 字节");
            }
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

}
