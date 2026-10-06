package com.carlos.devkit.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.support.TemplatePathValidator;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.manager.TemplateFileManager;
import com.carlos.devkit.manager.TemplateGroupManager;
import com.carlos.devkit.manager.TemplateVersionManager;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.entity.DevTemplate;
import com.carlos.devkit.pojo.enums.DevkitStatusEnum;
import com.carlos.devkit.pojo.enums.TemplateFileTypeEnum;
import com.carlos.devkit.pojo.param.TemplateFileCreateParam;
import com.carlos.devkit.pojo.param.TemplateFileRenameParam;
import com.carlos.devkit.pojo.param.TemplateFileSaveParam;
import com.carlos.devkit.pojo.vo.TemplateFileNodeVO;
import com.carlos.devkit.service.TemplateFileService;
import com.carlos.devkit.support.ZipPacker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 模板文件 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateFileServiceImpl implements TemplateFileService {

    /**
     * 模板文件后缀
     */
    private static final String FTL_SUFFIX = ".ftl";

    /**
     * 文本正文编码
     */
    private static final String ENCODING_TEXT = "TEXT";

    private final TemplateFileManager templateFileManager;

    private final TemplateGroupManager templateGroupManager;

    private final TemplateVersionManager templateVersionManager;

    private final DevkitProperties devkitProperties;

    @Override
    public List<TemplateFileNodeVO> listTree(final Serializable groupId) {
        requireGroup(groupId);
        List<TemplateFileDTO> files = templateFileManager.listByGroupId(groupId);
        return buildTree(files);
    }

    @Override
    public TemplateFileDTO getDetail(final Serializable id) {
        TemplateFileDTO dto = templateFileManager.getDtoById(id);
        if (dto == null) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception();
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFile(final TemplateFileCreateParam param) {
        requireGroup(param.getGroupId());
        String filePath;
        try {
            filePath = TemplatePathValidator.validate(param.getFilePath());
        } catch (IllegalArgumentException e) {
            throw DevkitErrorCode.TEMPLATE_PATH_INVALID.exception(e.getMessage());
        }
        if (templateFileManager.countByPath(param.getGroupId(), filePath, null) > 0) {
            throw DevkitErrorCode.TEMPLATE_FILE_PATH_EXISTS.exception(filePath);
        }
        String content = StrUtil.nullToEmpty(param.getContent());
        checkContentSize(content);

        TemplateFileDTO dto = new TemplateFileDTO()
            .setGroupId(param.getGroupId())
            .setFilePath(filePath)
            .setFileName(FilenameUtils.getName(filePath))
            .setFileExt(FilenameUtils.getExtension(filePath))
            .setFileType(param.getFileType())
            .setLoopFlag(StrUtil.endWith(filePath, FTL_SUFFIX)
                && StrUtil.contains(FilenameUtils.getBaseName(filePath), "[for]"))
            .setContent(content)
            .setContentEncoding(ENCODING_TEXT)
            .setFileSize(content.getBytes(StandardCharsets.UTF_8).length)
            .setMd5(DigestUtil.md5Hex(content))
            .setSortNum(0)
            .setStatus(DevkitStatusEnum.ENABLED.getCode())
            .setRemark(param.getRemark());
        templateFileManager.add(dto);
        log.info("新增模板文件, groupId:{}, path:{}", param.getGroupId(), filePath);
        return dto.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFile(final TemplateFileSaveParam param) {
        TemplateFileDTO old = getDetail(param.getId());
        String content = StrUtil.nullToEmpty(param.getContent());
        checkContentSize(content);
        String newMd5 = DigestUtil.md5Hex(content);
        if (StrUtil.equals(old.getMd5(), newMd5)) {
            log.debug("模板正文未变化，跳过保存: {}", old.getFilePath());
            return true;
        }
        // 保存前先落一版历史，保证可回滚
        snapshot(old, param.getChangeLog());

        TemplateFileDTO update = new TemplateFileDTO()
            .setId(old.getId())
            .setContent(content)
            .setContentEncoding(ENCODING_TEXT)
            .setFileSize(content.getBytes(StandardCharsets.UTF_8).length)
            .setMd5(newMd5)
            .setLoopFlag(StrUtil.endWith(old.getFilePath(), FTL_SUFFIX)
                && StrUtil.contains(FilenameUtils.getBaseName(old.getFilePath()), "[for]"));
        return templateFileManager.modify(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean renameFile(final TemplateFileRenameParam param) {
        TemplateFileDTO old = getDetail(param.getId());
        String newPath;
        try {
            newPath = TemplatePathValidator.validate(param.getNewPath());
        } catch (IllegalArgumentException e) {
            throw DevkitErrorCode.TEMPLATE_PATH_INVALID.exception(e.getMessage());
        }
        if (StrUtil.equals(old.getFilePath(), newPath)) {
            return true;
        }
        if (templateFileManager.countByPath(old.getGroupId(), newPath, old.getId()) > 0) {
            throw DevkitErrorCode.TEMPLATE_FILE_PATH_EXISTS.exception(newPath);
        }
        TemplateFileDTO update = new TemplateFileDTO()
            .setId(old.getId())
            .setFilePath(newPath)
            .setFileName(FilenameUtils.getName(newPath))
            .setFileExt(FilenameUtils.getExtension(newPath))
            .setLoopFlag(StrUtil.endWith(newPath, FTL_SUFFIX)
                && StrUtil.contains(FilenameUtils.getBaseName(newPath), "[for]"));
        return templateFileManager.modify(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFiles(final Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception("请选择要删除的模板文件");
        }
        return templateFileManager.delete(new ArrayList<>(ids));
    }

    @Override
    public byte[] exportGroup(final Serializable groupId) {
        requireGroup(groupId);
        List<TemplateFileDTO> files = templateFileManager.listEnabledByGroupId(groupId);
        if (files.isEmpty()) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception("该模板组下没有可导出的模板文件");
        }
        List<GeneratedFile> entries = new ArrayList<>(files.size());
        for (TemplateFileDTO file : files) {
            byte[] bytes = toBytes(file);
            entries.add(new GeneratedFile(file.getFilePath(), bytes, file.getContentEncoding() == null
                || ENCODING_TEXT.equalsIgnoreCase(file.getContentEncoding()), bytes.length));
        }
        return ZipPacker.pack(entries);
    }

    @Override
    public List<TemplateSourceFile> listSourceFiles(final Serializable groupId) {
        List<TemplateFileDTO> files = templateFileManager.listEnabledByGroupId(groupId);
        if (files.isEmpty()) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception("该模板组下没有启用的模板文件");
        }
        List<TemplateSourceFile> sources = new ArrayList<>(files.size());
        for (TemplateFileDTO file : files) {
            sources.add(new TemplateSourceFile(file.getFilePath(), file.getContent(),
                file.getContentEncoding()));
        }
        return sources;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByGroupId(final Serializable groupId) {
        return templateFileManager.deleteByGroupId(groupId);
    }

    /**
     * 落一版模板历史
     *
     * @param old       保存前的模板文件
     * @param changeLog 变更说明
     */
    private void snapshot(final TemplateFileDTO old, final String changeLog) {
        TemplateVersionDTO version = new TemplateVersionDTO()
            .setTemplateId(old.getId())
            .setGroupId(old.getGroupId())
            .setVersionNo(templateVersionManager.nextVersionNo(old.getId()))
            .setFilePath(old.getFilePath())
            .setContent(old.getContent())
            .setContentEncoding(old.getContentEncoding() == null ? ENCODING_TEXT : old.getContentEncoding())
            .setFileSize(old.getFileSize() == null ? 0 : old.getFileSize())
            .setMd5(old.getMd5())
            .setChangeLog(StrUtil.maxLength(changeLog, 500));
        templateVersionManager.add(version);
        // 超出保留数量的旧版本清理
        int keep = devkitProperties.getTemplateMaxVersions();
        List<Long> expired = templateVersionManager.listExpiredVersionIds(old.getId(), keep);
        if (!expired.isEmpty()) {
            templateVersionManager.delete(new ArrayList<>(expired));
            log.debug("清理模板[{}]超期历史版本 {} 个", old.getId(), expired.size());
        }
    }

    /**
     * 模板正文 → 字节
     *
     * @param file 模板文件
     * @return 字节内容
     */
    private byte[] toBytes(final TemplateFileDTO file) {
        String content = StrUtil.nullToEmpty(file.getContent());
        if (file.getContentEncoding() != null && !ENCODING_TEXT.equalsIgnoreCase(file.getContentEncoding())) {
            return Base64.getDecoder().decode(content);
        }
        return content.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 校验正文大小
     *
     * @param content 正文
     */
    private void checkContentSize(final String content) {
        int size = content.getBytes(StandardCharsets.UTF_8).length;
        if (size > devkitProperties.getMaxTemplateContentSize()) {
            throw DevkitErrorCode.TEMPLATE_CONTENT_TOO_LARGE.exception(
                "模板正文 " + size + " 字节，超过 " + devkitProperties.getMaxTemplateContentSize() + " 字节上限");
        }
    }

    /**
     * 校验模板组存在
     *
     * @param groupId 模板组ID
     * @return 模板组 DTO
     */
    private TemplateGroupDTO requireGroup(final Serializable groupId) {
        TemplateGroupDTO group = templateGroupManager.getDtoById(groupId);
        if (group == null) {
            throw DevkitErrorCode.TEMPLATE_GROUP_NOT_FOUND.exception();
        }
        return group;
    }

    /**
     * 由扁平文件清单构建目录树
     *
     * @param files 模板文件清单
     * @return 树
     */
    private List<TemplateFileNodeVO> buildTree(final List<TemplateFileDTO> files) {
        TemplateFileNodeVO root = new TemplateFileNodeVO()
            .setKey("/")
            .setTitle("/")
            .setNodeType("dir")
            .setChildren(new ArrayList<>());
        Map<String, TemplateFileNodeVO> dirIndex = new LinkedHashMap<>();
        dirIndex.put("", root);

        for (TemplateFileDTO file : files) {
            String path = file.getFilePath();
            String[] segments = path.split("/");
            StringBuilder prefix = new StringBuilder();
            TemplateFileNodeVO parent = root;
            for (int i = 0; i < segments.length - 1; i++) {
                if (prefix.length() > 0) {
                    prefix.append('/');
                }
                prefix.append(segments[i]);
                String dirKey = prefix.toString();
                TemplateFileNodeVO dir = dirIndex.get(dirKey);
                if (dir == null) {
                    dir = new TemplateFileNodeVO()
                        .setKey("dir:" + dirKey)
                        .setTitle(segments[i])
                        .setNodeType("dir")
                        .setFilePath(dirKey)
                        .setChildren(new ArrayList<>());
                    dirIndex.put(dirKey, dir);
                    parent.getChildren().add(dir);
                }
                parent = dir;
            }
            parent.getChildren().add(new TemplateFileNodeVO()
                .setKey("file:" + file.getId())
                .setTitle(segments[segments.length - 1])
                .setNodeType("file")
                .setFileId(file.getId())
                .setFilePath(path)
                .setFileType(file.getFileType())
                .setLoopFlag(file.getLoopFlag())
                .setFileSize(file.getFileSize())
                .setStatus(file.getStatus()));
        }
        sortTree(root.getChildren());
        return root.getChildren();
    }

    /**
     * 递归排序：目录在前，文件在后，各自按名称升序
     *
     * @param nodes 节点
     */
    private void sortTree(final List<TemplateFileNodeVO> nodes) {
        nodes.sort(Comparator
            .comparing((TemplateFileNodeVO node) -> "dir".equals(node.getNodeType()) ? 0 : 1)
            .thenComparing(TemplateFileNodeVO::getTitle));
        for (TemplateFileNodeVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortTree(node.getChildren());
            }
        }
    }
}
