package com.carlos.devkit.engine.codegen;

import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.pojo.enums.FieldNameTypeEnum;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 * 代码生成参数（FreeMarker 渲染上下文中的 project 对象）
 * </p>
 * <p>
 * 字段名与 carlos-tools 原 {@code CodeGenerateInfo} 保持一致，
 * 现有模板里的 {@code ${project.packageName}} / {@code ${project.author}} 无需改动。
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Accessors(chain = true)
@Data
public class CodeGenParam {

    /**
     * 项目名（产物根目录名）
     */
    private String projectName;

    /**
     * 包名（如 com.carlos.demo）
     */
    private String packageName;

    /**
     * 包名拆分
     */
    private List<String> packageNameItems;

    /**
     * 输出路径（沙箱内，仅内部使用）
     */
    private String outputPath;

    /**
     * 作者（写入产物注释）
     */
    private String author;

    /**
     * 是否保留表名前缀
     */
    private boolean useTablePrefix = true;

    /**
     * 字段命名方式
     */
    private FieldNameTypeEnum nameType = FieldNameTypeEnum.NOT_PREFIX_AND_CAMEL;

    /**
     * 选中的数据表
     */
    private List<TableBaseInfo> tables;

    /**
     * 模板组元数据
     */
    private TemplateGroupMeta templateGroup;
}
