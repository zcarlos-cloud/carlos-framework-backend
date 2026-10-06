package com.carlos.fx.codege.config;

import java.io.File;

/**
 * <p>
 * 代码生成器常量
 * </p>
 *
 * @author Carlos
 * @date 2021/11/22 16:31
 */
public interface CodegeConstant {

    /**
     * 删除字段名称
     */
    String FTL_PARAM_KEY_PROJECT = "project";
    String FTL_PARAM_KEY_TABLE = "table";
    String FTL_PARAM_KEY_DATABASE = "database";

    /** 临时目录 */
    String TEMP_DIR = System.getProperty("java.io.tmpdir") + "/codege_temp";


    /**
     * 数据库boolean类型字段前缀
     */
    String BOOLEAN_PREFIX = "is_";

    /**
     * 模板跟目录
     */
    String TEMPLATE_ROOT_PATH = "templates";
    String RESOURCE_ROOT_PATH = "classpath*:**/*";

    /**
     * 模板描述文件文件名
     */
    String TEMPLATE_DESCRIBE_FILE_NAME = "template.xml";

    /**
     * java文件后缀
     */
    String JAVA_FILE_EXT = ".java";

    /**
     * 模板文件后缀
     */
    String TEMPLATE_FILE_EXT = ".ftl";

    /**
     * 某个模板文件的根目录
     */
    String TEMPLATE_MAIN = "template";

    /**
     * 源码相对工程根目录的路径
     */
    String SRC_MAIN_JAVA = File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator;

    /**
     * 模板零时存储文件夹
     */
    String SRC_MAIN_TEMP = File.separator + "src" + File.separator + "main" + File.separator + "temp" + File.separator;
}
