package com.carlos.devkit.pojo.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 模板文件 传输对象（Service ↔ Manager）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Data
@Accessors(chain = true)
public class TemplateFileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 所属模板组 ID
     */
    private Long groupId;

    /**
     * 组内相对路径
     */
    private String filePath;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 扩展名
     */
    private String fileExt;

    /**
     * 文件类型：1=模板 2=静态资源
     */
    private Integer fileType;

    /**
     * 是否循环渲染
     */
    private Boolean loopFlag;

    /**
     * 文件正文
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
     * 正文 MD5
     */
    private String md5;

    /**
     * 排序值
     */
    private Integer sortNum;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
