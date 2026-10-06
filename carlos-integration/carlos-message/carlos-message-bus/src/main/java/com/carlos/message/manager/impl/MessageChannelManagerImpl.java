package com.carlos.message.manager.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.message.convert.MessageChannelConvert;
import com.carlos.message.manager.MessageChannelManager;
import com.carlos.message.mapper.MessageChannelMapper;
import com.carlos.message.pojo.dto.MessageChannelDTO;
import com.carlos.message.pojo.entity.MessageChannel;
import com.carlos.message.pojo.param.MessageChannelPageParam;
import com.carlos.message.pojo.vo.MessageChannelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 消息渠道配置 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageChannelManagerImpl extends BaseServiceImpl<MessageChannelMapper, MessageChannel> implements MessageChannelManager {

    @Override
    public boolean add(MessageChannelDTO dto) {
        MessageChannel entity = MessageChannelConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MessageChannel' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'MessageChannel' data: id:{}", entity.getId());
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
            log.warn("Remove 'MessageChannel' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MessageChannel' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(MessageChannelDTO dto) {
        MessageChannel entity = MessageChannelConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MessageChannel' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'MessageChannel' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public MessageChannelDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MessageChannel entity = getBaseMapper().selectById(id);
        return MessageChannelConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<MessageChannelVO> getPage(MessageChannelPageParam param) {
        LambdaQueryWrapper<MessageChannel> wrapper = queryWrapper();
        if (StrUtil.isNotBlank(param.getChannelCode())) {
            wrapper.like(MessageChannel::getChannelCode, param.getChannelCode());
        }
        if (StrUtil.isNotBlank(param.getChannelName())) {
            wrapper.like(MessageChannel::getChannelName, param.getChannelName());
        }
        if (param.getChannelType() != null && param.getChannelType() > 0) {
            wrapper.eq(MessageChannel::getChannelType, param.getChannelType());
        }
        if (param.getEnabled() != null) {
            wrapper.eq(MessageChannel::getEnabled, param.getEnabled());
        }
        wrapper.orderByDesc(MessageChannel::getCreateTime);
        PageInfo<MessageChannel> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, MessageChannelConvert.INSTANCE::toVO);
    }

    @Override
    public MessageChannelDTO getByChannelCode(String channelCode) {
        if (StrUtil.isBlank(channelCode)) {
            log.warn("channelCode can't be blank");
            return null;
        }
        LambdaQueryWrapper<MessageChannel> wrapper = new LambdaQueryWrapper<MessageChannel>()
            .eq(MessageChannel::getChannelCode, channelCode);
        MessageChannel entity = getOne(wrapper, false);
        if (entity == null) {
            log.warn("MessageChannel not found by channelCode: {}", channelCode);
            return null;
        }
        return MessageChannelConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public List<MessageChannelDTO> listByType(Integer channelType) {
        if (channelType == null || channelType <= 0) {
            log.warn("channelType can't be null or less than 1");
            return ListUtil.empty();
        }
        LambdaQueryWrapper<MessageChannel> wrapper = new LambdaQueryWrapper<MessageChannel>()
            .eq(MessageChannel::getChannelType, channelType)
            .eq(MessageChannel::getEnabled, 1)
            .orderByDesc(MessageChannel::getWeight)
            .orderByDesc(MessageChannel::getCreateTime);
        List<MessageChannel> entities = list(wrapper);
        return MessageChannelConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public List<MessageChannelDTO> getAvailableChannels() {
        LambdaQueryWrapper<MessageChannel> wrapper = new LambdaQueryWrapper<MessageChannel>()
            .eq(MessageChannel::getEnabled, 1)
            .orderByDesc(MessageChannel::getWeight)
            .orderByDesc(MessageChannel::getCreateTime);
        List<MessageChannel> entities = list(wrapper);
        return MessageChannelConvert.INSTANCE.toDTO(entities);
    }

    @Override
    public boolean updateStatus(Serializable id, Integer enabled) {
        if (id == null || enabled == null) {
            log.warn("id and enabled can't be null");
            return false;
        }
        if (enabled < 0 || enabled > 2) {
            log.warn("enabled value must be between 0 and 2, got: {}", enabled);
            return false;
        }
        MessageChannel entity = new MessageChannel();
        entity.setId(Convert.toLong(id));
        entity.setEnabled(enabled);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update MessageChannel status fail, id:{}, enabled:{}", id, enabled);
            return false;
        }
        log.debug("Update MessageChannel status success, id:{}, enabled:{}", id, enabled);
        return true;
    }
}
