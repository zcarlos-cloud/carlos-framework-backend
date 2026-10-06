package com.carlos.message.convert;

import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import com.carlos.message.pojo.vo.MessageRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * <p>
 * 消息对象转换
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Mapper(componentModel = "spring")
public interface MessageConvert {

    MessageConvert INSTANCE = Mappers.getMapper(MessageConvert.class);

    MessageRecordVO toVO(MessageRecordDTO dto);

    List<MessageRecordVO> toVOList(List<MessageRecordDTO> dtoList);

    MessageRecordDTO toDTO(MessageRecord entity);

    List<MessageRecordDTO> toDTOList(List<MessageRecord> entityList);

    MessageReceiverDTO toDTO(MessageReceiver entity);

    List<MessageReceiverDTO> toReceiverList(List<MessageReceiver> entityList);

    @Named("statusToDesc")
    default String statusToDesc(Integer status) {
        MessageReceiverStatusEnum enumObj = MessageReceiverStatusEnum.of(status);
        return enumObj != null ? enumObj.getDesc() : "";
    }
}
