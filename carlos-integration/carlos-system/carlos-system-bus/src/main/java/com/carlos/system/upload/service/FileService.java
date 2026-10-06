package com.carlos.system.upload.service;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.exception.BusinessException;
import com.carlos.core.response.Result;
import com.carlos.core.util.ExecutorUtil;
import com.carlos.oss.core.OssTemplate;
import com.carlos.oss.utils.OssUtil;
import com.carlos.system.enums.SystemErrorCode;
import com.carlos.system.upload.config.SystemFileProperties;
import com.carlos.system.upload.convert.UploadFileConvert;
import com.carlos.system.upload.pojo.dto.UploadFileDTO;
import com.carlos.system.upload.pojo.dto.UploadRecordDTO;
import com.carlos.system.upload.pojo.dto.UploadResultDTO;
import com.carlos.system.upload.pojo.enums.FileStorageTypeEnum;
import com.carlos.system.upload.pojo.enums.FileUploadStatusEnum;
import com.carlos.system.upload.pojo.param.UploadRecordPageParam;
import com.carlos.system.upload.pojo.vo.FileStatisticsVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * <p>
 * 文件相关服务
 * </p>
 *
 * @author Carlos
 * @date 2021/12/16 11:38
 */
@Slf4j
@AllArgsConstructor
@Service
public class FileService {

    private final UploadRecordService uploadRecordService;

    private final SystemFileProperties systemFileProperties;

    private static final ThreadPoolExecutor UPLOAD_POOL = ExecutorUtil.get(2, 8, "upload", 100, null);

    /** 文件base64前缀 */
    private static final String BASE_PREFIX_FORMAT = "data:%s;base64,";

    /**
     * 上传文件
     *
     * @param namespace 文件保存位置
     * @param files     上传文件列表
     * @return com.carlos.system.upload.pojo.dto.UploadResultDTO
     * @author Carlos
     * @date 2024/1/5 16:59
     */
    public UploadResultDTO upload(String namespace, List<UploadFileDTO> files) {
        if (CharSequenceUtil.isBlank(namespace)) {
            namespace = OssUtil.getDefaultBucket();
            log.warn("file namespace is null, use default bucket: {}", namespace);
        }

        final String groupId = RandomUtil.randomNumbers(10);
        final String prefix = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN) + StrUtil.SLASH + groupId;
        final List<Future<UploadFileDTO>> futures = new ArrayList<>(files.size());
        for (int i = 1; i <= files.size(); i++) {
            final UploadFileDTO file = files.get(i - 1);
            file.setGroupId(groupId);
            final int finalI = i;
            String finalNamespace = namespace;
            final Future<UploadFileDTO> res = UPLOAD_POOL.submit(() -> {
                final String originalFilename = file.getName();
                final String suffix = StrUtil.UNDERLINE + finalI + StrUtil.DOT + FileUtil.extName(originalFilename);
                final String obj = prefix + suffix;

                try {
                    OssUtil.putObject(finalNamespace, obj, IoUtil.toStream(file.getBytes()));
                } catch (Exception e) {
                    throw new BusinessException(SystemErrorCode.SYS_FILE_UPLOAD_FAILED, "文件上传失败！文件名称：" + originalFilename, e);
                }
                final UploadRecordDTO record = new UploadRecordDTO();
                record.setGroupId(groupId);
                record.setRepositoryName(finalNamespace);
                record.setRepositoryUrl(obj);
                record.setOriginalName(originalFilename);
                // 保存完整元数据
                record.setFileSize((long) file.getBytes().length);
                record.setContentType(detectContentType(originalFilename));
                record.setFileExtension(FileUtil.extName(originalFilename));
                record.setStorageType(detectStorageType());
                record.setUploadStatus(FileUploadStatusEnum.SUCCESS.getCode());
                uploadRecordService.addUploadRecord(record);
                file.setId(record.getId());
                file.setUrl(getDownloadUrl(record.getId()));
                file.setFileSize(record.getFileSize());
                file.setContentType(record.getContentType());
                file.setFileExtension(record.getFileExtension());
                return file;
            });
            futures.add(res);
        }

