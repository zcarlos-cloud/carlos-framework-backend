package com.carlos.message.manager.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.message.convert.MessageSendLogConvert;
import com.carlos.message.manager.MessageSendLogManager;
import com.carlos.message.mapper.MessageSendLogMapper;
import com.carlos.message.pojo.dto.MessageSendLogDTO;
import com.carlos.message.pojo.entity.MessageSendLog;
import com.carlos.message.pojo.param.MessageSendLogPageParam;
import com.carlos.message.pojo.vo.MessageSendLogVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * <p>
 * 消息发送日志 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageSendLogManagerImpl extends BaseServiceImpl<MessageSendLogMapper, MessageSendLog> implements MessageSendLogManager {

    @Override
    public boolean add(MessageSendLogDTO dto) {
        MessageSendLog entity = MessageSendLogConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MessageSendLog' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'MessageSendLog' data: id:{}", entity.getId());
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
            log.warn("Remove 'MessageSendLog' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MessageSendLog' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(MessageSendLogDTO dto) {
        MessageSendLog entity = MessageSendLogConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MessageSendLog' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'MessageSendLog' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public MessageSendLogDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MessageSendLog entity = getBaseMapper().selectById(id);
        return MessageSendLogConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<MessageSendLogVO> getPage(MessageSendLogPageParam param) {
        LambdaQueryWrapper<MessageSendLog> wrapper = queryWrapper();
        wrapper.select(

            MessageSendLog::getId,
            MessageSendLog::getMessageId,
            MessageSendLog::getReceiverId,
            MessageSendLog::getChannelCode,
            MessageSendLog::getRequestParam,
            MessageSendLog::getResponseData,
            MessageSendLog::getSuccess,
            MessageSendLog::getErrorCode,
            MessageSendLog::getErrorMessage,
            MessageSendLog::getCostTime,
            MessageSendLog::getCreateTime
        );
        // 未参与过滤的字段（前端传了也不生效）：requestParam / responseData / errorMessage / costTime
        // 前两者是请求与响应的原始报文（长文本），后两者是结果统计值。
        if (StrUtil.isNotBlank(param.getMessageId())) {
            wrapper.eq(MessageSendLog::getMessageId, param.getMessageId());
        }
        if (param.getReceiverId() != null) {
            wrapper.eq(MessageSendLog::getReceiverId, param.getReceiverId());
        }
        if (StrUtil.isNotBlank(param.getChannelCode())) {
            wrapper.eq(MessageSendLog::getChannelCode, param.getChannelCode());
        }
        if (param.getSuccess() != null) {
            wrapper.eq(MessageSendLog::getSuccess, param.getSuccess());
        }
        if (StrUtil.isNotBlank(param.getErrorCode())) {
            wrapper.eq(MessageSendLog::getErrorCode, param.getErrorCode());
        }
        if (param.getStart() != null) {
            wrapper.ge(MessageSendLog::getCreateTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.le(MessageSendLog::getCreateTime, param.getEnd());
        }
        // 分页必须有稳定排序，否则 PostgreSQL 下翻页结果可能重复或漏行
        wrapper.orderByDesc(MessageSendLog::getCreateTime);
        PageInfo<MessageSendLog> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, MessageSendLogConvert.INSTANCE::toVO);
    }

}
