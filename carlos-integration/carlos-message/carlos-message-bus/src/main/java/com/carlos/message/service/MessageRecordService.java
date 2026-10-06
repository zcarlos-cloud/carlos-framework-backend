package com.carlos.message.service;

import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 消息记录表 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRecordService {

    private final MessageRecordManager recordManager;

    /**
     * 新增消息记录表
     *
     * @param dto 消息记录表数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageRecord(MessageRecordDTO dto) {
        boolean success = recordManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageRecord' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息记录表
     *
     * @param ids 消息记录表id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageRecord(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = recordManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息记录表信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageRecord(MessageRecordDTO dto) {
        boolean success = recordManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageRecord' data: id:{}", dto.getId());
    }

}
