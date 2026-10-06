package com.carlos.fx.codege.service;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.NamingCase;
import cn.hutool.core.util.CharUtil;
import cn.hutool.core.util.StrUtil;
import com.carlos.fx.codege.config.CodeGenerateInfo;
import com.carlos.fx.codege.config.CodegeConstant;
import com.carlos.fx.codege.config.DatabaseInfo;
import com.carlos.fx.codege.entity.*;
import com.carlos.fx.codege.enums.CodeDirectEnum;
import com.carlos.fx.utils.DatabaseUtil;
import com.carlos.fx.utils.NameUtil;
import com.carlos.fx.utils.TemplateUtil;
import com.carlos.fx.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * <p>
 * 代码生成类
 * </p>
 *
 * @author Carlos
 * @date 2019/10/19 23:45 ---------     -------------   --------------------------------------
 */
@Slf4j
@Service
@Scope("prototype")
public class CodeGeneratorService {

    /**
     * 数据库字段类型和Java数据类型映射
     */
    private static final Map<String, String> CONVERT_MAP = XmlUtils.readConvertXml();

    public void createObject(DatabaseInfo databaseInfo, CodeGenerateInfo codeGenerateInfo) throws Exception {
        log.info("0.代码生成中.................................................................................");
        try {
            TemplateBaseInfo templateBaseInfo = codeGenerateInfo.getTemplateBaseInfo();
            File templatePath = new File(templateBaseInfo.getPath() + File.separator + CodegeConstant.TEMPLATE_MAIN);
            File projectPath = new File(codeGenerateInfo.getOutputPath());
            // 复制模板文件到项目目录
            log.info("1.复制项目模板到指定的项目目录中");
            FileUtil.copy(templatePath, projectPath, true);
            // 修改项目名称 工程根目录
            log.info("2.更改模板的目录名为项目名");
            File projectRoot = FileUtil.rename(new File(codeGenerateInfo.getOutputPath() + File.separator + CodegeConstant.TEMPLATE_MAIN),
                    codeGenerateInfo.getProjectName(), true);
            // 创建基础包 并且把模板文件放入包中(因为模板中不包含包名路径)
            log.info("3.将模板文件移动至指定的包名路径：{}{}{}", codeGenerateInfo.getPackageName(), StrUtil.DOT, codeGenerateInfo.getPackageName());
            moveTemplate2PackagePath(projectRoot, codeGenerateInfo.getPackageName());

            //

            List<TableBaseInfo> tables = codeGenerateInfo.getTables();
            List<ObjectInfo> tableDetails = getTablesDetailInfo(databaseInfo, codeGenerateInfo, tables);

            // 使用注入的DatabaseService，传入databaseInfo和templateConfig
            log.info("4.根据模板文件生成对应的项目文件");
            // 根据模板文件生成对应的Java文件
            Map<String, Object> map = new HashMap<>(4);
            map.put(CodegeConstant.FTL_PARAM_KEY_DATABASE, databaseInfo);
            map.put(CodegeConstant.FTL_PARAM_KEY_PROJECT, codeGenerateInfo);


            createFile(projectRoot, map, tableDetails);
        } catch (Exception e) {
            log.error("代码生成失败！", e);
            throw new Exception(e);
        }

    }


    /**
     * 将模板中源码 Java文件夹下的所有文件放入到指定的目录下，比如 java/com/carlos/test 目录下
     *
     * @param projectRootPath 项目的根目录
     */
    public void moveTemplate2PackagePath(File projectRootPath, String packageName) {
        // TODO: Carlos 2020/9/9 如果是多模块模板  这个目录下会有多个src/main/java目录
        File source = new File(projectRootPath.getPath() + CodegeConstant.SRC_MAIN_JAVA);
        File target = new File(projectRootPath.getPath() + CodegeConstant.SRC_MAIN_TEMP + packageName.replace(StrUtil.DOT, File.separator));
        try {
            log.debug("3.1.将模板文件移动到临时目录：{}", target.getAbsolutePath());
            FileUtils.moveDirectory(source, target);
        } catch (IOException e) {
            log.error("文件移动失败！ {}->{}", source.getPath(), target.getPath());
        }
        // 修改临时目录为正式的项目目录
        log.debug("3.2.修改临时目录为正式的Java文件目录");
        FileUtil.rename(new File(projectRootPath.getPath() + CodegeConstant.SRC_MAIN_TEMP), "java", true);
    }

