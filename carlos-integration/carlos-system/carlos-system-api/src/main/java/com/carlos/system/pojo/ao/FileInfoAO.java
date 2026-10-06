package com.carlos.system.pojo.ao;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 文件基本信息
 *
 * @author Carlos
 * @date 2021-12-15 17:40:44
 */
@Data
@Accessors(chain = true)
public class FileInfoAO {

    /**
     * 主键
     */
    private Long id;
    /**
     * 文件分组
     */
    private String groupId;
    /**
     * 文件名称
     */
    private String name;
    /**
     * 文件url
     */
    private String url;
    /**
     * 文件流
     */
    private byte[] bytes;
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    /**
     * MIME类型
     */
    private String contentType;
    /**
     * 文件扩展名
     */
    private String fileExtension;
}
