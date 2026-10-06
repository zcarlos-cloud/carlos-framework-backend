package com.carlos.fx.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.fx.codege.config.CodegeConstant;
import com.carlos.fx.codege.entity.TemplateBaseInfo;
import com.carlos.fx.codege.entity.TemplateInfo;
import com.carlos.fx.codege.enums.CodeDirectEnum;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 模板相关功能
 * </p>
 *
 * @author Carlos
 * @date 2020/9/5 10:15
 */
@Slf4j
public class TemplateUtil {

    /**
     * 模板配置类
     */
    private final static Configuration FREEMARKER_CONFIG =
            new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);

    /**
     * 获取所有的模板的基本信息
     *
     * @return java.util.Map<java.lang.String, java.lang.String>
     * @author Carlos
     * @date 2020/9/5 10:19
     */
    public static List<TemplateBaseInfo> getTemplatesBaseInfo(File templateRootPath) {
        File[] templates = templateRootPath.listFiles();
        List<TemplateBaseInfo> list = new LinkedList<>();
        if (ArrayUtil.isEmpty(templates)) {
            log.warn("模板为空");
            return list;
        }

        // 便利所有的模板
        for (File templatePath : templates) {
            File templateDescFile = FileUtil.file(templatePath, CodegeConstant.TEMPLATE_DESCRIBE_FILE_NAME);
            if (!FileUtil.exist(templateDescFile)) {
                log.error("目录[{}]不存在描述文件[{}]", templatePath.getPath(), CodegeConstant.TEMPLATE_DESCRIBE_FILE_NAME);
                continue;
            }
            TemplateBaseInfo templateBaseInfo = XmlUtils.readTemplateBaseInfo(templateDescFile, TemplateBaseInfo.class);
            templateBaseInfo.setPath(templateDescFile.getParent());
            list.add(templateBaseInfo);
        }

        log.debug("模板信息加载成功");
        return list;
    }

    /**
     * 处理模板文件
     *
     * @param file 模板文件   如 xxx.java.ftl 文件
     * @return 模板的基本信息
     * @author Carlos
     * @date 2020/9/7 17:23
     */
    public static TemplateInfo dealTemplateFile(File file) {
        // 处理非模板文件 非模板文件不用处理
        if (!FileUtil.pathEndsWith(file, CodegeConstant.TEMPLATE_FILE_EXT)) {
            log.debug("非模板文件, 忽略处理:" + file.getPath());
            return null;
        }

        TemplateInfo templateInfo = new TemplateInfo();
        templateInfo.setFile(file);
        templateInfo.setName(FileNameUtil.getName(file));
        templateInfo.setPath(file.getParent());
        // 文件处理前的名字
        String preName = FileNameUtil.mainName(file);
        // 判断文件名中是否还有循环生成指令
        if (StrUtil.contains(preName, CodeDirectEnum.FOR.getValue())) {
            templateInfo.setLoop(true);
            preName = StrUtil.removeAll(preName, CodeDirectEnum.FOR.getValue());
        }
        templateInfo.setPreName(preName);
        return templateInfo;
    }

    /**
     * 根据模板生成对应的文件
     *
     * @param params       模板中对应的参数信息
     * @param templateName 模板名称
     * @param filePath     模板所在的路径
     * @param targetName   文件的名字
     * @author Carlos
     * @date 2020/9/8 0:09
     */
    public static void createClassFile(Map<String, Object> params, String templateName, String filePath,
                                       String targetName) {
        if (params == null) {
            params = new HashMap<>(1);
        }
        // 加载模板
        Template template;
        try {
            FREEMARKER_CONFIG.setDirectoryForTemplateLoading(new File(filePath));
            template = FREEMARKER_CONFIG.getTemplate(templateName);
            String targetFile = filePath + File.separator + targetName;
            Writer out = new FileWriter(targetFile);
            template.process(params, out);
            log.info("{}创建成功", targetFile);
        } catch (Exception e) {
            log.error("文件生成失败: 路径:{}||{}->{}", filePath, templateName, targetName, e);

        }

    }

    public static void createClassFile(Map<String, Object> params, TemplateInfo templateInfo) {
        createClassFile(params, templateInfo.getName(), templateInfo.getPath(), templateInfo.getTargetName());
    }

}