        final List<UploadFileDTO> records = new ArrayList<>();
        for (final Future<UploadFileDTO> future : futures) {
            UploadFileDTO file;
            try {
                file = future.get();
            } catch (final InterruptedException | ExecutionException e) {
                log.error("File upload failed: message:{}", e.getMessage(), e);
                continue;
            }
            records.add(file);
            log.info("File save success, id={}", file.getId());
        }
        return new UploadResultDTO(groupId, records);
    }

    /**
     * 上传 @MultipartFile类型文件
     *
     * @param namespace 上传空间
     * @param files     文件列表
     * @return com.carlos.system.upload.pojo.dto.UploadResultDTO
     */
    public UploadResultDTO uploadMultipartFile(String namespace, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            validateFile(file);
        }
        List<UploadFileDTO> uploadFiles = files.stream().map(i -> {
            UploadFileDTO file = new UploadFileDTO();
            file.setName(FileNameUtil.getName(i.getOriginalFilename()));
            byte[] bytes;
            try {
                bytes = i.getBytes();
            } catch (IOException e) {
                log.error("get file error! Error message:{}", e.getMessage(), e);
                throw new BusinessException(e);
            }
            file.setBytes(bytes);
            file.setFileSize(i.getSize());
            file.setContentType(i.getContentType());
            file.setFileExtension(FileUtil.extName(file.getName()));
            return file;
        }).collect(Collectors.toList());
        return this.upload(namespace, uploadFiles);
    }

    /**
     * 验证文件合法性（后缀、MIME类型、大小）
     *
     * @param file 上传文件
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw SystemErrorCode.SYS_FILE_CONTENT_EMPTY.exception();
        }
        // 1. 文件大小检查
        long maxSize = Optional.ofNullable(systemFileProperties.getMaxFileSize()).orElse(100 * 1024 * 1024L);
        if (file.getSize() > maxSize) {
            throw SystemErrorCode.SYS_FILE_SIZE_EXCEEDED.exception(
                    "文件大小不能超过 " + FileUtil.readableFileSize(maxSize));
        }
        // 2. 文件名长度检查
        String originalFilename = file.getOriginalFilename();
        if (CharSequenceUtil.isBlank(originalFilename)) {
            throw SystemErrorCode.SYS_PARAM_FILE_NAME_EMPTY.exception();
        }
        if (originalFilename.length() > 200) {
            throw SystemErrorCode.SYS_FILE_NAME_TOO_LONG.exception();
        }
        // 3. 后缀名检查
        String extension = FileUtil.extName(FileNameUtil.getName(originalFilename));
        Set<String> supportExt = systemFileProperties.getSupportExt();
        if (CollUtil.isNotEmpty(supportExt) && !supportExt.contains(extension)) {
            throw SystemErrorCode.SYS_FILE_TYPE_NOT_ALLOWED.exception(
                    "不支持" + extension + "类型文件上传！仅支持" + CollUtil.join(supportExt, StrUtil.SLASH) + "格式");
        }
        // 4. MIME类型检查（防绕过）
        String contentType = file.getContentType();
        if (CharSequenceUtil.isNotBlank(contentType)) {
            Set<String> forbiddenMimeTypes = systemFileProperties.getForbiddenMimeTypes();
            if (CollUtil.isNotEmpty(forbiddenMimeTypes) && forbiddenMimeTypes.contains(contentType)) {
                throw SystemErrorCode.SYS_FILE_TYPE_NOT_ALLOWED.exception("不安全的文件类型");
            }
        }
    }

    /**
     * 验证匿名上传
     *
     * @param file 上传文件
     */
    public void validateAnonymousUpload(MultipartFile file) {
        Boolean enabled = systemFileProperties.getAnonymousUploadEnabled();
        if (!Boolean.TRUE.equals(enabled)) {
            throw SystemErrorCode.SYS_FILE_UPLOAD_FAILED.exception("匿名上传未开启");
        }
        if (file != null) {
            String extension = FileUtil.extName(FileNameUtil.getName(file.getOriginalFilename()));
            Set<String> allowedExt = systemFileProperties.getAnonymousUploadAllowedExt();
            if (CollUtil.isNotEmpty(allowedExt) && !allowedExt.contains(extension)) {
                throw SystemErrorCode.SYS_FILE_TYPE_NOT_ALLOWED.exception(
                        "匿名上传不支持" + extension + "类型文件");
            }
        }
    }

    /**
     * 文件字节生成base64
     *
     * @param bytes    文件字节数组
     * @param mimeType 文件mimeType
     * @return java.lang.String
     */
    private String codeBase64(byte[] bytes, String mimeType) {
        if (CharSequenceUtil.isBlank(mimeType)) {
            return Base64.encode(bytes);
        }
        String format = StrUtil.format(BASE_PREFIX_FORMAT, mimeType);
        return format + Base64.encode(bytes);
    }

    /**
     * 流式下载文件
     *
     * @param id       文件id
     * @param response HTTP响应
     */
    public void downloadById(final Long id, final HttpServletResponse response) {
        UploadRecordDTO dto = uploadRecordService.getInfoById(id);
        if (dto == null) {
            throw SystemErrorCode.SYS_FILE_NOT_FOUND.exception("文件不存在！id=" + id);
        }
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = dto.getOriginalName();
        String mimeType = Optional.ofNullable(dto.getContentType())
                .orElse(FileUtil.getMimeType(fileName));
        try (InputStream stream = OssUtil.getObject(dto.getRepositoryName(), dto.getRepositoryUrl())) {
            if (stream == null) {
                JakartaServletUtil.write(response, JSONUtil.toJsonStr(Result.error("文件读取失败！")), ContentType.JSON.getValue());
                return;
            }
            JakartaServletUtil.write(response, stream, mimeType, fileName);
        } catch (Exception e) {
            log.error("文件下载失败：{}", fileName, e);
            throw SystemErrorCode.SYS_FILE_DOWNLOAD_FAILED.exception();
        }
    }

    /**
     * 获取文件信息
     *
     * @param id 文件id
     * @return com.carlos.system.upload.pojo.dto.UploadFileDTO
     * @author Carlos
     * @date 2024/1/5 11:32
     */
    public UploadFileDTO getFileInfo(Long id, boolean loadStream) {
        UploadRecordDTO dto = uploadRecordService.getInfoById(id);
        if (dto == null) {
            throw SystemErrorCode.SYS_FILE_NOT_FOUND.exception("文件不存在！id=" + id);
        }
        UploadFileDTO fileinfo = new UploadFileDTO();
        fileinfo.setId(dto.getId());
        fileinfo.setGroupId(dto.getGroupId());
        fileinfo.setUrl(this.getDownloadUrl(id));
        fileinfo.setName(dto.getOriginalName());
        fileinfo.setDesc(dto.getOriginalName());
        fileinfo.setFileSize(dto.getFileSize());
        fileinfo.setContentType(dto.getContentType());
        fileinfo.setFileExtension(dto.getFileExtension());
        if (loadStream) {
            InputStream stream = OssUtil.getObject(dto.getRepositoryName(), dto.getRepositoryUrl());
            if (stream != null) {
                fileinfo.setBytes(IoUtil.readBytes(stream));
            }
        }
        return fileinfo;
    }

    /**
     * 获取文件下载地址
     *
     * @param id 文件id
     * @return java.lang.String
     * @author Carlos
     * @date 2024/1/5 13:43
     */
    public String getDownloadUrl(Serializable id) {
        String baseUrl = systemFileProperties.getBaseUrl();
        String downloadPath = systemFileProperties.getDownloadPath();
        return baseUrl + downloadPath + id;
    }

    /**
     * 根据批次号获取一批次文件id
     *
     * @param groupId 批次id
     * @return java.util.List<com.carlos.system.upload.pojo.dto.UploadFileDTO>
     * @author Carlos
     * @date 2024/1/5 14:17
     */
    public List<UploadFileDTO> getFileGroup(String groupId) {
        List<UploadRecordDTO> files = uploadRecordService.getInfoByGroupId(groupId);
        return files.stream().map(i -> {
            UploadFileDTO file = UploadFileConvert.INSTANCE.toDTO(i);
            file.setUrl(getDownloadUrl(file.getId()));
            return file;
        }).collect(Collectors.toList());
    }

    /**
     * 批量获取文件信息
     *
     * @param ids 文件ids
     * @return java.util.List<com.carlos.system.upload.pojo.dto.UploadFileDTO>
     * @author Carlos
     * @date 2024/1/5 14:25
     */
    public List<UploadFileDTO> getFileInfo(Set<Long> ids) {
        List<UploadRecordDTO> files = uploadRecordService.getInfoByIds(ids);
        return files.stream().map(i -> {
            UploadFileDTO file = UploadFileConvert.INSTANCE.toDTO(i);
            file.setUrl(getDownloadUrl(file.getId()));
            return file;
        }).collect(Collectors.toList());
    }

    /**
     * 删除单个文件（删除记录 + 删除OSS对象）
     *
     * @param id 文件id
     */
    public void deleteFile(Long id) {
        UploadRecordDTO dto = uploadRecordService.getInfoById(id);
        if (dto == null) {
            throw SystemErrorCode.SYS_FILE_NOT_FOUND.exception("文件不存在！id=" + id);
        }
        // 删除OSS对象
        try {
            OssUtil.deleteObject(dto.getRepositoryName(), dto.getRepositoryUrl());
        } catch (Exception e) {
            log.warn("删除OSS对象失败, bucket:{}, object:{}", dto.getRepositoryName(), dto.getRepositoryUrl(), e);
        }
        // 逻辑删除记录
        uploadRecordService.deleteUploadRecord(Set.of(Long.valueOf(id)));
    }

    /**
     * 批量删除文件
     *
     * @param ids 文件id集合
     */
    public void deleteFiles(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        List<UploadRecordDTO> records = uploadRecordService.getInfoByIds(ids);
        if (CollUtil.isEmpty(records)) {
            return;
        }
        // 批量删除OSS对象
        for (UploadRecordDTO dto : records) {
            try {
                OssUtil.deleteObject(dto.getRepositoryName(), dto.getRepositoryUrl());
            } catch (Exception e) {
                log.warn("删除OSS对象失败, bucket:{}, object:{}", dto.getRepositoryName(), dto.getRepositoryUrl(), e);
            }
        }
        // 批量逻辑删除记录
        uploadRecordService.deleteUploadRecord(ids.stream().map(Long::valueOf).collect(Collectors.toSet()));
    }

    /**
     * 更新文件信息
     *
     * @param id  文件id
     * @param dto 更新信息
     */
    public void updateFile(Long id, UploadRecordDTO dto) {
        UploadRecordDTO existing = uploadRecordService.getInfoById(id);
        if (existing == null) {
            throw SystemErrorCode.SYS_FILE_NOT_FOUND.exception("文件不存在！id=" + id);
        }
        dto.setId(existing.getId());
        uploadRecordService.updateUploadRecord(dto);
    }

    /**
     * 分页查询文件列表
     *
     * @param param 分页参数
     * @return com.baomidou.mybatisplus.core.metadata.IPage
     */
    public IPage<UploadRecordDTO> page(UploadRecordPageParam param) {
        return uploadRecordService.page(param);
    }

    /**
     * 获取文件统计信息
     *
     * @return com.carlos.system.upload.pojo.vo.FileStatisticsVO
     */
    public FileStatisticsVO getStatistics() {
        // 总文件数
        long totalCount = uploadRecordService.count();
        // 总存储大小
        long totalSize = uploadRecordService.totalSize();
        // 今日上传数
        long todayUploadCount = uploadRecordService.todayUploadCount();
        // 按类型分布
        Map<String, Long> typeDistribution = uploadRecordService.typeDistribution();

        FileStatisticsVO vo = new FileStatisticsVO();
        vo.setTotalCount(totalCount);
        vo.setTotalSize(totalSize);
        vo.setReadableTotalSize(FileUtil.readableFileSize(totalSize));
        vo.setTodayUploadCount(todayUploadCount);
        vo.setTypeDistribution(typeDistribution != null ? typeDistribution : new HashMap<>());
        return vo;
    }

    /**
     * 检测文件MIME类型
     *
     * @param fileName 文件名
     * @return MIME类型
     */
    private String detectContentType(String fileName) {
        String ext = FileUtil.extName(fileName);
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            case "mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }

    /**
     * 检测当前使用的存储类型
     *
     * @return 存储类型编码
     */
    private String detectStorageType() {
        try {
            OssTemplate template = OssUtil.getTemplate();
            if (template instanceof com.carlos.oss.adapter.LocalOssTemplate) {
                return FileStorageTypeEnum.LOCAL.getCode();
            }
            return FileStorageTypeEnum.S3.getCode();
        } catch (Exception e) {
            return FileStorageTypeEnum.S3.getCode();
        }
    }
}
