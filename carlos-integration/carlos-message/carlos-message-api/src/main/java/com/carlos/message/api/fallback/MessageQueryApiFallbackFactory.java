package com.carlos.message.api.fallback;

import com.carlos.core.pagination.Paging;
import com.carlos.core.response.Result;
import com.carlos.message.api.MessageQueryApi;
import com.carlos.message.pojo.ao.MessageReceiverAO;
import com.carlos.message.pojo.ao.MessageRecordAO;
import com.carlos.message.pojo.param.MessagePageParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>
 * 消息查询服务熔断降级工厂
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Slf4j
@Component
public class MessageQueryApiFallbackFactory implements FallbackFactory<MessageQueryApi> {

    @Override
    public MessageQueryApi create(Throwable cause) {
        log.error("消息查询服务调用失败", cause);
        return new MessageQueryApi() {
            @Override
            public Result<MessageRecordAO> getById(String messageId) {
                return Result.error("消息查询服务暂不可用，请稍后重试");
            }

            @Override
            public Result<Paging<MessageRecordAO>> page(MessagePageParam param) {
                return Result.error("消息查询服务暂不可用，请稍后重试");
            }

            @Override
            public Result<List<MessageReceiverAO>> getReceivers(String messageId) {
                return Result.error("消息查询服务暂不可用，请稍后重试");
            }

            @Override
            public Result<List<MessageRecordAO>> getUnread(String userId) {
                return Result.error("消息查询服务暂不可用，请稍后重试");
            }
        };
    }
}
