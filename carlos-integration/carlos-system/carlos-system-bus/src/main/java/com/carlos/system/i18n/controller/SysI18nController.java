package com.carlos.system.i18n.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdList;
import com.carlos.core.response.Result;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.redis.multilevel.MultiLevelCacheUtil;
import com.carlos.system.i18n.convert.SysI18nConvert;
import com.carlos.system.i18n.manager.SysI18nManager;
import com.carlos.system.i18n.pojo.param.SysI18nCreateParam;
import com.carlos.system.i18n.pojo.param.SysI18nPageParam;
import com.carlos.system.i18n.pojo.param.SysI18nUpdateParam;
import com.carlos.system.i18n.pojo.vo.SysI18nMessageVO;
import com.carlos.system.i18n.pojo.vo.SysI18nMessagesVO;
import com.carlos.system.i18n.service.SysI18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 国际化消息 Rest服务接口
 * </p>
 *
 * @author carlos
 * @date 2026-05-26
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "国际化消息")
public class SysI18nController {

    public static final String BASE_NAME = "国际化消息";
    private static final String I18N_CACHE_PREFIX = "system:i18n:messages:";
    private static final long I18N_CACHE_TIMEOUT = 30;
    private static final TimeUnit I18N_CACHE_TIME_UNIT = TimeUnit.MINUTES;

    /**
     * 允许的语言标识正则：zh_CN、en-US、zh、en 等
     */
    private static final String LANG_PATTERN = "^[a-zA-Z]{2}([-_][a-zA-Z]{2})?$";

    /**
     * 模块编码参数最大长度，防止缓存 key 过长和 IN 子句过大
     */
    private static final int MODULE_CODES_MAX_LENGTH = 200;

    private final SysI18nService i18nService;
    private final SysI18nManager i18nManager;

    @Log(
        title = "国际化消息",
        logType = "SYS_I18N_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增国际化消息: ' + #param.msgKey",
        targetType = "SYS_I18N",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:i18n', 'create')")
    @PostMapping("/sys/i18n")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated SysI18nCreateParam param) {
        i18nService.add(param);
        evictI18nCache(param.getLang());
    }

    @Log(
        title = "国际化消息",
        logType = "SYS_I18N_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除国际化消息: ' + #param.ids",
        targetType = "SYS_I18N",
        targetId = "#param.ids",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:i18n', 'delete')")
    @PostMapping("/sys/i18n/delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody @Validated ParamIdList<Long> param) {
        i18nService.delete(param.getIds());
        // 批量删除时无法精确定位语言，清除全部 i18n 缓存
        evictAllI18nCache();
    }

    @Log(
        title = "国际化消息",
        logType = "SYS_I18N_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改国际化消息: ' + #param.msgKey",
        targetType = "SYS_I18N",
        targetId = "#param.id",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:i18n', 'update')")
    @PostMapping("/sys/i18n/update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated SysI18nUpdateParam param) {
        i18nService.update(param);
        evictI18nCache(param.getLang());
    }

    /**
     * 详情
     *
     * 路径变量必须用 {@link PathVariable} 绑定：此前写的是 {@code @RequestParam Long id}，
     * 于是 {@code {id}} 这段路径从不参与绑定，调 {@code GET /sys/i18n/123} 会因为没有
     * 名为 id 的请求参数而返回 400，只有反直觉的 {@code /sys/i18n/x?id=123} 能用。
     *
     * @param id 主键
     */
    @PreAuthorize("hasPermission('manage:i18n', 'view')")
    @GetMapping("/sys/i18n/{id}")
    @Operation(summary = BASE_NAME + "详情")
    public SysI18nMessageVO detail(@PathVariable Long id) {
        return SysI18nConvert.INSTANCE.toVO(i18nManager.getDtoById(id));
    }

    @PreAuthorize("hasPermission('manage:i18n', 'view')")
    @GetMapping("/sys/i18n/page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<SysI18nMessageVO> page(SysI18nPageParam param) {
        return i18nManager.getPage(param);
    }

    // 公开只读：前端应用初始化（可能未登录）即调用，已入网关 OAuth2 白名单，不得加 @PreAuthorize
    @GetMapping("/sys/i18n/messages")
    @Operation(summary = "获取指定语言的国际化消息")
    public Result<SysI18nMessagesVO> getMessages(
            @RequestParam("lang")
            @Pattern(regexp = LANG_PATTERN, message = "语言标识格式不正确")
            String lang,
            @RequestParam(value = "moduleCodes", required = false)
            @Size(max = MODULE_CODES_MAX_LENGTH, message = "模块编码参数过长")
            String moduleCodes) {

        List<String> codeList = normalizeModuleCodes(moduleCodes);

        String cacheKey = buildCacheKey(lang, codeList);

        @SuppressWarnings("unchecked")
        Map<String, String> messages = (Map<String, String>) MultiLevelCacheUtil.get(cacheKey, () -> {
            Map<String, String> result = i18nManager.getMessagesByLangAndModules(lang, codeList);
            return (Serializable) result;
        }, I18N_CACHE_TIMEOUT, I18N_CACHE_TIME_UNIT);

        SysI18nMessagesVO vo = new SysI18nMessagesVO();
        vo.setLang(lang);
        vo.setMessages(messages);
        return Result.success(vo);
    }

    /**
     * 规范化模块编码参数：去重、排序、过滤空值，保证缓存 key 稳定
     *
     * @param moduleCodes 原始逗号分隔字符串
     * @return 规范化后的编码列表，为空或未传时返回 null
     */
    private List<String> normalizeModuleCodes(String moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            return null;
        }
        Set<String> codeSet = Arrays.stream(moduleCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toCollection(TreeSet::new));
        return codeSet.isEmpty() ? null : List.copyOf(codeSet);
    }

    private String buildCacheKey(String lang, List<String> moduleCodes) {
        if (moduleCodes == null || moduleCodes.isEmpty()) {
            return I18N_CACHE_PREFIX + lang;
        }
        return I18N_CACHE_PREFIX + lang + ":" + String.join(",", moduleCodes);
    }

    private void evictI18nCache(String lang) {
        MultiLevelCacheUtil.evictByPrefix(I18N_CACHE_PREFIX + lang);
    }

    private void evictAllI18nCache() {
        // 清除全部以 I18N_CACHE_PREFIX 开头的缓存，避免误清其它业务缓存
        MultiLevelCacheUtil.evictByPrefix(I18N_CACHE_PREFIX);
    }

}
