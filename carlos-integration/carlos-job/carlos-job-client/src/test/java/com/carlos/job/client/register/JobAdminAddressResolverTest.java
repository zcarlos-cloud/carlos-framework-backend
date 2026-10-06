package com.carlos.job.client.register;

import com.carlos.job.client.config.JobClientProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>
 * 调度中心地址解析器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class JobAdminAddressResolverTest {

    @Test
    @DisplayName("应该返回去掉结尾斜杠与空白的地址")
    void should_trim_trailing_slash_and_blank() {
        JobClientProperties properties = new JobClientProperties();
        properties.setAdminUrl(" http://127.0.0.1:9516/ ");

        assertEquals("http://127.0.0.1:9516", new JobAdminAddressResolver(properties).resolve());
    }

    @Test
    @DisplayName("应该返回null当未配置地址时")
    void should_return_null_when_not_configured() {
        JobClientProperties properties = new JobClientProperties();

        assertNull(new JobAdminAddressResolver(properties).resolve());
    }

    @Test
    @DisplayName("应该返回null当地址为空白时")
    void should_return_null_when_blank() {
        JobClientProperties properties = new JobClientProperties();
        properties.setAdminUrl("   ");

        assertNull(new JobAdminAddressResolver(properties).resolve());
    }

}
