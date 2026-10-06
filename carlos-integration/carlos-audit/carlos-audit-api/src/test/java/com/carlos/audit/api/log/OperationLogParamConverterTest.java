package com.carlos.audit.api.log;

import cn.hutool.json.JSONUtil;
import com.carlos.audit.api.pojo.enums.AuditLogAuthTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogBizChannelEnum;
import com.carlos.audit.api.pojo.enums.AuditLogCategoryEnum;
import com.carlos.audit.api.pojo.enums.AuditLogPrincipalTypeEnum;
import com.carlos.audit.api.pojo.enums.AuditLogStatusEnum;
import com.carlos.audit.api.pojo.param.ApiAuditLogMainParam;
import com.carlos.log.entity.OperationLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OperationLogParamConverter} 单元测试
 *
 * @author Carlos
 */
class OperationLogParamConverterTest {

    private final OperationLogParamConverter converter = new OperationLogParamConverter();

    @Test
    @DisplayName("入参为 null 时应返回 null 而不是抛异常")
    void should_return_null_when_source_is_null() {
        assertThat(converter.convert(null)).isNull();
    }

    @Test
    @DisplayName("普通字段应原样直通")
    void should_copy_plain_fields() {
        LocalDateTime now = LocalDateTime.now();
        OperationLog source = new OperationLog();
        source.setLogType("ORG_USER_CREATE");
        source.setOperation("新增用户: zhangsan");
        source.setServerTime(now);
        source.setEventTime(now);
        source.setEventDate(LocalDate.now());
        source.setPrincipalId("1001");
        source.setPrincipalName("admin");
        source.setTenantId("0");
        source.setTargetType("ORG_USER");
        source.setTargetId("2002");
        source.setTargetName("zhangsan");
        source.setDurationMs(37);
        source.setRiskLevel(50);
        source.setTraceId("trace-1");
        source.setAppName("carlos-org");

        ApiAuditLogMainParam target = converter.convert(source);

        assertThat(target.getLogType()).isEqualTo("ORG_USER_CREATE");
        assertThat(target.getOperation()).isEqualTo("新增用户: zhangsan");
        assertThat(target.getServerTime()).isEqualTo(now);
        assertThat(target.getPrincipalId()).isEqualTo("1001");
        assertThat(target.getPrincipalName()).isEqualTo("admin");
        assertThat(target.getTenantId()).isEqualTo("0");
        assertThat(target.getTargetType()).isEqualTo("ORG_USER");
        assertThat(target.getTargetId()).isEqualTo("2002");
        assertThat(target.getTargetName()).isEqualTo("zhangsan");
        assertThat(target.getDurationMs()).isEqualTo(37);
        assertThat(target.getRiskLevel()).isEqualTo(50);
        assertThat(target.getTraceId()).isEqualTo("trace-1");
        assertThat(target.getAppName()).isEqualTo("carlos-org");
    }

    @Nested
    @DisplayName("String → 枚举映射")
    class EnumMapping {

        @Test
        @DisplayName("合法枚举名应正确映射（大小写不敏感）")
        void should_map_valid_enum_names_case_insensitively() {
            OperationLog source = baseLog();
            source.setCategory("security");
            source.setPrincipalType("user");
            source.setLogStatus("fail");
            source.setAuthType("PASSWORD");
            source.setBizChannel("web");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getCategory()).isEqualTo(AuditLogCategoryEnum.SECURITY);
            assertThat(target.getPrincipalType()).isEqualTo(AuditLogPrincipalTypeEnum.USER);
            assertThat(target.getLogStatus()).isEqualTo(AuditLogStatusEnum.FAIL);
            assertThat(target.getAuthType()).isEqualTo(AuditLogAuthTypeEnum.PASSWORD);
            assertThat(target.getBizChannel()).isEqualTo(AuditLogBizChannelEnum.WEB);
        }

