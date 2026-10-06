package com.carlos.devkit.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.carlos.devkit.engine.codegen.CodeGenEngine;
import com.carlos.devkit.engine.codegen.CodeGenParam;
import com.carlos.devkit.engine.model.ColumnMeta;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.engine.model.TableMeta;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.render.FreemarkerRenderer;
import com.carlos.devkit.engine.support.JdbcMetaReader;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.pojo.dto.GenerateRecordDTO;
import com.carlos.devkit.pojo.dto.TemplateFileDTO;
import com.carlos.devkit.pojo.dto.TemplateRenderPreviewDTO;
import com.carlos.devkit.pojo.enums.TemplateTypeEnum;
import com.carlos.devkit.pojo.param.CodeGenRunParam;
import com.carlos.devkit.pojo.param.TemplateRenderPreviewParam;
import com.carlos.devkit.service.CodeGenService;
import com.carlos.devkit.service.DataSourceService;
import com.carlos.devkit.service.GenerateRecordService;
import com.carlos.devkit.service.TemplateFileService;
import com.carlos.devkit.service.TemplateGroupService;
import com.carlos.devkit.support.Sandbox;
import com.carlos.devkit.support.ZipPacker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 代码生成 业务接口实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeGenServiceImpl implements CodeGenService {

    private final DataSourceService dataSourceService;

    private final TemplateGroupService templateGroupService;

    private final TemplateFileService templateFileService;

    private final GenerateRecordService generateRecordService;

    private final CodeGenEngine codeGenEngine;

    private final JdbcMetaReader jdbcMetaReader;

    private final FreemarkerRenderer freemarkerRenderer;

    private final Sandbox sandbox;

    @Override
    public List<String> listSchemas(final Long dsId) {
        return jdbcMetaReader.listSchemas(dataSourceService.resolveTarget(dsId, null));
    }

    @Override
    public List<TableBaseInfo> listTables(final Long dsId, final String schema, final String keyword) {
        return jdbcMetaReader.listTables(dataSourceService.resolveTarget(dsId, schema), schema, keyword);
    }

    @Override
    public List<ColumnMeta> listColumns(final Long dsId, final String schema, final String tableName) {
        TargetDatabase target = dataSourceService.resolveTarget(dsId, schema);
        Connection connection = null;
        try {
            connection = jdbcMetaReader.openConnection(target);
            CodeGenParam param = new CodeGenParam().setUseTablePrefix(true);
            return codeGenEngine.readColumns(connection, tableName, param, null, new TableMeta());
        } catch (Exception e) {
            log.error("读取列元数据失败: {}/{}", schema, tableName, e);
            throw DevkitErrorCode.DATASOURCE_CONNECT_FAILED.exception(e.getMessage());
        } finally {
            jdbcMetaReader.release(connection);
        }
    }

    @Override
    public GeneratedResult preview(final CodeGenRunParam param) {
        return run(param);
    }

    @Override
    public byte[] generate(final CodeGenRunParam param) {
        GeneratedResult result = run(param);
        return ZipPacker.pack(result.getFiles());
    }

    @Override
    public TemplateRenderPreviewDTO previewTemplate(final TemplateRenderPreviewParam param) {
        TemplateFileDTO file = templateFileService.getDetail(param.getTemplateId());
        TemplateGroupMeta groupMeta = templateGroupService.getMeta(file.getGroupId());
        TargetDatabase target = dataSourceService.resolveTarget(param.getDsId(), param.getSchema());

        CodeGenParam genParam = new CodeGenParam()
            .setProjectName("preview")
            .setPackageName(StrUtil.blankToDefault(param.getPackageName(), "com.carlos.demo"))
            .setAuthor(StrUtil.blankToDefault(param.getAuthor(), "Carlos"))
            .setUseTablePrefix(!Boolean.FALSE.equals(param.getUseTablePrefix()))
            .setNameType(param.getNameType());

        Connection connection = null;
        Path work = sandbox.create();
        try {
            connection = jdbcMetaReader.openConnection(target);
            TableMeta tableMeta = codeGenEngine.readSingleTableMeta(
                connection, param.getTableName(), genParam, groupMeta);

            // 把该模板落盘后单独渲染
            Path templateFile = work.resolve(StrUtil.blankToDefault(file.getFileName(), "preview.ftl"));
            Files.writeString(templateFile, StrUtil.nullToEmpty(file.getContent()), StandardCharsets.UTF_8);

            Map<String, Object> params = new HashMap<>(4);
            params.put("project", genParam);
            params.put("table", tableMeta);
            params.put("database", target);
            String content = freemarkerRenderer.render(templateFile.toFile(), params);
            return new TemplateRenderPreviewDTO().setSuccess(Boolean.TRUE).setContent(content);
        } catch (Exception e) {
            log.warn("模板渲染预览失败: {}", e.getMessage());
            return new TemplateRenderPreviewDTO()
                .setSuccess(Boolean.FALSE)
                .setError(StrUtil.maxLength(e.getMessage(), 2000));
        } finally {
            jdbcMetaReader.release(connection);
            sandbox.destroy(work);
        }
    }

    /**
     * 执行代码生成（预览与下载共用），并落生成记录
     *
     * @param param 生成参数
     * @return 生成产物
     */
    private GeneratedResult run(final CodeGenRunParam param) {
        TemplateGroupMeta groupMeta = templateGroupService.getMeta(param.getGroupId());
        List<TemplateSourceFile> templates = templateFileService.listSourceFiles(param.getGroupId());
        TargetDatabase target = dataSourceService.resolveTarget(param.getDsId(), param.getSchema());

        CodeGenParam genParam = new CodeGenParam()
            .setProjectName(param.getProjectName())
            .setPackageName(param.getPackageName())
            .setPackageNameItems(StrUtil.split(param.getPackageName(), '.'))
            .setAuthor(StrUtil.blankToDefault(param.getAuthor(), "Carlos"))
            .setUseTablePrefix(!Boolean.FALSE.equals(param.getUseTablePrefix()))
            .setNameType(param.getNameType())
            .setTables(param.getTableNames().stream().map(name -> new TableBaseInfo(name, null)).toList())
            .setTemplateGroup(groupMeta);

        Path work = sandbox.create();
        Connection connection = null;
        try {
            connection = jdbcMetaReader.openConnection(target);
            GeneratedResult result = codeGenEngine.generate(
                groupMeta, groupMeta.getTemplateRoot(), templates, genParam, target, connection, work);
            record(param, groupMeta, target, result, null);
            return result;
        } catch (RuntimeException e) {
            record(param, groupMeta, target, null, e);
            throw e;
        } catch (Exception e) {
            record(param, groupMeta, target, null, e);
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception(e.getMessage());
        } finally {
            jdbcMetaReader.release(connection);
            sandbox.destroy(work);
        }
    }

    /**
     * 落生成记录
     *
     * @param param   生成参数
     * @param group   模板组
     * @param target  目标库
     * @param result  产物（失败时为空）
     * @param failure 失败异常（成功时为空）
     */
    private void record(final CodeGenRunParam param, final TemplateGroupMeta group,
                        final TargetDatabase target, final GeneratedResult result, final Exception failure) {
        GenerateRecordDTO record = new GenerateRecordDTO()
            .setBizType(TemplateTypeEnum.CODE.getCode())
            .setTemplateGroupId(group.getGroupId())
            .setTemplateGroupCode(group.getCode())
            .setTemplateGroupName(group.getName())
            .setDsId(param.getDsId())
            .setProjectName(param.getProjectName())
            .setPackageName(param.getPackageName())
            .setGenParams(JSON.toJSONString(param))
            .setStatus(failure == null ? 1 : 0)
            .setErrorMsg(failure == null ? null : failure.getMessage())
            .setFileCount(result == null ? 0 : result.getFiles().size())
            .setTotalSize(result == null ? 0L : result.getTotalSize())
            .setDurationMs(result == null ? 0L : result.getDurationMs());
        generateRecordService.record(record);
    }
}
