package com.carlos.system.upload.pojo.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * <p>
 * 文件信息 显示层对象，向页面传输的对象
 * </p>
 *
 * @author Carlos
 * @date 2022-1-27 13:53:35
 */
@Data
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class FileInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;
    @Schema(description = "文件id")
    private Long id;
    @Schema(description = "文件组id")
    private String groupId;
    @Schema(description = "文件名")
    private String name;
    @Schema(description = "文件地址")
    private String url;
    @Schema(description = "文件base64")
    private String base64;
    @Schema(description = "文件大小（字节）")
    private Long fileSize;
    @Schema(description = "MIME类型")
    private String contentType;
    @Schema(description = "文件扩展名")
    private String fileExtension;
}
