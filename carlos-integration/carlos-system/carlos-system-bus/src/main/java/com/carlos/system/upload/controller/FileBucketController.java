package com.carlos.system.upload.controller;


import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.upload.pojo.param.BucketCreateParam;
import com.carlos.system.upload.service.FileBucketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>
 * 文件上传相关接口
 * </p>
 *
 * @author Carlos
 * @date 2021-12-8 16:13:43
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/file/bucket")
@Tag(name = "文件bucket操作")
public class FileBucketController {

    private final FileBucketService fileBucketService;

    @Log(
        title = "文件Bucket",
        logType = "SYS_FILE_BUCKET_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'创建 bucket'",
        targetType = "SYS_FILE_BUCKET",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:file-bucket', 'create')")
    @PostMapping("create")
    @Operation(summary = "创建bucket")
    public void create(@RequestBody @Validated List<BucketCreateParam> buckets) {
        fileBucketService.createBuckets(buckets);
    }

    @Log(
        title = "文件Bucket",
        logType = "SYS_FILE_BUCKET_POLICY",
        businessType = BusinessType.UPDATE,
        operation = "'设置 bucket 策略'",
        targetType = "SYS_FILE_BUCKET",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:file-bucket', 'setPolicy')")
    @PostMapping("policy")
    @Operation(summary = "设置Policy")
    public void setPrivate(@RequestBody @Validated List<BucketCreateParam> buckets) {
        fileBucketService.setPolicies(buckets);
    }
}
