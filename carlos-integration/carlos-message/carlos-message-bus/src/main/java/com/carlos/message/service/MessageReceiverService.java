package com.carlos.message.service;

import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.pojo.dto.MessageReceiverDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 消息接收人表 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageReceiverService {

    private final MessageReceiverManager receiverManager;

    /**
     * 新增消息接收人表
     *
     * @param dto 消息接收人表数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageReceiver(MessageReceiverDTO dto) {
        boolean success = receiverManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageReceiver' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息接收人表
     *
     * @param ids 消息接收人表id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageReceiver(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = receiverManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息接收人表信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageReceiver(MessageReceiverDTO dto) {
        boolean success = receiverManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageReceiver' data: id:{}", dto.getId());
    }

}
