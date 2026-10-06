package com.carlos.message.service;

import cn.hutool.core.util.StrUtil;
import com.carlos.core.response.CommonErrorCode;
import com.carlos.message.enums.MessageErrorCode;
import com.carlos.message.manager.MessageTypeManager;
import com.carlos.message.pojo.dto.MessageTypeDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Set;


/**
 * <p>
 * 消息类型 业务
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageTypeService {

    private final MessageTypeManager typeManager;

    /**
     * 新增消息类型
     *
     * @param dto 消息类型数据
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void addMessageType(MessageTypeDTO dto) {
        validateTypeCodeUnique(dto.getTypeCode());
        boolean success = typeManager.add(dto);
        if (!success) {
            // 保存失败的应对措施
            return;
        }
        Serializable id = dto.getId();
        log.info("Insert 'MessageType' data: id:{}", id);
        // 保存完成的后续业务
    }

    /**
     * 删除消息类型
     *
     * @param ids 消息类型id
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void deleteMessageType(Set<Long> ids) {
        for (Serializable id : ids) {
            boolean success = typeManager.delete(id);
            if (!success) {
                // 删除失败的措施
                continue;
            }
            // 删除成功的后续业务
        }
    }

    /**
     * 修改消息类型信息
     *
     * @param dto 对象信息
     * @author Carlos
     * @date 2026年3月12日 上午11:17:06
     */
    public void updateMessageType(MessageTypeDTO dto) {
        boolean success = typeManager.modify(dto);
        if (!success) {
            // 修改失败操作
            return;
        }
        // 修改成功的后续操作
        log.info("Update 'MessageType' data: id:{}", dto.getId());
    }

    /**
     * 根据类型编码查询消息类型
     *
     * @param typeCode 类型编码
     * @return 消息类型DTO
     * @author Carlos
     * @date 2026年3月12日
     */
    public MessageTypeDTO getByTypeCode(String typeCode) {
        if (StrUtil.isBlank(typeCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTypeDTO dto = typeManager.getByTypeCode(typeCode);
        if (dto == null) {
            throw MessageErrorCode.MSG_RECORD_NOT_FOUND.exception("消息类型不存在: " + typeCode);
        }
        return dto;
    }

    /**
     * 启用消息类型
     *
     * @param id 主键ID
     * @author Carlos
     * @date 2026年3月12日
     */
    public void enableType(Serializable id) {
        MessageTypeDTO existing = typeManager.getDtoById(id);
        if (existing == null) {
            throw MessageErrorCode.MSG_RECORD_NOT_FOUND.exception();
        }
        if (Boolean.TRUE.equals(existing.getEnabled())) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("消息类型已处于启用状态");
        }
        boolean success = typeManager.updateStatus(id, Boolean.TRUE);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("启用消息类型失败");
        }
        log.info("Enable MessageType: id:{}", id);
    }

    /**
     * 禁用消息类型
     *
     * @param id 主键ID
     * @author Carlos
     * @date 2026年3月12日
     */
    public void disableType(Serializable id) {
        MessageTypeDTO existing = typeManager.getDtoById(id);
        if (existing == null) {
            throw MessageErrorCode.MSG_RECORD_NOT_FOUND.exception();
        }
        if (Boolean.FALSE.equals(existing.getEnabled())) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("消息类型已处于禁用状态");
        }
        boolean success = typeManager.updateStatus(id, Boolean.FALSE);
        if (!success) {
            throw CommonErrorCode.BUSINESS_ERROR.exception("禁用消息类型失败");
        }
        log.info("Disable MessageType: id:{}", id);
    }

    /**
     * 新增时校验typeCode唯一性
     *
     * @param typeCode 类型编码
     * @author Carlos
     * @date 2026年3月12日
     */
    public void validateTypeCodeUnique(String typeCode) {
        if (StrUtil.isBlank(typeCode)) {
            throw MessageErrorCode.MSG_PARAM_TEMPLATE_CODE_EMPTY.exception();
        }
        MessageTypeDTO existing = typeManager.getByTypeCode(typeCode);
        if (existing != null) {
            throw MessageErrorCode.MSG_TEMPLATE_ALREADY_EXISTS.exception("类型编码已存在: " + typeCode);
        }
    }

}
