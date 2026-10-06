package com.carlos.message.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.param.MessageRecordPageParam;
import com.carlos.message.pojo.vo.MessageRecordVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 消息记录表 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
public interface MessageRecordManager extends BaseService<MessageRecord> {

    /**
     * 新增消息记录表
     *
     * @param dto 消息记录表数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean add(MessageRecordDTO dto);

    /**
     * 删除消息记录表
     *
     * @param id 消息记录表id
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean delete(Serializable id);

    /**
     * 修改消息记录表信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean modify(MessageRecordDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.message.pojo.dto.MessageRecordDTO
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    Paging<MessageRecordVO> getPage(MessageRecordPageParam param);

    /**
     * 根据消息ID查询
     *
     * @param messageId 消息ID
     * @return 记录
     */
    MessageRecord getByMessageId(String messageId);

    /**
     * 查询需要清理的历史记录ID
     *
     * @param days 天数
     * @return ID列表
     */
    List<Long> getExpiredIds(int days);

    /**
     * 更新统计信息
     *
     * @param messageId 消息ID
     * @param successCount 成功数
     * @param failCount 失败数
     * @return 是否成功
     */
    boolean updateStatistics(String messageId, int successCount, int failCount);
}
