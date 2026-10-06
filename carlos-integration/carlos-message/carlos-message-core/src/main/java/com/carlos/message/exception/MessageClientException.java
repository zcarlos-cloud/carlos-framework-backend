package com.carlos.message.exception;


import com.carlos.core.response.ErrorCode;

/**
 * <p>
 *
 * </p>
 *
 * @author Carlos
 * @date 2022/1/12
 */
public class MessageClientException extends com.carlos.message.exception.MessageException {

    public MessageClientException() {
        super("消息客户端异常！");
    }

    public MessageClientException(String message) {
        super(message);
    }

    public MessageClientException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public MessageClientException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MessageClientException(Throwable cause) {
        super(cause);
    }

    public MessageClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
