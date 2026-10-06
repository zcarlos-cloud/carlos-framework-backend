package com.carlos.integration.module.dingtalk.support.msg;

import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OA娑堟伅
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OAMsg extends Msg {
    private static final long serialVersionUID = 2599774517599455299L;
    /**
     * 娑堟伅浣?
     */

    private OapiMessageCorpconversationAsyncsendV2Request.Body body;
    /**
     * 娑堟伅澶撮儴鍐呭
     */

    private OapiMessageCorpconversationAsyncsendV2Request.Head head;
    /**
     * 娑堟伅鐐瑰嚮閾炬帴鍦板潃锛屽綋鍙戦佹秷鎭涓哄皬绋嬪簭鏃舵敮鎸佸皬绋嬪簭璺宠浆閾炬?
     */

    private String messageUrl;
    /**
     * PC绔鐐瑰嚮娑堟伅鏃惰烦杞鍒扮殑鍦板潃
     */

    private String pcMessageUrl;
    /**
     * 鐘舵佹爮
     */

    private OapiMessageCorpconversationAsyncsendV2Request.StatusBar statusBar;
}
