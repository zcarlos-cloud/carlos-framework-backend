package com.carlos.job.pojo.enums;

import com.carlos.core.enums.AppEnum;
import com.carlos.core.enums.BaseEnum;
import com.carlos.core.enums.EnumColor;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <p>
 * 任务状态枚举（Quartz Trigger 状态透传）
 * </p>
 * <p>
 * job_info.status 直接存 Quartz TriggerState 名（NORMAL/PAUSED/ERROR 等），承载字段保持
 * String 不动；本枚举仅作为 /app/enums 字典源，供前端状态下拉与状态列渲染。
 * </p>
 *
 * @author Carlos
 * @date 2026年9月29日
 */
@AppEnum(code = "JobStatus")
@Getter
@AllArgsConstructor
public enum JobStatusEnum implements BaseEnum {

    NONE("NONE", "未调度", "None", EnumColor.MUTED),

    NORMAL("NORMAL", "正常", "Normal", EnumColor.SUCCESS),

    PAUSED("PAUSED", "暂停", "Paused", EnumColor.WARNING),

    COMPLETE("COMPLETE", "已完成", "Complete", EnumColor.MUTED),

    ERROR("ERROR", "异常", "Error", EnumColor.DANGER),

    BLOCKED("BLOCKED", "阻塞中", "Blocked", EnumColor.WARNING);

    private final String code;

    private final String desc;

    /** 英文描述：随 /app/enums 下发，前端按当前语言取用 */
    private final String descEn;

    /** 语义色：随 /app/enums 下发，避免前端为每个枚举值维护颜色表 */
    private final EnumColor color;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getDesc() {
        return desc;
    }

}
