package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 鏂囦欢娑堟伅
 *
 * @author top auto create
 * @since 1.0, null
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileMsg extends Msg {
    private static final long serialVersionUID = 8133117921592134818L;
    /**
     * 濯掍綋鏂囦欢id銆傚紩鐢ㄧ殑濯掍綋鏂囦欢鏈澶?0MB
     */

    private String mediaId;

}

