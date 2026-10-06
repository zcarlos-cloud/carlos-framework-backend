package com.carlos.message.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.message.pojo.dto.MessageSendLogDTO;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.param.MessageSendLogPageParam;
import com.carlos.message.pojo.vo.MessageSendLogVO;

import java.io.Serializable;

/**
 * <p>
 * 消息发送日志 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
public interface MessageSendLogManager extends BaseService<MessageSendLog> {

    /**
     * 新增消息发送日志
     *
     * @param dto 消息发送日志数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean add(MessageSendLogDTO dto);

    /**
     * 删除消息发送日志
     *
     * @param id 消息发送日志id
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean delete(Serializable id);

    /**
     * 修改消息发送日志信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean modify(MessageSendLogDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.message.pojo.dto.MessageSendLogDTO
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    Paging<MessageSendLogVO> getPage(MessageSendLogPageParam param);
}
