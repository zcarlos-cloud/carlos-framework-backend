package com.carlos.sms.ums.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class UmsSendRequest {

    /** 企业编号 */
    @JsonProperty("SpCode")
    private String spCode;
    /** 用户名称 */
    @JsonProperty("LoginName")
    private String loginName;
    /** 用户密码 */
    @JsonProperty("Password")
    private String password;
    /** 短信内容, 最大402个字或字符（短信内容要求的编码为gb2312或gbk），短信发送必须按照短信模板，否则就会报模板不符，短信模板说明见4.1.1。 */
    @JsonProperty("MessageContent")
    private String messageContent;
    /** 手机号码(多个号码用”,”分隔)，最多1000个号码 */
    @JsonProperty("UserNumber")
    private String userNumber;
    /** 流水号，20位数字，唯一 （规则自定义,建议时间格式精确到毫秒）必填参数，与回执接口中的流水号一一对应，不传后面回执接口无法查询数据。 */
    @JsonProperty("SerialNumber")
    private String serialNumber;
    /** 预约发送时间，格式:yyyyMMddHHmmss,如‘20090901010101’，立即发送请填空（预约时间要写当前时间5分钟之后的时间，若预约时间少于5分钟，则为立即发送。） */
    @JsonProperty("ScheduleTime")
    private String scheduleTime;
    /** 提交时检测方式
     1 --- 提交号码中有效的号码仍正常发出短信，无效的号码在返回参数faillist中列出

     不为1 或该参数不存在 --- 提交号码中只要有无效的号码，那么所有的号码都不发出短信，所有的号码在返回参数faillist中列出 */
    @JsonProperty("f")
    private String f;
}
