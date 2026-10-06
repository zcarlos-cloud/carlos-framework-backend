package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图片消息
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ImageMsg extends Msg {
    private static final long serialVersionUID = 5111316984979551457L;
    /**
     * 图片消息
     */

    private String mediaId;
}

