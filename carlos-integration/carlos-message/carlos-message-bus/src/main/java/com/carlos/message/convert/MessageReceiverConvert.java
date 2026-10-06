package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.param.MessageReceiverCreateParam;
import com.carlos.message.pojo.param.MessageReceiverUpdateParam;
import com.carlos.message.pojo.vo.MessageReceiverVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息接收人表 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Mapper(uses = {CommonConvert.class})
public interface MessageReceiverConvert {

    MessageReceiverConvert INSTANCE = Mappers.getMapper(MessageReceiverConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverDTO toDTO(MessageReceiverCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverDTO toDTO(MessageReceiverUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageReceiverDTO> toDTO(List<MessageReceiver> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverDTO toDTO(MessageReceiver entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiver toDO(MessageReceiverDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverVO toVO(MessageReceiverDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageReceiverVO> toVO(List<MessageReceiver> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageReceiverVO toVO(MessageReceiver entity);
}
