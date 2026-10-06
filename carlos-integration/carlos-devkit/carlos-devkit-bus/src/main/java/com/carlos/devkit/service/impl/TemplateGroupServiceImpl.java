package com.carlos.devkit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.pagination.Paging;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.manager.TemplateFileManager;
import com.carlos.devkit.manager.TemplateGroupManager;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.param.TemplateGroupCopyParam;
import com.carlos.devkit.pojo.param.TemplateGroupPageParam;
import com.carlos.devkit.service.TemplateGroupService;
import com.carlos.devkit.pojo.enums.DevkitStatusEnum;
import com.carlos.devkit.pojo.enums.TemplateTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 模板组 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateGroupServiceImpl implements TemplateGroupService {

    private final TemplateGroupManager templateGroupManager;

    private final TemplateFileManager templateFileManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addGroup(final TemplateGroupDTO dto) {
        if (templateGroupManager.countByCode(dto.getGroupCode(), null) > 0) {
            throw DevkitErrorCode.TEMPLATE_GROUP_CODE_EXISTS.exception(dto.getGroupCode());
        }
        dto.setBuiltIn(Boolean.FALSE);
        if (dto.getStatus() == null) {
            dto.setStatus(DevkitStatusEnum.ENABLED.getCode());
        }
        if (StrUtil.isBlank(dto.getTemplateRoot())) {
            dto.setTemplateRoot("template");
        }
        templateGroupManager.add(dto);
        log.info("新增模板组, id:{}, groupCode:{}", dto.getId(), dto.getGroupCode());
        return dto.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateGroup(final TemplateGroupDTO dto) {
        TemplateGroupDTO old = requireGroup(dto.getId());
        // groupCode 不可改；内置模板不允许改根目录名
        dto.setGroupCode(old.getGroupCode());
        if (Boolean.TRUE.equals(old.getBuiltIn()) && StrUtil.isNotBlank(dto.getTemplateRoot())
            && !StrUtil.equals(old.getTemplateRoot(), dto.getTemplateRoot())) {
            throw DevkitErrorCode.TEMPLATE_BUILT_IN_READONLY.exception("内置模板的根目录名不可修改");
        }
        return templateGroupManager.modify(dto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteGroup(final Serializable id) {
        TemplateGroupDTO old = requireGroup(id);
        if (Boolean.TRUE.equals(old.getBuiltIn())) {
            throw DevkitErrorCode.TEMPLATE_BUILT_IN_READONLY.exception(
                "内置模板组 [" + old.getGroupName() + "] 不可删除，如需下线请停用");
        }
        boolean success = templateGroupManager.delete(id);
        if (success) {
            templateFileManager.deleteByGroupId(id);
        }
        return success;
    }

    @Override
    public TemplateGroupDTO getDetail(final Serializable id) {
        TemplateGroupDTO dto = requireGroup(id);
        dto.setFileCount(templateFileManager.countByGroupIds(List.of(dto.getId()))
            .getOrDefault(dto.getId(), 0));
        return dto;
    }

    @Override
    public Paging<TemplateGroupDTO> getPage(final TemplateGroupPageParam param) {
        Paging<TemplateGroupDTO> paging = templateGroupManager.getPage(param);
        fillFileCount(paging.getRecords());
        return paging;
    }

    @Override
    public List<TemplateGroupDTO> listByType(final Integer templateType) {
        List<TemplateGroupDTO> list = templateGroupManager.listByType(templateType);
        fillFileCount(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyGroup(final TemplateGroupCopyParam param) {
        TemplateGroupDTO source = requireGroup(param.getId());
        if (templateGroupManager.countByCode(param.getNewGroupCode(), null) > 0) {
            throw DevkitErrorCode.TEMPLATE_GROUP_CODE_EXISTS.exception(param.getNewGroupCode());
        }
        TemplateGroupDTO target = new TemplateGroupDTO()
            .setGroupCode(param.getNewGroupCode())
            .setGroupName(param.getNewGroupName())
            .setDescription(source.getDescription())
            .setTemplateType(source.getTemplateType())
            .setTechStack(source.getTechStack())
            .setDbType(source.getDbType())
            .setTemplateRoot(source.getTemplateRoot())
            .setLogicDeleteFields(source.getLogicDeleteFields())
            .setVersionFields(source.getVersionFields())
            .setCommonFields(source.getCommonFields())
            .setBuiltIn(Boolean.FALSE)
            .setStatus(DevkitStatusEnum.ENABLED.getCode())
            .setSortNum(source.getSortNum())
            .setRemark("复制自 " + source.getGroupCode());
        templateGroupManager.add(target);

        if (Boolean.TRUE.equals(param.getCopyFiles())) {
            List<TemplateFileDTO> files = templateFileManager.listByGroupId(source.getId());
            for (TemplateFileDTO file : files) {
                TemplateFileDTO copy = new TemplateFileDTO()
                    .setGroupId(target.getId())
                    .setFilePath(file.getFilePath())
                    .setFileName(file.getFileName())
                    .setFileExt(file.getFileExt())
                    .setFileType(file.getFileType())
                    .setLoopFlag(file.getLoopFlag())
                    .setContentEncoding(file.getContentEncoding())
                    .setSortNum(file.getSortNum())
                    .setStatus(file.getStatus())
                    .setRemark(file.getRemark());
                // 复制时必须带上正文，listByGroupId 不返回正文，故逐条取详情
                TemplateFileDTO origin = templateFileManager.getDtoById(file.getId());
                copy.setContent(origin.getContent());
                copy.setFileSize(origin.getFileSize());
                copy.setMd5(origin.getMd5());
                templateFileManager.add(copy);
            }
        }
        log.info("复制模板组 {} -> {} (id:{})", source.getGroupCode(), param.getNewGroupCode(), target.getId());
        return target.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleStatus(final Serializable id, final Integer status) {
        requireGroup(id);
        return templateGroupManager.modify(new TemplateGroupDTO().setId((Long) id).setStatus(status));
    }

    @Override
    public TemplateGroupMeta getMeta(final Serializable groupId) {
        TemplateGroupDTO group = requireGroup(groupId);
        if (!DevkitStatusEnum.ENABLED.getCode().equals(group.getStatus())) {
            throw DevkitErrorCode.TEMPLATE_GROUP_DISABLED.exception(group.getGroupName());
        }
        return new TemplateGroupMeta()
            .setGroupId(group.getId())
            .setCode(group.getGroupCode())
            .setName(group.getGroupName())
            .setDescribe(group.getDescription())
            .setTemplateRoot(StrUtil.blankToDefault(group.getTemplateRoot(), "template"))
            .setLogicDeleteFields(splitFields(group.getLogicDeleteFields()))
            .setVersionFields(splitFields(group.getVersionFields()))
            .setCommonFields(splitFields(group.getCommonFields()));
    }

    /**
     * 校验模板组存在并返回
     *
     * @param id 模板组ID
     * @return 模板组 DTO
     */
    private TemplateGroupDTO requireGroup(final Serializable id) {
        TemplateGroupDTO dto = templateGroupManager.getDtoById(id);
        if (dto == null) {
            throw DevkitErrorCode.TEMPLATE_GROUP_NOT_FOUND.exception();
        }
        return dto;
    }

    /**
     * 批量回填模板文件数
     *
     * @param records 模板组列表
     */
    private void fillFileCount(final List<TemplateGroupDTO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> ids = records.stream().map(TemplateGroupDTO::getId).toList();
        Map<Long, Integer> countMap = templateFileManager.countByGroupIds(ids);
        records.forEach(item -> item.setFileCount(countMap.getOrDefault(item.getId(), 0)));
    }

    /**
     * 逗号分隔字段清单 → 集合
     *
     * @param fields 逗号分隔字符串
     * @return 字段集合
     */
    private Set<String> splitFields(final String fields) {
        if (StrUtil.isBlank(fields)) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        Arrays.stream(fields.split(","))
            .map(StrUtil::trim)
            .filter(StrUtil::isNotBlank)
            .forEach(result::add);
        return result;
    }
}
