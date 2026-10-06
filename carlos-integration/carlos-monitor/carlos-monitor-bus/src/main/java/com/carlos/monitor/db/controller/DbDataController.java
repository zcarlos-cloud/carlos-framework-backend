package com.carlos.monitor.db.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.carlos.core.response.Result;
import com.carlos.monitor.db.pojo.dto.DbColumnRow;
import com.carlos.monitor.db.pojo.dto.DbTableRow;
import com.carlos.monitor.db.pojo.dto.SqlExecResult;
import com.carlos.monitor.db.pojo.vo.SqlExecResultVO;
import com.carlos.monitor.db.proxy.DbDataProxy;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 数据库元数据与表数据浏览 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/monitor/db")
@Tag(name = "数据库数据浏览")
public class DbDataController {

    private final DbDataProxy dbDataProxy;

    @PreAuthorize("hasPermission('monitor:db:data', 'view')")
    @GetMapping("/{id}/metadata/schemas")
    @Operation(summary = "schema 列表")
    public Result<List<String>> schemas(@PathVariable("id") Long id) {
        return Result.success(dbDataProxy.listSchemas(id));
    }

    @PreAuthorize("hasPermission('monitor:db:data', 'view')")
    @GetMapping("/{id}/metadata/{schema}/tables")
    @Operation(summary = "表列表")
    public Result<List<DbTableRow>> tables(@PathVariable("id") Long id, @PathVariable("schema") String schema) {
        return Result.success(dbDataProxy.listTables(id, schema));
    }

    @PreAuthorize("hasPermission('monitor:db:data', 'view')")
    @GetMapping("/{id}/metadata/tables/{table}/columns")
    @Operation(summary = "列列表")
    public Result<List<DbColumnRow>> columns(@PathVariable("id") Long id,
                                             @PathVariable("table") String table,
                                             @RequestParam("schema") String schema) {
        return Result.success(dbDataProxy.listColumns(id, schema, table));
    }

    @PreAuthorize("hasPermission('monitor:db:data', 'view')")
    @GetMapping("/{id}/metadata/tables/{table}/ddl")
    @Operation(summary = "建表 DDL 查看")
    public Result<String> tableDdl(@PathVariable("id") Long id,
                                   @PathVariable("table") String table,
                                   @RequestParam("schema") String schema) {
        return Result.success(dbDataProxy.getTableDdl(id, schema, table));
    }

    @PreAuthorize("hasPermission('monitor:db:data', 'view')")
    @GetMapping("/{id}/table-data")
    @Operation(summary = "表数据分页浏览", description = "强制分页 + 列名白名单 + 敏感列脱敏；"
        + "conditions 为 JSON 数组：[{\"column\":\"name\",\"op\":\"eq|like\",\"value\":\"x\"}]")
    public Result<SqlExecResultVO> tableData(@PathVariable("id") Long id,
                                             @RequestParam("schema") String schema,
                                             @RequestParam("table") String table,
                                             @RequestParam(value = "conditions", required = false) String conditions,
                                             @RequestParam(value = "orderBy", required = false) String orderBy,
                                             @RequestParam(value = "direction", defaultValue = "asc") String direction,
                                             @RequestParam(value = "page", defaultValue = "1") int page,
                                             @RequestParam(value = "size", defaultValue = "100") int size) {
        List<DbDataProxy.TableCondition> conditionList = parseConditions(conditions);
        SqlExecResult result = dbDataProxy.tableData(id, schema, table, conditionList,
            orderBy, "desc".equalsIgnoreCase(direction), page, size);
        return Result.success(toVO(result));
    }

    /**
     * conditions JSON 解析（格式非法时按空条件处理）
     */
    private List<DbDataProxy.TableCondition> parseConditions(String conditions) {
        List<DbDataProxy.TableCondition> list = new ArrayList<>();
        if (StrUtil.isBlank(conditions) || !JSONUtil.isTypeJSON(conditions)) {
            return list;
        }
        for (Object item : JSONUtil.parseArray(conditions)) {
            JSONObject json = (JSONObject) item;
            String column = json.getStr("column");
            String value = json.getStr("value");
            if (StrUtil.isBlank(column) || value == null) {
                continue;
            }
            String op = "like".equalsIgnoreCase(json.getStr("op")) ? "like" : "eq";
            list.add(new DbDataProxy.TableCondition(column, op, value));
        }
        return list;
    }

    private SqlExecResultVO toVO(SqlExecResult result) {
        SqlExecResultVO vo = new SqlExecResultVO();
        vo.setColumns(result.columns());
        vo.setRows(result.rows());
        vo.setAffectedRows(result.affectedRows());
        vo.setTruncated(result.truncated());
        return vo;
    }
}
