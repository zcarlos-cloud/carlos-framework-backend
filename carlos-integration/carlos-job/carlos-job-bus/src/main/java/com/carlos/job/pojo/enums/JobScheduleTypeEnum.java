package com.carlos.job.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任务调度类型枚举（映射 Quartz Trigger 类型）
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@AppEnum(code = "JobScheduleType")
@Getter
@AllArgsConstructor
public enum JobScheduleTypeEnum implements BaseEnum {

    CRON(1, "Cron 表达式", "Cron Expression"),
    FIXED_INTERVAL(2, "固定间隔", "Fixed Interval"),
    ONCE(3, "一次性触发", "One-time");

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

    public static JobScheduleTypeEnum of(Integer code) {
        for (JobScheduleTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

}
