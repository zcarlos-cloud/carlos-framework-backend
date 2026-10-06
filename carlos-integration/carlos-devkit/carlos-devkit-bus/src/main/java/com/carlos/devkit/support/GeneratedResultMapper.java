package com.carlos.devkit.support;

import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.pojo.vo.GeneratedFileVO;
import com.carlos.devkit.pojo.vo.GeneratedResultVO;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 * 生成产物 视图映射器
 * </p>
 * <p>
 * 二进制文件只下发大小；文本文件下发内容并做长度截断，避免超大 payload。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public final class GeneratedResultMapper {

    /**
     * 预览时单个文本文件下发的最大字符数
     */
    private static final int PREVIEW_MAX_CHARS = 200_000;

    /**
     * 截断提示
     */
    private static final String TRUNCATED_HINT = "\n... (内容过长，已截断，请下载完整产物)";

    private GeneratedResultMapper() {
    }

    /**
     * 产物 → VO
     *
     * @param result 产物
     * @return 产物 VO
     */
    public static GeneratedResultVO toVO(final GeneratedResult result) {
        List<GeneratedFileVO> files = result.getFiles().stream().map(GeneratedResultMapper::toFileVO).toList();
        return new GeneratedResultVO()
            .setRootName(result.getRootName())
            .setFileCount(files.size())
            .setTotalSize(result.getTotalSize())
            .setDurationMs(result.getDurationMs())
            .setFiles(files);
    }

    /**
     * 产物文件 → VO
     *
     * @param file 产物文件
     * @return 文件 VO
     */
    private static GeneratedFileVO toFileVO(final GeneratedFile file) {
        GeneratedFileVO vo = new GeneratedFileVO()
            .setPath(file.getPath())
            .setText(file.isText())
            .setSize(file.getSize());
        if (file.isText()) {
            String content = new String(file.getContent(), StandardCharsets.UTF_8);
            vo.setContent(content.length() > PREVIEW_MAX_CHARS
                ? content.substring(0, PREVIEW_MAX_CHARS) + TRUNCATED_HINT
                : content);
        }
        return vo;
    }
}
