package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageChannelDTO;
import com.carlos.message.pojo.entity.MessageChannel;
import com.carlos.message.pojo.param.MessageChannelCreateParam;
import com.carlos.message.pojo.param.MessageChannelUpdateParam;
import com.carlos.message.pojo.vo.MessageChannelVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息渠道配置 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Mapper(uses = {CommonConvert.class})
public interface MessageChannelConvert {

    MessageChannelConvert INSTANCE = Mappers.getMapper(MessageChannelConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelDTO toDTO(MessageChannelCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelDTO toDTO(MessageChannelUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageChannelDTO> toDTO(List<MessageChannel> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelDTO toDTO(MessageChannel entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannel toDO(MessageChannelDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelVO toVO(MessageChannelDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageChannelVO> toVO(List<MessageChannel> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageChannelVO toVO(MessageChannel entity);
}
