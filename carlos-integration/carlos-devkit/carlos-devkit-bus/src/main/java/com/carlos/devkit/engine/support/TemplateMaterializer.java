package com.carlos.devkit.engine.support;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.engine.DevkitConstants;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.enums.DevkitErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * <p>
 * 模板物化器
 * </p>
 * <p>
 * 把数据库中的模板文件还原成磁盘上的目录树，供引擎按原有「遍历目录 + 渲染」逻辑消费。
 * 取代 carlos-tools 的 {@code ResourceUtil.copyResourceFile2Temp}（原逻辑从 classpath 解包）。
 * </p>
 * <p>
 * 路径一律经 {@link TemplatePathValidator} 校验，防止用户编辑的模板路径穿越出沙箱。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
public class TemplateMaterializer {

    /**
     * 将模板文件物化到沙箱目录
     *
     * @param groupCode    模板组编码（仅用于日志）
     * @param templateRoot 模板根目录名，默认 {@code template}
     * @param sourceFiles  模板文件清单
     * @param workDir      本次请求的沙箱目录
     * @return 物化后的模板根目录（{@code workDir/group/template}）
     */
    public File materialize(final String groupCode, final String templateRoot,
                            final List<TemplateSourceFile> sourceFiles, final Path workDir) {
        String rootName = StrUtil.isBlank(templateRoot) ? DevkitConstants.TEMPLATE_MAIN : templateRoot;
        Path groupDir = workDir.resolve("group").resolve(rootName).normalize();
        try {
            Files.createDirectories(groupDir);
        } catch (Exception e) {
            log.error("模板物化目录创建失败: {}", groupDir, e);
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception("模板沙箱创建失败");
        }

        int count = 0;
        for (TemplateSourceFile sourceFile : sourceFiles) {
            Path target;
            try {
                target = TemplatePathValidator.resolveInside(groupDir, sourceFile.getPath());
            } catch (IllegalArgumentException e) {
                throw DevkitErrorCode.TEMPLATE_PATH_INVALID.exception(e.getMessage());
            }
            try {
                Path parent = target.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                byte[] bytes = sourceFile.isText()
                    ? sourceFile.getContent().getBytes(StandardCharsets.UTF_8)
                    : Base64.getDecoder().decode(sourceFile.getContent());
                Files.write(target, bytes);
                count++;
            } catch (Exception e) {
                log.error("模板文件物化失败: {}/{}", groupCode, sourceFile.getPath(), e);
                throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception(
                    "模板文件物化失败：" + sourceFile.getPath());
            }
        }
        log.debug("模板组[{}]物化完成，共 {} 个文件 -> {}", groupCode, count, groupDir);
        File templateDir = groupDir.toFile();
        if (!FileUtil.exist(templateDir)) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception("模板组[" + groupCode + "]下没有任何模板文件");
        }
        return templateDir;
    }
}
