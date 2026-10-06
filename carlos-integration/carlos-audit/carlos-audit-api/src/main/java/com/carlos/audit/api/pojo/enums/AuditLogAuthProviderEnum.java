package com.carlos.audit.api.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 审计日志认证源枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-03-06
 */
@AppEnum(code = "AuditLogAuthProvider")
@Getter
@AllArgsConstructor
public enum AuditLogAuthProviderEnum implements BaseEnum {

    /**
     * 本地认证
     */
    LOCAL(1, "本地认证", "Local"),

    /**
     * 微信
     */
    WECHAT(2, "微信", "WeChat"),

    /**
     * 钉钉
     */
    DINGTALK(3, "钉钉", "DingTalk"),

    /**
     * 企业微信
     */
    WECHAT_WORK(4, "企业微信", "WeCom"),

    /**
     * 飞书
     */
    FEISHU(5, "飞书", "Feishu"),

    /**
     * GitHub
     */
    GITHUB(6, "GitHub", "GitHub"),

    /**
     * GitLab
     */
    GITLAB(7, "GitLab", "GitLab"),

    /**
     * 其他第三方
     */
    OTHER(99, "其他第三方", "Other");

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }
}
