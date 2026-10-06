package com.carlos.log.aspect;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.carlos.disruptor.core.DisruptorTemplate;
import com.carlos.log.annotation.Log;
import com.carlos.log.entity.OperationLog;
import com.carlos.log.enums.BusinessType;
import com.carlos.log.storage.LogStorage;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link LogAspect} 单元测试
 * <p>
 * 重点覆盖请求参数的敏感字段剔除 —— 该能力此前只声明未实现，导致明文口令/密钥
 * 被原样写入审计库。
 * </p>
 *
 * @author Carlos
 */
class LogAspectTest {

    private final List<OperationLog> captured = new ArrayList<>();

    private final SampleEndpoints endpoints = new SampleEndpoints();

    private LogAspect aspect;

    @BeforeEach
    void setUp() {
        captured.clear();
        aspect = new LogAspect(new EmptyDisruptorProvider(), new CapturingLogStorage());
    }

    @Test
    @DisplayName("默认策略下 password 字段不应出现在请求参数里")
    void should_exclude_password_by_default() {
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");
        param.setPassword("Hadoop@2022!");

        OperationLog log = invoke("createUser", param);

        assertThat(paramJson(log)).contains("zhangsan");
        assertThat(paramJson(log)).doesNotContain("Hadoop@2022!");
        assertThat(paramJson(log)).doesNotContain("password");
    }

    @Test
    @DisplayName("排除词按「包含」匹配：newPassword / secretKey / accessKey 这类派生字段名也应命中")
    void should_exclude_derived_field_names() {
        StorageParam param = new StorageParam();
        param.setBucketName("carlos-default");
        param.setAccessKey("AKIAIOSFODNN7EXAMPLE");
        param.setSecretKey("super-secret-value");
        param.setNewPassword("brand-new-password");

        OperationLog log = invoke("createStorage", param);

        // 非敏感的配置项要留着
        assertThat(paramJson(log)).contains("carlos-default");
        assertThat(paramJson(log)).doesNotContain("AKIAIOSFODNN7EXAMPLE");
        assertThat(paramJson(log)).doesNotContain("super-secret-value");
        assertThat(paramJson(log)).doesNotContain("brand-new-password");
    }

    @Test
    @DisplayName("嵌套对象与数组内的敏感字段也应被递归剔除")
    void should_exclude_nested_sensitive_fields() {
        NestedParam param = new NestedParam();
        param.setName("外层");
        NestedDetail detail = new NestedDetail();
        detail.setToken("abcdefghijklmnopqrstuvwxyz0123456789");
        detail.setUsername("inner");
        param.setDetail(detail);
        param.setDetails(List.of(detail));

        OperationLog log = invoke("createNested", param);

        assertThat(paramJson(log)).contains("外层");
        assertThat(paramJson(log)).contains("inner");
        assertThat(paramJson(log)).doesNotContain("abcdefghijklmnopqrstuvwxyz0123456789");
    }

    @Test
    @DisplayName("字面就叫 credential 的字段整体剔除（宁可多排除，不可漏排除）")
    void should_drop_whole_credential_subtree() {
        CredentialHolderParam param = new CredentialHolderParam();
        param.setId(7L);
        CredentialHolderParam.Credential credential = new CredentialHolderParam.Credential();
        credential.setUsername("inner");
        credential.setPassword("Secret#2026");
        param.setCredential(credential);

        OperationLog log = invoke("createWithCredential", param);

        assertThat(paramJson(log)).contains("7");
        assertThat(paramJson(log)).doesNotContain("inner");
        assertThat(paramJson(log)).doesNotContain("Secret#2026");
        assertThat(paramJson(log)).doesNotContain("credential");
    }

    @Test
    @DisplayName("excludeParams 默认值必须覆盖真实字段名：pwd / newPwd / oldPwd / confirmPwd")
    void should_exclude_pwd_style_field_names_by_default() {
        PwdParam param = new PwdParam();
        param.setId(1001L);
        param.setNewPwd("BrandNew#2026");
        param.setOldPwd("OldSecret#2025");
        param.setConfirmPwd("BrandNew#2026");

        OperationLog log = invoke("resetPassword", param);

        // 有效载荷（id）要留着 —— 排除的是口令，不是整条参数
        assertThat(paramJson(log)).contains("1001");
        assertThat(paramJson(log)).doesNotContain("BrandNew#2026");
        assertThat(paramJson(log)).doesNotContain("OldSecret#2025");
        assertThat(paramJson(log)).doesNotContain("newPwd");
        assertThat(paramJson(log)).doesNotContain("oldPwd");
        assertThat(paramJson(log)).doesNotContain("confirmPwd");
    }

    @Test
    @DisplayName("以裸标量传入的敏感参数（如 @RequestHeader String token）也应按参数名剔除")
    void should_exclude_sensitive_bare_argument_by_parameter_name() {
        JobRegisterParam param = new JobRegisterParam();
        param.setJobName("demoJob");
        String token = "9f2c98d5f580b0a4418b79c4af87e27cddf55f80605fb3fb";

        OperationLog log = invoke("register", param, token);

        // 参数对象要留着，裸标量凭据必须剔除
        assertThat(paramJson(log)).contains("demoJob");
        assertThat(paramJson(log)).doesNotContain("9f2c98d5f580b0a4418b79c4af87e27cddf55f80605fb3fb");
        assertThat(paramJson(log)).doesNotContain("9f2c98d5");
    }

