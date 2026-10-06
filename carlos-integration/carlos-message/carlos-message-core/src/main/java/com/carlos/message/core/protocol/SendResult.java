package com.carlos.message.core.protocol;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>
 * 发送结果
 * </p>
 *
 * @author Carlos
 * @date 2026/3/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 渠道返回的消息ID
     */
    private String channelMessageId;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    private Long costTime;

    /**
     * 创建成功结果
     *
     * @param channelMessageId 渠道消息ID
     * @return 结果
     */
    public static SendResult success(String channelMessageId) {
        return SendResult.builder()
                .success(true)
                .channelMessageId(channelMessageId)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorCode 错误码
     * @param errorMsg  错误信息
     * @return 结果
     */
    public static SendResult fail(String errorCode, String errorMsg) {
        return SendResult.builder()
                .success(false)
                .errorCode(errorCode)
            .errorMessage(errorMsg)
                .build();
    }

    /**
     * 创建失败结果
     *
     * @param errorMsg 错误信息
     * @return 结果
     */
    public static SendResult fail(String errorMsg) {
        return fail(null, errorMsg);
    }
}
