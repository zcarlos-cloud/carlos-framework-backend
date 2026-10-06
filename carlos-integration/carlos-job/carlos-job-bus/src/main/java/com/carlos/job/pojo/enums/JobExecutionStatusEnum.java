package com.carlos.job.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任务执行状态枚举
 * </p>
 *
 * @author Carlos
 * @date 2026年9月22日
 */
@AppEnum(code = "JobExecutionStatus")
@Getter
@AllArgsConstructor
public enum JobExecutionStatusEnum implements BaseEnum {

    SUCCESS(1, "成功", "Success", EnumColor.SUCCESS),
    FAILED(2, "失败", "Failed", EnumColor.DANGER),
    RUNNING(3, "执行中", "Running", EnumColor.PRIMARY);

    @EnumValue
    private final Integer code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

    public static JobExecutionStatusEnum of(Integer code) {
        for (JobExecutionStatusEnum status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }

}
