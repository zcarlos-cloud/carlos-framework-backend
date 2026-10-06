package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文本消息
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TextMsg extends Msg {
    private static final long serialVersionUID = 7136822967334988419L;
    /**
     * 文本消息
     */

    private String content;

}


