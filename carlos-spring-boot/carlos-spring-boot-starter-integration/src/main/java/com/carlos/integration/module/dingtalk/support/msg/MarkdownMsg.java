package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * markdown娑堟伅
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MarkdownMsg extends Msg {
    private static final long serialVersionUID = 4352594231334542361L;
    /**
     * markdown鏍煎紡鐨勬秷鎭锛屽缓璁500瀛楃︿互鍐
     */

    private String text;
    /**
     * 棣栧睆浼氳瘽閫忓嚭鐨勫睍绀哄唴瀹?
     */

    private String title;

}
	
