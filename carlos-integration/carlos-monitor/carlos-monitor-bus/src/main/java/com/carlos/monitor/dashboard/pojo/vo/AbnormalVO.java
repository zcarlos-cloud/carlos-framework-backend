package com.carlos.monitor.dashboard.pojo.vo;

import com.carlos.monitor.alarm.pojo.vo.AlarmRecordVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 异常滚动 显示层对象（异常实例 + 最新告警）
 * </p>
 *
 * @author Carlos
 * @date 2026年10月5日
 */
@Data
@Accessors(chain = true)
@Schema(description = "异常滚动视图对象")
public class AbnormalVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "异常实例列表")
    private List<AbnormalInstanceVO> abnormalInstances;

    @Schema(description = "最新未恢复告警（滚动条）")
    private List<AlarmRecordVO> alarms;

    /**
     * 异常实例
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "异常实例")
    public static class AbnormalInstanceVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "实例ID")
        private Long instanceId;

        @Schema(description = "实例名称")
        private String instanceName;

        @Schema(description = "实例类型")
        private String instanceType;

        @Schema(description = "环境")
        private String env;

        @Schema(description = "健康状态")
        private String healthStatus;

        @Schema(description = "最近成功采集时间")
        private LocalDateTime lastCollectTime;
    }
}
