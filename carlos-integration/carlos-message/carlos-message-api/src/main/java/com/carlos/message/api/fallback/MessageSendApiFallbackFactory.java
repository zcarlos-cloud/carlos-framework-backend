package com.carlos.message.api.fallback;

import com.carlos.core.response.Result;
import com.carlos.message.api.MessageSendApi;
import com.carlos.message.pojo.ao.MessageStatusAO;
import com.carlos.message.pojo.ao.SendResultAO;
import com.carlos.message.pojo.param.MessageSendParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息发送服务熔断降级工厂
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@Component
public class MessageSendApiFallbackFactory implements FallbackFactory<MessageSendApi> {

    @Override
    public MessageSendApi create(Throwable cause) {
        log.error("消息发送服务调用失败", cause);
        return new MessageSendApi() {
            @Override
            public Result<SendResultAO> send(MessageSendParam param) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }

            @Override
            public Result<String> sendAsync(MessageSendParam param) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }

            @Override
            public Result<SendResultAO> sendBatch(List<MessageSendParam> params) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }

            @Override
            public Result<String> schedule(MessageSendParam param, LocalDateTime scheduleTime) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }

            @Override
            public Result<Boolean> revoke(String messageId) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }

            @Override
            public Result<MessageStatusAO> queryStatus(String messageId) {
                return Result.error("消息发送服务暂不可用，请稍后重试");
            }
        };
    }
}
