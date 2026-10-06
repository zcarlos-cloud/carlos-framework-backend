package com.carlos.monitor.db.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 慢 SQL 分页 显示层对象
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
@Data
@Accessors(chain = true)
@Schema(description = "慢SQL分页视图对象")
public class SlowSqlPageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "慢 SQL 记录")
    private List<SlowSqlVO> records;

    @Schema(description = "下一页偏移")
    private Integer nextOffset;

    @Schema(description = "是否还有更多")
    private Boolean hasMore;

    /**
     * <p>
     * 慢 SQL 记录
     * </p>
     */
    @Data
    @Accessors(chain = true)
    @Schema(description = "慢SQL记录")
    public static class SlowSqlVO implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "SQL 指纹")
        private String fingerprint;

        @Schema(description = "样例 SQL（参数已脱敏）")
        private String sampleSql;

        @Schema(description = "库名")
        private String dbName;

        @Schema(description = "统计窗口内执行次数")
        private Long execCount;

        @Schema(description = "平均耗时（毫秒）")
        private Double avgMs;

        @Schema(description = "最大耗时（毫秒）")
        private Double maxMs;

        @Schema(description = "最近执行时间")
        private String lastExecTime;

        @Schema(description = "采集时间")
        private String collectTime;
    }
}
