package com.carlos.message.manager;

import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseService;
import com.carlos.message.pojo.dto.MessageTypeDTO;
import com.carlos.message.pojo.entity.MessageType;
import com.carlos.message.pojo.param.MessageTypePageParam;
import com.carlos.message.pojo.vo.MessageTypeVO;

import java.io.Serializable;

/**
 * <p>
 * 消息类型 查询封装接口
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
public interface MessageTypeManager extends BaseService<MessageType> {

    /**
     * 新增消息类型
     *
     * @param dto 消息类型数据
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean add(MessageTypeDTO dto);

    /**
     * 删除消息类型
     *
     * @param id 消息类型id
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean delete(Serializable id);

    /**
     * 修改消息类型信息
     *
     * @param dto 对象信息
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    boolean modify(MessageTypeDTO dto);

    /**
     * 获取数据详情
     *
     * @param id 主键id
     * @return com.carlos.message.pojo.dto.MessageTypeDTO
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTypeDTO getDtoById(Serializable id);

    /**
     * 分页列表
     *
     * @param  param 分页参数
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    Paging<MessageTypeVO> getPage(MessageTypePageParam param);

    /**
     * 根据类型编码查询消息类型
     *
     * @param typeCode 类型编码
     * @return 消息类型DTO
     * @author Carlos
     * @date 2026年3月12日
     */
    MessageTypeDTO getByTypeCode(String typeCode);

    /**
     * 更新消息类型状态
     *
     * @param id 主键ID
     * @param enabled 是否启用
     * @return boolean
     * @author Carlos
     * @date 2026年3月12日
     */
    boolean updateStatus(Serializable id, Boolean enabled);
}
