package com.carlos.devkit.engine.projectgen;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.DevkitConstants;
import com.carlos.devkit.engine.enums.ProjectDirectEnum;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.render.FreemarkerRenderer;
import com.carlos.devkit.engine.support.NameUtil;
import com.carlos.devkit.engine.support.OutputCollector;
import com.carlos.devkit.engine.support.TemplateMaterializer;
import com.carlos.devkit.enums.DevkitErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 工程生成引擎
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code ProjectGeneratorService}：递归遍历模板目录，
 * 按 {@link ProjectDirectEnum} 替换路径占位符，{@code .ftl} 渲染、其余拷贝。
 * 差异：
 * <ul>
 *   <li>模板来源为数据库，先物化到沙箱；</li>
 *   <li>渲染引擎由 Hutool TemplateEngine 统一为本模块的 {@link FreemarkerRenderer}
 *       （Hutool 默认实现即 FreeMarker，渲染行为一致，且顺带修掉了原实现在 jar 内
 *       {@code ResourceUtil.getResource} 取不到模板的缺陷）；</li>
 *   <li>产物收集进内存返回，不写业务磁盘。</li>
 * </ul>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectGenEngine {

    private final DevkitProperties devkitProperties;

    private final TemplateMaterializer templateMaterializer;

    private final FreemarkerRenderer freemarkerRenderer;

    /**
     * 执行工程生成
     *
     * @param groupMeta    模板组元数据
     * @param templateRoot 模板根目录名
     * @param templates    模板源文件（来自数据库）
     * @param param        生成参数
     * @param workDir      本次请求的沙箱目录
     * @return 生成产物
     */
    public GeneratedResult generate(final TemplateGroupMeta groupMeta, final String templateRoot,
                                    final List<TemplateSourceFile> templates, final ProjectGenParam param,
                                    final Path workDir) {
        long start = System.currentTimeMillis();
        try {
            String artifactId = StrUtil.blankToDefault(param.getArtifactId(), "demo");
            String underlineName = StrUtil.replace(artifactId, StrUtil.DASHED, StrUtil.UNDERLINE);
            param.setUnderlineName(underlineName);
            param.setCamelName(NameUtil.getCamelName(underlineName, false));
            if (StrUtil.isBlank(param.getProjectName())) {
                param.setProjectName(artifactId);
            }
            // 包名缺省由 groupId 还原，避免模板里 ${project.packageName} 求值为 null
            if (StrUtil.isBlank(param.getPackageName())) {
                param.setPackageName(StrUtil.join(StrUtil.DOT, param.getPackageNameItems()));
            }

            File templateDir = templateMaterializer.materialize(
                groupMeta.getCode(), templateRoot, templates, workDir);
            File outDir = workDir.resolve(DevkitConstants.OUTPUT_DIR).toFile();
            FileUtil.mkdir(outDir);
            param.setOutputPath(outDir.getAbsolutePath());

            String packageNames = StrUtil.join(File.separator, param.getPackageNameItems());
            generateDir(param, templateDir, packageNames, templateDir.getAbsolutePath());

            List<GeneratedFile> files = OutputCollector.collect(
                outDir, devkitProperties.getMaxFileCount(), devkitProperties.getMaxOutputSize());
            long totalSize = files.stream().mapToLong(GeneratedFile::getSize).sum();
            long duration = System.currentTimeMillis() - start;
            log.info("工程生成完成：文件 {} 个，共 {} 字节，耗时 {}ms", files.size(), totalSize, duration);
            return new GeneratedResult(param.getProjectName(), files, totalSize, duration);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("工程生成失败", e);
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception(e.getMessage());
        }
    }

    /**
     * 递归遍历模板目录生成工程文件
     *
     * @param param              生成参数
     * @param templateDir        当前模板目录
     * @param packageNames       包名目录片段
     * @param templateRootPath   模板根目录绝对路径（用于计算相对输出路径）
     */
    private void generateDir(final ProjectGenParam param, final File templateDir,
                             final String packageNames, final String templateRootPath) {
        File[] files = FileUtil.ls(templateDir.getAbsolutePath());
        if (files == null) {
            log.warn("目录为空:{}", templateDir.getPath());
            return;
        }
        for (File file : files) {
            String path = resolveTargetPath(file.getAbsolutePath(), templateRootPath, param);
            if (file.isDirectory()) {
                FileUtil.mkdir(path);
                generateDir(param, file, packageNames, templateRootPath);
                continue;
            }
            if (!file.isFile()) {
                continue;
            }
            String extName = FileUtil.extName(file);
            if (!DevkitConstants.TEMPLATE_FILE_EXT.equals(extName)) {
                FileUtil.copyFile(file, new File(path));
                continue;
            }
            File target = new File(StrUtil.removeSuffix(path, DevkitConstants.TEMPLATE_FILE_SUFFIX));
            Map<String, Object> params = new HashMap<>(2);
            params.put(DevkitConstants.FTL_PARAM_KEY_PROJECT, param);
            freemarkerRenderer.renderToFile(file, params, target);
        }
    }

    /**
     * 计算模板文件对应的目标路径（替换全部路径占位符）
     *
     * @param absolutePath     模板文件绝对路径
     * @param templateRootPath 模板根目录绝对路径
     * @param param            生成参数
     * @return 目标路径
     */
    private String resolveTargetPath(final String absolutePath, final String templateRootPath,
                                     final ProjectGenParam param) {
        String path = absolutePath.replace(templateRootPath, param.getOutputPath());
        for (ProjectDirectEnum directEnum : ProjectDirectEnum.values()) {
            path = switch (directEnum) {
                case ARTIFACT_ID -> path.replace(directEnum.getValue(), param.getArtifactId());
                case PACKAGE -> path.replace(directEnum.getValue(),
                    StrUtil.join(File.separator, param.getPackageNameItems()));
                case CAMEL_NAME -> path.replace(directEnum.getValue(), param.getCamelName());
                case UNDERLINE_NAME -> path.replace(directEnum.getValue(), param.getUnderlineName());
            };
        }
        return path;
    }
}
