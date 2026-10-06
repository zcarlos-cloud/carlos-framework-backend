package com.carlos.message.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import com.carlos.message.pojo.entity.MessageChannel;
import com.carlos.message.pojo.param.MessageChannelPageParam;
import com.carlos.message.pojo.vo.MessageChannelVO;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 消息渠道配置 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
public interface MessageChannelManager extends BaseService<MessageChannel> {

    /**
     * 新增消息渠道配置
     *
     * @param dto 消息渠道配置数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean add(MessageChannelDTO dto);

    /**
     * 删除消息渠道配置
     *
     * @param id 消息渠道配置id
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean delete(Serializable id);

    /**
     * 修改消息渠道配置信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean modify(MessageChannelDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.message.pojo.dto.MessageChannelDTO
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    Paging<MessageChannelVO> getPage(MessageChannelPageParam param);

    /**
     * 根据渠道编码查询消息渠道
     *
     * @param channelCode 渠道编码
     * @return 消息渠道DTO
     * @author Carlos
     * @date 2026年3月12日
     */
    MessageChannelDTO getByChannelCode(String channelCode);

    /**
     * 根据渠道类型查询列表
     *
     * @param channelType 渠道类型
     * @return 消息渠道列表
     * @author Carlos
     * @date 2026年3月12日
     */
    List<MessageChannelDTO> listByType(Integer channelType);

    /**
     * 查询所有可用的渠道
     *
     * @return 可用渠道列表
     * @author Carlos
     * @date 2026年3月12日
     */
    List<MessageChannelDTO> getAvailableChannels();

    /**
     * 更新消息渠道状态
     *
     * @param id 主键ID
     * @param enabled 状态（0-禁用 1-启用 2-故障）
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日
     */
    boolean updateStatus(Serializable id, Integer enabled);
}