    @Test
    @DisplayName("saveRequestParams=false 时不记录任何参数")
    void should_not_record_params_when_disabled() {
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");

        OperationLog log = invoke("importUsers", param);

        assertThat(log.getRequestPayloadRef()).isNull();
    }

    @Test
    @DisplayName("MultipartFile 参数应被跳过而不是尝试序列化")
    void should_skip_multipart_file_arguments() {
        MultipartFile file = mock(MultipartFile.class);
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");

        OperationLog log = invoke("upload", file, param);

        JSONArray params = JSONUtil.parseArray(log.getRequestPayloadRef());
        assertThat(params).hasSize(1);
        assertThat(params.getJSONObject(0).getStr("username")).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("MultipartFile[] 批量上传参数也应被跳过（数组不是 MultipartFile 的实例）")
    void should_skip_multipart_file_array_arguments() {
        MultipartFile[] files = {mock(MultipartFile.class), mock(MultipartFile.class)};
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");

        OperationLog log = invoke("uploadBatch", files, param);

        JSONArray params = JSONUtil.parseArray(log.getRequestPayloadRef());
        assertThat(params).hasSize(1);
        assertThat(params.getJSONObject(0).getStr("username")).isEqualTo("zhangsan");
    }

    @Test
    @DisplayName("方法抛异常时也应落一条 FAIL 日志并把异常写入 dynamicExtras 来源字段")
    void should_record_failure_and_rethrow() throws Throwable {
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");

        Object[] args = {param};
        ProceedingJoinPoint point = pointFor(findMethod("createUser", args), args);
        when(point.proceed()).thenThrow(new IllegalStateException("用户名已存在"));

        assertThatCode(() -> aspect.around(point, findAnnotation("createUser", args)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("用户名已存在");

        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).getLogStatus()).isEqualTo("FAIL");
        assertThat(captured.get(0).getException()).isEqualTo("用户名已存在");
    }

    @Test
    @DisplayName("condition 为 false 时不应记录日志，但业务方法必须照常执行")
    void should_skip_logging_when_condition_is_false() throws Throwable {
        UserCreateParam param = new UserCreateParam();
        param.setUsername("zhangsan");

        Object[] args = {param};
        ProceedingJoinPoint point = pointFor(findMethod("conditionallyLogged", args), args);
        when(point.proceed()).thenReturn("业务返回值");

        Object result = aspect.around(point, findAnnotation("conditionallyLogged", args));

        assertThat(result).isEqualTo("业务返回值");
        assertThat(captured).isEmpty();
    }

    @Test
    @DisplayName("参数超过 maxParamLength 时应截断，避免单条日志过大")
    void should_truncate_long_params() {
        UserCreateParam param = new UserCreateParam();
        param.setUsername("x".repeat(500));
        param.setRemark("y".repeat(500));

        OperationLog log = invoke("createShortParamUser", param);

        assertThat(log.getRequestPayloadRef()).hasSizeLessThan(400);
        assertThat(log.getRequestPayloadRef()).endsWith("...(truncated)");
    }

    /**
     * 调用带注解的示例方法并返回捕获到的日志
     */
    private OperationLog invoke(String methodName, Object... args) {
        try {
            ProceedingJoinPoint point = pointFor(findMethod(methodName, args), args);
            when(point.proceed()).thenReturn(null);
            aspect.around(point, findAnnotation(methodName, args));
        } catch (Throwable e) {
            throw new AssertionError("调用 " + methodName + " 失败", e);
        }
        assertThat(captured).hasSize(1);
        return captured.get(0);
    }

    /**
     * 构造 JoinPoint 替身
     * <p>
     * 必须提供真实的 {@link MethodSignature} 与 target —— SpEL 上下文按参数名绑定变量
     * （{@code #param}），缺了签名就取不到参数名，表达式会静默退化成「原样返回表达式字符串」。
     * </p>
     */
    private ProceedingJoinPoint pointFor(Method method, Object[] args) {
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getArgs()).thenReturn(args);
        when(point.getTarget()).thenReturn(endpoints);
        return point;
    }

