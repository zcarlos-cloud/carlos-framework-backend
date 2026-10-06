package com.carlos.devkit.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 开发工具-模板组 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@TableName("dev_template_group")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevTemplateGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 模板组编码（全局唯一）
     */
    @TableField("group_code")
    private String groupCode;

    /**
     * 模板组显示名
     */
    @TableField("group_name")
    private String groupName;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 模板类型：1=代码生成 2=工程生成
     */
    @TableField("template_type")
    private Integer templateType;

    /**
     * 技术栈编码
     */
    @TableField("tech_stack")
    private String techStack;

    /**
     * 适用数据库
     */
    @TableField("db_type")
    private String dbType;

    /**
     * 物化时模板根目录名
     */
    @TableField("template_root")
    private String templateRoot;

    /**
     * 逻辑删除字段名清单（逗号分隔）
     */
    @TableField("logic_delete_fields")
    private String logicDeleteFields;

    /**
     * 乐观锁版本字段名清单（逗号分隔）
     */
    @TableField("version_fields")
    private String versionFields;

    /**
     * 通用字段名清单（逗号分隔）
     */
    @TableField("common_fields")
    private String commonFields;

    /**
     * 是否内置模板
     */
    @TableField("built_in")
    private Boolean builtIn;

    /**
     * 状态：1=启用 0=停用
     */
    @TableField("status")
    private Integer status;

    /**
     * 排序值
     */
    @TableField("sort_num")
    private Integer sortNum;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 修改人
     */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private Long updateBy;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
