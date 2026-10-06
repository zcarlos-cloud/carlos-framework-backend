package com.carlos.system.upload.pojo.param;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


/**
 * <p>
 * 文件上传参数
 * </p>
 *
 * @author Carlos
 * @date 2021-11-10 10:45:55
 */

@Data
public class SingleUploadParam {

    @NotNull(message = "上传文件不能为空")
    @Schema(description = "文件")
    private MultipartFile file;

    /**
     * Bucket names must be unique across all existing bucket names in Amazon S3.
     * <p>
     * Bucket名称在Amazon S3中的所有现有Bucket名称中必须是唯一的。
     * <p>
     * Bucket names must comply with DNS naming conventions.
     * <p>
     * Bucket名称必须符合DNS命名约定。
     * <p>
     * Bucket names must be at least 3 and no more than 63 characters long.
     * <p>
     * 存储桶名称的长度必须至少为3个字符，且不得超过63个字符。
     * <p>
     * Bucket names must not contain uppercase characters or underscores.
     * <p>
     * Bucket名称不能包含大写字符或下划线。
     * <p>
     * Bucket names must start with a lowercase letter or number.
     * <p>
     * Bucket名称必须以小写字母或数字开头。
     * <p>
     * Bucket names must be a series of one or more labels. Adjacent labels are separated by a single period (.). Bucket names can contain lowercase
     * letters, numbers, and hyphens. Each label must start and end with a lowercase letter or a number.
     * <p>
     * Bucket名称必须是一系列一个或多个标签。相邻的标签用一个句点（.）分隔。Bucket名称可以包含小写字母、数字和连字符。每个标签必须以小写字母或数字开头和结尾。
     * <p>
     * Bucket names must not be formatted as an IP address (for example, 192.168.5.4).
     * <p>
     * Bucket名称不能格式化为IP地址（例如192.168.5.4）。
     * <p>
     * When you use virtual hosted–style buckets with Secure Sockets Layer (SSL), the SSL wildcard certificate only matches buckets that don't contain
     * periods. To work around this, use HTTP or write your own certificate verification logic. We recommend that you do not use periods (".") in
     * bucket names when using virtual hosted–style buckets.
     * <p>
     * 当使用带有安全套接字层（SSL）的虚拟托管样式存储桶时，SSL通配符证书只匹配不包含句点的存储桶。要解决此问题，请使用HTTP或编写自己的证书验证逻辑。建议在使用虚拟托管样式存储桶时，不要在存储桶名称中使用句点（“.”）。
     */
    @Size(max = 63, message = "文件空间长度不能超过63")
    @Schema(description = "文件空间")
    private String namespace;

}