    private Method findMethod(String methodName, Object[] args) throws NoSuchMethodException {
        for (Method method : SampleEndpoints.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                return method;
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private Log findAnnotation(String methodName, Object[] args) throws NoSuchMethodException {
        return findMethod(methodName, args).getAnnotation(Log.class);
    }

    private String paramJson(OperationLog log) {
        assertThat(log.getRequestPayloadRef()).isNotNull();
        return log.getRequestPayloadRef();
    }

    /**
     * 供测试使用的日志存储器
     */
    private class CapturingLogStorage implements LogStorage {

        @Override
        public boolean store(OperationLog log) {
            captured.add(log);
            return true;
        }

        @Override
        public boolean storeBatch(List<OperationLog> logs) {
            captured.addAll(logs);
            return true;
        }
    }

    /**
     * 同步模式下 DisruptorTemplate 可能不存在，此处模拟该情形
     */
    private static class EmptyDisruptorProvider implements ObjectProvider<DisruptorTemplate<OperationLog>> {

        @Override
        public DisruptorTemplate<OperationLog> getObject() {
            throw new UnsupportedOperationException();
        }

        @Override
        public DisruptorTemplate<OperationLog> getIfAvailable() {
            return null;
        }
    }

    // ==================== 示例端点 ====================

    @SuppressWarnings("unused")
    private static class SampleEndpoints {

        @Log(title = "用户管理", logType = "ORG_USER_CREATE", businessType = BusinessType.INSERT)
        void createUser(UserCreateParam param) {
        }

        @Log(title = "用户管理", logType = "ORG_USER_CREATE", businessType = BusinessType.INSERT,
                maxParamLength = 200)
        void createShortParamUser(UserCreateParam param) {
        }

        @Log(title = "用户管理", logType = "ORG_USER_IMPORT", businessType = BusinessType.IMPORT,
                saveRequestParams = false)
        void importUsers(UserCreateParam param) {
        }

        @Log(title = "文件服务", logType = "SYS_FILE_UPLOAD", businessType = BusinessType.INSERT)
        void upload(MultipartFile file, UserCreateParam param) {
        }

        @Log(title = "文件服务", logType = "SYS_FILE_UPLOAD_BATCH", businessType = BusinessType.INSERT)
        void uploadBatch(MultipartFile[] files, UserCreateParam param) {
        }

        @Log(title = "存储配置", logType = "SYS_STORAGE_CREATE", businessType = BusinessType.INSERT)
        void createStorage(StorageParam param) {
        }

        @Log(title = "用户管理", logType = "ORG_USER_RESET_PASSWORD", businessType = BusinessType.UPDATE,
                riskLevel = 60)
        void resetPassword(PwdParam param) {
        }

        @Log(title = "嵌套", logType = "TEST_NESTED", businessType = BusinessType.INSERT)
        void createNested(NestedParam param) {
        }

        @Log(title = "凭据", logType = "TEST_CREDENTIAL", businessType = BusinessType.INSERT)
        void createWithCredential(CredentialHolderParam param) {
        }

        /**
         * 敏感值以裸标量传入（对应真实场景里的 @RequestHeader String token）——
         * 它不是对象字段，只按字段名过滤挡不住
         */
        @Log(title = "任务注册", logType = "TEST_JOB_REGISTER", businessType = BusinessType.INSERT)
        void register(JobRegisterParam param, String token) {
        }

        @Log(title = "用户管理", logType = "ORG_USER_CREATE", businessType = BusinessType.INSERT,
                condition = "#param.username == 'never'")
        String conditionallyLogged(UserCreateParam param) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    public static class UserCreateParam {

        private String username;

        private String password;

        private String remark;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    @SuppressWarnings("unused")
    public static class StorageParam {

        private String bucketName;

        private String accessKey;

        private String secretKey;

        private String newPassword;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * 字段名刻意用本代码库真实的写法（pwd 系），而不是想当然的 "password"
     */
    @SuppressWarnings("unused")
    public static class PwdParam {

        private Long id;

        private String newPwd;

        private String oldPwd;

        private String confirmPwd;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getNewPwd() {
            return newPwd;
        }

        public void setNewPwd(String newPwd) {
            this.newPwd = newPwd;
        }

        public String getOldPwd() {
            return oldPwd;
        }

        public void setOldPwd(String oldPwd) {
            this.oldPwd = oldPwd;
        }

        public String getConfirmPwd() {
            return confirmPwd;
        }

        public void setConfirmPwd(String confirmPwd) {
            this.confirmPwd = confirmPwd;
        }
    }

    @SuppressWarnings("unused")
    public static class NestedParam {

        private String name;

        private NestedDetail detail;

        private List<NestedDetail> details;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public NestedDetail getDetail() {
            return detail;
        }

        public void setDetail(NestedDetail detail) {
            this.detail = detail;
        }

        public List<NestedDetail> getDetails() {
            return details;
        }

        public void setDetails(List<NestedDetail> details) {
            this.details = details;
        }
    }

    @SuppressWarnings("unused")
    public static class NestedDetail {

        private String username;

        private String token;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    @SuppressWarnings("unused")
    public static class JobRegisterParam {

        private String jobName;

        public String getJobName() {
            return jobName;
        }

        public void setJobName(String jobName) {
            this.jobName = jobName;
        }
    }

    /**
     * 字段名就叫 credential —— 应被整体剔除，而不是只剔里面名为敏感的那些字段
     */
    @SuppressWarnings("unused")
    public static class CredentialHolderParam {

        private Long id;

        private Credential credential;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Credential getCredential() {
            return credential;
        }

        public void setCredential(Credential credential) {
            this.credential = credential;
        }

        @SuppressWarnings("unused")
        public static class Credential {

            private String username;

            private String password;

            public String getUsername() {
                return username;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }
    }
}
