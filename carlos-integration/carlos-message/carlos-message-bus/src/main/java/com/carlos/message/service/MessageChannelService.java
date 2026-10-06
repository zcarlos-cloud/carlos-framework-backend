package com.carlos.message.service;

import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 消息渠道配置 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageChannelService {

    private final MessageChannelManager channelManager;

    /**
     * 新增消息渠道配置
     *
     * @param dto 消息渠道配置数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageChannel(MessageChannelDTO dto) {
        boolean success = channelManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageChannel' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息渠道配置
     *
     * @param ids 消息渠道配置id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageChannel(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = channelManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息渠道配置信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageChannel(MessageChannelDTO dto) {
        boolean success = channelManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageChannel' data: id:{}", dto.getId());
    }

    /**
     * 启用消息渠道
     *
     * @param id 渠道ID
     */
    public void enableChannel(Serializable id) {
        boolean success = channelManager.updateStatus(id, 1);
        if (!success) {
            log.warn("Enable MessageChannel fail, id:{}", id);
            return;
        }
        log.info("Enable MessageChannel success, id:{}", id);
    }

    /**
     * 禁用消息渠道
     *
     * @param id 渠道ID
     */
    public void disableChannel(Serializable id) {
        boolean success = channelManager.updateStatus(id, 0);
        if (!success) {
            log.warn("Disable MessageChannel fail, id:{}", id);
            return;
        }
        log.info("Disable MessageChannel success, id:{}", id);
    }

    /**
     * 标记消息渠道为故障状态
     *
     * @param id 渠道ID
     */
    public void markFaulted(Serializable id) {
        boolean success = channelManager.updateStatus(id, 2);
        if (!success) {
            log.warn("Mark MessageChannel as faulted fail, id:{}", id);
            return;
        }
        log.info("Mark MessageChannel as faulted success, id:{}", id);
    }

}
