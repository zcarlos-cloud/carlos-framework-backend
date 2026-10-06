package com.carlos.integration.module.dingtalk.support.msg;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoiceMsg extends Msg {
    private static final long serialVersionUID = 8116843149852161251L;
    /**
     * 姝ｆ暣鏁帮紝灏忎簬60锛岃〃绀洪煶棰戞椂闀?
     */

    private String duration;
    /**
     * 濯掍綋鏂囦欢id銆?MB锛屾挱鏀鹃暱搴︿笉瓒呰繃60s锛孉MR鏍煎紡
     */

    private String mediaId;

}
