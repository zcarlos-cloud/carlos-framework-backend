package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageSendLogDTO;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.param.MessageSendLogCreateParam;
import com.carlos.message.pojo.param.MessageSendLogUpdateParam;
import com.carlos.message.pojo.vo.MessageSendLogVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息发送日志 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Mapper(uses = {CommonConvert.class})
public interface MessageSendLogConvert {

    MessageSendLogConvert INSTANCE = Mappers.getMapper(MessageSendLogConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogDTO toDTO(MessageSendLogCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogDTO toDTO(MessageSendLogUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageSendLogDTO> toDTO(List<MessageSendLog> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogDTO toDTO(MessageSendLog entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLog toDO(MessageSendLogDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogVO toVO(MessageSendLogDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageSendLogVO> toVO(List<MessageSendLog> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageSendLogVO toVO(MessageSendLog entity);
}
