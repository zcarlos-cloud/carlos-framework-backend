package com.carlos.json.util;

import cn.hutool.core.date.*;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;

/**
 * <p>
 * String 转换器工具类
 * </p>
 *
 * @author carlos
 * @date 2020/4/14 12:58
 */
@Slf4j
public class ConvertUtil {

    /**
     * 日期格式化数组
     */
    private static final String[] FORMATS = {
        DatePattern.NORM_DATETIME_MS_PATTERN,
        DatePattern.NORM_DATETIME_PATTERN,
        DatePattern.NORM_DATETIME_MINUTE_PATTERN,
        "yyyy-MM-dd HH",
        DatePattern.NORM_DATE_PATTERN,
        "yyyy-MM"
    };

    /**
     * 如果日期字符串为空,则直接返回空 使用格式化组进行格式化,如果解析成功,则直接返回 否则,抛出非法参数异常
     *
     * @param source 字符串日期
     * @return 解析后的日期类型:java.util.Date
     * @author carlos
     * @date 2020/4/14 12:18
     */
    public static Date string2Date(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        source = source.trim();
        DateTime dateTime = DateUtil.parse(source, FORMATS);
        return dateTime.toJdkDate();
    }

    /**
     * String 转 Double
     *
     * @param source 字符串
     * @author carlos
     * @date 2020/4/14 13:00
     */
    public static Double string2Double(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        if (NumberUtil.isDouble(source)) {
            return Double.parseDouble(source);
        }
        if (NumberUtil.isNumber(source)) {
            return Double.valueOf(source);
        }
        return null;
    }

    /**
     * String 转 Integer
     *
     * @param source 字符串
     * @author carlos
     * @date 2020/4/14 13:00
     */
    public static Integer string2Integer(String source) {
        if (StringUtils.isBlank(source)) {
            return null;
        }
        if (NumberUtil.isInteger(source)) {
            return Integer.parseInt(source);
        }

        if (NumberUtil.isNumber(source)) {
            return Integer.valueOf(source);
        }
        return null;
    }

    /**
     * string2LocalDateTime
     *
     * @param source 带日期和时间的字符串时间
     * @return java.time.LocalDate
     * @author carlos
     * @date 2022/3/18 14:01
     */
    public static LocalDateTime string2LocalDateTime(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }
        try {
            return LocalDateTimeUtil.parse(source);
        } catch (Exception e) {

        }

        try {
            Calendar calendar = DateUtil.parseByPatterns(source,
                DatePattern.NORM_DATETIME_PATTERN,
                DatePattern.NORM_DATETIME_MINUTE_PATTERN,
                DatePattern.NORM_DATE_PATTERN,
                DatePattern.NORM_MONTH_PATTERN,
                DatePattern.NORM_DATETIME_MS_PATTERN,
                DatePattern.CHINESE_DATE_TIME_PATTERN,
                DatePattern.UTC_MS_WITH_ZONE_OFFSET_PATTERN
            );
            return CalendarUtil.toLocalDateTime(calendar);
        } catch (Exception e) {
            log.error("Can't parse [{}]  To LocalDateTime", source);
            return null;
        }
    }

    /**
     * string2LocalDate
     *
     * @param source 包含 年月日的字符串时间格式
     * @return java.time.LocalDate
     * @author carlos
     * @date 2022/3/18 14:11
     */
    public static LocalDate string2LocalDate(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        try {
            return LocalDateTimeUtil.parseDate(source);
        } catch (Exception e) {

        }

        Calendar calendar;
        try {
            calendar = DateUtil.parseByPatterns(source,
                DatePattern.NORM_DATETIME_PATTERN,
                DatePattern.NORM_DATETIME_MINUTE_PATTERN,
                DatePattern.NORM_DATE_PATTERN,
                DatePattern.NORM_MONTH_PATTERN,
                DatePattern.NORM_DATETIME_MS_PATTERN,
                DatePattern.CHINESE_DATE_TIME_PATTERN,
                DatePattern.UTC_MS_WITH_ZONE_OFFSET_PATTERN
            );
            return CalendarUtil.toLocalDateTime(calendar).toLocalDate();
        } catch (DateException e) {
            log.error("Can't parse [{}] -> 'LocalDate'", source);
            return null;
        }
    }

    /**
     * string2LocalTime
     *
     * @param source 参数0
     * @return java.time.LocalTime
     * @author carlos
     * @date 2022/3/18 14:07
     */
    public static LocalTime string2LocalTime(String source) {
        if (StrUtil.isBlank(source)) {
            return null;
        }

        Calendar calendar = DateUtil.parseByPatterns(source,
            DatePattern.NORM_DATETIME_PATTERN,
            DatePattern.NORM_DATETIME_MINUTE_PATTERN
        );
        return CalendarUtil.toLocalDateTime(calendar).toLocalTime();
    }
}
