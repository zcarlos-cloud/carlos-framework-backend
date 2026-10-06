package com.carlos.system.upload.apiimpl;


import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.carlos.core.response.Result;
import com.carlos.system.api.ApiFile;
import com.carlos.system.pojo.ao.FileInfoAO;
import com.carlos.system.pojo.ao.UploadResultAO;
import com.carlos.system.pojo.param.ApiFileUpdateParam;
import com.carlos.system.pojo.param.ApiFileUploadParam;
import com.carlos.system.pojo.param.ApiFileUploadParam.UploadFile;
import com.carlos.system.upload.convert.UploadFileConvert;
import com.carlos.system.upload.convert.UploadRecordConvert;
import com.carlos.system.upload.pojo.dto.UploadFileDTO;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.dto.UploadResultDTO;
import com.carlos.system.upload.service.FileService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 动态信息 api接口
 * </p>
 *
 * @author Carlos
 * @date 2021-12-15 17:40:44
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("api/sys/file")
@Tag(name = "文件接口")
public class ApiFileImpl implements ApiFile {


    private final FileService fileService;


    @Override
    @Operation(summary = "获取文件信息")
    @GetMapping("info")
    public Result<FileInfoAO> getFile(@RequestParam("id") Long id) {
        if (id == null) {
            return Result.error("文件id不能为空");
        }
        UploadFileDTO dto;
        try {
            dto = fileService.getFileInfo(id, false);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
        return Result.success(UploadFileConvert.INSTANCE.toAO(dto));
    }

    @Override
    @GetMapping("stream/info")
    @Operation(summary = "获取文件流")
    public Result<FileInfoAO> getFileStreamInfo(@RequestParam("id") Long id) {
        if (id == null) {
            return Result.error("文件id不能为空");
        }
        UploadFileDTO fileInfo = fileService.getFileInfo(id, true);
        return Result.success(UploadFileConvert.INSTANCE.toAO(fileInfo));
    }


    @Override
    @Operation(summary = "获取一组文件信息")
    @GetMapping("group")
    public Result<List<FileInfoAO>> getGroupFile(@RequestParam("groupId") String groupId) {
        if (groupId == null) {
            return Result.success(Collections.emptyList());
        }
        List<UploadFileDTO> files = fileService.getFileGroup(groupId);
        return Result.success(UploadRecordConvert.INSTANCE.uploadFile2AO(files));
    }

    @Override
    @Operation(summary = "批量获取文件信息")
    @GetMapping("info/ids")
    public Result<List<FileInfoAO>> getFiles(@RequestParam("ids") Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Result.success(Collections.emptyList());
        }
        List<UploadFileDTO> files = fileService.getFileInfo(ids);
        return Result.success(UploadRecordConvert.INSTANCE.uploadFile2AO(files));
    }

    @Override
    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public Result<UploadResultAO> upload(@RequestBody @Validated ApiFileUploadParam params) {
        List<UploadFile> files = params.getFiles();
        List<UploadFileDTO> dtos = files.stream().map(f -> {
            UploadFileDTO uploadFileDTO = new UploadFileDTO();
            uploadFileDTO.setName(f.getName());
            uploadFileDTO.setBytes(f.getBytes());
            return uploadFileDTO;
        }).collect(Collectors.toList());
        UploadResultDTO result = fileService.upload(params.getNamespace(), dtos);
        return Result.success(UploadFileConvert.INSTANCE.toAO(result));
    }

    @Override
    @Operation(summary = "删除文件")
    @PostMapping("delete")
    public Result<Void> delete(@RequestParam("id") Long id) {
        fileService.deleteFile(id);
        return Result.success();
    }

    @Override
    @Operation(summary = "批量删除文件")
    @PostMapping("deleteBatch")
    public Result<Void> deleteBatch(@RequestBody Set<Long> ids) {
        fileService.deleteFiles(ids);
        return Result.success();
    }

    @Override
    @Operation(summary = "更新文件信息")
    @PostMapping("update")
    public Result<Void> update(@RequestParam("id") Long id, @RequestBody ApiFileUpdateParam param) {
        UploadRecordDTO dto = new UploadRecordDTO();
        dto.setOriginalName(param.getOriginalName());
        dto.setUses(param.getUses());
        fileService.updateFile(id, dto);
        return Result.success();
    }
}
