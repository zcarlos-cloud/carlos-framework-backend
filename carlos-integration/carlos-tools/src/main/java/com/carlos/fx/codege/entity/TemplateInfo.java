package com.carlos.fx.codege.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.File;
import java.io.Serializable;

/**
 * <p>
 * ftl模板文件信息
 * </p>
 *
 * @author Carlos
 * @date 2021/11/22 18:15
 */
@Accessors(chain = true)
@Data
public class TemplateInfo implements Serializable {

    /**
     * 模板文件
     */
    private File file;

    /**
     * 模板的名字
     */
    private String name;

    /**
     * 模板路径
     */
    private String path;

    /**
     * 处理前的名字
     */
    private String preName;

    /**
     * 目标文件名字
     */
    private String targetName;

    /**
     * 是否循环生成文件
     */
    private boolean loop;

}
