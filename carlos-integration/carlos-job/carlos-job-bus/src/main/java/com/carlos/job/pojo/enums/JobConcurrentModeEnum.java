package com.carlos.job.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任务并发模式枚举
 * </p>
 * <p>
 * SERIAL 串行：同一任务集群级互斥（Quartz JDBC 集群下全集群生效）；
 * PARALLEL 并行：允许同一任务并发执行多个实例
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@AppEnum(code = "JobConcurrentMode")
@Getter
@AllArgsConstructor
public enum JobConcurrentModeEnum implements BaseEnum {

    SERIAL(1, "串行（集群互斥）", "Serial (cluster mutex)"),
    PARALLEL(2, "并行", "Parallel");

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

    public static JobConcurrentModeEnum of(Integer code) {
        for (JobConcurrentModeEnum mode : values()) {
            if (mode.getCode().equals(code)) {
                return mode;
            }
        }
        return null;
    }

    /**
     * 空值兜底为串行（现状行为，防任务重入）
     *
     * @param code 模式编码
     * @return 模式枚举
     */
    public static JobConcurrentModeEnum ofDefault(Integer code) {
        JobConcurrentModeEnum mode = of(code);
        return mode == null ? SERIAL : mode;
    }

}
