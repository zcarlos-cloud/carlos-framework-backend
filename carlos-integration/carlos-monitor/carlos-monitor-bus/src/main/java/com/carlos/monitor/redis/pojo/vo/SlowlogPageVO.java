package com.carlos.monitor.redis.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * Redis 慢日志分页 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月3日
 */
@Data
@Accessors(chain = true)
@Schema(description = "Redis慢日志分页视图对象")
public class SlowlogPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "慢日志记录")
    private List<SlowlogVO> records;

    @Schema(description = "下一页偏移")
    private Integer nextOffset;

    @Schema(description = "是否还有更多")
    private Boolean hasMore;

    /**
     * <p>
     * 慢日志记录
     * </p>
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "慢日志记录")
    public static class SlowlogVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "节点地址")
        private String node;

        @Schema(description = "实例侧 SLOWLOG ID")
        private Long slowlogId;

        @Schema(description = "命令执行时间")
        private String ts;

        @Schema(description = "耗时（微秒）")
        private Long durationUs;

        @Schema(description = "命令文本（value 已脱敏）")
        private String command;

        @Schema(description = "客户端地址")
        private String clientAddr;
    }
}
