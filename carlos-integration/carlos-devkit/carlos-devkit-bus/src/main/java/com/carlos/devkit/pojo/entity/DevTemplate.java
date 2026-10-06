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
 * 开发工具-模板文件 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@TableName("dev_template")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevTemplate implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 所属模板组 ID
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 组内相对路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 扩展名
     */
    @TableField("file_ext")
    private String fileExt;

    /**
     * 文件类型：1=模板(.ftl) 2=静态资源
     */
    @TableField("file_type")
    private Integer fileType;

    /**
     * 是否循环渲染（文件名含 [for]）
     */
    @TableField("loop_flag")
    private Boolean loopFlag;

    /**
     * 文件正文
     */
    @TableField("content")
    private String content;

    /**
     * 正文编码：TEXT / BASE64
     */
    @TableField("content_encoding")
    private String contentEncoding;

    /**
     * 正文字节数
     */
    @TableField("file_size")
    private Integer fileSize;

    /**
     * 正文 MD5
     */
    @TableField("md5")
    private String md5;

    /**
     * 同层级排序值
     */
    @TableField("sort_num")
    private Integer sortNum;

    /**
     * 状态：1=启用 0=停用
     */
    @TableField("status")
    private Integer status;

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
