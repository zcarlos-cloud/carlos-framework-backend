package com.carlos.monitor.db.pojo.dto;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * SQL 控制台执行结果
 * </p>
 *
 * @author Carlos
 * @date 2026年10月4日
 */
public record SqlExecResult(List<String> columns, List<Map<String, Object>> rows,
                            Integer affectedRows, Boolean truncated) {
}
