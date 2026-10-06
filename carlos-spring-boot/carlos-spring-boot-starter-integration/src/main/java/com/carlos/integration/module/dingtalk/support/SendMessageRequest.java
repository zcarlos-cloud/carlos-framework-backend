package com.carlos.integration.module.dingtalk.support;

import com.carlos.integration.module.dingtalk.support.msg.Msg;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * <p>
 *   娑堟伅鍙戦佸弬鏁?
 * </p>
 *
 * @author Carlos
 * @date 2025-04-15 00:08
 */
@Data
@Accessors(chain = true)
public class SendMessageRequest {

    /**
     * 鎺ㄩ佸唴瀹?宸茶繃鏃讹紝浣跨敤msg杩涜屾浛浠
     */
    @Deprecated
    private String context;
    /**
     * 鎺ㄩ佸硅薄鐢佃?
     */
    private List<String> pushPhoneList;
    /**
     * 鎺ㄩ侀儴闂╥d
     */
    private List<String> pushDeptList;

    /** 娑堟伅鍐呭 */
    private Msg msg;

}
