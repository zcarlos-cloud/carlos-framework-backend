package com.carlos.devkit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.projectgen.ProjectGenEngine;
import com.carlos.devkit.engine.projectgen.ProjectGenParam;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.dto.TemplateGroupDTO;
import com.carlos.devkit.pojo.enums.TemplateTypeEnum;
import com.carlos.devkit.pojo.param.ProjectGenRunParam;
import com.carlos.devkit.service.GenerateRecordService;
import com.carlos.devkit.service.ProjectGenService;
import com.carlos.devkit.service.TemplateFileService;
import com.carlos.devkit.service.TemplateGroupService;
import com.carlos.devkit.support.Sandbox;
import com.carlos.devkit.support.ZipPacker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * <p>
 * 工程生成 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectGenServiceImpl implements ProjectGenService {

    private final TemplateGroupService templateGroupService;

    private final TemplateFileService templateFileService;

    private final GenerateRecordService generateRecordService;

    private final ProjectGenEngine projectGenEngine;

    private final Sandbox sandbox;

    @Override
    public List<TemplateGroupDTO> listTemplates() {
        return templateGroupService.listByType(TemplateTypeEnum.PROJECT.getCode());
    }

    @Override
    public GeneratedResult preview(final ProjectGenRunParam param) {
        return run(param);
    }

    @Override
    public byte[] generate(final ProjectGenRunParam param) {
        GeneratedResult result = run(param);
        return ZipPacker.pack(result.getFiles());
    }

    /**
     * 执行工程生成（预览与下载共用），并落生成记录
     *
     * @param param 生成参数
     * @return 生成产物
     */
    private GeneratedResult run(final ProjectGenRunParam param) {
        TemplateGroupMeta groupMeta = templateGroupService.getMeta(param.getTemplateGroupId());
        if (!TemplateTypeEnum.PROJECT.getCode().equals(
            templateGroupService.getDetail(param.getTemplateGroupId()).getTemplateType())) {
            throw DevkitErrorCode.GENERATE_PARAM_INVALID.exception(
                "模板组 [" + groupMeta.getName() + "] 不是工程生成模板");
        }
        List<TemplateSourceFile> templates = templateFileService.listSourceFiles(param.getTemplateGroupId());

        ProjectGenParam genParam = new ProjectGenParam()
            .setProjectName(StrUtil.blankToDefault(param.getProjectName(), param.getArtifactId()))
            .setDescribe(StrUtil.nullToEmpty(param.getDescribe()))
            .setGroupId(param.getMavenGroupId())
            .setPackageNameItems(StrUtil.split(param.getMavenGroupId(), '.'))
            .setArtifactId(param.getArtifactId())
            .setVersion(StrUtil.blankToDefault(param.getVersion(), "1.0.0-SNAPSHOT"))
            .setPackageName(param.getMavenGroupId())
            .setAuthor(StrUtil.blankToDefault(param.getAuthor(), "Carlos"))
            .setTemplateGroup(groupMeta);

        Path work = sandbox.create();
        try {
            GeneratedResult result = projectGenEngine.generate(
                groupMeta, groupMeta.getTemplateRoot(), templates, genParam, work);
            record(param, groupMeta, result, null);
            return result;
        } catch (RuntimeException e) {
            record(param, groupMeta, null, e);
            throw e;
        } finally {
            sandbox.destroy(work);
        }
    }

    /**
     * 落生成记录
     *
     * @param param   生成参数
     * @param group   模板组
     * @param result  产物（失败时为空）
     * @param failure 失败异常（成功时为空）
     */
    private void record(final ProjectGenRunParam param, final TemplateGroupMeta group,
                        final GeneratedResult result, final Exception failure) {
        GenerateRecordDTO record = new GenerateRecordDTO()
            .setBizType(TemplateTypeEnum.PROJECT.getCode())
            .setTemplateGroupId(group.getGroupId())
            .setTemplateGroupCode(group.getCode())
            .setTemplateGroupName(group.getName())
            .setProjectName(StrUtil.blankToDefault(param.getProjectName(), param.getArtifactId()))
            .setPackageName(param.getMavenGroupId())
            .setGenParams(JSON.toJSONString(param))
            .setStatus(failure == null ? 1 : 0)
            .setErrorMsg(failure == null ? null : failure.getMessage())
            .setFileCount(result == null ? 0 : result.getFiles().size())
            .setTotalSize(result == null ? 0L : result.getTotalSize())
            .setDurationMs(result == null ? 0L : result.getDurationMs());
        generateRecordService.record(record);
    }
}
