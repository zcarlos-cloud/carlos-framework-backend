package com.carlos.audit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SensitiveDataUtil 单元测试
 */
class SensitiveDataUtilTest {

    @Test
    void should_mask_password_field_in_json() {
        String json = """
            {"username":"admin","password":"secret123","age":30}""";
        String result = SensitiveDataUtil.maskJson(json);
        assertTrue(result.contains("\"password\":\"*********\""));
        assertTrue(result.contains("\"username\":\"admin\""));
    }

    @Test
    void should_mask_phone_field_in_json() {
        String json = """
            {"userName":"张三","phone":"13812345678"}""";
        String result = SensitiveDataUtil.maskJson(json);
        assertTrue(result.contains("\"phone\":\"138****5678\""));
    }

    @Test
    void should_mask_id_card_field_in_json() {
        String json = """
            {"name":"张三","idCard":"11010119900307000X"}""";
        String result = SensitiveDataUtil.maskJson(json);
        assertTrue(result.contains("\"idCard\":\"110101********000X\""));
    }

    @Test
    void should_mask_email_and_token_in_nested_json() {
        String json = """
            {"user":{"email":"zhangsan@example.com","token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5Nqeg"}}""";
        String result = SensitiveDataUtil.maskJson(json);
        assertTrue(result.contains("\"email\":\"z*******@example.com\""));
        assertTrue(result.contains("\"token\":\"eyJhbGciOi****NHl0w5Nqeg\""));
    }

    @Test
    void should_return_null_or_blank_as_is() {
        assertNull(SensitiveDataUtil.maskJson(null));
        assertEquals("", SensitiveDataUtil.maskJson(""));
        assertEquals("   ", SensitiveDataUtil.maskJson("   "));
    }

    @Test
    void should_return_original_when_json_invalid() {
        String invalid = "{not a valid json";
        assertEquals(invalid, SensitiveDataUtil.maskJson(invalid));
    }
}
