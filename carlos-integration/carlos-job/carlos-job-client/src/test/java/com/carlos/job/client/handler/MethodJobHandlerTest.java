package com.carlos.job.client.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>
 * 方法级任务处理器单元测试
 * </p>
 *
 * @author Carlos
 * @date 2026年9月23日
 */
class MethodJobHandlerTest {

    private static final String BEAN_NAME = "sampleJob";

    private DefaultListableBeanFactory beanFactory;

    private SampleJob target;

    @BeforeEach
    void setUp() {
        beanFactory = new DefaultListableBeanFactory();
        target = new SampleJob();
        beanFactory.registerSingleton(BEAN_NAME, target);
    }

    @Test
    @DisplayName("应该执行无参方法并返回成功")
    void should_execute_no_arg_method_and_return_ok() throws Exception {
        MethodJobHandler handler = handlerOf("noArgs");

        JobResult result = handler.execute(null);

        assertTrue(result.getSuccess());
        assertTrue(target.isInvoked());
    }

    @Test
    @DisplayName("应该把参数透传给接收String的方法")
    void should_pass_params_when_method_accepts_string() throws Exception {
        MethodJobHandler handler = handlerOf("withParams", String.class);

        JobResult result = handler.execute("{\"id\":7}");

        assertTrue(result.getSuccess());
        assertEquals("{\"id\":7}", target.getReceived());
    }

    @Test
    @DisplayName("应该原样透传方法返回的JobResult")
    void should_pass_through_job_result_when_method_returns_one() throws Exception {
        MethodJobHandler handler = handlerOf("failed");

        JobResult result = handler.execute(null);

        assertFalse(result.getSuccess());
        assertEquals("业务失败", result.getMessage());
    }

    @Test
    @DisplayName("应该原样抛出目标方法异常而不是包装成反射异常")
    void should_throw_business_exception_when_target_throws() throws Exception {
        MethodJobHandler handler = handlerOf("throwing");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> handler.execute(null));

        assertEquals("业务异常", exception.getMessage());
    }

    @Test
    @DisplayName("应该在执行时才解析目标Bean以便拿到代理对象")
    void should_resolve_target_bean_at_execution_time() throws Exception {
        MethodJobHandler handler = handlerOf("noArgs");
        // 执行前替换容器中的实例：缓存了引用就会打到旧对象上
        SampleJob replaced = new SampleJob();
        beanFactory.destroySingleton(BEAN_NAME);
        beanFactory.registerSingleton(BEAN_NAME, replaced);

        handler.execute(null);

        assertTrue(replaced.isInvoked());
        assertFalse(target.isInvoked());
    }

    @Test
    @DisplayName("应该返回注解声明的处理器名称")
    void should_return_declared_handler_name() throws Exception {
        assertEquals("sampleHandler", handlerOf("noArgs").name());
    }

    @Test
    @DisplayName("应该判为失败当声明返回JobResult的方法返回null时")
    void should_fail_when_declared_job_result_is_null() throws Exception {
        MethodJobHandler handler = handlerOf("nullResult");

        JobResult result = handler.execute(null);

        assertFalse(result.getSuccess());
        assertTrue(result.getMessage().contains("null"));
    }

    private MethodJobHandler handlerOf(String methodName, Class<?>... paramTypes) throws Exception {
        Method method = SampleJob.class.getMethod(methodName, paramTypes);
        return new MethodJobHandler(beanFactory, BEAN_NAME, "sampleHandler", method, method.getParameterCount() == 1);
    }

    /**
     * 被测目标：四种合法签名的样例
     */
    static class SampleJob {

        private boolean invoked;

        private String received;

        public boolean isInvoked() {
            return invoked;
        }

        public String getReceived() {
            return received;
        }

        public void noArgs() {
            this.invoked = true;
        }

        public void withParams(String params) {
            this.invoked = true;
            this.received = params;
        }

        public JobResult failed() {
            return JobResult.fail("业务失败");
        }

        public JobResult nullResult() {
            return null;
        }

        public void throwing() {
            throw new IllegalStateException("业务异常");
        }

    }

}