    /**
     * 递归遍历父目录下的模板文件
     *
     * @param projectRoot 项目的根目录
     */
    private void createFile(File projectRoot, Map<String, Object> params, List<ObjectInfo> objs) {
        File[] files = projectRoot.listFiles();
        if (files == null) {
            log.debug("目录为空:{}", projectRoot.getPath());
            return;
        }

        TemplateInfo templateInfo;
        for (File file : files) {
            if (file.isDirectory()) {
                log.debug("进入目录:{}", file.getPath());
                // 递归目录
                createFile(file, params, objs);
            }
            // 将模板文件处理成对应的文件
            log.debug("开始处理模板文件：{}", file.getPath());
            templateInfo = TemplateUtil.dealTemplateFile(file);
            if (templateInfo == null) {
                continue;
            }
            if (templateInfo.isLoop()) {
                // 如果文件需要循环处理
                for (ObjectInfo obj : objs) {
                    params.put(CodegeConstant.FTL_PARAM_KEY_TABLE, obj);
                    templateInfo.setTargetName(StrUtil.replace(templateInfo.getPreName(), CodeDirectEnum.BASE.getValue(), obj.getClassPrefix()));
                    TemplateUtil.createClassFile(params, templateInfo);

                }
            } else {
                templateInfo.setTargetName(templateInfo.getPreName());
                TemplateUtil.createClassFile(params, templateInfo);
            }
            if (templateInfo.getFile().delete()) {
                log.info("删除模板文件:{}", templateInfo.getFile().getPath());
            }
        }
    }


    /**
     * 获取数据库中相关的表信息
     *
     * @return List<Table> 返回数据库中所有的表信息
     * @author Carlos
     * @date 2019/4/12 17:01
     */
    public List<ObjectInfo> getTablesDetailInfo(DatabaseInfo databaseInfo, CodeGenerateInfo codeGenerateInfo, List<TableBaseInfo> tables) throws SQLException {
        List<ObjectInfo> list = new ArrayList<>();
        ObjectInfo objectInfo;
        // 表名
        List<ColumnBean> columnList;
        for (TableBaseInfo tableBaseInfo : tables) {
            objectInfo = new ObjectInfo();
            String tableName = tableBaseInfo.getName();
            objectInfo.setComment(StrUtil.removeAll(tableBaseInfo.getComment(), CharPool.CR, CharPool.LF, CharPool.TAB));
            log.info("开始获取表信息：{}", tableName);

            // 排除部分表
            if (tableName.contains("=") || tableName.contains("$")) {
                continue;
            }

            List<String> items = StrUtil.split(tableName, CharUtil.UNDERLINE);


            boolean useTablePrefix = codeGenerateInfo.isUseTablePrefix();
            if (items.size() == 1) {
                objectInfo.setModule("");
            } else {
                objectInfo.setModule(items.getFirst());
            }


            // if (useTablePrefix) {
            //     table.setBeanName("");
            //     table.setBeanPropertyName("");
            //     table.setSimplePropertyName("");
            //     table.setApiPath("");
            // }
            // 设置模块名


            objectInfo.setName(tableName.toLowerCase());

            objectInfo.setClassPrefix(NameUtil.getClassNamePrefix(tableName, codeGenerateInfo.isUseTablePrefix()));
            objectInfo.setClassPropertyPrefix(NameUtil.getClassPropertyNamePrefix(objectInfo.getClassPrefix()));
            objectInfo.setClassMainPrefix(StrUtil.lowerFirst(StrUtil.removePrefix(objectInfo.getClassPropertyPrefix(), objectInfo.getModule())));
            objectInfo.setApiPath(NamingCase.toSymbolCase(objectInfo.getClassPrefix(), CharUtil.SLASH));

            columnList = getColumnList(tableName, databaseInfo, codeGenerateInfo, objectInfo);
            // 获取表字段信息
            objectInfo.setColumns(columnList);
            list.add(objectInfo);
        }
        return list;
    }

