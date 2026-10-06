package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageTemplateDTO;
import com.carlos.message.pojo.entity.MessageTemplate;
import com.carlos.message.pojo.param.MessageTemplateCreateParam;
import com.carlos.message.pojo.param.MessageTemplateUpdateParam;
import com.carlos.message.pojo.vo.MessageTemplateVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息模板 转换器
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Mapper(uses = {CommonConvert.class})
public interface MessageTemplateConvert {

    MessageTemplateConvert INSTANCE = Mappers.getMapper(MessageTemplateConvert.class);

    /**
     * 接口新增参数对象转数据传输对象
     *
     * @param param 新增参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplateDTO toDTO(MessageTemplateCreateParam param);

    /**
     * 接口修改参数对象转数据传输对象
     *
     * @param param 修改参数
     * @return 数据传输对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplateDTO toDTO(MessageTemplateUpdateParam param);

    /**
     * 持久化对象列表转数据传输对象列表
     *
     * @param dos 数据持久化对象列表
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageTemplateDTO> toDTO(List<MessageTemplate> dos);

    /**
     * 持久化对象转数据传输对象
     *
     * @param entity 数据持久化对象
     * @return 数据传输对象列表
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplateDTO toDTO(MessageTemplate entity);

    /**
     * 数据传输对象转数据持久化对象
     *
     * @param dto 数据传输对象
     * @return 数据持久化对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplate toDO(MessageTemplateDTO dto);

    /**
     * 数据传输对象转数据显示对象
     *
     * @param dto 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplateVO toVO(MessageTemplateDTO dto);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param dos 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    List<MessageTemplateVO> toVO(List<MessageTemplate> dos);

    /**
     * 数据持久对象转数据显示对象
     *
     * @param entity 数据传输对象
     * @return 数据显示对象
     * @author Carlos
     * @date 2026年3月12日 上午11:17:05
     */
    MessageTemplateVO toVO(MessageTemplate entity);
}
