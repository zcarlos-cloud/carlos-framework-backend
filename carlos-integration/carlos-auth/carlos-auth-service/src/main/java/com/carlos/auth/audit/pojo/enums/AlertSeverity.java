package com.carlos.auth.audit.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 告警级别枚举
 * </p>
 *
 * @author Carlos
 * @date 2026-02-26
 */
@AppEnum(code = "AlertSeverity")
@Getter
@AllArgsConstructor
public enum AlertSeverity implements BaseEnum<String> {

    /**
     * 低级别
     */
    LOW("low", "低级别", "普通事件，监控即可"),

    /**
     * 中级别
     */
    MEDIUM("medium", "中级别", "需要注意，建议查看"),

    /**
     * 高级别
     */
    HIGH("high", "高级别", "需要处理，可能影响安全"),

    /**
     * 严重级别
     */
    CRITICAL("critical", "严重级别", "需要立即处理的安全事件");

    private final String code;
    private final String name;
    private final String description;

    @Override
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getDesc() {
        return description;
    }
}
