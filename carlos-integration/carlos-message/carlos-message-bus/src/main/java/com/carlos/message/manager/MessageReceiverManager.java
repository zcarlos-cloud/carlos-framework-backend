package com.carlos.message.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.param.MessageReceiverPageParam;
import com.carlos.message.pojo.vo.MessageReceiverVO;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息接收人表 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
public interface MessageReceiverManager extends BaseService<MessageReceiver> {

    /**
     * 新增消息接收人表
     *
     * @param dto 消息接收人表数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean add(MessageReceiverDTO dto);

    /**
     * 删除消息接收人表
     *
     * @param id 消息接收人表id
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean delete(Serializable id);

    /**
     * 修改消息接收人表信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean modify(MessageReceiverDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.message.pojo.dto.MessageReceiverDTO
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    Paging<MessageReceiverVO> getPage(MessageReceiverPageParam param);

    /**
     * 根据消息ID查询接收人列表
     *
     * @param messageId 消息ID
     * @return 接收人列表
     */
    List<MessageReceiver> listByMessageId(String messageId);

    /**
     * 根据接收人ID和状态查询
     *
     * @param receiverId 接收人ID
     * @param status     状态
     * @return 接收人列表
     */
    List<MessageReceiver> listByReceiverAndStatus(String receiverId, Integer status);

    /**
     * 更新状态
     *
     * @param id     ID
     * @param status 状态
     * @return 是否成功
     */
    boolean updateStatus(Long id, Integer status);

    /**
     * 增加失败次数
     *
     * @param id         ID
     * @param failReason 失败原因
     * @return 是否成功
     */
    boolean incrementFailCount(Long id, String failReason);

    /**
     * 查询已到期的定时待发消息接收人（scheduleTime <= before 且 status=PENDING）
     *
     * @param before 截止时间（通常传入当前时间）
     * @return 接收人列表
     */
    List<MessageReceiver> listScheduledPending(LocalDateTime before);
}
