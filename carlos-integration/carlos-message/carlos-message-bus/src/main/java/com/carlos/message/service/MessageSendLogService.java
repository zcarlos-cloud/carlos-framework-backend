package com.carlos.message.service;

import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.pojo.dto.MessageSendLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 消息发送日志 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageSendLogService {

    private final MessageSendLogManager sendLogManager;

    /**
     * 新增消息发送日志
     *
     * @param dto 消息发送日志数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageSendLog(MessageSendLogDTO dto) {
        boolean success = sendLogManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageSendLog' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息发送日志
     *
     * @param ids 消息发送日志id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageSendLog(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = sendLogManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息发送日志信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageSendLog(MessageSendLogDTO dto) {
        boolean success = sendLogManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageSendLog' data: id:{}", dto.getId());
    }

}
