package com.carlos.boot.web.converter;

import com.carlos.json.util.ConvertUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * 字符串类型转换器集合，提供 String 到各种目标类型的 Spring Converter 实现。
 *
 * @author Carlos
 * @since 3.0.0
 */
public final class StringConverters {

    private StringConverters() {
    }

    /**
     * String 转 Date 转换器。
     */
    public static class StringToDateConverter implements Converter<String, Date> {

        @Override
        public Date convert(@Nullable String source) {
            return ConvertUtil.string2Date(source);
        }
    }

    /**
     * String 转 Double 转换器。
     */
    public static class StringToDoubleConverter implements Converter<String, Double> {

        @Override
        public Double convert(@Nullable String source) {
            return ConvertUtil.string2Double(source);
        }
    }

    /**
     * String 转 Integer 转换器。
     */
    public static class StringToIntegerConverter implements Converter<String, Integer> {

        @Override
        public Integer convert(@Nullable String source) {
            return ConvertUtil.string2Integer(source);
        }
    }

    /**
     * String 转 LocalDateTime 转换器。
     */
    public static class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

        @Override
        public LocalDateTime convert(@Nullable String source) {
            return ConvertUtil.string2LocalDateTime(source);
        }
    }

    /**
     * String 转 LocalDate 转换器。
     */
    public static class StringToLocalDateConverter implements Converter<String, LocalDate> {

        @Override
        public LocalDate convert(@Nullable String source) {
            return ConvertUtil.string2LocalDate(source);
        }
    }

    /**
     * String 转 LocalTime 转换器。
     */
    public static class StringToLocalTimeConverter implements Converter<String, LocalTime> {

        @Override
        public LocalTime convert(@Nullable String source) {
            return ConvertUtil.string2LocalTime(source);
        }
    }
}
