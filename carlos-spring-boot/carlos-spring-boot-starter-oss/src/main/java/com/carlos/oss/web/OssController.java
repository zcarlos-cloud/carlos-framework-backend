package com.carlos.oss.web;

import cn.hutool.core.io.file.FileNameUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.oss.config.OssProperties;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.model.OssClientInfo;
import com.carlos.oss.model.OssFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * <p>
 * OSS 接口控制器
 * 提供 OSS 相关信息查询和基础操作接口
 * </p>
 *
 * @author carlos
 * @date 2026-03-06
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/oss")
@Tag(name = "OSS 对象存储接口")
// 本类由 OssWebAutoConfiguration 以 @Bean 注册（那里已按同一开关把关），因此它并不是「靠扫描生效」的。
// 但 @RestController 是 @Component 的元注解，宿主应用的扫描根一旦覆盖 com.carlos.oss.web，
// 扫描会再注册一份<b>完全不过条件</b>的实例 —— 两个后果都很难看：
//   1. OSS 关闭（无 OssProperties Bean）时扫描到它 → 构造失败、<b>整个服务起不来</b>；
//   2. OSS 开启且扫描根较宽时，与自动配置那份<b>同名 Bean 冲突</b>（overriding 默认 false）。
// 补上这个条件后，OSS 关闭时扫描直接跳过本类；开启时由自动配置的 @ConditionalOnMissingBean 让路。
// 组件扫描会尊重 @ConditionalOnProperty（SpringBootCondition 是普通 Condition，在扫描阶段即被求值）。
@ConditionalOnProperty(prefix = "carlos.oss", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OssController {

    private final OssProperties properties;
    private final OssTemplate ossTemplate;

    public OssController(OssProperties properties, OssTemplate ossTemplate) {
        this.properties = properties;
        this.ossTemplate = ossTemplate;
    }

    /**
     * 获取 OSS 客户端初始化信息
     */
    @GetMapping("/init")
    @Operation(summary = "获取 OSS 初始化信息")
    public Result<OssClientInfo> init() {
        return Result.success(OssClientInfo.builder()
            .endpoint(properties.getEndpoint())
            .publicEndpoint(properties.getPublicEndpoint())
            .defaultBucket(properties.getBucketName())
            .region(properties.getRegion())
            .pathStyleAccess(properties.isPathStyleAccess())
            .type(properties.getType() != null ? properties.getType().name() : null)
            .build());
    }

    /**
     * 获取默认桶名称
     */
    @GetMapping("/bucket/default")
    @Operation(summary = "获取默认桶名称")
    public Result<String> getDefaultBucket() {
        return Result.success(properties.getBucketName());
    }

    /**
     * 列出所有桶
     */
    @GetMapping("/bucket/list")
    @Operation(summary = "列出所有存储桶")
    public Result<List<String>> listBuckets() {
        return Result.success(ossTemplate.listBuckets());
    }

    /**
     * 检查桶是否存在
     */
    @GetMapping("/bucket/exists")
    @Operation(summary = "检查存储桶是否存在")
    @Parameter(name = "bucketName", description = "桶名称")
    public Result<Boolean> bucketExists(@RequestParam String bucketName) {
        return Result.success(ossTemplate.bucketExists(bucketName));
    }

    /**
     * 创建桶
     */
    @PostMapping("/bucket/create")
    @Operation(summary = "创建存储桶")
    @Parameter(name = "bucketName", description = "桶名称")
    public Result<Void> createBucket(@RequestParam String bucketName) {
        ossTemplate.createBucket(bucketName);
        return Result.success();
    }

    /**
     * 删除桶
     */
    @PostMapping("/bucket/delete")
    @Operation(summary = "删除存储桶")
    @Parameter(name = "bucketName", description = "桶名称")
    public Result<Void> deleteBucket(@RequestParam String bucketName) {
        ossTemplate.deleteBucket(bucketName);
        return Result.success();
    }

    /**
     * 上传文件
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    @Parameter(name = "file", description = "文件")
    @Parameter(name = "objectName", description = "对象名称（可选，默认使用原文件名）")
    @Parameter(name = "bucketName", description = "桶名称（可选，默认使用配置桶）")
    public Result<OssFile> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(required = false) String objectName,
        @RequestParam(required = false) String bucketName) {

        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        String originalFilename = file.getOriginalFilename();
        String targetObject = objectName != null ? objectName : (originalFilename != null ? FileNameUtil.getName(originalFilename) : null);

        try {
            OssFile ossFile = ossTemplate.putObject(targetBucket, targetObject,
                file.getInputStream(), file.getContentType());
            return Result.success(ossFile);
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw CommonErrorCode.FILE_UPLOAD_ERROR.exception("Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     */
    @GetMapping("/info")
    @Operation(summary = "获取文件信息")
    @Parameter(name = "objectName", description = "对象名称")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    public Result<OssFile> getObjectInfo(
        @RequestParam String objectName,
        @RequestParam(required = false) String bucketName) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        return Result.success(ossTemplate.getObjectInfo(targetBucket, objectName));
    }

    /**
     * 检查文件是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查文件是否存在")
    @Parameter(name = "objectName", description = "对象名称")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    public Result<Boolean> objectExists(
        @RequestParam String objectName,
        @RequestParam(required = false) String bucketName) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        return Result.success(ossTemplate.objectExists(targetBucket, objectName));
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete")
    @Operation(summary = "删除文件")
    @Parameter(name = "objectName", description = "对象名称")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    public Result<Void> deleteObject(
        @RequestParam String objectName,
        @RequestParam(required = false) String bucketName) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        ossTemplate.deleteObject(targetBucket, objectName);
        return Result.success();
    }

    /**
     * 获取文件访问URL
     */
    @GetMapping("/url")
    @Operation(summary = "获取文件访问URL")
    @Parameter(name = "objectName", description = "对象名称")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    public Result<String> getObjectUrl(
        @RequestParam String objectName,
        @RequestParam(required = false) String bucketName) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        return Result.success(ossTemplate.getObjectUrl(targetBucket, objectName));
    }

    /**
     * 获取预签名URL
     */
    @GetMapping("/presigned-url")
    @Operation(summary = "获取预签名URL（临时访问链接）")
    @Parameter(name = "objectName", description = "对象名称")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    @Parameter(name = "expires", description = "过期时间（秒，默认3600，最大604800）")
    public Result<String> getPresignedUrl(
        @RequestParam String objectName,
        @RequestParam(required = false) String bucketName,
        @RequestParam(required = false, defaultValue = "3600")
        @Min(1) @Max(604800) int expires) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        return Result.success(ossTemplate.getPresignedUrl(targetBucket, objectName, expires));
    }

    /**
     * 列出文件
     */
    @GetMapping("/list")
    @Operation(summary = "列出文件")
    @Parameter(name = "prefix", description = "前缀（可选）")
    @Parameter(name = "bucketName", description = "桶名称（可选）")
    public Result<List<OssFile>> listObjects(
        @RequestParam(required = false) String prefix,
        @RequestParam(required = false) String bucketName) {
        String targetBucket = bucketName != null ? bucketName : properties.getBucketName();
        return Result.success(ossTemplate.listObjects(targetBucket, prefix));
    }
}
