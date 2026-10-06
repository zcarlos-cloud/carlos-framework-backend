package com.carlos.system.dict.controller;


import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.dict.convert.SysDictItemConvert;
import com.carlos.system.dict.manager.SysDictItemManager;
import com.carlos.system.dict.pojo.dto.SysDictItemDTO;
import com.carlos.system.dict.pojo.param.SysDictItemCreateParam;
import com.carlos.system.dict.pojo.param.SysDictItemPageParam;
import com.carlos.system.dict.pojo.param.SysDictItemUpdateParam;
import com.carlos.system.dict.pojo.vo.SysDictItemListVO;
import com.carlos.system.dict.pojo.vo.SysDictItemVO;
import com.carlos.system.dict.service.SysDictItemService;
import com.carlos.system.enums.SystemErrorCode;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 系统字典详情 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2021-11-22 14:49:00
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/dict/item")
@Tag(name = "系统字典选项")
public class SysDictItemController {

    public static final String BASE_NAME = "字典选项";

    private final SysDictItemService dictItemService;

    private final SysDictItemManager dictItemManager;


    @Log(
        title = "字典选项",
        logType = "SYS_DICT_ITEM_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增字典选项: ' + #param.itemName",
        targetType = "SYS_DICT_ITEM",
        targetId = "#param.dictId",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:dict:item', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)

    public void add(@RequestBody @Validated SysDictItemCreateParam param) {
        SysDictItemDTO dto = SysDictItemConvert.INSTANCE.toDTO(param);
        this.dictItemService.addDictItem(param.getDictId(), List.of(dto));
    }

    @Log(
        title = "字典选项",
        logType = "SYS_DICT_ITEM_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除字典选项: ' + #param.ids",
        targetType = "SYS_DICT_ITEM",
        targetId = "#param.ids",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:dict:item', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)

    public boolean delete(@RequestBody @Validated ParamIdSet<Long> param) {
        return this.dictItemService.deleteDictItem(Sets.newHashSet(param.getIds()));
    }


    @Log(
        title = "字典选项",
        logType = "SYS_DICT_ITEM_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改字典选项: ' + #command.itemName",
        targetType = "SYS_DICT_ITEM",
        targetId = "#command.id",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:dict:item', 'update')")
    @PostMapping("update")
    @Operation(summary = "修改" + BASE_NAME)

    public boolean update(@RequestBody @Validated SysDictItemUpdateParam command) {
        SysDictItemDTO dto = SysDictItemConvert.INSTANCE.toDTO(command);
        return this.dictItemService.updateDictItem(dto);
    }

    @PreAuthorize("hasPermission('manage:dict:item', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")

    public SysDictItemVO detail(@RequestParam Long id) {
        return SysDictItemConvert.INSTANCE.toVO(this.dictItemManager.getItemById(id));
    }

    @PreAuthorize("hasPermission('manage:dict:item', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")

    public IPage<SysDictItemVO> page(SysDictItemPageParam param) {
        return this.dictItemManager.getPage(param);
    }

    @PreAuthorize("hasPermission('manage:dict:item', 'view')")
    @GetMapping("list")
    @Operation(summary = BASE_NAME + "列表")

    public List<SysDictItemVO> list(Long dictId) {
        return SysDictItemConvert.INSTANCE.toListVO(this.dictItemManager.listItems(Sets.newHashSet(dictId), null, true));
    }

    @PreAuthorize("hasPermission('manage:dict:item', 'view')")
    @GetMapping("select")
    @Operation(summary = BASE_NAME + "下拉列表")

    public List<SysDictItemListVO> select(String code, String name) {
        if (StringUtils.isBlank(code)) {
            throw SystemErrorCode.SYS_PARAM_DICT_CODE_EMPTY.exception();
        }
        return SysDictItemConvert.INSTANCE.toVO(this.dictItemService.getItemsByDictCode(code, name));
    }

}
