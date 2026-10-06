package com.carlos.ai.service;

import java.io.InputStream;

/**
 * <p>
 * AI 文档解析服务接口
 * </p>
 *
 * <p>
 * 提供 PDF 等文档格式的内容提取能力。
 * </p>
 *
 * @author carlos
 * @since 3.0.0
 */
public interface AiDocumentService {

    /**
     * 解析文档内容
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于识别格式）
     * @return 文档文本内容
     */
    String parseDocument(InputStream inputStream, String fileName);

}
