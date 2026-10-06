package com.carlos.job.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 调度过期策略枚举（映射 Quartz misfire 指令）
 * </p>
 * <p>
 * 任务因调度器停机/线程耗尽错过触发时间后的处理方式
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@AppEnum(code = "JobMisfireStrategy")
@Getter
@AllArgsConstructor
public enum JobMisfireStrategyEnum implements BaseEnum {

    SMART_POLICY(0, "智能策略", "Smart Policy"),
    IGNORE_MISFIRES(1, "忽略错过（补偿全部）", "Ignore Misfires (compensate all)"),
    FIRE_AND_PROCEED(2, "立即执行一次", "Fire Once Now"),
    DO_NOTHING(3, "等待下次触发", "Wait for Next");

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

    public static JobMisfireStrategyEnum of(Integer code) {
        for (JobMisfireStrategyEnum strategy : values()) {
            if (strategy.getCode().equals(code)) {
                return strategy;
            }
        }
        return null;
    }

    /**
     * 空值兜底为智能策略
     *
     * @param code 策略编码
     * @return 策略枚举
     */
    public static JobMisfireStrategyEnum ofDefault(Integer code) {
        JobMisfireStrategyEnum strategy = of(code);
        return strategy == null ? SMART_POLICY : strategy;
    }

}
