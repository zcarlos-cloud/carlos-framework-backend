package com.carlos.system.news.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carlos.core.param.ParamId;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.system.news.convert.SysNewsConvert;
import com.carlos.system.news.manager.SysNewsManager;
import com.carlos.system.news.pojo.param.SysNewsCreateParam;
import com.carlos.system.news.pojo.param.SysNewsMyPageParam;
import com.carlos.system.news.pojo.param.SysNewsPageParam;
import com.carlos.system.news.pojo.param.SysNewsUpdateParam;
import com.carlos.system.news.pojo.vo.SysNewsVO;
import com.carlos.system.news.service.SysNewsService;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 公告/站内通知 rest服务接口
 * </p>
 *
 * <p>管理端：CRUD + 发布/下线；用户端：我的公告分页（含已读标记）、标记已读、未读数。</p>
 *
 * @author carlos
 * @since 3.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/news")
@Tag(name = "公告管理")
public class SysNewsController {

    public static final String BASE_NAME = "公告";

    private final SysNewsService newsService;

    private final SysNewsManager newsManager;

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_CREATE",
        businessType = BusinessType.INSERT,
        operation = "'新增公告: ' + #param.title",
        targetType = "SYS_NEWS",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:news', 'create')")
    @PostMapping
    @Operation(summary = "新增" + BASE_NAME)
    public boolean add(@RequestBody @Validated SysNewsCreateParam param) {
        return this.newsService.addNews(SysNewsConvert.INSTANCE.toDTO(param), param.getScopeRoleIds());
    }

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_DELETE",
        businessType = BusinessType.DELETE,
        operation = "'删除公告: ' + #param.ids",
        targetType = "SYS_NEWS",
        targetId = "#param.ids",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:news', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public boolean delete(@RequestBody @Validated ParamIdSet<Long> param) {
        return this.newsService.deleteNews(Sets.newHashSet(param.getIds()));
    }

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_UPDATE",
        businessType = BusinessType.UPDATE,
        operation = "'修改公告: ' + #param.title",
        targetType = "SYS_NEWS",
        targetId = "#param.id",
        riskLevel = 20)
    @PreAuthorize("hasPermission('manage:news', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public boolean update(@RequestBody @Validated SysNewsUpdateParam param) {
        return this.newsService.updateNews(SysNewsConvert.INSTANCE.toDTO(param), param.getScopeRoleIds());
    }

    @PreAuthorize("hasPermission('manage:news', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public SysNewsVO detail(@RequestParam Long id) {
        return SysNewsConvert.INSTANCE.toVO(this.newsService.getDetail(id));
    }

    @PreAuthorize("hasPermission('manage:news', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public IPage<SysNewsVO> page(SysNewsPageParam param) {
        return this.newsManager.getPage(param);
    }

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_PUBLISH",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'发布公告: ' + #param.id",
        targetType = "SYS_NEWS",
        targetId = "#param.id",
        riskLevel = 40)
    @PreAuthorize("hasPermission('manage:news', 'publish')")
    @PostMapping("publish")
    @Operation(summary = "发布" + BASE_NAME)
    public boolean publish(@RequestBody @Validated ParamId<Long> param) {
        return this.newsService.publish(param.getId());
    }

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_OFFLINE",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'下线公告: ' + #param.id",
        targetType = "SYS_NEWS",
        targetId = "#param.id",
        riskLevel = 50)
    @PreAuthorize("hasPermission('manage:news', 'offline')")
    @PostMapping("offline")
    @Operation(summary = "下线" + BASE_NAME)
    public boolean offline(@RequestBody @Validated ParamId<Long> param) {
        return this.newsService.offline(param.getId());
    }

    @PreAuthorize("hasPermission('manage:news', 'view')")
    @GetMapping("my/page")
    @Operation(summary = "我的" + BASE_NAME + "分页（含已读标记）")
    public IPage<SysNewsVO> myPage(SysNewsMyPageParam param) {
        return this.newsService.myPage(param);
    }

    @PreAuthorize("hasPermission('manage:news', 'view')")
    @GetMapping("my/detail")
    @Operation(summary = "我的" + BASE_NAME + "详情（可见性校验）")
    public SysNewsVO myDetail(@RequestParam Long id) {
        return SysNewsConvert.INSTANCE.toVO(this.newsService.myDetail(id));
    }

    @Log(
        title = "公告管理",
        logType = "SYS_NEWS_READ",
        businessType = BusinessType.UPDATE_STATUS,
        operation = "'标记公告已读: ' + #id",
        targetType = "SYS_NEWS",
        targetId = "#id",
        riskLevel = 10)
    @PreAuthorize("hasPermission('manage:news', 'read')")
    @PostMapping("read")
    @Operation(summary = "标记已读")
    public boolean read(@RequestParam Long id) {
        this.newsService.read(id);
        return true;
    }

    @PreAuthorize("hasPermission('manage:news', 'view')")
    @GetMapping("unread-count")
    @Operation(summary = "未读" + BASE_NAME + "数")
    public long unreadCount() {
        return this.newsService.unreadCount();
    }

}
