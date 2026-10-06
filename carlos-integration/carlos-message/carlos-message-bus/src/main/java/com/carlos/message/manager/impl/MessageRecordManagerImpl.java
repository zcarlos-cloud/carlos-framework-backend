package com.carlos.message.manager.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.datasource.base.BaseServiceImpl;
import com.carlos.datasource.pagination.MybatisPage;
import com.carlos.datasource.pagination.PageInfo;
import com.carlos.message.convert.MessageRecordConvert;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.mapper.MessageRecordMapper;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.param.MessageRecordPageParam;
import com.carlos.message.pojo.vo.MessageRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息记录表 查询封装实现类
 * </p>
 *
 * @author Carlos
 * @date 2026年3月12日 上午11:17:05
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class MessageRecordManagerImpl extends BaseServiceImpl<MessageRecordMapper, MessageRecord> implements MessageRecordManager {

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean add(MessageRecordDTO dto) {
        MessageRecord entity = MessageRecordConvert.INSTANCE.toDO(dto);
        boolean success = save(entity);
        if (!success) {
            log.warn("Insert 'MessageRecord' data fail, entity:{}", entity);
            return false;
        }
        dto.setId(entity.getId());
        // 保存完成的后续
        log.debug("Insert 'MessageRecord' data: id:{}", entity.getId());
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
            log.warn("Remove 'MessageRecord' data fail, id:{}", id);
            return false;
        }
        log.debug("Remove 'MessageRecord' data by id:{}", id);
        return true;
    }

    @Override
    public boolean modify(MessageRecordDTO dto) {
        MessageRecord entity = MessageRecordConvert.INSTANCE.toDO(dto);
        boolean success = updateById(entity);
        if (!success) {
            log.warn("Update 'MessageRecord' data fail, entity:{}", entity);
            return false;
        }
        // 修改成功的后续操作
        log.debug("Update 'MessageRecord' data by id:{}", dto.getId());
        return true;
    }

    @Override
    public MessageRecordDTO getDtoById(Serializable id) {
        if (id == null) {
            log.warn("id is null");
            return null;
        }
        MessageRecord entity = getBaseMapper().selectById(id);
        return MessageRecordConvert.INSTANCE.toDTO(entity);
    }

    @Override
    public Paging<MessageRecordVO> getPage(MessageRecordPageParam param) {
        LambdaQueryWrapper<MessageRecord> wrapper = queryWrapper();
        wrapper.select(

            MessageRecord::getId,
            MessageRecord::getMessageId,
            MessageRecord::getTemplateCode,
            MessageRecord::getTemplateParams,
            MessageRecord::getMessageType,
            MessageRecord::getMessageTitle,
            MessageRecord::getMessageContent,
            MessageRecord::getSenderId,
            MessageRecord::getSenderName,
            MessageRecord::getSenderSystem,
            MessageRecord::getFeedbackType,
            MessageRecord::getFeedbackContent,
            MessageRecord::getPriority,
            MessageRecord::getValidUntil,
            MessageRecord::getTotalCount,
            MessageRecord::getSuccessCount,
            MessageRecord::getFailCount,
            MessageRecord::getExtras,
            MessageRecord::getCreateBy,
            MessageRecord::getCreateTime,
            MessageRecord::getUpdateBy,
            MessageRecord::getUpdateTime
        );
        // 未参与过滤的字段（前端传了也不生效）：templateParams / messageContent / feedbackType /
        // feedbackContent / validUntil / totalCount / successCount / failCount / extras
        // 以上均为 JSON 载荷、正文长文本或统计值，不适合作列表筛选项，如需支持请另行确认。
        if (StrUtil.isNotBlank(param.getMessageId())) {
            wrapper.eq(MessageRecord::getMessageId, param.getMessageId());
        }
        if (StrUtil.isNotBlank(param.getTemplateCode())) {
            wrapper.eq(MessageRecord::getTemplateCode, param.getTemplateCode());
        }
        if (StrUtil.isNotBlank(param.getMessageType())) {
            wrapper.eq(MessageRecord::getMessageType, param.getMessageType());
        }
        if (StrUtil.isNotBlank(param.getMessageTitle())) {
            wrapper.like(MessageRecord::getMessageTitle, param.getMessageTitle());
        }
        if (StrUtil.isNotBlank(param.getSenderId())) {
            wrapper.eq(MessageRecord::getSenderId, param.getSenderId());
        }
        if (StrUtil.isNotBlank(param.getSenderName())) {
            wrapper.like(MessageRecord::getSenderName, param.getSenderName());
        }
        if (StrUtil.isNotBlank(param.getSenderSystem())) {
            wrapper.eq(MessageRecord::getSenderSystem, param.getSenderSystem());
        }
        if (param.getPriority() != null) {
            wrapper.eq(MessageRecord::getPriority, param.getPriority());
        }
        if (param.getStart() != null) {
            wrapper.ge(MessageRecord::getCreateTime, param.getStart());
        }
        if (param.getEnd() != null) {
            wrapper.le(MessageRecord::getCreateTime, param.getEnd());
        }
        // 分页必须有稳定排序，否则 PostgreSQL 下翻页结果可能重复或漏行
        wrapper.orderByDesc(MessageRecord::getCreateTime);
        PageInfo<MessageRecord> page = page(pageInfo(param), wrapper);
        return MybatisPage.convert(page, MessageRecordConvert.INSTANCE::toVO);
    }

    @Override
    public MessageRecord getByMessageId(String messageId) {
        // 1. 尝试从缓存获取
        String cacheKey = "message:record:" + messageId;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSONUtil.toBean(cached, MessageRecord.class);
            }
        } catch (Exception e) {
            log.warn("读取消息缓存失败: messageId={}", messageId);
        }

        // 2. 从数据库查询
        LambdaQueryWrapper<MessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageRecord::getMessageId, messageId);
        MessageRecord record = baseMapper.selectOne(wrapper);

        // 3. 写入缓存
        if (record != null) {
            try {
                stringRedisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(record), java.time.Duration.ofMinutes(1));
            } catch (Exception e) {
                log.warn("写入消息缓存失败: messageId={}", messageId);
            }
        }
        return record;
    }

    @Override
    public List<Long> getExpiredIds(int days) {
        LocalDateTime expireTime = LocalDateTime.now().minus(days, ChronoUnit.DAYS);
        LambdaQueryWrapper<MessageRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(MessageRecord::getId)
            .lt(MessageRecord::getCreateTime, expireTime);
        return baseMapper.selectList(wrapper).stream()
            .map(MessageRecord::getId)
            .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatistics(String messageId, int successCount, int failCount) {
        MessageRecord record = getByMessageId(messageId);
        if (record == null) {
            return false;
        }
        record.setSuccessCount(successCount);
        record.setFailCount(failCount);
        boolean updated = updateById(record);
        if (updated) {
            stringRedisTemplate.delete("message:record:" + messageId);
        }
        return updated;
    }
}
