package com.carlos.devkit.engine;

import java.io.File;

/**
 * <p>
 * 开发工具平台引擎常量
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface DevkitConstants {

    /**
     * FreeMarker 渲染上下文：工程/代码生成参数
     */
    String FTL_PARAM_KEY_PROJECT = "project";

    /**
     * FreeMarker 渲染上下文：当前表元数据
     */
    String FTL_PARAM_KEY_TABLE = "table";

    /**
     * FreeMarker 渲染上下文：目标数据库信息
     */
    String FTL_PARAM_KEY_DATABASE = "database";

    /**
     * 数据库 boolean 类型字段前缀
     */
    String BOOLEAN_PREFIX = "is_";

    /**
     * 模板文件后缀
     */
    String TEMPLATE_FILE_EXT = "ftl";

    /**
     * 模板文件后缀（含点）
     */
    String TEMPLATE_FILE_SUFFIX = ".ftl";

    /**
     * 模板根目录名（物化后模板文件所在目录）
     */
    String TEMPLATE_MAIN = "template";

    /**
     * 源码相对工程根目录的路径
     */
    String SRC_MAIN_JAVA = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;

    /**
     * 包路径注入用的临时源码目录
     */
    String SRC_MAIN_TEMP = File.separator + "src" + File.separator + "main" + File.separator + "temp" + File.separator;

    /**
     * 数据库类型 → Java 类型映射文件（classpath 资源）
     */
    String TYPE_CONVERTER_RESOURCE = "devkit/typeConverter.xml";

    /**
     * 生成产物根目录在临时沙箱中的相对路径
     */
    String OUTPUT_DIR = "out";

    /**
     * 模板物化根目录在临时沙箱中的相对路径
     */
    String TEMPLATE_WORK_DIR = "templates";
}
