package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 閾炬帴娑堟伅
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LinkMsg extends Msg {
    private static final long serialVersionUID = 2757883691527167563L;
    /**
     * 娑堟伅鐐瑰嚮閾炬帴鍦板潃
     */

    private String messageUrl;
    /**
     * 鍥剧墖鍦板潃
     */

    private String picUrl;
    /**
     * 娑堟伅鏍囬橈紝寤鸿?00瀛楃︿互鍐
     */

    private String text;
    /**
     * 娑堟伅鎻忚堪锛屽缓璁?00瀛楃︿互鍐
     */

    private String title;
}

