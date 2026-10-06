package com.carlos.devkit.service;

import com.carlos.devkit.engine.model.ColumnMeta;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.pojo.dto.TemplateRenderPreviewDTO;
import com.carlos.devkit.pojo.param.CodeGenRunParam;
import com.carlos.devkit.pojo.param.TemplateRenderPreviewParam;

import java.util.List;

/**
 * <p>
 * 代码生成 业务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
public interface CodeGenService {

    /**
     * 列出目标实例下的库 / schema
     *
     * @param dsId 数据源ID
     * @return 库 / schema 清单
     */
    List<String> listSchemas(Long dsId);

    /**
     * 列出目标库下的表
     *
     * @param dsId    数据源ID
     * @param schema  库 / schema
     * @param keyword 表名关键字（可空）
     * @return 表清单
     */
    List<TableBaseInfo> listTables(Long dsId, String schema, String keyword);

    /**
     * 列出目标表的列元数据
     *
     * @param dsId      数据源ID
     * @param schema    库 / schema
     * @param tableName 表名
     * @return 列元数据
     */
    List<ColumnMeta> listColumns(Long dsId, String schema, String tableName);

    /**
     * 代码生成预览（产物在内存中，供文件树 + 代码高亮）
     *
     * @param param 生成参数
     * @return 生成产物
     */
    GeneratedResult preview(CodeGenRunParam param);

    /**
     * 代码生成并打包下载
     *
     * @param param 生成参数
     * @return zip 字节
     */
    byte[] generate(CodeGenRunParam param);

    /**
     * 用样例表渲染单个模板（在线编辑即时预览）
     *
     * @param param 预览参数
     * @return 渲染结果
     */
    TemplateRenderPreviewDTO previewTemplate(TemplateRenderPreviewParam param);
}
