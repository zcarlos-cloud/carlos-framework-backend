package com.carlos.fx.codege.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Set;

/**
 * <p>
 * 模板描述文件基本信息
 * </p>
 *
 * @author Carlos
 * @date 2021/11/22 18:15
 */
@Accessors(chain = true)
@Data
public class TemplateBaseInfo implements Serializable {


    /**
     * 模板的名字
     */
    private String name;

    /**
     * 模板描述
     */
    private String describe;

    /**
     * 模板路径
     */
    private String path;

    /**
     * 通用字段
     */
    private Field commonFields;

    /**
     * 逻辑删除字段
     */
    private Field logicDeleteFields;

    /**
     * 乐观锁版本字段
     */
    private Field versionFields;

    @Data
    public static class Field {

        private Set<String> name;
    }


}
