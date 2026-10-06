package com.carlos.job.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任务触发类型枚举
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@AppEnum(code = "JobTriggerType")
@Getter
@AllArgsConstructor
public enum JobTriggerTypeEnum implements BaseEnum {

    CRON(1, "Cron 定时", "Cron"),
    MANUAL(2, "手动触发", "Manual"),
    RETRY(3, "失败重试", "Retry");

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

    public static JobTriggerTypeEnum of(Integer code) {
        for (JobTriggerTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }

}
