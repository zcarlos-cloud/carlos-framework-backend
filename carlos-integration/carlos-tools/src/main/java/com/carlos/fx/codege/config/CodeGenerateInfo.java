package com.carlos.fx.codege.config;

import com.carlos.fx.codege.entity.TableBaseInfo;
import com.carlos.fx.codege.entity.TemplateBaseInfo;
import com.carlos.fx.codege.enums.FieldNameTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 项目相关属性
 * </p>
 *
 * @author Carlos
 * @date 2021/10/28 13:58
 */
@Accessors(chain = true)
@Data
public class CodeGenerateInfo {

    /**
     * 项目名称
     */
    private String projectName;
    /**
     * groupId  com.xxxx
     */
    private String packageName;
    /**
     * groupId 拆分 com.xxxx
     */
    private List<String> packageNameItems;
    /**
     * 生成代码保存路径
     */
    private String outputPath;
    /**
     * 生成代码保存路径
     */
    private String author;

    /** 是否使用表名前缀 */
    private boolean useTablePrefix = true;


    /**
     * 模板信息
     *
     * @since 3.0
     */
    private TemplateBaseInfo templateBaseInfo;

    /**
     * 表名列表
     *
     * @since 3.0
     */
    private List<TableBaseInfo> tables;

    /**
     * 数据库字段命名方式
     */
    private FieldNameTypeEnum nameType = FieldNameTypeEnum.NOT_PREFIX_AND_CAMEL;
}
