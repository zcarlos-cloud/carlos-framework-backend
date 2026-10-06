package com.carlos.message.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.message.convert.MessageConvert;
import com.carlos.message.manager.MessageReceiverManager;
import com.carlos.message.manager.MessageRecordManager;
import com.carlos.message.pojo.dto.MessageReceiverDTO;
import com.carlos.message.pojo.dto.MessageRecordDTO;
import com.carlos.message.pojo.entity.MessageReceiver;
import com.carlos.message.pojo.entity.MessageRecord;
import com.carlos.message.pojo.enums.MessageReceiverStatusEnum;
import com.carlos.message.pojo.param.MessagePageParam;
import com.carlos.message.pojo.param.MessageRecordPageParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 消息查询服务
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageQueryService {

    private final MessageRecordManager messageRecordManager;
    private final MessageReceiverManager messageReceiverManager;
    private final MessageConvert messageConvert;

    /**
     * 根据消息ID查询详情
     *
     * @param messageId 消息ID
     * @return 消息记录
     */
    public Result<MessageRecordDTO> getById(String messageId) {
        log.info("查询消息详情: {}", messageId);
        if (StrUtil.isBlank(messageId)) {
            return Result.error("消息ID不能为空");
        }
        MessageRecord record = messageRecordManager.getByMessageId(messageId);
        if (record == null) {
            return Result.error("消息不存在: " + messageId);
        }
        return Result.success(messageConvert.toDTO(record));
    }

    /**
     * 分页查询消息记录
     *
     * @param param 查询参数
     * @return 分页结果
     */
    public Result<Paging<MessageRecordDTO>> page(MessagePageParam param) {
        log.info("分页查询消息记录: {}", param);
        MessageRecordPageParam pageParam = buildRecordPageParam(param);
        Paging<com.carlos.message.pojo.vo.MessageRecordVO> voPage = messageRecordManager.getPage(pageParam);
        // 将 VO 转换为 DTO（VO 字段是 DTO 的子集）
        Paging<MessageRecordDTO> result = new Paging<>();
        result.setTotal(voPage.getTotal());
        result.setCurrent(voPage.getCurrent());
        result.setSize(voPage.getSize());
        result.setPages(voPage.getPages());
        List<MessageRecordDTO> dtoList = voPage.getRecords() == null ? new ArrayList<>()
            : voPage.getRecords().stream().map(vo -> {
            MessageRecordDTO dto = new MessageRecordDTO();
            dto.setMessageId(vo.getMessageId());
            dto.setTemplateCode(vo.getTemplateCode());
            dto.setMessageType(vo.getMessageType());
            dto.setMessageTitle(vo.getMessageTitle());
            dto.setMessageContent(vo.getMessageContent());
            dto.setSenderId(vo.getSenderId());
            dto.setSenderName(vo.getSenderName());
            dto.setSenderSystem(vo.getSenderSystem());
            dto.setPriority(vo.getPriority());
            dto.setTotalCount(vo.getTotalCount());
            dto.setSuccessCount(vo.getSuccessCount());
            dto.setFailCount(vo.getFailCount());
            dto.setCreateTime(vo.getCreateTime());
            return dto;
        }).collect(Collectors.toList());
        result.setRecords(dtoList);
        return Result.success(result);
    }

    /**
     * 查询消息接收人列表
     *
     * @param messageId 消息ID
     * @return 接收人列表
     */
    public Result<List<MessageReceiverDTO>> getReceivers(String messageId) {
        log.info("查询消息接收人列表: {}", messageId);
        if (StrUtil.isBlank(messageId)) {
            return Result.error("消息ID不能为空");
        }
        List<MessageReceiver> receivers = messageReceiverManager.listByMessageId(messageId);
        return Result.success(messageConvert.toReceiverList(receivers));
    }

    /**
     * 查询用户的未读消息（状态不是已读/撤回的消息）
     *
     * @param userId 用户ID
     * @return 未读消息列表
     */
    public Result<List<MessageRecordDTO>> getUnread(String userId) {
        log.info("查询用户未读消息: {}", userId);
        if (StrUtil.isBlank(userId)) {
            return Result.error("用户ID不能为空");
        }

        // 查询该用户所有未读的消息接收记录（排除已读/撤回状态）
        LambdaQueryWrapper<MessageReceiver> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MessageReceiver::getReceiverId, userId)
            .notIn(MessageReceiver::getStatus,
                MessageReceiverStatusEnum.READ.getCode(),
                MessageReceiverStatusEnum.REVOKED.getCode())
            .orderByDesc(MessageReceiver::getCreateTime);

        List<MessageReceiver> receiverList = messageReceiverManager.list(wrapper);
        if (receiverList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 根据 messageId 去重，查询消息记录详情
        List<String> messageIds = receiverList.stream()
            .map(MessageReceiver::getMessageId)
            .distinct()
            .collect(Collectors.toList());

        List<MessageRecordDTO> dtoList = new ArrayList<>();
        for (String messageId : messageIds) {
            MessageRecord record = messageRecordManager.getByMessageId(messageId);
            if (record != null) {
                dtoList.add(messageConvert.toDTO(record));
            }
        }
        return Result.success(dtoList);
    }

    /**
     * 查询消息状态
     *
     * @param messageId 消息ID
     * @return 消息状态（基于接收人发送情况）
     */
    public Result<Integer> queryStatus(String messageId) {
        log.info("查询消息状态: {}", messageId);
        if (StrUtil.isBlank(messageId)) {
            return Result.error("消息ID不能为空");
        }
        MessageRecord record = messageRecordManager.getByMessageId(messageId);
        if (record == null) {
            return Result.error("消息不存在");
        }
        // 根据成功/失败数计算聚合状态
        // 全部成功：2（已发送）；全部失败：5（失败）；部分：2（已发送）；无记录：0（待发送）
        int total = record.getTotalCount() != null ? record.getTotalCount() : 0;
        int success = record.getSuccessCount() != null ? record.getSuccessCount() : 0;
        int fail = record.getFailCount() != null ? record.getFailCount() : 0;
        int status;
        if (total == 0) {
            status = MessageReceiverStatusEnum.PENDING.getCode();
        } else if (success == 0 && fail == total) {
            status = MessageReceiverStatusEnum.FAILED.getCode();
        } else {
            status = MessageReceiverStatusEnum.SENT.getCode();
        }
        return Result.success(status);
    }

    /**
     * 将 MessagePageParam 转换为 MessageRecordPageParam
     */
    private MessageRecordPageParam buildRecordPageParam(MessagePageParam param) {
        MessageRecordPageParam pageParam = new MessageRecordPageParam();
        pageParam.setCurrent(param.getCurrent());
        pageParam.setSize(param.getSize());
        pageParam.setMessageId(param.getMessageId());
        pageParam.setTemplateCode(param.getTemplateCode());
        pageParam.setMessageType(param.getMessageType());
        pageParam.setSenderId(param.getSenderId());
        pageParam.setSenderSystem(param.getSenderSystem());
        if (param.getStartTime() != null) {
            pageParam.setStart(param.getStartTime());
        }
        if (param.getEndTime() != null) {
            pageParam.setEnd(param.getEndTime());
        }
        return pageParam;
    }
}