    /**
     * 获取表的列信息
     *
     * @author Carlos
     * @date 2019/12/26 13:42
     */
    private List<ColumnBean> getColumnList(String tableName, DatabaseInfo databaseInfo, CodeGenerateInfo codeGenerateInfo,
                                           ObjectInfo objInfo) throws SQLException {
        TemplateBaseInfo templateBaseInfo = codeGenerateInfo.getTemplateBaseInfo();
        Connection connection = databaseInfo.getConnection();

        String catalog = connection.getCatalog();
        String schema = connection.getSchema();
        DatabaseMetaData metaData = connection.getMetaData();


        // 存储主键字段名
        List<String> keys = new ArrayList<>();
        // 获取主键描述
        ResultSet primaryKeys = metaData.getPrimaryKeys(catalog, schema, tableName);
        while (primaryKeys.next()) {
            String keyname = primaryKeys.getString("COLUMN_NAME");
            if (keyname.toUpperCase().equals(keyname)) {
                keyname = keyname.toLowerCase();
            }
            keys.add(keyname);
        }

        // 表列
        ResultSet resultSet = metaData.getColumns(catalog, schema, tableName, null);
        List<ColumnBean> columnList = new ArrayList<>();
        ColumnBean column;
        // 列名
        String columnName;
        // 属性名
        String propertyName;
        // 列类型
        String columnDbType;
        // 对应的Java数据类型
        String fullJavaType;
        // 备注
        String remarks;
        // 自增
        boolean autoIncrement;
        // 是否允许空值
        boolean nullable;
        // 字段默认值
        String columnDefault;
        // 小数位数
        int decimalDigits;
        // 列大小
        int columnSize;
        Set<String> imports = new HashSet<>();
        while (resultSet.next()) {
            column = new ColumnBean();
            columnName = resultSet.getString("COLUMN_NAME");
            autoIncrement = "YES".equals(resultSet.getString("IS_AUTOINCREMENT"));
            nullable = !"0".equals(resultSet.getString("NULLABLE"));
            columnDbType = resultSet.getString("TYPE_NAME");
            columnDefault = resultSet.getString("COLUMN_DEF");
            remarks = resultSet.getString("REMARKS");
            decimalDigits = resultSet.getInt("DECIMAL_DIGITS");
            columnSize = resultSet.getInt("COLUMN_SIZE");
            column.setAutoIncrement(autoIncrement);
            column.setNullable(nullable);
            column.setColumnName(columnName);
            column.setColumnDbType(columnDbType);
            // 获取数据库数据类型对应的Java数据类型 全包名路径
            fullJavaType = getColumnFullJavaType(columnDbType);
            imports.add(fullJavaType);
            column.setColumnType(fullJavaType);
            // 如果列备注为空，则使用字段名称作为备注名
            if (remarks == null) {
                remarks = columnName;
                // 去除备注中的换行符
            }
            column.setColumnComment(StrUtil.removeAll(remarks, CharPool.CR, CharPool.LF, CharPool.TAB));
            // 主键列处理
            if (keys.contains(columnName)) {
                column.setPrimaryKey(true);
                objInfo.setPrimaryKey(columnName);
            } else {
                column.setPrimaryKey(false);
            }

            boolean logicDelete = checkLogicDeleteField(columnName, templateBaseInfo.getLogicDeleteFields());

            if (logicDelete) {
                // 设置表的逻辑删除字段
                objInfo.setLogicDelete(columnName);
                column.setLogicField(logicDelete);
            }

            // 逻辑删除字段处理
            if (logicDelete || checkBooleanField(columnName)) {
                fullJavaType = "java.long.Boolean";
                // 去除数据库字段中的is_前缀， is前缀在Java类型为boolean时有时候会出错
                columnName = StrUtil.removePrefixIgnoreCase(columnName, CodegeConstant.BOOLEAN_PREFIX);
            }

            column.setJavaType(StrUtil.subAfter(fullJavaType, CharUtil.DOT, true));

            // 针对键类型字段，如id  xxx_id 字段，统一采用Serializable类型，方便进行id类型更改，提升系统兼容性
            checkIdColumn(column);
            column.setDecimalDigits(decimalDigits);
            column.setColumnsSize(columnSize);
            column.setColumnDefault(columnDefault);
            propertyName = switch (codeGenerateInfo.getNameType()) {
                case NOT_PREFIX_AND_UNDERLINE -> NameUtil.underLineToCamel(columnName);
                case NOT_PREFIX_AND_CAMEL -> columnName;
                case PREFIX_AND_UNDERLINE -> NameUtil.delPrefixToCamel(columnName);
                case PREFIX_AND_CAMEL -> NameUtil.delPrefix(columnName);
                case ALL_UPPER -> NameUtil.upperToLowerAndCamel(columnName);
                default -> columnName;
            };
            // 设置数据库列名对应的属性名
            column.setPropertyName(propertyName);
            column.setPropertyNameUp(NameUtil.getPropertyNameUp(column.getPropertyName()));
            column.setCommonField(checkCommonField(columnName, templateBaseInfo.getCommonFields()));
            column.setVersionField(checkVersionField(columnName, templateBaseInfo.getVersionFields()));
            columnList.add(column);
        }
        objInfo.setImports(imports);
        DatabaseUtil.release(connection, resultSet);
        return columnList;
    }

