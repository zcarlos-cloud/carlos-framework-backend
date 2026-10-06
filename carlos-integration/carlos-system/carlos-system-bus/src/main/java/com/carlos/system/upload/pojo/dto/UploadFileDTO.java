package com.carlos.system.upload.pojo.dto;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * <p>
 * 文件上传记录 数据传输对象，service和manager向外传输对象
 * </p>
 *
 * @author Carlos
 */
@Data
@Accessors(chain = true)
public class UploadFileDTO {

    /**
     * 文件id 非必填
     */
    private Long id;

    /**
     * 文件组id
     */
    private String groupId;
    /**
     * 文件url
     */
    private String url;
    /**
     * 文件名称
     */
    private String name;

    /**
     * 文件描述
     */
    private String desc;

    /** 文件子节数组 */
    private byte[] bytes;

    /** 文件base64 */
    private String base64;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME类型 */
    private String contentType;

    /** 文件扩展名 */
    private String fileExtension;

}
