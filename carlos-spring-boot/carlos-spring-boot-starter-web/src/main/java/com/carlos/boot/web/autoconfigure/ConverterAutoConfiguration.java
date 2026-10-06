package com.carlos.boot.web.autoconfigure;

import com.carlos.boot.web.converter.DateJsonComponent;
import com.carlos.boot.web.converter.LongJsonComponent;
import com.carlos.boot.web.converter.StringConverters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

/**
 * 转换器自动配置：Spring {@code Converter} + Jackson 组件。
 *
 * <p>提供多格式日期字符串的智能解析，支持 6~8 种常见日期格式依次尝试匹配。
 * 这是 Spring Boot 内置 {@code spring.mvc.format.*} 单格式配置不具备的差异化功能。</p>
 *
 * <p><strong>注意事项：</strong></p>
 * <ul>
 *   <li>Double/Integer 转换器已移除，使用 Spring Boot 内置 StringToNumberConverterFactory</li>
 *   <li>Jackson 组件（{@link LongJsonComponent} / {@link DateJsonComponent}）在下面显式注册为 Bean，
 *       原因见 {@link #longJsonComponent()} 的说明</li>
 * </ul>
 *
 * @author Carlos
 * @since 3.0.0
 */
@Slf4j
@AutoConfiguration
public class ConverterAutoConfiguration {

    // ==================== 多格式日期转换器（Spring Boot 不具备） ====================

    @Bean
    public Converter<String, Date> stringToDateConverter() {
        return new StringConverters.StringToDateConverter();
    }

    @Bean
    public Converter<String, LocalDateTime> stringToLocalDateTimeConverter() {
        return new StringConverters.StringToLocalDateTimeConverter();
    }

    @Bean
    public Converter<String, LocalTime> stringToLocalTimeConverter() {
        return new StringConverters.StringToLocalTimeConverter();
    }

    @Bean
    public Converter<String, LocalDate> stringToLocalDateConverter() {
        return new StringConverters.StringToLocalDateConverter();
    }

    // ==================== Jackson 组件 ====================

    /**
     * Long → JSON <b>字符串</b> 的 Jackson 组件
     *
     * <h3>为什么要在这里显式注册，而不是只靠 {@code @JacksonComponent}「自动注册」</h3>
     * <p>
     * {@code @JacksonComponent} 的发现依赖<b>宿主应用的组件扫描根</b>（即 {@code @SpringBootApplication}
     * 所在包）。本类所在的 {@code com.carlos.boot.web.converter} 并不在业务服务的包下，所以：
     * </p>
     * <ul>
     *   <li>启动类放在 {@code com.carlos} 的服务（扫得到）能生效 —— 但这是<b>巧合</b>，
     *       取决于启动类的包位置这一与序列化毫无关系的因素；</li>
     *   <li>启动类放在自己业务包下的服务（如 {@code com.carlos.audit}）扫不到，于是
     *       {@code Long} 以 JSON <b>数字</b>下发。</li>
     * </ul>
     * <p>
     * 后果不是「少个格式」而是<b>静默的数据错误</b>：雪花 ID 是 19 位（约 2.1e18），
     * 超过 JS {@code Number} 的安全整数上限（2^53≈9e15），前端拿到的是被舍入的值，且看不出异常。
     * 实测：列表给前端 {@code 2103870125818683392}，前端按 {@code 2103870125818683400}
     * 去查详情 → 查不到 → 响应无 data → 审计日志详情抽屉一片空白。
     * </p>
     * <p>
     * 因此改为框架层显式注册：与扫描根彻底解耦，所有服务一律生效。
     * </p>
     * <p>
     * {@code @ConditionalOnMissingBean} 用来兜住<b>本来就能扫到</b>的服务（system / org / message / job
     * 的 -cloud 启动类在 {@code com.carlos}）：它们已经通过扫描注册了同名 Bean，
     * 此处必须退让，否则同名 Bean 冲突（{@code allow-bean-definition-overriding} 默认 false）
     * 会让那些服务直接起不来。自动配置晚于组件扫描，该条件能正确判断。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public LongJsonComponent longJsonComponent() {
        return new LongJsonComponent();
    }

    /**
     * Date 序列化/反序列化 Jackson 组件
     * <p>
     * 与 {@link #longJsonComponent()} 同因（扫描根依赖），一并显式注册。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean
    public DateJsonComponent dateJsonComponent() {
        return new DateJsonComponent();
    }
}
