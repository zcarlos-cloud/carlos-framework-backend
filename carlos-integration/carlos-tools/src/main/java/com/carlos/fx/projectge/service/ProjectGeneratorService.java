package com.carlos.fx.projectge.service;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateConfig.ResourceMode;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;
import cn.hutool.json.JSONUtil;
import com.carlos.fx.codege.entity.TemplateBaseInfo;
import com.carlos.fx.exception.GeneratorException;
import com.carlos.fx.projectge.config.ProjectGeConstant;
import com.carlos.fx.projectge.entity.ProjectInfo;
import com.carlos.fx.projectge.enums.ProjectDirectEnum;
import com.carlos.fx.utils.NameUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 项目生成主业务
 * </p>
 *
 * @author Carlos
 * @date 2019/10/19 23:45 ---------     -------------   --------------------------------------
 */
@Slf4j
@Service
@Scope("prototype")
public class ProjectGeneratorService {

    public void createObject(ProjectInfo projectInfo) {
        log.info("项目生成中,项目信息JSON:{}", JSONUtil.toJsonPrettyStr(projectInfo));
        String artifactId = projectInfo.getArtifactId();
        String name = StrUtil.replace(artifactId, StrUtil.DASHED, StrUtil.UNDERLINE);
        String camelName = NameUtil.getCamelName(name, false);
        projectInfo.setCamelName(camelName);
        projectInfo.setUnderlineName(name);
        try {
            TemplateBaseInfo selectTemplate = projectInfo.getSelectTemplate();
            File templatePath = new File(selectTemplate.getPath() + File.separator + ProjectGeConstant.TEMPLATE_MAIN);
            List<String> packageNameItems = projectInfo.getPackageNameItems();
            String packageNames = StrUtil.join(File.separator, packageNameItems);
            // 遍历目录及目录名
            generateDir(projectInfo, templatePath, packageNames);
        } catch (Exception e) {
            log.error("生成失败！", e);
            throw new GeneratorException(e);
        }
        log.info("项目生成完成.........");
    }

    /**
     * createFile
     *
     * @param templateDir      模板中的文件目录
     * @param packageNames         groupId所组成的目录
     * @author Carlos
     * @date 2023/7/2 21:07
     */
    private void generateDir(ProjectInfo projectInfo, File templateDir, String packageNames) {
        String templatePath = templateDir.getAbsolutePath();
        File[] files = FileUtil.ls(templatePath);
        if (files == null) {
            log.warn("目录为空:{}", templateDir.getPath());
            return;
        }

        String templateRootPath = projectInfo.getSelectTemplate().getPath() + File.separator + ProjectGeConstant.TEMPLATE_MAIN;
        ProjectDirectEnum[] directEnums = ProjectDirectEnum.values();
        for (File file : files) {
            // 在对应的目标文件夹下创建目录
            String absolutePath = file.getAbsolutePath();
            String path = absolutePath.replace(templateRootPath, projectInfo.getOutputPath());
            for (ProjectDirectEnum directEnum : directEnums) {
                path = switch (directEnum) {
                    case ARTIFACT_ID -> path.replace(directEnum.getValue(), projectInfo.getArtifactId());
                    case PACKAGE -> path.replace(directEnum.getValue(), packageNames);
                    case CAMEL_NAME -> path.replace(directEnum.getValue(), projectInfo.getCamelName());
                    case UNDERLINE_NAME -> path.replace(directEnum.getValue(), projectInfo.getUnderlineName());
                    default -> throw new IllegalStateException("Unexpected value: " + directEnum);
                };
            }


            if (FileUtil.isDirectory(file)) {
                FileUtil.mkdir(path);
                if (file.isDirectory()) {
                    log.info("进入目录:{}", file.getPath());
                    // 递归目录
                    generateDir(projectInfo, file, packageNames);
                }
                log.info("创建目录：{}", path);
                continue;
            }
            if (FileUtil.isFile(file)) {
                log.info("开始处理文件:{}", file.getPath());
                String extName = FileUtil.extName(file);
                if (!extName.equals(ProjectGeConstant.TEMPLATE_FILE_EXT)) {
                    FileUtil.copyFile(file, new File(path));
                    log.info("复制文件：{}  ->  {}", file.getPath(), path);
                } else {
                    path = path.replace(".ftl", StrUtil.EMPTY);
                    try {
                        // 加载目录下的模板文件
                        TemplateConfig templateConfig = new TemplateConfig(file.getParent(), ResourceMode.FILE);
                        TemplateEngine engine = TemplateUtil.createEngine(templateConfig);
                        Template template = engine.getTemplate(FileUtil.getName(file));

                        Map<String, Object> params = new HashMap<>();
                        params.put(ProjectGeConstant.FTL_PARAM_KEY_PROJECT, projectInfo);
                        template.render(params, new File(path));
                        log.info("生成文件：{}  ->  {}", file.getPath(), path);
                    } catch (Exception e) {
                        log.error("文件生成失败: 路径:{}", file.getPath(), e);
                    }
                }
            }
        }
    }


}
