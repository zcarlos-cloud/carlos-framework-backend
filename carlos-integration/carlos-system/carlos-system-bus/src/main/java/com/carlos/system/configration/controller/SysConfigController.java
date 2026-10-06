package com.carlos.system.configration.controller;


import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.idev.excel.EasyExcel;
import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.configration.convert.SysConfigConvert;
import com.carlos.system.configration.listener.SysConfigExcelListener;
import com.carlos.system.configration.manager.SysConfigManager;
import com.carlos.system.configration.pojo.dto.SysConfigDTO;
import com.carlos.system.configration.pojo.excel.SysConfigExcel;
import com.carlos.system.configration.pojo.param.SysConfigCreateParam;
import com.carlos.system.configration.pojo.param.SysConfigPageParam;
import com.carlos.system.configration.pojo.param.SysConfigUpdateParam;
import com.carlos.system.configration.pojo.vo.SysConfigLoginPageVO;
import com.carlos.system.configration.pojo.vo.SysConfigVO;
import com.carlos.system.configration.service.SysConfigService;
import com.carlos.util.easyexcel.ExcelUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>
 * 系统配置 rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2022-11-3 13:47:54
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/config")
@Tag(name = "系统配置")
public class SysConfigController {

    public static final String BASE_NAME = "系统配置";

    private final SysConfigService configService;

    private final SysConfigManager configManager;


    @Log(
        title = "系统配置",
        logType = "SYS_CONFIG_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增系统配置: ' + #param.configCode",
        targetType = "SYS_CONFIG",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:config', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)

    public void add(@RequestBody @Validated SysConfigCreateParam param) {
        SysConfigDTO dto = SysConfigConvert.INSTANCE.toDTO(param);
        this.configService.addSysConfig(dto);
    }

    @Log(
        title = "系统配置",
        logType = "SYS_CONFIG_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除系统配置: ' + #param.ids",
        targetType = "SYS_CONFIG",
        targetId = "#param.ids",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:config', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)

    public void delete(@RequestBody @Validated ParamIdSet<Long> param) {
        this.configService.deleteSysConfig(param.getIds());
    }


    @Log(
        title = "系统配置",
        logType = "SYS_CONFIG_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改系统配置: ' + #param.configCode",
        targetType = "SYS_CONFIG",
        targetId = "#param.id",
        riskLevel = 30)
    @PreAuthorize("hasPermission('manage:config', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)

    public void update(@RequestBody @Validated SysConfigUpdateParam param) {
        SysConfigDTO dto = SysConfigConvert.INSTANCE.toDTO(param);
        this.configService.updateSysConfig(dto);
    }

    @PreAuthorize("hasPermission('manage:config', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")

    public SysConfigVO detail(@RequestParam Long id) {
        return SysConfigConvert.INSTANCE.toVO(this.configManager.getDtoById(id));
    }

    @PreAuthorize("hasPermission('manage:config', 'view')")
    @GetMapping("code")
    @Operation(summary = "根据code获取配置")

    public SysConfigVO getByCode(String code) {
        return SysConfigConvert.INSTANCE.toVO(this.configService.getByCode(code));
    }

    @PreAuthorize("hasPermission('manage:config', 'view')")
    @GetMapping("all")
    @Operation(summary = "获取全部配置")

    public Map<String, SysConfigVO> getAllConfig() {
        List<SysConfigDTO> configs = configService.listConfig();
        Map<String, SysConfigVO> map = new HashMap<>();
        for (SysConfigDTO config : configs) {
            map.put(config.getConfigCode(), SysConfigConvert.INSTANCE.toVO(config));
        }
        return map;
    }

    @PreAuthorize("hasPermission('manage:config', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")

    public Paging<SysConfigVO> page(SysConfigPageParam param) {
        return this.configManager.getPage(param);
    }


    @PreAuthorize("hasPermission('manage:config', 'view')")
    @GetMapping("loginpage")
    @Operation(summary = "登录页配置")
    public SysConfigLoginPageVO loginpage() {
        return configService.getHomePageConfig();
    }

    @Log(
        title = "系统配置",
        logType = "SYS_CONFIG_IMPORT",
        businessType = BusinessType.IMPORT,
        operation = "'导入系统配置'",
        targetType = "SYS_CONFIG",
        saveRequestParams = false,
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:config', 'import')")
    @PostMapping("import")
    @Operation(summary = "导入" + BASE_NAME)
    public Result<?> importData(@RequestPart MultipartFile file) {
        SysConfigExcelListener listener = new SysConfigExcelListener(configService);
        try {
            String filename = FileNameUtil.getName(file.getOriginalFilename());
            if (StrUtil.isBlank(filename)) {
                throw CommonErrorCode.PARAM_MISSING.exception("文件名不能为空");
            }
            ExcelUtil.checkExcel(filename);
            EasyExcel.read(file.getInputStream(), SysConfigExcel.class, listener).sheet().doRead();
        } catch (IOException e) {
            return Result.error("数据导入失败！");
        }
        return Result.success("系统配置导入成功！");
    }

    /**
     * 导出系统配置
     *
     * <p>
     * 接收与列表页相同的查询条件，导出的是**当前筛选结果**（此前导出整表）。
     * </p>
     */
    @Log(
        title = "系统配置",
        logType = "SYS_CONFIG_EXPORT",
        businessType = BusinessType.EXPORT,
        operation = "'导出系统配置'",
        targetType = "SYS_CONFIG",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:config', 'export')")
    @GetMapping("export")
    @Operation(summary = "导出" + BASE_NAME)
    public void export(SysConfigPageParam param, HttpServletResponse response) {
        configService.export(param, response, false);
    }

    @PreAuthorize("hasPermission('manage:config', 'export')")
    @GetMapping("export/template")
    @Operation(summary = "导出" + BASE_NAME + "模板")
    public void exportTemplate(HttpServletResponse response) {
        // 模板不带数据，无需查询条件
        configService.export(null, response, true);
    }
}
