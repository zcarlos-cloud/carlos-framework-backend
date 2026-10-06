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
 * 开发工具-生成记录 数据源对象（流水，只增不改）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@TableName("dev_generate_record")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevGenerateRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 业务类型：1=代码生成 2=工程生成
     */
    @TableField("biz_type")
    private Integer bizType;

    /**
     * 模板组 ID
     */
    @TableField("template_group_id")
    private Long templateGroupId;

    /**
     * 模板组编码快照
     */
    @TableField("template_group_code")
    private String templateGroupCode;

    /**
     * 模板组名称快照
     */
    @TableField("template_group_name")
    private String templateGroupName;

    /**
     * 数据源 ID
     */
    @TableField("ds_id")
    private Long dsId;

    /**
     * 数据源名称快照
     */
    @TableField("ds_name")
    private String dsName;

    /**
     * 项目名
     */
    @TableField("project_name")
    private String projectName;

    /**
     * 包名
     */
    @TableField("package_name")
    private String packageName;

    /**
     * 生成参数 JSON 快照
     */
    @TableField("gen_params")
    private String genParams;

    /**
     * 产物文件数
     */
    @TableField("file_count")
    private Integer fileCount;

    /**
     * 产物总字节数
     */
    @TableField("total_size")
    private Long totalSize;

    /**
     * 生成耗时（毫秒）
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 状态：1=成功 0=失败
     */
    @TableField("status")
    private Integer status;

    /**
     * 失败原因
     */
    @TableField("error_msg")
    private String errorMsg;

    /**
     * 产物 OSS 对象键（预留）
     */
    @TableField("oss_object_key")
    private String ossObjectKey;

    /**
     * 操作人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 生成时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
