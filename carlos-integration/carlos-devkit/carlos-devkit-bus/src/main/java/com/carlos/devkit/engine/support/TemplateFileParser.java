package com.carlos.devkit.engine.support;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.engine.DevkitConstants;
import com.carlos.devkit.engine.enums.CodeDirectEnum;
import com.carlos.devkit.engine.model.RenderTemplateFile;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * 模板文件解析工具
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code TemplateUtil.dealTemplateFile}，负责识别
 * {@code [for]} 循环指令并拆分出目标文件名。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
public final class TemplateFileParser {

    private TemplateFileParser() {
    }

    /**
     * 列出目录下的模板文件对应的运行时信息（跳过非 .ftl 文件）
     *
     * @param dir 目录
     * @return 模板文件运行时信息列表
     */
    public static List<RenderTemplateFile> listTemplateFiles(final File dir) {
        List<RenderTemplateFile> list = new LinkedList<>();
        File[] files = FileUtil.ls(dir.getAbsolutePath());
        if (ArrayUtil.isEmpty(files)) {
            return list;
        }
        for (File file : files) {
            if (file.isFile()) {
                RenderTemplateFile templateFile = parse(file);
                if (templateFile != null) {
                    list.add(templateFile);
                }
            }
        }
        return list;
    }

    /**
     * 解析模板文件
     *
     * @param file 模板文件，如 {@code [base][for].java.ftl}
     * @return 模板运行时信息；非模板文件返回 {@code null}
     */
    public static RenderTemplateFile parse(final File file) {
        if (!FileUtil.pathEndsWith(file, DevkitConstants.TEMPLATE_FILE_SUFFIX)) {
            log.debug("非模板文件, 忽略处理: {}", file.getPath());
            return null;
        }
        RenderTemplateFile templateFile = new RenderTemplateFile();
        templateFile.setFile(file);
        templateFile.setName(FileNameUtil.getName(file));
        templateFile.setPath(file.getParent());
        String preName = FileNameUtil.mainName(file);
        if (StrUtil.contains(preName, CodeDirectEnum.FOR.getValue())) {
            templateFile.setLoop(true);
            preName = StrUtil.removeAll(preName, CodeDirectEnum.FOR.getValue());
        }
        templateFile.setPreName(preName);
        return templateFile;
    }
}
