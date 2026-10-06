package com.carlos.devkit.service.impl;

import com.carlos.core.pagination.Paging;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.manager.TemplateFileManager;
import com.carlos.devkit.manager.TemplateVersionManager;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.dto.TemplateVersionDTO;
import com.carlos.devkit.pojo.param.TemplateFileSaveParam;
import com.carlos.devkit.pojo.param.TemplateVersionPageParam;
import com.carlos.devkit.service.TemplateFileService;
import com.carlos.devkit.service.TemplateVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

/**
 * <p>
 * 模板版本历史 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateVersionServiceImpl implements TemplateVersionService {

    private final TemplateVersionManager templateVersionManager;

    private final TemplateFileManager templateFileManager;

    private final TemplateFileService templateFileService;

    @Override
    public Paging<TemplateVersionDTO> getPage(final TemplateVersionPageParam param) {
        return templateVersionManager.getPage(param);
    }

    @Override
    public TemplateVersionDTO getDetail(final Serializable id) {
        TemplateVersionDTO dto = templateVersionManager.getDtoById(id);
        if (dto == null) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception("版本不存在或已删除");
        }
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rollback(final Serializable versionId) {
        TemplateVersionDTO version = getDetail(versionId);
        TemplateFileDTO file = templateFileManager.getDtoById(version.getTemplateId());
        if (file == null) {
            throw DevkitErrorCode.TEMPLATE_FILE_NOT_FOUND.exception(
                "该版本对应的模板文件已被删除，无法回滚");
        }
        // saveFile 会先把「当前内容」落一版，再写入目标内容 → 回滚本身也可回滚
        boolean success = templateFileService.saveFile(new TemplateFileSaveParam()
            .setId(file.getId())
            .setContent(version.getContent())
            .setChangeLog("回滚到 v" + version.getVersionNo()));
        log.info("模板[{}]回滚到版本 v{}", file.getFilePath(), version.getVersionNo());
        return success;
    }
}
