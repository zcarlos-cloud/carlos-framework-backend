package com.carlos.system.upload.controller;


import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.upload.convert.UploadFileConvert;
import com.carlos.system.upload.convert.UploadRecordConvert;
import com.carlos.system.upload.pojo.dto.UploadFileDTO;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.dto.UploadResultDTO;
import com.carlos.system.upload.pojo.param.SingleUploadParam;
import com.carlos.system.upload.pojo.param.UploadRecordPageParam;
import com.carlos.system.upload.pojo.vo.FileInfoVO;
import com.carlos.system.upload.pojo.vo.FileStatisticsVO;
import com.carlos.system.upload.pojo.vo.UploadRecordVO;
import com.carlos.system.upload.pojo.vo.UploadResultVO;
import com.carlos.system.upload.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
@RequestMapping("/sys/file")
@Tag(name = "文件服务")
public class FileController {

    private final FileService fileService;

    @Log(
        title = "文件管理",
        logType = "SYS_FILE_UPLOAD",
        businessType = BusinessType.INSERT,
        operation = "'上传文件，namespace: ' + #namespace",
        targetType = "SYS_FILE",
        saveRequestParams = false,
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:file', 'create')")
    @PostMapping("upload")
    @Operation(summary = "上传文件")
    public Result<UploadResultVO> upload(@RequestPart @RequestParam("files") MultipartFile[] files, @RequestParam("namespace") String namespace) {
        UploadResultDTO dto = fileService.uploadMultipartFile(namespace, Arrays.asList(files));
        UploadResultVO vo = UploadFileConvert.INSTANCE.toResultVO(dto);
        return Result.success(vo);
    }

    // @Validated 不可省：非 @RequestBody 的 POJO 参数（隐式 @ModelAttribute）同样要
    // 显式标注才会触发校验，否则 SingleUploadParam.file 上的 @NotNull 是条死规则
    @Log(
        title = "文件管理",
        logType = "SYS_FILE_SINGLE_UPLOAD",
        businessType = BusinessType.INSERT,
        operation = "'单文件上传，namespace: ' + #param.namespace",
        targetType = "SYS_FILE",
        saveRequestParams = false,
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:file', 'create')")
    @PostMapping("singleUpload")
    @Operation(summary = "单文件上传")
    public Result<FileInfoVO> singleUpload(@Validated SingleUploadParam param) {
        return doSingleUpload(param, false);
    }

    @Log(
        title = "文件管理",
        logType = "SYS_FILE_ANONYMOUS_UPLOAD",
        businessType = BusinessType.INSERT,
        operation = "'匿名上传单文件，namespace: ' + #param.namespace",
        targetType = "SYS_FILE",
        saveRequestParams = false,
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:file', 'create')")
    @PostMapping("singleUploadForAnonymous")
    @Operation(summary = "匿名单文件上传")
    public Result<FileInfoVO> singleUploadForAnonymous(@Validated SingleUploadParam param) {
        return doSingleUpload(param, true);
    }

    private Result<FileInfoVO> doSingleUpload(SingleUploadParam param, boolean anonymous) {
        if (anonymous) {
            fileService.validateAnonymousUpload(param.getFile());
        }
        UploadResultDTO result = fileService.uploadMultipartFile(param.getNamespace(), List.of(param.getFile()));
        List<UploadFileDTO> files = result.getFiles();
        if (CollUtil.isEmpty(files)) {
            throw SystemErrorCode.SYS_FILE_UPLOAD_FAILED.exception();
        }
        UploadFileDTO record = files.stream().findFirst().get();
        return Result.success(UploadFileConvert.INSTANCE.toFileVO(record));
    }

    @Operation(summary = "获取二进制文件流", description = "下载文件或者播放文件")
    @PreAuthorize("hasPermission('manage:file', 'view')")
    @GetMapping("load")
    public void download(@RequestParam("id") Long id, HttpServletResponse response) {
        if (id == null) {
            return;
        }
        fileService.downloadById(id, response);
    }

    @Operation(summary = "查询文件详情信息", description = "查询文件信息获取地址")
    @PreAuthorize("hasPermission('manage:file', 'view')")
    @GetMapping("detail")
    public Result<FileInfoVO> getFile(@RequestParam("id") Long id) {
        if (id == null) {
            return Result.error("文件ID不能为空");
        }
        UploadFileDTO dto = fileService.getFileInfo(id, false);
        return Result.success(UploadFileConvert.INSTANCE.toFileVO(dto));
    }

    @Operation(summary = "文件列表分页查询")
    @PreAuthorize("hasPermission('manage:file', 'view')")
    @GetMapping("page")
    public Result<IPage<UploadRecordVO>> page(UploadRecordPageParam param) {
        IPage<UploadRecordDTO> page = fileService.page(param);
        return Result.success(page.convert(UploadRecordConvert.INSTANCE::toVO));
    }

    @Log(
        title = "文件管理",
        logType = "SYS_FILE_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除文件: ' + #id",
        targetType = "SYS_FILE",
        targetId = "#id",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:file', 'delete')")
    @Operation(summary = "删除文件")
    @PostMapping("delete")
    public Result<Void> delete(@RequestParam("id") Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }

    @Log(
        title = "文件管理",
        logType = "SYS_FILE_DELETE_BATCH",
        businessType = BusinessType.DELETE,
        operation = "'批量删除文件: ' + #ids",
        targetType = "SYS_FILE",
        targetId = "#ids",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:file', 'delete')")
    @Operation(summary = "批量删除文件")
    @PostMapping("deleteBatch")
    public Result<Void> deleteBatch(@RequestBody @NotEmpty(message = "文件ID集合不能为空") Set<Long> ids) {
        fileService.deleteFiles(ids);
        return Result.success();
    }

    @Log(
        title = "文件管理",
        logType = "SYS_FILE_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'更新文件记录: ' + #id",
        targetType = "SYS_FILE",
        targetId = "#id",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:file', 'update')")
    @Operation(summary = "更新文件信息")
    @PostMapping("update")
    public Result<Void> update(@RequestParam("id") Long id, @RequestBody @Validated UploadRecordDTO dto) {
        fileService.updateFile(id, dto);
        return Result.success();
    }

    @Operation(summary = "文件统计")
    @PreAuthorize("hasPermission('manage:file', 'view')")
    @GetMapping("statistics")
    public Result<FileStatisticsVO> statistics() {
        return Result.success(fileService.getStatistics());
    }
}
