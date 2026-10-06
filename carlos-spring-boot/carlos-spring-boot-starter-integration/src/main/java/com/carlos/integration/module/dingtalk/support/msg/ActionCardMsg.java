package com.carlos.integration.module.dingtalk.support.msg;

import com.dingtalk.api.request.OapiMessageCorpconversationAsyncsendV2Request;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 鍗＄墖娑堟伅
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ActionCardMsg extends Msg {
    private static final long serialVersionUID = 2194296449645234598L;
    /**
     * 浣跨敤鐙绔嬭烦杞珹ctionCard鏍峰紡鏃剁殑鎸夐挳鍒楄〃锛涘繀椤讳笌btn_orientation鍚屾椂璁剧疆
     */

    private List<OapiMessageCorpconversationAsyncsendV2Request.BtnJsonList> btnJsonList;
    /**
     * 浣跨敤鐙绔嬭烦杞珹ctionCard鏍峰紡鏃剁殑鎸夐挳鎺掑垪鏂瑰紡锛岀珫鐩存帓鍒?0)锛屾í鍚戞帓鍒?1)锛涘繀椤讳笌btn_json_list鍚屾椂璁剧疆
     */

    private String btnOrientation;
    /**
     * 娑堟伅鍐呭癸紝鏀鎸乵arkdown锛岃娉曞弬鑰冩爣鍑唌arkdown璇娉曘傚缓璁?000涓瀛楃︿互鍐?
     */

    private String markdown;
    /**
     * 浣跨敤鏁翠綋璺宠浆ActionCard鏍峰紡鏃剁殑鏍囬橈紝蹇呴』涓巗ingle_url鍚屾椂璁剧疆锛屾渶闀?0涓瀛楃?
     */

    private String singleTitle;
    /**
     * 娑堟伅鐐瑰嚮閾炬帴鍦板潃锛屽綋鍙戦佹秷鎭涓哄皬绋嬪簭鏃舵敮鎸佸皬绋嬪簭璺宠浆閾炬帴锛屾渶闀?00涓瀛楃?
     */

    private String singleUrl;
    /**
     * 閫忓嚭鍒颁細璇濆垪琛ㄥ拰閫氱煡鐨勬枃妗堬紝鏈闀?4涓瀛楃?
     */

    private String title;

}