    private void checkIdColumn(ColumnBean column) {
        String columnName = column.getColumnName();
        // if (column.isPrimaryKey() || columnName.endsWith("_id")) {
        //     String className = ClassUtil.getClassName(Serializable.class, true);
        //     column.setJavaType(className);
        // }
    }

    /**
     * 检查逻辑删除字段
     *
     * @param columnName 数据库字段名
     * @return boolean
     * @author Carlos
     * @date 2021/11/22 16:22
     */
    private boolean checkLogicDeleteField(String columnName, TemplateBaseInfo.Field fields) {
        if (fields == null) {
            return false;
        }
        Set<String> name = fields.getName();
        if (CollUtil.isEmpty(name)) {
            return false;
        }
        return name.contains(columnName);
    }

    /**
     * 检查是否是boolean类型字段
     *
     * @param columnName 列名
     * @return boolean
     * @author Carlos
     * @date 2021/12/28 15:42
     */
    private boolean checkBooleanField(String columnName) {
        return columnName.startsWith(CodegeConstant.BOOLEAN_PREFIX);
    }

    /**
     * 检查字段是否是通用字段
     *
     * @param columnName 列名
     * @return boolean
     * @author Carlos
     * @date 2021/11/22 15:20
     */
    private static boolean checkCommonField(String columnName, TemplateBaseInfo.Field fields) {
        if (fields == null) {
            return false;
        }
        Set<String> name = fields.getName();
        if (CollUtil.isEmpty(name)) {
            return false;
        }
        return name.contains(columnName);
    }

    /**
     * 检查字段是否是版本字段
     *
     * @param columnName 列名
     * @return boolean
     */
    private static boolean checkVersionField(String columnName, TemplateBaseInfo.Field fields) {
        if (fields == null) {
            return false;
        }
        Set<String> name = fields.getName();
        if (CollUtil.isEmpty(name)) {
            return false;
        }
        return name.contains(columnName);
    }

    /**
     * 获取数据库字段对应的Java类型
     *
     * @param columnDbType 数据库数据类型
     * @author Carlos
     * @date 2019/12/31 13:45
     */
    private static String getColumnFullJavaType(String columnDbType) {
        String s = CONVERT_MAP.get(columnDbType);
        if (StrUtil.isBlank(s)) {
            s = "java.lang.String";
        }
        return s;
    }

}
