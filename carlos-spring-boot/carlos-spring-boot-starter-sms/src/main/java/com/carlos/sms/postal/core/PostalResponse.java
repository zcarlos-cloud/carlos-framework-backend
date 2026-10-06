package com.carlos.sms.postal.core;

import lombok.Data;

/**
 * <p>
 * 邮政云短信响应结果
 * </p>
 *
 * @author Carlos
 * @date 2023/11/21 20:46
 */
@Data
public class PostalResponse {

    private String errorCode;
    private String errorMsg;
    private Object data = null;

}
