package com.carlos.system.dict.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.dict.convert.SysDictConvert;
import com.carlos.system.dict.manager.SysDictCacheManager;
import com.carlos.system.dict.manager.SysDictManager;
import com.carlos.system.dict.pojo.dto.SysDictDTO;
import com.carlos.system.dict.pojo.param.SysDictCreateParam;
import com.carlos.system.dict.pojo.param.SysDictPageParam;
import com.carlos.system.dict.pojo.param.SysDictUpdateParam;
import com.carlos.system.dict.pojo.vo.SysDictListVO;
import com.carlos.system.dict.pojo.vo.SysDictVO;
import com.carlos.system.dict.service.SysDictService;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 系统字典 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/dict")
@Tag(name = "系统字典类型")
public class SysDictController {

    public static final String BASE_NAME = "字典";

    private final SysDictService dictService;

    private final SysDictManager dictManager;

    private final SysDictCacheManager cacheManager;


    @Log(
        title = "字典管理",
        logType = "SYS_DICT_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增字典: ' + #param.dictName",
        targetType = "SYS_DICT",
        riskLevel = 25)
    @PreAuthorize("hasPermission('manage:dict', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)

    public boolean add(@RequestBody @Validated SysDictCreateParam param) {
        SysDictDTO dto = SysDictConvert.INSTANCE.toDTO(param);
        return this.dictService.addDict(dto, param.getItems());
    }

    @Log(
        title = "字典管理",
        logType = "SYS_DICT_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除字典: ' + #param.ids",
        targetType = "SYS_DICT",
        targetId = "#param.ids",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:dict', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)

    public boolean delete(@RequestBody @Validated ParamIdSet<Long> param) {
        return this.dictService.deleteDict(Sets.newHashSet(param.getIds()));
    }

    @Log(
        title = "字典管理",
        logType = "SYS_DICT_UPDATE",
        businessType = BusinessType.UPDATE,
        // 形参名是 command 而不是 param —— SpEL 变量按真实形参名绑定，写错不会报错，
        // 只会让 operation/targetId 静默变成原始表达式字符串
        operation = "'修改字典: ' + #command.dictName",
        targetType = "SYS_DICT",
        targetId = "#command.id",
        riskLevel = 25)
    @PreAuthorize("hasPermission('manage:dict', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)

    public boolean update(@RequestBody @Validated SysDictUpdateParam command) {
        SysDictDTO dto = SysDictConvert.INSTANCE.toDTO(command);
        return this.dictService.update(dto, command.getItems());
    }


    @PreAuthorize("hasPermission('manage:dict', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysDictVO detail(@RequestParam Long id) {
        return SysDictConvert.INSTANCE.toVO(this.dictService.getDetail(id));
    }

    @PreAuthorize("hasPermission('manage:dict', 'view')")
    @GetMapping("list")
    @Operation(summary = BASE_NAME + "下拉列表", description = "支持按名称过滤")
    public List<SysDictListVO> list(String name) {
        return SysDictConvert.INSTANCE.toVO(this.dictService.getListByName(name));
    }

    @PreAuthorize("hasPermission('manage:dict', 'view')")
    @GetMapping("full/list")
    @Operation(summary = BASE_NAME + "下拉列表 + 选项列表", description = "支持按名称过滤")
    public List<SysDictListVO> fullList(String name) {
        return SysDictConvert.INSTANCE.toVO(this.dictService.getListByName(name));
    }

    @PreAuthorize("hasPermission('manage:dict', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public IPage<SysDictVO> page(SysDictPageParam param) {
        return this.dictManager.getPage(param);
    }


    @Log(
        title = "字典管理",
        logType = "SYS_DICT_INIT_TXT",
        businessType = BusinessType.IMPORT,
        operation = "'从 txt 文件初始化字典: ' + #path",
        targetType = "SYS_DICT",
        riskLevel = 60)
    @PreAuthorize("hasPermission('manage:dict', 'import')")
    @GetMapping("initWithTxt")
    @Operation(summary = "使用txt文本初始化")
    public void initWithTxt(String path) {
        dictService.initWithTxt(path);
    }


    @Log(
        title = "字典管理",
        logType = "SYS_DICT_CACHE_REFRESH",
        businessType = BusinessType.REFRESH,
        operation = "'强制刷新字典缓存'",
        targetType = "SYS_DICT",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:dict', 'refresh')")
    @PostMapping("cache/refresh")
    @Operation(summary = "强制刷新" + BASE_NAME)

    public boolean refresh() {
        cacheManager.clearCache();
        this.cacheManager.initCache();
        return true;
    }

}
