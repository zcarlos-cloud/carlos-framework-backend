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
 * 开发工具-模板版本历史 数据源对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
@TableName("dev_template_version")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DevTemplateVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花ID）
     */
    @TableId(type = IdType.ASSIGN_ID, value = "id")
    private Long id;

    /**
     * 模板 ID
     */
    @TableField("template_id")
    private Long templateId;

    /**
     * 模板组 ID（冗余）
     */
    @TableField("group_id")
    private Long groupId;

    /**
     * 版本号
     */
    @TableField("version_no")
    private Integer versionNo;

    /**
     * 保存时的文件路径快照
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 该版本的正文快照
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
     * 变更说明
     */
    @TableField("change_log")
    private String changeLog;

    /**
     * 保存人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private Long createBy;

    /**
     * 保存时间
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
