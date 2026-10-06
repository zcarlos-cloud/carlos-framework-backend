package com.carlos.message.manager.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.message.convert.MessageTemplateConvert;
import com.carlos.message.manager.MessageTemplateManager;
import com.carlos.message.manager.MessageTypeManager;
import com.carlos.message.mapper.MessageTemplateMapper;
import com.carlos.message.pojo.dto.MessageTemplateDTO;
import com.carlos.message.pojo.dto.MessageTypeDTO;
import com.carlos.message.pojo.entity.MessageTemplate;
import com.carlos.message.pojo.param.MessageTemplatePageParam;
import com.carlos.message.pojo.vo.MessageTemplateVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 消息模板 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageTemplateManagerImpl extends BaseServiceImpl<MessageTemplateMapper, MessageTemplate> implements MessageTemplateManager {

    private final MessageTypeManager messageTypeManager;


    @Override
    public boolean add(MessageTemplateDTO dto) {
        MessageTemplate entity = MessageTemplateConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MessageTemplate' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'MessageTemplate' data: id:{}", entity.getId());
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
            log.warn("Remove 'MessageTemplate' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MessageTemplate' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(MessageTemplateDTO dto) {
        MessageTemplate entity = MessageTemplateConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MessageTemplate' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'MessageTemplate' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public MessageTemplateDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MessageTemplate entity = getBaseMapper().selectById(id);
        return MessageTemplateConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<MessageTemplateVO> getPage(MessageTemplatePageParam param) {
        LambdaQueryWrapper<MessageTemplate> wrapper = queryWrapper();
        wrapper.select(

            MessageTemplate::getId,
            MessageTemplate::getTypeId,
            MessageTemplate::getTemplateCode,
            MessageTemplate::getTemplateName,
            MessageTemplate::getTitleTemplate,
            MessageTemplate::getContentTemplate,
            MessageTemplate::getParamSchema,
            MessageTemplate::getChannelConfig,
            MessageTemplate::getEnabled,
            MessageTemplate::getCreateBy,
            MessageTemplate::getCreateTime,
            MessageTemplate::getUpdateBy,
            MessageTemplate::getUpdateTime
        );
        // 未参与过滤的字段（前端传了也不生效）：contentTemplate / paramSchema / channelConfig
        // 分别为正文长文本与两份 JSON 定义，不适合作列表筛选项。
        if (param.getTypeId() != null) {
            wrapper.eq(MessageTemplate::getTypeId, param.getTypeId());
        }
        if (StrUtil.isNotBlank(param.getTemplateCode())) {
            wrapper.like(MessageTemplate::getTemplateCode, param.getTemplateCode());
        }
        if (StrUtil.isNotBlank(param.getTemplateName())) {
            wrapper.like(MessageTemplate::getTemplateName, param.getTemplateName());
        }
        if (StrUtil.isNotBlank(param.getTitleTemplate())) {
            wrapper.like(MessageTemplate::getTitleTemplate, param.getTitleTemplate());
        }
        if (param.getEnabled() != null) {
            wrapper.eq(MessageTemplate::getEnabled, param.getEnabled());
        }
        if (param.getStart() != null) {
            wrapper.ge(MessageTemplate::getCreateTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.le(MessageTemplate::getCreateTime, param.getEnd());
        }
        // 分页必须有稳定排序，否则 PostgreSQL 下翻页结果可能重复或漏行
        wrapper.orderByDesc(MessageTemplate::getCreateTime);
        PageInfo<MessageTemplate> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, MessageTemplateConvert.INSTANCE::toVO);
    }

    @Override
    public MessageTemplateDTO getByTemplateCode(String templateCode) {
        if (StrUtil.isBlank(templateCode)) {
            log.warn("templateCode can't be blank");
            return null;
        }
        LambdaQueryWrapper<MessageTemplate> wrapper = new LambdaQueryWrapper<MessageTemplate>()
            .eq(MessageTemplate::getTemplateCode, templateCode);
        MessageTemplate entity = getOne(wrapper, false);
        if (entity == null) {
            log.warn("MessageTemplate not found by templateCode: {}", templateCode);
            return null;
        }
        MessageTemplateDTO dto = MessageTemplateConvert.INSTANCE.toDTO(entity);
        // 填充 typeCode
        if (dto.getTypeId() != null) {
            MessageTypeDTO typeDTO = messageTypeManager.getDtoById(dto.getTypeId());
            if (typeDTO != null) {
                dto.setTypeCode(typeDTO.getTypeCode());
            }
        }
        return dto;
    }

    @Override
    public boolean publish(Serializable id) {
        if (id == null) {
            log.warn("id can't be null");
            return false;
        }
        MessageTemplateDTO existing = getDtoById(id);
        if (existing == null) {
            log.warn("MessageTemplate not found by id: {}", id);
            return false;
        }
        if (existing.getEnabled() != null && existing.getEnabled()) {
            log.warn("MessageTemplate already enabled, id: {}", id);
            return false;
        }
        MessageTemplate entity = new MessageTemplate();
        entity.setId(Convert.toLong(id));
        entity.setEnabled(Boolean.TRUE);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Publish MessageTemplate fail, id:{}", id);
            return false;
        }
        log.debug("Publish MessageTemplate success, id:{}", id);
        return true;
    }

    @Override
    public boolean updateStatus(Serializable id, Boolean enabled) {
        if (id == null || enabled == null) {
            log.warn("id and enabled can't be null");
            return false;
        }
        MessageTemplate entity = new MessageTemplate();
        entity.setId(Convert.toLong(id));
        entity.setEnabled(enabled);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update MessageTemplate status fail, id:{}, enabled:{}", id, enabled);
            return false;
        }
        log.debug("Update MessageTemplate status success, id:{}, enabled:{}", id, enabled);
        return true;
    }
}
