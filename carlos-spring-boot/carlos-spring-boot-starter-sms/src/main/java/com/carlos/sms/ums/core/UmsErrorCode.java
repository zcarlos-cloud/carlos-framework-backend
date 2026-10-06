package com.carlos.sms.ums.core;


import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum UmsErrorCode {
    CODE_0(0, "发送短信成功"),
    CODE_1(1, "提交参数不能为空"),
    CODE_2(2, "账号无效或权限不足"),
    CODE_3(3, "账号密码错误,"),
    CODE_4(4, "预约发送时间格式不正确，应为yyyyMMddHHmmss"),
    CODE_5(5, "IP不合法,"),
    CODE_6(6, "号码中含有无效号码或不在规定的号段或为免打扰号码（包含系统黑名单号码）"),
    CODE_7(7, "非法关键字"),
    CODE_8(8, "内容长度超过上限，最大402字或字符"),
    CODE_9(9, "接受号码过多，最大1000"),
    CODE_12(12, "您尚未订购[普通短信业务]，暂不能发送该类信息"),
    CODE_13(13, "您的[普通短信业务]剩余数量发送不足，暂不能发送该类信息"),
    CODE_14(14, "流水号格式不正确"),
    CODE_15(15, "流水号重复"),
    CODE_16(16, "超出发送上限（操作员帐户当日发送上限）"),
    CODE_17(17, "余额不足"),
    CODE_18(18, "扣费不成功"),
    CODE_20(20, "系统错误"),
    CODE_21(21, "密码错误次数达到5次"),
    CODE_24(24, "帐户状态不正常"),
    CODE_25(25, "账户权限不足"),
    CODE_26(26, "需要人工审核"),
    CODE_28(28, "发送内容与模板不符"),
    CODE_29(29, "扩展号太长或不是数字&accnum="),
    CODE_32(32, "同一号码相同内容发送次数太多（默认24小时内，验证码类发送5次或相同内容3次以上会报此错误。）"),
    CODE_33(33, "同一号码验证码提交过快");


    private final Integer code;
    private final String message;

}
