package com.carlos.devkit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板版本历史 传输对象（Service ↔ Manager）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class TemplateVersionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 模板 ID
     */
    private Long templateId;

    /**
     * 模板组 ID
     */
    private Long groupId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 文件路径快照
     */
    private String filePath;

    /**
     * 正文快照
     */
    private String content;

    /**
     * 正文编码
     */
    private String contentEncoding;

    /**
     * 正文字节数
     */
    private Integer fileSize;

    /**
     * MD5
     */
    private String md5;

    /**
     * 变更说明
     */
    private String changeLog;

    /**
     * 保存人
     */
    private Long createBy;

    /**
     * 保存时间
     */
    private LocalDateTime createTime;
}
