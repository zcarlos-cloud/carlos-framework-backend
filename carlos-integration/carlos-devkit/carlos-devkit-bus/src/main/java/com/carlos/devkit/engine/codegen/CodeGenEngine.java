package com.carlos.devkit.engine.codegen;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.devkit.config.DevkitProperties;
import com.carlos.devkit.engine.DevkitConstants;
import com.carlos.devkit.engine.enums.CodeDirectEnum;
import com.carlos.devkit.engine.model.ColumnMeta;
import com.carlos.devkit.engine.model.GeneratedFile;
import com.carlos.devkit.engine.model.GeneratedResult;
import com.carlos.devkit.engine.model.RenderTemplateFile;
import com.carlos.devkit.engine.model.TableBaseInfo;
import com.carlos.devkit.engine.model.TableMeta;
import com.carlos.devkit.engine.model.TargetDatabase;
import com.carlos.devkit.engine.model.TemplateGroupMeta;
import com.carlos.devkit.engine.model.TemplateSourceFile;
import com.carlos.devkit.engine.render.FreemarkerRenderer;
import com.carlos.devkit.engine.support.JdbcMetaReader;
import com.carlos.devkit.engine.support.NameUtil;
import com.carlos.devkit.engine.support.OutputCollector;
import com.carlos.devkit.engine.support.TemplateFileParser;
import com.carlos.devkit.engine.support.TemplateMaterializer;
import com.carlos.devkit.engine.support.TypeConverter;
import com.carlos.devkit.enums.DevkitErrorCode;
import com.carlos.devkit.pojo.enums.FieldNameTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * 代码生成引擎
 * </p>
 * <p>
 * 迁移自 carlos-tools 原 {@code CodeGeneratorService}，渲染流程逐字保留：
 * 复制模板 → 改名项目根目录 → 注入包路径 → 读库表元数据 → 递归渲染。
 * 差异在于：
 * <ul>
 *   <li>模板来源为数据库（经 {@link TemplateMaterializer} 物化），不再解包 classpath；</li>
 *   <li>产物在请求级沙箱目录中生成，随后收集进内存（{@link OutputCollector}），不落业务磁盘；</li>
 *   <li>沙箱目录由调用方（Service）在 {@code finally} 中整体删除。</li>
 * </ul>
 * </p>
 *
 * @author Carlos
 * @date 2026年10月6日
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeGenEngine {

    private final DevkitProperties devkitProperties;

    private final TemplateMaterializer templateMaterializer;

    private final FreemarkerRenderer freemarkerRenderer;

    private final JdbcMetaReader jdbcMetaReader;

    /**
     * 执行代码生成
     *
     * @param groupMeta     模板组元数据
     * @param templateRoot  模板根目录名
     * @param templates     模板源文件（来自数据库）
     * @param param         生成参数
     * @param target        目标数据库（渲染上下文 database 项）
     * @param connection    目标库连接（由调用方负责关闭）
     * @param workDir       本次请求的沙箱目录
     * @return 生成产物
     */
    public GeneratedResult generate(final TemplateGroupMeta groupMeta, final String templateRoot,
                                    final List<TemplateSourceFile> templates, final CodeGenParam param,
                                    final TargetDatabase target, final Connection connection,
                                    final Path workDir) {
        long start = System.currentTimeMillis();
        try {
            // 1. 模板入库 → 落盘
            File materializedRoot = templateMaterializer.materialize(
                groupMeta.getCode(), templateRoot, templates, workDir);
            log.info("1.复制项目模板到指定的项目目录中");
            File outDir = workDir.resolve(DevkitConstants.OUTPUT_DIR).toFile();
            FileUtil.copy(materializedRoot, outDir, true);

            // 2. 修改工程根目录名为项目名
            String rootName = StrUtil.blankToDefault(param.getProjectName(), "generated");
            log.info("2.更改模板的目录名为项目名: {}", rootName);
            File projectRoot = FileUtil.rename(
                new File(outDir, materializedRoot.getName()), rootName, true);
            param.setOutputPath(projectRoot.getAbsolutePath());

            // 3. 把模板文件挪进包路径（模板源码树里不含包名目录）
            log.info("3.将模板文件移动至指定的包名路径：{}", param.getPackageName());
            moveTemplate2PackagePath(projectRoot, param.getPackageName());

            // 4. 读取库表元数据
            List<TableMeta> tableDetails = readTablesDetailInfo(connection, param, groupMeta);

            // 5. 渲染
            log.info("4.根据模板文件生成对应的项目文件");
            Map<String, Object> params = new HashMap<>(4);
            params.put(DevkitConstants.FTL_PARAM_KEY_DATABASE, target);
            params.put(DevkitConstants.FTL_PARAM_KEY_PROJECT, param);
            renderFiles(projectRoot, params, tableDetails);

            // 产物相对 outDir 收集：路径自带项目名目录，zip 解压后即为完整工程
            List<GeneratedFile> files = OutputCollector.collect(
                outDir, devkitProperties.getMaxFileCount(), devkitProperties.getMaxOutputSize());
            long totalSize = files.stream().mapToLong(GeneratedFile::getSize).sum();
            long duration = System.currentTimeMillis() - start;
            log.info("代码生成完成：文件 {} 个，共 {} 字节，耗时 {}ms", files.size(), totalSize, duration);
            return new GeneratedResult(rootName, files, totalSize, duration);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("代码生成失败", e);
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception(e.getMessage());
        }
    }

    /**
     * 将模板中 {@code src/main/java} 下的文件挪到指定包名目录
     *
     * @param projectRoot 项目根目录
     * @param packageName 包名
     */
    private void moveTemplate2PackagePath(final File projectRoot, final String packageName) {
        File source = new File(projectRoot.getPath() + DevkitConstants.SRC_MAIN_JAVA);
        if (!source.exists()) {
            log.warn("模板中不存在 src/main/java 目录，跳过包路径注入：{}", projectRoot.getPath());
            return;
        }
        File temp = new File(projectRoot.getPath() + DevkitConstants.SRC_MAIN_TEMP);
        File target = new File(temp, StrUtil.nullToEmpty(packageName).replace(StrUtil.DOT, File.separator));
        try {
            log.debug("3.1.将模板文件移动到临时目录：{}", target.getAbsolutePath());
            FileUtils.moveDirectory(source, target);
        } catch (IOException e) {
            log.error("文件移动失败！ {} -> {}", source.getPath(), target.getPath(), e);
            throw DevkitErrorCode.GENERATE_ENGINE_ERROR.exception("包路径注入失败");
        }
        log.debug("3.2.修改临时目录为正式的Java文件目录");
        FileUtil.rename(temp, "java", true);
    }

    /**
     * 递归遍历目录，把 .ftl 渲染成目标文件
     *
     * @param projectRoot 项目根目录
     * @param params      渲染参数
     * @param tableMetas  表元数据
     */
    private void renderFiles(final File projectRoot, final Map<String, Object> params,
                             final List<TableMeta> tableMetas) {
        File[] files = projectRoot.listFiles();
        if (files == null) {
            log.debug("目录为空:{}", projectRoot.getPath());
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                log.debug("进入目录:{}", file.getPath());
                renderFiles(file, params, tableMetas);
                continue;
            }
            RenderTemplateFile templateFile = TemplateFileParser.parse(file);
            if (templateFile == null) {
                continue;
            }
            if (templateFile.isLoop()) {
                for (TableMeta tableMeta : tableMetas) {
                    params.put(DevkitConstants.FTL_PARAM_KEY_TABLE, tableMeta);
                    templateFile.setTargetName(StrUtil.replace(
                        templateFile.getPreName(), CodeDirectEnum.BASE.getValue(), tableMeta.getClassPrefix()));
                    renderOne(params, templateFile);
                }
            } else {
                templateFile.setTargetName(templateFile.getPreName());
                renderOne(params, templateFile);
            }
            if (file.delete()) {
                log.debug("删除模板文件:{}", file.getPath());
            }
        }
    }

    /**
     * 渲染单个模板文件
     *
     * @param params       渲染参数
     * @param templateFile 模板文件
     */
    private void renderOne(final Map<String, Object> params, final RenderTemplateFile templateFile) {
        File target = new File(templateFile.getPath(), templateFile.getTargetName());
        freemarkerRenderer.renderToFile(templateFile.getFile(), params, target);
    }

    /**
     * 读取选中表的完整元数据
     *
     * @param connection 目标库连接
     * @param param      生成参数
     * @param groupMeta  模板组元数据
     * @return 表元数据清单
     */
    private List<TableMeta> readTablesDetailInfo(final Connection connection, final CodeGenParam param,
                                                 final TemplateGroupMeta groupMeta) throws SQLException {
        List<TableMeta> list = new ArrayList<>();
        List<TableBaseInfo> tables = param.getTables();
        if (CollUtil.isEmpty(tables)) {
            throw DevkitErrorCode.CODEGEN_NO_TABLE_SELECTED.exception();
        }
        for (TableBaseInfo tableBaseInfo : tables) {
            String tableName = tableBaseInfo.getName();
            if (StrUtil.containsAny(tableName, "=", "$")) {
                continue;
            }
            TableMeta tableMeta = new TableMeta();
            tableMeta.setComment(StrUtil.removeAll(
                StrUtil.nullToEmpty(tableBaseInfo.getComment()), CharPool.CR, CharPool.LF, CharPool.TAB));
            log.debug("开始获取表信息：{}", tableName);

            List<String> items = StrUtil.split(tableName, CharUtil.UNDERLINE);
            tableMeta.setModule(items.size() == 1 ? "" : items.getFirst());
            tableMeta.setName(tableName.toLowerCase());
            tableMeta.setClassPrefix(NameUtil.getClassNamePrefix(tableName, param.isUseTablePrefix()));
            tableMeta.setClassPropertyPrefix(NameUtil.getClassPropertyNamePrefix(tableMeta.getClassPrefix()));
            tableMeta.setClassMainPrefix(StrUtil.lowerFirst(
                StrUtil.removePrefix(tableMeta.getClassPropertyPrefix(), tableMeta.getModule())));
            tableMeta.setApiPath(NamingCase.toSymbolCase(tableMeta.getClassPrefix(), CharUtil.SLASH));
            tableMeta.setColumns(readColumns(connection, tableName, param, groupMeta, tableMeta));
            list.add(tableMeta);
        }
        return list;
    }

    /**
     * 读取单表完整元数据（供预览 / 列清单接口复用）
     *
     * @param connection 目标库连接
     * @param tableName  表名
     * @param param      生成参数
     * @param groupMeta  模板组元数据（可空，空表示不做字段标记）
     * @return 表元数据
     * @throws SQLException 元数据读取失败
     */
    public TableMeta readSingleTableMeta(final Connection connection, final String tableName,
                                         final CodeGenParam param, final TemplateGroupMeta groupMeta)
        throws SQLException {
        TableMeta tableMeta = new TableMeta();
        tableMeta.setComment(StrUtil.EMPTY);
        List<String> items = StrUtil.split(tableName, CharUtil.UNDERLINE);
        tableMeta.setModule(items.size() == 1 ? "" : items.getFirst());
        tableMeta.setName(tableName.toLowerCase());
        tableMeta.setClassPrefix(NameUtil.getClassNamePrefix(tableName, param.isUseTablePrefix()));
        tableMeta.setClassPropertyPrefix(NameUtil.getClassPropertyNamePrefix(tableMeta.getClassPrefix()));
        tableMeta.setClassMainPrefix(StrUtil.lowerFirst(
            StrUtil.removePrefix(tableMeta.getClassPropertyPrefix(), tableMeta.getModule())));
        tableMeta.setApiPath(NamingCase.toSymbolCase(tableMeta.getClassPrefix(), CharUtil.SLASH));
        tableMeta.setColumns(readColumns(connection, tableName, param, groupMeta, tableMeta));
        return tableMeta;
    }

    /**
     * 读取单表列元数据
     *
     * @param connection 目标库连接
     * @param tableName  表名
     * @param param      生成参数
     * @param groupMeta  模板组元数据（可空，空表示不做字段标记）
     * @param tableMeta  表元数据（回填主键 / 逻辑删除字段）
     * @return 列元数据
     */
    public List<ColumnMeta> readColumns(final Connection connection, final String tableName,
                                        final CodeGenParam param, final TemplateGroupMeta groupMeta,
                                        final TableMeta tableMeta) throws SQLException {
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        DatabaseMetaData metaData = connection.getMetaData();

        Set<String> keys = new HashSet<>();
        try (ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName)) {
            while (primaryKeys.next()) {
                String keyName = primaryKeys.getString("COLUMN_NAME");
                if (keyName.toUpperCase().equals(keyName)) {
                    keyName = keyName.toLowerCase();
                }
                keys.add(keyName);
            }
        }

        List<ColumnMeta> columnList = new ArrayList<>();
        Set<String> imports = new HashSet<>();
        Set<String> logicFields = groupMeta == null ? Set.of() : groupMeta.getLogicDeleteFields();
        Set<String> commonFields = groupMeta == null ? Set.of() : groupMeta.getCommonFields();
        Set<String> versionFields = groupMeta == null ? Set.of() : groupMeta.getVersionFields();
        try (ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, null)) {
            while (resultSet.next()) {
                ColumnMeta column = new ColumnMeta();
                String columnName = resultSet.getString("COLUMN_NAME");
                column.setAutoIncrement("YES".equals(resultSet.getString("IS_AUTOINCREMENT")));
                column.setNullable(!"0".equals(resultSet.getString("NULLABLE")));
                String columnDbType = resultSet.getString("TYPE_NAME");
                column.setColumnName(columnName);
                column.setColumnDbType(columnDbType);
                column.setColumnDefault(resultSet.getString("COLUMN_DEF"));
                column.setDecimalDigits(resultSet.getInt("DECIMAL_DIGITS"));
                column.setColumnsSize(resultSet.getInt("COLUMN_SIZE"));

                String fullJavaType = TypeConverter.getFullJavaType(columnDbType);
                imports.add(fullJavaType);
                column.setColumnType(fullJavaType);

                String remarks = resultSet.getString("REMARKS");
                if (remarks == null) {
                    remarks = columnName;
                }
                column.setColumnComment(StrUtil.removeAll(remarks, CharPool.CR, CharPool.LF, CharPool.TAB));

                if (keys.contains(columnName)) {
                    column.setPrimaryKey(true);
                    tableMeta.setPrimaryKey(columnName);
                }

                boolean logicDelete = contains(logicFields, columnName);
                if (logicDelete) {
                    tableMeta.setLogicDelete(columnName);
                    column.setLogicField(true);
                }
                // 逻辑删除与 is_ 前缀字段统一按 Boolean 处理，并去掉 is_ 前缀
                if (logicDelete || StrUtil.startWith(columnName, DevkitConstants.BOOLEAN_PREFIX)) {
                    fullJavaType = "java.lang.Boolean";
                    columnName = StrUtil.removePrefixIgnoreCase(columnName, DevkitConstants.BOOLEAN_PREFIX);
                }
                column.setJavaType(StrUtil.subAfter(fullJavaType, CharUtil.DOT, true));
                column.setPropertyName(resolvePropertyName(columnName, param.getNameType()));
                column.setPropertyNameUp(NameUtil.getPropertyNameUp(column.getPropertyName()));
                column.setCommonField(contains(commonFields, columnName));
                column.setVersionField(contains(versionFields, columnName));
                columnList.add(column);
            }
        }
        tableMeta.setImports(imports);
        return columnList;
    }

    /**
     * 按命名方式解析属性名
     *
     * @param columnName 列名
     * @param nameType   命名方式
     * @return 属性名
     */
    private String resolvePropertyName(final String columnName, final FieldNameTypeEnum nameType) {
        if (nameType == null) {
            return columnName;
        }
        return switch (nameType) {
            case NOT_PREFIX_AND_UNDERLINE -> NameUtil.underLineToCamel(columnName);
            case NOT_PREFIX_AND_CAMEL -> columnName;
            case PREFIX_AND_UNDERLINE -> NameUtil.delPrefixToCamel(columnName);
            case PREFIX_AND_CAMEL -> NameUtil.delPrefix(columnName);
            case ALL_UPPER -> NameUtil.upperToLowerAndCamel(columnName);
        };
    }

    /**
     * 判断字段名是否在给定清单中
     *
     * @param fields     字段清单
     * @param columnName 列名
     * @return true 表示命中
     */
    private boolean contains(final Set<String> fields, final String columnName) {
        return CollUtil.isNotEmpty(fields) && fields.contains(columnName);
    }
}
