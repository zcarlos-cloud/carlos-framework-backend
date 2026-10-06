package com.carlos.message.manager.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.message.convert.MessageTypeConvert;
import com.carlos.message.manager.MessageTypeManager;
import com.carlos.message.mapper.MessageTypeMapper;
import com.carlos.message.pojo.dto.MessageTypeDTO;
import com.carlos.message.pojo.entity.MessageType;
import com.carlos.message.pojo.param.MessageTypePageParam;
import com.carlos.message.pojo.vo.MessageTypeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 消息类型 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageTypeManagerImpl extends BaseServiceImpl<MessageTypeMapper, MessageType> implements MessageTypeManager {

    @Override
    public boolean add(MessageTypeDTO dto) {
        MessageType entity = MessageTypeConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MessageType' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'MessageType' data: id:{}", entity.getId());
        return true;
    }

    @Override
    public boolean delete(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        boolean success = removeById(id);
        if (!success) {
            log.warn("Remove 'MessageType' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MessageType' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(MessageTypeDTO dto) {
        MessageType entity = MessageTypeConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MessageType' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'MessageType' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public MessageTypeDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MessageType entity = getBaseMapper().selectById(id);
        return MessageTypeConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<MessageTypeVO> getPage(MessageTypePageParam param) {
        LambdaQueryWrapper<MessageType> wrapper = queryWrapper();
        wrapper.select(

            MessageType::getId,
            MessageType::getTypeCode,
            MessageType::getTypeName,
            MessageType::getEnabled,
            MessageType::getCreateBy,
            MessageType::getCreateTime,
            MessageType::getUpdateBy,
            MessageType::getUpdateTime
        );
        if (StrUtil.isNotBlank(param.getTypeCode())) {
            wrapper.like(MessageType::getTypeCode, param.getTypeCode());
        }
        if (StrUtil.isNotBlank(param.getTypeName())) {
            wrapper.like(MessageType::getTypeName, param.getTypeName());
        }
        if (param.getEnabled() != null) {
            wrapper.eq(MessageType::getEnabled, param.getEnabled());
        }
        if (param.getStart() != null) {
            wrapper.ge(MessageType::getCreateTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.le(MessageType::getCreateTime, param.getEnd());
        }
        // 分页必须有稳定排序，否则 PostgreSQL 下翻页结果可能重复或漏行
        wrapper.orderByDesc(MessageType::getCreateTime);
        PageInfo<MessageType> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, MessageTypeConvert.INSTANCE::toVO);
    }

    @Override
    public MessageTypeDTO getByTypeCode(String typeCode) {
        if (StrUtil.isBlank(typeCode)) {
            log.warn("typeCode can't be blank");
            return null;
        }
        LambdaQueryWrapper<MessageType> wrapper = new LambdaQueryWrapper<MessageType>()
            .eq(MessageType::getTypeCode, typeCode);
        MessageType entity = getOne(wrapper, false);
        if (entity == null) {
            log.warn("MessageType not found by typeCode: {}", typeCode);
            return null;
        }
        return MessageTypeConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public boolean updateStatus(Serializable id, Boolean enabled) {
        if (id == null || enabled == null) {
            log.warn("id and enabled can't be null");
            return false;
        }
        MessageType entity = new MessageType();
        entity.setId(Convert.toLong(id));
        entity.setEnabled(enabled);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update MessageType status fail, id:{}, enabled:{}", id, enabled);
            return false;
        }
        log.debug("Update MessageType status success, id:{}, enabled:{}", id, enabled);
        return true;
    }

}
