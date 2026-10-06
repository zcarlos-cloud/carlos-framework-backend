package com.carlos.message.controller;

import com.carlos.core.pagination.Paging;
import com.carlos.core.param.ParamIdSet;
import com.carlos.log.annotation.Log;
import com.carlos.log.enums.BusinessType;
import com.carlos.message.convert.MessageChannelConvert;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import com.carlos.message.pojo.param.MessageChannelCreateParam;
import com.carlos.message.pojo.param.MessageChannelPageParam;
import com.carlos.message.pojo.param.MessageChannelUpdateParam;
import com.carlos.message.pojo.vo.MessageChannelVO;
import com.carlos.message.service.MessageChannelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * <p>
 * 消息渠道配置 rest服务接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("message/channel")
@Tag(name = "消息渠道配置")
public class MessageChannelController {

    public static final String BASE_NAME = "消息渠道配置";

    private final MessageChannelService channelService;

    private final MessageChannelManager channelManager;


    /**
     * <p>
     * <b>必须关掉请求参数采集</b>：{@code channelConfig} 是一个<b>字符串</b>，其值是 JSON
     * （如钉钉的 {@code {"appKey":…,"appSecret":…,"agentId":…}}）。
     * {@code LogAspect} 的 {@code excludeParams} 只能按<b>序列化后的 JSON 键名</b>剔除，
     * 看不进字符串值内部，因此 {@code appSecret} 这类凭据会被原样写进审计库并保留 90 天。
     * 关掉之后审计信息由 {@code operation} 里的 channelCode 承载，不损失可读性。
     * </p>
     */
    @Log(title = "消息渠道配置", logType = "MSG_CHANNEL_CREATE", businessType = BusinessType.INSERT,
        operation = "'新增渠道: ' + #param.channelCode", targetType = "MSG_CHANNEL", riskLevel = 30,
        saveRequestParams = false)
    @PreAuthorize("hasPermission('message:channel', 'create')")
    @PostMapping("add")
    @Operation(summary = "新增" + BASE_NAME)
    public void add(@RequestBody @Validated MessageChannelCreateParam param) {
        MessageChannelDTO dto = MessageChannelConvert.INSTANCE.toDTO(param);
        channelService.addMessageChannel(dto);
    }


    @Log(title = "消息渠道配置", logType = "MSG_CHANNEL_DELETE", businessType = BusinessType.DELETE,
        operation = "'删除渠道: ' + #param.ids", targetId = "#param.ids",
        targetType = "MSG_CHANNEL", riskLevel = 50)
    @PreAuthorize("hasPermission('message:channel', 'delete')")
    @PostMapping("delete")
    @Operation(summary = "删除" + BASE_NAME)
    public void delete(@RequestBody ParamIdSet<Long> param) {
        channelService.deleteMessageChannel(param.getIds());
    }


    /**
     * 同 {@link #add}：{@code channelConfig} 内含凭据，不得采集请求参数
     */
    @Log(title = "消息渠道配置", logType = "MSG_CHANNEL_UPDATE", businessType = BusinessType.UPDATE,
        operation = "'更新渠道: ' + #param.channelCode", targetId = "#param.id",
        targetType = "MSG_CHANNEL", riskLevel = 30, saveRequestParams = false)
    @PreAuthorize("hasPermission('message:channel', 'update')")
    @PostMapping("update")
    @Operation(summary = "更新" + BASE_NAME)
    public void update(@RequestBody @Validated MessageChannelUpdateParam param) {
        MessageChannelDTO dto = MessageChannelConvert.INSTANCE.toDTO(param);
        channelService.updateMessageChannel(dto);
    }


    @PreAuthorize("hasPermission('message:channel', 'view')")
    @GetMapping("detail")
    @Operation(summary = BASE_NAME + "详情")
    public MessageChannelVO detail(Long id) {
        return MessageChannelConvert.INSTANCE.toVO(channelManager.getDtoById(id));
    }


    @PreAuthorize("hasPermission('message:channel', 'view')")
    @GetMapping("page")
    @Operation(summary = BASE_NAME + "分页列表")
    public Paging<MessageChannelVO> page(MessageChannelPageParam param) {
        return channelManager.getPage(param);
    }

    @Log(title = "消息渠道配置", logType = "MSG_CHANNEL_ENABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'启用渠道, id: ' + #id", targetId = "#id", targetType = "MSG_CHANNEL", riskLevel = 40)
    @PreAuthorize("hasPermission('message:channel', 'changeState')")
    @PostMapping("enable")
    @Operation(summary = "启用" + BASE_NAME)
    public void enable(@RequestParam Long id) {
        channelService.enableChannel(id);
    }

    @Log(title = "消息渠道配置", logType = "MSG_CHANNEL_DISABLE", businessType = BusinessType.UPDATE_STATUS,
        operation = "'禁用渠道, id: ' + #id", targetId = "#id", targetType = "MSG_CHANNEL", riskLevel = 40)
    @PreAuthorize("hasPermission('message:channel', 'changeState')")
    @PostMapping("disable")
    @Operation(summary = "禁用" + BASE_NAME)
    public void disable(@RequestParam Long id) {
        channelService.disableChannel(id);
    }
}
