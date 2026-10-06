package com.carlos.message.api;

import com.carlos.core.response.Result;
import com.carlos.message.ServiceNameConstant;
import com.carlos.message.api.fallback.MessageSendApiFallbackFactory;
import com.carlos.message.pojo.ao.MessageStatusAO;
import com.carlos.message.pojo.ao.SendResultAO;
import com.carlos.message.pojo.param.MessageSendParam;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 消息发送服务（Feign接口定义）
 * </p>
 *
 * 路径规范：以 /api 开头
 * 熔断降级：配置 fallbackFactory
 *
 * @author Carlos
 * @date 2026/3/11
 */
@FeignClient(
    name = ServiceNameConstant.SERVICE_NAME,
        contextId = "messageSendApi",
        path = "/api/message",
        fallbackFactory = MessageSendApiFallbackFactory.class
)
public interface MessageSendApi {

    /**
     * 同步发送消息
     *
     * @param param 发送请求
     * @return 发送结果
     */
    @PostMapping("/send")
    Result<SendResultAO> send(@RequestBody @Valid MessageSendParam param);

    /**
     * 异步发送消息
     *
     * @param param 发送请求
     * @return 消息ID
     */
    @PostMapping("/sendAsync")
    Result<String> sendAsync(@RequestBody @Valid MessageSendParam param);

    /**
     * 批量发送消息
     *
     * @param params 发送请求列表
     * @return 批量发送结果
     */
    @PostMapping("/sendBatch")
    Result<SendResultAO> sendBatch(@RequestBody @Valid List<MessageSendParam> params);

    /**
     * 定时发送消息
     *
     * @param param        发送请求
     * @param scheduleTime 定时时间
     * @return 消息ID
     */
    @PostMapping("/schedule")
    Result<String> schedule(
            @RequestBody @Valid MessageSendParam param,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime scheduleTime
    );

    /**
     * 撤回消息
     *
     * @param messageId 消息ID
     * @return 是否成功
     */
    @PostMapping("/revoke")
    Result<Boolean> revoke(@RequestParam("messageId") String messageId);

    /**
     * 查询消息状态
     *
     * @param messageId 消息ID
     * @return 消息状态
     */
    @GetMapping("/status")
    Result<MessageStatusAO> queryStatus(@RequestParam("messageId") String messageId);
}
