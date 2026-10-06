package com.carlos.sms.wocloud.core;

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
public class WoCloudResponse {

    /**
     * 错误代码：0 成功
     */
    private Integer code;
    private String msg;
    private Object data = null;

}
