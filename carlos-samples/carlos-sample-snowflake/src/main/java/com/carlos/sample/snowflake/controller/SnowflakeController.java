package com.carlos.sample.snowflake.controller;

import com.carlos.snowflake.SnowflakeUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 雪花算法 ID 生成接口
 * </p>
 *
 * @author carlos
 * @date 2026/3/15
 */
@RestController
@RequestMapping("snowflake")
@Tag(name = "雪花算法ID生成")
@Slf4j
public class SnowflakeController {

    /**
     * 获取长整型 ID
     *
     * @return long ID
     */
    @GetMapping("longId")
    @Operation(summary = "获取长整型ID")
    public long longId() {
        long id = SnowflakeUtil.longId();
        log.info("Generated long id: {}", id);
        return id;
    }

    /**
     * 获取字符串 ID
     *
     * @return String ID
     */
    @GetMapping("strId")
    @Operation(summary = "获取字符串ID")
    public String strId() {
        String id = SnowflakeUtil.strId();
        log.info("Generated string id: {}", id);
        return id;
    }

    /**
     * 批量获取 ID
     *
     * @param count 数量
     * @return ID 列表
     */
    @GetMapping("batch")
    @Operation(summary = "批量获取ID")
    public Map<String, Object> batch(int count) {
        if (count <= 0 || count > 100) {
            count = 10;
        }
        Map<String, Object> result = new HashMap<>(4);
        long[] longIds = new long[count];
        String[] strIds = new String[count];
        for (int i = 0; i < count; i++) {
            longIds[i] = SnowflakeUtil.longId();
            strIds[i] = SnowflakeUtil.strId();
        }
        result.put("count", count);
        result.put("longIds", longIds);
        result.put("strIds", strIds);
        log.info("Generated {} ids", count);
        return result;
    }
}