        @Test
        @DisplayName("非法枚举名应回退到安全默认值而不是抛异常")
        void should_fall_back_on_unknown_enum_names() {
            OperationLog source = baseLog();
            source.setCategory("NOT_A_CATEGORY");
            source.setPrincipalType("NOT_A_TYPE");
            source.setLogStatus("NOT_A_STATUS");
            source.setBizChannel("NOT_A_CHANNEL");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getCategory()).isEqualTo(AuditLogCategoryEnum.BUSINESS);
            assertThat(target.getPrincipalType()).isEqualTo(AuditLogPrincipalTypeEnum.USER);
            assertThat(target.getLogStatus()).isEqualTo(AuditLogStatusEnum.SUCCESS);
            assertThat(target.getBizChannel()).isEqualTo(AuditLogBizChannelEnum.WEB);
        }

        @Test
        @DisplayName("authType/authProvider 无默认值，非法名应映射为 null")
        void should_map_unknown_auth_fields_to_null() {
            OperationLog source = baseLog();
            source.setAuthType("NOT_A_TYPE");
            source.setAuthProvider("NOT_A_PROVIDER");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getAuthType()).isNull();
            assertThat(target.getAuthProvider()).isNull();
        }
    }

    @Nested
    @DisplayName("必填字段兜底（消费端不做 Bean Validation）")
    class EnsureDefaults {

        @Test
        @DisplayName("时间字段全空时应补齐，且 eventDate 与 eventTime 自洽")
        void should_fill_time_fields() {
            OperationLog source = new OperationLog();
            source.setLogType("JOB_RUN_ONCE");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getServerTime()).isNotNull();
            assertThat(target.getEventTime()).isEqualTo(target.getServerTime());
            assertThat(target.getEventDate()).isEqualTo(target.getServerTime().toLocalDate());
            assertThat(target.getCreatedTime()).isNotNull();
            assertThat(target.getRetentionDeadline()).isAfter(LocalDate.now());
            assertThat(target.getLogSchemaVersion()).isEqualTo(1);
        }

        @Test
        @DisplayName("无登录身份时 principalId 应兜底为匿名主体名，不能留空")
        void should_fill_principal_id_when_anonymous() {
            OperationLog source = new OperationLog();
            source.setLogType("JOB_RUN_ONCE");
            source.setPrincipalType("ANONYMOUS");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getPrincipalType()).isEqualTo(AuditLogPrincipalTypeEnum.ANONYMOUS);
            assertThat(target.getPrincipalId()).isEqualTo("ANONYMOUS");
        }

        @Test
        @DisplayName("operation 为空时应兜底为 logType，logType 也为空则兜底为 UNKNOWN")
        void should_fill_operation_and_log_type() {
            OperationLog source = new OperationLog();
            source.setLogType("SYS_DICT_ADD");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getOperation()).isEqualTo("SYS_DICT_ADD");

            ApiAuditLogMainParam blankTarget = converter.convert(new OperationLog());

            assertThat(blankTarget.getLogType()).isEqualTo("UNKNOWN");
            assertThat(blankTarget.getOperation()).isEqualTo("UNKNOWN");
            assertThat(blankTarget.getCategory()).isEqualTo(AuditLogCategoryEnum.BUSINESS);
            assertThat(blankTarget.getBizChannel()).isEqualTo(AuditLogBizChannelEnum.WEB);
            assertThat(blankTarget.getLogStatus()).isEqualTo(AuditLogStatusEnum.SUCCESS);
        }
    }

    @Nested
    @DisplayName("dynamicExtras 折叠")
    class DynamicExtras {

        @Test
        @DisplayName("异常/URL/方法应折叠进 dynamicExtras，并保证是可解析的 JSON")
        void should_fold_extra_fields_into_dynamic_extras() {
            OperationLog source = baseLog();
            source.setException("部门不存在");
            source.setExceptionClass("com.carlos.core.exception.ServiceException");
            source.setUrl("/org/user/add");
            source.setHttpMethod("POST");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getDynamicExtras()).isNotNull();
            var extras = JSONUtil.parseObj(target.getDynamicExtras());
            assertThat(extras.getStr("exception")).isEqualTo("部门不存在");
            assertThat(extras.getStr("exceptionClass"))
                .isEqualTo("com.carlos.core.exception.ServiceException");
            assertThat(extras.getStr("url")).isEqualTo("/org/user/add");
            assertThat(extras.getStr("httpMethod")).isEqualTo("POST");
        }

        @Test
        @DisplayName("含引号/换行的异常消息也应被正确转义（手工拼字符串的经典缺陷）")
        void should_escape_special_characters() {
            OperationLog source = baseLog();
            source.setException("非法参数: \"name\"\n\t<空>");

            ApiAuditLogMainParam target = converter.convert(source);

            var extras = JSONUtil.parseObj(target.getDynamicExtras());
            assertThat(extras.getStr("exception")).isEqualTo("非法参数: \"name\"\n\t<空>");
        }

        @Test
        @DisplayName("无额外字段时 dynamicExtras 应为 null")
        void should_leave_dynamic_extras_null_when_nothing_to_fold() {
            ApiAuditLogMainParam target = converter.convert(baseLog());

            assertThat(target.getDynamicExtras()).isNull();
        }

        @Test
        @DisplayName("已有 dynamicExtras 时应原样保留，不覆盖既有写入方的约定")
        void should_keep_existing_dynamic_extras() {
            OperationLog source = baseLog();
            source.setDynamicExtras("{\"custom\":\"kept\"}");
            source.setException("should be ignored");

            ApiAuditLogMainParam target = converter.convert(source);

            assertThat(target.getDynamicExtras()).isEqualTo("{\"custom\":\"kept\"}");
        }
    }

    @Test
    @DisplayName("payloadStorageType 不应被映射（消费端对该字段做无保护的 Enum.valueOf）")
    void should_not_map_payload_storage_type() {
        OperationLog source = baseLog();
        source.setPayloadStorageType("NOT_A_VALID_STORAGE_TYPE");

        ApiAuditLogMainParam target = converter.convert(source);

        assertThat(target.getPayloadStorageType()).isNull();
    }

    /**
     * 构造一条字段齐备、不会触发兜底分支的日志
     */
    private OperationLog baseLog() {
        LocalDateTime now = LocalDateTime.now();
        OperationLog source = new OperationLog();
        source.setLogType("ORG_USER_CREATE");
        source.setOperation("新增用户");
        source.setServerTime(now);
        source.setEventTime(now);
        source.setEventDate(LocalDate.now());
        source.setPrincipalId("1001");
        source.setPrincipalType("USER");
        source.setLogStatus("SUCCESS");
        source.setCategory("BUSINESS");
        source.setBizChannel("WEB");
        return source;
    }
}
