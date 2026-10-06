package com.carlos.devkit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板组 传输对象（Service ↔ Manager）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class TemplateGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板组编码
     */
    private String groupCode;

    /**
     * 模板组显示名
     */
    private String groupName;

    /**
     * 描述
     */
    private String description;

    /**
     * 模板类型：1=代码生成 2=工程生成
     */
    private Integer templateType;

    /**
     * 技术栈编码
     */
    private String techStack;

    /**
     * 适用数据库
     */
    private String dbType;

    /**
     * 模板根目录名
     */
    private String templateRoot;

    /**
     * 逻辑删除字段清单（逗号分隔）
     */
    private String logicDeleteFields;

    /**
     * 乐观锁版本字段清单（逗号分隔）
     */
    private String versionFields;

    /**
     * 通用字段清单（逗号分隔）
     */
    private String commonFields;

    /**
     * 是否内置模板
     */
    private Boolean builtIn;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 排序值
     */
    private Integer sortNum;

    /**
     * 备注
     */
    private String remark;

    /**
     * 模板文件数（列表回填）
     */
    private Integer fileCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
