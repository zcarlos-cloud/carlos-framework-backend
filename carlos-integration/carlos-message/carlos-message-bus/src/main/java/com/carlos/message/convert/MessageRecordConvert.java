package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.param.MessageRecordCreateParam;
import com.carlos.message.pojo.param.MessageRecordUpdateParam;
import com.carlos.message.pojo.vo.MessageRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息记录表 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Mapper(uses = {CommonConvert.class})
public interface MessageRecordConvert {

    MessageRecordConvert INSTANCE = Mappers.getMapper(MessageRecordConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordDTO toDTO(MessageRecordCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordDTO toDTO(MessageRecordUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageRecordDTO> toDTO(List<MessageRecord> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordDTO toDTO(MessageRecord entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecord toDO(MessageRecordDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordVO toVO(MessageRecordDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageRecordVO> toVO(List<MessageRecord> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageRecordVO toVO(MessageRecord entity);
}
